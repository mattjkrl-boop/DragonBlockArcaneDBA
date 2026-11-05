package com.dragonblockarcanedba.client.ability;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.ability.AbilityRegistry;
import com.dragonblockarcanedba.client.render.ability.ChargeRingRenderer;
import com.dragonblockarcanedba.network.AbilitySyncPayload;
import com.dragonblockarcanedba.network.CastSyncPayload;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.Identifier;

/**
 * Client entry point for the ability rebuild: receives the server's
 * ability definitions (AbilitySyncPayload) and re-parses them with
 * the shared AbilityDef parser into the shared AbilityRegistry, so
 * the client's bindings, carrier parameters, and visual parameters
 * are the server's own data. Also starts the weapon input handler.
 * If no sync has arrived, the registry is empty and everything
 * ability-related fails closed: no visuals, no click payloads.
 */
public final class ClientAbility {
    private ClientAbility() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(AbilitySyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> applySync(payload));
        });
        ClientPlayNetworking.registerGlobalReceiver(CastSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> CastAnimations.onCastSync(payload));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CastAnimations.clear());
        WeaponInputHandler.register();
        ChargeRingRenderer.register();
    }

    private static void applySync(AbilitySyncPayload payload) {
        AbilityRegistry.clear();
        int loaded = 0;
        for (AbilitySyncPayload.Entry entry : payload.defs()) {
            try {
                Identifier id = Identifier.parse(entry.id());
                JsonElement element = JsonParser.parseString(entry.json());
                if (!element.isJsonObject()) {
                    throw new IllegalArgumentException("top level must be a JSON object");
                }
                AbilityDef def = AbilityDef.parse(id, element.getAsJsonObject());
                String error = AbilityRegistry.register(def);
                if (error != null) {
                    DragonBlockArcaneDBA.LOGGER.error("Synced ability {} rejected: {}", entry.id(), error);
                    continue;
                }
                loaded++;
            } catch (Exception e) {
                DragonBlockArcaneDBA.LOGGER.error("Synced ability {} failed to parse: {}", entry.id(), e.getMessage());
            }
        }
        DragonBlockArcaneDBA.LOGGER.info("Client received {} ability definitions", loaded);
    }
}
