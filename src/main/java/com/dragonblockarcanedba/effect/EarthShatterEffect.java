package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.particles.BlockParticleOption;
import com.dragonblockarcanedba.particle.DbaMaterialOptions;
import com.dragonblockarcanedba.particle.DbaParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;

/**
 * Earth Shatter — Concussive ground shockwave debuff from the Z-Sword.
 * 
 * - Movement Speed (-70%)
 * - Attack Damage (-40%)
 * - Attack Speed (-30%)
 * - Cracking earth rubble and dust particles.
 */
public class EarthShatterEffect extends MobEffect {
    public EarthShatterEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B4513); // Saddle Brown

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.earth_shatter.speed"),
            -0.70,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        this.addAttributeModifier(
            Attributes.ATTACK_DAMAGE,
            DragonBlockArcaneDBA.id("effect.earth_shatter.damage"),
            -0.40,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        this.addAttributeModifier(
            Attributes.ATTACK_SPEED,
            DragonBlockArcaneDBA.id("effect.earth_shatter.attack_speed"),
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
        if (entity.tickCount % 3 == 0) {
            double px = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double py = entity.getY() + 0.2;
            double pz = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

            level.sendParticles(
                new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, Blocks.DIRT.defaultBlockState()),
                px, py, pz,
                1, 0.1, 0.1, 0.1, 0.05
            );
            level.sendParticles(
                DbaMaterialOptions.of(DbaMaterialOptions.DUST, 0x8B4513, 1.0f, entity.getId() * 31 + entity.tickCount),
                px, py, pz,
                1, 0.1, 0.1, 0.1, 0.05
            );
            // Complex layer: earth and stone SHARD_CHIP at the same tick point and rate as the rubble and dust.
            level.sendParticles(
                DbaParticles.SHARD_CHIP,
                px, py, pz,
                1, 0.1, 0.1, 0.1, 0.05
            );
        }
        return true;
    }
}
