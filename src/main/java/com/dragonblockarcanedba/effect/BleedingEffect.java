package com.dragonblockarcanedba.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class BleedingEffect extends MobEffect {
    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000); // Dark red color
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true; // Tick every tick so we can spawn dripping particles
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        int interval = Math.max(1, 40 >> amplifier);

        // Visual dripping effect (every 4 ticks to avoid particle spam)
        if (entity.tickCount % 4 == 0) {
            level.sendParticles(
                new net.minecraft.core.particles.DustParticleOptions(0x8B0000, 1.2F),
                entity.getX() + (level.getRandom().nextFloat() - 0.5) * entity.getBbWidth(), 
                entity.getY() + level.getRandom().nextFloat() * entity.getBbHeight(), 
                entity.getZ() + (level.getRandom().nextFloat() - 0.5) * entity.getBbWidth(),
                1, // count
                0.0, -0.05, 0.0, // delta (simulate dripping down)
                0.0 // speed
            );
        }

        // Apply damage based on interval
        if (entity.tickCount % interval == 0) {
            // Magic damage natively bypasses standard armor
            entity.hurtServer(level, level.damageSources().magic(), 1.0F); // 1.0F = half a heart
        }
        return true;
    }
}
