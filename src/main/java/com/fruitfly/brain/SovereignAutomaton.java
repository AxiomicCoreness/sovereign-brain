package com.fruitfly.brain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/** Orchestrator. Advances SovereigntyState on a time threshold. */
public final class SovereignAutomaton {

    private static final Logger LOG = LoggerFactory.getLogger(SovereignAutomaton.class);

    private final List<LIFRK4> swarm;
    private final Connectome connectome;
    private final DarkStateInvariant darkState;
    private SovereigntyState state;
    private double timeSeconds;

    public SovereignAutomaton(Connectome connectome, int swarmSize) {
        this.connectome = connectome;
        this.swarm = new ArrayList<>(swarmSize);
        for (int i = 0; i < swarmSize; i++) {
            this.swarm.add(new LIFRK4());
        }
        this.darkState = new DarkStateInvariant();
        this.state = SovereigntyState.OBSERVATION;
        this.timeSeconds = 0.0;
    }

    public void step(double dt, double externalCurrent) {
        final int n = swarm.size();
        final double[] inputTo = new double[n];

        // Accumulate outgoing currents into targets (network, not self-loop)
        for (int j = 0; j < n; j++) {
            double vj = swarm.get(j).voltage();
            double[] outOfJ = connectome.currentsFrom(j, vj, dt);
            for (int k = 0; k < n; k++) {
                inputTo[k] += outOfJ[k];
            }
        }

        for (int i = 0; i < n; i++) {
            final double drive = externalCurrent + inputTo[i];
            swarm.get(i).step(timeSeconds, dt, t -> drive);
        }

        timeSeconds += dt;
        // Advance at most one phase per call; document dt ≤ 0.1 if multi-threshold needed
        double threshold = 0.1 * (state.ordinal() + 1);
        if (timeSeconds >= threshold) {
            SovereigntyState next = state.next();
            if (next != state) {
                LOG.info("Sovereignty phase: {} -> {}", state, next);
                state = next;
            }
        }
    }

    public String respond(String text, double dt, double external) {
        step(dt, external);
        return "phase=" + state + " t=" + timeSeconds;
    }

    public SovereigntyState state() { return state; }
    public double time() { return timeSeconds; }
    public List<LIFRK4> swarm() { return swarm; }
    public Connectome connectome() { return connectome; }
    public DarkStateInvariant darkState() { return darkState; }

    public void collapseToDarkState() {
        state = SovereigntyState.DARK_STATE;
        LOG.info("Automaton collapsed to DARK_STATE");
    }
}
