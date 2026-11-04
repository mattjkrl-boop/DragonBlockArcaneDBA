package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.particle.DbaMaterialOptions;
import com.dragonblockarcanedba.particle.DbaParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Valor Stun — Holy blade suppression and pinning from Grand Sword shards & Valor Fields.
 * 
 * - Stacks from 1 to 4 (amplifier 0 to 3+).
 * - Each stack reduces movement speed by 25%.
 * - At max stacks (amplifier 3+), target is completely immobilized in holy light.
 * - Radiates radiant golden suppression runes.
 */
public class ValorStunEffect extends MobEffect {
    public ValorStunEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFD700); // Gold

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.valor_stun.speed"),
            -0.25,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (amplifier >= 3) {
            Vec3 vel = entity.getDeltaMovement();
            entity.setDeltaMovement(0.0, vel.y < 0 ? vel.y : 0.0, 0.0);
            com.dragonblockarcanedba.util.DbaCompat.markHurt(entity);
        }

        if (entity.tickCount % 2 == 0) {
            double px = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double py = entity.getY() + level.getRandom().nextDouble() * entity.getBbHeight();
            double pz = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

            level.sendParticles(
                DbaMaterialOptions.of(DbaMaterialOptions.PLASMA, 0xFFD700, 1.3f, entity.getId() * 31 + entity.tickCount),
                px, py, pz,
                1, 0.0, 0.03, 0.0, 0.01
            );
            // Complex layer: soul and valor WISP_CURL at the same tick point and rate as the golden plasma.
            level.sendParticles(
                DbaParticles.WISP_CURL,
                px, py, pz,
                1, 0.0, 0.03, 0.0, 0.01
            );
        }

        return true;
    }
}
