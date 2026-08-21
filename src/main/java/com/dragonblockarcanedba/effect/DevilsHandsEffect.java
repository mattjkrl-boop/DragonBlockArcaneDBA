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
 * Custom "Devil's Hands" status effect — Extremely oppressive late-game effect.
 * 
 * Repeatedly applies chaotic debuffs:
 * - Movement Speed & Attack Speed suppression
 * - Chaotic stuttering paralysis & dark ash
 * - Dark Red Fire (deals percentage-based damage)
 */
public class DevilsHandsEffect extends MobEffect {
    public DevilsHandsEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000); // Dark Red

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.devils_hands.speed"),
            -0.60,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        this.addAttributeModifier(
            Attributes.ATTACK_SPEED,
            DragonBlockArcaneDBA.id("effect.devils_hands.attack_speed"),
            -0.50,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Chaotic random freeze triggers
        float chanceToTrigger = 0.08f + (amplifier * 0.03f);
        if (level.getRandom().nextFloat() < chanceToTrigger) {
            Vec3 vel = entity.getDeltaMovement();
            entity.setDeltaMovement(vel.x * 0.1, vel.y < 0 ? vel.y : 0.0, vel.z * 0.1);
            entity.hurtMarked = true;
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
