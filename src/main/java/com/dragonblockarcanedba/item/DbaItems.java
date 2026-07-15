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

    public static final Item SPACE_POD = new SpacePodItem(
        new Item.Properties().setId(SPACE_POD_KEY).stacksTo(1)
    );

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, SPACE_POD_KEY, SPACE_POD);
        DragonBlockArcaneDBA.LOGGER.info("Registered DBA items");
    }
}
