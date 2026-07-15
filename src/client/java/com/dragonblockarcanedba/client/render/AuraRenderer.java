package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import java.util.Random;

public class AuraRenderer {
    private static final Random RANDOM = new Random();

    public static void renderAura(AbstractClientPlayer player) {
        if (player instanceof PlayerStatsAccessor accessor) {
            Identifier formId = accessor.dba$getActiveFormId();
            if (formId != null) {
                // If transformed, spawn aura particles
                for (int i = 0; i < 2; i++) {
                    double x = player.getX() + (RANDOM.nextDouble() - 0.5) * 1.2;
                    double y = player.getY() + RANDOM.nextDouble() * 2.0;
                    double z = player.getZ() + (RANDOM.nextDouble() - 0.5) * 1.2;

                    // Spawn client-side particles
                    player.level().addParticle(
                        ParticleTypes.GLOW,
                        x, y, z,
                        0.0, 0.05, 0.0
                    );
                }
            }
        }
    }
}
