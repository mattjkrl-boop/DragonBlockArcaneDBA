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

        // Register Items
        DbaItems.register();

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
