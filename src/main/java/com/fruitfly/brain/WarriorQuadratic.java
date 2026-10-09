package com.fruitfly.brain;

/**
 * Seed polynomial x^2 - x - 1 = 0.
 *
 * Roots:
 *   x+ = (1 + sqrt(5)) / 2 = phi
 *   x- = (1 - sqrt(5)) / 2 = -1/phi
 *
 * Vieta: sum = 1, product = -1, discriminant = 5, vertex = 1/2.
 * Not a general quadratic solver.
 */
public final class WarriorQuadratic {

    public static final double A = 1.0;
    public static final double B = -1.0;
    public static final double C = -1.0;

    /** b^2 - 4ac = 5. */
    public static final double DISCRIMINANT = 5.0;

    private WarriorQuadratic() {
    }

    public static double positiveRoot() {
        return (-B + Math.sqrt(DISCRIMINANT)) / (2.0 * A);
    }

    public static double negativeRoot() {
        return (-B - Math.sqrt(DISCRIMINANT)) / (2.0 * A);
    }

    public static double evaluate(double x) {
        return A * x * x + B * x + C;
    }

    public static double sumResidual() {
        return Math.abs((positiveRoot() + negativeRoot()) - (-B / A));
    }

    public static double productResidual() {
        return Math.abs((positiveRoot() * negativeRoot()) - (C / A));
    }

    public static double seedResidual() {
        return Math.abs(evaluate(positiveRoot()));
    }

    public static double vertexX() {
        return -B / (2.0 * A);
    }

    public static boolean verify(double tol) {
        return seedResidual() < tol
            && sumResidual() < tol
            && productResidual() < tol
            && Math.abs(positiveRoot() - SovereignConstants.PHI) < tol;
    }

    public static boolean verify() {
        return verify(1e-12);
    }
}
