package com.fruitfly.brain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Command-line entry point. */
public final class SovereignMain {

    private static final Logger LOG = LoggerFactory.getLogger(SovereignMain.class);

    public static void main(String[] args) {
        LOG.info("Sovereign Brain starting. phi = {}", SovereignConstants.PHI);

        int n = 144;
        Connectome connectome = Connectome.random(n, 0.05, 20260720L);
        LOG.info("Connectome built: {} neurons, {} synapses",
                connectome.neuronCount(), connectome.synapseCount());

        DarkStateInvariant dark = new DarkStateInvariant();
        LOG.info("Dark-state verify: {} lambda2={} residual={}",
                dark.verify(), dark.lambda2(), dark.eigenvalueResidual());
        LOG.info("Quadratic-seed residual: {}", dark.quadraticSeedResidual());

        SovereignAutomaton automaton = new SovereignAutomaton(connectome, n);
        double dt = 1e-4;
        int steps = 10_000;
        for (int k = 0; k < steps; k++) {
            automaton.step(dt, 0.5);
        }
        LOG.info("Simulation complete at t = {} s, final state = {}",
                automaton.time(), automaton.state());

        double[] voltages = automaton.swarm().stream()
                .mapToDouble(LIFRK4::voltage).toArray();
        double[][] tensor = new double[n][n];
        GradientSynchronization.gradientSynchronizationPriority(voltages, tensor);
        LOG.info("Gradient sync applied with eta = {}", SovereignConstants.ETA_UNITY);
        LOG.info("Sovereign Brain terminated");
    }

    private SovereignMain() {
    }
}
