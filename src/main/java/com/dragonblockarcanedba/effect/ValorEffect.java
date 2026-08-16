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
 * Valor — Holy combat empowerment granted by the Grand Sword's Valor Field.
 * 
 * Provides combat empowerment natively via AttributeModifiers:
 * - Attack Damage (+20% per level)
 * - Attack Speed (+20% per level)
 * - Armor (+4.0 per level)
 * - Armor Toughness (+2.0 per level)
 * 
 * Does NOT clutter the HUD with vanilla Strength, Resistance, or Haste effects.
 */
public class ValorEffect extends MobEffect {
    public ValorEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700);

        // Attack Damage boost (+20% per level)
        this.addAttributeModifier(
            Attributes.ATTACK_DAMAGE,
            DragonBlockArcaneDBA.id("effect.valor.damage"),
            0.20,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        // Attack Speed boost (+20% per level)
        this.addAttributeModifier(
            Attributes.ATTACK_SPEED,
            DragonBlockArcaneDBA.id("effect.valor.speed"),
            0.20,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        // Armor defense (+4.0 per level)
        this.addAttributeModifier(
            Attributes.ARMOR,
            DragonBlockArcaneDBA.id("effect.valor.armor"),
            4.0,
            AttributeModifier.Operation.ADD_VALUE
        );

        // Armor Toughness (+2.0 per level)
        this.addAttributeModifier(
            Attributes.ARMOR_TOUGHNESS,
            DragonBlockArcaneDBA.id("effect.valor.toughness"),
            2.0,
            AttributeModifier.Operation.ADD_VALUE
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Radiant golden particles
        if (entity.tickCount % 3 == 0) {
            double px = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double py = entity.getY() + level.getRandom().nextDouble() * entity.getBbHeight();
            double pz = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

            level.sendParticles(
                new DustParticleOptions(0xFFD700, 1.4f),
                px, py, pz,
                1, 0.0, 0.04, 0.0, 0.01
            );
        }

        return true;
    }
}

