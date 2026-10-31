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
    }
}
