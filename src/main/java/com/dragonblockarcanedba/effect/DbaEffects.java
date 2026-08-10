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

    public static final MobEffect BLEEDING = new BleedingEffect();
    public static final MobEffect MELTING = new MeltingEffect();
    
    public static Holder<MobEffect> BLEEDING_HOLDER;
    public static Holder<MobEffect> MELTING_HOLDER;

    public static void register() {
        BLEEDING_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, BLEEDING_KEY, BLEEDING);
        MELTING_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, MELTING_KEY, MELTING);
        DragonBlockArcaneDBA.LOGGER.info("Registered DBA effects");
    }
}
