package com.dragonblockarcanedba.dimension;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class PlanetDimension {
    private final String planetName;
    private final double gravityFactor; // 1.0 is Earth standard gravity
    private final boolean lowOxygen;
    private final int oxygenTickRate;

    public PlanetDimension(String name, double gravity, boolean lowOxygen, int tickRate) {
        this.planetName = name;
        this.gravityFactor = gravity;
        this.lowOxygen = lowOxygen;
        this.oxygenTickRate = tickRate;
    }

    public String getPlanetName() {
        return planetName;
    }

    public double getGravityFactor() {
        return gravityFactor;
    }

    public boolean hasLowOxygen() {
        return lowOxygen;
    }

    public void tickPlanetEffects(LivingEntity entity) {
        // Dynamic Gravity Simulation
        if (gravityFactor != 1.0 && !entity.onGround() && !entity.onClimbable()) {
            // standard gravity constant is ~0.08 blocks/tick
            double gravityAdjustment = (gravityFactor - 1.0) * 0.08;
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, -gravityAdjustment, 0));
        }

        // Atmosphere / Oxygen depletion damage
        if (lowOxygen && entity instanceof Player player) {
            if (player.tickCount % oxygenTickRate == 0 && !player.isCreative() && !player.isSpectator()) {
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, false));
            }
        }
    }
}
