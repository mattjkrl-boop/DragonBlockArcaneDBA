package com.dragonblockarcanedba.client;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.client.gui.DbaMenuScreen;
import com.dragonblockarcanedba.network.StatsSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class DragonBlockArcaneDBAClient implements ClientModInitializer {
    public static KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        // Register keybinding to open character stats GUI
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.dragonblockarcanedba.open_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KeyMapping.Category.MISC
        ));

        // Register HUD Overlay and remove vanilla health bar
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.removeElement(net.minecraft.resources.Identifier.parse("minecraft:health_bar"));
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("hud_overlay"), new com.dragonblockarcanedba.client.gui.DbaHudOverlay());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                if (client.player != null) {
                    client.setScreenAndShow(new DbaMenuScreen());
                }
            }
            if (client.level != null) {
                for (net.minecraft.world.entity.player.Player player : client.level.players()) {
                    if (player instanceof net.minecraft.client.player.AbstractClientPlayer clientPlayer) {
                        com.dragonblockarcanedba.client.render.AuraRenderer.renderAura(clientPlayer);
                    }
                }
            }
        });

        // Register client side sync receiver
        ClientPlayNetworking.registerGlobalReceiver(StatsSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player instanceof PlayerStatsAccessor accessor) {
                    CompoundTag nbt = payload.nbtData();
                    accessor.dba$setRaceId(Identifier.parse(nbt.getStringOr("raceId", "dragonblockarcanedba:human")));
                    accessor.dba$setCurrentKi(nbt.getDoubleOr("currentKi", 100.0));
                    accessor.dba$setCurrentStamina(nbt.getDoubleOr("currentStamina", 100.0));
                    accessor.dba$setLevel(nbt.getIntOr("level", 1));
                    accessor.dba$setXp(nbt.getIntOr("xp", 0));
                    accessor.dba$setStatPoints(nbt.getIntOr("ap", 0));
                    accessor.dba$setSkinColor(nbt.getStringOr("skinColor", ""));
                    accessor.dba$setHairColor(nbt.getStringOr("hairColor", ""));

                    CompoundTag stats = nbt.getCompoundOrEmpty("stats");
                    for (String key : stats.keySet()) {
                        int val = stats.getIntOr(key, 0);
                        switch (key) {
                            case "strength" -> accessor.dba$setStrength(val);
                            case "dexterity" -> accessor.dba$setDexterity(val);
                            case "defense" -> accessor.dba$setDefense(val);
                            case "willpower" -> accessor.dba$setWillpower(val);
                            case "spirit" -> accessor.dba$setSpirit(val);
                        }
                    }

                    if (nbt.contains("activeFormId")) {
                        accessor.dba$setActiveFormId(Identifier.parse(nbt.getStringOr("activeFormId", "")));
                    } else {
                        accessor.dba$setActiveFormId(null);
                    }

                    CompoundTag mastery = nbt.getCompoundOrEmpty("mastery");
                    for (String key : mastery.keySet()) {
                        accessor.dba$setFormMastery(Identifier.parse(key), mastery.getDoubleOr(key, 0.0));
                    }
                }
            });
        });

        // Register Space Pod screen opener (S2C)
        ClientPlayNetworking.registerGlobalReceiver(
            com.dragonblockarcanedba.network.SpacePodOpenPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    context.client().setScreenAndShow(
                        new com.dragonblockarcanedba.client.gui.SpacePodScreen()
                    );
                });
            }
        );

        // Register Race Selection screen opener (S2C)
        ClientPlayNetworking.registerGlobalReceiver(
            com.dragonblockarcanedba.network.RaceSelectOpenPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    context.client().setScreenAndShow(
                        new com.dragonblockarcanedba.client.gui.RaceSelectionScreen()
                    );
                });
            }
        );
    }
}
