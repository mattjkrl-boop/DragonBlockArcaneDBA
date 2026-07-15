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

    public static void register() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, TRANSFORM_GENERIC_ID, TRANSFORM_GENERIC);
        Registry.register(BuiltInRegistries.SOUND_EVENT, ARCOSIAN_HURT_ID, ARCOSIAN_HURT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, NAMEKIAN_HURT_ID, NAMEKIAN_HURT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SAIYAN_HURT_ID, SAIYAN_HURT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SPACE_POD_LAUNCH_ID, SPACE_POD_LAUNCH);
        Registry.register(BuiltInRegistries.SOUND_EVENT, KAMEHAMEHA_CHARGE_ID, KAMEHAMEHA_CHARGE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, KAMEHAMEHA_FIRE_ID, KAMEHAMEHA_FIRE);
    }
}
