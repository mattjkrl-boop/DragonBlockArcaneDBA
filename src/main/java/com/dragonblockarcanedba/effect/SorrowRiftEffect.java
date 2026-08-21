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
 * Sorrow Rift — Weeping dimensional tear affliction from Sickle of Sorrow.
 * 
 * - Movement Speed (-50%)
 * - Attack Speed (-30%)
 * - Periodic void bleeding damage
 * - Purple tears and shadowy mist.
 */
public class SorrowRiftEffect extends MobEffect {
    public SorrowRiftEffect() {
        super(MobEffectCategory.HARMFUL, 0x4B0082); // Indigo

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.sorrow_rift.speed"),
            -0.50,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        this.addAttributeModifier(
            Attributes.ATTACK_SPEED,
            DragonBlockArcaneDBA.id("effect.sorrow_rift.attack_speed"),
            -0.30,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Periodic tear damage every 20 ticks
        if (entity.tickCount % 20 == 0) {
            float damage = 10.0f + (amplifier * 5.0f);
            entity.hurtServer(level, level.damageSources().magic(), damage);
        }

        // Purple tears & portal embers
        if (entity.tickCount % 2 == 0) {
            double px = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double py = entity.getY() + level.getRandom().nextDouble() * entity.getBbHeight();
            double pz = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

            level.sendParticles(
                new DustParticleOptions(0x8A2BE2, 1.2f),
                px, py, pz,
                1, 0.0, -0.02, 0.0, 0.01
            );
            level.sendParticles(
                ParticleTypes.PORTAL,
                px, py, pz,
                1, 0.0, 0.0, 0.0, 0.05
            );
        }

        return true;
    }
}
