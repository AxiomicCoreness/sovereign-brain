package com.fruitfly.brain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LIFRK4Test {

    @Test
    void subThresholdInputDoesNotFire() {
        LIFRK4 neuron = new LIFRK4();
        for (int k = 0; k < 1000; k++) {
            neuron.step(k * 1e-4, 1e-4, t -> 0.0);
        }
        assertEquals(0, neuron.spikeCount());
    }

    @Test
    void strongInputFires() {
        LIFRK4 neuron = new LIFRK4();
        boolean fired = false;
        for (int k = 0; k < 10_000 && !fired; k++) {
            fired = neuron.step(k * 1e-4, 1e-4, t -> 5.0);
        }
        assertTrue(fired, "neuron should fire under 5 uA drive");
        assertEquals(1, neuron.spikeCount());
    }

    @Test
    void refractoryClampHolds() {
        LIFRK4 neuron = new LIFRK4();
        boolean fired = false;
        double t = 0.0;
        double dt = 1e-4;
        while (!fired && t < 1.0) {
            fired = neuron.step(t, dt, x -> 5.0);
            t += dt;
        }
        assertTrue(fired);
        assertEquals(-70.0, neuron.voltage(), 1e-9);
    }
}
