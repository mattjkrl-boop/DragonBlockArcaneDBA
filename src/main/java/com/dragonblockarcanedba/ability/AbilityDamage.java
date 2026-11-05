package com.dragonblockarcanedba.ability;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.util.DbaDamage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
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
        AbilityDef.DamageSpec dmg = def.damage();
        double stat = 0;
        if (caster instanceof Player player) {
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
            stat = switch (dmg.stat()) {
                case "strength" -> accessor.dba$getStrength();
                case "dexterity" -> accessor.dba$getDexterity();
                case "spirit" -> accessor.dba$getSpirit();
                case "willpower" -> accessor.dba$getWillpower();
                case "defense" -> accessor.dba$getDefense();
                default -> 0;
            };
        }
        double raw = (dmg.base() + stat * dmg.perStat()) * power;
        return DbaEffectsConfig.capDamage((float) (raw * falloffFactor(def, distFrac)));
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
