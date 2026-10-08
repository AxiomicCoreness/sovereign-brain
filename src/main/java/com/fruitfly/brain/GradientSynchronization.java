package com.fruitfly.brain;

/** Hebbian tensor update, then row L2 normalisation. */
public final class GradientSynchronization {

    private GradientSynchronization() {
    }

    public static double[][] gradientSynchronizationPriority(
            double[] swarmVoltages,
            double[][] tensor,
            double learningRate) {
        int n = swarmVoltages.length;
        if (tensor.length != n) {
            throw new IllegalArgumentException("tensor dims must match swarm size");
        }
        double[] s = new double[n];
        for (int i = 0; i < n; i++) {
            s[i] = swarmVoltages[i] >= -45.0 ? 1.0 : 0.0;
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                tensor[i][j] += learningRate * s[i] * s[j];
            }
        }
        for (int i = 0; i < n; i++) {
            double rowNorm = 0.0;
            for (int j = 0; j < n; j++) {
                rowNorm += tensor[i][j] * tensor[i][j];
            }
            rowNorm = Math.sqrt(rowNorm);
            if (rowNorm > 1e-12) {
                for (int j = 0; j < n; j++) {
                    tensor[i][j] /= rowNorm;
                }
            }
        }
        return tensor;
    }

    public static double[][] gradientSynchronizationPriority(
            double[] swarmVoltages,
            double[][] tensor) {
        return gradientSynchronizationPriority(
                swarmVoltages, tensor, SovereignConstants.ETA_UNITY);
    }
}
