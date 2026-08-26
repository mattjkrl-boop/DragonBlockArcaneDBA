package com.dragonblockarcanedba.dimension;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;

/**
 * Handles dimension travel for the Space Pod and Afterlife systems.
 * Maps planet names to dimension resource keys, resolves player personal spawn points or world spawns,
 * guarantees safe non-suffocating landing spots, and teleports the player smoothly.
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
     * If the player has a personal spawn point in that dimension, teleports them there if safe.
     * Otherwise, locates the nearest safe landing spot around world spawn.
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
        if (server == null) {
            return false;
        }

        ServerLevel targetLevel = server.getLevel(targetKey);
        if (targetLevel == null) {
            DragonBlockArcaneDBA.LOGGER.warn("Dimension {} is not loaded!", targetKey.identifier());
            return false;
        }

        // Don't teleport if already in the target dimension
        if (player.level().dimension().equals(targetKey)) {
            return false;
        }

        // Launch sounds at origin
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LAUNCH, net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.6f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 0.8f);

        double targetX;
        double targetY;
        double targetZ;
        float targetYaw = player.getYRot();
        float targetPitch = player.getXRot();

        if ("otherworld".equalsIgnoreCase(destination)) {
            // Otherworld Check-In Station
            Vec3 stationSpawn = OtherworldStationGenerator.ensureStationAndGetSpawn(targetLevel);
            targetX = stationSpawn.x;
            targetY = stationSpawn.y;
            targetZ = stationSpawn.z;
            targetYaw = 0.0f; // Facing north toward King Yemma's desk
            targetPitch = 0.0f;
        } else {
            // 1. Resolve candidate spawn position
            BlockPos candidatePos = null;
            try {
                ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
                if (respawnConfig != null) {
                    // Check fields/methods via reflection or direct access safely
                    for (java.lang.reflect.Field f : respawnConfig.getClass().getDeclaredFields()) {
                        f.setAccessible(true);
                        Object val = f.get(respawnConfig);
                        if (val instanceof ResourceKey<?> rk && rk.equals(targetKey)) {
                            // Dimension matched, now look for BlockPos
                            for (java.lang.reflect.Field pf : respawnConfig.getClass().getDeclaredFields()) {
                                pf.setAccessible(true);
                                Object pVal = pf.get(respawnConfig);
                                if (pVal instanceof BlockPos bp) {
                                    candidatePos = bp;
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            // 2. If no personal spawn in this dimension, use the dimension's world spawn
            if (candidatePos == null) {
                try {
                    candidatePos = targetLevel.getRespawnData().pos();
                } catch (Throwable ignored) {
                    candidatePos = new BlockPos(0, 64, 0);
                }
            }

            // 3. Find the nearest safe, non-suffocating spot around the candidate
            Vec3 safePos = findSafeLandingPosition(targetLevel, candidatePos);
            targetX = safePos.x;
            targetY = safePos.y;
            targetZ = safePos.z;
        }

        // Reset fall momentum and extinguish fire
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        player.clearFire();

        player.teleportTo(
            targetLevel,
            targetX,
            targetY,
            targetZ,
            Set.of(),
            targetYaw,
            targetPitch,
            false
        );

        // Arrival sounds at destination
        targetLevel.playSound(null, targetX, targetY, targetZ,
            net.minecraft.sounds.SoundEvents.PORTAL_TRAVEL, net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 1.2f);
        targetLevel.playSound(null, targetX, targetY, targetZ,
            net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 1.4f);

        DragonBlockArcaneDBA.LOGGER.info("Player {} traveled safely to {} at ({}, {}, {})",
                player.getName().getString(), destination, targetX, targetY, targetZ);
        return true;
    }

    /**
     * Checks whether a given position has solid footing and 2 blocks of non-suffocating space.
     */
    public static boolean isSafeStandingSpot(ServerLevel level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);

        // Floor must be solid and non-damaging
        if (below.isAir() || !below.blocksMotion()) {
            return false;
        }
        if (below.is(Blocks.LAVA) || below.is(Blocks.FIRE) || below.is(Blocks.SOUL_FIRE)
                || below.is(Blocks.MAGMA_BLOCK) || below.is(Blocks.CACTUS) || below.is(Blocks.SWEET_BERRY_BUSH)) {
            return false;
        }

        // Feet and head space must be clear (non-suffocating)
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());

        if (feet.blocksMotion() || feet.isSuffocating(level, pos) || feet.is(Blocks.LAVA) || feet.is(Blocks.FIRE)) {
            return false;
        }
        if (head.blocksMotion() || head.isSuffocating(level, pos.above()) || head.is(Blocks.LAVA) || head.is(Blocks.FIRE)) {
            return false;
        }

        return true;
    }

    /**
     * Finds the closest guaranteed safe landing spot around a candidate position, searching in an expanding spiral.
     * If no safe spot is naturally available, automatically constructs a clean 3x3 platform with clear airspace.
     */
    public static Vec3 findSafeLandingPosition(ServerLevel level, BlockPos basePos) {
        int originX = basePos.getX();
        int originZ = basePos.getZ();

        // 1. Preload the primary chunk
        level.getChunk(originX >> 4, originZ >> 4, ChunkStatus.FULL, true);

        // 2. Direct check on candidate position
        if (isSafeStandingSpot(level, basePos)) {
            return new Vec3(originX + 0.5, basePos.getY(), originZ + 0.5);
        }

        // 3. Search surface heightmap directly at origin
        int directSurfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, originX, originZ);
        BlockPos directSurface = new BlockPos(originX, directSurfaceY, originZ);
        if (isSafeStandingSpot(level, directSurface)) {
            return new Vec3(originX + 0.5, directSurfaceY, originZ + 0.5);
        }

        // 4. Expanding spiral search (radius 1 to 24 blocks)
        for (int r = 1; r <= 24; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue; // Only check outer perimeter of current ring
                    }
                    int checkX = originX + dx;
                    int checkZ = originZ + dz;

                    // Ensure chunk is loaded
                    level.getChunk(checkX >> 4, checkZ >> 4, ChunkStatus.FULL, true);

                    // Check top surface
                    int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, checkX, checkZ);
                    if (surfaceY > level.getMinY() && surfaceY < level.getMaxY()) {
                        BlockPos candidate = new BlockPos(checkX, surfaceY, checkZ);
                        if (isSafeStandingSpot(level, candidate)) {
                            return new Vec3(checkX + 0.5, surfaceY, checkZ + 0.5);
                        }
                    }

                    // Check downward for caves/structures around the base altitude
                    int startY = Math.min(surfaceY, basePos.getY() + 10);
                    int endY = Math.max(level.getMinY() + 1, basePos.getY() - 30);
                    for (int checkY = startY; checkY >= endY; checkY--) {
                        BlockPos candidate = new BlockPos(checkX, checkY, checkZ);
                        if (isSafeStandingSpot(level, candidate)) {
                            return new Vec3(checkX + 0.5, checkY, checkZ + 0.5);
                        }
                    }
                }
            }
        }

        // 5. Fallback: Construct a safe 3x3 platform to prevent falling into the void or suffocating in solid terrain
        int safeY = Math.max(64, Math.min(140, basePos.getY()));
        BlockPos landing = new BlockPos(originX, safeY, originZ);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlockAndUpdate(landing.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                level.setBlockAndUpdate(landing.offset(x, 0, z), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(landing.offset(x, 1, z), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(landing.offset(x, 2, z), Blocks.AIR.defaultBlockState());
            }
        }

        return new Vec3(landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5);
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
