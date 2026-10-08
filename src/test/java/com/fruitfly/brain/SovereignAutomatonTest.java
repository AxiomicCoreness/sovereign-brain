package com.fruitfly.brain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SovereignAutomatonTest {

    @Test
    void stateAdvancesOverTime() {
        Connectome c = Connectome.random(16, 0.1, 1L);
        SovereignAutomaton a = new SovereignAutomaton(c, 16);
        SovereigntyState start = a.state();
        for (int k = 0; k < 10_000; k++) {
            a.step(1e-4, 0.5);
        }
        assertNotEquals(start, a.state());
    }

    @Test
    void darkStateIsAbsorbing() {
        Connectome c = Connectome.random(8, 0.1, 2L);
        SovereignAutomaton a = new SovereignAutomaton(c, 8);
        a.collapseToDarkState();
        assertEquals(SovereigntyState.DARK_STATE, a.state());
        a.step(1e-4, 0.5);
        assertEquals(SovereigntyState.DARK_STATE, a.state());
    }
}
