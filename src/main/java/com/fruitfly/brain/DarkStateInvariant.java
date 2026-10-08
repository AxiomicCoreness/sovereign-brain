package com.fruitfly.brain;

/**
 * Trimer Hamiltonian.
 * Dark-state vector (1, 0, -1)/sqrt(2) has eigenvalue 1.
 */
public final class DarkStateInvariant {

    private final double[][] h;
    private final double lambda2;
    private final double lambdaMinus;
    private final double lambdaPlus;
    private final double[] darkStateVector;
    private final double quadraticSeedResidual;

    public DarkStateInvariant() {
        double a = SovereignConstants.PHI_INV;
        this.h = new double[][] {
                {1.0, a, 0.0},
                {a, 2.0, a},
                {0.0, a, 1.0}
        };
        double disc = 9.0 - 8.0 / SovereignConstants.PHI;
        this.lambda2 = 1.0;
        this.lambdaMinus = (3.0 - Math.sqrt(disc)) / 2.0;
        this.lambdaPlus = (3.0 + Math.sqrt(disc)) / 2.0;
        double invSqrt2 = 1.0 / Math.sqrt(2.0);
        this.darkStateVector = new double[] {invSqrt2, 0.0, -invSqrt2};
        double x = SovereignConstants.PHI;
        this.quadraticSeedResidual = x * x - x - 1.0;
    }

    public double[][] hamiltonian() { return h; }
    public double lambda2() { return lambda2; }
    public double lambdaMinus() { return lambdaMinus; }
    public double lambdaPlus() { return lambdaPlus; }
    public double[] darkStateVector() { return darkStateVector.clone(); }
    public double quadraticSeedResidual() { return quadraticSeedResidual; }

    public double eigenvalueResidual() {
        double[] hv = matVec(h, darkStateVector);
        double w = darkStateVector[0];
        double lam = hv[0] / w;
        return Math.abs(lam - 1.0);
    }

    public boolean verify() {
        return Math.abs(quadraticSeedResidual) < 1e-12
                && eigenvalueResidual() < 1e-12;
    }

    private static double[] matVec(double[][] a, double[] v) {
        int n = v.length;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            double s = 0.0;
            for (int j = 0; j < n; j++) {
                s += a[i][j] * v[j];
            }
            out[i] = s;
        }
        return out;
    }
}
