package com.dragonblockarcanedba.dimension;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Map;
import java.util.Set;

/**
 * Handles dimension travel for the Space Pod system.
 * Maps planet names to dimension resource keys and teleports the player.
 */
public class DimensionTravel {
    // Planet dimension resource keys
    public static final ResourceKey<Level> NAMEK = ResourceKey.create(
        Registries.DIMENSION, DragonBlockArcaneDBA.id("namek")
    );
    public static final ResourceKey<Level> VEGETA = ResourceKey.create(
        Registries.DIMENSION, DragonBlockArcaneDBA.id("vegeta")
    );
    public static final ResourceKey<Level> YARDRAT = ResourceKey.create(
        Registries.DIMENSION, DragonBlockArcaneDBA.id("yardrat")
    );
    public static final ResourceKey<Level> OTHERWORLD = ResourceKey.create(
        Registries.DIMENSION, DragonBlockArcaneDBA.id("otherworld")
    );

    private static final Map<String, ResourceKey<Level>> DESTINATIONS = Map.of(
        "namek", NAMEK,
        "vegeta", VEGETA,
        "yardrat", YARDRAT,
        "otherworld", OTHERWORLD,
        "overworld", Level.OVERWORLD
    );

    // Planet dimension configurations (gravity, atmosphere)
    private static final Map<String, PlanetDimension> PLANET_CONFIGS = Map.of(
        "namek", new PlanetDimension("Namek", 0.9, false, 0),
        "vegeta", new PlanetDimension("Vegeta", 1.5, false, 0),
        "yardrat", new PlanetDimension("Yardrat", 0.8, true, 200)
    );

    /**
     * Teleports a player to the specified planet dimension.
     * If the player is already in a custom DBA dimension, "overworld" returns them home.
     *
     * @return true if teleportation was initiated, false if destination is invalid
     */
    public static boolean travelTo(ServerPlayer player, String destination) {
        ResourceKey<Level> targetKey = DESTINATIONS.get(destination.toLowerCase());
        if (targetKey == null) {
            DragonBlockArcaneDBA.LOGGER.warn("Unknown Space Pod destination: {}", destination);
            return false;
        }

        MinecraftServer server = player.level().getServer();

        ServerLevel targetLevel = server.getLevel(targetKey);
        if (targetLevel == null) {
            DragonBlockArcaneDBA.LOGGER.warn("Dimension {} is not loaded!", targetKey.identifier());
            return false;
        }

        // Don't teleport if already in the target dimension
        if (player.level().dimension().equals(targetKey)) {
            return false;
        }

        // Find a safe landing Y at the center of the target dimension
        int safeY = targetLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0) + 1;
        // Ensure minimum Y of 64 if dimension is freshly generated
        if (safeY < 1) safeY = 64;

        // Launch sounds at origin
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LAUNCH, net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.6f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 0.8f);

        player.teleportTo(
            targetLevel,
            0.5,
            safeY,
            0.5,
            Set.of(),
            player.getYRot(),
            player.getXRot(),
            false
        );

        // Arrival sounds at destination
        targetLevel.playSound(null, 0.5, safeY, 0.5,
            net.minecraft.sounds.SoundEvents.PORTAL_TRAVEL, net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 1.2f);
        targetLevel.playSound(null, 0.5, safeY, 0.5,
            net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 1.4f);

        DragonBlockArcaneDBA.LOGGER.info("Player {} traveled to {}", player.getName().getString(), destination);
        return true;
    }

    /**
     * Gets the PlanetDimension config for the given dimension key.
     * Returns null if the player is not in a DBA custom dimension.
     */
    public static PlanetDimension getPlanetConfig(ResourceKey<Level> dimensionKey) {
        String path = dimensionKey.identifier().getPath();
        return PLANET_CONFIGS.get(path);
    }
}
