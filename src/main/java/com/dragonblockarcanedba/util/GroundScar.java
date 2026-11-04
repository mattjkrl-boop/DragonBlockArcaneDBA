package com.dragonblockarcanedba.util;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Temporary ground scarring (SPECTACLE-STANDARD point 3): abilities change the
 * ground for a while, then the world gets its original blocks back.
 *
 * Crash safe by construction: pending restores live in the level's SavedData,
 * not in memory. If the server dies mid-scar, the entries survive in the world
 * file and the first server tick after restart restores everything overdue.
 * (This replaces the failure mode where an in-memory restore list loses real
 * placed blocks into the saved world permanently.)
 *
 * Protected blocks are never scarred: anything with a block entity, bedrock,
 * barriers, command and structure blocks, end portal frames, spawners.
 */
public final class GroundScar {
    private GroundScar() {}

    public record Entry(BlockPos pos, String original, String scarred, long restoreAt) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
            Codec.STRING.fieldOf("original").forGetter(Entry::original),
            Codec.STRING.fieldOf("scarred").forGetter(Entry::scarred),
            Codec.LONG.fieldOf("restoreAt").forGetter(Entry::restoreAt)
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

    public static final SavedDataType<Data> TYPE = new SavedDataType<>(
        DragonBlockArcaneDBA.id("ground_scar"),
        Data::new,
        Data.CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static boolean isProtected(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) != null) return true;
        Block b = state.getBlock();
        return b == Blocks.BEDROCK || b == Blocks.BARRIER || b == Blocks.SPAWNER
            || b == Blocks.END_PORTAL_FRAME || b == Blocks.END_PORTAL || b == Blocks.NETHER_PORTAL
            || b == Blocks.COMMAND_BLOCK || b == Blocks.CHAIN_COMMAND_BLOCK || b == Blocks.REPEATING_COMMAND_BLOCK
            || b == Blocks.STRUCTURE_BLOCK || b == Blocks.JIGSAW;
    }

    /**
     * Change one block for restoreTicks game ticks. Returns true when placed.
     * Re-scarring an already scarred cell keeps the true original and extends
     * the restore time, so stacked scars still restore to the real ground.
     */
    public static boolean scar(ServerLevel level, BlockPos pos, BlockState newState, int restoreTicks) {
        BlockState current = level.getBlockState(pos);
        if (current == newState) return false;
        if (isProtected(level, pos, current)) return false;
        Data data = data(level);
        long restoreAt = level.getGameTime() + Math.max(1, restoreTicks);
        for (int i = 0; i < data.entries.size(); i++) {
            Entry e = data.entries.get(i);
            if (e.pos().equals(pos)) {
                data.entries.set(i, new Entry(pos.immutable(), e.original(), BlockStateParser.serialize(newState), Math.max(e.restoreAt(), restoreAt)));
                level.setBlock(pos, newState, Block.UPDATE_ALL);
                data.setDirty();
                return true;
            }
        }
        data.entries.add(new Entry(pos.immutable(), BlockStateParser.serialize(current), BlockStateParser.serialize(newState), restoreAt));
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        data.setDirty();
        return true;
    }

    /** Scar a filled disk of surface cells. y is the ground level to write at. */
    public static int scarDisk(ServerLevel level, BlockPos center, int radius, BlockState state,
                               int restoreTicks, float chance, net.minecraft.util.RandomSource rand) {
        int placed = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                if (rand.nextFloat() > chance) continue;
                m.set(center.getX() + dx, center.getY(), center.getZ() + dz);
                if (scar(level, m, state, restoreTicks)) placed++;
            }
        }
        return placed;
    }

    private static BlockState parseOriginal(ServerLevel level, String text) {
        try {
            var lookup = level.holderLookup(Registries.BLOCK);
            BlockStateParser.BlockResult result = BlockStateParser.parseForBlock(lookup, text, false);
            return result.blockState();
        } catch (Exception e) {
            return Blocks.AIR.defaultBlockState();
        }
    }

    /** Register the restore ticker. Called once from the mod initializer. */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GroundScar::tickServer);
    }

    private static void tickServer(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            Data data = level.getDataStorage().get(TYPE);
            if (data == null || data.entries.isEmpty()) continue;
            long now = level.getGameTime();
            boolean changed = false;
            Iterator<Entry> it = data.entries.iterator();
            while (it.hasNext()) {
                Entry e = it.next();
                if (e.restoreAt() <= now) {
                    // Restore only when the cell still holds the scar itself; if a
                    // player built over it meanwhile, their block wins.
                    BlockState current = level.getBlockState(e.pos());
                    if (!isProtected(level, e.pos(), current)
                        && BlockStateParser.serialize(current).equals(e.scarred())) {
                        level.setBlock(e.pos(), parseOriginal(level, e.original()), Block.UPDATE_ALL);
                    }
                    it.remove();
                    changed = true;
                }
            }
            if (changed) data.setDirty();
        }
    }
}
