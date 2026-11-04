package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Surface burn left by the lava sword. Fire goes on air that already sits on a
 * solid face. Each swing also has a budget of real lava sources, and those are
 * placed first, on the first surface cells the swing reaches, so using the
 * sword always leaves some lava on the ground. The rest is fire. Both are
 * put back after a few seconds. The placement skips the fluid's on-place so the
 * source does not flow, and the server tick puts the old block back.
 *
 * Crash safety: the pending list below is only a working cache. Every pending
 * restore is also written into the level's SavedData the moment it is placed,
 * the same pattern GroundScar uses, so if the server dies mid burn the record
 * survives in the world file. The first level tick after a restart adopts the
 * saved entries back: overdue ones restore at once, live ones resume their
 * tick watch, and the 80 tick restore stays exact across the crash.
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
    /** Levels whose saved entries this server run has already adopted back. */
    private static final Set<ServerLevel> ADOPTED = new HashSet<>();

    /** One saved restore, in the level's SavedData. Watch cells are a lava cell's neighbor snapshot. */
    public record Entry(BlockPos pos, String original, long restoreAt, boolean lava,
                        List<BlockPos> watch, List<String> watched) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
            Codec.STRING.fieldOf("original").forGetter(Entry::original),
            Codec.LONG.fieldOf("restoreAt").forGetter(Entry::restoreAt),
            Codec.BOOL.fieldOf("lava").forGetter(Entry::lava),
            BlockPos.CODEC.listOf().fieldOf("watch").forGetter(Entry::watch),
            Codec.STRING.listOf().fieldOf("watched").forGetter(Entry::watched)
        ).apply(i, Entry::new));
    }

    public static final class Data extends SavedData {
        public final List<Entry> entries = new ArrayList<>();
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().fieldOf("entries").forGetter(d -> d.entries)
        ).apply(i, list -> {
            Data d = new Data();
            d.entries.addAll(list);
            return d;
        }));
    }

    private static final SavedDataType<Data> TYPE = new SavedDataType<>(
        DragonBlockArcaneDBA.id("lava_sword_ground"),
        Data::new,
        Data.CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

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
                                float inner, float outer, int lavaBudget, double minDot) {
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
                if (dot < minDot) {
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
            if (!ground.isFaceSturdy(level, groundPos, Direction.UP)) {
                continue;
            }
            BlockPos surface = new BlockPos(x, cursor.getY(), z);
            return igniteSurface(level, surface, ground, allowLava);
        }
        return false;
    }

    private static boolean igniteSurface(ServerLevel level, BlockPos surface, BlockState ground, boolean allowLava) {
        if (alreadyPending(level, surface)) {
            return false;
        }
        BlockState previous = level.getBlockState(surface);
        if (!previous.isAir()) {
            return false;
        }
        // The placed cell is always air, so the protection list can never
        // guard the cell itself (an earlier version consulted it there, where
        // it could never fire). Where it does guard something real is the
        // ground the burn sits on: never put fire or lava directly on top of
        // a chest, a spawner, a barrel, or a bedrock floor.
        if (protectedSolid(ground)) {
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
        Pending pending = new Pending(level, pos, previous, level.getGameTime() + LIFE_TICKS, lava, watch, watched);
        PENDING.add(pending);
        persistAdd(pending);
        clearTicks(level, pos);
    }

    /** Mirror a fresh pending restore into the level's SavedData, so a crash cannot orphan the placed block. */
    private static void persistAdd(Pending pending) {
        Data data = pending.level.getDataStorage().computeIfAbsent(TYPE);
        List<BlockPos> watch = new ArrayList<>();
        List<String> watched = new ArrayList<>();
        if (pending.watch != null && pending.watched != null) {
            for (int i = 0; i < pending.watch.length; i++) {
                if (pending.watch[i] == null || pending.watched[i] == null) {
                    continue;
                }
                watch.add(pending.watch[i]);
                watched.add(BlockStateParser.serialize(pending.watched[i]));
            }
        }
        data.entries.add(new Entry(pending.pos, BlockStateParser.serialize(pending.restore),
            pending.until, pending.lava, watch, watched));
        data.setDirty();
    }

    /** Drop a saved restore once the cell has been dealt with, so it is never restored twice. */
    private static void persistRemove(ServerLevel level, BlockPos pos) {
        Data data = level.getDataStorage().get(TYPE);
        if (data == null) {
            return;
        }
        if (data.entries.removeIf(e -> e.pos().equals(pos))) {
            data.setDirty();
        }
    }

    private static BlockState parseState(ServerLevel level, String text) {
        try {
            var lookup = level.holderLookup(Registries.BLOCK);
            BlockStateParser.BlockResult result = BlockStateParser.parseForBlock(lookup, text, false);
            return result.blockState();
        } catch (Exception e) {
            return Blocks.AIR.defaultBlockState();
        }
    }

    /**
     * First tick for a level in this server run: take back whatever the
     * SavedData kept through a crash or a hard kill. Overdue entries restore
     * immediately, live ones resume their tick watch with their neighbor
     * snapshots intact, and entries whose cell no longer holds our burn are
     * dropped, so a block a player placed while the server was down is never
     * stomped by a stale restore.
     */
    private static void adoptPersisted(ServerLevel level) {
        if (!ADOPTED.add(level)) {
            return;
        }
        Data data = level.getDataStorage().get(TYPE);
        if (data == null || data.entries.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        boolean changed = false;
        Iterator<Entry> it = data.entries.iterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            if (alreadyPending(level, entry.pos())) {
                continue;
            }
            Pending pending = rebuild(level, entry);
            if (now >= entry.restoreAt()) {
                restore(pending);
                it.remove();
                changed = true;
                continue;
            }
            if (level.isLoaded(entry.pos()) && !stillOurs(level, pending)) {
                it.remove();
                changed = true;
                continue;
            }
            PENDING.add(pending);
        }
        if (changed) {
            data.setDirty();
        }
    }

    private static Pending rebuild(ServerLevel level, Entry entry) {
        BlockPos[] watch = null;
        BlockState[] watched = null;
        if (entry.lava() && !entry.watch().isEmpty()) {
            int n = entry.watch().size();
            watch = new BlockPos[n];
            watched = new BlockState[n];
            for (int i = 0; i < n; i++) {
                watch[i] = entry.watch().get(i);
                String text = i < entry.watched().size() ? entry.watched().get(i) : null;
                watched[i] = text != null ? parseState(level, text) : level.getBlockState(watch[i]);
            }
        }
        return new Pending(level, entry.pos().immutable(), parseState(level, entry.original()),
            entry.restoreAt(), entry.lava(), watch, watched);
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

    /**
     * Blocks a burn is never placed on top of, and the neighbor states hold()
     * never reverts a cell away from. The placed cell itself is always air, so
     * this list guards the ground under the burn and the lava cells' neighbors.
     */
    /** The sword's own protected-ground list, exposed for LavaField's placement checks. */
    public static boolean isProtectedGround(BlockState state) {
        return protectedSolid(state);
    }

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

    /**
     * Startup registration, called from the lava sword item's class init so
     * the restore ticker and the crash adoption run from the first server
     * tick, not only after the first swing. Safe to call more than once.
     */
    public static void register() {
        ensureHooks();
    }

    private static void ensureHooks() {
        if (HOOKED.compareAndSet(false, true)) {
            ServerTickEvents.END_LEVEL_TICK.register(LavaSwordGround::onLevelTick);
            ServerLifecycleEvents.SERVER_STOPPING.register(LavaSwordGround::onStopping);
            ServerLifecycleEvents.SERVER_STOPPED.register(server -> ADOPTED.clear());
        }
    }

    private static void onLevelTick(ServerLevel level) {
        adoptPersisted(level);
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
                persistRemove(level, pending.pos);
                it.remove();
                continue;
            }
            if (!level.isLoaded(pending.pos)) {
                continue;
            }
            if (!stillOurs(level, pending)) {
                persistRemove(level, pending.pos);
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
                persistRemove(pending.level, pending.pos);
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
        // The restore state is the air the burn was placed into, so there is
        // nothing for the protection list to guard here; the only question
        // that matters is whether the cell still holds our burn.
        if (stillOurs(level, pending)) {
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
