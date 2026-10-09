package com.fruitfly.brain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarriorQuadraticTest {

    @Test
    void positiveRootIsPhi() {
        assertEquals(SovereignConstants.PHI, WarriorQuadratic.positiveRoot(), 1e-15);
    }

    @Test
    void negativeRootIsMinusInvPhi() {
        assertEquals(-SovereignConstants.PHI_INV, WarriorQuadratic.negativeRoot(), 1e-15);
    }

    @Test
    void vietaSumIsOne() {
        assertEquals(1.0,
                WarriorQuadratic.positiveRoot() + WarriorQuadratic.negativeRoot(),
                1e-15);
    }

    @Test
    void vietaProductIsMinusOne() {
        assertEquals(-1.0,
                WarriorQuadratic.positiveRoot() * WarriorQuadratic.negativeRoot(),
                1e-15);
    }

    @Test
    void seedEvaluatesToZeroAtPhi() {
        assertTrue(Math.abs(WarriorQuadratic.evaluate(SovereignConstants.PHI)) < 1e-15);
    }

    @Test
    void discriminantIsFive() {
        assertEquals(5.0, WarriorQuadratic.DISCRIMINANT, 1e-15);
    }

    @Test
    void vertexIsHalf() {
        assertEquals(0.5, WarriorQuadratic.vertexX(), 1e-15);
    }

    @Test
    void verifyAllInvariants() {
        assertTrue(WarriorQuadratic.verify());
    }
}
