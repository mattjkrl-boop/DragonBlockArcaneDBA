package com.dragonblockarcanedba.block;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class DbaBlocks {

    public static final Block YELLOW_CLOUD_BLOCK = registerBlock("yellow_cloud_block",
            new Block(BlockBehaviour.Properties.of()));

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(BuiltInRegistries.BLOCK, DragonBlockArcaneDBA.id(name), block);
    }

    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(BuiltInRegistries.ITEM, DragonBlockArcaneDBA.id(name),
                new BlockItem(block, new Item.Properties()));
    }

    public static void register() {
        DragonBlockArcaneDBA.LOGGER.info("Registering Blocks for " + DragonBlockArcaneDBA.MOD_ID);
    }
}
