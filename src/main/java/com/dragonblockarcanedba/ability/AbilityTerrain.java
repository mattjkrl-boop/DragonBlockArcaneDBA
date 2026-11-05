package com.dragonblockarcanedba.ability;

import com.dragonblockarcanedba.util.PermanentDestruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 6 terrain module: the bridge between ability data (the
 * terrain block) and util/PermanentDestruction. All edits go through
 * PermanentDestruction, so its protection rules and caps always
 * apply. Block ids in data are strings; resolution here is null-safe
 * and an unresolved id is a silent no-op (the loader already logged
 * it loudly at load time).
 */
public final class AbilityTerrain {
    private AbilityTerrain() {}

    private static BlockState stateOf(String blockId) {
        if (blockId == null) {
            return null;
        }
        Identifier id = Identifier.tryParse(blockId);
        if (id == null) {
            return null;
        }
        var block = BuiltInRegistries.BLOCK.getValue(id);
        return block == null ? null : block.defaultBlockState();
    }

    /** Crater event: a PermanentDestruction crater sized by the spec, tier by radius. */
    public static void craterAt(ServerLevel level, BlockPos pos, AbilityDef.TerrainSpec spec) {
        if (spec == null || spec.craterDepth() <= 0) {
            return;
        }
        float tier = spec.radius() <= 5 ? PermanentDestruction.TIER_STANDARD
                : PermanentDestruction.TIER_HEAVY;
        PermanentDestruction.crater(level, pos, spec.radius(), spec.craterDepth(),
                false, tier, spec.cap(), level.getRandom());
    }

    /** Place event: a filled disk of the place block at the given ground position. */
    public static void placeDiskAt(ServerLevel level, BlockPos pos, AbilityDef.TerrainSpec spec) {
        if (spec == null) {
            return;
        }
        BlockState state = stateOf(spec.placeId());
        if (state == null) {
            return;
        }
        PermanentDestruction.convertDisk(level, pos, Math.max(1, (int) Math.round(spec.radius())),
                state, 1.0f, level.getRandom());
    }

    /** Both halves of a spawn, finale, or impact event at one position. */
    public static void eventAt(ServerLevel level, BlockPos pos, AbilityDef.TerrainSpec spec) {
        craterAt(level, pos, spec);
        placeDiskAt(level, pos, spec);
    }

    /**
     * Trail event, one tick of a travelling wave: converts the surface
     * block at the wave's ground position to the place block. Returns
     * how many blocks were converted (0 or 1).
     */
    public static int trailStep(ServerLevel level, Entity wave, AbilityDef.TerrainSpec spec, int budgetLeft) {
        if (spec == null || budgetLeft <= 0) {
            return 0;
        }
        BlockState state = stateOf(spec.placeId());
        if (state == null) {
            return 0;
        }
        BlockPos surface = PermanentDestruction.findSurface(level,
                (int) Math.floor(wave.getX()), (int) Math.floor(wave.getZ()),
                (int) Math.floor(wave.getY()));
        if (surface == null) {
            return 0;
        }
        BlockPos ground = surface.below();
        BlockState current = level.getBlockState(ground);
        if (PermanentDestruction.isProtected(level, ground, current)) {
            return 0;
        }
        return PermanentDestruction.convert(level, ground, state) ? 1 : 0;
    }

    /**
     * Field event, one tick of a cloud: converts up to budgetPerTick
     * random surface positions inside the current radius to the place
     * block. Returns how many blocks were converted.
     */
    public static int fieldStep(ServerLevel level, Vec3 center, double currentRadius,
                                AbilityDef.TerrainSpec spec, int budgetPerTick) {
        if (spec == null || budgetPerTick <= 0 || currentRadius <= 0) {
            return 0;
        }
        BlockState state = stateOf(spec.placeId());
        if (state == null) {
            return 0;
        }
        int converted = 0;
        int attempts = budgetPerTick * 3;
        for (int i = 0; i < attempts && converted < budgetPerTick; i++) {
            double ang = level.getRandom().nextDouble() * Math.PI * 2.0;
            double rr = currentRadius * Math.sqrt(level.getRandom().nextDouble());
            int x = (int) Math.floor(center.x + Math.cos(ang) * rr);
            int z = (int) Math.floor(center.z + Math.sin(ang) * rr);
            BlockPos surface = PermanentDestruction.findSurface(level, x, z, (int) Math.floor(center.y));
            if (surface == null) {
                continue;
            }
            BlockPos ground = surface.below();
            if (PermanentDestruction.convert(level, ground, state)) {
                converted++;
            }
        }
        return converted;
    }

    /**
     * Field crust: when a field cloud ends, lava blocks in the surface
     * band inside the radius become the crust block, up to the cap.
     */
    public static void crustRing(ServerLevel level, Vec3 center, double radius, AbilityDef.TerrainSpec spec) {
        if (spec == null) {
            return;
        }
        BlockState crust = stateOf(spec.crustId());
        if (crust == null || radius <= 0) {
            return;
        }
        int r = (int) Math.ceil(radius);
        int converted = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r && converted < spec.cap(); dx++) {
            for (int dz = -r; dz <= r && converted < spec.cap(); dz++) {
                if (dx * dx + dz * dz > r * r) {
                    continue;
                }
                for (int dy = -2; dy <= 2 && converted < spec.cap(); dy++) {
                    m.set((int) Math.floor(center.x) + dx,
                            (int) Math.floor(center.y) + dy,
                            (int) Math.floor(center.z) + dz);
                    if (level.getBlockState(m).is(Blocks.LAVA)
                            && PermanentDestruction.convert(level, m, crust)) {
                        converted++;
                    }
                }
            }
        }
    }
}
