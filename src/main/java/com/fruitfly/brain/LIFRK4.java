package com.fruitfly.brain;

import java.util.function.DoubleUnaryOperator;

/**
 * Leaky integrate-and-fire neuron, RK4 macro-step.
 * Threshold is checked on the completed step, not on each internal stage.
 */
public final class LIFRK4 {

    private final double tauM;
    private final double vRest;
    private final double vReset;
    private final double vThreshold;
    private final double rM;
    private final double refractory;

    private double v;
    private double lastSpikeTime;
    private boolean firedThisStep;
    private long spikeCount;

    public LIFRK4() {
        this(0.020, -65.0, -70.0, -45.0, 10.0, 0.0022);
    }

    public LIFRK4(double tauM, double vRest, double vReset,
                  double vThreshold, double rM, double refractory) {
        this.tauM = tauM;
        this.vRest = vRest;
        this.vReset = vReset;
        this.vThreshold = vThreshold;
        this.rM = rM;
        this.refractory = refractory;
        this.v = vRest;
        this.lastSpikeTime = -Double.MAX_VALUE;
    }

    private double rhs(double vIn, double iIn) {
        return (-(vIn - vRest) + rM * iIn) / tauM;
    }

    public boolean step(double t, double dt, DoubleUnaryOperator input) {
        firedThisStep = false;
        if (t - lastSpikeTime < refractory) {
            v = vReset;
            return false;
        }

        double i1 = input.applyAsDouble(t);
        double k1 = rhs(v, i1);

        double v2 = v + 0.5 * dt * k1;
        double i2 = input.applyAsDouble(t + 0.5 * dt);
        double k2 = rhs(v2, i2);

        double v3 = v + 0.5 * dt * k2;
        double i3 = input.applyAsDouble(t + 0.5 * dt);
        double k3 = rhs(v3, i3);

        double v4 = v + dt * k3;
        double i4 = input.applyAsDouble(t + dt);
        double k4 = rhs(v4, i4);

        double vNext = v + (dt / 6.0) * (k1 + 2.0 * k2 + 2.0 * k3 + k4);
        if (vNext >= vThreshold) {
            v = vReset;
            lastSpikeTime = t + dt;
            firedThisStep = true;
            spikeCount++;
        } else {
            v = vNext;
        }
        return firedThisStep;
    }

    public double voltage() { return v; }
    public boolean firedThisStep() { return firedThisStep; }
    public long spikeCount() { return spikeCount; }
    public double tauM() { return tauM; }
    public double threshold() { return vThreshold; }

    public void reset() {
        v = vRest;
        lastSpikeTime = -Double.MAX_VALUE;
        firedThisStep = false;
        spikeCount = 0;
    }
}
