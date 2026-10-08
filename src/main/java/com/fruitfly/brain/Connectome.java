package com.fruitfly.brain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** CSR connectome with a delay ring per synapse. */
public final class Connectome {

    private final int n;
    private final int[] rowPtr;
    private final int[] colIdx;
    private final double[] weight;
    private final double[] delayMs;
    private final ArrayDeque<Double>[] delayRing;

    @SuppressWarnings("unchecked")
    public Connectome(int n, int[] rowPtr, int[] colIdx,
                      double[] weight, double[] delayMs) {
        if (rowPtr.length != n + 1) {
            throw new IllegalArgumentException("rowPtr must be length n+1");
        }
        if (colIdx.length != weight.length || weight.length != delayMs.length) {
            throw new IllegalArgumentException("CSR arrays must agree in nnz");
        }
        this.n = n;
        this.rowPtr = rowPtr;
        this.colIdx = colIdx;
        this.weight = weight;
        this.delayMs = delayMs;
        this.delayRing = new ArrayDeque[colIdx.length];
        for (int i = 0; i < colIdx.length; i++) {
            this.delayRing[i] = new ArrayDeque<>();
        }
    }

    public int neuronCount() { return n; }
    public int synapseCount() { return colIdx.length; }

    public int[] postsynaptic(int i) {
        int start = rowPtr[i];
        int end = rowPtr[i + 1];
        int[] out = new int[end - start];
        System.arraycopy(colIdx, start, out, 0, out.length);
        return out;
    }

    public double[] weightsOf(int i) {
        int start = rowPtr[i];
        int end = rowPtr[i + 1];
        double[] out = new double[end - start];
        System.arraycopy(weight, start, out, 0, out.length);
        return out;
    }

    public double[] currentsFrom(int pre, double preVoltage, double dtSeconds) {
        int start = rowPtr[pre];
        int end = rowPtr[pre + 1];
        double[] currents = new double[n];
        for (int k = start; k < end; k++) {
            int post = colIdx[k];
            delayRing[k].addLast(preVoltage);
            int ringLen = Math.max(2,
                    (int) Math.ceil(delayMs[k] / (dtSeconds * 1000.0)) + 1);
            while (delayRing[k].size() > ringLen) {
                delayRing[k].removeFirst();
            }
            Double delayed = delayRing[k].peekFirst();
            if (delayed != null) {
                currents[post] += weight[k] * delayed;
            }
        }
        return currents;
    }

    public static Connectome random(int n, double sparsity, long seed) {
        Random rng = new Random(seed);
        List<Integer> col = new ArrayList<>();
        List<Double> w = new ArrayList<>();
        List<Double> d = new ArrayList<>();
        int[] row = new int[n + 1];
        for (int i = 0; i < n; i++) {
            row[i] = col.size();
            for (int j = 0; j < n; j++) {
                if (rng.nextDouble() < sparsity && i != j) {
                    col.add(j);
                    w.add(rng.nextDouble() * 2.0 - 1.0);
                    d.add(1.0 + rng.nextDouble() * 4.0);
                }
            }
        }
        row[n] = col.size();
        int[] ci = col.stream().mapToInt(Integer::intValue).toArray();
        double[] ww = w.stream().mapToDouble(Double::doubleValue).toArray();
        double[] dd = d.stream().mapToDouble(Double::doubleValue).toArray();
        return new Connectome(n, row, ci, ww, dd);
    }
}
