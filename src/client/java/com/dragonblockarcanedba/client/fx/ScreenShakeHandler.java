package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.ability.FeedbackMath;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Client camera shake store, 3.0 (Phase 5). Shakes arrive as
 * ScreenShakePayloads, each naming an origin and a radius (radius
 * 0 is a direct shake with no falloff: the legacy movement and
 * finisher sends). The store holds the active shakes and
 * arbitrates Iron's Spells style: every tick, each shake's
 * effective amplitude is its intensity scaled by the
 * distance-squared falloff from the local player to its origin
 * (ability/FeedbackMath) and by its own trauma-squared decay, and
 * the strongest effective shake drives the camera. Weaker shakes
 * are not discarded the way 2.0 replaced-or-ignored them; they
 * wait in the store and can surface as the winner decays, which
 * is what nearest-wins arbitration means with several live
 * sources. The sway itself is unchanged from 2.0: layered sines
 * (two frequencies per axis, random phases per trigger) plus a
 * directional kick that decays with the same envelope.
 *
 * During hitstop the camera holds a single sharp offset, which
 * reads as a freeze frame on impact; shakes do not age while the
 * hold runs, exactly as 2.0 behaved. Hitstop here is the camera
 * hold the MobPhysics finishers already used. It is NOT the
 * ability hit-stop the Phase 5 schema defers (a caster-local
 * animation and carrier clock scale); see the Phase 5 report.
 */
public final class ScreenShakeHandler {
    private ScreenShakeHandler() {}

    private static final class Shake {
        float intensity;
        int duration;
        int elapsed;
        double ox, oy, oz;
        float radius;
        float kickYaw, kickPitch;
        float phaseA, phaseB;
    }

    private static final int MAX_ACTIVE = 8;
    private static final List<Shake> ACTIVE = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private static int hitstop;
    private static float holdYaw, holdPitch;
    private static float prevYaw, prevPitch, curYaw, curPitch;
    private static long tickCount;
    /** Global multiplier from config, 0 disables shake. */
    public static float scale = 1.0f;

    /**
     * Where the viewer stands, for falloff. A supplier so the
     * verification harness can stand somewhere deterministic; in
     * game it reads the local player every tick, so walking toward
     * or away from a shake's origin mid-shake changes its weight,
     * as the arbitration promises.
     */
    public static Supplier<Vec3> viewerPos = () -> {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.player != null ? mc.player.position() : null;
    };

    /** Direct shake (no origin falloff): the legacy call shape. */
    public static void trigger(float intensity, int duration, int hitstopTicks) {
        Vec3 v = viewerPos.get();
        trigger(intensity, duration, hitstopTicks,
                v != null ? v.x : 0, v != null ? v.y : 0, v != null ? v.z : 0, 0f);
    }

    public static void trigger(float newIntensity, int newDuration, int newHitstop,
                               double ox, double oy, double oz, float radius) {
        Shake s = new Shake();
        s.intensity = Math.min(newIntensity, 6.0f);
        s.duration = Math.max(1, newDuration);
        s.elapsed = 0;
        s.ox = ox;
        s.oy = oy;
        s.oz = oz;
        s.radius = radius;
        // Directional kick: the impulse pushes one way first.
        double angle = RANDOM.nextDouble() * Math.PI * 2.0;
        s.kickYaw = (float) Math.cos(angle);
        s.kickPitch = (float) Math.sin(angle) * 0.7f;
        // Fresh sway phases so repeated shakes do not sync up.
        s.phaseA = RANDOM.nextFloat() * (float) (Math.PI * 2.0);
        s.phaseB = RANDOM.nextFloat() * (float) (Math.PI * 2.0);
        if (ACTIVE.size() >= MAX_ACTIVE) {
            ACTIVE.remove(weakest());
        }
        ACTIVE.add(s);
        if (newHitstop > hitstop) {
            hitstop = newHitstop;
            float eff = effectiveAmp(s, viewerPos.get());
            holdYaw = (RANDOM.nextFloat() - 0.5f) * 2f * eff;
            holdPitch = (RANDOM.nextFloat() - 0.5f) * 2f * eff;
        }
    }

    /** The active shake with the smallest current effective amplitude. */
    private static Shake weakest() {
        Vec3 v = viewerPos.get();
        Shake worst = null;
        float worstAmp = Float.MAX_VALUE;
        for (Shake s : ACTIVE) {
            float amp = effectiveAmp(s, v);
            if (amp < worstAmp) {
                worstAmp = amp;
                worst = s;
            }
        }
        return worst;
    }

    /** Effective amplitude of one shake for a viewer at the given position (null viewer: at the origin). */
    static float effectiveAmp(Shake s, Vec3 viewer) {
        float k = (s.duration - s.elapsed) / (float) s.duration;
        if (k <= 0f) {
            return 0f;
        }
        double distSq = 0.0;
        if (viewer != null) {
            double dx = viewer.x - s.ox;
            double dy = viewer.y - s.oy;
            double dz = viewer.z - s.oz;
            distSq = dx * dx + dy * dy + dz * dz;
        }
        return s.intensity * FeedbackMath.falloff(distSq, s.radius) * k * k;
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
        if (ACTIVE.isEmpty()) {
            curYaw = 0f;
            curPitch = 0f;
            return;
        }
        Vec3 v = viewerPos.get();
        Shake winner = null;
        float winnerAmp = 0f;
        Iterator<Shake> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Shake s = it.next();
            // Sample first, age second: a fresh shake's first tick
            // is its full strength, the old handler's sequence
            // (remaining duration, then decrement).
            float amp = effectiveAmp(s, v);
            if (amp > winnerAmp) {
                winnerAmp = amp;
                winner = s;
            }
            s.elapsed++;
            if (s.elapsed >= s.duration) {
                it.remove();
            }
        }
        if (winner == null) {
            curYaw = 0f;
            curPitch = 0f;
            return;
        }
        double t = tickCount;
        // Smooth sway: two layered sines per axis, kept inside the old jitter's range.
        double swayYaw = (Math.sin(t * 1.9 + winner.phaseA) + Math.sin(t * 3.7 + winner.phaseB) * 0.5) / 1.5;
        double swayPitch = (Math.sin(t * 2.3 + winner.phaseB) + Math.sin(t * 4.1 + winner.phaseA) * 0.5) / 1.5;
        curYaw = (float) (swayYaw * winnerAmp * 0.6 + winner.kickYaw * winnerAmp * 0.4);
        curPitch = (float) (swayPitch * winnerAmp * 0.6 + winner.kickPitch * winnerAmp * 0.4);
    }

    public static float yaw(float partialTick) {
        return (prevYaw + (curYaw - prevYaw) * partialTick) * scale;
    }

    public static float pitch(float partialTick) {
        return (prevPitch + (curPitch - prevPitch) * partialTick) * scale;
    }
}
