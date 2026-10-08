package com.fruitfly.brain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DarkStateInvariantTest {

    @Test
    void darkStateEigenvalueIsOne() {
        DarkStateInvariant d = new DarkStateInvariant();
        assertEquals(1.0, d.lambda2(), 1e-12);
        assertTrue(d.eigenvalueResidual() < 1e-12);
    }

    @Test
    void quadraticSeedRootIsPhi() {
        DarkStateInvariant d = new DarkStateInvariant();
        assertTrue(Math.abs(d.quadraticSeedResidual()) < 1e-12);
    }

    @Test
    void eigenvectorsMatchAnalyticValues() {
        DarkStateInvariant d = new DarkStateInvariant();
        assertEquals(0.493058, d.lambdaMinus(), 1e-5);
        assertEquals(2.506942, d.lambdaPlus(), 1e-5);
    }

    @Test
    void verifyReturnsTrue() {
        assertTrue(new DarkStateInvariant().verify());
    }
}
