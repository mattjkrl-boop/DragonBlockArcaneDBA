package com.dragonblockarcanedba.block;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class DbaBlocks {

    // --- Otherworld ---
    public static final ResourceKey<Block> YELLOW_CLOUD_BLOCK_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("yellow_cloud_block")
    );
    public static final Block YELLOW_CLOUD_BLOCK = new Block(
        BlockBehaviour.Properties.of()
            .setId(YELLOW_CLOUD_BLOCK_KEY)
            .mapColor(MapColor.COLOR_YELLOW)
            .strength(0.2f)
            .sound(SoundType.WOOL)
    );

    // --- Planet Namek ---
    public static final ResourceKey<Block> NAMEK_GRASS_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("namek_grass")
    );
    public static final Block NAMEK_GRASS = new Block(
        BlockBehaviour.Properties.of()
            .setId(NAMEK_GRASS_KEY)
            .mapColor(MapColor.COLOR_CYAN)
            .strength(0.6f)
            .sound(SoundType.GRASS)
    );

    public static final ResourceKey<Block> NAMEK_STONE_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("namek_stone")
    );
    public static final Block NAMEK_STONE = new Block(
        BlockBehaviour.Properties.of()
            .setId(NAMEK_STONE_KEY)
            .mapColor(MapColor.COLOR_GREEN)
            .strength(1.5f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    );

    // --- Planet Vegeta ---
    public static final ResourceKey<Block> VEGETA_GRASS_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("vegeta_grass")
    );
    public static final Block VEGETA_GRASS = new Block(
        BlockBehaviour.Properties.of()
            .setId(VEGETA_GRASS_KEY)
            .mapColor(MapColor.COLOR_RED)
            .strength(0.6f)
            .sound(SoundType.GRASS)
    );

    public static final ResourceKey<Block> VEGETA_STONE_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("vegeta_stone")
    );
    public static final Block VEGETA_STONE = new Block(
        BlockBehaviour.Properties.of()
            .setId(VEGETA_STONE_KEY)
            .mapColor(MapColor.TERRACOTTA_RED)
            .strength(1.5f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    );

    // --- Planet Yardrat ---
    public static final ResourceKey<Block> YARDRAT_GRASS_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("yardrat_grass")
    );
    public static final Block YARDRAT_GRASS = new Block(
        BlockBehaviour.Properties.of()
            .setId(YARDRAT_GRASS_KEY)
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(0.6f)
            .sound(SoundType.GRASS)
    );

    public static final ResourceKey<Block> YARDRAT_STONE_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("yardrat_stone")
    );
    public static final Block YARDRAT_STONE = new Block(
        BlockBehaviour.Properties.of()
            .setId(YARDRAT_STONE_KEY)
            .mapColor(MapColor.COLOR_LIGHT_BLUE)
            .strength(1.5f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    );

    // --- Registration helpers ---
    private static void registerBlock(ResourceKey<Block> key, Block block) {
        Registry.register(BuiltInRegistries.BLOCK, key, block);
        // Register a corresponding BlockItem
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, key.identifier());
        Registry.register(BuiltInRegistries.ITEM, itemKey,
            new BlockItem(block, new Item.Properties().setId(itemKey)));
    }

    public static void register() {
        DragonBlockArcaneDBA.LOGGER.info("Registering Blocks for " + DragonBlockArcaneDBA.MOD_ID);

        registerBlock(YELLOW_CLOUD_BLOCK_KEY, YELLOW_CLOUD_BLOCK);
        registerBlock(NAMEK_GRASS_KEY, NAMEK_GRASS);
        registerBlock(NAMEK_STONE_KEY, NAMEK_STONE);
        registerBlock(VEGETA_GRASS_KEY, VEGETA_GRASS);
        registerBlock(VEGETA_STONE_KEY, VEGETA_STONE);
        registerBlock(YARDRAT_GRASS_KEY, YARDRAT_GRASS);
        registerBlock(YARDRAT_STONE_KEY, YARDRAT_STONE);
    }
}
