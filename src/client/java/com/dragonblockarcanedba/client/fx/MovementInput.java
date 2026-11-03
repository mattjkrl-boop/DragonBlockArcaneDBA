package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.network.C2SMovementPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Client input for the Phase 5 movement layer. Sends intent only; the server
 * validates and applies all physics. Dash has its own key (registered in the
 * client init). Ki step is an edge press of the vanilla jump key while airborne;
 * fast fall is an edge press of the vanilla sneak key while airborne. Verbs stay
 * silent while creative-flying or spectating, matching the server's rejection.
 */
public final class MovementInput {
    private MovementInput() {}

    /** Set from the client init after the key mapping is registered. */
    public static KeyMapping dashKey;

    private static boolean jumpWasDown;
    private static boolean sneakWasDown;

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.gui.screen() != null) {
            jumpWasDown = sneakWasDown = false;
            return;
        }
        if (dashKey != null) {
            while (dashKey.consumeClick()) {
                send(C2SMovementPayload.DASH);
            }
        }
        boolean airborne = !player.onGround() && !player.getAbilities().flying && !player.isSpectator();
        boolean jumpDown = mc.options.keyJump.isDown();
        boolean sneakDown = mc.options.keyShift.isDown();
        if (airborne && jumpDown && !jumpWasDown) send(C2SMovementPayload.KI_STEP);
        if (airborne && sneakDown && !sneakWasDown) send(C2SMovementPayload.FAST_FALL);
        jumpWasDown = jumpDown;
        sneakWasDown = sneakDown;
    }

    private static void send(int action) {
        ClientPlayNetworking.send(new C2SMovementPayload(action));
    }
}
