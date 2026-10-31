package com.dragonblockarcanedba.client.fx;

import net.minecraft.util.RandomSource;

/**
 * Client camera shake and hitstop state. Shakes stack (strongest wins) and decay over their duration.
 * During hitstop the camera holds a single sharp offset, which reads as a freeze frame on impact.
 */
public final class ScreenShakeHandler {
    private ScreenShakeHandler() {}

    private static final RandomSource RANDOM = RandomSource.create();
    private static float intensity;
    private static int duration;
    private static int remaining;
    private static int hitstop;
    private static float holdYaw, holdPitch;
    private static float prevYaw, prevPitch, curYaw, curPitch;
    /** Global multiplier from config, 0 disables shake. */
    public static float scale = 1.0f;

    public static void trigger(float newIntensity, int newDuration, int newHitstop) {
        float active = remaining > 0 ? intensity * remaining / (float) Math.max(1, duration) : 0f;
        if (newIntensity >= active) {
            intensity = Math.min(newIntensity, 6.0f);
            duration = Math.max(1, newDuration);
            remaining = duration;
        }
        if (newHitstop > hitstop) {
            hitstop = newHitstop;
            holdYaw = (RANDOM.nextFloat() - 0.5f) * 2f * intensity;
            holdPitch = (RANDOM.nextFloat() - 0.5f) * 2f * intensity;
        }
    }

    public static void tick() {
        prevYaw = curYaw;
        prevPitch = curPitch;
        if (hitstop > 0) {
            hitstop--;
            curYaw = holdYaw;
            curPitch = holdPitch;
            return;
        }
        if (remaining <= 0) {
            curYaw = 0f;
            curPitch = 0f;
            return;
        }
        float k = remaining / (float) duration;
        float amp = intensity * k * k;
        curYaw = (RANDOM.nextFloat() - 0.5f) * 2f * amp;
        curPitch = (RANDOM.nextFloat() - 0.5f) * 2f * amp;
        remaining--;
    }

    public static float yaw(float partialTick) {
        return (prevYaw + (curYaw - prevYaw) * partialTick) * scale;
    }

    public static float pitch(float partialTick) {
        return (prevPitch + (curPitch - prevPitch) * partialTick) * scale;
    }
}
