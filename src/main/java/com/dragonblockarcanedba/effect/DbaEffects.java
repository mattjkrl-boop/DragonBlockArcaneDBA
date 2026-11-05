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
    public static final ResourceKey<MobEffect> HOLLOWED_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("hollowed")
    );
    public static final ResourceKey<MobEffect> DARK_FADED_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("dark_faded")
    );
    public static final ResourceKey<MobEffect> RIFTED_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("rifted")
    );
    public static final ResourceKey<MobEffect> VALOR_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("valor")
    );
    public static final ResourceKey<MobEffect> CINEMATIC_TRACKING_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("cinematic_tracking")
    );
    public static final ResourceKey<MobEffect> MARKED_BY_EVIL_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("marked_by_evil")
    );
    public static final ResourceKey<MobEffect> SILENT_MARK_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("silent_mark")
    );

    // --- NEW WEAPON EFFECTS ---
    public static final ResourceKey<MobEffect> CELESTIAL_GRACE_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("celestial_grace")
    );
    public static final ResourceKey<MobEffect> TEMPORAL_STASIS_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("temporal_stasis")
    );
    public static final ResourceKey<MobEffect> ANCIENT_WEIGHT_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("ancient_weight")
    );
    public static final ResourceKey<MobEffect> EARTH_SHATTER_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("earth_shatter")
    );
    public static final ResourceKey<MobEffect> SPIRIT_IMPALE_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("spirit_impale")
    );
    public static final ResourceKey<MobEffect> SORROW_RIFT_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("sorrow_rift")
    );
    public static final ResourceKey<MobEffect> JUDGEMENT_LOCK_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("judgement_lock")
    );
    public static final ResourceKey<MobEffect> POLE_STUN_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("pole_stun")
    );
    public static final ResourceKey<MobEffect> OX_BRACE_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("ox_brace")
    );
    public static final ResourceKey<MobEffect> FISSURE_STUN_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("fissure_stun")
    );
    public static final ResourceKey<MobEffect> BLADE_GUARD_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("blade_guard")
    );
    public static final ResourceKey<MobEffect> DEMON_SURGE_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("demon_surge")
    );
    public static final ResourceKey<MobEffect> PETRIFICATION_CURSE_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("petrification_curse")
    );
    public static final ResourceKey<MobEffect> HEROIC_FOCUS_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("heroic_focus")
    );
    public static final ResourceKey<MobEffect> ENERGY_OVERCHARGE_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("energy_overcharge")
    );
    public static final ResourceKey<MobEffect> VALOR_STUN_KEY = ResourceKey.create(
        Registries.MOB_EFFECT, DragonBlockArcaneDBA.id("valor_stun")
    );

    public static final MobEffect BLEEDING = new BleedingEffect();
    public static final MobEffect MELTING = new MeltingEffect();
    public static final MobEffect DEVILS_HANDS = new DevilsHandsEffect();
    public static final MobEffect MOVEMENT_CURSE = new MovementCurseEffect();
    public static final MobEffect STORM_OF_DARKNESS = new StormOfDarknessEffect();
    public static final MobEffect HOLLOWED = new HollowedEffect();
    public static final MobEffect DARK_FADED = new DarkFadedEffect();
    public static final MobEffect RIFTED = new RiftedEffect();
    public static final MobEffect VALOR = new ValorEffect();
    public static final MobEffect CINEMATIC_TRACKING = new CinematicTrackingEffect();
    public static final MobEffect MARKED_BY_EVIL = new MarkedByEvilEffect();
    public static final MobEffect SILENT_MARK = new SilentMarkEffect();

    public static final MobEffect CELESTIAL_GRACE = new CelestialGraceEffect();
    public static final MobEffect TEMPORAL_STASIS = new TemporalStasisEffect();
    public static final MobEffect ANCIENT_WEIGHT = new AncientWeightEffect();
    public static final MobEffect EARTH_SHATTER = new EarthShatterEffect();
    public static final MobEffect SPIRIT_IMPALE = new SpiritImpaleEffect();
    public static final MobEffect SORROW_RIFT = new SorrowRiftEffect();
    public static final MobEffect JUDGEMENT_LOCK = new JudgementLockEffect();
    public static final MobEffect POLE_STUN = new PoleStunEffect();
    public static final MobEffect OX_BRACE = new OxBraceEffect();
    public static final MobEffect FISSURE_STUN = new FissureStunEffect();
    public static final MobEffect BLADE_GUARD = new BladeGuardEffect();
    public static final MobEffect DEMON_SURGE = new DemonSurgeEffect();
    public static final MobEffect PETRIFICATION_CURSE = new PetrificationCurseEffect();
    public static final MobEffect HEROIC_FOCUS = new HeroicFocusEffect();
    public static final MobEffect ENERGY_OVERCHARGE = new EnergyOverchargeEffect();
    public static final MobEffect VALOR_STUN = new ValorStunEffect();
    
    public static Holder<MobEffect> BLEEDING_HOLDER;
    public static Holder<MobEffect> MELTING_HOLDER;
    public static Holder<MobEffect> DEVILS_HANDS_HOLDER;
    public static Holder<MobEffect> MOVEMENT_CURSE_HOLDER;
    public static Holder<MobEffect> STORM_OF_DARKNESS_HOLDER;
    public static Holder<MobEffect> HOLLOWED_HOLDER;
    public static Holder<MobEffect> DARK_FADED_HOLDER;
    public static Holder<MobEffect> RIFTED_HOLDER;
    public static Holder<MobEffect> VALOR_HOLDER;
    public static Holder<MobEffect> CINEMATIC_TRACKING_HOLDER;
    public static Holder<MobEffect> MARKED_BY_EVIL_HOLDER;
    public static Holder<MobEffect> SILENT_MARK_HOLDER;

    public static Holder<MobEffect> CELESTIAL_GRACE_HOLDER;
    public static Holder<MobEffect> TEMPORAL_STASIS_HOLDER;
    public static Holder<MobEffect> ANCIENT_WEIGHT_HOLDER;
    public static Holder<MobEffect> EARTH_SHATTER_HOLDER;
    public static Holder<MobEffect> SPIRIT_IMPALE_HOLDER;
    public static Holder<MobEffect> SORROW_RIFT_HOLDER;
    public static Holder<MobEffect> JUDGEMENT_LOCK_HOLDER;
    public static Holder<MobEffect> POLE_STUN_HOLDER;
    public static Holder<MobEffect> OX_BRACE_HOLDER;
    public static Holder<MobEffect> FISSURE_STUN_HOLDER;
    public static Holder<MobEffect> BLADE_GUARD_HOLDER;
    public static Holder<MobEffect> DEMON_SURGE_HOLDER;
    public static Holder<MobEffect> PETRIFICATION_CURSE_HOLDER;
    public static Holder<MobEffect> HEROIC_FOCUS_HOLDER;
    public static Holder<MobEffect> ENERGY_OVERCHARGE_HOLDER;
    public static Holder<MobEffect> VALOR_STUN_HOLDER;

    public static void register() {
        BLEEDING_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, BLEEDING_KEY, BLEEDING);
        MELTING_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, MELTING_KEY, MELTING);
        DEVILS_HANDS_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, DEVILS_HANDS_KEY, DEVILS_HANDS);
        MOVEMENT_CURSE_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, MOVEMENT_CURSE_KEY, MOVEMENT_CURSE);
        STORM_OF_DARKNESS_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, STORM_OF_DARKNESS_KEY, STORM_OF_DARKNESS);
        HOLLOWED_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, HOLLOWED_KEY, HOLLOWED);
        DARK_FADED_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, DARK_FADED_KEY, DARK_FADED);
        RIFTED_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, RIFTED_KEY, RIFTED);
        VALOR_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, VALOR_KEY, VALOR);
        CINEMATIC_TRACKING_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, CINEMATIC_TRACKING_KEY, CINEMATIC_TRACKING);
        MARKED_BY_EVIL_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, MARKED_BY_EVIL_KEY, MARKED_BY_EVIL);
        SILENT_MARK_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, SILENT_MARK_KEY, SILENT_MARK);

        CELESTIAL_GRACE_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, CELESTIAL_GRACE_KEY, CELESTIAL_GRACE);
        TEMPORAL_STASIS_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, TEMPORAL_STASIS_KEY, TEMPORAL_STASIS);
        ANCIENT_WEIGHT_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, ANCIENT_WEIGHT_KEY, ANCIENT_WEIGHT);
        EARTH_SHATTER_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, EARTH_SHATTER_KEY, EARTH_SHATTER);
        SPIRIT_IMPALE_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, SPIRIT_IMPALE_KEY, SPIRIT_IMPALE);
        SORROW_RIFT_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, SORROW_RIFT_KEY, SORROW_RIFT);
        JUDGEMENT_LOCK_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, JUDGEMENT_LOCK_KEY, JUDGEMENT_LOCK);
        POLE_STUN_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, POLE_STUN_KEY, POLE_STUN);
        OX_BRACE_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, OX_BRACE_KEY, OX_BRACE);
        FISSURE_STUN_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, FISSURE_STUN_KEY, FISSURE_STUN);
        BLADE_GUARD_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, BLADE_GUARD_KEY, BLADE_GUARD);
        DEMON_SURGE_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, DEMON_SURGE_KEY, DEMON_SURGE);
        PETRIFICATION_CURSE_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, PETRIFICATION_CURSE_KEY, PETRIFICATION_CURSE);
        HEROIC_FOCUS_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, HEROIC_FOCUS_KEY, HEROIC_FOCUS);
        ENERGY_OVERCHARGE_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, ENERGY_OVERCHARGE_KEY, ENERGY_OVERCHARGE);
        VALOR_STUN_HOLDER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, VALOR_STUN_KEY, VALOR_STUN);

        DragonBlockArcaneDBA.LOGGER.info("Registered DBA effects");
    }
}
