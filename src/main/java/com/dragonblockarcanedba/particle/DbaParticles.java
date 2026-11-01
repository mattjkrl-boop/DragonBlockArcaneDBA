package com.dragonblockarcanedba.particle;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Custom additive glow particles. Rendering lives in the client DbaParticleProviders. */
public final class DbaParticles {
    private DbaParticles() {}

    public static final SimpleParticleType SPARK = FabricParticleTypes.simple(true);
    public static final SimpleParticleType EMBER = FabricParticleTypes.simple(true);
    public static final SimpleParticleType VOID_SMOKE = FabricParticleTypes.simple(true);
    public static final SimpleParticleType WIND = FabricParticleTypes.simple(true);
    public static final SimpleParticleType KI_MOTE = FabricParticleTypes.simple(true);

    public static void register() {
        reg("spark", SPARK);
        reg("ember", EMBER);
        reg("void_smoke", VOID_SMOKE);
        reg("wind", WIND);
        reg("ki_mote", KI_MOTE);
    }

    private static void reg(String name, SimpleParticleType type) {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("dragonblockarcanedba", name), type);
    }
}
