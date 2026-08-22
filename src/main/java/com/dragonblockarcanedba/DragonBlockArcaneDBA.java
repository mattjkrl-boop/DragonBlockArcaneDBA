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
                    stack.getItem() instanceof com.dragonblockarcanedba.item.BraveSwordItem) {
                    return net.minecraft.world.InteractionResult.FAIL;
                }
            }
            return net.minecraft.world.InteractionResult.PASS;
        });

        // Register Attack Hook for Stamina Drain
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            net.minecraft.world.item.Item heldItem = player.getItemInHand(hand).getItem();
            if (heldItem instanceof com.dragonblockarcanedba.item.SaberItem ||
                heldItem instanceof com.dragonblockarcanedba.item.GrandSwordItem ||
                heldItem instanceof com.dragonblockarcanedba.item.OxKingsAxeItem) {
                return net.minecraft.world.InteractionResult.FAIL;
            }

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
                
                // Prevent normal death
                player.setHealth(player.getMaxHealth());
                player.removeAllEffects();
                
                // Add blindness for a smooth "passing out" fade to black transition
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 80, 1, false, false, false));

                
                // Find otherworld
                net.minecraft.server.level.ServerLevel otherworld = ((net.minecraft.server.level.ServerLevel)entity.level()).getServer().getLevel(
                    net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id("otherworld"))
                );
                
                if (otherworld != null) {
                    int startY = 100;
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(0, startY, 0);
                    
                    // Generate structure if floor isn't planks
                    net.minecraft.core.BlockPos floor = pos.below();
                    if (!otherworld.getBlockState(floor).is(net.minecraft.world.level.block.Blocks.OAK_PLANKS)) {
                        for(int x = -3; x <= 3; x++) {
                            for(int y = -1; y <= 4; y++) {
                                for(int z = -3; z <= 3; z++) {
                                    net.minecraft.core.BlockPos p = pos.offset(x, y, z);
                                    if (y == -1 || y == 4 || x == -3 || x == 3 || z == -3 || z == 3) {
                                        otherworld.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState());
                                    } else {
                                        otherworld.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                                    }
                                }
                            }
                        }
                        // Entrance doorway
                        otherworld.setBlockAndUpdate(pos.offset(0, 0, -3), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                        otherworld.setBlockAndUpdate(pos.offset(0, 1, -3), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                        
                        // Glass Windows for natural light and view outside
                        otherworld.setBlockAndUpdate(pos.offset(-3, 1, 0), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
                        otherworld.setBlockAndUpdate(pos.offset(3, 1, 0), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
                        
                        // Desk
                        otherworld.setBlockAndUpdate(pos.offset(0, 0, 1), net.minecraft.world.level.block.Blocks.SPRUCE_STAIRS.defaultBlockState());
                        
                        // Light sources (Lanterns)
                        otherworld.setBlockAndUpdate(pos.offset(1, 1, 1), net.minecraft.world.level.block.Blocks.LANTERN.defaultBlockState());
                        otherworld.setBlockAndUpdate(pos.offset(-1, 1, 1), net.minecraft.world.level.block.Blocks.LANTERN.defaultBlockState());
                        otherworld.setBlockAndUpdate(pos.offset(0, 3, 0), net.minecraft.world.level.block.Blocks.LANTERN.defaultBlockState());
                        
                        // Spawn guide
                        com.dragonblockarcanedba.entity.OtherworldGuideEntity guide = com.dragonblockarcanedba.entity.DbaEntities.OTHERWORLD_GUIDE.create(otherworld, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                        if (guide != null) {
                            guide.setPos(0.5, startY, 2.5);
                            otherworld.addFreshEntity(guide);
                        }
                    }
                    
                    player.teleportTo(otherworld, 0.5, startY, -1.5, java.util.Collections.emptySet(), 0, 0, false);
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
            com.dragonblockarcanedba.ki.KiTechniqueHandler.onPlayerDisconnect(playerUuid);
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
        });

    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
