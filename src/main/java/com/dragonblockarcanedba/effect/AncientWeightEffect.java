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
 * Ancient Weight — Unbreakable hyperarmor & grounding while channeling the Z-Sword.
 * 
 * Bundles:
 * - Knockback Resistance (+1.0 / immune to knockback)
 * - Armor (+10.0)
 * - Armor Toughness (+6.0)
 * - Movement Speed (-60% heavy weapon drag)
 * - Ancient golden/bronze ground runes.
 */
public class AncientWeightEffect extends MobEffect {
    public AncientWeightEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xDAA520); // Goldenrod

        this.addAttributeModifier(
            Attributes.KNOCKBACK_RESISTANCE,
            DragonBlockArcaneDBA.id("effect.ancient_weight.knockback"),
            1.0,
            AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
            Attributes.ARMOR,
            DragonBlockArcaneDBA.id("effect.ancient_weight.armor"),
            10.0,
            AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
            Attributes.ARMOR_TOUGHNESS,
            DragonBlockArcaneDBA.id("effect.ancient_weight.toughness"),
            6.0,
            AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.ancient_weight.speed"),
            -0.60,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.tickCount % 3 == 0) {
            double px = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double py = entity.getY() + 0.1;
            double pz = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

            level.sendParticles(
                new DustParticleOptions(0xB8860B, 1.2f), // Dark Goldenrod
                px, py, pz,
                1, 0.0, 0.01, 0.0, 0.01
            );
        }
        return true;
    }
}
