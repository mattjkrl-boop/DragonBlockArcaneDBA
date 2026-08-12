package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Items;

public class DbaItems {
    public static final ResourceKey<Item> SPACE_POD_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("space_pod")
    );
    public static final ResourceKey<Item> BRONZE_COIN_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("bronze_coin")
    );
    public static final ResourceKey<Item> BANSHO_FAN_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("bansho_fan")
    );
    public static final ResourceKey<Item> DBA_ORB_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("dba_orb")
    );
    public static final ResourceKey<Item> KI_SHARD_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("ki_shard")
    );
    public static final ResourceKey<Item> RECOVERY_CAPSULE_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("recovery_capsule")
    );
    public static final ResourceKey<CreativeModeTab> DBA_TAB_KEY = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB, DragonBlockArcaneDBA.id("dba_items")
    );
    public static final ResourceKey<Item> DROP_RATE_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("drop_rate")
    );
    public static final ResourceKey<Item> DUNGEON_COIN_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("dungeon_coin")
    );
    public static final ResourceKey<Item> FAC_COIN_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("fac_coin")
    );
    public static final ResourceKey<Item> FAC_LOOP_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("fac_loop")
    );
    public static final ResourceKey<Item> FAC_POWER_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("fac_power")
    );
    public static final ResourceKey<Item> T1_COIN_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("t1_coin")
    );
    public static final ResourceKey<Item> T2_COIN_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("t2_coin")
    );
    public static final ResourceKey<Item> TP_COIN_0_1_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("tp_coin_0_1")
    );
    public static final ResourceKey<Item> ORBITAL_COIN_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("orbital_coin")
    );
    public static final ResourceKey<Item> TRADE_COIN_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("trade_coin")
    );
    public static final ResourceKey<Item> GOLD_COIN_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("gold_coin")
    );
    public static final ResourceKey<Item> SPIRIT_SWORD_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("spirit_sword")
    );
    public static final ResourceKey<Item> BLUE_ORB_1_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("blue_orb_1")
    );
    public static final ResourceKey<Item> SILVER_COIN_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("silver_coin")
    );
    public static final ResourceKey<Item> BLUE_ORB_2_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("blue_orb_2")
    );
    public static final ResourceKey<Item> BLUE_KI_ORB_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("blue_ki_orb")
    );
    public static final ResourceKey<Item> MAJIN_MARK_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("majin_mark")
    );
    public static final ResourceKey<Item> XP_AURA_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("xp_aura")
    );
    public static final ResourceKey<Item> DNA_HELIX_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("dna_helix")
    );
    public static final ResourceKey<Item> GOLD_KI_SHARD_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("gold_ki_shard")
    );
    public static final ResourceKey<Item> SICKLE_OF_SORROW_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("sickle_of_sorrow")
    );
    public static final ResourceKey<Item> TIME_COIN_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("time_coin")
    );
    public static final ResourceKey<Item> RED_CROWN_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("red_crown")
    );
    public static final ResourceKey<Item> BLUE_CROWN_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("blue_crown")
    );
    public static final ResourceKey<Item> PURPLE_KI_ORB_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("purple_ki_orb")
    );
    public static final ResourceKey<Item> RED_FRUIT_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("red_fruit")
    );
    public static final ResourceKey<Item> WHIS_STAFF_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("whis_staff")
    );
    public static final ResourceKey<Item> PURPLE_ORB_LARGE_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("purple_orb_large")
    );
    public static final ResourceKey<Item> PURPLE_ORB_SMALL_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("purple_orb_small")
    );
    public static final ResourceKey<Item> RED_TICKETS_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("red_tickets")
    );
    public static final ResourceKey<Item> DBA_LOGO_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("dba_logo")
    );
    public static final ResourceKey<Item> SENZU_BEAN_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("senzu_bean")
    );
    public static final ResourceKey<Item> SENZU_SPROUT_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("senzu_sprout")
    );
    public static final ResourceKey<Item> FLYING_NIMBUS_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("flying_nimbus")
    );
    public static final ResourceKey<Item> EARTH_DRAGON_BALL_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("earth_dragon_ball")
    );
    public static final ResourceKey<Item> SILVER_ZENI_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("silver_zeni")
    );
    public static final ResourceKey<Item> BLOOD_RUBY_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("blood_ruby")
    );
    public static final ResourceKey<Item> GAMMET_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("gammet")
    );
    public static final ResourceKey<Item> DINO_MEAT_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("dino_meat")
    );
    public static final ResourceKey<Item> KATCHIN_SHARD_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("katchin_shard")
    );
    public static final ResourceKey<Item> DRAGSTONE_INGOT_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("dragstone_ingot")
    );
    public static final ResourceKey<Item> AETHERIUM_INGOT_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("aetherium_ingot")
    );
    public static final ResourceKey<Item> BAUXITE_INGOT_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("bauxite_ingot")
    );
    public static final ResourceKey<Item> TIN_INGOT_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("tin_ingot")
    );
    public static final ResourceKey<Item> SILVER_INGOT_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("silver_ingot")
    );
    public static final ResourceKey<Item> STEEL_INGOT_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("steel_ingot")
    );
    public static final ResourceKey<Item> SPIRIT_CRYSTAL_KEY = ResourceKey.create(
        Registries.ITEM, DragonBlockArcaneDBA.id("spirit_crystal")
    );

    public static final Item SPACE_POD = new SpacePodItem(
        new Item.Properties().setId(SPACE_POD_KEY).stacksTo(1)
    );
    public static final Item BRONZE_COIN = new Item(
        new Item.Properties().setId(BRONZE_COIN_KEY)
    );
    public static final Item BANSHO_FAN = new BanshoFanItem(
        new Item.Properties().setId(BANSHO_FAN_KEY).stacksTo(1)
    );
    public static final Item DBA_ORB = new Item(
        new Item.Properties().setId(DBA_ORB_KEY)
    );
    public static final Item KI_SHARD = new Item(
        new Item.Properties().setId(KI_SHARD_KEY)
    );
    public static final Item RECOVERY_CAPSULE = new Item(
        new Item.Properties().setId(RECOVERY_CAPSULE_KEY)
    );

    public static final Item DROP_RATE = new Item(
        new Item.Properties().setId(DROP_RATE_KEY)
    );
    public static final Item DUNGEON_COIN = new Item(
        new Item.Properties().setId(DUNGEON_COIN_KEY)
    );
    public static final Item FAC_COIN = new Item(
        new Item.Properties().setId(FAC_COIN_KEY)
    );
    public static final Item FAC_LOOP = new Item(
        new Item.Properties().setId(FAC_LOOP_KEY)
    );
    public static final Item FAC_POWER = new Item(
        new Item.Properties().setId(FAC_POWER_KEY)
    );
    public static final Item T1_COIN = new Item(
        new Item.Properties().setId(T1_COIN_KEY)
    );
    public static final Item T2_COIN = new Item(
        new Item.Properties().setId(T2_COIN_KEY)
    );
    public static final Item TP_COIN_0_1 = new Item(
        new Item.Properties().setId(TP_COIN_0_1_KEY)
    );
    public static final Item ORBITAL_COIN = new Item(
        new Item.Properties().setId(ORBITAL_COIN_KEY)
    );
    public static final Item TRADE_COIN = new Item(
        new Item.Properties().setId(TRADE_COIN_KEY)
    );
    public static final Item GOLD_COIN = new Item(
        new Item.Properties().setId(GOLD_COIN_KEY)
    );
    public static final Item SPIRIT_SWORD = new SpiritSwordItem(
        new Item.Properties().setId(SPIRIT_SWORD_KEY).stacksTo(1)
    );
    public static final Item BLUE_ORB_1 = new Item(
        new Item.Properties().setId(BLUE_ORB_1_KEY)
    );
    public static final Item SILVER_COIN = new Item(
        new Item.Properties().setId(SILVER_COIN_KEY)
    );
    public static final Item BLUE_ORB_2 = new Item(
        new Item.Properties().setId(BLUE_ORB_2_KEY)
    );
    public static final Item BLUE_KI_ORB = new Item(
        new Item.Properties().setId(BLUE_KI_ORB_KEY)
    );
    public static final Item MAJIN_MARK = new Item(
        new Item.Properties().setId(MAJIN_MARK_KEY)
    );
    public static final Item XP_AURA = new Item(
        new Item.Properties().setId(XP_AURA_KEY)
    );
    public static final Item DNA_HELIX = new Item(
        new Item.Properties().setId(DNA_HELIX_KEY)
    );
    public static final Item GOLD_KI_SHARD = new Item(
        new Item.Properties().setId(GOLD_KI_SHARD_KEY)
    );
    public static final Item SICKLE_OF_SORROW = new SickleOfSorrowItem(
        new Item.Properties().setId(SICKLE_OF_SORROW_KEY).stacksTo(1)
    );
    public static final Item TIME_COIN = new Item(
        new Item.Properties().setId(TIME_COIN_KEY)
    );
    public static final Item RED_CROWN = new Item(
        new Item.Properties().setId(RED_CROWN_KEY)
    );
    public static final Item BLUE_CROWN = new Item(
        new Item.Properties().setId(BLUE_CROWN_KEY)
    );
    public static final Item PURPLE_KI_ORB = new Item(
        new Item.Properties().setId(PURPLE_KI_ORB_KEY)
    );
    public static final Item RED_FRUIT = new Item(
        new Item.Properties().setId(RED_FRUIT_KEY)
    );
    public static final Item WHIS_STAFF = new WhisStaffItem(
        new Item.Properties().setId(WHIS_STAFF_KEY).stacksTo(1)
    );
    public static final Item PURPLE_ORB_LARGE = new Item(
        new Item.Properties().setId(PURPLE_ORB_LARGE_KEY)
    );
    public static final Item PURPLE_ORB_SMALL = new Item(
        new Item.Properties().setId(PURPLE_ORB_SMALL_KEY)
    );
    public static final Item RED_TICKETS = new Item(
        new Item.Properties().setId(RED_TICKETS_KEY)
    );
    public static final Item DBA_LOGO = new Item(
        new Item.Properties().setId(DBA_LOGO_KEY)
    );
    public static final Item SENZU_BEAN = new SenzuBeanItem(
        new Item.Properties().setId(SENZU_BEAN_KEY)
    );
    public static final Item SENZU_SPROUT = new BlockItem(
        com.dragonblockarcanedba.block.DbaBlocks.SENZU_PLANT,
        new Item.Properties().setId(SENZU_SPROUT_KEY)
    );
    public static final Item FLYING_NIMBUS = new FlyingNimbusItem(
        new Item.Properties().setId(FLYING_NIMBUS_KEY).stacksTo(1)
    );
    public static final Item EARTH_DRAGON_BALL = new BlockItem(
        com.dragonblockarcanedba.block.DbaBlocks.EARTH_DRAGON_BALL,
        new Item.Properties().setId(EARTH_DRAGON_BALL_KEY).stacksTo(1)
    );
    public static final Item SILVER_ZENI = new Item(
        new Item.Properties().setId(SILVER_ZENI_KEY)
    );
    public static final Item BLOOD_RUBY = new Item(
        new Item.Properties().setId(BLOOD_RUBY_KEY)
    );
    public static final Item GAMMET = new Item(
        new Item.Properties().setId(GAMMET_KEY)
    );
    public static final Item DINO_MEAT = new DinoMeatItem(
        new Item.Properties().setId(DINO_MEAT_KEY).food(
            new FoodProperties.Builder().nutrition(6).saturationModifier(0.5f).build()
        )
    );
    public static final Item KATCHIN_SHARD = new Item(
        new Item.Properties().setId(KATCHIN_SHARD_KEY)
    );
    public static final Item DRAGSTONE_INGOT = new Item(
        new Item.Properties().setId(DRAGSTONE_INGOT_KEY)
    );
    public static final Item AETHERIUM_INGOT = new Item(
        new Item.Properties().setId(AETHERIUM_INGOT_KEY)
    );
    public static final Item BAUXITE_INGOT = new Item(
        new Item.Properties().setId(BAUXITE_INGOT_KEY)
    );
    public static final Item TIN_INGOT = new Item(
        new Item.Properties().setId(TIN_INGOT_KEY)
    );
    public static final Item SILVER_INGOT = new Item(
        new Item.Properties().setId(SILVER_INGOT_KEY)
    );
    public static final Item STEEL_INGOT = new Item(
        new Item.Properties().setId(STEEL_INGOT_KEY)
    );
    public static final Item SPIRIT_CRYSTAL = new Item(
        new Item.Properties().setId(SPIRIT_CRYSTAL_KEY)
    );

    public static final CreativeModeTab DBA_TAB = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
        .title(Component.translatable("itemGroup.dragonblockarcanedba.dba_items"))
        .icon(() -> new net.minecraft.world.item.ItemStack(DBA_LOGO))
        .displayItems((itemDisplayParameters, output) -> {
            output.accept(SPACE_POD);
            output.accept(BRONZE_COIN);
            output.accept(BANSHO_FAN);
            output.accept(DBA_ORB);
            output.accept(KI_SHARD);
            output.accept(RECOVERY_CAPSULE);
            output.accept(DROP_RATE);
            output.accept(DUNGEON_COIN);
            output.accept(FAC_COIN);
            output.accept(FAC_LOOP);
            output.accept(FAC_POWER);
            output.accept(T1_COIN);
            output.accept(T2_COIN);
            output.accept(TP_COIN_0_1);
            output.accept(ORBITAL_COIN);
            output.accept(TRADE_COIN);
            output.accept(GOLD_COIN);
            output.accept(SPIRIT_SWORD);
            output.accept(BLUE_ORB_1);
            output.accept(SILVER_COIN);
            output.accept(BLUE_ORB_2);
            output.accept(BLUE_KI_ORB);
            output.accept(MAJIN_MARK);
            output.accept(XP_AURA);
            output.accept(DNA_HELIX);
            output.accept(GOLD_KI_SHARD);
            output.accept(SICKLE_OF_SORROW);
            output.accept(TIME_COIN);
            output.accept(RED_CROWN);
            output.accept(BLUE_CROWN);
            output.accept(PURPLE_KI_ORB);
            output.accept(RED_FRUIT);
            output.accept(WHIS_STAFF);
            output.accept(PURPLE_ORB_LARGE);
            output.accept(PURPLE_ORB_SMALL);
            output.accept(RED_TICKETS);
            output.accept(DBA_LOGO);
            output.accept(SENZU_BEAN);
            output.accept(SENZU_SPROUT);
            output.accept(FLYING_NIMBUS);
            output.accept(EARTH_DRAGON_BALL);
            output.accept(SILVER_ZENI);
            output.accept(BLOOD_RUBY);
            output.accept(GAMMET);
            output.accept(DINO_MEAT);
            output.accept(KATCHIN_SHARD);
            output.accept(DRAGSTONE_INGOT);
            output.accept(AETHERIUM_INGOT);
            output.accept(BAUXITE_INGOT);
            output.accept(TIN_INGOT);
            output.accept(SILVER_INGOT);
            output.accept(STEEL_INGOT);
            output.accept(SPIRIT_CRYSTAL);
            output.accept(com.dragonblockarcanedba.block.DbaBlocks.KATCHIN_ORE);
            output.accept(com.dragonblockarcanedba.block.DbaBlocks.DRAGSTONE_ORE);
            output.accept(com.dragonblockarcanedba.block.DbaBlocks.AETHERIUM_ORE);
            output.accept(com.dragonblockarcanedba.block.DbaBlocks.BAUXITE_ORE);
            output.accept(com.dragonblockarcanedba.block.DbaBlocks.TIN_ORE);
            output.accept(com.dragonblockarcanedba.block.DbaBlocks.SILVER_ORE);
        })
        .build();

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, SPACE_POD_KEY, SPACE_POD);
        Registry.register(BuiltInRegistries.ITEM, BRONZE_COIN_KEY, BRONZE_COIN);
        Registry.register(BuiltInRegistries.ITEM, BANSHO_FAN_KEY, BANSHO_FAN);
        Registry.register(BuiltInRegistries.ITEM, DBA_ORB_KEY, DBA_ORB);
        Registry.register(BuiltInRegistries.ITEM, KI_SHARD_KEY, KI_SHARD);
        Registry.register(BuiltInRegistries.ITEM, RECOVERY_CAPSULE_KEY, RECOVERY_CAPSULE);
        Registry.register(BuiltInRegistries.ITEM, DROP_RATE_KEY, DROP_RATE);
        Registry.register(BuiltInRegistries.ITEM, DUNGEON_COIN_KEY, DUNGEON_COIN);
        Registry.register(BuiltInRegistries.ITEM, FAC_COIN_KEY, FAC_COIN);
        Registry.register(BuiltInRegistries.ITEM, FAC_LOOP_KEY, FAC_LOOP);
        Registry.register(BuiltInRegistries.ITEM, FAC_POWER_KEY, FAC_POWER);
        Registry.register(BuiltInRegistries.ITEM, T1_COIN_KEY, T1_COIN);
        Registry.register(BuiltInRegistries.ITEM, T2_COIN_KEY, T2_COIN);
        Registry.register(BuiltInRegistries.ITEM, TP_COIN_0_1_KEY, TP_COIN_0_1);
        Registry.register(BuiltInRegistries.ITEM, ORBITAL_COIN_KEY, ORBITAL_COIN);
        Registry.register(BuiltInRegistries.ITEM, TRADE_COIN_KEY, TRADE_COIN);
        Registry.register(BuiltInRegistries.ITEM, GOLD_COIN_KEY, GOLD_COIN);
        Registry.register(BuiltInRegistries.ITEM, SPIRIT_SWORD_KEY, SPIRIT_SWORD);
        Registry.register(BuiltInRegistries.ITEM, BLUE_ORB_1_KEY, BLUE_ORB_1);
        Registry.register(BuiltInRegistries.ITEM, SILVER_COIN_KEY, SILVER_COIN);
        Registry.register(BuiltInRegistries.ITEM, BLUE_ORB_2_KEY, BLUE_ORB_2);
        Registry.register(BuiltInRegistries.ITEM, BLUE_KI_ORB_KEY, BLUE_KI_ORB);
        Registry.register(BuiltInRegistries.ITEM, MAJIN_MARK_KEY, MAJIN_MARK);
        Registry.register(BuiltInRegistries.ITEM, XP_AURA_KEY, XP_AURA);
        Registry.register(BuiltInRegistries.ITEM, DNA_HELIX_KEY, DNA_HELIX);
        Registry.register(BuiltInRegistries.ITEM, GOLD_KI_SHARD_KEY, GOLD_KI_SHARD);
        Registry.register(BuiltInRegistries.ITEM, SICKLE_OF_SORROW_KEY, SICKLE_OF_SORROW);
        Registry.register(BuiltInRegistries.ITEM, TIME_COIN_KEY, TIME_COIN);
        Registry.register(BuiltInRegistries.ITEM, RED_CROWN_KEY, RED_CROWN);
        Registry.register(BuiltInRegistries.ITEM, BLUE_CROWN_KEY, BLUE_CROWN);
        Registry.register(BuiltInRegistries.ITEM, PURPLE_KI_ORB_KEY, PURPLE_KI_ORB);
        Registry.register(BuiltInRegistries.ITEM, RED_FRUIT_KEY, RED_FRUIT);
        Registry.register(BuiltInRegistries.ITEM, WHIS_STAFF_KEY, WHIS_STAFF);
        Registry.register(BuiltInRegistries.ITEM, PURPLE_ORB_LARGE_KEY, PURPLE_ORB_LARGE);
        Registry.register(BuiltInRegistries.ITEM, PURPLE_ORB_SMALL_KEY, PURPLE_ORB_SMALL);
        Registry.register(BuiltInRegistries.ITEM, RED_TICKETS_KEY, RED_TICKETS);
        Registry.register(BuiltInRegistries.ITEM, DBA_LOGO_KEY, DBA_LOGO);
        Registry.register(BuiltInRegistries.ITEM, SENZU_BEAN_KEY, SENZU_BEAN);
        Registry.register(BuiltInRegistries.ITEM, SENZU_SPROUT_KEY, SENZU_SPROUT);
        Registry.register(BuiltInRegistries.ITEM, FLYING_NIMBUS_KEY, FLYING_NIMBUS);
        Registry.register(BuiltInRegistries.ITEM, EARTH_DRAGON_BALL_KEY, EARTH_DRAGON_BALL);
        Registry.register(BuiltInRegistries.ITEM, SILVER_ZENI_KEY, SILVER_ZENI);
        Registry.register(BuiltInRegistries.ITEM, BLOOD_RUBY_KEY, BLOOD_RUBY);
        Registry.register(BuiltInRegistries.ITEM, GAMMET_KEY, GAMMET);
        Registry.register(BuiltInRegistries.ITEM, DINO_MEAT_KEY, DINO_MEAT);
        Registry.register(BuiltInRegistries.ITEM, KATCHIN_SHARD_KEY, KATCHIN_SHARD);
        Registry.register(BuiltInRegistries.ITEM, DRAGSTONE_INGOT_KEY, DRAGSTONE_INGOT);
        Registry.register(BuiltInRegistries.ITEM, AETHERIUM_INGOT_KEY, AETHERIUM_INGOT);
        Registry.register(BuiltInRegistries.ITEM, BAUXITE_INGOT_KEY, BAUXITE_INGOT);
        Registry.register(BuiltInRegistries.ITEM, TIN_INGOT_KEY, TIN_INGOT);
        Registry.register(BuiltInRegistries.ITEM, SILVER_INGOT_KEY, SILVER_INGOT);
        Registry.register(BuiltInRegistries.ITEM, STEEL_INGOT_KEY, STEEL_INGOT);
        Registry.register(BuiltInRegistries.ITEM, SPIRIT_CRYSTAL_KEY, SPIRIT_CRYSTAL);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, DBA_TAB_KEY, DBA_TAB);
        DragonBlockArcaneDBA.LOGGER.info("Registered DBA items");
    }
}
