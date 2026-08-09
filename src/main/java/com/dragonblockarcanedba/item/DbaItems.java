package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

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

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, SPACE_POD_KEY, SPACE_POD);
        Registry.register(BuiltInRegistries.ITEM, BRONZE_COIN_KEY, BRONZE_COIN);
        Registry.register(BuiltInRegistries.ITEM, BANSHO_FAN_KEY, BANSHO_FAN);
        Registry.register(BuiltInRegistries.ITEM, DBA_ORB_KEY, DBA_ORB);
        Registry.register(BuiltInRegistries.ITEM, KI_SHARD_KEY, KI_SHARD);
        Registry.register(BuiltInRegistries.ITEM, RECOVERY_CAPSULE_KEY, RECOVERY_CAPSULE);
        DragonBlockArcaneDBA.LOGGER.info("Registered DBA items");
    }
}
