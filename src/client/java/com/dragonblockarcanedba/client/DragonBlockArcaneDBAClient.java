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
    public static KeyMapping openEmoteMenuKey;
    public static KeyMapping techSlot1Key;
    public static KeyMapping techSlot2Key;
    public static KeyMapping techSlot3Key;

    /** Ticks the ki slot key has been held during the current charge. */
    private static final int[] kiSlotHold = new int[3];
    /** The charged shot for this hold was already sent. */
    private static final boolean[] kiSlotFired = new boolean[3];



    @Override
    public void onInitializeClient() {
        com.dragonblockarcanedba.client.dev.DevPreviewCamera.register();
        com.dragonblockarcanedba.client.render.geo.DbaGeoWeaponRenderers.init();
        com.dragonblockarcanedba.client.render.geo.DbaDragonRenderers.init();
        net.minecraft.client.gui.screens.MenuScreens.register(
            com.dragonblockarcanedba.inventory.DbaMenus.GRAVITY_TRAINING,
            com.dragonblockarcanedba.client.gui.GravityTrainingScreen::new
        );

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
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            com.dragonblockarcanedba.entity.DbaEntities.SHENRON,
            ctx -> com.dragonblockarcanedba.client.render.geo.DbaDragonRenderers.create(ctx, "shenron")
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            com.dragonblockarcanedba.entity.DbaEntities.PORUNGA,
            ctx -> com.dragonblockarcanedba.client.render.geo.DbaDragonRenderers.create(ctx, "porunga")
        );


        net.minecraft.client.renderer.special.SpecialModelRenderers.ID_MAPPER.put(
            com.dragonblockarcanedba.DragonBlockArcaneDBA.id("procedural_weapon"),
            com.dragonblockarcanedba.client.render.item.ProceduralWeaponUnbakedModel.CODEC
        );


        // Register model layers

        // Load persisted config from disk
        com.dragonblockarcanedba.client.config.DbaConfig.load();

        // Register keybinding to open character stats GUI
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.dragonblockarcanedba.open_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KeyMapping.Category.MISC
        ));
        openEmoteMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.dragonblockarcanedba.open_emote_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
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

        ClientPlayNetworking.registerGlobalReceiver(com.dragonblockarcanedba.network.ScreenShakePayload.TYPE, (payload, context) ->
            {
                com.dragonblockarcanedba.client.fx.ScreenShakeHandler.trigger(payload.intensity(), payload.durationTicks(), payload.hitstopTicks());
                com.dragonblockarcanedba.client.fx.DbaPostFx.onShake(payload.intensity());
            });
        ClientPlayNetworking.registerGlobalReceiver(com.dragonblockarcanedba.network.AtmospherePayload.TYPE, (payload, context) ->
            {
                com.dragonblockarcanedba.client.fx.AtmosphereClient.accept(payload);
            });
        ClientTickEvents.END_CLIENT_TICK.register(client -> com.dragonblockarcanedba.client.fx.ScreenShakeHandler.tick());
        ClientTickEvents.END_CLIENT_TICK.register(com.dragonblockarcanedba.client.fx.AtmosphereClient::tick);
        ClientTickEvents.END_CLIENT_TICK.register(com.dragonblockarcanedba.client.fx.DbaPostFx::tick);
        com.dragonblockarcanedba.client.fx.MovementInput.dashKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.dragonblockarcanedba.dash",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KeyMapping.Category.MISC
        ));
        ClientTickEvents.END_CLIENT_TICK.register(com.dragonblockarcanedba.client.fx.MovementInput::tick);
        com.dragonblockarcanedba.client.fx.TumbleFx.register();
        ClientTickEvents.END_CLIENT_TICK.register(com.dragonblockarcanedba.client.fx.TumbleFx::tick);
        ClientTickEvents.END_CLIENT_TICK.register(com.dragonblockarcanedba.client.fx.DamageDirectionFx::tick);
        com.dragonblockarcanedba.client.fx.StaggerFx.register();
        ClientTickEvents.END_CLIENT_TICK.register(com.dragonblockarcanedba.client.fx.StaggerFx::tick);
        com.dragonblockarcanedba.client.particle.DbaParticleProviders.register();
        com.dragonblockarcanedba.client.fx.ScreenShakeHandler.scale = com.dragonblockarcanedba.config.DbaEffectsConfig.screenShake();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                if (client.player == null) {
                    continue;
                }
                if (client.gui.screen() == null) {
                    client.setScreenAndShow(new DbaMenuScreen());
                }
            }
            while (openEmoteMenuKey.consumeClick()) {
                if (client.player == null) {
                    continue;
                }
                if (client.gui.screen() == null) {
                    client.setScreenAndShow(new com.dragonblockarcanedba.client.gui.EmoteSelectionScreen());
                }
            }
            tickKiTechniqueSlot(client, techSlot1Key, 0);
            tickKiTechniqueSlot(client, techSlot2Key, 1);
            tickKiTechniqueSlot(client, techSlot3Key, 2);

            // Otherworld atmospheric layered cloud particles
            com.dragonblockarcanedba.client.particle.OtherworldCloudParticleManager.tick(client);

            if (client.level != null) {
                for (net.minecraft.world.entity.player.Player player : client.level.players()) {
                    if (player instanceof net.minecraft.client.player.AbstractClientPlayer clientPlayer) {
                        com.dragonblockarcanedba.client.render.AuraRenderer.renderAura(clientPlayer);
                    }
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(com.dragonblockarcanedba.network.RaceConfigsSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                payload.configs().forEach((id, config) -> {
                    com.dragonblockarcanedba.config.RaceConfigManager.setConfig(id, config);
                });
            });
        });

        // Register client side sync receiver
        ClientPlayNetworking.registerGlobalReceiver(StatsSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player instanceof PlayerStatsAccessor accessor) {
                    CompoundTag nbt = payload.nbtData();
                    accessor.dba$setRaceId(Identifier.parse(nbt.getStringOr("raceId", "dragonblockarcanedba:human")));
                    accessor.dba$setHasSelectedRace(nbt.getBooleanOr("hasSelectedRace", false));
                    accessor.dba$setCurrentKi(nbt.getDoubleOr("currentKi", 100.0));
                    accessor.dba$setCurrentStamina(nbt.getDoubleOr("currentStamina", 100.0));
                    accessor.dba$setLevel(nbt.getIntOr("level", 1));
                    accessor.dba$setXp(nbt.getIntOr("xp", 0));
                    accessor.dba$setStatPoints(nbt.getIntOr("ap", 0));
                    accessor.dba$setSpeedPercent(nbt.getIntOr("speedPercent", 100));
                    accessor.dba$setTailSevered(nbt.getBooleanOr("tailSevered", false));
                    accessor.dba$setSkinColor(nbt.getStringOr("skinColor", ""));
                    accessor.dba$setHairColor(nbt.getStringOr("hairColor", ""));
                    accessor.dba$setEyeColor(nbt.getStringOr("eyeColor", ""));
                    accessor.dba$setActiveEmote(nbt.getStringOr("activeEmote", ""));

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

                    CompoundTag techLevels = nbt.getCompoundOrEmpty("techniqueLevels");
                    for (String key : techLevels.keySet()) {
                        accessor.dba$setTechniqueLevel(key, techLevels.getIntOr(key, 1));
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
                                int chargeTicks = kiTechNbt.getIntOr("slot" + i + "_chargeTicks", com.dragonblockarcanedba.ki.KiTechnique.DEFAULT_CHARGE_TICKS);
                                double size = kiTechNbt.getDoubleOr("slot" + i + "_size", com.dragonblockarcanedba.ki.KiTechnique.DEFAULT_SIZE);
                                accessor.dba$setKiTechniqueSlot(i, new com.dragonblockarcanedba.ki.KiTechnique(
                                        type, pct, color, barrage, false, chargeTicks, size));
                            }
                        }
                    }
                }
            });
        });

        // Register Player Ki Broadcast receiver for Ki Sense
        ClientPlayNetworking.registerGlobalReceiver(
            com.dragonblockarcanedba.network.PlayerKiBroadcastPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    if (context.client().level != null) {
                        net.minecraft.world.entity.Entity entity = context.client().level.getEntity(payload.entityId());
                        if (entity instanceof PlayerStatsAccessor targetAccessor) {
                            targetAccessor.dba$setCurrentKi(payload.currentKi());
                        }
                    }
                });
            }
        );

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
        // Register Transform Broadcast receiver (S2C), for multiplayer aura sync
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
                            accessor.dba$setTailSevered(payload.tailSevered());
                            if (!payload.skinColor().isEmpty()) {
                                accessor.dba$setSkinColor(payload.skinColor());
                            }
                            if (!payload.hairColor().isEmpty()) {
                                accessor.dba$setHairColor(payload.hairColor());
                            }
                            if (!payload.eyeColor().isEmpty()) {
                                accessor.dba$setEyeColor(payload.eyeColor());
                            }
                        }
                    }
                });
            }
        );

        // Register Emote Broadcast receiver (S2C), for multiplayer emote syncing
        ClientPlayNetworking.registerGlobalReceiver(
            com.dragonblockarcanedba.network.EmoteBroadcastPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    if (context.client().level != null) {
                        net.minecraft.world.entity.Entity entity = context.client().level.getEntity(payload.entityId());
                        if (entity instanceof PlayerStatsAccessor accessor) {
                            accessor.dba$setActiveEmote(payload.emote());
                        }
                    }
                });
            }
        );

        // Initialize native Bedrock animation registry (68 keyframed animations)
        com.dragonblockarcanedba.client.render.animation.BedrockAnimationRegistry.init();

        // Initialize Better Player Model real-time dynamic texture hooks and model sync
        com.dragonblockarcanedba.client.compat.BpmCompatClient.init();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register(
            (handler, sender, client) -> {
                if (client.player instanceof PlayerStatsAccessor accessor) {
                    Identifier raceId = accessor.dba$getRaceId();
                    String racePath = (raceId != null) ? raceId.getPath().toLowerCase() : "yardrat";
                    com.dragonblockarcanedba.client.compat.BpmCompatClient.enforceModel(racePath);
                } else {
                    com.dragonblockarcanedba.client.compat.BpmCompatClient.enforceYardratModel();
                }
            }
        );
    }

    /**
     * A saved charge of 0 still fires on the press, including key repeat.
     * A saved charge holds the slot key until that many ticks have passed, then fires once.
     */
    private static void tickKiTechniqueSlot(net.minecraft.client.Minecraft client, KeyMapping key, int slot) {
        if (slot < 0 || slot > 2) {
            return;
        }
        int need = 0;
        boolean chargedKi = false;
        if (client.player instanceof PlayerStatsAccessor accessor) {
            String equipped = accessor.dba$getEquippedTechnique(slot);
            if (equipped == null || equipped.isEmpty()) {
                com.dragonblockarcanedba.ki.KiTechnique tech = accessor.dba$getKiTechniqueSlot(slot);
                if (tech != null && !tech.isEmpty) {
                    need = tech.getChargeTicks();
                    chargedKi = need > 0;
                }
            }
        }
        if (!chargedKi) {
            kiSlotHold[slot] = 0;
            kiSlotFired[slot] = false;
            while (key.consumeClick()) {
                if (client.player != null) {
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                            new com.dragonblockarcanedba.network.C2SKiTechniqueFirePayload(slot));
                }
            }
            return;
        }
        boolean down = key.isDown();
        while (key.consumeClick()) {
            // Drop key-repeat clicks so they do not dump out after the charge ends.
        }
        if (!down || client.player == null) {
            kiSlotHold[slot] = 0;
            kiSlotFired[slot] = false;
            return;
        }
        if (kiSlotFired[slot]) {
            return;
        }
        kiSlotHold[slot]++;
        if (kiSlotHold[slot] >= need) {
            int held = kiSlotHold[slot];
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                    new com.dragonblockarcanedba.network.C2SKiTechniqueFirePayload(slot, held));
            kiSlotFired[slot] = true;
        }
    }
}
