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
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Procedural generator and manager for King Yemma's Check-In Station in the Otherworld.
 * Ensures the terrain is generated first, finds the optimal surface height for the temple,
 * clears all intersecting terrain features, builds the ornate celestial pavilion and foundation,
 * and spawns the player safely inside the temple at King Yemma's desk.
 */
public class OtherworldStationGenerator {

    private static Block getBlockOrFallback(String name, Block fallback) {
        var opt = BuiltInRegistries.BLOCK.getOptional(ResourceKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(name)));
        return opt.orElse(fallback);
    }

    /**
     * Finds an existing station in the Otherworld, or generates a new one on top of the terrain.
     * Guarantees the Otherworld Guide is at the desk and returns the safe player spawn position.
     */
    public static Vec3 ensureStationAndGetSpawn(ServerLevel level) {
        // 1. Force load and complete chunk generation in the 3x3 chunk area around (0, 0)
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                level.getChunk(cx, cz, ChunkStatus.FULL, true);
            }
        }

        // 2. Check if the station has already been built by scanning at X=0, Z=0
        BlockPos existingCenter = findExistingStation(level);

        BlockPos stationCenter;
        if (existingCenter != null) {
            stationCenter = existingCenter;
            DragonBlockArcaneDBA.LOGGER.info("Located existing Otherworld Check-In Station at {}", stationCenter);
        } else {
            // Find optimal surface Y from the generated terrain (checking only raw terrain)
            int maxGround = 64;
            for (int x = -6; x <= 6; x += 3) {
                for (int z = -16; z <= 6; z += 3) {
                    int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    if (h > maxGround && h < 200) {
                        maxGround = h;
                    }
                }
            }

            int baseY = Math.max(70, Math.min(160, maxGround + 1));
            stationCenter = new BlockPos(0, baseY, 0);

            // Build the temple and clear out any terrain/spires/crystals
            buildStation(level, stationCenter);
        }

        // 3. Guarantee that the spawn airspace in front of King Yemma's desk is 100% clear of obstruction
        Block redCarpet = getBlockOrFallback("red_carpet", Blocks.GOLD_BLOCK);
        Block yellowCarpet = getBlockOrFallback("yellow_carpet", Blocks.SMOOTH_QUARTZ);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -5; dz <= 0; dz++) {
                // Ensure solid floor below
                BlockPos floorPos = stationCenter.offset(dx, -1, dz);
                if (level.getBlockState(floorPos).isAir()) {
                    level.setBlockAndUpdate(floorPos, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                }

                // Clear standing airspace
                for (int dy = 0; dy <= 3; dy++) {
                    BlockPos p = stationCenter.offset(dx, dy, dz);
                    if (dy == 0) {
                        // Maintain carpet on floor
                        if (dx == 0) {
                            level.setBlockAndUpdate(p, redCarpet.defaultBlockState());
                        } else if (Math.abs(dx) == 1) {
                            level.setBlockAndUpdate(p, yellowCarpet.defaultBlockState());
                        } else if (level.getBlockState(p).blocksMotion()) {
                            level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                        }
                    } else {
                        // Open airspace above
                        if (level.getBlockState(p).blocksMotion() || level.getBlockState(p).isSuffocating(level, p)) {
                            level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
            }
        }

        // 4. Ensure the Otherworld Guide is alive at the desk
        ensureGuide(level, stationCenter);

        // 5. Return safe spawn location in front of King Yemma's desk
        return new Vec3(stationCenter.getX() + 0.5, stationCenter.getY(), stationCenter.getZ() - 2.5);
    }

    /**
     * Checks if the station was previously generated by scanning for key structural anchors:
     * - The King Yemma desk lectern at (0, y+1, 3)
     * - The quartz pillars at (-5, y, -6) and (5, y, 6)
     * - The central sea lantern floor inlay at (0, y-1, 0)
     */
    private static BlockPos findExistingStation(ServerLevel level) {
        for (int y = 50; y <= 220; y++) {
            BlockPos lecternPos = new BlockPos(0, y + 1, 3);
            BlockPos deskSlabPos = new BlockPos(0, y, 4);
            BlockPos centerFloorPos = new BlockPos(0, y - 1, 0);

            // 1. Primary check: King Yemma's desk
            if (level.getBlockState(lecternPos).is(Blocks.LECTERN)) {
                return new BlockPos(0, y, 0);
            }

            // 2. Secondary check: Desk slab + floor
            if (level.getBlockState(deskSlabPos).is(Blocks.POLISHED_BLACKSTONE_SLAB)
                    && (level.getBlockState(centerFloorPos).is(Blocks.SEA_LANTERN) || level.getBlockState(centerFloorPos).is(Blocks.GOLD_BLOCK))) {
                return new BlockPos(0, y, 0);
            }

            // 3. Tertiary check: Chiseled Quartz Pillars at pavilion corners
            if (level.getBlockState(new BlockPos(-5, y, -6)).is(Blocks.CHISELED_QUARTZ_BLOCK)
                    && level.getBlockState(new BlockPos(5, y, 6)).is(Blocks.CHISELED_QUARTZ_BLOCK)
                    && level.getBlockState(centerFloorPos).is(Blocks.SEA_LANTERN)) {
                return new BlockPos(0, y, 0);
            }
        }
        return null;
    }

    /**
     * Builds the celestial Check-In Station pavilion and foundation.
     */
    private static void buildStation(ServerLevel level, BlockPos center) {
        Block redCarpet = getBlockOrFallback("red_carpet", Blocks.GOLD_BLOCK);
        Block yellowCarpet = getBlockOrFallback("yellow_carpet", Blocks.SMOOTH_QUARTZ);
        Block chainBlock = getBlockOrFallback("chain", Blocks.POLISHED_BLACKSTONE_WALL);

        // 1. CLEAR AIRSPACE: X [-8 to 8], Z [-22 to 10], Y [0 to 14]
        // This ensures no rock spires, cloud blocks, ores, or terrain intersect the temple interior!
        for (int x = -8; x <= 8; x++) {
            for (int z = -22; z <= 10; z++) {
                for (int y = 0; y <= 14; y++) {
                    BlockPos p = center.offset(x, y, z);
                    level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                }
            }
        }

        // 2. SOLID DEEP FOUNDATION: Anchor down into the terrain below
        for (int x = -7; x <= 7; x++) {
            for (int z = -8; z <= 8; z++) {
                for (int dy = -1; dy >= -15; dy--) {
                    BlockPos p = center.offset(x, dy, z);
                    if (dy == -1) {
                        level.setBlockAndUpdate(p, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                    } else if (dy >= -4) {
                        level.setBlockAndUpdate(p, Blocks.GILDED_BLACKSTONE.defaultBlockState());
                    } else {
                        if (level.getBlockState(p).isAir()) {
                            level.setBlockAndUpdate(p, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                        }
                    }
                }
            }
        }

        // Foundation under the grand pathway (Z = -9 to -22)
        for (int z = -9; z >= -22; z--) {
            for (int x = -3; x <= 3; x++) {
                for (int dy = -1; dy >= -10; dy--) {
                    BlockPos p = center.offset(x, dy, z);
                    if (dy == -1) {
                        level.setBlockAndUpdate(p, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                    } else if (level.getBlockState(p).isAir()) {
                        level.setBlockAndUpdate(p, Blocks.GILDED_BLACKSTONE.defaultBlockState());
                    }
                }
            }
        }

        // 3. MAIN FLOOR LAYOUT (Y = -1):
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

        // 4. PILLARS (Y = 0 to 5) at corners and sides:
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

        // 5. LOW WALLS / BALUSTRADES on sides (Y = 0):
        for (int z = -6; z <= 6; z++) {
            if (z != 0 && z != -6 && z != 6) {
                level.setBlockAndUpdate(center.offset(-5, 0, z), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
                level.setBlockAndUpdate(center.offset(5, 0, z), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
            }
        }

        // 6. BACK WALL (Z = 6 and 7):
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

        // 7. ROOF & CEILING CANOPY (Y = 6 to 8):
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

        // 8. CHANDELIERS / LANTERNS:
        for (int i = 0; i < pillarXs.length; i++) {
            int px = pillarXs[i];
            int pz = pillarZs[i];
            BlockPos chain = center.offset(px, 5, pz);
            level.setBlockAndUpdate(chain, Blocks.SEA_LANTERN.defaultBlockState());
        }

        // Central Chandelier
        level.setBlockAndUpdate(center.offset(0, 6, -1), chainBlock.defaultBlockState());
        level.setBlockAndUpdate(center.offset(0, 5, -1), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));

        // 9. KING YEMMA'S CHECK-IN DESK (at Z = 3):
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

        // 10. GRAND PATHWAY extending out front (Z = -8 to -22):
        for (int z = -8; z >= -22; z--) {
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
