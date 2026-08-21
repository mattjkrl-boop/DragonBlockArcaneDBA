package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Dark Faded — Void Corruption & Blindness from Hollow's Edge.
 * 
 * - Reduces speed by 35%.
 * - Inflicts Darkness.
 * - Deals periodic Void/Magic damage and spawns pitch black particles.
 */
public class DarkFadedEffect extends MobEffect {
    public DarkFadedEffect() {
        super(MobEffectCategory.HARMFUL, 0x0A0014);

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.dark_faded.speed"),
            -0.35,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        this.addAttributeModifier(
            Attributes.ATTACK_DAMAGE,
            DragonBlockArcaneDBA.id("effect.dark_faded.damage"),
            -0.25,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Dense black particles
        if (entity.tickCount % 2 == 0) {
            for (int i = 0; i < 2; i++) {
                double px = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                double py = entity.getY() + level.getRandom().nextDouble() * entity.getBbHeight();
                double pz = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

                level.sendParticles(
                    new DustParticleOptions(0x05000A, 1.4F),
                    px, py, pz,
                    1, 0.0, 0.05, 0.0, 0.01
                );
                level.sendParticles(
                    ParticleTypes.SQUID_INK,
                    px, py, pz,
                    1, 0.0, 0.02, 0.0, 0.02
                );
            }
        }

        // Void tick damage every 10 ticks
        if (entity.tickCount % 10 == 0) {
            float damage = 35.0f + (amplifier * 15.0f);
            entity.hurtServer(level, level.damageSources().magic(), damage);
        }
        return true;
    }
}
