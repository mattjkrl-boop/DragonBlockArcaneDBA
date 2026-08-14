package com.dragonblockarcanedba.effect;


import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;


/**
 * Custom "Devil's Hands" status effect — Extremely oppressive late-game effect.
 * 
 * Repeatedly applies chaotic debuffs:
 * - Blindness
 * - Complete immobilization (Slowness high amp)
 * - Dark Red Fire (deals high percentage-based damage)
 */
public class DevilsHandsEffect extends MobEffect {
    public DevilsHandsEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000); // Dark Red
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Chaotic random triggers
        // The higher the amplifier, the more frequently it triggers
        float chanceToTrigger = 0.05f + (amplifier * 0.02f);
        
        // Random stuttering Blindness and Freeze
        if (level.getRandom().nextFloat() < chanceToTrigger) {
            // Random duration between 1 to 3 seconds
            int duration = 20 + level.getRandom().nextInt(40);
            
            if (level.getRandom().nextBoolean()) {
                // Apply Blindness
                entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0, false, false));
            } else {
                // Apply Movement Freeze (Slowness VII basically stops them)
                entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 6, false, false));
            }
        }

        // Apply Dark Red Fire Damage (more frequently, every ~10 ticks on average)
        if (level.getRandom().nextFloat() < 0.1f + (amplifier * 0.05f)) {
            // Deal substantial damage: base 5.0 + 1% of max health per level
            float percentDamage = entity.getMaxHealth() * 0.01f * (amplifier + 1);
            float damage = Math.max(5.0f, percentDamage);
            entity.hurtServer(level, level.damageSources().onFire(), damage);
        }

        // Custom Dark Red Fire Particles
        if (entity.tickCount % 2 == 0) {
            float width = entity.getBbWidth();
            float height = entity.getBbHeight();
            
            // Dark red flames
            level.sendParticles(
                new DustParticleOptions(0x8B0000, 1.5F), // Dark Red
                entity.getX() + (level.getRandom().nextFloat() - 0.5) * width * 1.5,
                entity.getY() + level.getRandom().nextFloat() * height,
                entity.getZ() + (level.getRandom().nextFloat() - 0.5) * width * 1.5,
                3, // count
                0.02, 0.1, 0.02, // delta (upward drift)
                0.02 // speed
            );
            
            // Black ash/smoke
            level.sendParticles(
                new DustParticleOptions(0x111111, 1.0F), // Near Black
                entity.getX() + (level.getRandom().nextFloat() - 0.5) * width,
                entity.getY() + level.getRandom().nextFloat() * height,
                entity.getZ() + (level.getRandom().nextFloat() - 0.5) * width,
                1,
                0.05, 0.15, 0.05,
                0.01
            );
        }

        return true;
    }
}
