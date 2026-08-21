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
 * Energy Overcharge — Stabilizer stance while overcharging Blaster Gun.
 * 
 * - Movement Speed (-50% lock-on drag)
 * - Knockback Resistance (+0.8)
 * - Plasma electric sparks.
 */
public class EnergyOverchargeEffect extends MobEffect {
    public EnergyOverchargeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00FF7F); // Spring Green

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.energy_overcharge.speed"),
            -0.50,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        this.addAttributeModifier(
            Attributes.KNOCKBACK_RESISTANCE,
            DragonBlockArcaneDBA.id("effect.energy_overcharge.knockback"),
            0.8,
            AttributeModifier.Operation.ADD_VALUE
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.tickCount % 2 == 0) {
            double px = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double py = entity.getY() + level.getRandom().nextDouble() * entity.getBbHeight();
            double pz = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

            level.sendParticles(
                new DustParticleOptions(0x00FF7F, 1.2f),
                px, py, pz,
                1, 0.0, 0.02, 0.0, 0.01
            );
        }
        return true;
    }
}
