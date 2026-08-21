package com.dragonblockarcanedba.util;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class MovementLimiterHelper {

    private MovementLimiterHelper() {}

    /**
     * Calculates the overall movement multiplier (0.0 to 1.0) for an entity,
     * taking into account all active custom CC / debuff status effects as well as
     * player speed limiter percentages.
     */
    public static double getMovementMultiplier(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return 0.0;
        }

        // Full immobilization CC checks
        if (entity.hasEffect(DbaEffects.TEMPORAL_STASIS_HOLDER) || entity.hasEffect(DbaEffects.JUDGEMENT_LOCK_HOLDER)) {
            return 0.0;
        }

        double mult = 1.0;

        // 1. Natural Movement Speed Attribute Scaling (Captures all custom effects, attribute modifiers, gear, buffs & debuffs)
        if (entity.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)) {
            double baseAttr = entity.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            double currAttr = entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            if (baseAttr > 0.0) {
                double attrRatio = currAttr / baseAttr;
                mult *= Math.max(0.0, attrRatio);
            }
        }

        // Spirit Impale (mid-air pin)
        if (entity.hasEffect(DbaEffects.SPIRIT_IMPALE_HOLDER)) {
            mult *= 0.15;
        }

        // Pole Stun (heavy wind concussion)
        if (entity.hasEffect(DbaEffects.POLE_STUN_HOLDER)) {
            mult *= 0.20;
        }

        // Petrification Curse (stone body)
        if (entity.hasEffect(DbaEffects.PETRIFICATION_CURSE_HOLDER)) {
            mult *= 0.20;
        }

        // Earth Shatter (stamina exhaustion / tremor)
        if (entity.hasEffect(DbaEffects.EARTH_SHATTER_HOLDER)) {
            mult *= 0.30;
        }

        // Devils Hands (demonic ground grasp)
        if (entity.hasEffect(DbaEffects.DEVILS_HANDS_HOLDER)) {
            mult *= 0.35;
        }

        // Ancient Weight (extreme hyper-gravity)
        if (entity.hasEffect(DbaEffects.ANCIENT_WEIGHT_HOLDER)) {
            mult *= 0.40;
        }

        // Sorrow Rift (void bleed & drag)
        if (entity.hasEffect(DbaEffects.SORROW_RIFT_HOLDER)) {
            mult *= 0.50;
        }

        // Storm of Darkness
        if (entity.hasEffect(DbaEffects.STORM_OF_DARKNESS_HOLDER)) {
            mult *= 0.55;
        }

        // Movement Curse (stacking chain drag)
        if (entity.hasEffect(DbaEffects.MOVEMENT_CURSE_HOLDER)) {
            MobEffectInstance eff = entity.getEffect(DbaEffects.MOVEMENT_CURSE_HOLDER);
            int amp = eff != null ? eff.getAmplifier() : 0;
            mult *= Math.max(0.10, 1.0 - (amp + 1) * 0.10);
        }

        // Player speed percentage slider limit (1-100%)
        if (entity instanceof Player player) {
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
            int speedPercent = accessor.dba$getSpeedPercent();
            if (speedPercent > 0 && speedPercent < 100) {
                mult *= (speedPercent / 100.0);
            }
        }

        return Math.max(0.0, Math.min(1.0, mult));
    }

    /**
     * Checks if an entity is effectively immobilized / rooted (multiplier < 0.05).
     */
    public static boolean isMovementImmobilized(LivingEntity entity) {
        return getMovementMultiplier(entity) < 0.05;
    }

    /**
     * Calculates and applies counter-wind aerodynamics for the Power Pole gale spin.
     * Cancels incoming momentum towards the pole wielder and applies an overwhelming
     * outward gale force that exceeds rushing velocity at close range.
     */
    public static void applyPowerPoleGaleForce(LivingEntity target, Vec3 polePos, double distance) {
        Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
        Vec3 toTarget = targetPos.subtract(polePos);
        if (toTarget.lengthSqr() < 0.001) {
            toTarget = new Vec3(0, 0, 1);
        }
        Vec3 toTargetDir = toTarget.normalize();
        Vec3 currentVel = target.getDeltaMovement();

        // Calculate incoming velocity heading directly towards the pole wielder
        double incomingSpeed = -currentVel.dot(toTargetDir); // > 0 if rushing towards pole

        Vec3 newVel = currentVel;

        // If target is rushing/flying towards the pole wielder:
        if (incomingSpeed > 0) {
            // Cancel out the entire inward forward momentum vector
            newVel = newVel.add(toTargetDir.scale(incomingSpeed));

            // Apply an overwhelming gale repulsion slightly faster than their incoming top force,
            // especially at the start/tip of the pole (0-6 blocks distance).
            double closeBonus = distance <= 4.0 ? 2.5 : (distance <= 8.0 ? 1.5 : 0.6);
            double counterForce = (incomingSpeed * 1.12) + closeBonus;

            newVel = newVel.add(toTargetDir.scale(counterForce)).add(0, 0.25, 0);
        } else {
            // Standard gale blast pushing outward
            double baseForce = Math.max(0.6, 2.2 / Math.max(1.0, distance / 4.0));
            newVel = newVel.add(toTargetDir.scale(baseForce)).add(0, 0.2, 0);
        }

        target.setDeltaMovement(newVel);
        target.hurtMarked = true;

        // Apply wind stagger / Pole Stun
        target.addEffect(new MobEffectInstance(DbaEffects.POLE_STUN_HOLDER, 35, 0, false, false));
    }
}
