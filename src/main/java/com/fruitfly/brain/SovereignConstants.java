package com.fruitfly.brain;

/**
 * phi-harmonic constants of the Sovereign Brain.
 * Namespace only; not instantiable.
 */
public final class SovereignConstants {

    /** The golden ratio phi = (1 + sqrt(5))/2. */
    public static final double PHI = (1.0 + Math.sqrt(5.0)) / 2.0;

    public static final double PHI2 = PHI * PHI;
    public static final double PHI3 = PHI * PHI * PHI;
    public static final double PHI4 = PHI * PHI * PHI * PHI;
    public static final double PHI5 = PHI * PHI * PHI * PHI * PHI;
    public static final double PHI6 = PHI * PHI * PHI * PHI * PHI * PHI;
    public static final double PHI7 = PHI6 * PHI;
    public static final double PHI8 = PHI6 * PHI2;
    public static final double PHI9 = PHI8 * PHI;

    public static final double PHI_INV = 1.0 / PHI;

    /** Math.pow underflows to 0 for these exponents in double. Kept as declarations. */
    public static final double PHI_MINUS_709  = Math.pow(PHI, -709);
    public static final double PHI_MINUS_1418 = Math.pow(PHI, -1418);
    public static final double PHI_MINUS_1500 = Math.pow(PHI, -1500);

    /** Exact learning rate. 0.999... equals 1; stored as 1.0. */
    public static final double ETA_UNITY = 1.0;
    public static final double LEARNING_RATE_UNITY = ETA_UNITY;

    private SovereignConstants() {
    }
}
