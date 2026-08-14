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
                    .sized(3.0f, 6.0f)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("shenron")))
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

    public static void register() {
        DragonBlockArcaneDBA.LOGGER.info("Registering Entities for " + DragonBlockArcaneDBA.MOD_ID);
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(OTHERWORLD_GUIDE, OtherworldGuideEntity.createMobAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(FLYING_NIMBUS, FlyingNimbusEntity.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(SHENRON, ShenronEntity.createAttributes());
    }
}
