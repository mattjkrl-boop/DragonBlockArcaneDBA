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
    public static final Block YELLOW_CLOUD_BLOCK = new YellowCloudBlock(
        BlockBehaviour.Properties.of()
            .setId(YELLOW_CLOUD_BLOCK_KEY)
            .mapColor(MapColor.COLOR_YELLOW)
            .noCollision()
            .noOcclusion()
            .instabreak()
            .sound(SoundType.WOOL)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false)
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

    public static final ResourceKey<Block> KATCHIN_ORE_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("katchin_ore")
    );
    public static final Block KATCHIN_ORE = new Block(
        BlockBehaviour.Properties.of()
            .setId(KATCHIN_ORE_KEY)
            .mapColor(MapColor.COLOR_GRAY)
            .strength(50.0f, 1200.0f) // Extreme strength (Obsidian level)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    );

    public static final ResourceKey<Block> DRAGSTONE_ORE_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("dragstone_ore")
    );
    public static final Block DRAGSTONE_ORE = new Block(
        BlockBehaviour.Properties.of()
            .setId(DRAGSTONE_ORE_KEY)
            .mapColor(MapColor.COLOR_RED)
            .strength(3.0f, 3.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    );

    public static final ResourceKey<Block> AETHERIUM_ORE_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("aetherium_ore")
    );
    public static final Block AETHERIUM_ORE = new Block(
        BlockBehaviour.Properties.of()
            .setId(AETHERIUM_ORE_KEY)
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(3.0f, 3.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    );

    public static final ResourceKey<Block> BAUXITE_ORE_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("bauxite_ore")
    );
    public static final Block BAUXITE_ORE = new Block(
        BlockBehaviour.Properties.of()
            .setId(BAUXITE_ORE_KEY)
            .mapColor(MapColor.COLOR_BROWN)
            .strength(3.0f, 3.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    );

    public static final ResourceKey<Block> TIN_ORE_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("tin_ore")
    );
    public static final Block TIN_ORE = new Block(
        BlockBehaviour.Properties.of()
            .setId(TIN_ORE_KEY)
            .mapColor(MapColor.COLOR_LIGHT_GRAY)
            .strength(3.0f, 3.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    );

    public static final ResourceKey<Block> SILVER_ORE_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("silver_ore")
    );
    public static final Block SILVER_ORE = new Block(
        BlockBehaviour.Properties.of()
            .setId(SILVER_ORE_KEY)
            .mapColor(MapColor.COLOR_LIGHT_GRAY)
            .strength(3.0f, 3.0f)
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

    public static final ResourceKey<Block> SENZU_PLANT_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("senzu_plant")
    );
    public static final Block SENZU_PLANT = new SenzuPlantBlock(
        BlockBehaviour.Properties.of()
            .setId(SENZU_PLANT_KEY)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.CROP)
    );

    public static final ResourceKey<Block> EARTH_DRAGON_BALL_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("earth_dragon_ball")
    );
    public static final Block EARTH_DRAGON_BALL = new EarthDragonBallBlock(
        BlockBehaviour.Properties.of()
            .setId(EARTH_DRAGON_BALL_KEY)
            .strength(0.5f)
            .sound(SoundType.GLASS)
            .noOcclusion()
    );

    public static final ResourceKey<Block> NAMEKIAN_DRAGON_BALL_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("namekian_dragon_ball")
    );
    public static final Block NAMEKIAN_DRAGON_BALL = new NamekianDragonBallBlock(
        BlockBehaviour.Properties.of()
            .setId(NAMEKIAN_DRAGON_BALL_KEY)
            .strength(0.5f)
            .sound(SoundType.GLASS)
            .noOcclusion()
    );

    public static final ResourceKey<Block> GRAVITY_TRAINING_BLOCK_KEY = ResourceKey.create(
        Registries.BLOCK, DragonBlockArcaneDBA.id("gravity_training_block")
    );
    public static final Block GRAVITY_TRAINING_BLOCK = new GravityTrainingBlock(
        BlockBehaviour.Properties.of()
            .setId(GRAVITY_TRAINING_BLOCK_KEY)
            .mapColor(MapColor.METAL)
            .strength(5.0f, 6.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
    );

    // --- Block Items ---
    public static BlockItem YELLOW_CLOUD_BLOCK_ITEM;
    public static BlockItem NAMEK_GRASS_ITEM;
    public static BlockItem NAMEK_STONE_ITEM;
    public static BlockItem VEGETA_GRASS_ITEM;
    public static BlockItem VEGETA_STONE_ITEM;
    public static BlockItem YARDRAT_GRASS_ITEM;
    public static BlockItem YARDRAT_STONE_ITEM;
    public static BlockItem KATCHIN_ORE_ITEM;
    public static BlockItem DRAGSTONE_ORE_ITEM;
    public static BlockItem AETHERIUM_ORE_ITEM;
    public static BlockItem BAUXITE_ORE_ITEM;
    public static BlockItem TIN_ORE_ITEM;
    public static BlockItem SILVER_ORE_ITEM;

    // --- Registration helpers ---
    private static BlockItem registerBlock(ResourceKey<Block> key, Block block) {
        Registry.register(BuiltInRegistries.BLOCK, key, block);
        // Register a corresponding BlockItem
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, key.identifier());
        BlockItem item = new BlockItem(block, new Item.Properties().setId(itemKey));
        item.registerBlocks(Item.BY_BLOCK, item);
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return item;
    }

    public static void register() {
        DragonBlockArcaneDBA.LOGGER.info("Registering Blocks for " + DragonBlockArcaneDBA.MOD_ID);

        YELLOW_CLOUD_BLOCK_ITEM = registerBlock(YELLOW_CLOUD_BLOCK_KEY, YELLOW_CLOUD_BLOCK);
        NAMEK_GRASS_ITEM = registerBlock(NAMEK_GRASS_KEY, NAMEK_GRASS);
        NAMEK_STONE_ITEM = registerBlock(NAMEK_STONE_KEY, NAMEK_STONE);
        VEGETA_GRASS_ITEM = registerBlock(VEGETA_GRASS_KEY, VEGETA_GRASS);
        VEGETA_STONE_ITEM = registerBlock(VEGETA_STONE_KEY, VEGETA_STONE);
        YARDRAT_GRASS_ITEM = registerBlock(YARDRAT_GRASS_KEY, YARDRAT_GRASS);
        YARDRAT_STONE_ITEM = registerBlock(YARDRAT_STONE_KEY, YARDRAT_STONE);
        KATCHIN_ORE_ITEM = registerBlock(KATCHIN_ORE_KEY, KATCHIN_ORE);
        DRAGSTONE_ORE_ITEM = registerBlock(DRAGSTONE_ORE_KEY, DRAGSTONE_ORE);
        AETHERIUM_ORE_ITEM = registerBlock(AETHERIUM_ORE_KEY, AETHERIUM_ORE);
        BAUXITE_ORE_ITEM = registerBlock(BAUXITE_ORE_KEY, BAUXITE_ORE);
        TIN_ORE_ITEM = registerBlock(TIN_ORE_KEY, TIN_ORE);
        SILVER_ORE_ITEM = registerBlock(SILVER_ORE_KEY, SILVER_ORE);

        Registry.register(BuiltInRegistries.BLOCK, SENZU_PLANT_KEY, SENZU_PLANT);
        Registry.register(BuiltInRegistries.BLOCK, EARTH_DRAGON_BALL_KEY, EARTH_DRAGON_BALL);
        Registry.register(BuiltInRegistries.BLOCK, NAMEKIAN_DRAGON_BALL_KEY, NAMEKIAN_DRAGON_BALL);
        Registry.register(BuiltInRegistries.BLOCK, GRAVITY_TRAINING_BLOCK_KEY, GRAVITY_TRAINING_BLOCK);
    }
}
