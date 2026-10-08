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
        for (int i = 0; i < swarm.size(); i++) {
            LIFRK4 neuron = swarm.get(i);
            double[] byNeuron = connectome.currentsFrom(i, neuron.voltage(), dt);
            double self = byNeuron[i];
            neuron.step(timeSeconds, dt, t -> externalCurrent + self);
        }
        timeSeconds += dt;
        if (timeSeconds >= 0.1 * (state.ordinal() + 1)) {
            SovereigntyState next = state.next();
            if (next != state) {
                LOG.info("Sovereignty phase: {} -> {}", state, next);
                state = next;
            }
        }
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
