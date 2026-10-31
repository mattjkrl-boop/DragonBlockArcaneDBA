package com.dragonblockarcanedba.fx;

import com.dragonblockarcanedba.network.ScreenShakePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Server side helper: sends camera shake and hitstop to players near a heavy impact, with distance falloff. */
public final class DbaImpactFx {
    private DbaImpactFx() {}

    public static final double RADIUS = 28.0;

    public static void heavy(Level level, double x, double y, double z) {
        shake(level, x, y, z, 2.2f, 12, 3);
        burst(level, x, y, z);
        level.playSound(null, x, y, z, com.dragonblockarcanedba.sound.DbaSounds.HEAVY_IMPACT, net.minecraft.sounds.SoundSource.PLAYERS, 1.6f, 0.9f + level.getRandom().nextFloat() * 0.2f);
    }

    /** Spark and wind burst at an impact point, sent as two particle packets. */
    public static void burst(Level level, double x, double y, double z) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
        if (density <= 0f) return;
        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK, x, y + 0.5, z, Math.round(40 * density), 0.6, 0.4, 0.6, 0.6);
        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.WIND, x, y + 0.3, z, Math.round(16 * density), 1.2, 0.1, 1.2, 0.15);
    }

    public static void medium(Level level, double x, double y, double z) {
        shake(level, x, y, z, 1.0f, 8, 2);
    }

    public static void shake(Level level, double x, double y, double z, float intensity, int duration, int hitstop) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        for (ServerPlayer p : serverLevel.players()) {
            double d = Math.sqrt(p.distanceToSqr(x, y, z));
            if (d > RADIUS) continue;
            float falloff = (float) (1.0 - d / RADIUS);
            float scaled = intensity * falloff * falloff;
            if (scaled < 0.05f) continue;
            ServerPlayNetworking.send(p, new ScreenShakePayload(scaled, duration, d < 8 ? hitstop : 0));
        }
    }
}
