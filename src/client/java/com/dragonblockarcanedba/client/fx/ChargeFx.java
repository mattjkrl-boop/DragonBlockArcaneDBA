package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-space charge effects so a charging player reads on every screen, not just their
 * own (companion to the DbaPostFx ki surge shader, which is local only). While any player
 * in the level is using an item, ki motes spiral inward around them, tightening as the
 * charge builds; when a charge of 10+ ticks ends, a ring of sparks bursts outward.
 * Purely client side: it reads only synced entity state and spawns client particles.
 * Spawn rates scale with the effects density config, and density 0 disables it.
 */
public final class ChargeFx {
    private ChargeFx() {}

    private static final Map<UUID, Integer> USE_TICKS = new HashMap<>();

    public static void tick(Minecraft mc) {
        if (mc.level == null) {
            USE_TICKS.clear();
            return;
        }
        float density = DbaEffectsConfig.effectDensity();
        for (Player player : mc.level.players()) {
            UUID id = player.getUUID();
            if (player.isUsingItem()) {
                int ticks = USE_TICKS.getOrDefault(id, 0) + 1;
                USE_TICKS.put(id, ticks);
                if (density > 0f) spawnMotes(mc, player, ticks, density);
            } else {
                Integer ticks = USE_TICKS.remove(id);
                if (ticks != null && ticks >= 10 && density > 0f) burst(mc, player, ticks, density);
            }
        }
    }

    private static void spawnMotes(Minecraft mc, Player player, int ticks, float density) {
        // Roughly 1 mote per tick at density 1, tightening spiral as the charge builds.
        if (player.getRandom().nextFloat() > 0.85f * density) return;
        double shrink = Math.max(0.35, 0.85 - ticks * 0.01);
        double angle = (ticks * 0.55) + player.getId() * 1.7;
        double x = player.getX() + Math.cos(angle) * shrink;
        double z = player.getZ() + Math.sin(angle) * shrink;
        double y = player.getY() + 0.25 + (ticks % 24) * 0.055;
        mc.level.addParticle(DbaParticles.KI_MOTE, x, y, z,
            (player.getX() - x) * 0.04, 0.035, (player.getZ() - z) * 0.04);
    }

    private static void burst(Minecraft mc, Player player, int ticks, float density) {
        int count = Math.min(26, (int) ((8 + ticks / 3) * density));
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / Math.max(1, count);
            double x = player.getX() + Math.cos(angle) * 0.4;
            double z = player.getZ() + Math.sin(angle) * 0.4;
            double y = player.getY() + 0.9;
            mc.level.addParticle(DbaParticles.SPARK, x, y, z,
                Math.cos(angle) * 0.22, 0.05, Math.sin(angle) * 0.22);
        }
    }
}
