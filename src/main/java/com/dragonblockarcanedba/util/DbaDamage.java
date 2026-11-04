package com.dragonblockarcanedba.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

/**
 * The mod's own damage types for weapon abilities (BESPOKE-STANDARD rule 4).
 * One factory per theme family; the matching JSON lives in
 * data/dragonblockarcanedba/damage_type and mirrors the vanilla player_attack
 * and magic shapes, so scaling and exhaustion behave exactly as the vanilla
 * types they replace did. Death messages are in the lang file.
 *
 * Honest exception, by design: the plain melee weapon hit keeps the vanilla
 * player attack source, because that hit IS the game's weapon hit (attack
 * damage attribute, speed, knockback, enchantments). Everything layered on
 * top of it (abilities, procs, ability dots, constructs) uses these types.
 *
 * Mapping from the vanilla types the weapons used before this pass:
 * ability magic and indirectMagic -> the weapon's theme family; mobAttack
 * and mobProjectile from ability entities -> the theme family with the
 * ability entity as the direct entity; lightningBolt -> tempest; inWall ->
 * crush; hotFloor -> lava; plain generic ability damage -> ki for energy
 * weapons, crush for physical ones.
 */
public final class DbaDamage {
    private DbaDamage() {}

    public static final ResourceKey<DamageType> KI = key("ki");
    public static final ResourceKey<DamageType> VOID = key("void");
    public static final ResourceKey<DamageType> CURSE = key("curse");
    public static final ResourceKey<DamageType> DARKNESS = key("darkness");
    public static final ResourceKey<DamageType> HELLFIRE = key("hellfire");
    public static final ResourceKey<DamageType> TEMPEST = key("tempest");
    public static final ResourceKey<DamageType> SPIRIT = key("spirit");
    public static final ResourceKey<DamageType> TEMPORAL = key("temporal");
    public static final ResourceKey<DamageType> GRAVITY = key("gravity");
    public static final ResourceKey<DamageType> LAVA = key("lava");
    public static final ResourceKey<DamageType> VALOR = key("valor");
    public static final ResourceKey<DamageType> CRUSH = key("crush");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,
            net.minecraft.resources.Identifier.fromNamespaceAndPath("dragonblockarcanedba", name));
    }

    private static DamageSource make(ResourceKey<DamageType> key, Entity direct, Entity attacker) {
        Entity anchor = attacker != null ? attacker : direct;
        Holder<DamageType> holder = anchor.level().registryAccess()
            .lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
        return new DamageSource(holder, direct, attacker);
    }

    public static DamageSource ki(Entity attacker) { return make(KI, null, attacker); }
    public static DamageSource ki(Entity direct, Entity attacker) { return make(KI, direct, attacker); }
    public static DamageSource voidDmg(Entity attacker) { return make(VOID, null, attacker); }
    public static DamageSource voidDmg(Entity direct, Entity attacker) { return make(VOID, direct, attacker); }
    public static DamageSource curse(Entity attacker) { return make(CURSE, null, attacker); }
    public static DamageSource curse(Entity direct, Entity attacker) { return make(CURSE, direct, attacker); }
    public static DamageSource darkness(Entity attacker) { return make(DARKNESS, null, attacker); }
    public static DamageSource darkness(Entity direct, Entity attacker) { return make(DARKNESS, direct, attacker); }
    public static DamageSource hellfire(Entity attacker) { return make(HELLFIRE, null, attacker); }
    public static DamageSource hellfire(Entity direct, Entity attacker) { return make(HELLFIRE, direct, attacker); }
    public static DamageSource tempest(Entity attacker) { return make(TEMPEST, null, attacker); }
    public static DamageSource tempest(Entity direct, Entity attacker) { return make(TEMPEST, direct, attacker); }
    public static DamageSource spirit(Entity attacker) { return make(SPIRIT, null, attacker); }
    public static DamageSource spirit(Entity direct, Entity attacker) { return make(SPIRIT, direct, attacker); }
    public static DamageSource temporal(Entity attacker) { return make(TEMPORAL, null, attacker); }
    public static DamageSource temporal(Entity direct, Entity attacker) { return make(TEMPORAL, direct, attacker); }
    public static DamageSource gravity(Entity attacker) { return make(GRAVITY, null, attacker); }
    public static DamageSource gravity(Entity direct, Entity attacker) { return make(GRAVITY, direct, attacker); }
    public static DamageSource lava(Entity attacker) { return make(LAVA, null, attacker); }
    public static DamageSource lava(Entity direct, Entity attacker) { return make(LAVA, direct, attacker); }
    public static DamageSource valor(Entity attacker) { return make(VALOR, null, attacker); }
    public static DamageSource valor(Entity direct, Entity attacker) { return make(VALOR, direct, attacker); }
    public static DamageSource crush(Entity attacker) { return make(CRUSH, null, attacker); }
    public static DamageSource crush(Entity direct, Entity attacker) { return make(CRUSH, direct, attacker); }
}
