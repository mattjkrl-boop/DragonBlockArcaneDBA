package com.dragonblockarcanedba.ability;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.util.DbaDamage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The one damage path for every ability: amount from the definition
 * (base plus stat scaling), scaled by cast power and by the
 * definition's falloff curve, capped by the shared effects config,
 * and applied through the mod's own damage types. Carriers never
 * compute damage themselves; they report a hit (target, distance
 * fraction) and this class does the rest.
 */
public final class AbilityDamage {
    private AbilityDamage() {}

    public static float falloffFactor(AbilityDef def, double distFrac) {
        AbilityDef.DamageSpec dmg = def.damage();
        if (!"linear".equals(dmg.falloff())) {
            return 1.0f;
        }
        double t = Math.max(0.0, Math.min(1.0, distFrac));
        return (float) (1.0 - (1.0 - dmg.falloffFloor()) * t);
    }

    public static float amount(AbilityDef def, Entity caster, float power, double distFrac) {
        return amount(def, caster, power, distFrac, 1.0);
    }

    /** As amount(), with an extra multiplicative scale (cloud finales and bursts). */
    public static float amount(AbilityDef def, Entity caster, float power, double distFrac, double scale) {
        AbilityDef.DamageSpec dmg = def.damage();
        double stat = statOf(caster, dmg.stat());
        double raw = (dmg.base() + stat * dmg.perStat()) * power * scale;
        return DbaEffectsConfig.capDamage((float) (raw * falloffFactor(def, distFrac)));
    }

    /** The caster's value for one named stat, or 0 for non-players and "none". */
    public static double statOf(Entity caster, String statName) {
        if (!(caster instanceof Player player)) {
            return 0;
        }
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        return switch (statName) {
            case "strength" -> accessor.dba$getStrength();
            case "dexterity" -> accessor.dba$getDexterity();
            case "spirit" -> accessor.dba$getSpirit();
            case "willpower" -> accessor.dba$getWillpower();
            case "defense" -> accessor.dba$getDefense();
            default -> 0;
        };
    }

    /**
     * Raw amount from explicit numbers (base plus stat times perStat,
     * capped, scaled by power), for callers whose damage numbers live
     * outside the def damage block (swarm shards).
     */
    public static float amountRaw(double base, String statName, double perStat, Entity attacker, float power) {
        double raw = (base + statOf(attacker, statName) * perStat) * power;
        return DbaEffectsConfig.capDamage((float) raw);
    }

    public static DamageSource source(AbilityDef def, Entity direct, Entity attacker) {
        return switch (def.damage().type()) {
            case "void" -> DbaDamage.voidDmg(direct, attacker);
            case "curse" -> DbaDamage.curse(direct, attacker);
            case "darkness" -> DbaDamage.darkness(direct, attacker);
            case "hellfire" -> DbaDamage.hellfire(direct, attacker);
            case "tempest" -> DbaDamage.tempest(direct, attacker);
            case "spirit" -> DbaDamage.spirit(direct, attacker);
            case "temporal" -> DbaDamage.temporal(direct, attacker);
            case "gravity" -> DbaDamage.gravity(direct, attacker);
            case "lava" -> DbaDamage.lava(direct, attacker);
            case "valor" -> DbaDamage.valor(direct, attacker);
            case "crush" -> DbaDamage.crush(direct, attacker);
            default -> DbaDamage.ki(direct, attacker);
        };
    }

    /** Applies the ability's damage to one target. distFrac is 0 at the center, 1 at the rim. */
    public static void apply(ServerLevel level, AbilityDef def, Entity direct, Entity attacker,
                             LivingEntity target, float power, double distFrac) {
        float dmg = amount(def, attacker, power, distFrac);
        if (dmg <= 0) {
            return;
        }
        target.hurtServer(level, source(def, direct, attacker), dmg);
    }

    /**
     * The Phase 6 hit path: damage through amount(), then the def's
     * onHit module when its scope matches this hit kind, then combo
     * reporting. meleeKind is true only for melee casts and chain
     * hops; every carrier hit passes false.
     */
    public static void applyHit(ServerLevel level, AbilityDef def, Entity direct, Entity attacker,
                                LivingEntity target, float power, double distFrac, boolean meleeKind) {
        applyHitScaled(level, def, direct, attacker, target, power, distFrac, meleeKind, 1.0);
    }

    /** applyHit with an extra amount scale, for scaled area helpers and model zones. */
    public static void applyHitScaled(ServerLevel level, AbilityDef def, Entity direct, Entity attacker,
                               LivingEntity target, float power, double distFrac, boolean meleeKind,
                               double scale) {
        float dmg = amount(def, attacker, power, distFrac, scale);
        if (dmg <= 0) {
            return;
        }
        target.hurtServer(level, source(def, direct, attacker), dmg);
        AbilityDef.OnHitSpec onHit = def.onHit();
        if (onHit != null) {
            boolean applies = switch (onHit.appliesTo()) {
                case "melee" -> meleeKind;
                case "carrier" -> !meleeKind;
                default -> true;
            };
            if (applies) {
                if (onHit.effectId() != null) {
                    MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(onHit.effectId());
                    if (effect != null) {
                        target.addEffect(new MobEffectInstance(
                                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                                onHit.durationTicks(), onHit.amplifier()));
                    }
                }
                if (onHit.healFraction() > 0 && attacker instanceof LivingEntity livingAttacker) {
                    livingAttacker.heal((float) (dmg * onHit.healFraction()));
                }
            }
        }
        if (attacker instanceof ServerPlayer serverPlayer && def.combo() != null) {
            AbilityService.noteComboHit(serverPlayer, def);
        }
    }

    /** Mirrors applyArea, but every target goes through applyHit (carrier kind). */
    public static void applyAreaHit(ServerLevel level, AbilityDef def, Entity direct, Entity attacker,
                                    Vec3 center, double radius, float power, Entity exempt) {
        applyAreaScaled(level, def, direct, attacker, center, radius, power, exempt, 1.0);
    }

    /** applyAreaHit with the amount scaled (cloud finales and spawn bursts). */
    public static void applyAreaScaled(ServerLevel level, AbilityDef def, Entity direct, Entity attacker,
                                       Vec3 center, double radius, float power, Entity exempt, double scale) {
        if (radius <= 0) {
            return;
        }
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != exempt)) {
            double dist = target.position().distanceTo(center);
            if (dist <= radius + target.getBbWidth() * 0.5) {
                applyHitScaled(level, def, direct, attacker, target, power, dist / radius, false, scale);
            }
        }
    }

    /** Applies the ability's damage to every living entity in a radius, with radial falloff. */
    public static void applyArea(ServerLevel level, AbilityDef def, Entity direct, Entity attacker,
                                 Vec3 center, double radius, float power, Entity exempt) {
        if (radius <= 0) {
            return;
        }
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != exempt)) {
            double dist = target.position().distanceTo(center);
            if (dist <= radius + target.getBbWidth() * 0.5) {
                apply(level, def, direct, attacker, target, power, dist / radius);
            }
        }
    }
}
