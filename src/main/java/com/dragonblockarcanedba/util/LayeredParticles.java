package com.dragonblockarcanedba.util;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaDustOptions;
import com.dragonblockarcanedba.particle.DbaParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Server side layered particle emitters (SPECTACLE-STANDARD point 1).
 * One call emits the three layers every spectacle effect is built from:
 * a dense bright core, a mid layer in the theme color, and a wispy outer layer.
 * Counts scale with the effect envelope and the effects config density, so a
 * move breathes with its own lifecycle instead of emitting flat out then stopping.
 */
public final class LayeredParticles {
    private LayeredParticles() {}

    /** Theme dust on the mod's own THEME_DUST type (bespoke pass: no vanilla dust). */
    public static DbaDustOptions dust(int argb, float scale) {
        return new DbaDustOptions(argb & 0xFFFFFF, scale);
    }

    private static int scaled(float base, float envelope) {
        float density = DbaEffectsConfig.effectDensity();
        if (density <= 0f || envelope <= 0f) return 0;
        return Math.max(0, Math.round(base * density * envelope));
    }

    /**
     * One tick of a standing vortex or column effect: core motes rising through
     * the middle, a helical mid band in the theme color, outer wisps drifting.
     * Call every tick (or every second tick for very large radii) from the
     * owning entity's server tick.
     */
    public static void emitVortex(ServerLevel level, Vec3 center, float radius, float height,
                                  float age, float life, int themeArgb, ParticleOptions outerType) {
        float env = FxEnvelope.envelope(age, life);
        if (env <= 0f) return;
        RandomSource rand = level.getRandom();
        double cx = center.x, cy = center.y, cz = center.z;

        // Core: dense bright motes spiralling up the middle.
        int core = scaled(3f, env);
        for (int i = 0; i < core; i++) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double r = radius * 0.18 * rand.nextDouble();
            double y = cy + rand.nextDouble() * height;
            level.sendParticles(DbaParticles.KI_MOTE,
                cx + Math.cos(a) * r, y, cz + Math.sin(a) * r,
                1, 0.02, 0.06, 0.02, 0.01);
        }

        // Mid: theme colored dust on a helical band, the layer that reads at range.
        int mid = scaled(5f, env);
        DbaDustOptions midDust = dust(themeArgb, 1.4f);
        for (int i = 0; i < mid; i++) {
            double a = rand.nextDouble() * Math.PI * 2.0 + age * 0.15;
            double r = radius * (0.55 + 0.25 * rand.nextDouble());
            double y = cy + rand.nextDouble() * height;
            level.sendParticles(midDust,
                cx + Math.cos(a) * r, y, cz + Math.sin(a) * r,
                1, 0.03, 0.05, 0.03, 0.01);
        }

        // Outer: wisps at the rim, slow and sparse, so the edge dissolves.
        int outer = scaled(3f, env);
        for (int i = 0; i < outer; i++) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double r = radius * (0.9 + 0.3 * rand.nextDouble());
            double y = cy + rand.nextDouble() * height * 0.8;
            level.sendParticles(outerType,
                cx + Math.cos(a) * r, y, cz + Math.sin(a) * r,
                1, 0.05, 0.03, 0.05, 0.01);
        }
    }

    /** Base dust burst where a move meets the ground (point 3): one call on the cast or impact tick. */
    public static void emitGroundBurst(ServerLevel level, Vec3 center, float radius, int themeArgb) {
        float density = DbaEffectsConfig.effectDensity();
        if (density <= 0f) return;
        RandomSource rand = level.getRandom();
        int n = Math.max(2, Math.round(14f * density));
        DbaDustOptions dust = dust(themeArgb, 1.2f);
        for (int i = 0; i < n; i++) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double r = radius * rand.nextDouble();
            level.sendParticles(dust,
                center.x + Math.cos(a) * r, center.y + 0.15, center.z + Math.sin(a) * r,
                1, 0.25, 0.08, 0.25, 0.05);
            if (i % 3 == 0) {
                level.sendParticles(DbaParticles.DUST_PUFF,
                    center.x + Math.cos(a) * r, center.y + 0.3, center.z + Math.sin(a) * r,
                    1, 0.2, 0.1, 0.2, 0.03);
            }
        }
    }

    /** Dissipation drizzle (point 5): theme particles falling as the effect dies. Call during the decay window. */
    public static void emitRain(ServerLevel level, Vec3 center, float radius, float height,
                                float age, float life, ParticleOptions type) {
        float env = FxEnvelope.envelope(age, life);
        if (env <= 0f) return;
        // Rain reads strongest while the envelope is falling, so invert it.
        float fall = 1f - env;
        int n = scaled(4f, Math.max(0.15f, fall));
        RandomSource rand = level.getRandom();
        for (int i = 0; i < n; i++) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double r = radius * rand.nextDouble();
            level.sendParticles(type,
                center.x + Math.cos(a) * r, center.y + height * (0.4 + 0.6 * rand.nextDouble()), center.z + Math.sin(a) * r,
                1, 0.02, -0.12, 0.02, 0.01);
        }
    }
}
