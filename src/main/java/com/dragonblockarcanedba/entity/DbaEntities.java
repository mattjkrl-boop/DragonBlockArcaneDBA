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

    public static void register() {
        DragonBlockArcaneDBA.LOGGER.info("Registering Entities for " + DragonBlockArcaneDBA.MOD_ID);
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(OTHERWORLD_GUIDE, OtherworldGuideEntity.createMobAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(FLYING_NIMBUS, FlyingNimbusEntity.createAttributes());
    }
}
