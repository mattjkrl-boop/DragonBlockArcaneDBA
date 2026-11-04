package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.particle.DbaMaterialOptions;
import com.dragonblockarcanedba.particle.DbaParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Silent Mark — Status effect applied by the Katana during Flashdraw dash.
 * Temporarily freezes/delays damage until the dash finishes, displaying silver slashes.
 */
public class SilentMarkEffect extends MobEffect {
    public SilentMarkEffect() {
        super(MobEffectCategory.HARMFUL, 0xE0E0FF); // Silver/White
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return tickCount % 2 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Silver slash particle sparks
        double ox = (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth() * 1.2;
        double oy = level.getRandom().nextDouble() * entity.getBbHeight();
        double oz = (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth() * 1.2;

        level.sendParticles(
            DbaMaterialOptions.of(DbaMaterialOptions.SOUL, 0xFFFFFF, 1.5f, entity.getId() * 31 + entity.tickCount),
            entity.getX() + ox, entity.getY() + oy, entity.getZ() + oz,
            1, 0, 0.02, 0, 0.01
        );
        // Complex layer: soul and valor WISP_CURL at the same tick point and rate as the silver soul emission.
        level.sendParticles(
            DbaParticles.WISP_CURL,
            entity.getX() + ox, entity.getY() + oy, entity.getZ() + oz,
            1, 0, 0.02, 0, 0.01
        );
        return true;
    }
}
