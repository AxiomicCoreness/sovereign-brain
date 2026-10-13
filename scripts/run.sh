#!/usr/bin/env bash
# Run the Sovereign Brain simulation.
#
# Modes:
#   (default)        interactive  — forwards "$@" to the JAR as-is
#   --autonomous     headless autonomous run — no TTY, no prompts, self-terminating
#   --automated      CI/automated run       — non-interactive, structured exit codes
#
# Autonomous ≠ automated:
#   autonomous — the process drives itself; no human in the loop at runtime
#   automated  — the process is driven by a machine (CI, cron, workflow); no human
#                at the invocation site either
# In practice: autonomous sets the JVM/runtime flags for a self-driving run;
# automated additionally sets the shell-level flags that make failures legible
# to the caller (set -euo pipefail already on; exit codes are the contract).
set -euo pipefail

# ── Resolve script and repo root ─────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${REPO_ROOT}"

# ── Mode detection ──────────────────────────────────────────────────
MODE="interactive"
EXTRA_JAVA_ARGS=()
PASSTHROUGH=()

for arg in "$@"; do
    case "${arg}" in
        --autonomous)
            # FIX 1: autonomous mode — self-driving, headless
            MODE="autonomous"
            ;;
        --automated)
            # FIX 2: automated mode — CI-driven, strict, machine-readable
            MODE="automated"
            ;;
        *)
            PASSTHROUGH+=("${arg}")
            ;;
    esac
done

case "${MODE}" in
    autonomous)
        # No TTY, no stdin, self-terminating. The JVM runs to completion
        # without waiting on any human input.
        EXTRA_JAVA_ARGS+=(
            "-Dsovereign.mode=autonomous"
            "-Dsovereign.headless=true"
            "-Djava.awt.headless=true"
            "-Dsovereign.selfTerminate=true"
        )
        # Close stdin so anything that tries to read blocks and exits,
        # rather than hanging the process forever.
        exec </dev/null
        ;;
    automated)
        # Machine-driven run: strict, non-interactive, unambiguous exit codes,
        # and structured output for downstream log parsers.
        EXTRA_JAVA_ARGS+=(
            "-Dsovereign.mode=automated"
            "-Dsovereign.headless=true"
            "-Djava.awt.headless=true"
            "-Dsovereign.selfTerminate=true"
            "-Dsovereign.exitOnError=true"
            "-Dsovereign.logFormat=structured"
        )
        # Fail the whole shell if any piped stage fails — already set above,
        # but restated here so it survives a future refactor of the top of file.
        set -o pipefail
        exec </dev/null
        ;;
    interactive)
        # Leave the JVM as a normal foreground process. No headless, no
        # self-terminate; a human is expected at the terminal.
        ;;
esac

# ── Build if missing ─────────────────────────────────────────────────
JAR="target/sovereign-brain-1.0.0-SOVEREIGN.jar"

if [[ ! -f "${JAR}" ]]; then
    if [[ "${MODE}" == "interactive" ]]; then
        echo "JAR not found, running build first..."
    else
        # In autonomous/automated modes, keep stdout clean for log parsers;
        # route the notice to stderr.
        echo "JAR not found, running build first..." >&2
    fi
    "${REPO_ROOT}/scripts/build.sh"
fi

# ── Launch ───────────────────────────────────────────────────────────
# JAVA_OPTS, if set by the caller or by CI, is honoured in all modes.
# EXTRA_JAVA_ARGS are mode-specific and come after JAVA_OPTS so that mode
# flags take precedence over generic flags.
# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} "${EXTRA_JAVA_ARGS[@]}" -jar "${JAR}" "${PASSTHROUGH[@]}"
