package com.dragonblockarcanedba.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Permanent weapon destruction (destruction expansion, Carter Nov 4
 * 2026): weapons change the world and the world keeps the change.
 * This class replaced the old restore system; nothing in it restores,
 * schedules, or records anything. A destroyed block is an ordinary
 * world edit, saved by the chunk system, so crash safety needs no
 * machinery here: there is no pending state to lose.
 *
 * The refusal rules are the old system's, kept whole: protected blocks
 * (anything with a block entity, bedrock, barriers, spawners, portal
 * frames and portals, command and structure blocks, jigsaws) are
 * never touched, and unbreakable blocks (negative destroy speed) are
 * refused on top of that regardless of the caller's power tier.
 * Fluids are never destroyed. Every sized operation takes a block
 * budget and clamps it to HARD_MAX_BLOCKS, so no single ability use
 * can delete a mountain or stall the server, and every operation is
 * safe under repeated calls: destroyed cells are air and are skipped,
 * refused cells stay refused.
 *
 * Server-side only by construction: every method takes a ServerLevel.
 */
public final class PermanentDestruction {
    private PermanentDestruction() {}

    /** Absolute ceiling on blocks changed by one call, whatever the caller asks for. */
    public static final int HARD_MAX_BLOCKS = 1024;

    /** Resistance tiers for callers (explosion resistance caps). */
    public static final float TIER_WEAK = 2.0f;
    public static final float TIER_STANDARD = 6.0f;
    public static final float TIER_HEAVY = 50.0f;

    /**
     * The protected-block test, moved here verbatim when the restore
     * system retired: anything with a block entity, plus bedrock,
     * barriers, spawners, portal frames and portals, command and
     * structure blocks, and jigsaws are never touched by weapon
     * terrain work.
     */
    public static boolean isProtected(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) != null) return true;
        Block b = state.getBlock();
        return b == Blocks.BEDROCK || b == Blocks.BARRIER || b == Blocks.SPAWNER
            || b == Blocks.END_PORTAL_FRAME || b == Blocks.END_PORTAL || b == Blocks.NETHER_PORTAL
            || b == Blocks.COMMAND_BLOCK || b == Blocks.CHAIN_COMMAND_BLOCK || b == Blocks.REPEATING_COMMAND_BLOCK
            || b == Blocks.STRUCTURE_BLOCK || b == Blocks.JIGSAW;
    }

    /**
     * The refusal test for breaking one cell. Air is not destroyable
     * (nothing to break), fluids are left alone, protected and
     * unbreakable blocks are refused, and anything tougher than the
     * caller's resistance cap survives.
     */
    public static boolean canDestroy(ServerLevel level, BlockPos pos, BlockState state,
                                     float maxBlastResistance) {
        if (state.isAir()) return false;
        if (!level.isLoaded(pos) || !level.isInWorldBounds(pos)) return false;
        if (state.getBlock() instanceof LiquidBlock) return false;
        if (isProtected(level, pos, state)) return false;
        if (state.getDestroySpeed(level, pos) < 0.0f) return false;
        return state.getBlock().getExplosionResistance() <= maxBlastResistance;
    }

    /** Break one block permanently. Returns true when a block was destroyed. */
    public static boolean destroy(ServerLevel level, BlockPos pos, boolean dropItems,
                                  float maxBlastResistance) {
        BlockState state = level.getBlockState(pos);
        if (!canDestroy(level, pos, state, maxBlastResistance)) return false;
        return level.destroyBlock(pos.immutable(), dropItems);
    }

    /**
     * A permanent crater: a jittered ellipsoid bowl centered at
     * center, radius wide and radius * depthScale deep and tall.
     * The jitter keeps the rim ragged instead of a clean geometric
     * cut. Stops at maxBlocks (clamped to the hard ceiling) and
     * returns how many blocks were destroyed.
     */
    public static int crater(ServerLevel level, BlockPos center, double radius, double depthScale,
                             boolean dropItems, float maxBlastResistance, int maxBlocks,
                             RandomSource rand) {
        int budget = clampBudget(maxBlocks);
        if (budget <= 0 || radius <= 0.0) return 0;
        double vertical = Math.max(1.0, radius * Math.max(0.1, depthScale));
        int r = (int) Math.ceil(radius);
        int rv = (int) Math.ceil(vertical);
        int destroyed = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = -rv; dy <= rv && destroyed < budget; dy++) {
            for (int dx = -r; dx <= r && destroyed < budget; dx++) {
                for (int dz = -r; dz <= r && destroyed < budget; dz++) {
                    double nx = dx / radius;
                    double ny = dy / vertical;
                    double nz = dz / radius;
                    double d = nx * nx + ny * ny + nz * nz;
                    // Ragged rim: the ellipsoid edge wobbles by up to
                    // about a fifth of the radius from cell to cell.
                    double edge = 1.0 + (rand.nextDouble() - 0.5) * 0.4;
                    if (d > edge * edge) continue;
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (destroy(level, m, dropItems, maxBlastResistance)) destroyed++;
                }
            }
        }
        return destroyed;
    }

    /**
     * A permanent fissure: a cut line starting at start, running
     * along direction (only its horizontal heading is used) for
     * length blocks, width cells across and depth cells down from
     * the start height. For ground cuts, trenches, and slash lines.
     */
    public static int fissure(ServerLevel level, BlockPos start, Vec3 direction, double length,
                              int width, int depth, boolean dropItems, float maxBlastResistance,
                              int maxBlocks) {
        int budget = clampBudget(maxBlocks);
        if (budget <= 0 || length <= 0.0 || depth <= 0) return 0;
        double len = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (len < 1.0e-4) return 0;
        double dirX = direction.x / len;
        double dirZ = direction.z / len;
        double perpX = -dirZ;
        double perpZ = dirX;
        int destroyed = 0;
        Set<Long> visited = new HashSet<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        double halfWidth = Math.max(0, width - 1) / 2.0;
        for (double step = 0.0; step <= length && destroyed < budget; step += 0.5) {
            double cx = start.getX() + 0.5 + dirX * step;
            double cz = start.getZ() + 0.5 + dirZ * step;
            for (double off = -halfWidth; off <= halfWidth && destroyed < budget; off += 1.0) {
                int bx = (int) Math.floor(cx + perpX * off);
                int bz = (int) Math.floor(cz + perpZ * off);
                for (int dy = 1; dy >= -depth && destroyed < budget; dy--) {
                    m.set(bx, start.getY() + dy, bz);
                    if (!visited.add(m.asLong())) continue;
                    if (destroy(level, m, dropItems, maxBlastResistance)) destroyed++;
                }
            }
        }
        return destroyed;
    }

    /**
     * Destroy the top solid block of each column in a disk at the
     * surface, the permanent version of a scour pass. y in center is
     * the ground level the callers already resolved.
     */
    public static int destroyDiskSurface(ServerLevel level, BlockPos center, int radius, float chance,
                                         boolean dropItems, float maxBlastResistance, RandomSource rand) {
        int placed = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                if (rand.nextFloat() > chance) continue;
                m.set(center.getX() + dx, center.getY(), center.getZ() + dz);
                if (destroy(level, m, dropItems, maxBlastResistance)) placed++;
            }
        }
        return placed;
    }

    /**
     * Permanently replace one block with a themed end state: obsidian
     * fields, blackstone scorch, soul soil, cooled crust, crystal.
     * Same refusal rules as destruction, minus the resistance cap: a
     * conversion is the weapon's own material claiming the cell, but
     * protected and unbreakable blocks still refuse, and a cell that
     * already holds the target state is left alone.
     */
    public static boolean convert(ServerLevel level, BlockPos pos, BlockState newState) {
        BlockState current = level.getBlockState(pos);
        if (current == newState) return false;
        if (!level.isLoaded(pos) || !level.isInWorldBounds(pos)) return false;
        if (isProtected(level, pos, current)) return false;
        if (current.getDestroySpeed(level, pos) < 0.0f) return false;
        return level.setBlock(pos.immutable(), newState, Block.UPDATE_ALL);
    }

    /** Convert a filled disk of surface cells permanently. y is the ground level to write at. */
    public static int convertDisk(ServerLevel level, BlockPos center, int radius, BlockState state,
                                  float chance, RandomSource rand) {
        int placed = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                if (rand.nextFloat() > chance) continue;
                m.set(center.getX() + dx, center.getY(), center.getZ() + dz);
                if (convert(level, m, state)) placed++;
            }
        }
        return placed;
    }

    /**
     * Find the surface cell of one column the way the lava systems do
     * (climb out of a solid hint up to 4, scan down up to 6 for sturdy
     * ground), exposed here so destruction callers resolve ground the
     * same way the rest of the mod does. Returns null when the column
     * has no usable surface.
     */
    public static BlockPos findSurface(ServerLevel level, int x, int z, int yHint) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        cursor.set(x, yHint, z);
        if (!level.isLoaded(cursor) || !level.isInWorldBounds(cursor)) return null;
        if (!level.getBlockState(cursor).isAir()) {
            boolean foundAir = false;
            for (int up = 1; up <= 4; up++) {
                cursor.set(x, yHint + up, z);
                if (!level.isLoaded(cursor) || !level.isInWorldBounds(cursor)) return null;
                if (level.getBlockState(cursor).isAir()) {
                    yHint += up;
                    foundAir = true;
                    break;
                }
            }
            if (!foundAir) return null;
        }
        for (int dy = 0; dy >= -6; dy--) {
            cursor.set(x, yHint + dy, z);
            if (!level.isLoaded(cursor) || !level.isInWorldBounds(cursor)) return null;
            if (!level.getBlockState(cursor).isAir()) return null;
            BlockPos groundPos = new BlockPos(x, cursor.getY() - 1, z);
            BlockState ground = level.getBlockState(groundPos);
            if (!ground.isFaceSturdy(level, groundPos, Direction.UP)) continue;
            return new BlockPos(x, cursor.getY(), z);
        }
        return null;
    }

    private static int clampBudget(int maxBlocks) {
        return Math.min(Math.max(0, maxBlocks), HARD_MAX_BLOCKS);
    }
}
