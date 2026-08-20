package com.dragonblockarcanedba.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Marked by Evil — Status effect applied by the Evil Spear.
 * Renders dark crimson marking particles around the target and amplifies subsequent Evil Spear attacks.
 */
public class MarkedByEvilEffect extends MobEffect {
    public MarkedByEvilEffect() {
        super(MobEffectCategory.HARMFUL, 0x880000); // Crimson dark red
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return tickCount % 5 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Crimson mark particles orbiting entity
        double angle = (entity.tickCount * 0.2);
        double radius = entity.getBbWidth() * 0.8 + 0.3;
        double px = entity.getX() + Math.cos(angle) * radius;
        double pz = entity.getZ() + Math.sin(angle) * radius;
        double py = entity.getY() + entity.getBbHeight() * 0.5;

        level.sendParticles(
            new DustParticleOptions(0xFF0033, 1.8f),
            px, py, pz,
            1, 0, 0.05, 0, 0.01
        );
        return true;
    }
}
