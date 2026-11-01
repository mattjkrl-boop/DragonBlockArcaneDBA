package com.dragonblockarcanedba.client.particle;

import com.dragonblockarcanedba.particle.DbaParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.core.particles.SimpleParticleType;

public final class DbaParticleProviders {
    private DbaParticleProviders() {}

    public static void register() {
        reg(DbaParticles.SPARK, DbaGlowParticle.SPARK);
        reg(DbaParticles.EMBER, DbaGlowParticle.EMBER);
        reg(DbaParticles.VOID_SMOKE, DbaGlowParticle.VOID_SMOKE);
        reg(DbaParticles.WIND, DbaGlowParticle.WIND);
        reg(DbaParticles.KI_MOTE, DbaGlowParticle.KI_MOTE);
    }

    private static void reg(SimpleParticleType type, DbaGlowParticle.Style style) {
        ParticleProviderRegistry.getInstance().register(type, sprites ->
            (options, level, x, y, z, dx, dy, dz, random) -> new DbaGlowParticle(level, x, y, z, dx, dy, dz, sprites, style));
    }
}
