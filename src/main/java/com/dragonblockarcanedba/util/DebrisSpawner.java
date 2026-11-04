package com.dragonblockarcanedba.util;

import com.dragonblockarcanedba.entity.DebrisEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Spawns helical debris fields (SPECTACLE-STANDARD point 2). By default the
 * palette is sampled from the actual ground around the center, so a storm
 * throws the terrain it is standing on, the way the reference video does.
 * Pass an explicit palette for themed debris (ice, netherrack, soul sand...).
 * Pieces come in three sizes, orbit at speeds that fall with radius, rise
 * together, then release and rain down inside DebrisEntity itself.
 */
public final class DebrisSpawner {
    private DebrisSpawner() {}

    private static final float[] SIZES = {0.45f, 0.7f, 1.0f};

    public static List<BlockState> sampleGroundPalette(ServerLevel level, Vec3 center, double radius) {
        List<BlockState> palette = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int cx = (int) Math.floor(center.x), cz = (int) Math.floor(center.z);
        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r && palette.size() < 6; dx += 2) {
            for (int dz = -r; dz <= r && palette.size() < 6; dz += 2) {
                if (dx * dx + dz * dz > r * r) continue;
                // Walk down from a few blocks above center height to find the surface.
                for (int dy = 3; dy >= -6; dy--) {
                    m.set(cx + dx, (int) Math.floor(center.y) + dy, cz + dz);
                    BlockState state = level.getBlockState(m);
                    if (!state.isAir() && state.getBlock() != Blocks.WATER && state.getBlock() != Blocks.LAVA) {
                        if (!palette.contains(state)) palette.add(state);
                        break;
                    }
                }
            }
        }
        if (palette.isEmpty()) {
            palette.add(Blocks.DIRT.defaultBlockState());
            palette.add(Blocks.STONE.defaultBlockState());
            palette.add(Blocks.GRAVEL.defaultBlockState());
        }
        return palette;
    }

    /**
     * Spawn a debris field. count is scaled by effect density inside
     * DebrisEntity counts at the call sites; pass the full intended count.
     * Returns how many pieces were spawned.
     */
    public static int spawnField(ServerLevel level, LivingEntity owner, Vec3 center, int count,
                                 double maxRadius, double riseHeight, int orbitTicks,
                                 List<BlockState> palette) {
        if (count <= 0 || palette == null || palette.isEmpty()) return 0;
        RandomSource rand = level.getRandom();
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            double radius = maxRadius * (0.25 + 0.75 * rand.nextDouble());
            double angle = rand.nextDouble() * Math.PI * 2.0;
            // Outer pieces orbit slower, inner pieces race: one coherent storm.
            double angularSpeed = (0.10 + 0.10 * rand.nextDouble()) * (2.2 / Math.max(1.0, radius));
            if (rand.nextBoolean()) angularSpeed = Math.abs(angularSpeed);
            double riseSpeed = (riseHeight / Math.max(10, orbitTicks)) * (0.8 + 0.4 * rand.nextDouble());
            float scale = SIZES[rand.nextInt(SIZES.length)];
            BlockState block = palette.get(rand.nextInt(palette.size()));
            DebrisEntity debris = new DebrisEntity(level, center, block, scale,
                rand.nextInt(), radius, angle, angularSpeed, riseSpeed,
                orbitTicks + rand.nextInt(20));
            if (owner != null) debris.setOwner(owner);
            level.addFreshEntity(debris);
            spawned++;
        }
        return spawned;
    }

    public static int spawnField(ServerLevel level, LivingEntity owner, Vec3 center, int count,
                                 double maxRadius, double riseHeight, int orbitTicks) {
        return spawnField(level, owner, center, count, maxRadius, riseHeight, orbitTicks,
            sampleGroundPalette(level, center, maxRadius));
    }
}
