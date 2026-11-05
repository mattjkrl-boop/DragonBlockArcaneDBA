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

    // Generic ability carriers (rebuild Phase 1): one entity type per
    // carrier, parameterized entirely by ability data. No per-weapon
    // entity types will ever be added again.
    public static final EntityType<WaveCarrierEntity> ABILITY_WAVE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ability_wave"),
            EntityType.Builder.<WaveCarrierEntity>of(WaveCarrierEntity::new, MobCategory.MISC)
                    .sized(2.0f, 3.0f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ability_wave")))
    );

    public static final EntityType<CloudCarrierEntity> ABILITY_CLOUD = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ability_cloud"),
            EntityType.Builder.<CloudCarrierEntity>of(CloudCarrierEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(16)
                    .updateInterval(5)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ability_cloud")))
    );

    public static final EntityType<ProjectileCarrierEntity> ABILITY_PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ability_projectile"),
            EntityType.Builder.<ProjectileCarrierEntity>of(ProjectileCarrierEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(8)
                    .updateInterval(5)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ability_projectile")))
    );

    public static final EntityType<BeamCarrierEntity> ABILITY_BEAM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ability_beam"),
            EntityType.Builder.<BeamCarrierEntity>of(BeamCarrierEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ability_beam")))
    );

    public static final EntityType<SwarmHostEntity> ABILITY_SWARM_HOST = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ability_swarm_host"),
            EntityType.Builder.<SwarmHostEntity>of(SwarmHostEntity::new, MobCategory.MISC)
                    .sized(1.2f, 1.2f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ability_swarm_host")))
    );

    public static final EntityType<SwarmShardEntity> ABILITY_SWARM_SHARD = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ability_swarm_shard"),
            EntityType.Builder.<SwarmShardEntity>of(SwarmShardEntity::new, MobCategory.MISC)
                    .sized(0.35f, 0.35f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ability_swarm_shard")))
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

    public static final EntityType<SaibamanEntity> SAIBAMAN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("saibaman"),
            EntityType.Builder.of(SaibamanEntity::new, MobCategory.MONSTER)
                    .sized(0.7f, 1.5f)
                    .clientTrackingRange(8)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("saibaman")))
    );

    public static final EntityType<FriezaSoldierEntity> FRIEZA_SOLDIER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("frieza_soldier"),
            EntityType.Builder.of(FriezaSoldierEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(8)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("frieza_soldier")))
    );

    public static final EntityType<NamekianWarriorEntity> NAMEKIAN_WARRIOR = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("namekian_warrior"),
            EntityType.Builder.of(NamekianWarriorEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 2.3f)
                    .clientTrackingRange(8)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("namekian_warrior")))
    );

    public static final EntityType<PatrolRobotEntity> PATROL_ROBOT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("patrol_robot"),
            EntityType.Builder.of(PatrolRobotEntity::new, MobCategory.CREATURE)
                    .sized(0.7f, 1.9f)
                    .clientTrackingRange(10)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("patrol_robot")))
    );

    public static void register() {
        DragonBlockArcaneDBA.LOGGER.info("Registering Entities for " + DragonBlockArcaneDBA.MOD_ID);
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(OTHERWORLD_GUIDE, OtherworldGuideEntity.createMobAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(FLYING_NIMBUS, FlyingNimbusEntity.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(SHENRON, ShenronEntity.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(PORUNGA, PorungaEntity.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(SAIBAMAN, SaibamanEntity.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(FRIEZA_SOLDIER, FriezaSoldierEntity.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(NAMEKIAN_WARRIOR, NamekianWarriorEntity.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(PATROL_ROBOT, PatrolRobotEntity.createAttributes());
        DbaHostileSpawns.register();
    }
}
