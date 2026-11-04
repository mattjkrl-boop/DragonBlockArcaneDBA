package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.block.DbaBlocks;
import com.dragonblockarcanedba.block.MagmaCrustBlock;
import com.dragonblockarcanedba.block.entity.VolcanoBlockEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.dragonblockarcanedba.util.DbaDamage;
import com.dragonblockarcanedba.util.PermanentDestruction;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Lava Sword redesign's ground engine. Where LavaSwordGround leaves
 * short-lived fire and no-flow lava sources, LavaField leaves the sword's
 * own material: magma crust blocks that cool through stages, channels
 * that flow downhill cell by cell, and planted mini volcanoes.
 *
 * The safety contract under the destruction expansion: placement only
 * ever replaces air above sturdy, unprotected ground (the shared
 * protection list plus the sword's own list); every pending change is
 * mirrored into the level's SavedData the moment it is made, so a crash
 * or a hard kill cannot lose a live schedule; and hard caps bound how
 * much field one level and one player can hold at once. What changed is
 * the ending: crust no longer restores. At the end of its schedule a POOL
 * cell settles into permanent basalt, a CHANNEL cell into basalt with a
 * quarter of the flown centerline settling as magma block instead, a
 * WAVE cell still vanishes (a permanent stripe under every cleave path
 * would paint the world), and a volcano site ends as a permanent dormant
 * volcano block. The serialized field is still named restoreAt, kept for
 * save compatibility; it now means the settle tick.
 *
 * Entry kinds: CRUST (a placed magma crust cell with a stage schedule),
 * CHANNEL (a cell scheduled to be placed as a channel's flow front
 * reaches it; its stage1At field is the placement tick), VOLCANO (the
 * site mirror for a planted mini volcano; the block entity drives the
 * eruptions, and the entry's end now just closes the record, leaving the
 * dormant block standing).
 */
public final class LavaField {
    public static final int PROFILE_POOL = 0;
    public static final int PROFILE_CHANNEL = 1;
    public static final int PROFILE_WAVE = 2;

    private static final int KIND_CRUST = 0;
    private static final int KIND_CHANNEL = 1;
    private static final int KIND_VOLCANO = 2;

    /** Stage boundaries per profile: ticks to cooling, ticks to dark, total life. */
    private static final int[][] STAGE_TICKS = {
        {60, 120, 200},   // POOL: pools and splats, the battlefield stays marked
        {40, 80, 140},    // CHANNEL: carved paths cool a little faster
        {14, 30, 54},     // WAVE: the cleave's rolling crest sinks quickly behind it
    };

    /** Mini volcano life, and the field entry's grace past it. */
    public static final int VOLCANO_LIFE_TICKS = 600;
    private static final int VOLCANO_ENTRY_GRACE = 25;

    /** Hard caps so fields cannot run away, however the sword is spammed. */
    private static final int MAX_CELLS_PER_LEVEL = 800;
    private static final int MAX_CELLS_PER_OWNER = 240;

    private static final int PLACE_FLAGS =
        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SKIP_ON_PLACE;
    private static final int STAGE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static final List<Pending> PENDING = new ArrayList<>();
    private static final AtomicBoolean HOOKED = new AtomicBoolean();
    private static final Set<ServerLevel> ADOPTED = new HashSet<>();

    private LavaField() {}

    public record Entry(BlockPos pos, String original, long stage1At, long stage2At, long restoreAt,
                        int kind, int profile, String owner, float damage, boolean centerline) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
            Codec.STRING.fieldOf("original").forGetter(Entry::original),
            Codec.LONG.fieldOf("stage1At").forGetter(Entry::stage1At),
            Codec.LONG.fieldOf("stage2At").forGetter(Entry::stage2At),
            Codec.LONG.fieldOf("restoreAt").forGetter(Entry::restoreAt),
            Codec.INT.fieldOf("kind").forGetter(Entry::kind),
            Codec.INT.fieldOf("profile").forGetter(Entry::profile),
            Codec.STRING.fieldOf("owner").forGetter(Entry::owner),
            Codec.FLOAT.fieldOf("damage").forGetter(Entry::damage),
            // Optional so entries written before the destruction expansion
            // still decode; they settle as non-centerline cells.
            Codec.BOOL.optionalFieldOf("centerline", false).forGetter(Entry::centerline)
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
        DragonBlockArcaneDBA.id("lava_field"),
        Data::new,
        Data.CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    // ------------------------------------------------------------------
    // Surface finding and placement validation
    // ------------------------------------------------------------------

    /**
     * The air cell a ground effect lands in for one column, found the way
     * LavaSwordGround.scorchColumn finds it: climb out of a solid hint cell
     * (up to 4), then scan down (up to 6) for the first sturdy ground. The
     * floor test is isFaceSturdy only: blocksMotion() was removed in 26.3
     * (the SpikeFlailSpinEntity precedent). Returns null when the column
     * has no usable surface.
     */
    public static BlockPos findSurface(ServerLevel level, int x, int z, int yHint) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        cursor.set(x, yHint, z);
        if (!level.isLoaded(cursor) || !level.isInWorldBounds(cursor)) {
            return null;
        }
        if (!level.getBlockState(cursor).isAir()) {
            boolean foundAir = false;
            for (int up = 1; up <= 4; up++) {
                cursor.set(x, yHint + up, z);
                if (!level.isLoaded(cursor) || !level.isInWorldBounds(cursor)) {
                    return null;
                }
                if (level.getBlockState(cursor).isAir()) {
                    yHint += up;
                    foundAir = true;
                    break;
                }
            }
            if (!foundAir) {
                return null;
            }
        }
        for (int dy = 0; dy >= -6; dy--) {
            cursor.set(x, yHint + dy, z);
            if (!level.isLoaded(cursor) || !level.isInWorldBounds(cursor)) {
                return null;
            }
            if (!level.getBlockState(cursor).isAir()) {
                return null;
            }
            BlockPos groundPos = new BlockPos(x, cursor.getY() - 1, z);
            BlockState ground = level.getBlockState(groundPos);
            if (!ground.isFaceSturdy(level, groundPos, Direction.UP)) {
                continue;
            }
            return new BlockPos(x, cursor.getY(), z);
        }
        return null;
    }

    /** Full refusal test for putting field material in one surface cell. */
    private static boolean canOccupy(ServerLevel level, BlockPos surface) {
        if (!level.isLoaded(surface) || !level.isInWorldBounds(surface)) {
            return false;
        }
        if (!level.getBlockState(surface).isAir()) {
            return false;
        }
        if (pendingAt(level, surface) != null) {
            return false;
        }
        BlockPos groundPos = surface.below();
        BlockState ground = level.getBlockState(groundPos);
        if (!ground.isFaceSturdy(level, groundPos, Direction.UP)) {
            return false;
        }
        // Protected ground, both lists: the shared destruction list
        // (block entities, bedrock, portals, command and structure
        // blocks) and the sword's own (chests, barrels, spawners under
        // the burn).
        if (PermanentDestruction.isProtected(level, groundPos, ground)) {
            return false;
        }
        return !LavaSwordGround.isProtectedGround(ground);
    }

    private static boolean capsAllow(ServerLevel level, UUID ownerId) {
        int levelCount = 0;
        int ownerCount = 0;
        String ownerText = ownerId != null ? ownerId.toString() : "";
        for (Pending p : PENDING) {
            if (p.level != level) {
                continue;
            }
            levelCount++;
            if (!ownerText.isEmpty() && ownerText.equals(p.owner)) {
                ownerCount++;
            }
        }
        return levelCount < MAX_CELLS_PER_LEVEL && ownerCount < MAX_CELLS_PER_OWNER;
    }

    // ------------------------------------------------------------------
    // Crust placement
    // ------------------------------------------------------------------

    /** Place one crust cell at a known surface position. */
    public static boolean placeCrust(ServerLevel level, BlockPos surface, int profile,
                                     UUID ownerId, float touchDamage) {
        return placeCrust(level, surface, profile, ownerId, touchDamage, false);
    }

    /**
     * Place one crust cell, optionally flagged as a channel centerline
     * cell (the flag only matters at settle time, where a quarter of
     * centerline channel cells end as magma block instead of basalt).
     */
    public static boolean placeCrust(ServerLevel level, BlockPos surface, int profile,
                                     UUID ownerId, float touchDamage, boolean centerline) {
        ensureHooks();
        if (!canOccupy(level, surface) || !capsAllow(level, ownerId)) {
            return false;
        }
        BlockState previous = level.getBlockState(surface);
        BlockState crust = DbaBlocks.MAGMA_CRUST.defaultBlockState().setValue(MagmaCrustBlock.STAGE, 0);
        if (!level.setBlock(surface, crust, PLACE_FLAGS)) {
            return false;
        }
        long now = level.getGameTime();
        int[] ticks = STAGE_TICKS[profile];
        Pending pending = new Pending(level, surface.immutable(), previous,
            now + ticks[0], now + ticks[1], now + ticks[2],
            KIND_CRUST, profile, ownerId != null ? ownerId.toString() : "", touchDamage, centerline);
        PENDING.add(pending);
        persistAdd(pending);
        return true;
    }

    /** Find the surface in one column and crust it. */
    public static boolean crustColumn(ServerLevel level, int x, int z, int yHint, int profile,
                                      Entity owner, float touchDamage) {
        BlockPos surface = findSurface(level, x, z, yHint);
        if (surface == null) {
            return false;
        }
        return placeCrust(level, surface, profile, owner != null ? owner.getUUID() : null, touchDamage);
    }

    /**
     * Crust pools along the eruption's moving lip, in the same forward
     * wedge and the same deterministic traversal order LavaSwordGround
     * uses for its lava budget: the first eligible cells in scan order
     * become pools until the budget is spent.
     */
    public static int crustArc(ServerLevel level, double x, double y, double z, float yRot,
                               float inner, float outer, int budget, double minDot,
                               Entity owner, float touchDamage) {
        ensureHooks();
        double yaw = Math.toRadians(yRot);
        double lookX = -Math.sin(yaw);
        double lookZ = Math.cos(yaw);
        int placed = 0;
        int max = Mth.ceil(outer + 0.4);
        int cx = Mth.floor(x);
        int cz = Mth.floor(z);
        int yHint = Mth.floor(y);
        UUID ownerId = owner != null ? owner.getUUID() : null;
        for (int dx = -max; dx <= max && placed < budget; dx++) {
            for (int dz = -max; dz <= max && placed < budget; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > outer + 0.4 || dist < inner - 0.7) {
                    continue;
                }
                double dot = dist < 0.05 ? 1.0 : (dx * lookX + dz * lookZ) / dist;
                if (dot < minDot) {
                    continue;
                }
                if (crustColumn(level, cx + dx, cz + dz, yHint, PROFILE_POOL, owner, touchDamage)) {
                    placed++;
                }
            }
        }
        return placed;
    }

    /**
     * One tick of the cleave's magma wave: crust the ground under the
     * flight segment, three cells wide across the direction of travel.
     * Cells are WAVE profile (they sink and cool fast behind the crest).
     * {@code placed} dedupes cells across the cleave's ticks; returns how
     * many cells this step added, capped by {@code budget}.
     */
    public static int waveStep(ServerLevel level, Vec3 from, Vec3 to, Entity owner,
                               float touchDamage, Set<Long> placed, int budget) {
        ensureHooks();
        if (budget <= 0) {
            return 0;
        }
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        double perpX = len > 0.01 ? dz / len : 1.0;
        double perpZ = len > 0.01 ? -dx / len : 0.0;
        int steps = Math.max(1, (int) Math.round(len / 0.7));
        int added = 0;
        for (int i = 0; i <= steps && added < budget; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            double px = from.x + dx * t;
            double pz = from.z + dz * t;
            int yHint = Mth.floor(from.y + (to.y - from.y) * t);
            for (int off = -1; off <= 1 && added < budget; off++) {
                int cx = Mth.floor(px + perpX * off);
                int cz = Mth.floor(pz + perpZ * off);
                BlockPos surface = findSurface(level, cx, cz, yHint);
                if (surface == null || !placed.add(surface.asLong())) {
                    continue;
                }
                if (placeCrust(level, surface, PROFILE_WAVE, owner != null ? owner.getUUID() : null, touchDamage)) {
                    added++;
                }
            }
        }
        return added;
    }

    /**
     * Carve the cleave's channel (idea 1). The flown centerline's wave
     * cells are upgraded in place to the longer CHANNEL profile, cells the
     * wave budget missed are scheduled onto the line, and from the path's
     * end the channel flows downhill: a greedy descent of up to 6 cells,
     * each scheduled 2 ticks after the previous, so the lava visibly runs
     * down the slope before the whole line crusts over.
     */
    public static void carveChannel(ServerLevel level, List<BlockPos> centerline, Entity owner, float touchDamage) {
        ensureHooks();
        if (centerline.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        UUID ownerId = owner != null ? owner.getUUID() : null;
        String ownerText = ownerId != null ? ownerId.toString() : "";
        int[] ticks = STAGE_TICKS[PROFILE_CHANNEL];
        for (int i = 0; i < centerline.size(); i++) {
            BlockPos cell = centerline.get(i);
            Pending existing = pendingAt(level, cell);
            if (existing != null && existing.kind == KIND_CRUST) {
                // Upgrade in place: same cell, longer channel schedule, and
                // the centerline flag so it can settle as magma block.
                existing.profile = PROFILE_CHANNEL;
                existing.centerline = true;
                existing.stage1At = now + ticks[0];
                existing.stage2At = now + ticks[1];
                existing.restoreAt = now + ticks[2];
                existing.damage = touchDamage;
                syncEntry(existing);
            } else if (existing == null) {
                scheduleChannelCell(level, cell, now + i, ownerText, touchDamage, true);
            }
        }
        // Downhill flow from the end of the path.
        BlockPos current = centerline.get(centerline.size() - 1);
        for (int step = 0; step < 6; step++) {
            BlockPos best = null;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos candidate = findSurface(level, current.getX() + dir.getStepX(),
                    current.getZ() + dir.getStepZ(), current.getY() + 1);
                if (candidate == null || candidate.getY() >= current.getY()) {
                    continue;
                }
                if (pendingAt(level, candidate) != null) {
                    continue;
                }
                if (best == null || candidate.getY() < best.getY()) {
                    best = candidate;
                }
            }
            if (best == null) {
                break;
            }
            scheduleChannelCell(level, best, now + (step + 1) * 2L, ownerText, touchDamage, false);
            current = best;
        }
    }

    private static void scheduleChannelCell(ServerLevel level, BlockPos cell, long placeAt,
                                            String ownerText, float touchDamage, boolean centerline) {
        if (!capsAllow(level, ownerText.isEmpty() ? null : UUID.fromString(ownerText))) {
            return;
        }
        Pending pending = new Pending(level, cell.immutable(), Blocks.AIR.defaultBlockState(),
            placeAt, 0, 0, KIND_CHANNEL, PROFILE_CHANNEL, ownerText, touchDamage, centerline);
        PENDING.add(pending);
        persistAdd(pending);
    }

    // ------------------------------------------------------------------
    // Volcano sites
    // ------------------------------------------------------------------

    /** Whether a volcano could be planted on this ground block right now. */
    public static boolean canPlantVolcano(ServerLevel level, BlockPos groundPos, UUID ownerId) {
        ensureHooks();
        if (activeVolcanoes(level, ownerId) >= 1) {
            return false;
        }
        BlockPos surface = groundPos.above();
        return canOccupy(level, surface) && capsAllow(level, ownerId);
    }

    /**
     * Plant a mini volcano with its base in the air cell above groundPos.
     * The block entity drives the eruptions; the field entry mirrors the
     * site so the volcano's record closes on schedule, leaving the
     * dormant block standing even if the entity is lost.
     */
    public static boolean plantVolcano(ServerLevel level, BlockPos groundPos, UUID ownerId, float blobDamage) {
        if (!canPlantVolcano(level, groundPos, ownerId)) {
            return false;
        }
        BlockPos surface = groundPos.above().immutable();
        BlockState previous = level.getBlockState(surface);
        if (!level.setBlock(surface, DbaBlocks.MINI_VOLCANO.defaultBlockState(), Block.UPDATE_ALL)) {
            return false;
        }
        if (!(level.getBlockEntity(surface) instanceof VolcanoBlockEntity volcano)) {
            level.setBlock(surface, previous, PLACE_FLAGS);
            return false;
        }
        long now = level.getGameTime();
        volcano.initialize(ownerId, now + VOLCANO_LIFE_TICKS, now + 30, blobDamage);
        Pending pending = new Pending(level, surface, previous, 0, 0,
            now + VOLCANO_LIFE_TICKS + VOLCANO_ENTRY_GRACE,
            KIND_VOLCANO, PROFILE_POOL, ownerId.toString(), blobDamage, false);
        PENDING.add(pending);
        persistAdd(pending);
        return true;
    }

    public static int activeVolcanoes(ServerLevel level, UUID ownerId) {
        String text = ownerId.toString();
        int count = 0;
        for (Pending p : PENDING) {
            if (p.level == level && p.kind == KIND_VOLCANO && text.equals(p.owner)) {
                count++;
            }
        }
        return count;
    }

    /** End a volcano site now: remove the block and its field entry, with feedback. */
    public static void endVolcano(ServerLevel level, BlockPos pos) {
        Pending pending = pendingAt(level, pos);
        if (pending != null && pending.kind == KIND_VOLCANO) {
            PENDING.remove(pending);
            persistRemove(level, pending.pos, pending.kind);
        }
        BlockState current = level.getBlockState(pos);
        if (current.is(DbaBlocks.MINI_VOLCANO)) {
            BlockState restoreTo = pending != null ? pending.restore : Blocks.AIR.defaultBlockState();
            level.setBlock(pos, restoreTo, PLACE_FLAGS);
            // The construct crumbles: rock chunks and a hiss of steam.
            level.sendParticles(new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, current),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 14, 0.4, 0.4, 0.4, 0.08);
            level.sendParticles(DbaParticles.ASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                8, 0.4, 0.3, 0.4, 0.03);
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                DbaSounds.GROUND_CRACK, SoundSource.BLOCKS, 1.0f, 0.8f);
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                DbaSounds.EXTINGUISH, SoundSource.BLOCKS, 0.8f, 0.9f);
        }
    }

    // ------------------------------------------------------------------
    // Crust contact (called by MagmaCrustBlock)
    // ------------------------------------------------------------------

    /**
     * A living entity is in or on a crust cell. Stage 0 burns for the
     * entry's full touch damage, stage 1 for 40 percent, stage 2 not at
     * all. The pools are the weapon's zoning: they burn everyone in them,
     * the planter included, exactly as the real lava sources they replace
     * did. Damage goes through the lava family (DbaDamage.LAVA): credited
     * to the entry's owner when they are in the level, otherwise built on
     * the same holder from the level's registry access.
     */
    public static void onCrustTouch(ServerLevel level, BlockPos pos, LivingEntity entity, int stage) {
        if (stage >= 2 || entity.isSpectator()) {
            return;
        }
        Pending pending = pendingAt(level, pos);
        float base = pending != null ? pending.damage : 40.0f;
        float amount = stage == 0 ? base : base * 0.4f;
        entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), stage == 0 ? 50 : 20));
        Player owner = null;
        if (pending != null && !pending.owner.isEmpty()) {
            owner = level.getPlayerByUUID(UUID.fromString(pending.owner));
        }
        DamageSource source;
        if (owner != null) {
            source = DbaDamage.lava(null, owner);
        } else {
            Holder<DamageType> holder = level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DbaDamage.LAVA);
            source = new DamageSource(holder);
        }
        entity.hurtServer(level, source,
            com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(amount));
    }

    // ------------------------------------------------------------------
    // Persistence, ticker, adoption
    // ------------------------------------------------------------------

    private static Pending pendingAt(ServerLevel level, BlockPos pos) {
        for (Pending p : PENDING) {
            if (p.level == level && p.pos.equals(pos)) {
                return p;
            }
        }
        return null;
    }

    private static void persistAdd(Pending pending) {
        Data data = pending.level.getDataStorage().computeIfAbsent(TYPE);
        data.entries.add(new Entry(pending.pos, BlockStateParser.serialize(pending.restore),
            pending.stage1At, pending.stage2At, pending.restoreAt,
            pending.kind, pending.profile, pending.owner, pending.damage, pending.centerline));
        data.setDirty();
    }

    private static void persistRemove(ServerLevel level, BlockPos pos, int kind) {
        Data data = level.getDataStorage().get(TYPE);
        if (data == null) {
            return;
        }
        if (data.entries.removeIf(e -> e.pos().equals(pos) && e.kind() == kind)) {
            data.setDirty();
        }
    }

    /** Rewrite a pending entry's saved copy after an in-place schedule change (channel upgrade). */
    private static void syncEntry(Pending pending) {
        Data data = pending.level.getDataStorage().get(TYPE);
        if (data == null) {
            return;
        }
        for (int i = 0; i < data.entries.size(); i++) {
            Entry e = data.entries.get(i);
            if (e.pos().equals(pending.pos) && e.kind() == pending.kind) {
                data.entries.set(i, new Entry(pending.pos, e.original(),
                    pending.stage1At, pending.stage2At, pending.restoreAt,
                    pending.kind, pending.profile, pending.owner, pending.damage, pending.centerline));
                data.setDirty();
                return;
            }
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

    public static void register() {
        ensureHooks();
    }

    private static void ensureHooks() {
        if (HOOKED.compareAndSet(false, true)) {
            ServerTickEvents.END_LEVEL_TICK.register(LavaField::onLevelTick);
            ServerLifecycleEvents.SERVER_STOPPING.register(LavaField::onStopping);
            ServerLifecycleEvents.SERVER_STOPPED.register(server -> ADOPTED.clear());
        }
    }

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
            if (pendingAt(level, entry.pos()) != null) {
                continue;
            }
            Pending pending = new Pending(level, entry.pos().immutable(), parseState(level, entry.original()),
                entry.stage1At(), entry.stage2At(), entry.restoreAt(),
                entry.kind(), entry.profile(), entry.owner(), entry.damage(), entry.centerline());
            if (entry.kind() == KIND_CHANNEL) {
                // A flow front that had not arrived when the server died
                // still arrives; one long overdue is placed at once by
                // the normal tick path below.
                PENDING.add(pending);
                continue;
            }
            if (now >= entry.restoreAt()) {
                // Overdue at adoption: a crust cell settles at once
                // (basalt, magma block, or vanish per profile); a volcano
                // entry just closes, the dormant block stays.
                if (entry.kind() == KIND_CRUST) {
                    settleCell(pending);
                }
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

    private static boolean stillOurs(ServerLevel level, Pending pending) {
        BlockState current = level.getBlockState(pending.pos);
        return switch (pending.kind) {
            case KIND_CRUST -> current.is(DbaBlocks.MAGMA_CRUST);
            case KIND_VOLCANO -> current.is(DbaBlocks.MINI_VOLCANO);
            default -> true;
        };
    }

    /**
     * The end of a crust cell's schedule, under the destruction expansion.
     * POOL cells settle into permanent basalt; CHANNEL cells settle into
     * basalt, with a quarter of the flown centerline settling as magma
     * block instead; WAVE cells vanish back to the air they replaced, the
     * one deliberate exception, because a permanent stripe under every
     * cleave path would paint the world. The settle goes through the
     * shared refusal rules; the cell provably still holds our own crust
     * (stillOurs above), so if the shared convert ever refuses it, writing
     * our own cell directly is safe.
     */
    private static void settleCell(Pending pending) {
        ServerLevel level = pending.level;
        if (!level.isInWorldBounds(pending.pos)) {
            return;
        }
        if (!stillOurs(level, pending)) {
            return;
        }
        if (pending.profile == PROFILE_WAVE) {
            level.setBlock(pending.pos, pending.restore, PLACE_FLAGS);
            return;
        }
        BlockState end = Blocks.BASALT.defaultBlockState();
        if (pending.profile == PROFILE_CHANNEL && pending.centerline
            && level.getRandom().nextDouble() < 0.25) {
            end = Blocks.MAGMA_BLOCK.defaultBlockState();
        }
        if (!PermanentDestruction.convert(level, pending.pos, end)) {
            level.setBlock(pending.pos, end, PLACE_FLAGS);
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
            switch (pending.kind) {
                case KIND_CHANNEL -> {
                    if (now >= pending.stage1At) {
                        it.remove();
                        persistRemove(level, pending.pos, KIND_CHANNEL);
                        // The restore state recorded for a channel front is
                        // a placeholder; placement re-reads the live cell.
                        // The centerline flag rides along into the crust.
                        placeCrust(level, pending.pos, PROFILE_CHANNEL,
                            pending.owner.isEmpty() ? null : UUID.fromString(pending.owner),
                            pending.damage, pending.centerline);
                    }
                }
                case KIND_CRUST -> {
                    if (!level.isLoaded(pending.pos)) {
                        continue;
                    }
                    if (!stillOurs(level, pending)) {
                        persistRemove(level, pending.pos, KIND_CRUST);
                        it.remove();
                        continue;
                    }
                    if (now >= pending.restoreAt) {
                        settleCell(pending);
                        persistRemove(level, pending.pos, KIND_CRUST);
                        it.remove();
                        continue;
                    }
                    advanceStage(level, pending, now);
                }
                case KIND_VOLCANO -> {
                    if (level.isLoaded(pending.pos) && !stillOurs(level, pending)) {
                        persistRemove(level, pending.pos, KIND_VOLCANO);
                        it.remove();
                        continue;
                    }
                    if (now >= pending.restoreAt) {
                        // The site's record ends here. The block entity
                        // went dormant at its own expiry a little earlier;
                        // the dormant volcano block is permanent now and
                        // stays standing.
                        persistRemove(level, pending.pos, KIND_VOLCANO);
                        it.remove();
                    }
                }
                default -> {
                    persistRemove(level, pending.pos, pending.kind);
                    it.remove();
                }
            }
        }
    }

    private static void advanceStage(ServerLevel level, Pending pending, long now) {
        BlockState current = level.getBlockState(pending.pos);
        int stage = current.getValue(MagmaCrustBlock.STAGE);
        if (now >= pending.stage2At && stage < 2) {
            level.setBlock(pending.pos, current.setValue(MagmaCrustBlock.STAGE, 2), STAGE_FLAGS);
            // The crust cracks over as it goes dark.
            level.sendParticles(new BlockParticleOption(DbaParticles.BLOCK_CHUNKS,
                    current.setValue(MagmaCrustBlock.STAGE, 2)),
                pending.pos.getX() + 0.5, pending.pos.getY() + 0.45, pending.pos.getZ() + 0.5,
                3, 0.3, 0.1, 0.3, 0.04);
        } else if (now >= pending.stage1At && stage < 1) {
            level.setBlock(pending.pos, current.setValue(MagmaCrustBlock.STAGE, 1), STAGE_FLAGS);
            level.sendParticles(DbaParticles.ASH,
                pending.pos.getX() + 0.5, pending.pos.getY() + 0.5, pending.pos.getZ() + 0.5,
                3, 0.3, 0.15, 0.3, 0.02);
        }
    }

    private static void onStopping(MinecraftServer server) {
        List<Pending> copy = new ArrayList<>(PENDING);
        PENDING.clear();
        for (Pending pending : copy) {
            if (pending.level.getServer() != server) {
                PENDING.add(pending);
                continue;
            }
            // Shutdown settles only what is already overdue and otherwise
            // leaves the SavedData records in place, so live schedules are
            // adopted and finish on time after the restart instead of
            // being torn out early.
            long now = pending.level.getGameTime();
            switch (pending.kind) {
                case KIND_CRUST -> {
                    if (now >= pending.restoreAt) {
                        settleCell(pending);
                        persistRemove(pending.level, pending.pos, pending.kind);
                    }
                }
                case KIND_VOLCANO -> {
                    if (now >= pending.restoreAt) {
                        persistRemove(pending.level, pending.pos, pending.kind);
                    }
                }
                default -> {
                    // Channel fronts and unknown kinds keep their records.
                }
            }
        }
    }

    private static final class Pending {
        final ServerLevel level;
        final BlockPos pos;
        final BlockState restore;
        long stage1At;
        long stage2At;
        long restoreAt;
        final int kind;
        int profile;
        final String owner;
        float damage;
        boolean centerline;

        Pending(ServerLevel level, BlockPos pos, BlockState restore, long stage1At, long stage2At,
                long restoreAt, int kind, int profile, String owner, float damage, boolean centerline) {
            this.level = level;
            this.pos = pos;
            this.restore = restore;
            this.stage1At = stage1At;
            this.stage2At = stage2At;
            this.restoreAt = restoreAt;
            this.kind = kind;
            this.profile = profile;
            this.owner = owner;
            this.damage = damage;
            this.centerline = centerline;
        }
    }
}
