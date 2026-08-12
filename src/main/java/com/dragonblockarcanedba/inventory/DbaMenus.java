package com.dragonblockarcanedba.inventory;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class DbaMenus {
    // Note: If we don't need extra data on open, simple MenuType is fine.
    // For GravityTrainingBlockEntity we can just use the BlockEntity lookup via blockpos if needed,
    // or just pass a simple factory. For 26.2 standard container, if we don't need network sync on init:
    
    public static final MenuType<GravityTrainingMenu> GRAVITY_TRAINING = Registry.register(
        BuiltInRegistries.MENU,
        DragonBlockArcaneDBA.id("gravity_training"),
        new MenuType<>((syncId, inventory) -> {
            return new GravityTrainingMenu(syncId, inventory);
        }, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS)
    );

    public static void register() {
        DragonBlockArcaneDBA.LOGGER.info("Registering Menus for " + DragonBlockArcaneDBA.MOD_ID);
    }
}
