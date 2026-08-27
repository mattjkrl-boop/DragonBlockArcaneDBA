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
    private static int oxAxeChargeTicks = 0;
    private static int grandSwordSpinTicks = 0;
    private static int daburaChargeTicks = 0;
    private static int evilSpearChargeTicks = 0;
    private static int katanaDrawTicks = 0;
    private static int blasterBarrageTicks = 0;
    private static int braveAssaultTicks = 0;
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
            com.dragonblockarcanedba.entity.DbaEntities.DIMENSIONAL_WARP_RIFT,
            com.dragonblockarcanedba.client.render.DimensionalWarpRiftRenderer::new
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
            com.dragonblockarcanedba.entity.DbaEntities.HOLLOW_CHARGE,
            com.dragonblockarcanedba.client.render.HollowChargeRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.HOLLOW_RUSH_TRAIL,
            com.dragonblockarcanedba.client.render.HollowRushTrailRenderer::new
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
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.AZURE_RUSH_TRAIL,
            com.dragonblockarcanedba.client.render.AzureRushTrailRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.AZURE_SONIC_QUAKE,
            com.dragonblockarcanedba.client.render.AzureSonicQuakeRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.AZURE_TEMPEST_CHANNEL,
            com.dragonblockarcanedba.client.render.AzureTempestChannelRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.OX_SHOCKWAVE,
            com.dragonblockarcanedba.client.render.OxShockwaveRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.OX_FISSURE,
            com.dragonblockarcanedba.client.render.OxFissureRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.OX_CHARGE,
            com.dragonblockarcanedba.client.render.OxChargeRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.OX_STANCE_AURA,
            com.dragonblockarcanedba.client.render.OxStanceAuraRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.KINGS_SLAM,
            com.dragonblockarcanedba.client.render.KingsSlamRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.GRAND_CRESCENT_WAVE,
            com.dragonblockarcanedba.client.render.GrandCrescentWaveRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.GRAND_BLADE_SHARD,
            com.dragonblockarcanedba.client.render.GrandBladeShardRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.VALOR_FIELD,
            com.dragonblockarcanedba.client.render.ValorFieldRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.DARKNESS_WAVE,
            com.dragonblockarcanedba.client.render.DarknessWaveRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.DARKNESS_BLADE,
            com.dragonblockarcanedba.client.render.DarknessBladeRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.DARKNESS_CHARGE,
            com.dragonblockarcanedba.client.render.DarknessChargeRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.DARKNESS_DOMAIN,
            com.dragonblockarcanedba.client.render.DarknessDomainRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.DARKNESS_SHATTER,
            com.dragonblockarcanedba.client.render.DarknessShatterRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.EVIL_SPEAR_PROJECTILE,
            com.dragonblockarcanedba.client.render.EvilSpearProjectileRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.BLASTER_BOLT,
            com.dragonblockarcanedba.client.render.BlasterBoltRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.BRAVE_SLASH,
            com.dragonblockarcanedba.client.render.BraveSlashRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.BRAVE_CROSS_SLASH,
            com.dragonblockarcanedba.client.render.BraveCrossSlashRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.BRAVE_CHARGE,
            com.dragonblockarcanedba.client.render.BraveChargeRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.BRAVE_RUSH_TRAIL,
            com.dragonblockarcanedba.client.render.BraveRushTrailRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.BRAVE_SHOCKWAVE,
            com.dragonblockarcanedba.client.render.BraveShockwaveRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.SORROW_SLASH,
            com.dragonblockarcanedba.client.render.SorrowSlashRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.DIMENSIONAL_RIFT,
            com.dragonblockarcanedba.client.render.DimensionalRiftRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.ABYSSAL_DOMAIN,
            com.dragonblockarcanedba.client.render.AbyssalDomainRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.CURSE_TELEGRAPH,
            com.dragonblockarcanedba.client.render.CurseTelegraphRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.CURSE_GROUND_SHATTER,
            com.dragonblockarcanedba.client.render.CurseGroundShatterRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.EVIL_SPEAR_CHARGE,
            com.dragonblockarcanedba.client.render.EvilSpearChargeRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.HELL_HUNT_IMPACT,
            com.dragonblockarcanedba.client.render.HellHuntImpactRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.BANSHO_CYCLONE,
            com.dragonblockarcanedba.client.render.BanshoCycloneRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.BANSHO_SHOCKWAVE,
            com.dragonblockarcanedba.client.render.BanshoShockwaveRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.BANSHO_WIND_PROJECTILE,
            com.dragonblockarcanedba.client.render.BanshoWindProjectileRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.GRAND_CLASH_SPARK,
            com.dragonblockarcanedba.client.render.GrandClashSparkRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.DEVIL_SLAM_SHOCKWAVE,
            com.dragonblockarcanedba.client.render.DevilSlamShockwaveRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.SPIRIT_IMPALE,
            com.dragonblockarcanedba.client.render.SpiritImpaleRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.SPIRIT_CANNON_BEAM,
            com.dragonblockarcanedba.client.render.SpiritCannonBeamRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.SABER_SLASH,
            com.dragonblockarcanedba.client.render.SaberSlashRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.SABER_LINE_SLASH,
            com.dragonblockarcanedba.client.render.SaberDimensionalLineSlashRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.SABER_VOID_TEAR,
            com.dragonblockarcanedba.client.render.SaberVoidTearRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.SABER_DODGE_SPARK,
            com.dragonblockarcanedba.client.render.SaberDodgeSparkRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.TIME_SHATTER,
            com.dragonblockarcanedba.client.render.TimeShatterRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.TEMPORAL_RIFT,
            com.dragonblockarcanedba.client.render.TemporalRiftRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.POWER_POLE_WHIRLWIND,
            com.dragonblockarcanedba.client.render.PowerPoleWhirlwindRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.POWER_POLE_EXTENSION,
            com.dragonblockarcanedba.client.render.PowerPoleExtensionRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.POWER_POLE_IMPACT,
            com.dragonblockarcanedba.client.render.PowerPoleImpactRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.ERASURE_CHARGE_ORB,
            com.dragonblockarcanedba.client.render.ErasureChargeOrbRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.ERASURE_CANNON_BEAM,
            com.dragonblockarcanedba.client.render.ErasureCannonBeamRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.SWIFT_CRESCENT,
            com.dragonblockarcanedba.client.render.SwiftCrescentRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.KATANA_CHARGE,
            com.dragonblockarcanedba.client.render.KatanaChargeRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.KATANA_AIM_GUIDE,
            com.dragonblockarcanedba.client.render.KatanaAimGuideRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.HEAVEN_SPLITTER,
            com.dragonblockarcanedba.client.render.HeavenSplitterRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.Z_CHARGE,
            com.dragonblockarcanedba.client.render.ZChargeRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.Z_STANCE_AURA,
            com.dragonblockarcanedba.client.render.ZStanceAuraRenderer::new
        );
        net.minecraft.client.renderer.entity.EntityRenderers.register(
            com.dragonblockarcanedba.entity.DbaEntities.Z_GRAVITY_SLAM,
            com.dragonblockarcanedba.client.render.ZGravitySlamRenderer::new
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

                // 7. Ox King's Axe Left-Click Groundbreaker Charge & Release
                if (stack.getItem() instanceof com.dragonblockarcanedba.item.OxKingsAxeItem) {
                    if (client.options.keyAttack.isDown()) {
                        oxAxeChargeTicks = Math.min(200, oxAxeChargeTicks + 1);
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK,
                            oxAxeChargeTicks
                        ));
                    } else if (oxAxeChargeTicks > 0) {
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_RELEASE,
                            oxAxeChargeTicks
                        ));
                        client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        oxAxeChargeTicks = 0;
                    }
                } else {
                    if (oxAxeChargeTicks > 0) {
                        oxAxeChargeTicks = 0;
                    }
                }

                // 8. Grand Sword Left-Click Spin & Blade Shards Fire
                if (stack.getItem() instanceof com.dragonblockarcanedba.item.GrandSwordItem) {
                    if (client.options.keyAttack.isDown()) {
                        grandSwordSpinTicks++;
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK,
                            grandSwordSpinTicks
                        ));

                        // Accelerating camera/player yaw spin client-side (15° to 35° per tick)
                        float spinProgress = Math.min(1.0f, grandSwordSpinTicks / 100.0f);
                        float spinSpeed = 15.0f + (spinProgress * 20.0f);
                        client.player.setYRot(client.player.getYRot() + spinSpeed);
                        client.player.yHeadRot = client.player.getYRot();
                        client.player.yBodyRot = client.player.getYRot();
                    } else if (grandSwordSpinTicks > 0) {
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_RELEASE,
                            grandSwordSpinTicks
                        ));
                        client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        grandSwordSpinTicks = 0;
                    }
                } else {
                    if (grandSwordSpinTicks > 0) {
                        grandSwordSpinTicks = 0;
                    }
                }

                // 9. Darkness Sword (Dabura Sword) Left-Click Abyssal Slash Charge & Release
                if (stack.getItem() instanceof com.dragonblockarcanedba.item.DaburaSwordItem) {
                    if (client.options.keyAttack.isDown()) {
                        daburaChargeTicks = Math.min(160, daburaChargeTicks + 1);
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK,
                            daburaChargeTicks
                        ));
                    } else if (daburaChargeTicks > 0) {
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_RELEASE,
                            daburaChargeTicks
                        ));
                        client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        daburaChargeTicks = 0;
                    }
                } else {
                    if (daburaChargeTicks > 0) {
                        daburaChargeTicks = 0;
                    }
                }

                // 10. Evil Spear Left-Click Impale Charge & Release
                if (stack.getItem() instanceof com.dragonblockarcanedba.item.EvilSpearItem) {
                    if (client.options.keyAttack.isDown()) {
                        evilSpearChargeTicks = Math.min(120, evilSpearChargeTicks + 1);
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK,
                            evilSpearChargeTicks
                        ));
                    } else if (evilSpearChargeTicks > 0) {
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_RELEASE,
                            evilSpearChargeTicks
                        ));
                        client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        evilSpearChargeTicks = 0;
                    }
                } else {
                    if (evilSpearChargeTicks > 0) {
                        evilSpearChargeTicks = 0;
                    }
                }

                // 11. Katana Left-Click Flashdraw Charge & Release
                if (stack.getItem() instanceof com.dragonblockarcanedba.item.KatanaItem) {
                    if (client.options.keyAttack.isDown()) {
                        katanaDrawTicks = Math.min(100, katanaDrawTicks + 1);
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK,
                            katanaDrawTicks
                        ));
                    } else if (katanaDrawTicks > 0) {
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_RELEASE,
                            katanaDrawTicks
                        ));
                        client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        katanaDrawTicks = 0;
                    }
                } else {
                    if (katanaDrawTicks > 0) {
                        katanaDrawTicks = 0;
                    }
                }

                // 12. Blaster Gun Left-Click Barrage (Continuous)
                if (stack.getItem() instanceof com.dragonblockarcanedba.item.BlasterGunItem) {
                    if (client.options.keyAttack.isDown()) {
                        blasterBarrageTicks++;
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK,
                            blasterBarrageTicks
                        ));
                        if (blasterBarrageTicks % 3 == 0) {
                            client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        }
                    } else if (blasterBarrageTicks > 0) {
                        blasterBarrageTicks = 0;
                    }
                } else {
                    if (blasterBarrageTicks > 0) {
                        blasterBarrageTicks = 0;
                    }
                }

                // 13. Brave Sword Left-Click Assault (Continuous Combo)
                if (stack.getItem() instanceof com.dragonblockarcanedba.item.BraveSwordItem) {
                    if (client.options.keyAttack.isDown()) {
                        braveAssaultTicks++;
                        ClientPlayNetworking.send(new com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload(
                            com.dragonblockarcanedba.network.C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK,
                            braveAssaultTicks
                        ));
                        if (braveAssaultTicks % 4 == 0) {
                            client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        }
                    } else if (braveAssaultTicks > 0) {
                        braveAssaultTicks = 0;
                    }
                } else {
                    if (braveAssaultTicks > 0) {
                        braveAssaultTicks = 0;
                    }
                }

                if (!isRapidWeapon && !(stack.getItem() instanceof com.dragonblockarcanedba.item.ZSwordItem) && !(stack.getItem() instanceof com.dragonblockarcanedba.item.CurseBladeItem) && !(stack.getItem() instanceof com.dragonblockarcanedba.item.HollowsEdgeItem) && !(stack.getItem() instanceof com.dragonblockarcanedba.item.AzureDragonSwordItem) && !(stack.getItem() instanceof com.dragonblockarcanedba.item.SaberItem) && !(stack.getItem() instanceof com.dragonblockarcanedba.item.OxKingsAxeItem) && !(stack.getItem() instanceof com.dragonblockarcanedba.item.GrandSwordItem) && !(stack.getItem() instanceof com.dragonblockarcanedba.item.DaburaSwordItem) && !(stack.getItem() instanceof com.dragonblockarcanedba.item.EvilSpearItem) && !(stack.getItem() instanceof com.dragonblockarcanedba.item.KatanaItem) && !(stack.getItem() instanceof com.dragonblockarcanedba.item.BlasterGunItem) && !(stack.getItem() instanceof com.dragonblockarcanedba.item.BraveSwordItem)) {
                    if (client.player.swingTime == 1 && client.player.swingingArm == net.minecraft.world.InteractionHand.MAIN_HAND) {
                        if (stack.getItem() instanceof com.dragonblockarcanedba.item.DevilTridentItem ||
                            stack.getItem() instanceof com.dragonblockarcanedba.item.SickleOfSorrowItem ||
                            stack.getItem() instanceof com.dragonblockarcanedba.item.SpiritSwordItem ||
                            stack.getItem() instanceof com.dragonblockarcanedba.item.BanshoFanItem ||
                            stack.getItem() instanceof com.dragonblockarcanedba.item.WhisStaffItem) {
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
                            accessor.dba$setTailSevered(payload.tailSevered());
                            if (!payload.skinColor().isEmpty()) {
                                accessor.dba$setSkinColor(payload.skinColor());
                            }
                            if (!payload.hairColor().isEmpty()) {
                                accessor.dba$setHairColor(payload.hairColor());
                            }
                        }
                    }
                });
            }
        );
    }
}
