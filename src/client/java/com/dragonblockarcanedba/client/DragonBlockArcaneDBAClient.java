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
    public static KeyMapping techSlot1Key;
    public static KeyMapping techSlot2Key;
    public static KeyMapping techSlot3Key;

    public static final net.minecraft.client.model.geom.ModelLayerLocation SHENRON_MODEL_LAYER = new net.minecraft.client.model.geom.ModelLayerLocation(
        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("shenron"), "main"
    );

    @Override
    public void onInitializeClient() {
        // Register Entity Renderers
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.OTHERWORLD_GUIDE,
            com.dragonblockarcanedba.client.render.OtherworldGuideRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.FLYING_NIMBUS,
            com.dragonblockarcanedba.client.render.FlyingNimbusRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(

            com.dragonblockarcanedba.entity.DbaEntities.KI_BLAST,
            com.dragonblockarcanedba.client.render.ki.KiBlastRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.KI_BEAM,
            com.dragonblockarcanedba.client.render.ki.KiBeamRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.KI_DISK,
            com.dragonblockarcanedba.client.render.ki.KiDiskRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.KI_LASER,
            com.dragonblockarcanedba.client.render.ki.KiLaserRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.KI_SPIRAL_BEAM,
            com.dragonblockarcanedba.client.render.ki.KiSpiralBeamRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.KI_EXPLOSION,
            com.dragonblockarcanedba.client.render.ki.KiExplosionRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.SHENRON,
            com.dragonblockarcanedba.client.render.ShenronRenderer::new
        );

        // Register model layers
        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
            SHENRON_MODEL_LAYER, com.dragonblockarcanedba.client.model.ShenronModel::createBodyLayer

        );



        // Load persisted config from disk
        com.dragonblockarcanedba.client.config.DbaConfig.load();
        
        // Register keybinding to open character stats GUI
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.dragonblockarcanedba.open_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KeyMapping.Category.MISC
        ));
        techSlot1Key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.dragonblockarcanedba.tech_slot_1",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F7,
            KeyMapping.Category.MISC
        ));
        techSlot2Key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.dragonblockarcanedba.tech_slot_2",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            KeyMapping.Category.MISC
        ));
        techSlot3Key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.dragonblockarcanedba.tech_slot_3",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
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
            while (techSlot1Key.consumeClick()) {
                if (client.player != null) ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SKiTechniqueFirePayload(0));
            }
            while (techSlot2Key.consumeClick()) {
                if (client.player != null) ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SKiTechniqueFirePayload(1));
            }
            while (techSlot3Key.consumeClick()) {
                if (client.player != null) ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SKiTechniqueFirePayload(2));
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
                            case "vitality" -> accessor.dba$setVitality(val);
                        }
                    }

                    CompoundTag statUpgrades = nbt.getCompoundOrEmpty("statUpgrades");
                    for (String key : statUpgrades.keySet()) {
                        int val = statUpgrades.getIntOr(key, 0);
                        accessor.dba$setStatUpgradeCount(key, val);
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
                    
                    CompoundTag techUnlocked = nbt.getCompoundOrEmpty("unlockedTechniques");
                    for (String key : techUnlocked.keySet()) {
                        accessor.dba$setTechniqueUnlocked(key, techUnlocked.getBooleanOr(key, false));
                    }
                    
                    CompoundTag techActive = nbt.getCompoundOrEmpty("activeTechniques");
                    for (String key : techActive.keySet()) {
                        accessor.dba$setTechniqueActive(key, techActive.getBooleanOr(key, false));
                    }

                    CompoundTag equipNbt = nbt.getCompoundOrEmpty("equippedTechniques");
                    for (int i = 0; i < 3; i++) {
                        accessor.dba$setEquippedTechnique(i, equipNbt.getStringOr("slot" + i, ""));
                    }
                    
                    CompoundTag kiTechNbt = nbt.getCompoundOrEmpty("kiTechniqueSlots");
                    for (int i = 0; i < 3; i++) {
                        if (kiTechNbt.getBooleanOr("slot" + i + "_empty", false)) {
                            accessor.dba$setKiTechniqueSlot(i, com.dragonblockarcanedba.ki.KiTechnique.EMPTY);
                        } else {
                            String typeStr = kiTechNbt.getStringOr("slot" + i + "_type", "");
                            if (!typeStr.isEmpty()) {
                                com.dragonblockarcanedba.ki.KiTechniqueType type = com.dragonblockarcanedba.ki.KiTechniqueType.fromString(typeStr);
                                int pct = kiTechNbt.getIntOr("slot" + i + "_pct", 50);
                                int color = kiTechNbt.getIntOr("slot" + i + "_color", 0xFF00AAFF);
                                boolean barrage = kiTechNbt.getBooleanOr("slot" + i + "_barrage", false);
                                accessor.dba$setKiTechniqueSlot(i, new com.dragonblockarcanedba.ki.KiTechnique(type, pct, color, barrage));
                            }
                        }
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

        // Register GUI
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
            com.dragonblockarcanedba.network.RaceSelectOpenPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    context.client().setScreenAndShow(new com.dragonblockarcanedba.client.gui.RaceSelectionScreen());
                });
            }
        );
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
            com.dragonblockarcanedba.network.ReviveUiOpenPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    context.client().setScreenAndShow(new com.dragonblockarcanedba.client.gui.ReviveScreen());
                });
            }
        );
        // Register Wish Menu screen opener (S2C)
        ClientPlayNetworking.registerGlobalReceiver(
            com.dragonblockarcanedba.network.WishMenuOpenPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    context.client().setScreenAndShow(
                        new com.dragonblockarcanedba.client.gui.WishScreen(payload.shenronId())
                    );
                });
            }
        );
        // Register Transform Broadcast receiver (S2C) — for multiplayer aura sync
        ClientPlayNetworking.registerGlobalReceiver(
            com.dragonblockarcanedba.network.TransformBroadcastPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    if (context.client().level != null) {
                        net.minecraft.world.entity.Entity entity = context.client().level.getEntity(payload.entityId());
                        if (entity instanceof PlayerStatsAccessor accessor) {
                            String formStr = payload.activeFormId();
                            if (formStr.isEmpty()) {
                                accessor.dba$setActiveFormId(null);
                            } else {
                                accessor.dba$setActiveFormId(Identifier.parse(formStr));
                            }
                            String raceStr = payload.raceId();
                            if (!raceStr.isEmpty()) {
                                accessor.dba$setRaceId(Identifier.parse(raceStr));
                            }
                        }
                    }
                });
            }
        );
    }
}
