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

    private static int weaponUseTimer = 0;
    private static int zSwordChargeTicks = 0;
    private static int hollowChargeTicks = 0;
    private static int saberChargeTicks = 0;
    private static boolean isAzureRushing = false;
    private static float lastCameraYRot = 0;
    private static float lastCameraXRot = 0;
    private static boolean manualSaberCameraOverride = false;

    public static final net.minecraft.client.model.geom.ModelLayerLocation SHENRON_MODEL_LAYER = new net.minecraft.client.model.geom.ModelLayerLocation(
        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("shenron"), "main"
    );

    @Override
    public void onInitializeClient() {
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
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.SHENRON,
            com.dragonblockarcanedba.client.render.ShenronRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.CURSE_LIGHTNING,
            com.dragonblockarcanedba.client.render.CurseLightningRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.DIMENSIONAL_SLASH,
            com.dragonblockarcanedba.client.render.DimensionalSlashRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.TRIDENT_SHARD,
            com.dragonblockarcanedba.client.render.TridentShardRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.Z_SHOCKWAVE,
            com.dragonblockarcanedba.client.render.ZShockwaveRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.CURSE_CHAIN,
            com.dragonblockarcanedba.client.render.CurseChainRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.DELAYED_LAUNCH,
            com.dragonblockarcanedba.client.render.DelayedLaunchRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.SKY_CRACKS,
            com.dragonblockarcanedba.client.render.SkyCracksRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.VOID_RIFT,
            com.dragonblockarcanedba.client.render.VoidRiftRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.HOLLOW_AFTERIMAGE,
            com.dragonblockarcanedba.client.render.HollowAfterimageRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.VOID_SLASH,
            com.dragonblockarcanedba.client.render.VoidSlashRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.AZURE_STORM,
            com.dragonblockarcanedba.client.render.AzureStormRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.AZURE_LIGHTNING,
            com.dragonblockarcanedba.client.render.AzureLightningRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.AZURE_TORNADO,
            com.dragonblockarcanedba.client.render.AzureTornadoRenderer::new
        );

        net.minecraft.client.renderer.special.SpecialModelRenderers.ID_MAPPER.put(
            com.dragonblockarcanedba.DragonBlockArcaneDBA.id("procedural_weapon"),
            com.dragonblockarcanedba.client.render.item.ProceduralWeaponUnbakedModel.CODEC
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

            // Detect continuous left-click and charging for specific weapons
            if (client.player != null) {
                if (weaponUseTimer > 0) {
                    weaponUseTimer--;
                }

                net.minecraft.world.item.ItemStack stack = client.player.getMainHandItem();

                // 1. Z Sword Left-Click Charge & Release
                if (stack.getItem() instanceof com.dragonblockarcanedba.item.ZSwordItem) {
                    if (client.options.keyAttack.isDown()) {
                        zSwordChargeTicks = Math.min(300, zSwordChargeTicks + 1);
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK,
                            zSwordChargeTicks
                        ));
                    } else if (zSwordChargeTicks > 0) {
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_RELEASE,
                            zSwordChargeTicks
                        ));
                        client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        zSwordChargeTicks = 0;
                    }
                } else {
                    if (zSwordChargeTicks > 0) {
                        zSwordChargeTicks = 0;
                    }
                }

                // 2. Hollow's Edge Charge & Multi-Dash
                if (stack.getItem() instanceof com.dragonblockarcanedba.item.HollowsEdgeItem) {
                    boolean isHoldingS = client.options.keyDown.isDown();
                    if (client.options.keyAttack.isDown()) {
                        hollowChargeTicks = Math.min(100, hollowChargeTicks + 1);
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK,
                            hollowChargeTicks,
                            isHoldingS
                        ));
                    } else if (hollowChargeTicks > 0) {
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_RELEASE,
                            hollowChargeTicks,
                            isHoldingS
                        ));
                        client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        hollowChargeTicks = 0;
                    } else if (client.player.swingTime == 1 && client.player.swingingArm == net.minecraft.world.InteractionHand.MAIN_HAND) {
                        // Click dash during active sequence
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_CLICK,
                            0,
                            isHoldingS
                        ));
                    }
                } else {
                    if (hollowChargeTicks > 0) {
                        hollowChargeTicks = 0;
                    }
                }

                // 3. Azure Dragon Sword Rush
                if (stack.getItem() instanceof com.dragonblockarcanedba.item.AzureDragonSwordItem) {
                    boolean isSneaking = client.options.keyShift.isDown();
                    if (client.options.keyAttack.isDown()) {
                        isAzureRushing = true;
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK,
                            0,
                            isSneaking
                        ));
                    } else if (isAzureRushing) {
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_RELEASE,
                            0,
                            isSneaking
                        ));
                        isAzureRushing = false;
                    }
                } else {
                    if (isAzureRushing) {
                        isAzureRushing = false;
                    }
                }

                // 4. Curse Blade Continuous Stream
                if (stack.getItem() instanceof com.dragonblockarcanedba.item.CurseBladeItem) {
                    if (client.options.keyAttack.isDown() && weaponUseTimer <= 0) {
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_CLICK,
                            0
                        ));
                        client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        weaponUseTimer = 4; // every 4 ticks
                    }
                }

                // 5. Dimensional Sword & Power Pole Rapid Fire
                boolean isRapidWeapon = stack.getItem() instanceof com.dragonblockarcanedba.item.DimensionalSwordItem || 
                                        stack.getItem() instanceof com.dragonblockarcanedba.item.PowerPoleItem;

                if (isRapidWeapon && client.options.keyAttack.isDown()) {
                    if (weaponUseTimer <= 0) {
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_CLICK,
                            0
                        ));
                        client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        weaponUseTimer = 2; // 2 ticks = 10 attacks per second
                    }
                }

                // 6. Saber Blitz Flurry (Hold Left Click)
                if (stack.getItem() instanceof com.dragonblockarcanedba.item.SaberItem) {
                    if (client.options.keyAttack.isDown()) {
                        saberChargeTicks++;
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK,
                            saberChargeTicks
                        ));
                        client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);

                        // Client-side Phasing / Transparency targeting
                        if (client.level != null) {
                            net.minecraft.world.phys.Vec3 eyePos = client.player.getEyePosition();
                            net.minecraft.world.phys.Vec3 look = client.player.getLookAngle();
                            java.util.List<net.minecraft.world.entity.LivingEntity> nearby = client.level.getEntitiesOfClass(
                                net.minecraft.world.entity.LivingEntity.class,
                                client.player.getBoundingBox().inflate(3.5),
                                e -> e.isAlive() && e != client.player && client.player.distanceTo(e) <= 3.5 && !e.isInvisible()
                            );
                            int bestId = -1;
                            double bestDot = 0.45;
                            for (net.minecraft.world.entity.LivingEntity e : nearby) {
                                net.minecraft.world.phys.Vec3 toE = e.getBoundingBox().getCenter().subtract(eyePos).normalize();
                                double dot = look.dot(toE);
                                if (dot > bestDot) {
                                    bestDot = dot;
                                    bestId = e.getId();
                                }
                            }
                            com.dragonblockarcanedba.item.SaberItem.clientPhasedEntityId = bestId;

                            // Auto-Aim Logic
                            float yDelta = Math.abs(client.player.getYRot() - lastCameraYRot);
                            float xDelta = Math.abs(client.player.getXRot() - lastCameraXRot);
                            // If player moves mouse significantly (>5 degrees), they take manual control
                            if (yDelta > 5.0f || xDelta > 5.0f) {
                                manualSaberCameraOverride = true;
                            }

                            if (!manualSaberCameraOverride && bestId != -1) {
                                net.minecraft.world.entity.Entity target = client.level.getEntity(bestId);
                                if (target != null) {
                                    net.minecraft.world.phys.Vec3 toTarget = target.getBoundingBox().getCenter().subtract(client.player.getEyePosition());
                                    double r = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
                                    float targetYRot = (float)(Math.atan2(toTarget.z, toTarget.x) * (180 / Math.PI)) - 90.0f;
                                    float targetXRot = (float)(-(Math.atan2(toTarget.y, r) * (180 / Math.PI)));

                                    float newYRot = net.minecraft.util.Mth.approachDegrees(client.player.getYRot(), targetYRot, 20.0f);
                                    float newXRot = net.minecraft.util.Mth.approachDegrees(client.player.getXRot(), targetXRot, 20.0f);
                                    client.player.setYRot(newYRot);
                                    client.player.setXRot(newXRot);
                                }
                            }
                        }
                        lastCameraYRot = client.player.getYRot();
                        lastCameraXRot = client.player.getXRot();
                    } else if (saberChargeTicks > 0) {
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_RELEASE,
                            saberChargeTicks
                        ));
                        saberChargeTicks = 0;
                        com.dragonblockarcanedba.item.SaberItem.clientPhasedEntityId = -1;
                        manualSaberCameraOverride = false;
                    } else {
                        com.dragonblockarcanedba.item.SaberItem.clientPhasedEntityId = -1;
                        manualSaberCameraOverride = false;
                        lastCameraYRot = client.player.getYRot();
                        lastCameraXRot = client.player.getXRot();
                    }
                } else {
                    if (saberChargeTicks > 0) {
                        saberChargeTicks = 0;
                        com.dragonblockarcanedba.item.SaberItem.clientPhasedEntityId = -1;
                        manualSaberCameraOverride = false;
                    }
                    lastCameraYRot = client.player.getYRot();
                    lastCameraXRot = client.player.getXRot();
                }

                if (!isRapidWeapon && !(stack.getItem() instanceof com.dragonblockarcanedba.item.ZSwordItem) && !(stack.getItem() instanceof com.dragonblockarcanedba.item.CurseBladeItem) && !(stack.getItem() instanceof com.dragonblockarcanedba.item.HollowsEdgeItem) && !(stack.getItem() instanceof com.dragonblockarcanedba.item.AzureDragonSwordItem) && !(stack.getItem() instanceof com.dragonblockarcanedba.item.SaberItem)) {
                    // Fallback for standard weapons detecting air click
                    if (client.player.swingTime == 1 && client.player.swingingArm == net.minecraft.world.InteractionHand.MAIN_HAND) {
                        if (stack.getItem() instanceof com.dragonblockarcanedba.item.DevilTridentItem) { // If there are other weapons
                            ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                                com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_CLICK,
                                0
                            ));
                        }
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
