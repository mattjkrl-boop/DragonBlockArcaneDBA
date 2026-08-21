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
 * Ox Brace — Immovable volcanic mountain poise while charging Ox King's Axe.
 * 
 * - Knockback Resistance (+1.0)
 * - Armor (+12.0)
 * - Armor Toughness (+8.0)
 * - Movement Speed (-50%)
 * - Fiery embers and volcanic steam.
 */
public class OxBraceEffect extends MobEffect {
    public OxBraceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF4500); // Orange Red

        this.addAttributeModifier(
            Attributes.KNOCKBACK_RESISTANCE,
            DragonBlockArcaneDBA.id("effect.ox_brace.knockback"),
            1.0,
            AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
            Attributes.ARMOR,
            DragonBlockArcaneDBA.id("effect.ox_brace.armor"),
            12.0,
            AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
            Attributes.ARMOR_TOUGHNESS,
            DragonBlockArcaneDBA.id("effect.ox_brace.toughness"),
            8.0,
            AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.ox_brace.speed"),
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
        if (entity.tickCount % 2 == 0) {
            double px = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double py = entity.getY() + 0.1;
            double pz = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

            level.sendParticles(
                new DustParticleOptions(0xFF4500, 1.2f),
                px, py, pz,
                1, 0.0, 0.02, 0.0, 0.01
            );
        }
        return true;
    }
}
