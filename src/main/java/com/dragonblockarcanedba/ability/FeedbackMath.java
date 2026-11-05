package com.dragonblockarcanedba.ability;

/**
 * Pure math for the Phase 5 feedback service. No Minecraft state,
 * no randomness: the same inputs give the same outputs on the
 * server, on every client, and in the verification harness.
 */
public final class FeedbackMath {
    private FeedbackMath() {}

    /**
     * Distance-squared falloff for a shake, Iron's Spells style:
     * full strength at the origin, falling quadratically to zero
     * at the radius. A radius of 0 or less means a direct shake
     * (no origin, no falloff: the legacy per-player sends) and
     * always returns 1.
     */
    public static float falloff(double distanceSq, double radius) {
        if (radius <= 0.0) {
            return 1.0f;
        }
        if (distanceSq <= 0.0) {
            return 1.0f;
        }
        double r2 = radius * radius;
        if (distanceSq >= r2) {
            return 0.0f;
        }
        double f = 1.0 - distanceSq / r2;
        return (float) f;
    }
}
