#!/usr/bin/env python3
"""
pythonIDE/optimize_toi.py — Two-ball optimal control with TOI-Velocity

Standard-library-only fallback when torch is unavailable.
Uses a low-dimensional control parametrization (12 knots per channel,
24 parameters total) with central-difference gradients + Adam.

Why not SPSA: SPSA estimates a single scalar gradient direction shared
by all parameters. That is fine in ~50 dims and hopeless at 960, where
it degenerates to a random walk around the initial guess. Parametrizing
the control as a smooth piecewise-linear curve reduces the search space
by ~40× and restores per-parameter gradients.
"""

import math
import random
import hashlib
import json
import os
from datetime import datetime, timezone

# Optional accelerators (not required)
try:
    import numpy as np
    HAS_NUMPY = True
except ImportError:
    HAS_NUMPY = False

try:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    HAS_MPL = True
except ImportError:
    HAS_MPL = False


# ======================================================================
# TOI-Velocity step  (stdlib port of toi_step.step_with_toi)
# State: [p1x, p1y, v1x, v1y, p2x, p2y, v2x, v2y]
# u    : [ux, uy]  (control on ball 1 only)
# ======================================================================
def step_with_toi(s, u, dt, r=0.2):
    p1x, p1y, v1x, v1y, p2x, p2y, v2x, v2y = s
    ux, uy = u

    # relative position / velocity (ball1 − ball2)
    px, py = p1x - p2x, p1y - p2y
    vx, vy = v1x - v2x, v1y - v2y

    # Time-of-impact: solve |p + v t|^2 = (2r)^2  (linear approx, valid for small dt)
    d2r = 4.0 * r * r
    a = vx * vx + vy * vy
    b = 2.0 * (px * vx + py * vy)
    c = px * px + py * py - d2r

    tcol = None
    if a > 1e-12 and c > 0.0:
        disc = b * b - 4.0 * a * c
        if disc >= 0.0:
            sq = math.sqrt(disc)
            for tc in ((-b - sq) / (2 * a), (-b + sq) / (2 * a)):
                if 1e-9 <= tc <= dt:
                    tcol = tc
                    break

    hit = 0
    if tcol is not None:
        # advance to collision (with acceleration)
        p1xc = p1x + v1x * tcol + 0.5 * ux * tcol * tcol
        p1yc = p1y + v1y * tcol + 0.5 * uy * tcol * tcol
        p2xc = p2x + v2x * tcol
        p2yc = p2y + v2y * tcol
        v1xc = v1x + ux * tcol
        v1yc = v1y + uy * tcol

        # collision normal (from ball 2 to ball 1)
        nx, ny = p1xc - p2xc, p1yc - p2yc
        nn = math.hypot(nx, ny)
        if nn > 1e-12:
            nx /= nn
            ny /= nn
        else:
            nx, ny = 0.0, 1.0

        # equal-mass elastic collision: swap normal components
        vn = (v1xc - v2x) * nx + (v1yc - v2y) * ny
        if vn < 0.0:
            v1xc -= vn * nx
            v1yc -= vn * ny
            v2x  += vn * nx
            v2y  += vn * ny
        hit = 1

        # advance remaining time
        dt_rem = dt - tcol
        p1x = p1xc + v1xc * dt_rem + 0.5 * ux * dt_rem * dt_rem
        p1y = p1yc + v1yc * dt_rem + 0.5 * uy * dt_rem * dt_rem
        v1x = v1xc + ux * dt_rem
        v1y = v1yc + uy * dt_rem
        p2x = p2xc + v2x * dt_rem
        p2y = p2yc + v2y * dt_rem
    else:
        # no collision this step
        p1x = p1x + v1x * dt + 0.5 * ux * dt * dt
        p1y = p1y + v1y * dt + 0.5 * uy * dt * dt
        v1x = v1x + ux * dt
        v1y = v1y + uy * dt
        p2x = p2x + v2x * dt
        p2y = p2y + v2y * dt

    return [p1x, p1y, v1x, v1y, p2x, p2y, v2x, v2y], hit


# ======================================================================
# Loss / forward rollout
# ======================================================================
def forward(u_seq, N, dt, lam, s0):
    s = list(s0)
    effort = 0.0
    hits = 0
    for i in range(N):
        ux, uy = u_seq[i]
        effort += (ux * ux + uy * uy)
        s, hit = step_with_toi(s, u_seq[i], dt)
        hits += hit
    loss = s[4] * s[4] + s[5] * s[5] + lam * effort * dt
    return loss, hits, s


# ======================================================================
# Control parametrization: K knots per channel, linear interpolation
# ======================================================================
def make_controls(params, N, K):
    """
    params = [ux_0 .. ux_{K-1}, uy_0 .. uy_{K-1}]
    Returns a list of N control vectors, linearly interpolated between knots.
    """
    ux_k = params[:K]
    uy_k = params[K:]
    u = []
    denom = (N - 1) if N > 1 else 1
    for i in range(N):
        pos = i * (K - 1) / denom
        j = int(pos)
        if j >= K - 1:
            j = K - 2
        fr = pos - j
        ux = (1.0 - fr) * ux_k[j] + fr * ux_k[j + 1]
        uy = (1.0 - fr) * uy_k[j] + fr * uy_k[j + 1]
        u.append([ux, uy])
    return u


# ======================================================================
# Main
# ======================================================================
def run_optimization():
    T, N = 1.0, 480
    dt = T / N
    r = 0.2
    lam = 0.01
    K = 12                                     # knots per channel
    n_par = 2 * K                              # 24 parameters total

    s0 = [-1.0, -2.0, 0.0, 0.0,
          -1.0, -1.0, 0.0, 0.0]

    # initial guess: constant u = [0, 3]
    params = [0.0] * K + [3.0] * K

    # Adam state
    m = [0.0] * n_par
    v = [0.0] * n_par
    b1, b2, eps_adam = 0.9, 0.999, 1e-8
    lr_adam = 0.15
    fd_eps = 1e-4                              # central-difference step
    n_iter = 150

    loss_history, hit_history = [], []

    print("=" * 80)
    print("TOI-Velocity Optimal Control — Two-Ball Shooter (stdlib fallback)")
    print(f"  T={T}s, N={N}, dt={dt:.6f}s, lam={lam}")
    print(f"  Backend: stdlib{'+numpy' if HAS_NUMPY else ''}, "
          f"optimizer: FD/Adam, K={K} knots ({n_par} params)")
    print("=" * 80)

    for it in range(n_iter):
        # loss at current params
        u_cur = make_controls(params, N, K)
        loss0, hits0, _ = forward(u_cur, N, dt, lam, s0)

        # central-difference gradient over all 2K params
        grad = [0.0] * n_par
        for p_i in range(n_par):
            pp = list(params); pp[p_i] += fd_eps
            pm = list(params); pm[p_i] -= fd_eps
            lp, _, _ = forward(make_controls(pp, N, K), N, dt, lam, s0)
            lm, _, _ = forward(make_controls(pm, N, K), N, dt, lam, s0)
            grad[p_i] = (lp - lm) / (2.0 * fd_eps)

        # Adam update
        for p_i in range(n_par):
            m[p_i] = b1 * m[p_i] + (1 - b1) * grad[p_i]
            v[p_i] = b2 * v[p_i] + (1 - b2) * grad[p_i] * grad[p_i]
            m_hat = m[p_i] / (1 - b1 ** (it + 1))
            v_hat = v[p_i] / (1 - b2 ** (it + 1))
            params[p_i] -= lr_adam * m_hat / (math.sqrt(v_hat) + eps_adam)

        loss_history.append(loss0)
        hit_history.append(hits0)

        if it % 15 == 0:
            uy_mean = sum(params[K:]) / K
            uy_std = (sum((x - uy_mean) ** 2 for x in params[K:]) / K) ** 0.5
            gnorm = math.sqrt(sum(g * g for g in grad))
            print(f"Iter {it:4d}: loss={loss0:.6f}, hits={hits0}, "
                  f"u_y_mean={uy_mean:.4f}±{uy_std:.4f}, |grad|={gnorm:.4f}")

    # ------------------------------------------------------------------
    # Final results
    # ------------------------------------------------------------------
    u_final_seq = make_controls(params, N, K)
    final_loss, final_hits, final_s = forward(u_final_seq, N, dt, lam, s0)
    p2_final = [final_s[4], final_s[5]]

    u_x = [row[0] for row in u_final_seq]
    u_y = [row[1] for row in u_final_seq]

    print()
    print("=" * 80)
    print("FINAL RESULTS")
    print("=" * 80)
    print(f"  Final loss: {final_loss:.6f}")
    print(f"  Hits: {final_hits}")
    print(f"  Ball 2 final position: ({p2_final[0]:.4f}, {p2_final[1]:.4f})")
    print(f"  Knots (u_x): {[round(x, 3) for x in params[:K]]}")
    print(f"  Knots (u_y): {[round(x, 3) for x in params[K:]]}")

    collision_idx = max(range(N), key=lambda i: u_y[i])
    print(f"  Estimated collision step: {collision_idx} ({collision_idx * dt:.4f}s)")

    print()
    print("-" * 80)
    print("CONTROL SEQUENCE — FIRST 10 STEPS:")
    for i in range(min(10, N)):
        print(f"  step {i:3d}: u_x={u_x[i]:.6f}, u_y={u_y[i]:.6f}")

    print()
    print("-" * 80)
    print("CONTROL SEQUENCE — PRE-COLLISION RAMP (steps around max):")
    rs, re = max(0, collision_idx - 10), min(N, collision_idx + 5)
    for i in range(rs, re):
        marker = " <-- collision" if i == collision_idx else ""
        print(f"  step {i:3d}: u_x={u_x[i]:.6f}, u_y={u_y[i]:.6f}{marker}")

    print()
    print("-" * 80)
    print("CONTROL SEQUENCE — LAST 10 STEPS:")
    for i in range(max(0, N - 10), N):
        print(f"  step {i:3d}: u_x={u_x[i]:.6f}, u_y={u_y[i]:.6f}")

    # ------------------------------------------------------------------
    # Cryptographic seal (untruncated)
    # ------------------------------------------------------------------
    hash_data = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "loss": final_loss,
        "hits": final_hits,
        "p2_final": [float(x) for x in p2_final],
        "u_first_10": [float(x) for x in u_x[:10]] + [float(x) for x in u_y[:10]],
        "u_last_10":  [float(x) for x in u_x[-10:]] + [float(x) for x in u_y[-10:]],
        "collision_step": int(collision_idx),
        "u_x_mean": float(sum(u_x) / N),
        "u_y_mean": float(sum(u_y) / N),
        "u_y_max":  float(max(u_y)),
        "K_knots": int(K),
    }
    hash_hex = hashlib.sha256(
        json.dumps(hash_data, sort_keys=True).encode()
    ).hexdigest()

    print()
    print("-" * 80)
    print("CRYPTOGRAPHIC SEAL:")
    print(f"  SHA-256: {hash_hex}")

    # ------------------------------------------------------------------
    # Optional plot
    # ------------------------------------------------------------------
    os.makedirs("ledger", exist_ok=True)
    if HAS_MPL:
        t = [i * dt for i in range(N)]
        fig, axes = plt.subplots(2, 2, figsize=(12, 8))

        axes[0, 0].plot(loss_history)
        axes[0, 0].set_title("Loss vs Iteration")
        axes[0, 0].set_xlabel("Iteration")
        axes[0, 0].set_ylabel("Loss")
        axes[0, 0].grid(True)

        axes[0, 1].plot(hit_history)
        axes[0, 1].set_title("Hits per Iteration")
        axes[0, 1].set_xlabel("Iteration")
        axes[0, 1].set_ylabel("Hits")
        axes[0, 1].grid(True)

        axes[1, 0].plot(t, u_x, label="u_x")
        axes[1, 0].plot(t, u_y, label="u_y")
        axes[1, 0].axvline(collision_idx * dt, color="r", linestyle="--", label="collision")
        axes[1, 0].set_title("Learned Control Sequence")
        axes[1, 0].set_xlabel("Time (s)")
        axes[1, 0].set_ylabel("Control")
        axes[1, 0].legend()
        axes[1, 0].grid(True)

        rs2, re2 = max(0, collision_idx - 60), min(N, collision_idx + 10)
        axes[1, 1].plot(t[rs2:re2], u_y[rs2:re2], "b-", linewidth=2)
        axes[1, 1].axvline(collision_idx * dt, color="r", linestyle="--", label="collision")
        axes[1, 1].set_title("Pre-Collision u_y Ramp (paper Fig. 8)")
        axes[1, 1].set_xlabel("Time (s)")
        axes[1, 1].set_ylabel("u_y")
        axes[1, 1].legend()
        axes[1, 1].grid(True)

        plt.tight_layout()
        plot_path = "ledger/optimize_toi_plot.png"
        plt.savefig(plot_path)
        print(f"\n  Plot saved: {plot_path}")

    # ------------------------------------------------------------------
    # Save JSON results
    # ------------------------------------------------------------------
    result_path = "ledger/optimize_toi_results.json"
    with open(result_path, "w") as f:
        json.dump({
            "loss_history": loss_history,
            "hit_history": hit_history,
            "u_x": u_x,
            "u_y": u_y,
            "u_x_knots": params[:K],
            "u_y_knots": params[K:],
            "final_loss": final_loss,
            "final_hits": final_hits,
            "p2_final": p2_final,
            "collision_step": int(collision_idx),
            "hash": hash_hex,
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }, f, indent=2)
    print(f"  Results saved: {result_path}")

    return {
        "loss": final_loss,
        "hits": final_hits,
        "p2_final": p2_final,
        "collision_step": int(collision_idx),
        "hash": hash_hex,
        "u_x": u_x,
        "u_y": u_y,
    }


if __name__ == "__main__":
    results = run_optimization()

    print()
    print("=" * 80)
    print("OPTIMIZATION COMPLETE")
    print("=" * 80)
    print(f"  Loss: {results['loss']:.6f}")
    print(f"  Hits: {results['hits']}")
    print(f"  Hash: {results['hash']}")
    print("=" * 80)
