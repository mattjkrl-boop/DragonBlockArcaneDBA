package com.dragonblockarcanedba.util;

/**
 * Shared lifecycle envelope for spectacle effects (SPECTACLE-STANDARD point 5).
 * Every long lived effect, entity side and renderer side, evaluates the same
 * envelope so particles, damage windows, alpha, and scale all agree: the effect
 * builds up, peaks, then dissipates. Nothing pops in and nothing vanishes.
 *
 * Pure math, no Minecraft state, safe on both sides.
 */
public final class FxEnvelope {
    private FxEnvelope() {}

    /** Standard fractions: build over the first 20 percent, dissipate over the last 30 percent. */
    public static final float STD_BUILD = 0.20f;
    public static final float STD_DECAY = 0.30f;

    public static float smoothstep(float t) {
        float c = Math.max(0f, Math.min(1f, t));
        return c * c * (3f - 2f * c);
    }

    /**
     * Envelope value 0..1 at the given age.
     * Rises 0 to 1 across the first buildFrac of life, holds 1, then falls to 0
     * across the last decayFrac of life. Ages past life return 0.
     */
    public static float envelope(float age, float life, float buildFrac, float decayFrac) {
        if (life <= 0f) return 0f;
        if (age <= 0f) return 0f;
        if (age >= life) return 0f;
        float t = age / life;
        float in = buildFrac <= 0f ? 1f : smoothstep(t / buildFrac);
        float out = decayFrac <= 0f ? 1f : smoothstep((1f - t) / decayFrac);
        return Math.min(in, out);
    }

    public static float envelope(float age, float life) {
        return envelope(age, life, STD_BUILD, STD_DECAY);
    }

    /** True while the effect is in its damaging window: past build-up, before dissipation ends. */
    public static boolean inActiveWindow(float age, float life, float buildFrac, float decayFrac) {
        if (life <= 0f) return false;
        float t = age / life;
        return t >= buildFrac && t <= 1f - decayFrac * 0.5f;
    }

    /** Gentle breathing pulse 0..1 around a base level, for idle motion in renderers. */
    public static float pulse(float age, float speed, float base, float amp) {
        return base + amp * (float) Math.sin(age * speed);
    }

    /**
     * Renderer distance scaling (SPECTACLE-STANDARD point 1): full detail within
     * near blocks, stepped down beyond mid and far so distant effects cost less.
     * Returns a multiplier for segment counts; alphas scale with sqrt of it.
     */
    public static float distScale(double cameraDistance, double near, double far) {
        if (cameraDistance <= near) return 1.0f;
        if (cameraDistance >= far) return 0.35f;
        double t = (cameraDistance - near) / (far - near);
        return (float) (1.0 - 0.65 * t);
    }

    public static float distScale(double cameraDistance) {
        return distScale(cameraDistance, 32.0, 96.0);
    }

    /** Scaled segment count for a ring or band, never below a readable minimum. */
    public static int segments(int base, float scale, int min) {
        return Math.max(min, Math.round(base * scale));
    }
}
