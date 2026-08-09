package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;

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

    public static final Item SPACE_POD = new SpacePodItem(
        new Item.Properties().setId(SPACE_POD_KEY).stacksTo(1)
    );
    public static final Item BRONZE_COIN = new Item(
        new Item.Properties().setId(BRONZE_COIN_KEY)
    );
    public static final Item BANSHO_FAN = new BanshoFanItem(
        new Item.Properties().setId(BANSHO_FAN_KEY)
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

    public static final CreativeModeTab DBA_TAB = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
        .title(Component.translatable("itemGroup.dragonblockarcanedba.dba_items"))
        .icon(() -> new net.minecraft.world.item.ItemStack(DBA_ORB))
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
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, DBA_TAB_KEY, DBA_TAB);
        DragonBlockArcaneDBA.LOGGER.info("Registered DBA items");
    }
}
