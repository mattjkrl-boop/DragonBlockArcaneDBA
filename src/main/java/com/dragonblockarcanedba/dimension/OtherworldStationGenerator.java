package com.dragonblockarcanedba.dimension;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.entity.DbaEntities;
import com.dragonblockarcanedba.entity.OtherworldGuideEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Procedural generator and manager for King Yemma's Check-In Station in the Otherworld.
 * Generates an ornate celestial pavilion with marble pillars, gold inlays, and chandeliers,
 * and guarantees that the Otherworld Guide entity is always present at their desk.
 */
public class OtherworldStationGenerator {

    public static final int STATION_Y = 100;
    public static final BlockPos STATION_CENTER = new BlockPos(0, STATION_Y, 0);

    private static Block getBlockOrFallback(String name, Block fallback) {
        var opt = BuiltInRegistries.BLOCK.getOptional(ResourceKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(name)));
        return opt.orElse(fallback);
    }

    /**
     * Ensures the Check-In Station structure exists and that the Otherworld Guide is alive.
     * Returns the safe player spawn position.
     */
    public static Vec3 ensureStationAndGetSpawn(ServerLevel level) {
        BlockPos floorPos = STATION_CENTER.below();
        
        // If the foundation block is not already placed, build the entire pavilion
        if (!level.getBlockState(floorPos).is(Blocks.POLISHED_BLACKSTONE)) {
            buildStation(level, STATION_CENTER);
        }

        // Ensure Guide Entity is present
        ensureGuide(level, STATION_CENTER);

        // Return player spawn position (standing in front of the desk, facing north)
        return new Vec3(0.5, STATION_Y, -2.5);
    }

    /**
     * Builds the celestial Check-In Station pavilion.
     */
    private static void buildStation(ServerLevel level, BlockPos center) {
        int baseY = center.getY();
        Block redCarpet = getBlockOrFallback("red_carpet", Blocks.GOLD_BLOCK);
        Block yellowCarpet = getBlockOrFallback("yellow_carpet", Blocks.SMOOTH_QUARTZ);
        Block chainBlock = getBlockOrFallback("chain", Blocks.POLISHED_BLACKSTONE_WALL);

        // 1. Clear bounding volume: X [-7 to 7], Z [-8 to 9], Y [0 to 9]
        for (int x = -7; x <= 7; x++) {
            for (int z = -8; z <= 9; z++) {
                for (int y = -3; y <= 9; y++) {
                    BlockPos p = center.offset(x, y, z);
                    if (y < 0) {
                        // Solid deep foundation
                        level.setBlockAndUpdate(p, Blocks.GILDED_BLACKSTONE.defaultBlockState());
                    } else {
                        level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }

        // 2. Floor layout (Y = -1):
        // Main floor: Polished Blackstone with Quartz border and Gold / Sea Lantern accents
        for (int x = -6; x <= 6; x++) {
            for (int z = -7; z <= 7; z++) {
                BlockPos p = center.offset(x, -1, z);
                if (Math.abs(x) == 6 || Math.abs(z) == 7) {
                    level.setBlockAndUpdate(p, Blocks.SMOOTH_QUARTZ.defaultBlockState());
                } else if (Math.abs(x) == 5 || Math.abs(z) == 6) {
                    level.setBlockAndUpdate(p, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                } else if (Math.abs(x) <= 1) {
                    // Central Gold runner floor
                    level.setBlockAndUpdate(p, Blocks.GOLD_BLOCK.defaultBlockState());
                } else {
                    level.setBlockAndUpdate(p, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                }
            }
        }

        // Sea Lantern Floor Inlays for ambient celestial lighting
        level.setBlockAndUpdate(center.offset(-4, -1, -4), Blocks.SEA_LANTERN.defaultBlockState());
        level.setBlockAndUpdate(center.offset(4, -1, -4), Blocks.SEA_LANTERN.defaultBlockState());
        level.setBlockAndUpdate(center.offset(-4, -1, 4), Blocks.SEA_LANTERN.defaultBlockState());
        level.setBlockAndUpdate(center.offset(4, -1, 4), Blocks.SEA_LANTERN.defaultBlockState());
        level.setBlockAndUpdate(center.offset(0, -1, 0), Blocks.SEA_LANTERN.defaultBlockState());

        // Carpet runner on top of floor
        for (int z = -6; z <= 2; z++) {
            level.setBlockAndUpdate(center.offset(0, 0, z), redCarpet.defaultBlockState());
            level.setBlockAndUpdate(center.offset(-1, 0, z), yellowCarpet.defaultBlockState());
            level.setBlockAndUpdate(center.offset(1, 0, z), yellowCarpet.defaultBlockState());
        }

        // 3. Pillars (Y = 0 to 5) at corners and sides:
        int[] pillarXs = {-5, 5, -5, 5, -5, 5};
        int[] pillarZs = {-6, -6, 0, 0, 6, 6};
        for (int i = 0; i < pillarXs.length; i++) {
            int px = pillarXs[i];
            int pz = pillarZs[i];
            for (int y = 0; y <= 5; y++) {
                BlockPos p = center.offset(px, y, pz);
                if (y == 0 || y == 5) {
                    level.setBlockAndUpdate(p, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
                } else {
                    level.setBlockAndUpdate(p, Blocks.QUARTZ_PILLAR.defaultBlockState());
                }
            }
        }

        // 4. Low Walls / Balustrades on sides (Y = 0):
        for (int z = -6; z <= 6; z++) {
            if (z != 0 && z != -6 && z != 6) {
                level.setBlockAndUpdate(center.offset(-5, 0, z), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
                level.setBlockAndUpdate(center.offset(5, 0, z), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
            }
        }

        // 5. Back Wall (Z = 6 and 7):
        for (int x = -4; x <= 4; x++) {
            for (int y = 0; y <= 4; y++) {
                BlockPos p = center.offset(x, y, 6);
                if (Math.abs(x) <= 2 && y >= 1 && y <= 3) {
                    level.setBlockAndUpdate(p, Blocks.TINTED_GLASS.defaultBlockState());
                } else {
                    level.setBlockAndUpdate(p, Blocks.SMOOTH_QUARTZ.defaultBlockState());
                }
            }
        }

        // 6. Roof & Ceiling Canopy (Y = 6 to 8):
        for (int x = -6; x <= 6; x++) {
            for (int z = -7; z <= 7; z++) {
                BlockPos p = center.offset(x, 6, z);
                if (Math.abs(x) == 6 || Math.abs(z) == 7) {
                    level.setBlockAndUpdate(p, Blocks.SMOOTH_QUARTZ.defaultBlockState());
                } else {
                    level.setBlockAndUpdate(p, Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState());
                }
            }
        }

        // Raised Central Dome / Skylight (Y = 7 to 8):
        for (int x = -3; x <= 3; x++) {
            for (int z = -4; z <= 4; z++) {
                level.setBlockAndUpdate(center.offset(x, 6, z), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(center.offset(x, 7, z), Blocks.GOLD_BLOCK.defaultBlockState());
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -3; z <= 3; z++) {
                level.setBlockAndUpdate(center.offset(x, 7, z), Blocks.SEA_LANTERN.defaultBlockState());
            }
        }

        // 7. Chandeliers / Lanterns:
        for (int i = 0; i < pillarXs.length; i++) {
            int px = pillarXs[i];
            int pz = pillarZs[i];
            BlockPos chain = center.offset(px, 5, pz);
            level.setBlockAndUpdate(chain, Blocks.SEA_LANTERN.defaultBlockState());
        }

        // Central Chandelier
        level.setBlockAndUpdate(center.offset(0, 6, -1), chainBlock.defaultBlockState());
        level.setBlockAndUpdate(center.offset(0, 5, -1), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));

        // 8. King Yemma's Check-In Desk (at Z = 3):
        for (int x = -2; x <= 2; x++) {
            BlockPos deskPos = center.offset(x, 0, 3);
            if (x == -2 || x == 2) {
                level.setBlockAndUpdate(deskPos, Blocks.GOLD_BLOCK.defaultBlockState());
            } else {
                level.setBlockAndUpdate(deskPos, Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState()
                        .setValue(StairBlock.FACING, Direction.SOUTH));
            }
            level.setBlockAndUpdate(center.offset(x, 0, 4), Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState());
        }
        // Desk accessories
        level.setBlockAndUpdate(center.offset(-1, 1, 3), Blocks.LANTERN.defaultBlockState());
        level.setBlockAndUpdate(center.offset(1, 1, 3), Blocks.LANTERN.defaultBlockState());
        level.setBlockAndUpdate(center.offset(0, 1, 3), Blocks.LECTERN.defaultBlockState());

        // 9. Grand Pathway extending out front (Z = -7 to -18):
        for (int z = -8; z >= -18; z--) {
            for (int x = -2; x <= 2; x++) {
                BlockPos p = center.offset(x, -1, z);
                if (Math.abs(x) == 2) {
                    level.setBlockAndUpdate(p, Blocks.SMOOTH_QUARTZ.defaultBlockState());
                    if (z % 4 == 0) {
                        level.setBlockAndUpdate(center.offset(x, 0, z), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
                        level.setBlockAndUpdate(center.offset(x, 1, z), Blocks.SOUL_LANTERN.defaultBlockState());
                    }
                } else {
                    level.setBlockAndUpdate(p, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                }
            }
        }

        DragonBlockArcaneDBA.LOGGER.info("Successfully generated King Yemma's Check-In Station in Otherworld at {}", center);
    }

    /**
     * Ensures that the Otherworld Guide entity is alive and placed at the desk.
     */
    private static void ensureGuide(ServerLevel level, BlockPos center) {
        BlockPos guidePos = center.offset(0, 0, 4);
        AABB box = new AABB(
                center.getX() - 5, center.getY() - 2, center.getZ() - 5,
                center.getX() + 5, center.getY() + 5, center.getZ() + 7
        );

        List<OtherworldGuideEntity> existingGuides = level.getEntitiesOfClass(OtherworldGuideEntity.class, box);
        if (existingGuides.isEmpty()) {
            OtherworldGuideEntity guide = DbaEntities.OTHERWORLD_GUIDE.create(level, EntitySpawnReason.COMMAND);
            if (guide != null) {
                guide.setPos(guidePos.getX() + 0.5, guidePos.getY(), guidePos.getZ() + 0.5);
                guide.setYRot(180.0f);
                guide.setYHeadRot(180.0f);
                level.addFreshEntity(guide);
                DragonBlockArcaneDBA.LOGGER.info("Spawned fresh Otherworld Guide at {}", guidePos);
            }
        }
    }
}
