package com.dragonblockarcanedba.client.fx;

import net.minecraft.util.RandomSource;

/**
 * Client camera shake and hitstop state, 2.0. Shakes stack (strongest wins) and decay
 * over their duration with a trauma-squared envelope. The offset itself is layered sine
 * sway (two frequencies per axis, random phases per trigger) plus a directional kick
 * that decays with the same envelope, so a hit first shoves the view one way and then
 * settles, instead of the old per-tick white-noise buzz. During hitstop the camera
 * holds a single sharp offset, which reads as a freeze frame on impact.
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
    private static float kickYaw, kickPitch;
    private static float phaseA, phaseB;
    private static long tickCount;
    /** Global multiplier from config, 0 disables shake. */
    public static float scale = 1.0f;

    public static void trigger(float newIntensity, int newDuration, int newHitstop) {
        float active = remaining > 0 ? intensity * remaining / (float) Math.max(1, duration) : 0f;
        if (newIntensity >= active) {
            intensity = Math.min(newIntensity, 6.0f);
            duration = Math.max(1, newDuration);
            remaining = duration;
            // Directional kick: the impulse pushes one way first.
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            kickYaw = (float) Math.cos(angle);
            kickPitch = (float) Math.sin(angle) * 0.7f;
            // Fresh sway phases so repeated shakes do not sync up.
            phaseA = RANDOM.nextFloat() * (float) (Math.PI * 2.0);
            phaseB = RANDOM.nextFloat() * (float) (Math.PI * 2.0);
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
        tickCount++;
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
        double t = tickCount;
        // Smooth sway: two layered sines per axis, kept inside the old jitter's range.
        double swayYaw = (Math.sin(t * 1.9 + phaseA) + Math.sin(t * 3.7 + phaseB) * 0.5) / 1.5;
        double swayPitch = (Math.sin(t * 2.3 + phaseB) + Math.sin(t * 4.1 + phaseA) * 0.5) / 1.5;
        curYaw = (float) (swayYaw * amp * 0.6 + kickYaw * amp * 0.4);
        curPitch = (float) (swayPitch * amp * 0.6 + kickPitch * amp * 0.4);
        remaining--;
    }

    public static float yaw(float partialTick) {
        return (prevYaw + (curYaw - prevYaw) * partialTick) * scale;
    }

    public static float pitch(float partialTick) {
        return (prevPitch + (curPitch - prevPitch) * partialTick) * scale;
    }
}
