package com.dragonblockarcanedba;

import com.dragonblockarcanedba.item.DbaItems;
import com.dragonblockarcanedba.registry.FormLoader;
import com.dragonblockarcanedba.registry.RaceLoader;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.dragonblockarcanedba.network.DbaNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        DbaItems.register();

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
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(
            net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES
        ).register(output -> output.accept(DbaItems.SPACE_POD));

        // Register Commands
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            com.dragonblockarcanedba.command.DbaCommand.register(dispatcher);
        });

        // Register Attack Hook for Stamina Drain
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide()) {
                com.dragonblockarcanedba.attribute.PlayerStatsAccessor accessor = (com.dragonblockarcanedba.attribute.PlayerStatsAccessor) player;
                double stamina = accessor.dba$getCurrentStamina();
                if (stamina < 8.0) {
                    // Apply 75% damage penalty approx and attack speed slowdown via effects
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 40, 2, false, false));
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MINING_FATIGUE, 40, 1, false, false));
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
                
                // Prevent normal death
                player.setHealth(player.getMaxHealth());
                player.removeAllEffects();
                
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
                }
                return false; // Cancel death
            }
            return true; // Allow normal death for non-players
        });

        // Trigger Race Selection on first join
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            net.minecraft.server.level.ServerPlayer player = handler.getPlayer();
            com.dragonblockarcanedba.attribute.PlayerStatsAccessor accessor = (com.dragonblockarcanedba.attribute.PlayerStatsAccessor) player;
            if (!accessor.dba$hasSelectedRace()) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.dragonblockarcanedba.network.RaceSelectOpenPayload());
            }
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
