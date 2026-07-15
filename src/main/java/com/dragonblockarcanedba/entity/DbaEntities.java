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

    public static void register() {
        DragonBlockArcaneDBA.LOGGER.info("Registering Entities for " + DragonBlockArcaneDBA.MOD_ID);
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(OTHERWORLD_GUIDE, OtherworldGuideEntity.createMobAttributes());
    }
}
