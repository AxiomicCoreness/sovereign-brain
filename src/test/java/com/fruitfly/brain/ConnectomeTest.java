package com.fruitfly.brain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectomeTest {

    @Test
    void randomConnectomeHasCorrectShape() {
        Connectome c = Connectome.random(64, 0.1, 42L);
        assertEquals(64, c.neuronCount());
        assertTrue(c.synapseCount() > 0);
    }

    @Test
    void csrRowPtrIsMonotone() {
        Connectome c = Connectome.random(32, 0.2, 7L);
        int[] rowPtr = rowPtrOf(c);
        for (int i = 0; i < rowPtr.length - 1; i++) {
            assertTrue(rowPtr[i] <= rowPtr[i + 1]);
        }
    }

    private static int[] rowPtrOf(Connectome c) {
        try {
            var field = Connectome.class.getDeclaredField("rowPtr");
            field.setAccessible(true);
            return (int[]) field.get(c);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
