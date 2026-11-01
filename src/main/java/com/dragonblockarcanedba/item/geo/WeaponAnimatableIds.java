package com.dragonblockarcanedba.item.geo;

import com.dragonblockarcanedba.item.DbaItems;
import com.geckolib.GeckoLibConstants;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.SingletonGeoAnimatable;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Server side identity for weapon animation. Registers every DbaGeoWeapon item as a synced
 * animatable and assigns a durable GeckoLib animatable id to each weapon stack found in a
 * player inventory (checked once a second). The id rides the stack's data components, so it
 * is the same on every client, and each client keys that stack's animation managers to it.
 * Without this, GeckoLib falls back to a stack hashCode id and per-holder animation state
 * cannot be told apart reliably in multiplayer.
 */
public final class WeaponAnimatableIds {
    private WeaponAnimatableIds() {}

    private static final Item[] WEAPONS = {
        DbaItems.AZURE_DRAGON_SWORD, DbaItems.BANSHO_FAN, DbaItems.BLASTER_GUN,
        DbaItems.BRAVE_SWORD, DbaItems.CURSED_BLADE, DbaItems.DABURA_SWORD,
        DbaItems.DEVIL_TRIDENT, DbaItems.DIMENSIONAL_SWORD, DbaItems.EVIL_SPEAR,
        DbaItems.GRAND_SWORD, DbaItems.HOLLOWS_EDGE, DbaItems.KATANA,
        DbaItems.OX_KINGS_AX, DbaItems.POWER_POLE, DbaItems.SABER,
        DbaItems.SICKLE_OF_SORROW, DbaItems.SPIRIT_SWORD, DbaItems.WHIS_STAFF,
        DbaItems.Z_SWORD
    };

    private static int tick;

    public static void register() {
        for (Item item : WEAPONS) {
            if (item instanceof SingletonGeoAnimatable animatable) {
                GeoItem.registerSyncedAnimatable(animatable);
            }
        }
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tick % 20 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                assignInInventory(player);
            }
        });
    }

    private static void assignInInventory(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof DbaGeoWeapon
                && stack.get(GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get()) == null) {
                GeoItem.getOrAssignId(stack, level);
            }
        }
    }
}
