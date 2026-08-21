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
 * Temporal Stasis — Complete temporal freeze inflicted by Whis Staff.
 * 
 * Freezes the victim in local time:
 * - Movement Speed (-100%)
 * - Attack Speed (-100%)
 * - Velocity frozen in place
 * - Celestial time distortion particles.
 */
public class TemporalStasisEffect extends MobEffect {
    public TemporalStasisEffect() {
        super(MobEffectCategory.HARMFUL, 0x1E90FF); // Dodger Blue

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.temporal_stasis.speed"),
            -1.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        this.addAttributeModifier(
            Attributes.ATTACK_SPEED,
            DragonBlockArcaneDBA.id("effect.temporal_stasis.attack_speed"),
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
        // Zero out horizontal velocity to ensure absolute freeze
        Vec3 vel = entity.getDeltaMovement();
        entity.setDeltaMovement(0.0, vel.y < 0 ? vel.y : 0.0, 0.0);
        entity.hurtMarked = true;

        // Time-distortion blue clock particles
        if (entity.tickCount % 2 == 0) {
            double angle = (entity.tickCount * 25) % 360;
            double rad = Math.toRadians(angle);
            double r = entity.getBbWidth() * 0.8 + 0.2;
            double px = entity.getX() + Math.cos(rad) * r;
            double py = entity.getY() + entity.getBbHeight() * 0.5;
            double pz = entity.getZ() + Math.sin(rad) * r;

            level.sendParticles(
                new DustParticleOptions(0x00BFFF, 1.2f),
                px, py, pz,
                1, 0.0, 0.02, 0.0, 0.01
            );
        }

        return true;
    }
}
