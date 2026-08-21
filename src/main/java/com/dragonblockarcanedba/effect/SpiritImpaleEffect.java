package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Spirit Impale — Piercing Ki Blade impalement from the Spirit Sword.
 * 
 * - Pins the victim levitating suspended in mid-air.
 * - Reduces Attack Damage (-30%).
 * - Reduces Movement Speed (-80%).
 * - Radiates piercing golden divine Ki particles.
 */
public class SpiritImpaleEffect extends MobEffect {
    public SpiritImpaleEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFDF00); // Golden Yellow

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.spirit_impale.speed"),
            -0.80,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        this.addAttributeModifier(
            Attributes.ATTACK_DAMAGE,
            DragonBlockArcaneDBA.id("effect.spirit_impale.damage"),
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
        // Suspend gently in mid-air
        Vec3 vel = entity.getDeltaMovement();
        double targetY = (entity.onGround() || vel.y < 0) ? 0.04 : 0.0;
        entity.setDeltaMovement(vel.x * 0.2, targetY, vel.z * 0.2);
        entity.hurtMarked = true;

        // Radiant golden ki piercing beams
        if (entity.tickCount % 2 == 0) {
            double px = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double py = entity.getY() + level.getRandom().nextDouble() * entity.getBbHeight();
            double pz = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

            level.sendParticles(
                new DustParticleOptions(0xFFD700, 1.4f),
                px, py, pz,
                1, 0.0, 0.05, 0.0, 0.01
            );
        }

        return true;
    }
}
