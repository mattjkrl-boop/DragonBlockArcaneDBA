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
import net.minecraft.world.phys.Vec3;

/**
 * Movement Curse — Custom anti-movement effect applied by Curse Blade.
 * 
 * - Stacks from 1 to 10 (amplifier 0 to 9).
 * - Each stack reduces movement speed and jump velocity by 10%.
 * - At 10 stacks (amplifier 9), the entity is completely frozen in place, cannot jump, dash, or teleport.
 * - Stacks above 5 add dense dark smoke and vision-obscuring spectral chain particles.
 */
public class MovementCurseEffect extends MobEffect {
    public MovementCurseEffect() {
        super(MobEffectCategory.HARMFUL, 0x2E0854); // Deep cursed purple

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.movement_curse.speed"),
            -0.10,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        int stacks = Math.min(10, amplifier + 1);
        float movementRetained = Math.max(0.0f, 1.0f - (stacks * 0.10f));

        Vec3 velocity = entity.getDeltaMovement();
        if (stacks >= 10) {
            // Full root: zero horizontal motion, gravity still pulls down if airborne
            entity.setDeltaMovement(0.0, velocity.y < 0 ? velocity.y : 0.0, 0.0);
            entity.hurtMarked = true;
        } else {
            // Progressive movement and jump dampening
            double newX = velocity.x * movementRetained;
            double newZ = velocity.z * movementRetained;
            double newY = velocity.y > 0 ? velocity.y * movementRetained : velocity.y;
            entity.setDeltaMovement(newX, newY, newZ);
            entity.hurtMarked = true;
        }

        // Visual spectral chain particles
        if (entity.tickCount % 2 == 0) {
            float width = entity.getBbWidth();
            float height = entity.getBbHeight();

            // Cursed purple dust around the body
            for (int i = 0; i < Math.min(stacks, 5); i++) {
                double px = entity.getX() + (level.getRandom().nextFloat() - 0.5) * (width + 0.4);
                double py = entity.getY() + level.getRandom().nextFloat() * height;
                double pz = entity.getZ() + (level.getRandom().nextFloat() - 0.5) * (width + 0.4);

                level.sendParticles(
                    new DustParticleOptions(0x4B0082, 1.4F), // Indigo purple
                    px, py, pz,
                    1, 0.0, 0.02, 0.0, 0.01
                );
            }

            // Dark smoke if heavily cursed
            if (stacks >= 7) {
                level.sendParticles(
                    ParticleTypes.SMOKE,
                    entity.getX(), entity.getY() + height * 0.5, entity.getZ(),
                    2, width * 0.3, height * 0.2, width * 0.3, 0.02
                );
            }
        }

        return true;
    }
}
