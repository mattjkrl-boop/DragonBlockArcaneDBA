package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.Holder;

public class DbaEffects {
    public static final ResourceKey<MobEffect> BLEEDING_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("bleeding")
    );
    public static final ResourceKey<MobEffect> MELTING_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("melting")
    );
    public static final ResourceKey<MobEffect> DEVILS_HANDS_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("devils_hands")
    );
    public static final ResourceKey<MobEffect> MOVEMENT_CURSE_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("movement_curse")
    );
    public static final ResourceKey<MobEffect> STORM_OF_DARKNESS_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("storm_of_darkness")
    );

    public static final MobEffect BLEEDING = new BleedingEffect();
    public static final MobEffect MELTING = new MeltingEffect();
    public static final MobEffect DEVILS_HANDS = new DevilsHandsEffect();
    public static final MobEffect MOVEMENT_CURSE = new MovementCurseEffect();
    public static final MobEffect STORM_OF_DARKNESS = new StormOfDarknessEffect();
    
    public static Holder<MobEffect> BLEEDING_HOLDER;
    public static Holder<MobEffect> MELTING_HOLDER;
    public static Holder<MobEffect> DEVILS_HANDS_HOLDER;
    public static Holder<MobEffect> MOVEMENT_CURSE_HOLDER;
    public static Holder<MobEffect> STORM_OF_DARKNESS_HOLDER;

    public static void register() {
        BLEEDING_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, BLEEDING_KEY, BLEEDING);
        MELTING_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, MELTING_KEY, MELTING);
        DEVILS_HANDS_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, DEVILS_HANDS_KEY, DEVILS_HANDS);
        MOVEMENT_CURSE_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, MOVEMENT_CURSE_KEY, MOVEMENT_CURSE);
        STORM_OF_DARKNESS_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, STORM_OF_DARKNESS_KEY, STORM_OF_DARKNESS);
        DragonBlockArcaneDBA.LOGGER.info("Registered DBA effects");
    }
}
