package com.dragonblockarcanedba.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Storm of Darkness — Status effect applied to enemies caught inside Curse Blade's Abyssal Eclipse.
 * 
 * - Causes Darkness and Nausea / Confusion.
 * - Progressively slows movement.
 * - Creates dark abyssal mist and wind ash around afflicted targets.
 */
public class StormOfDarknessEffect extends MobEffect {
    public StormOfDarknessEffect() {
        super(MobEffectCategory.HARMFUL, 0x0A0A14); // Abyssal black
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Apply Darkness and Nausea continuously
        entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0, false, false));

        // Progressive movement slowing
        Vec3 vel = entity.getDeltaMovement();
        float drag = Math.max(0.4f, 0.85f - (amplifier * 0.1f));
        entity.setDeltaMovement(vel.x * drag, vel.y > 0 ? vel.y * drag : vel.y, vel.z * drag);
        entity.hurtMarked = true;

        // Visual storm particles
        if (entity.tickCount % 3 == 0) {
            level.sendParticles(
                ParticleTypes.SMOKE,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                3, entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.4, 0.05
            );
            level.sendParticles(
                new DustParticleOptions(0x8B0000, 1.2F), // Dark crimson
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                2, 0.3, 0.3, 0.3, 0.02
            );
            level.sendParticles(
                new DustParticleOptions(0x0A0A0A, 1.5F), // Black ash
                entity.getX(), entity.getY() + entity.getBbHeight(), entity.getZ(),
                1, 0.4, 0.4, 0.4, 0.01
            );
            if (level.getRandom().nextFloat() < 0.2f) {
                level.sendParticles(
                    new DustParticleOptions(0xFFFF00, 1.0F), // Yellow static
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                    1, 0.5, 0.5, 0.5, 0.01
                );
            }
        }

        return true;
    }
}
