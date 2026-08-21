package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Pole Stun — Heavy blunt concussion from Power Pole impact.
 * 
 * - Movement Speed (-80%)
 * - Attack Speed (-60%)
 * - Rotating golden dizzy stars around the head.
 */
public class PoleStunEffect extends MobEffect {
    public PoleStunEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4500); // Orange Red

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.pole_stun.speed"),
            -0.80,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        this.addAttributeModifier(
            Attributes.ATTACK_SPEED,
            DragonBlockArcaneDBA.id("effect.pole_stun.attack_speed"),
            -0.60,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Rotating dizzy stars above head
        if (entity.tickCount % 2 == 0) {
            double angle = (entity.tickCount * 20) % 360;
            double rad = Math.toRadians(angle);
            double r = entity.getBbWidth() * 0.6 + 0.2;
            double px = entity.getX() + Math.cos(rad) * r;
            double py = entity.getY() + entity.getBbHeight() + 0.3;
            double pz = entity.getZ() + Math.sin(rad) * r;

            level.sendParticles(
                new DustParticleOptions(0xFFD700, 1.3f), // Gold
                px, py, pz,
                1, 0.0, 0.01, 0.0, 0.01
            );
        }

        return true;
    }
}
