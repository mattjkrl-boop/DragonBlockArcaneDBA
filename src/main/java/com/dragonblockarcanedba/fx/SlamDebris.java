package com.dragonblockarcanedba.fx;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The one debris system for ground slams, generalizing the patterns that used to
 * live per ability. On a slam's first tick it throws a burst of block debris made
 * from the actual ground blocks under the impact area (sampled per point, so a
 * slam on stone throws stone and a slam on dirt throws dirt), plus dust and one
 * accent particle per slam family. It never destroys blocks; block destruction
 * stays with the abilities that already own it (OxShockwave). Density scales
 * with the effects config like every other DBA effect.
 */
public final class SlamDebris {
    private SlamDebris() {}

    public enum Profile { OX_FISSURE, Z_GRAVITY, KINGS_SLAM }

    public static void burst(ServerLevel level, Vec3 center, double radius, Profile profile) {
        float density = DbaEffectsConfig.effectDensity();
        if (density <= 0f) return;
        RandomSource random = level.getRandom();
        int count = Math.max(8, Math.min(140, Math.round((30f + (float) radius * 5f) * density)));
        int centerY = (int) Math.floor(center.y);

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(random.nextDouble()) * radius;
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            BlockPos solid = surfaceBlock(level, (int) Math.floor(x), centerY, (int) Math.floor(z));
            if (solid == null) continue;
            BlockState state = level.getBlockState(solid);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                x, solid.getY() + 1.1, z,
                2, 0.28, 0.38, 0.28, 0.12);
        }

        // Dust cloud over the whole impact area.
        level.sendParticles(ParticleTypes.POOF, center.x, center.y + 0.25, center.z,
            Math.max(4, count / 3), radius * 0.45, 0.15, radius * 0.45, 0.05);

        // One accent per slam family: magma embers for the Ox patterns, bright
        // sparks for the gravity slam, both for the King's Slam.
        int accent = Math.max(4, count / 3);
        switch (profile) {
            case OX_FISSURE -> level.sendParticles(DbaParticles.EMBER, center.x, center.y + 0.3, center.z,
                accent, radius * 0.4, 0.25, radius * 0.4, 0.08);
            case Z_GRAVITY -> level.sendParticles(DbaParticles.SPARK, center.x, center.y + 0.3, center.z,
                accent, radius * 0.4, 0.25, radius * 0.4, 0.08);
            case KINGS_SLAM -> {
                level.sendParticles(DbaParticles.EMBER, center.x, center.y + 0.3, center.z,
                    accent / 2, radius * 0.4, 0.25, radius * 0.4, 0.08);
                level.sendParticles(DbaParticles.SPARK, center.x, center.y + 0.3, center.z,
                    accent / 2, radius * 0.4, 0.25, radius * 0.4, 0.08);
            }
        }
    }

    /** Finds the top solid block near the expected ground level, or null over a void or shaft. */
    private static BlockPos surfaceBlock(ServerLevel level, int x, int centerY, int z) {
        for (int y = centerY + 2; y >= centerY - 5; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir()) return pos;
        }
        return null;
    }
}
