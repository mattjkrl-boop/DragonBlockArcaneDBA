package com.dragonblockarcanedba.client.hooks;

import com.dragonblockarcanedba.client.wish.WishRitualVisuals;
import com.dragonblockarcanedba.network.WishRitualStartPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

/**
 * Client entry for the dragon-ball ritual. The wish panel itself stays on the
 * existing menu payload; this only plays the formation and the summon.
 */
public final class WishClient implements ClientModInitializer {
    private static boolean registered;

    @Override
    public void onInitializeClient() {
        init();
    }

    public static void init() {
        if (registered) {
            return;
        }
        registered = true;
        ClientPlayNetworking.registerGlobalReceiver(WishRitualStartPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> WishRitualVisuals.begin(payload));
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(WishRitualVisuals::render);
    }
}
