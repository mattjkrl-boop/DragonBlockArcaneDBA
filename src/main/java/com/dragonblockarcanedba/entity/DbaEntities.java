package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class DbaEntities {
    
    public static final EntityType<OtherworldGuideEntity> OTHERWORLD_GUIDE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("otherworld_guide"),
            EntityType.Builder.of(OtherworldGuideEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("otherworld_guide")))
    );

    public static final EntityType<FlyingNimbusEntity> FLYING_NIMBUS = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("flying_nimbus"),
            EntityType.Builder.of(FlyingNimbusEntity::new, MobCategory.MISC)
                    .sized(1.2f, 0.4f)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("flying_nimbus")))
    );

    public static final EntityType<DimensionalSlashEntity> DIMENSIONAL_SLASH = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("dimensional_slash"),
            EntityType.Builder.<DimensionalSlashEntity>of(DimensionalSlashEntity::new, MobCategory.MISC)
                    .sized(2.0f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("dimensional_slash")))
    );

    public static final EntityType<TridentShardEntity> TRIDENT_SHARD = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("trident_shard"),
            EntityType.Builder.<TridentShardEntity>of(TridentShardEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("trident_shard")))
    );


    public static final EntityType<KiBlastEntity> KI_BLAST = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ki_blast"),
            EntityType.Builder.<KiBlastEntity>of(KiBlastEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ki_blast")))
    );

    public static final EntityType<KiBeamEntity> KI_BEAM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ki_beam"),
            EntityType.Builder.<KiBeamEntity>of(KiBeamEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f) // Bounding box doesn't matter much for rendering
                    .clientTrackingRange(10)
                    .updateInterval(1) // Update every tick so it stays glued to the player
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ki_beam")))
    );

    public static final EntityType<KiDiskEntity> KI_DISK = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ki_disk"),
            EntityType.Builder.<KiDiskEntity>of(KiDiskEntity::new, MobCategory.MISC)
                    .sized(1.5f, 0.2f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ki_disk")))
    );

    public static final EntityType<KiLaserEntity> KI_LASER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ki_laser"),
            EntityType.Builder.<KiLaserEntity>of(KiLaserEntity::new, MobCategory.MISC)
                    .sized(0.2f, 0.2f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ki_laser")))
    );

    public static final EntityType<KiSpiralBeamEntity> KI_SPIRAL_BEAM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ki_spiral_beam"),
            EntityType.Builder.<KiSpiralBeamEntity>of(KiSpiralBeamEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ki_spiral_beam")))
    );

    public static final EntityType<KiExplosionEntity> KI_EXPLOSION = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ki_explosion"),
            EntityType.Builder.<KiExplosionEntity>of(KiExplosionEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f) // Size changes dynamically anyway
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ki_explosion")))
    );

    public static final EntityType<ShenronEntity> SHENRON = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("shenron"),
            EntityType.Builder.of(ShenronEntity::new, MobCategory.MISC)
                    .sized(4.0f, 16.0f)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("shenron")))
    );

    public static final EntityType<PorungaEntity> PORUNGA = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("porunga"),
            EntityType.Builder.of(PorungaEntity::new, MobCategory.MISC)
                    .sized(6.0f, 20.0f)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("porunga")))
    );

    public static final EntityType<ZShockwaveEntity> Z_SHOCKWAVE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("z_shockwave"),
            EntityType.Builder.<ZShockwaveEntity>of(ZShockwaveEntity::new, MobCategory.MISC)
                    .sized(4.0f, 1.2f)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("z_shockwave")))
    );

    public static final EntityType<CurseLightningEntity> CURSE_LIGHTNING = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("curse_lightning"),
            EntityType.Builder.<CurseLightningEntity>of(CurseLightningEntity::new, MobCategory.MISC)
                    .sized(0.0f, 0.0f)
                    .clientTrackingRange(64)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("curse_lightning")))
    );

    public static final EntityType<CurseChainEntity> CURSE_CHAIN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("curse_chain"),
            EntityType.Builder.<CurseChainEntity>of(CurseChainEntity::new, MobCategory.MISC)
                    .sized(0.6f, 0.6f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("curse_chain")))
    );

    public static final EntityType<DelayedLaunchEntity> DELAYED_LAUNCH = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("delayed_launch"),
            EntityType.Builder.<DelayedLaunchEntity>of(DelayedLaunchEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("delayed_launch")))
    );

    public static final EntityType<SkyCracksEntity> SKY_CRACKS = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("sky_cracks"),
            EntityType.Builder.<SkyCracksEntity>of(SkyCracksEntity::new, MobCategory.MISC)
                    .sized(100.0f, 100.0f) // Giant bounding box
                    .clientTrackingRange(256) // Max tracking range
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("sky_cracks")))
    );

    public static final EntityType<VoidRiftEntity> VOID_RIFT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("void_rift"),
            EntityType.Builder.<VoidRiftEntity>of(VoidRiftEntity::new, MobCategory.MISC)
                    .sized(3.0f, 3.0f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("void_rift")))
    );

    public static final EntityType<HollowAfterimageEntity> HOLLOW_AFTERIMAGE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("hollow_afterimage"),
            EntityType.Builder.<HollowAfterimageEntity>of(HollowAfterimageEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("hollow_afterimage")))
    );

    public static final EntityType<VoidSlashEntity> VOID_SLASH = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("void_slash"),
            EntityType.Builder.<VoidSlashEntity>of(VoidSlashEntity::new, MobCategory.MISC)
                    .sized(3.0f, 0.8f)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("void_slash")))
    );

    public static final EntityType<HollowChargeEntity> HOLLOW_CHARGE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("hollow_charge"),
            EntityType.Builder.<HollowChargeEntity>of(HollowChargeEntity::new, MobCategory.MISC)
                    .sized(3.0f, 3.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("hollow_charge")))
    );

    public static final EntityType<HollowRushTrailEntity> HOLLOW_RUSH_TRAIL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("hollow_rush_trail"),
            EntityType.Builder.<HollowRushTrailEntity>of(HollowRushTrailEntity::new, MobCategory.MISC)
                    .sized(2.0f, 2.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("hollow_rush_trail")))
    );

    public static final EntityType<AzureStormEntity> AZURE_STORM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("azure_storm"),
            EntityType.Builder.<AzureStormEntity>of(AzureStormEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("azure_storm")))
    );

    public static final EntityType<AzureLightningEntity> AZURE_LIGHTNING = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("azure_lightning"),
            EntityType.Builder.<AzureLightningEntity>of(AzureLightningEntity::new, MobCategory.MISC)
                    .sized(0.0f, 0.0f)
                    .clientTrackingRange(64)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("azure_lightning")))
    );

    public static final EntityType<AzureTornadoEntity> AZURE_TORNADO = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("azure_tornado"),
            EntityType.Builder.<AzureTornadoEntity>of(AzureTornadoEntity::new, MobCategory.MISC)
                    .sized(4.0f, 8.0f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("azure_tornado")))
    );

    public static final EntityType<AzureRushTrailEntity> AZURE_RUSH_TRAIL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("azure_rush_trail"),
            EntityType.Builder.<AzureRushTrailEntity>of(AzureRushTrailEntity::new, MobCategory.MISC)
                    .sized(1.5f, 1.5f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("azure_rush_trail")))
    );

    public static final EntityType<AzureSonicQuakeEntity> AZURE_SONIC_QUAKE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("azure_sonic_quake"),
            EntityType.Builder.<AzureSonicQuakeEntity>of(AzureSonicQuakeEntity::new, MobCategory.MISC)
                    .sized(8.0f, 2.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("azure_sonic_quake")))
    );

    public static final EntityType<AzureTempestChannelEntity> AZURE_TEMPEST_CHANNEL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("azure_tempest_channel"),
            EntityType.Builder.<AzureTempestChannelEntity>of(AzureTempestChannelEntity::new, MobCategory.MISC)
                    .sized(4.0f, 4.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("azure_tempest_channel")))
    );

    public static final EntityType<OxShockwaveEntity> OX_SHOCKWAVE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ox_shockwave"),
            EntityType.Builder.<OxShockwaveEntity>of(OxShockwaveEntity::new, MobCategory.MISC)
                    .sized(2.0f, 1.0f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ox_shockwave")))
    );

    public static final EntityType<OxFissureEntity> OX_FISSURE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ox_fissure"),
            EntityType.Builder.<OxFissureEntity>of(OxFissureEntity::new, MobCategory.MISC)
                    .sized(3.0f, 0.5f)
                    .clientTrackingRange(12)
                    .updateInterval(2)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ox_fissure")))
    );

    public static final EntityType<GrandCrescentWaveEntity> GRAND_CRESCENT_WAVE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("grand_crescent_wave"),
            EntityType.Builder.<GrandCrescentWaveEntity>of(GrandCrescentWaveEntity::new, MobCategory.MISC)
                    .sized(3.5f, 1.0f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("grand_crescent_wave")))
    );

    public static final EntityType<GrandBladeShardEntity> GRAND_BLADE_SHARD = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("grand_blade_shard"),
            EntityType.Builder.<GrandBladeShardEntity>of(GrandBladeShardEntity::new, MobCategory.MISC)
                    .sized(0.6f, 0.6f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("grand_blade_shard")))
    );

    public static final EntityType<ValorFieldEntity> VALOR_FIELD = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("valor_field"),
            EntityType.Builder.<ValorFieldEntity>of(ValorFieldEntity::new, MobCategory.MISC)
                    .sized(18.0f, 18.0f)
                    .clientTrackingRange(24)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("valor_field")))
    );

    public static final EntityType<DarknessWaveEntity> DARKNESS_WAVE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("darkness_wave"),
            EntityType.Builder.<DarknessWaveEntity>of(DarknessWaveEntity::new, MobCategory.MISC)
                    .sized(3.5f, 1.2f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("darkness_wave")))
    );

    public static final EntityType<DarknessBladeEntity> DARKNESS_BLADE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("darkness_blade"),
            EntityType.Builder.<DarknessBladeEntity>of(DarknessBladeEntity::new, MobCategory.MISC)
                    .sized(1.0f, 6.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("darkness_blade")))
    );

    public static final EntityType<EvilSpearProjectileEntity> EVIL_SPEAR_PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("evil_spear_projectile"),
            EntityType.Builder.<EvilSpearProjectileEntity>of(EvilSpearProjectileEntity::new, MobCategory.MISC)
                    .sized(1.2f, 1.2f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("evil_spear_projectile")))
    );

    public static final EntityType<BlasterBoltEntity> BLASTER_BOLT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("blaster_bolt"),
            EntityType.Builder.<BlasterBoltEntity>of(BlasterBoltEntity::new, MobCategory.MISC)
                    .sized(0.6f, 0.6f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("blaster_bolt")))
    );

    public static final EntityType<BraveSlashEntity> BRAVE_SLASH = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("brave_slash"),
            EntityType.Builder.<BraveSlashEntity>of(BraveSlashEntity::new, MobCategory.MISC)
                    .sized(3.0f, 1.2f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("brave_slash")))
    );

    public static final EntityType<BraveCrossSlashEntity> BRAVE_CROSS_SLASH = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("brave_cross_slash"),
            EntityType.Builder.<BraveCrossSlashEntity>of(BraveCrossSlashEntity::new, MobCategory.MISC)
                    .sized(4.5f, 2.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("brave_cross_slash")))
    );

    public static final EntityType<BraveChargeEntity> BRAVE_CHARGE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("brave_charge"),
            EntityType.Builder.<BraveChargeEntity>of(BraveChargeEntity::new, MobCategory.MISC)
                    .sized(2.0f, 2.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("brave_charge")))
    );

    public static final EntityType<BraveRushTrailEntity> BRAVE_RUSH_TRAIL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("brave_rush_trail"),
            EntityType.Builder.<BraveRushTrailEntity>of(BraveRushTrailEntity::new, MobCategory.MISC)
                    .sized(2.0f, 2.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("brave_rush_trail")))
    );

    public static final EntityType<BraveShockwaveEntity> BRAVE_SHOCKWAVE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("brave_shockwave"),
            EntityType.Builder.<BraveShockwaveEntity>of(BraveShockwaveEntity::new, MobCategory.MISC)
                    .sized(8.0f, 2.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("brave_shockwave")))
    );

    public static final EntityType<SorrowSlashEntity> SORROW_SLASH = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("sorrow_slash"),
            EntityType.Builder.<SorrowSlashEntity>of(SorrowSlashEntity::new, MobCategory.MISC)
                    .sized(3.4f, 1.2f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("sorrow_slash")))
    );

    public static final EntityType<DimensionalRiftEntity> DIMENSIONAL_RIFT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("dimensional_rift"),
            EntityType.Builder.<DimensionalRiftEntity>of(DimensionalRiftEntity::new, MobCategory.MISC)
                    .sized(15.0f, 6.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("dimensional_rift")))
    );

    public static final EntityType<AbyssalDomainEntity> ABYSSAL_DOMAIN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("abyssal_domain"),
            EntityType.Builder.<AbyssalDomainEntity>of(AbyssalDomainEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("abyssal_domain")))
    );

    public static final EntityType<CurseTelegraphEntity> CURSE_TELEGRAPH = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("curse_telegraph"),
            EntityType.Builder.<CurseTelegraphEntity>of(CurseTelegraphEntity::new, MobCategory.MISC)
                    .sized(2.0f, 0.5f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("curse_telegraph")))
    );

    public static final EntityType<CurseGroundShatterEntity> CURSE_GROUND_SHATTER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("curse_ground_shatter"),
            EntityType.Builder.<CurseGroundShatterEntity>of(CurseGroundShatterEntity::new, MobCategory.MISC)
                    .sized(4.0f, 1.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("curse_ground_shatter")))
    );

    public static final EntityType<EvilSpearChargeEntity> EVIL_SPEAR_CHARGE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("evil_spear_charge"),
            EntityType.Builder.<EvilSpearChargeEntity>of(EvilSpearChargeEntity::new, MobCategory.MISC)
                    .sized(2.0f, 2.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("evil_spear_charge")))
    );

    public static final EntityType<HellHuntImpactEntity> HELL_HUNT_IMPACT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("hell_hunt_impact"),
            EntityType.Builder.<HellHuntImpactEntity>of(HellHuntImpactEntity::new, MobCategory.MISC)
                    .sized(4.5f, 1.5f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("hell_hunt_impact")))
    );

    public static final EntityType<BanshoCycloneEntity> BANSHO_CYCLONE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("bansho_cyclone"),
            EntityType.Builder.<BanshoCycloneEntity>of(BanshoCycloneEntity::new, MobCategory.MISC)
                    .sized(4.0f, 6.5f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("bansho_cyclone")))
    );

    public static final EntityType<BanshoShockwaveEntity> BANSHO_SHOCKWAVE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("bansho_shockwave"),
            EntityType.Builder.<BanshoShockwaveEntity>of(BanshoShockwaveEntity::new, MobCategory.MISC)
                    .sized(3.0f, 1.5f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("bansho_shockwave")))
    );

    public static final EntityType<BanshoWindProjectileEntity> BANSHO_WIND_PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("bansho_wind_projectile"),
            EntityType.Builder.<BanshoWindProjectileEntity>of(BanshoWindProjectileEntity::new, MobCategory.MISC)
                    .sized(0.8f, 0.8f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("bansho_wind_projectile")))
    );

    public static final EntityType<GrandClashSparkEntity> GRAND_CLASH_SPARK = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("grand_clash_spark"),
            EntityType.Builder.<GrandClashSparkEntity>of(GrandClashSparkEntity::new, MobCategory.MISC)
                    .sized(1.5f, 1.5f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("grand_clash_spark")))
    );

    public static final EntityType<DevilSlamShockwaveEntity> DEVIL_SLAM_SHOCKWAVE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("devil_slam_shockwave"),
            EntityType.Builder.<DevilSlamShockwaveEntity>of(DevilSlamShockwaveEntity::new, MobCategory.MISC)
                    .sized(5.0f, 2.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("devil_slam_shockwave")))
    );

    public static final EntityType<SpiritImpaleEntity> SPIRIT_IMPALE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("spirit_impale"),
            EntityType.Builder.<SpiritImpaleEntity>of(SpiritImpaleEntity::new, MobCategory.MISC)
                    .sized(2.5f, 2.5f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("spirit_impale")))
    );

    public static final EntityType<SpiritCannonBeamEntity> SPIRIT_CANNON_BEAM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("spirit_cannon_beam"),
            EntityType.Builder.<SpiritCannonBeamEntity>of(SpiritCannonBeamEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("spirit_cannon_beam")))
    );

    public static final EntityType<SaberSlashEntity> SABER_SLASH = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("saber_slash"),
            EntityType.Builder.<SaberSlashEntity>of(SaberSlashEntity::new, MobCategory.MISC)
                    .sized(3.0f, 1.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("saber_slash")))
    );

    public static final EntityType<SaberDimensionalLineSlashEntity> SABER_LINE_SLASH = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("saber_line_slash"),
            EntityType.Builder.<SaberDimensionalLineSlashEntity>of(SaberDimensionalLineSlashEntity::new, MobCategory.MISC)
                    .sized(4.0f, 4.0f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("saber_line_slash")))
    );

    public static final EntityType<SaberVoidTearEntity> SABER_VOID_TEAR = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("saber_void_tear"),
            EntityType.Builder.<SaberVoidTearEntity>of(SaberVoidTearEntity::new, MobCategory.MISC)
                    .sized(2.5f, 2.5f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("saber_void_tear")))
    );

    public static final EntityType<SaberDodgeSparkEntity> SABER_DODGE_SPARK = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("saber_dodge_spark"),
            EntityType.Builder.<SaberDodgeSparkEntity>of(SaberDodgeSparkEntity::new, MobCategory.MISC)
                    .sized(2.0f, 2.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("saber_dodge_spark")))
    );

    public static final EntityType<TimeShatterEntity> TIME_SHATTER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("time_shatter"),
            EntityType.Builder.<TimeShatterEntity>of(TimeShatterEntity::new, MobCategory.MISC)
                    .sized(2.5f, 2.5f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("time_shatter")))
    );

    public static final EntityType<TemporalRiftEntity> TEMPORAL_RIFT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("temporal_rift"),
            EntityType.Builder.<TemporalRiftEntity>of(TemporalRiftEntity::new, MobCategory.MISC)
                    .sized(24.0f, 14.0f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("temporal_rift")))
    );

    public static final EntityType<PowerPoleWhirlwindEntity> POWER_POLE_WHIRLWIND = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("power_pole_whirlwind"),
            EntityType.Builder.<PowerPoleWhirlwindEntity>of(PowerPoleWhirlwindEntity::new, MobCategory.MISC)
                    .sized(4.0f, 4.0f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("power_pole_whirlwind")))
    );

    public static final EntityType<PowerPoleExtensionEntity> POWER_POLE_EXTENSION = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("power_pole_extension"),
            EntityType.Builder.<PowerPoleExtensionEntity>of(PowerPoleExtensionEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("power_pole_extension")))
    );

    public static final EntityType<PowerPoleImpactEntity> POWER_POLE_IMPACT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("power_pole_impact"),
            EntityType.Builder.<PowerPoleImpactEntity>of(PowerPoleImpactEntity::new, MobCategory.MISC)
                    .sized(3.0f, 3.0f)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("power_pole_impact")))
    );

    public static final EntityType<ErasureChargeOrbEntity> ERASURE_CHARGE_ORB = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("erasure_charge_orb"),
            EntityType.Builder.<ErasureChargeOrbEntity>of(ErasureChargeOrbEntity::new, MobCategory.MISC)
                    .sized(1.5f, 1.5f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("erasure_charge_orb")))
    );

    public static final EntityType<ErasureCannonBeamEntity> ERASURE_CANNON_BEAM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("erasure_cannon_beam"),
            EntityType.Builder.<ErasureCannonBeamEntity>of(ErasureCannonBeamEntity::new, MobCategory.MISC)
                    .sized(2.0f, 2.0f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("erasure_cannon_beam")))
    );

    public static final EntityType<DimensionalWarpRiftEntity> DIMENSIONAL_WARP_RIFT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("dimensional_warp_rift"),
            EntityType.Builder.<DimensionalWarpRiftEntity>of(DimensionalWarpRiftEntity::new, MobCategory.MISC)
                    .sized(4.0f, 4.0f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("dimensional_warp_rift")))
    );

    public static final EntityType<DarknessChargeEntity> DARKNESS_CHARGE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("darkness_charge"),
            EntityType.Builder.<DarknessChargeEntity>of(DarknessChargeEntity::new, MobCategory.MISC)
                    .sized(2.0f, 2.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("darkness_charge")))
    );

    public static final EntityType<DarknessDomainEntity> DARKNESS_DOMAIN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("darkness_domain"),
            EntityType.Builder.<DarknessDomainEntity>of(DarknessDomainEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("darkness_domain")))
    );

    public static final EntityType<DarknessShatterEntity> DARKNESS_SHATTER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("darkness_shatter"),
            EntityType.Builder.<DarknessShatterEntity>of(DarknessShatterEntity::new, MobCategory.MISC)
                    .sized(4.0f, 1.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("darkness_shatter")))
    );

    public static final EntityType<SwiftCrescentEntity> SWIFT_CRESCENT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("swift_crescent"),
            EntityType.Builder.<SwiftCrescentEntity>of(SwiftCrescentEntity::new, MobCategory.MISC)
                    .sized(2.5f, 1.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("swift_crescent")))
    );

    public static final EntityType<KatanaChargeEntity> KATANA_CHARGE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("katana_charge"),
            EntityType.Builder.<KatanaChargeEntity>of(KatanaChargeEntity::new, MobCategory.MISC)
                    .sized(2.0f, 2.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("katana_charge")))
    );

    public static final EntityType<KatanaAimGuideEntity> KATANA_AIM_GUIDE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("katana_aim_guide"),
            EntityType.Builder.<KatanaAimGuideEntity>of(KatanaAimGuideEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("katana_aim_guide")))
    );

    public static final EntityType<HeavenSplitterEntity> HEAVEN_SPLITTER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("heaven_splitter"),
            EntityType.Builder.<HeavenSplitterEntity>of(HeavenSplitterEntity::new, MobCategory.MISC)
                    .sized(3.0f, 8.0f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("heaven_splitter")))
    );

    public static final EntityType<OxChargeEntity> OX_CHARGE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ox_charge"),
            EntityType.Builder.<OxChargeEntity>of(OxChargeEntity::new, MobCategory.MISC)
                    .sized(3.0f, 3.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ox_charge")))
    );

    public static final EntityType<OxStanceAuraEntity> OX_STANCE_AURA = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ox_stance_aura"),
            EntityType.Builder.<OxStanceAuraEntity>of(OxStanceAuraEntity::new, MobCategory.MISC)
                    .sized(4.0f, 4.0f)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ox_stance_aura")))
    );

    public static final EntityType<KingsSlamEntity> KINGS_SLAM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("kings_slam"),
            EntityType.Builder.<KingsSlamEntity>of(KingsSlamEntity::new, MobCategory.MISC)
                    .sized(5.0f, 3.0f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("kings_slam")))
    );

    public static final EntityType<ZChargeEntity> Z_CHARGE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("z_charge"),
            EntityType.Builder.<ZChargeEntity>of(ZChargeEntity::new, MobCategory.MISC)
                    .sized(3.0f, 3.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("z_charge")))
    );

    public static final EntityType<ZStanceAuraEntity> Z_STANCE_AURA = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("z_stance_aura"),
            EntityType.Builder.<ZStanceAuraEntity>of(ZStanceAuraEntity::new, MobCategory.MISC)
                    .sized(4.0f, 4.0f)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("z_stance_aura")))
    );

    public static final EntityType<ZGravitySlamEntity> Z_GRAVITY_SLAM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("z_gravity_slam"),
            EntityType.Builder.<ZGravitySlamEntity>of(ZGravitySlamEntity::new, MobCategory.MISC)
                    .sized(5.0f, 3.0f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("z_gravity_slam")))
    );

    public static void register() {
        DragonBlockArcaneDBA.LOGGER.info("Registering Entities for " + DragonBlockArcaneDBA.MOD_ID);
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(OTHERWORLD_GUIDE, OtherworldGuideEntity.createMobAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(FLYING_NIMBUS, FlyingNimbusEntity.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(SHENRON, ShenronEntity.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(PORUNGA, PorungaEntity.createAttributes());
    }
}
