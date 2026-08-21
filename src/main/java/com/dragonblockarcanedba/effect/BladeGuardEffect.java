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
 * Blade Guard — Perfect parry and counter stance for Katana.
 * 
 * - Armor (+30.0)
 * - Armor Toughness (+15.0)
 * - Knockback Resistance (+1.0)
 * - Movement Speed (-40%)
 * - Gleaming white blade steel glints.
 */
public class BladeGuardEffect extends MobEffect {
    public BladeGuardEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF); // White

        this.addAttributeModifier(
            Attributes.ARMOR,
            DragonBlockArcaneDBA.id("effect.blade_guard.armor"),
            30.0,
            AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
            Attributes.ARMOR_TOUGHNESS,
            DragonBlockArcaneDBA.id("effect.blade_guard.toughness"),
            15.0,
            AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
            Attributes.KNOCKBACK_RESISTANCE,
            DragonBlockArcaneDBA.id("effect.blade_guard.knockback"),
            1.0,
            AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.blade_guard.speed"),
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
        if (entity.tickCount % 2 == 0) {
            double px = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double py = entity.getY() + level.getRandom().nextDouble() * entity.getBbHeight();
            double pz = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

            level.sendParticles(
                new DustParticleOptions(0xFFFFFF, 1.4f),
                px, py, pz,
                1, 0.0, 0.04, 0.0, 0.01
            );
        }
        return true;
    }
}
