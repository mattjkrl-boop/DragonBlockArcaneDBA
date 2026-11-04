package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.particle.DbaMaterialOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Celestial Grace — Angelic empowerment from wielding the Whis Staff.
 * 
 * Bundles:
 * - Movement Speed (+50% per level)
 * - Jump Boost / Jump Strength (+0.6 per level)
 * - Damage Resistance (+6 Armor, +4 Armor Toughness per level)
 * - Safe Fall Distance (+10 blocks)
 * - Divine cyan aura particles.
 */
public class CelestialGraceEffect extends MobEffect {
    public CelestialGraceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00FFFF); // Cyan

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.celestial_grace.speed"),
            0.50,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        this.addAttributeModifier(
            Attributes.JUMP_STRENGTH,
            DragonBlockArcaneDBA.id("effect.celestial_grace.jump"),
            0.60,
            AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
            Attributes.ARMOR,
            DragonBlockArcaneDBA.id("effect.celestial_grace.armor"),
            6.0,
            AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
            Attributes.ARMOR_TOUGHNESS,
            DragonBlockArcaneDBA.id("effect.celestial_grace.toughness"),
            4.0,
            AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
            Attributes.SAFE_FALL_DISTANCE,
            DragonBlockArcaneDBA.id("effect.celestial_grace.fall_distance"),
            10.0,
            AttributeModifier.Operation.ADD_VALUE
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Divine angelic cyan/white particles
        if (entity.tickCount % 2 == 0) {
            double px = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double py = entity.getY() + level.getRandom().nextDouble() * entity.getBbHeight();
            double pz = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

            level.sendParticles(
                DbaMaterialOptions.of(DbaMaterialOptions.SOUL, 0x00FFFF, 1.3f, entity.getId() * 31 + entity.tickCount), // Cyan
                px, py, pz,
                1, 0.0, 0.04, 0.0, 0.01
            );
            level.sendParticles(
                DbaMaterialOptions.of(DbaMaterialOptions.VAPOR, 0xFFFFFF, 1.0f, entity.getId() * 31 + entity.tickCount + 1), // White shimmer
                px, py, pz,
                1, 0.0, 0.02, 0.0, 0.01
            );
        }

        return true;
    }
}
