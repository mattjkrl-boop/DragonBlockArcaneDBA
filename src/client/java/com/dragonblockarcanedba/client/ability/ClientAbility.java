package com.dragonblockarcanedba.client.ability;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.ability.AbilityRegistry;
import com.dragonblockarcanedba.client.render.ability.ChargeRingRenderer;
import com.dragonblockarcanedba.network.AbilitySyncPayload;
import com.dragonblockarcanedba.network.CastSyncPayload;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

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
        ClientAbilityFx.init();
        com.dragonblockarcanedba.client.fx.DomainFog.init();
        WeaponInputHandler.register();
        ChargeRingRenderer.register();
        registerTooltips();
    }

    /**
     * Data-driven weapon tooltips (Phase 6): right under the item
     * name, one gray line per bound slot, left then right, naming
     * the ability the synced registry binds to that slot. The slot
     * label is a lang string; the ability name comes from the def
     * itself, so a datapack rename renames the tooltip. Unbound
     * items and an unsynced registry add nothing.
     */
    private static void registerTooltips() {
        ItemTooltipCallback.EVENT.register((stack, context, tooltipType, lines) -> {
            if (lines.size() < 1 || stack.isEmpty()) {
                return;
            }
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            List<Component> abilityLines = new ArrayList<>();
            AbilityDef left = AbilityRegistry.forItem(itemId, AbilityDef.Slot.LEFT);
            if (left != null) {
                abilityLines.add(Component.empty()
                        .append(Component.translatable("dba.tooltip.left_click"))
                        .append(Component.literal(": "))
                        .append(Component.literal(left.name()))
                        .withStyle(ChatFormatting.GRAY));
            }
            AbilityDef right = AbilityRegistry.forItem(itemId, AbilityDef.Slot.RIGHT);
            if (right != null) {
                abilityLines.add(Component.empty()
                        .append(Component.translatable("dba.tooltip.right_click"))
                        .append(Component.literal(": "))
                        .append(Component.literal(right.name()))
                        .withStyle(ChatFormatting.GRAY));
            }
            if (!abilityLines.isEmpty()) {
                lines.addAll(1, abilityLines);
            }
        });
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
