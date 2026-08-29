package com.dragonblockarcanedba;

import com.dragonblockarcanedba.item.DbaItems;
import com.dragonblockarcanedba.registry.FormLoader;
import com.dragonblockarcanedba.registry.RaceLoader;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.dragonblockarcanedba.network.DbaNetwork;
import com.dragonblockarcanedba.effect.DbaEffects;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class DragonBlockArcaneDBA implements ModInitializer {
    public static final String MOD_ID = "dragonblockarcanedba";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Dragon Block Arcane DBA!");

        // Register Sound Events
        DbaSounds.register();

        // Register Items and Blocks
        com.dragonblockarcanedba.block.DbaBlocks.register();
        com.dragonblockarcanedba.block.entity.DbaBlockEntities.register();
        DbaItems.register();
        com.dragonblockarcanedba.inventory.DbaMenus.register();
        DbaEffects.register();

        // Worldgen
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, id("katchin_ore"))
        );

        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, id("dragstone_ore"))
        );

        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, id("aetherium_ore"))
        );

        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, id("bauxite_ore"))
        );

        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, id("tin_ore"))
        );

        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, id("silver_ore"))
        );

        // Initialize default built-in registries (Races, Forms)
        com.dragonblockarcanedba.registry.DbaRegistries.initDefaults();

        // Register Entities
        com.dragonblockarcanedba.entity.DbaEntities.register();

        // Register Networking
        DbaNetwork.registerCommon();
        DbaNetwork.registerServer();

        // Register JSON dynamic data loaders using non-deprecated ResourceLoader
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
            Identifier.fromNamespaceAndPath("dragonblockarcanedba", "races"),
            new RaceLoader()
        );
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
            Identifier.fromNamespaceAndPath("dragonblockarcanedba", "forms"),
            new FormLoader()
        );

        // Register creative tab modification using new Fabric 26.2 API
        // Moved items to custom tab in DbaItems.java

        // Register Commands
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            com.dragonblockarcanedba.command.DbaCommand.register(dispatcher);
        });

        // Block Mining for rapid-fire / charging weapons (Dimensional Sword, Power Pole, Z Sword, Curse Blade, Hollow's Edge, Azure Dragon Sword)
        net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!player.isSpectator()) {
                if (player instanceof com.dragonblockarcanedba.attribute.PlayerStatsAccessor acc && acc.dba$isSickleActive()) {
                    return net.minecraft.world.InteractionResult.FAIL;
                }
                net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof com.dragonblockarcanedba.item.DimensionalSwordItem || 
                    stack.getItem() instanceof com.dragonblockarcanedba.item.PowerPoleItem ||
                    stack.getItem() instanceof com.dragonblockarcanedba.item.ZSwordItem ||
                    stack.getItem() instanceof com.dragonblockarcanedba.item.CurseBladeItem ||
                    stack.getItem() instanceof com.dragonblockarcanedba.item.HollowsEdgeItem ||
                    stack.getItem() instanceof com.dragonblockarcanedba.item.AzureDragonSwordItem ||
                    stack.getItem() instanceof com.dragonblockarcanedba.item.SaberItem ||
                    stack.getItem() instanceof com.dragonblockarcanedba.item.OxKingsAxeItem ||
                    stack.getItem() instanceof com.dragonblockarcanedba.item.GrandSwordItem ||
                    stack.getItem() instanceof com.dragonblockarcanedba.item.DaburaSwordItem ||
                    stack.getItem() instanceof com.dragonblockarcanedba.item.EvilSpearItem ||
                    stack.getItem() instanceof com.dragonblockarcanedba.item.KatanaItem ||
                    stack.getItem() instanceof com.dragonblockarcanedba.item.BlasterGunItem ||
                    stack.getItem() instanceof com.dragonblockarcanedba.item.BraveSwordItem ||
                    stack.getItem() instanceof com.dragonblockarcanedba.item.SickleOfSorrowItem) {
                    return net.minecraft.world.InteractionResult.FAIL;
                }

            }
            return net.minecraft.world.InteractionResult.PASS;
        });

        // Register Attack Hook for Stamina Drain and Sickle Technique Melee
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof com.dragonblockarcanedba.attribute.PlayerStatsAccessor acc && acc.dba$isSickleActive()) {
                if (!world.isClientSide() && entity instanceof net.minecraft.world.entity.LivingEntity target && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    com.dragonblockarcanedba.item.SickleOfSorrowItem.executeTechniqueMelee(serverPlayer, target);
                }
                return net.minecraft.world.InteractionResult.FAIL;
            }

            net.minecraft.world.item.Item heldItem = player.getItemInHand(hand).getItem();
            if (heldItem instanceof com.dragonblockarcanedba.item.SaberItem ||
                heldItem instanceof com.dragonblockarcanedba.item.GrandSwordItem ||
                heldItem instanceof com.dragonblockarcanedba.item.OxKingsAxeItem) {
                return net.minecraft.world.InteractionResult.FAIL;
            }

            if (!world.isClientSide()) {
                boolean isDbaWeapon = heldItem instanceof com.dragonblockarcanedba.item.SpiritSwordItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.SickleOfSorrowItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.HollowsEdgeItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.DimensionalSwordItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.PowerPoleItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.DevilTridentItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.AzureDragonSwordItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.CurseBladeItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.SaberItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.OxKingsAxeItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.GrandSwordItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.DaburaSwordItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.ZSwordItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.EvilSpearItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.BraveSwordItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.KatanaItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.BlasterGunItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.BanshoFanItem ||
                                      heldItem instanceof com.dragonblockarcanedba.item.WhisStaffItem;

                if (!isDbaWeapon) {
                    com.dragonblockarcanedba.attribute.PlayerStatsAccessor accessor = (com.dragonblockarcanedba.attribute.PlayerStatsAccessor) player;
                    double stamina = accessor.dba$getCurrentStamina();
                    if (stamina < 8.0) {
                        // Apply stamina exhaustion weakness & fatigue via custom Earth Shatter effect
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.EARTH_SHATTER_HOLDER, 40, 0, false, false));
                    } else {
                        double strength = com.dragonblockarcanedba.attribute.PlayerStats.getEffectiveStat(player, "strength");
                        double drain = 8.0 + (strength * 0.25); // Scales with strength
                        accessor.dba$addStamina(-drain);
                    }
                    accessor.dba$syncStats();
                }

                // Devil Trident Target logic
                net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof com.dragonblockarcanedba.item.DevilTridentItem) {
                    net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                    if (tag.getBoolean("isDeployed").orElse(false)) {
                        tag.putString("swarmTarget", entity.getUUID().toString());
                        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                    }
                }

                // Azure Dragon Sword Target lock-on logic (Tweak B)
                if (stack.getItem() instanceof com.dragonblockarcanedba.item.AzureDragonSwordItem && entity instanceof net.minecraft.world.entity.LivingEntity) {
                    com.dragonblockarcanedba.item.AzureDragonSwordItem.LOCKED_TARGET_MAP.put(player.getUUID(), entity.getUUID());
                }
            }
            return net.minecraft.world.InteractionResult.PASS;
        });

        // Disable off-hand and normal item use while Sickle is active, and route right-click to Dimensional Rift
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player instanceof com.dragonblockarcanedba.attribute.PlayerStatsAccessor acc && acc.dba$isSickleActive()) {
                if (hand == net.minecraft.world.InteractionHand.OFF_HAND) {
                    return net.minecraft.world.InteractionResult.FAIL;
                }
                com.dragonblockarcanedba.item.SickleOfSorrowItem.performTechniqueDimensionalRift(player);
                return net.minecraft.world.InteractionResult.FAIL;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });

        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player instanceof com.dragonblockarcanedba.attribute.PlayerStatsAccessor acc && acc.dba$isSickleActive()) {
                if (hand == net.minecraft.world.InteractionHand.OFF_HAND) {
                    return net.minecraft.world.InteractionResult.FAIL;
                }
                com.dragonblockarcanedba.item.SickleOfSorrowItem.performTechniqueDimensionalRift(player);
                return net.minecraft.world.InteractionResult.FAIL;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });

        // Register Planet Gravity & Oxygen Tick Hook
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (net.minecraft.server.level.ServerLevel world : server.getAllLevels()) {
                com.dragonblockarcanedba.dimension.PlanetDimension planet = com.dragonblockarcanedba.dimension.DimensionTravel.getPlanetConfig(world.dimension());
                if (planet != null) {
                    // Apply effects to all players in the dimension
                    for (net.minecraft.server.level.ServerPlayer player : world.players()) {
                        planet.tickPlanetEffects(player);
                    }
                }
            }

            // Devil Trident Shard Swarm AI
            for (net.minecraft.server.level.ServerLevel world : server.getAllLevels()) {
                for (net.minecraft.server.level.ServerPlayer player : world.players()) {
                    com.dragonblockarcanedba.item.DevilTridentItem.manageShardSwarm(player, world);
                }
            }

            // Saber Server Tick (Blitz sequences, Flash Step recharge, Escalating Speed expire)
            com.dragonblockarcanedba.item.SaberItem.tickServer(server);
        });

        // Register Death Hook (Otherworld mechanics)
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, damageMultiplier) -> {
            if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
                com.dragonblockarcanedba.attribute.PlayerStatsAccessor accessor = (com.dragonblockarcanedba.attribute.PlayerStatsAccessor) player;
                
                // --- Death / Respawn Handling ---
                // 1. Clear active transformation form
                accessor.dba$setActiveFormId(null);
                
                // 2. Reset Ki to full
                accessor.dba$setCurrentKi(com.dragonblockarcanedba.attribute.PlayerStats.getMaxKi(player));
                
                // 3. Reset Stamina to full
                accessor.dba$setCurrentStamina(com.dragonblockarcanedba.attribute.PlayerStats.getMaxStamina(player));
                
                // 4. Apply 10% XP penalty (lose 10% of current XP, never go below 0)
                int currentXp = accessor.dba$getXp();
                int penalty = (int)(currentXp * 0.10);
                accessor.dba$setXp(Math.max(0, currentXp - penalty));
                
                // 5. Sync stats to client
                accessor.dba$syncStats();
                
                // Reset player physical status
                player.setHealth(player.getMaxHealth());
                player.removeAllEffects();
                player.resetFallDistance();
                player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                player.clearFire();
                player.stopRiding();
                
                // Add blindness for a smooth "passing out" fade to black transition
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 80, 1, false, false, false));

                // Find otherworld
                net.minecraft.server.level.ServerLevel otherworld = ((net.minecraft.server.level.ServerLevel)entity.level()).getServer().getLevel(
                    net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id("otherworld"))
                );
                
                if (otherworld != null) {
                    net.minecraft.world.phys.Vec3 spawn = com.dragonblockarcanedba.dimension.OtherworldStationGenerator.ensureStationAndGetSpawn(otherworld);
                    player.teleportTo(otherworld, spawn.x, spawn.y, spawn.z, java.util.Collections.emptySet(), 0.0f, 0.0f, false);
                    return false; // Cancel death only if safely teleported to Otherworld
                }
                return true; // Fallback to normal vanilla death if otherworld is unavailable
            }
            return true; // Allow normal death for non-players
        });

        // Trigger Race Selection, sync stats on join, and sync transformations across players
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            net.minecraft.server.level.ServerPlayer player = handler.getPlayer();
            com.dragonblockarcanedba.attribute.PlayerStatsAccessor accessor = (com.dragonblockarcanedba.attribute.PlayerStatsAccessor) player;
            accessor.dba$syncStats();
            if (!accessor.dba$hasSelectedRace()) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.dragonblockarcanedba.network.RaceSelectOpenPayload());
            }

            // Purge any orphan/lingering physics modifiers (e.g. bugged bounce attributes from older sessions)
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.purgeAllDbaModifiers(player);

            // Broadcast joining player's transformation state to others
            com.dragonblockarcanedba.network.DbaNetwork.broadcastTransformState(player);
            // Send existing players' active transformation states to the newly joined player
            if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
                for (net.minecraft.server.level.ServerPlayer other : level.players()) {
                    if (other != player) {
                        com.dragonblockarcanedba.network.DbaNetwork.sendTransformStateTo(other, player);
                    }
                }
            }
        });

        // Sync transformation state when an entity starts tracking a player (render distance entry)
        net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents.START_TRACKING.register((trackedEntity, trackingPlayer) -> {
            if (trackedEntity instanceof net.minecraft.server.level.ServerPlayer trackedPlayer) {
                com.dragonblockarcanedba.network.DbaNetwork.sendTransformStateTo(trackedPlayer, trackingPlayer);
            }
        });

        // Clean up static maps and memory on player disconnect
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            java.util.UUID playerUuid = handler.getPlayer().getUUID();
            com.dragonblockarcanedba.item.AzureDragonSwordItem.onPlayerDisconnect(playerUuid);
            com.dragonblockarcanedba.item.SaberItem.onPlayerDisconnect(playerUuid);
            com.dragonblockarcanedba.item.HollowsEdgeItem.onPlayerDisconnect(playerUuid);
            com.dragonblockarcanedba.item.GrandSwordItem.onPlayerDisconnect(playerUuid);
            com.dragonblockarcanedba.item.BraveSwordItem.onPlayerDisconnect(playerUuid);
            com.dragonblockarcanedba.item.DaburaSwordItem.onPlayerDisconnect(playerUuid);
            com.dragonblockarcanedba.ki.KiTechniqueHandler.onPlayerDisconnect(playerUuid);
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.onPlayerDisconnect(playerUuid);
        });

        // Copy DBA data across death/respawn so race, stats, and level are preserved
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            com.dragonblockarcanedba.attribute.PlayerStatsAccessor oldAccessor = (com.dragonblockarcanedba.attribute.PlayerStatsAccessor) oldPlayer;
            com.dragonblockarcanedba.attribute.PlayerStatsAccessor newAccessor = (com.dragonblockarcanedba.attribute.PlayerStatsAccessor) newPlayer;
            newAccessor.dba$copyFrom(oldAccessor);
        });

        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            com.dragonblockarcanedba.attribute.PlayerStatsAccessor newAccessor = (com.dragonblockarcanedba.attribute.PlayerStatsAccessor) newPlayer;
            newAccessor.dba$syncStats();
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.purgeAllDbaModifiers(newPlayer);
        });

        // Ticker for timed physical attributes (e.g. temporary ricochets, launch air drag)
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.tick();
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
