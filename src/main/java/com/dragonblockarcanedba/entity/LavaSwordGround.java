package com.dragonblockarcanedba.entity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Surface burn left by the lava sword. Fire goes on air that already sits on a
 * solid face. Each swing also has a budget of real lava sources, and those are
 * placed first, on the first surface cells the swing reaches, so using the
 * sword always leaves some lava on the ground. The rest is fire. Both are
 * put back after a few seconds. The placement skips the fluid's on-place so the
 * source does not flow, and the server tick puts the old block back.
 */
public final class LavaSwordGround {
    /** Four seconds. Long enough to see, short enough that it does not become the world. */
    private static final int LIFE_TICKS = 80;
    /**
     * Clients see the block. Neighbors are not told, and the block's on-place
     * does not run, so lava does not schedule a flow or cook adjacent water
     * into cobblestone, stone, or obsidian.
     */
    private static final int PLACE_FLAGS =
        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SKIP_ON_PLACE;
    private static final BlockState LAVA_SOURCE = Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 0);
    private static final Direction[] NEIGHBORS = Direction.values();
    private static final List<Pending> PENDING = new ArrayList<>();
    private static final AtomicBoolean HOOKED = new AtomicBoolean();

    private LavaSwordGround() {}

    /** Ground under a traveling cleave. {@code lavaBudget} is how many sources this swing may still leave. */
    public static int scorchDisk(ServerLevel level, double x, double y, double z, double radius, int lavaBudget) {
        ensureHooks();
        int placed = 0;
        int reach = Mth.ceil(radius);
        int cx = Mth.floor(x);
        int cz = Mth.floor(z);
        int yHint = Mth.floor(y);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                if (scorchColumn(level, cx + dx, cz + dz, yHint, placed < lavaBudget)) {
                    placed++;
                }
            }
        }
        return placed;
    }

    /**
     * The eruption's moving lip, in the same forward wedge the damage uses.
     * {@code lavaBudget} is how many sources this eruption may still leave.
     */
    public static int scorchArc(ServerLevel level, double x, double y, double z, float yRot,
                                float inner, float outer, int lavaBudget) {
        ensureHooks();
        double yaw = Math.toRadians(yRot);
        double lookX = -Math.sin(yaw);
        double lookZ = Math.cos(yaw);
        int placed = 0;
        int max = Mth.ceil(outer + 0.4);
        int cx = Mth.floor(x);
        int cz = Mth.floor(z);
        int yHint = Mth.floor(y);
        for (int dx = -max; dx <= max; dx++) {
            for (int dz = -max; dz <= max; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > outer + 0.4 || dist < inner - 0.7) {
                    continue;
                }
                double dot = dist < 0.05 ? 1.0 : (dx * lookX + dz * lookZ) / dist;
                if (dot < 0.15) {
                    continue;
                }
                if (scorchColumn(level, cx + dx, cz + dz, yHint, placed < lavaBudget)) {
                    placed++;
                }
            }
        }
        return placed;
    }

    /** Scorch the segment a cleave travels this tick, wide enough to cover the crescent. */
    public static int scorchSegment(ServerLevel level, Vec3 from, Vec3 to, double radius, int lavaBudget) {
        double midX = (from.x + to.x) * 0.5;
        double midY = (from.y + to.y) * 0.5;
        double midZ = (from.z + to.z) * 0.5;
        return scorchDisk(level, midX, midY, midZ, radius, lavaBudget);
    }

    /**
     * Scorch the exact block a cleave landed on. The pile goes in the cell
     * above that block, on top of it, never under it. Returns 1 when a lava
     * source was spent, 0 otherwise.
     */
    public static int scorchLanding(ServerLevel level, BlockPos hitBlock, int lavaBudget) {
        ensureHooks();
        boolean lava = scorchColumn(level, hitBlock.getX(), hitBlock.getZ(), hitBlock.getY() + 1, lavaBudget > 0);
        return lava ? 1 : 0;
    }

    private static boolean scorchColumn(ServerLevel level, int x, int z, int yHint, boolean allowLava) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        // The hint can sit inside a block: a cleave's landing tick ends inside
        // the block it hit, and an eruption ring climbs rising ground. The old
        // code gave up on the first solid cell, so the landing block got no
        // pile at all and the burn showed up on lower ground beside it, under
        // the block it landed on. Climb to the first air cell above instead.
        cursor.set(x, yHint, z);
        if (!level.isLoaded(cursor) || !level.isInWorldBounds(cursor)) {
            return false;
        }
        if (!level.getBlockState(cursor).isAir()) {
            boolean foundAir = false;
            for (int up = 1; up <= 4; up++) {
                cursor.set(x, yHint + up, z);
                if (!level.isLoaded(cursor) || !level.isInWorldBounds(cursor)) {
                    return false;
                }
                if (level.getBlockState(cursor).isAir()) {
                    yHint += up;
                    foundAir = true;
                    break;
                }
            }
            if (!foundAir) {
                return false;
            }
        }
        for (int dy = 0; dy >= -6; dy--) {
            cursor.set(x, yHint + dy, z);
            if (!level.isLoaded(cursor) || !level.isInWorldBounds(cursor)) {
                return false;
            }
            BlockState here = level.getBlockState(cursor);
            if (!here.isAir()) {
                return false;
            }
            BlockPos groundPos = new BlockPos(x, cursor.getY() - 1, z);
            BlockState ground = level.getBlockState(groundPos);
            if (!ground.blocksMotion() || !ground.isFaceSturdy(level, groundPos, Direction.UP)) {
                continue;
            }
            BlockPos surface = new BlockPos(x, cursor.getY(), z);
            return igniteSurface(level, surface, allowLava);
        }
        return false;
    }

    private static boolean igniteSurface(ServerLevel level, BlockPos surface, boolean allowLava) {
        if (alreadyPending(level, surface)) {
            return false;
        }
        BlockState previous = level.getBlockState(surface);
        if (!previous.isAir() || protectedSolid(previous)) {
            return false;
        }
        if (occupiedByPlayer(level, surface)) {
            return false;
        }
        boolean lava = allowLava && !nearWater(level, surface);
        if (lava) {
            if (!level.setBlock(surface, LAVA_SOURCE, PLACE_FLAGS)) {
                return false;
            }
            remember(level, surface, previous, true);
            return true;
        }
        if (!BaseFireBlock.canBePlacedAt(level, surface, Direction.UP)) {
            return false;
        }
        BlockState fire = BaseFireBlock.getState(level, surface);
        if (!level.setBlock(surface, fire, PLACE_FLAGS)) {
            return false;
        }
        remember(level, surface, previous, false);
        return false;
    }

    private static void remember(ServerLevel level, BlockPos pos, BlockState previous, boolean lava) {
        BlockPos[] watch = null;
        BlockState[] watched = null;
        if (lava) {
            watch = new BlockPos[26];
            watched = new BlockState[26];
            int n = 0;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos near = new BlockPos(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                        watch[n] = near;
                        watched[n] = level.getBlockState(near);
                        n++;
                    }
                }
            }
        }
        PENDING.add(new Pending(level, pos, previous, level.getGameTime() + LIFE_TICKS, lava, watch, watched));
        clearTicks(level, pos);
    }

    private static boolean nearWater(ServerLevel level, BlockPos pos) {
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            return true;
        }
        for (Direction direction : NEIGHBORS) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    private static boolean occupiedByPlayer(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(Player.class, new AABB(pos), Player::isAlive).isEmpty();
    }

    /** Cells this weapon is not allowed to destroy, even for a moment. */
    private static boolean protectedSolid(BlockState state) {
        return state.is(Blocks.BEDROCK)
            || state.is(Blocks.SPAWNER)
            || state.is(Blocks.TRIAL_SPAWNER)
            || state.is(Blocks.CHEST)
            || state.is(Blocks.TRAPPED_CHEST)
            || state.is(Blocks.ENDER_CHEST)
            || state.is(Blocks.BARREL);
    }

    private static boolean alreadyPending(ServerLevel level, BlockPos pos) {
        for (Pending pending : PENDING) {
            if (pending.level == level && pending.pos.equals(pos)) {
                return true;
            }
        }
        return false;
    }

    private static void ensureHooks() {
        if (HOOKED.compareAndSet(false, true)) {
            ServerTickEvents.END_LEVEL_TICK.register(LavaSwordGround::onLevelTick);
            ServerLifecycleEvents.SERVER_STOPPING.register(LavaSwordGround::onStopping);
        }
    }

    private static void onLevelTick(ServerLevel level) {
        if (PENDING.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending pending = it.next();
            if (pending.level != level) {
                continue;
            }
            if (now >= pending.until) {
                restore(pending);
                it.remove();
                continue;
            }
            if (!level.isLoaded(pending.pos)) {
                continue;
            }
            if (!stillOurs(level, pending)) {
                it.remove();
                continue;
            }
            hold(level, pending);
        }
    }

    private static void onStopping(MinecraftServer server) {
        List<Pending> copy = new ArrayList<>(PENDING);
        PENDING.clear();
        for (Pending pending : copy) {
            if (pending.level.getServer() == server) {
                restore(pending);
            } else {
                PENDING.add(pending);
            }
        }
    }

    private static boolean stillOurs(ServerLevel level, Pending pending) {
        BlockState current = level.getBlockState(pending.pos);
        if (pending.lava) {
            return current.is(Blocks.LAVA);
        }
        return current.getBlock() instanceof BaseFireBlock;
    }

    private static void hold(ServerLevel level, Pending pending) {
        clearTicks(level, pending.pos);
        if (!pending.lava || pending.watch == null) {
            return;
        }
        for (int i = 0; i < pending.watch.length; i++) {
            BlockPos near = pending.watch[i];
            BlockState was = pending.watched[i];
            if (near == null || was == null || !level.isLoaded(near)) {
                continue;
            }
            BlockState nowState = level.getBlockState(near);
            boolean flowed = nowState.is(Blocks.LAVA) && !was.is(Blocks.LAVA);
            boolean cooked = lavaConversion(was, nowState);
            boolean strayFire = nowState.getBlock() instanceof BaseFireBlock
                && !(was.getBlock() instanceof BaseFireBlock)
                && !alreadyPending(level, near);
            if (flowed || cooked || strayFire) {
                if (!protectedSolid(was)) {
                    level.setBlock(near, was, PLACE_FLAGS);
                    clearTicks(level, near);
                }
            }
        }
    }

    private static boolean lavaConversion(BlockState was, BlockState nowState) {
        if (was.is(Blocks.COBBLESTONE) || was.is(Blocks.STONE) || was.is(Blocks.OBSIDIAN) || was.is(Blocks.BASALT)) {
            return false;
        }
        return nowState.is(Blocks.COBBLESTONE) || nowState.is(Blocks.STONE)
            || nowState.is(Blocks.OBSIDIAN) || nowState.is(Blocks.BASALT);
    }

    private static void restore(Pending pending) {
        ServerLevel level = pending.level;
        if (!level.isInWorldBounds(pending.pos)) {
            return;
        }
        hold(level, pending);
        if (stillOurs(level, pending) && !protectedSolid(pending.restore)) {
            level.setBlock(pending.pos, pending.restore, PLACE_FLAGS);
        }
        clearTicks(level, pending.pos);
    }

    private static void clearTicks(ServerLevel level, BlockPos pos) {
        BoundingBox box = new BoundingBox(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
        level.getBlockTicks().clearArea(box);
        level.getFluidTicks().clearArea(box);
    }

    private static final class Pending {
        final ServerLevel level;
        final BlockPos pos;
        final BlockState restore;
        final long until;
        final boolean lava;
        final BlockPos[] watch;
        final BlockState[] watched;

        Pending(ServerLevel level, BlockPos pos, BlockState restore, long until, boolean lava,
                BlockPos[] watch, BlockState[] watched) {
            this.level = level;
            this.pos = pos;
            this.restore = restore;
            this.until = until;
            this.lava = lava;
            this.watch = watch;
            this.watched = watched;
        }
    }
}
