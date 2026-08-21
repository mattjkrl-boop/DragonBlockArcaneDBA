package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Judgement Lock — Dimensional slash stasis from Saber's Judgement Blitz.
 * 
 * Complete cinematic paralysis during multi-slash execution:
 * - Movement Speed (-100%)
 * - Attack Speed (-100%)
 * - Attack Damage (-100%)
 * - Frozen velocity
 * - Dimensional fracture sparks.
 */
public class JudgementLockEffect extends MobEffect {
    public JudgementLockEffect() {
        super(MobEffectCategory.HARMFUL, 0x00E5FF); // Electric Blue / Cyan

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.judgement_lock.speed"),
            -1.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        this.addAttributeModifier(
            Attributes.ATTACK_SPEED,
            DragonBlockArcaneDBA.id("effect.judgement_lock.attack_speed"),
            -1.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        this.addAttributeModifier(
            Attributes.ATTACK_DAMAGE,
            DragonBlockArcaneDBA.id("effect.judgement_lock.damage"),
            -1.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        Vec3 vel = entity.getDeltaMovement();
        entity.setDeltaMovement(0.0, vel.y < 0 ? vel.y : 0.0, 0.0);
        entity.hurtMarked = true;

        if (entity.tickCount % 2 == 0) {
            double px = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double py = entity.getY() + level.getRandom().nextDouble() * entity.getBbHeight();
            double pz = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

            level.sendParticles(
                new DustParticleOptions(0x00E5FF, 1.4f),
                px, py, pz,
                1, 0.0, 0.02, 0.0, 0.01
            );
        }

        return true;
    }
}
