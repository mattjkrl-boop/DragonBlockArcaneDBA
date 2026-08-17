package com.dragonblockarcanedba.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Cinematic Tracking Effect — Invisible internal effect used by DelayedDamageMixin
 * to hold and synchronize delayed damage combos until ability animations finish.
 * 
 * Has no particles, no icon, and no gameplay attribute changes.
 */
public class CinematicTrackingEffect extends MobEffect {
    public CinematicTrackingEffect() {
        super(MobEffectCategory.NEUTRAL, 0x000000);
    }
}
