package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.dimension.DimensionTravel;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Namek's biome is plains and Vegeta's is badlands, the same biomes as the overworld.
 * Spawns are therefore limited to those two dimension ids instead of a biome modifier.
 */
public final class DbaHostileSpawns {
    private static final int ATTEMPT_INTERVAL_TICKS = 300;
    private static final int NEARBY_CAP = 3;
    private static final double NEARBY_RANGE = 56.0;

    private DbaHostileSpawns() {}

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(DbaHostileSpawns::onLevelTick);
    }

    private static void onLevelTick(ServerLevel level) {
        boolean namek = level.dimension().equals(DimensionTravel.NAMEK);
        boolean vegeta = level.dimension().equals(DimensionTravel.VEGETA);
        if (!namek && !vegeta) {
            return;
        }
        if (level.getServer().getWorldData().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        if (level.getServer().getTickCount() % ATTEMPT_INTERVAL_TICKS != 0) {
            return;
        }
        List<ServerPlayer> players = level.players();
        for (int i = 0; i < players.size(); i++) {
            trySpawnFor(level, players.get(i), namek);
        }
    }

    private static void trySpawnFor(ServerLevel level, ServerPlayer player, boolean namek) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        RandomSource random = level.getRandom();
        if (random.nextFloat() > 0.4f) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(NEARBY_RANGE);
        int nearby = level.getEntities(EntityTypeTest.forClass(DbaHostileEntity.class), area, DbaHostileEntity::isAlive).size();
        if (nearby >= NEARBY_CAP) {
            return;
        }

        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = 22.0 + random.nextDouble() * 14.0;
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
        BlockPos feet = findFeet(level, x, z);
        if (feet == null || Math.abs(feet.getY() - player.getY()) > 24.0) {
            return;
        }
        if (!level.isPositionEntityTicking(feet)) {
            return;
        }

        EntityType<? extends DbaHostileEntity> type = pickType(namek, random);
        DbaHostileEntity mob = type.create(level, EntitySpawnReason.NATURAL);
        if (mob == null) {
            return;
        }
        mob.setPos(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(feet), EntitySpawnReason.NATURAL, null);
        level.addFreshEntity(mob);
    }

    private static EntityType<? extends DbaHostileEntity> pickType(boolean namek, RandomSource random) {
        if (namek) {
            if (random.nextInt(3) == 0) {
                return DbaEntities.NAMEKIAN_WARRIOR;
            }
            return DbaEntities.FRIEZA_SOLDIER;
        }
        if (random.nextInt(5) == 0) {
            return DbaEntities.FRIEZA_SOLDIER;
        }
        return DbaEntities.SAIBAMAN;
    }

    private static BlockPos findFeet(ServerLevel level, int x, int z) {
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos atSurface = new BlockPos(x, surface, z);
        if (standable(level, atSurface)) {
            return atSurface;
        }
        BlockPos above = atSurface.above();
        if (standable(level, above)) {
            return above;
        }
        return null;
    }

    private static boolean standable(ServerLevel level, BlockPos feet) {
        BlockPos groundPos = new BlockPos(feet.getX(), feet.getY() - 1, feet.getZ());
        BlockState ground = level.getBlockState(groundPos);
        BlockState body = level.getBlockState(feet);
        BlockState head = level.getBlockState(feet.above());
        return ground.isSolid() && body.isAir() && head.isAir() && body.getFluidState().isEmpty();
    }
}
