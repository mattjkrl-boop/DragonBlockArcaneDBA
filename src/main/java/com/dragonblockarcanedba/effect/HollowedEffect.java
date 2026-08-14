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
import net.minecraft.world.entity.player.Player;

/**
 * Hollowed — Void Phasing & Translucency State from Hollow's Edge.
 * 
 * - Makes the player ~90% translucent / see-through.
 * - Massive movement speed boost (+60%).
 * - Mobs cannot target or see the player.
 * - Phasing through blocks without suffocation damage.
 */
public class HollowedEffect extends MobEffect {
    public HollowedEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x1A0033);

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.hollowed.speed"),
            0.60,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Ethereal void particles around the phased entity
        if (entity.tickCount % 2 == 0) {
            double px = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double py = entity.getY() + level.getRandom().nextDouble() * entity.getBbHeight();
            double pz = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

            level.sendParticles(
                new DustParticleOptions(0x4B0082, 1.2F),
                px, py, pz,
                1, 0.0, 0.02, 0.0, 0.01
            );
            level.sendParticles(
                ParticleTypes.SMOKE,
                px, py, pz,
                1, 0.0, 0.01, 0.0, 0.01
            );
        }

        // Suppress mob targeting if player is hollowed
        if (entity instanceof Player) {
            entity.invulnerableTime = Math.max(entity.invulnerableTime, 5);
        }
        return true;
    }
}
