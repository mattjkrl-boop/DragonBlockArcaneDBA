package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Rifted — Spatial Energy Surge from entering a Void Rift.
 * 
 * Bundles:
 * - Speed IV (+40% Movement Speed)
 * - Strength IV (+15 Attack Damage)
 * - Jump Boost III
 * - Slow Falling
 */
public class RiftedEffect extends MobEffect {
    public RiftedEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8A2BE2);

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.rifted.speed"),
            0.40,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        this.addAttributeModifier(
            Attributes.ATTACK_DAMAGE,
            DragonBlockArcaneDBA.id("effect.rifted.damage"),
            15.0,
            AttributeModifier.Operation.ADD_VALUE
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Apply Jump Boost and Slow Falling
        if (!entity.hasEffect(MobEffects.JUMP_BOOST)) {
            entity.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 40, 2, false, false));
        }
        if (!entity.hasEffect(MobEffects.SLOW_FALLING)) {
            entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
        }

        // Purple rift sparks
        if (entity.tickCount % 2 == 0) {
            double px = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double py = entity.getY() + level.getRandom().nextDouble() * entity.getBbHeight();
            double pz = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

            level.sendParticles(
                new DustParticleOptions(0x9400D3, 1.3F),
                px, py, pz,
                1, 0.0, 0.04, 0.0, 0.01
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
