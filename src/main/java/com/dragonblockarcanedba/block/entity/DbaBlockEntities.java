package com.dragonblockarcanedba.block.entity;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.block.DbaBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class DbaBlockEntities {

    public static final BlockEntityType<GravityTrainingBlockEntity> GRAVITY_TRAINING_BLOCK_ENTITY =
        Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            DragonBlockArcaneDBA.id("gravity_training_block_entity"),
            new BlockEntityType<>(GravityTrainingBlockEntity::new, java.util.Set.of(DbaBlocks.GRAVITY_TRAINING_BLOCK))
        );

    public static void register() {
        DragonBlockArcaneDBA.LOGGER.info("Registering Block Entities for " + DragonBlockArcaneDBA.MOD_ID);
        // Registration is static, loading the class triggers it.
    }
}
