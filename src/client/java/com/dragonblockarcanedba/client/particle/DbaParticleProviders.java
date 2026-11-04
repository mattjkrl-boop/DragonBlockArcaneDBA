package com.dragonblockarcanedba.client.particle;

import com.dragonblockarcanedba.particle.DbaParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class DbaParticleProviders {
    private DbaParticleProviders() {}

    public static void register() {
        reg(DbaParticles.SPARK, DbaGlowParticle.SPARK);
        reg(DbaParticles.EMBER, DbaGlowParticle.EMBER);
        reg(DbaParticles.VOID_SMOKE, DbaGlowParticle.VOID_SMOKE);
        reg(DbaParticles.WIND, DbaGlowParticle.WIND);
        reg(DbaParticles.KI_MOTE, DbaGlowParticle.KI_MOTE);
        reg(DbaParticles.ASH, DbaGlowParticle.ASH);
        reg(DbaParticles.WISP, DbaGlowParticle.WISP);
        reg(DbaParticles.RAIN, DbaGlowParticle.RAIN);
        reg(DbaParticles.DUST_PUFF, DbaGlowParticle.DUST_PUFF);
        reg(DbaParticles.CRIT_SPARK, DbaGlowParticle.CRIT_SPARK);
        reg(DbaParticles.FLAME, DbaGlowParticle.FLAME_TONGUE);
        reg(DbaParticles.BLAST, DbaGlowParticle.BLAST);
        reg(DbaParticles.CLOUD_PUFF, DbaGlowParticle.CLOUD_PUFF);
        reg(DbaParticles.DAMAGE_SPARK, DbaGlowParticle.DAMAGE_SPARK);
        reg(DbaParticles.HAPPY_SPARKLE, DbaGlowParticle.HAPPY_SPARKLE);
        // Theme dust: tinted per particle by its options.
        ParticleProviderRegistry.getInstance().register(DbaParticles.THEME_DUST, sprites ->
            (options, level, x, y, z, dx, dy, dz, random) -> new DbaDustParticle(level, x, y, z, dx, dy, dz, sprites, options));
        // Block chunks: terrain chunks textured from the carried BlockState, drawn by the
        // mod's own registered type (the particle class itself is the engine's chunk particle).
        ParticleProviderRegistry.getInstance().register(DbaParticles.BLOCK_CHUNKS,
            (options, level, x, y, z, dx, dy, dz, random) -> new TerrainParticle(level, x, y, z, dx, dy, dz, options.getState()));
    }

    private static void reg(SimpleParticleType type, DbaGlowParticle.Style style) {
        ParticleProviderRegistry.getInstance().register(type, sprites ->
            (options, level, x, y, z, dx, dy, dz, random) -> new DbaGlowParticle(level, x, y, z, dx, dy, dz, sprites, style));
    }
}
