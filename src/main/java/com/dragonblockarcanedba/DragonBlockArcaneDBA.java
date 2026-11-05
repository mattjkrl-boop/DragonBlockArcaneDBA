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

public class DragonBlockArcaneDBA implements ModInitializer {
    public static final String MOD_ID = "dragonblockarcanedba";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Dragon Block Arcane DBA!");

        // Register Sound Events
        com.dragonblockarcanedba.config.DbaEffectsConfig.load();
        DbaSounds.register();
        com.dragonblockarcanedba.particle.DbaParticles.register();

        // Register Items and Blocks
        com.dragonblockarcanedba.block.DbaBlocks.register();
        com.dragonblockarcanedba.block.entity.DbaBlockEntities.register();
        DbaItems.register();
        com.dragonblockarcanedba.item.geo.WeaponAnimatableIds.register();
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
        com.dragonblockarcanedba.ki.KiBarrier.register();

        // Register Networking
        DbaNetwork.registerCommon();
        DbaNetwork.registerServer();
        com.dragonblockarcanedba.wish.DragonWishRitual.register();
        com.dragonblockarcanedba.movement.MovementPhysics.register();
        com.dragonblockarcanedba.movement.MobPhysics.register();
        com.dragonblockarcanedba.util.AtmosphereServer.register();

        // Register JSON dynamic data loaders using non-deprecated ResourceLoader
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
            Identifier.fromNamespaceAndPath("dragonblockarcanedba", "race_configs"),
            new com.dragonblockarcanedba.config.RaceConfigManager()
        );
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
            Identifier.fromNamespaceAndPath("dragonblockarcanedba", "races"),
            new RaceLoader()
        );
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
            Identifier.fromNamespaceAndPath("dragonblockarcanedba", "forms"),
            new FormLoader()
        );
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
            Identifier.fromNamespaceAndPath("dragonblockarcanedba", "abilities"),
            new com.dragonblockarcanedba.ability.AbilityLoader()
        );
        com.dragonblockarcanedba.ability.AbilityService.register();

        // Register creative tab modification using new Fabric 26.2 API
        // Moved items to custom tab in DbaItems.java

        // Register Commands
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            com.dragonblockarcanedba.command.DbaCommand.register(dispatcher);
        });

        // Register Attack Hook for melee stamina drain (all melee, no weapon exceptions)
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide()) {
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
            com.dragonblockarcanedba.compat.BpmCompatServer.onPlayerJoin(player);
            if (!accessor.dba$hasSelectedRace()) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.dragonblockarcanedba.network.RaceSelectOpenPayload());
            }
            
            // Sync Race Configs
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.dragonblockarcanedba.network.RaceConfigsSyncPayload(com.dragonblockarcanedba.config.RaceConfigManager.getAllConfigs()));

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
            com.dragonblockarcanedba.ki.KiTechniqueHandler.onPlayerDisconnect(playerUuid);
            com.dragonblockarcanedba.ki.KiBarrier.onPlayerDisconnect(playerUuid);
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
