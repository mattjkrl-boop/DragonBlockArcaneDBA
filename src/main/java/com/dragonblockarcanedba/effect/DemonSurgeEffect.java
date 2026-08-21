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
 * Demon Surge — Demonic rush frenzy from Evil Spear.
 * 
 * - Movement Speed (+45%)
 * - Attack Speed (+50%)
 * - Attack Damage (+6.0)
 * - Jump Boost / Strength (+0.6)
 * - Safe Fall Distance (+8 blocks)
 * - Blood crimson shadow particles.
 */
public class DemonSurgeEffect extends MobEffect {
    public DemonSurgeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xDC143C); // Crimson

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.demon_surge.speed"),
            0.45,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        this.addAttributeModifier(
            Attributes.ATTACK_SPEED,
            DragonBlockArcaneDBA.id("effect.demon_surge.attack_speed"),
            0.50,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        this.addAttributeModifier(
            Attributes.ATTACK_DAMAGE,
            DragonBlockArcaneDBA.id("effect.demon_surge.damage"),
            6.0,
            AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
            Attributes.JUMP_STRENGTH,
            DragonBlockArcaneDBA.id("effect.demon_surge.jump"),
            0.60,
            AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
            Attributes.SAFE_FALL_DISTANCE,
            DragonBlockArcaneDBA.id("effect.demon_surge.fall_distance"),
            8.0,
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
                new DustParticleOptions(0xDC143C, 1.3f), // Crimson
                px, py, pz,
                1, 0.0, 0.04, 0.0, 0.01
            );
        }
        return true;
    }
}
