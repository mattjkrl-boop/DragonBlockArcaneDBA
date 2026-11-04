package com.dragonblockarcanedba.sound;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.sounds.SoundEvent;

public class DbaSounds {
    // Transformations
    public static final Identifier TRANSFORM_GENERIC_ID = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "transform_generic");
    public static final SoundEvent TRANSFORM_GENERIC = SoundEvent.createVariableRangeEvent(TRANSFORM_GENERIC_ID);

    // Race Sound Profiles
    public static final Identifier ARCOSIAN_HURT_ID = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "arcosian_hurt");
    public static final SoundEvent ARCOSIAN_HURT = SoundEvent.createVariableRangeEvent(ARCOSIAN_HURT_ID);

    public static final Identifier NAMEKIAN_HURT_ID = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "namekian_hurt");
    public static final SoundEvent NAMEKIAN_HURT = SoundEvent.createVariableRangeEvent(NAMEKIAN_HURT_ID);

    public static final Identifier SAIYAN_HURT_ID = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "saiyan_hurt");
    public static final SoundEvent SAIYAN_HURT = SoundEvent.createVariableRangeEvent(SAIYAN_HURT_ID);

    // Vehicles / Dimensions
    public static final Identifier SPACE_POD_LAUNCH_ID = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "space_pod_launch");
    public static final SoundEvent SPACE_POD_LAUNCH = SoundEvent.createVariableRangeEvent(SPACE_POD_LAUNCH_ID);

    // Techniques
    public static final Identifier KAMEHAMEHA_CHARGE_ID = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "kamehameha_charge");
    public static final SoundEvent KAMEHAMEHA_CHARGE = SoundEvent.createVariableRangeEvent(KAMEHAMEHA_CHARGE_ID);

    public static final Identifier KAMEHAMEHA_FIRE_ID = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "kamehameha_fire");
    public static final SoundEvent KAMEHAMEHA_FIRE = SoundEvent.createVariableRangeEvent(KAMEHAMEHA_FIRE_ID);

    // Layered ability sounds
    public static final SoundEvent ABILITY_CHARGE = sfx("ability_charge");
    public static final SoundEvent BLADE_SHING = sfx("blade_shing");
    public static final SoundEvent BLASTER_SHOT = sfx("blaster_shot");
    public static final SoundEvent DARK_PULSE = sfx("dark_pulse");
    public static final SoundEvent HEAVY_IMPACT = sfx("heavy_impact");
    public static final SoundEvent LIGHTNING_ZAP = sfx("lightning_zap");
    public static final SoundEvent SABER_HUM = sfx("saber_hum");
    public static final SoundEvent SLASH_WHOOSH = sfx("slash_whoosh");
    public static final SoundEvent TIME_STOP = sfx("time_stop");
    public static final SoundEvent WIND_GUST = sfx("wind_gust");


    // Bespoke pass bank: synthesized by tools/gen_bespoke_sounds.py, named by
    // use case. Phase 2 swap guide (vanilla use case -> bespoke event):
    // swings PLAYER_ATTACK_SWEEP -> SLASH_WHOOSH / SWING_LIGHT / SWING_HEAVY /
    // SWING_FAN by weapon weight; explosion GENERIC_EXPLODE -> BOOM_BIG /
    // RELEASE_BLAST / HEAVY_IMPACT; slam ANVIL_LAND -> SLAM_THUD / GROUND_CRACK;
    // crit PLAYER_ATTACK_CRIT -> CRIT_HIT; thunder -> THUNDER_CRACK /
    // THUNDER_DARK (storms play these from AtmosphereClient); zaps -> LIGHTNING_ZAP /
    // ZAP_DARK; charge start BEACON_ACTIVATE -> CHARGE_RISE / CHARGE_DARK /
    // ABILITY_CHARGE; charge hum BEACON_AMBIENT -> CHARGE_HUM; milestones ->
    // CHARGE_FULL; sonic boom -> BOOM_SONIC; flight ELYTRA_FLYING -> FLIGHT_WIND;
    // wind bursts -> WIND_GUST / SWING_FAN; teleport -> TELEPORT_WHOOSH; portal ->
    // RIFT_AMBIENT / RIFT_OPEN; fire -> FIRE_SHOT / FIRE_IGNITE; lava -> LAVA_BUBBLE /
    // EXTINGUISH; crystal and glass -> CRYSTAL_CHIME / GLASS_CRACK; chains ->
    // CHAIN_RATTLE; trident -> TRIDENT_THROW / TRIDENT_HIT; casts -> CAST_MAGIC /
    // CAST_DARK; dark marks and curses -> MARK_HIT / HEARTBEAT_DARK / DARK_PULSE;
    // souls -> SOUL_DRIFT; void ambience -> VOID_AMBIENT; block and parry ->
    // SHIELD_CLANK; weapon hits -> HIT_FLESH / HIT_METAL / THUNK; break -> ITEM_SHATTER;
    // fizzle and fail -> FIZZLE; holy booms -> BOOM_HOLY; beams -> BEAM_CHARGE / BEAM_FIRE.
    public static final SoundEvent SWING_LIGHT = sfx("swing_light");
    public static final SoundEvent SWING_HEAVY = sfx("swing_heavy");
    public static final SoundEvent SWING_FAN = sfx("swing_fan");
    public static final SoundEvent HIT_FLESH = sfx("hit_flesh");
    public static final SoundEvent HIT_METAL = sfx("hit_metal");
    public static final SoundEvent CRIT_HIT = sfx("crit_hit");
    public static final SoundEvent MARK_HIT = sfx("mark_hit");
    public static final SoundEvent CHARGE_RISE = sfx("charge_rise");
    public static final SoundEvent CHARGE_HUM = sfx("charge_hum");
    public static final SoundEvent CHARGE_FULL = sfx("charge_full");
    public static final SoundEvent CHARGE_DARK = sfx("charge_dark");
    public static final SoundEvent RELEASE_BLAST = sfx("release_blast");
    public static final SoundEvent RELEASE_DARK = sfx("release_dark");
    public static final SoundEvent BEAM_CHARGE = sfx("beam_charge");
    public static final SoundEvent BEAM_FIRE = sfx("beam_fire");
    public static final SoundEvent BOOM_BIG = sfx("boom_big");
    public static final SoundEvent BOOM_SONIC = sfx("boom_sonic");
    public static final SoundEvent BOOM_HOLY = sfx("boom_holy");
    public static final SoundEvent SLAM_THUD = sfx("slam_thud");
    public static final SoundEvent GROUND_CRACK = sfx("ground_crack");
    public static final SoundEvent THUNDER_CRACK = sfx("thunder_crack");
    public static final SoundEvent THUNDER_DARK = sfx("thunder_dark");
    public static final SoundEvent ZAP_DARK = sfx("zap_dark");
    public static final SoundEvent TELEPORT_WHOOSH = sfx("teleport_whoosh");
    public static final SoundEvent RIFT_AMBIENT = sfx("rift_ambient");
    public static final SoundEvent RIFT_OPEN = sfx("rift_open");
    public static final SoundEvent FLIGHT_WIND = sfx("flight_wind");
    public static final SoundEvent VOID_AMBIENT = sfx("void_ambient");
    public static final SoundEvent FIRE_SHOT = sfx("fire_shot");
    public static final SoundEvent FIRE_IGNITE = sfx("fire_ignite");
    public static final SoundEvent LAVA_BUBBLE = sfx("lava_bubble");
    public static final SoundEvent EXTINGUISH = sfx("extinguish");
    public static final SoundEvent CRYSTAL_CHIME = sfx("crystal_chime");
    public static final SoundEvent GLASS_CRACK = sfx("glass_crack");
    public static final SoundEvent CHAIN_RATTLE = sfx("chain_rattle");
    public static final SoundEvent TRIDENT_THROW = sfx("trident_throw");
    public static final SoundEvent TRIDENT_HIT = sfx("trident_hit");
    public static final SoundEvent CAST_MAGIC = sfx("cast_magic");
    public static final SoundEvent CAST_DARK = sfx("cast_dark");
    public static final SoundEvent SOUL_DRIFT = sfx("soul_drift");
    public static final SoundEvent SHIELD_CLANK = sfx("shield_clank");
    public static final SoundEvent ITEM_SHATTER = sfx("item_shatter");
    public static final SoundEvent FIZZLE = sfx("fizzle");
    public static final SoundEvent THUNK = sfx("thunk");
    public static final SoundEvent HEARTBEAT_DARK = sfx("heartbeat_dark");

    private static SoundEvent sfx(String name) {
        return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("dragonblockarcanedba", name));
    }

    public static void register() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, TRANSFORM_GENERIC_ID, TRANSFORM_GENERIC);
        Registry.register(BuiltInRegistries.SOUND_EVENT, ARCOSIAN_HURT_ID, ARCOSIAN_HURT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, NAMEKIAN_HURT_ID, NAMEKIAN_HURT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SAIYAN_HURT_ID, SAIYAN_HURT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SPACE_POD_LAUNCH_ID, SPACE_POD_LAUNCH);
        Registry.register(BuiltInRegistries.SOUND_EVENT, KAMEHAMEHA_CHARGE_ID, KAMEHAMEHA_CHARGE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, KAMEHAMEHA_FIRE_ID, KAMEHAMEHA_FIRE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, ABILITY_CHARGE.location(), ABILITY_CHARGE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, BLADE_SHING.location(), BLADE_SHING);
        Registry.register(BuiltInRegistries.SOUND_EVENT, BLASTER_SHOT.location(), BLASTER_SHOT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, DARK_PULSE.location(), DARK_PULSE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, HEAVY_IMPACT.location(), HEAVY_IMPACT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, LIGHTNING_ZAP.location(), LIGHTNING_ZAP);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SABER_HUM.location(), SABER_HUM);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SLASH_WHOOSH.location(), SLASH_WHOOSH);
        Registry.register(BuiltInRegistries.SOUND_EVENT, TIME_STOP.location(), TIME_STOP);
        Registry.register(BuiltInRegistries.SOUND_EVENT, WIND_GUST.location(), WIND_GUST);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SWING_LIGHT.location(), SWING_LIGHT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SWING_HEAVY.location(), SWING_HEAVY);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SWING_FAN.location(), SWING_FAN);
        Registry.register(BuiltInRegistries.SOUND_EVENT, HIT_FLESH.location(), HIT_FLESH);
        Registry.register(BuiltInRegistries.SOUND_EVENT, HIT_METAL.location(), HIT_METAL);
        Registry.register(BuiltInRegistries.SOUND_EVENT, CRIT_HIT.location(), CRIT_HIT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, MARK_HIT.location(), MARK_HIT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, CHARGE_RISE.location(), CHARGE_RISE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, CHARGE_HUM.location(), CHARGE_HUM);
        Registry.register(BuiltInRegistries.SOUND_EVENT, CHARGE_FULL.location(), CHARGE_FULL);
        Registry.register(BuiltInRegistries.SOUND_EVENT, CHARGE_DARK.location(), CHARGE_DARK);
        Registry.register(BuiltInRegistries.SOUND_EVENT, RELEASE_BLAST.location(), RELEASE_BLAST);
        Registry.register(BuiltInRegistries.SOUND_EVENT, RELEASE_DARK.location(), RELEASE_DARK);
        Registry.register(BuiltInRegistries.SOUND_EVENT, BEAM_CHARGE.location(), BEAM_CHARGE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, BEAM_FIRE.location(), BEAM_FIRE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, BOOM_BIG.location(), BOOM_BIG);
        Registry.register(BuiltInRegistries.SOUND_EVENT, BOOM_SONIC.location(), BOOM_SONIC);
        Registry.register(BuiltInRegistries.SOUND_EVENT, BOOM_HOLY.location(), BOOM_HOLY);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SLAM_THUD.location(), SLAM_THUD);
        Registry.register(BuiltInRegistries.SOUND_EVENT, GROUND_CRACK.location(), GROUND_CRACK);
        Registry.register(BuiltInRegistries.SOUND_EVENT, THUNDER_CRACK.location(), THUNDER_CRACK);
        Registry.register(BuiltInRegistries.SOUND_EVENT, THUNDER_DARK.location(), THUNDER_DARK);
        Registry.register(BuiltInRegistries.SOUND_EVENT, ZAP_DARK.location(), ZAP_DARK);
        Registry.register(BuiltInRegistries.SOUND_EVENT, TELEPORT_WHOOSH.location(), TELEPORT_WHOOSH);
        Registry.register(BuiltInRegistries.SOUND_EVENT, RIFT_AMBIENT.location(), RIFT_AMBIENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, RIFT_OPEN.location(), RIFT_OPEN);
        Registry.register(BuiltInRegistries.SOUND_EVENT, FLIGHT_WIND.location(), FLIGHT_WIND);
        Registry.register(BuiltInRegistries.SOUND_EVENT, VOID_AMBIENT.location(), VOID_AMBIENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, FIRE_SHOT.location(), FIRE_SHOT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, FIRE_IGNITE.location(), FIRE_IGNITE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, LAVA_BUBBLE.location(), LAVA_BUBBLE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, EXTINGUISH.location(), EXTINGUISH);
        Registry.register(BuiltInRegistries.SOUND_EVENT, CRYSTAL_CHIME.location(), CRYSTAL_CHIME);
        Registry.register(BuiltInRegistries.SOUND_EVENT, GLASS_CRACK.location(), GLASS_CRACK);
        Registry.register(BuiltInRegistries.SOUND_EVENT, CHAIN_RATTLE.location(), CHAIN_RATTLE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, TRIDENT_THROW.location(), TRIDENT_THROW);
        Registry.register(BuiltInRegistries.SOUND_EVENT, TRIDENT_HIT.location(), TRIDENT_HIT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, CAST_MAGIC.location(), CAST_MAGIC);
        Registry.register(BuiltInRegistries.SOUND_EVENT, CAST_DARK.location(), CAST_DARK);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SOUL_DRIFT.location(), SOUL_DRIFT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SHIELD_CLANK.location(), SHIELD_CLANK);
        Registry.register(BuiltInRegistries.SOUND_EVENT, ITEM_SHATTER.location(), ITEM_SHATTER);
        Registry.register(BuiltInRegistries.SOUND_EVENT, FIZZLE.location(), FIZZLE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, THUNK.location(), THUNK);
        Registry.register(BuiltInRegistries.SOUND_EVENT, HEARTBEAT_DARK.location(), HEARTBEAT_DARK);
    }
}
