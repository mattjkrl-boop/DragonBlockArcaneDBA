package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Fissure Stun — Volcanic tremor shockwave debuff from Ox King's Axe fissures.
 * 
 * - Movement Speed (-75%)
 * - Attack Speed (-40%)
 * - Crushing tremor particles and volcanic smoke.
 */
public class FissureStunEffect extends MobEffect {
    public FissureStunEffect() {
        super(MobEffectCategory.HARMFUL, 0x800000); // Maroon

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.fissure_stun.speed"),
            -0.75,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        this.addAttributeModifier(
            Attributes.ATTACK_SPEED,
            DragonBlockArcaneDBA.id("effect.fissure_stun.attack_speed"),
            -0.40,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.tickCount % 3 == 0) {
            level.sendParticles(
                ParticleTypes.SMOKE,
                entity.getX(), entity.getY() + 0.2, entity.getZ(),
                2, entity.getBbWidth() * 0.3, 0.1, entity.getBbWidth() * 0.3, 0.02
            );
        }
        return true;
    }
}
