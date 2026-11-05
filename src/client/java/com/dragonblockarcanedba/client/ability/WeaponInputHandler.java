package com.dragonblockarcanedba.client.ability;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.ability.AbilityRegistry;
import com.dragonblockarcanedba.network.C2SWeaponClickPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Watches the attack and use keys while the main hand holds a weapon
 * the synced bindings give abilities to, and turns key edges into
 * C2SWeaponClickPayload traffic: press on the rising edge, release
 * with the counted hold ticks on the falling edge. It knows nothing
 * about any specific weapon or ability; the binding lookup is the
 * whole intelligence. Silent in screens and menus; if a screen opens
 * mid-hold, the pending release is sent first so a server-side
 * charge never dangles.
 */
public final class WeaponInputHandler {
    private WeaponInputHandler() {}

    private static boolean attackDown;
    private static boolean useDown;
    private static boolean attackSent;
    private static boolean useSent;
    private static int attackHeld;
    private static int useHeld;
    private static Identifier lastItem;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(WeaponInputHandler::tick);
    }

    private static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            reset();
            return;
        }
        if (client.gui.screen() != null) {
            // Release anything held so a charge cannot dangle behind a menu.
            if (attackSent) {
                send(C2SWeaponClickPayload.BUTTON_LEFT, C2SWeaponClickPayload.ACTION_RELEASE, attackHeld);
            }
            if (useSent) {
                send(C2SWeaponClickPayload.BUTTON_RIGHT, C2SWeaponClickPayload.ACTION_RELEASE, useHeld);
            }
            reset();
            return;
        }

        ItemStack stack = client.player.getMainHandItem();
        Identifier itemId = stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null || !itemId.equals(lastItem)) {
            // Weapon changed: drop local edge state. The server cancels
            // any orphaned charge on its own held-item check.
            attackDown = useDown = false;
            attackSent = useSent = false;
            attackHeld = useHeld = 0;
            lastItem = itemId;
            if (itemId == null) {
                return;
            }
        }

        boolean leftBound = AbilityRegistry.binding(itemId, AbilityDef.Slot.LEFT) != null;
        boolean rightBound = AbilityRegistry.binding(itemId, AbilityDef.Slot.RIGHT) != null;

        boolean atk = leftBound && client.options.keyAttack.isDown();
        if (atk && !attackDown) {
            send(C2SWeaponClickPayload.BUTTON_LEFT, C2SWeaponClickPayload.ACTION_PRESS, 0);
            attackSent = true;
            attackHeld = 0;
        }
        if (atk) {
            attackHeld++;
        }
        if (!atk && attackDown && attackSent) {
            send(C2SWeaponClickPayload.BUTTON_LEFT, C2SWeaponClickPayload.ACTION_RELEASE, attackHeld);
            attackSent = false;
        }
        attackDown = atk;

        boolean use = rightBound && client.options.keyUse.isDown();
        if (use && !useDown) {
            send(C2SWeaponClickPayload.BUTTON_RIGHT, C2SWeaponClickPayload.ACTION_PRESS, 0);
            useSent = true;
            useHeld = 0;
        }
        if (use) {
            useHeld++;
        }
        if (!use && useDown && useSent) {
            send(C2SWeaponClickPayload.BUTTON_RIGHT, C2SWeaponClickPayload.ACTION_RELEASE, useHeld);
            useSent = false;
        }
        useDown = use;
    }

    private static void reset() {
        attackDown = useDown = false;
        attackSent = useSent = false;
        attackHeld = useHeld = 0;
        lastItem = null;
    }

    private static void send(int button, int action, int heldTicks) {
        if (ClientPlayNetworking.canSend(C2SWeaponClickPayload.TYPE)) {
            ClientPlayNetworking.send(new C2SWeaponClickPayload(button, action, heldTicks));
        }
    }
}
