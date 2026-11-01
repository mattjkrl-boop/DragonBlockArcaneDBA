package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaParticles;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import java.util.Random;

/**
 * Transformed players breathe with a ki aura. The old version spawned two vanilla
 * glow motes at a flat rate forever. Now the emission runs on a pulse wave (each
 * player phase-shifted so a crowd does not blink in sync): the aura swells and
 * settles, rising faster at the crest. While the player charges (holding use) the
 * pulse becomes a surge: more motes, tighter column, faster rise, reading as power
 * being pulled in before a release. Uses the mod's own KI_MOTE particle (soft
 * gold-white, additive) and respects the effects density config.
 */
public class AuraRenderer {
    private static final Random RANDOM = new Random();

    public static void renderAura(AbstractClientPlayer player) {
        if (!(player instanceof PlayerStatsAccessor accessor)) return;
        Identifier formId = accessor.dba$getActiveFormId();
        if (formId == null) return;
        float density = DbaEffectsConfig.effectDensity();
        if (density <= 0f) return;

        boolean charging = player.isUsingItem();
        double phase = player.getUUID().hashCode() & 0xFF;
        double pulse = 0.5 + 0.5 * Math.sin(player.tickCount * 0.32 + phase);
        int count;
        double spread, rise;
        if (charging) {
            count = 3 + (pulse > 0.5 ? 2 : 0);
            spread = 0.8;
            rise = 0.10 + pulse * 0.08;
        } else {
            count = 1 + (pulse > 0.62 ? 2 : 0) + (pulse > 0.9 ? 1 : 0);
            spread = 1.2;
            rise = 0.04 + pulse * 0.05;
        }
        count = Math.max(1, Math.round(count * density));
        for (int i = 0; i < count; i++) {
            double x = player.getX() + (RANDOM.nextDouble() - 0.5) * spread;
            double y = player.getY() + RANDOM.nextDouble() * 2.0;
            double z = player.getZ() + (RANDOM.nextDouble() - 0.5) * spread;
            player.level().addParticle(DbaParticles.KI_MOTE, x, y, z, 0.0, rise, 0.0);
        }
    }
}
