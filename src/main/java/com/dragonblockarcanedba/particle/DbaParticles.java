package com.dragonblockarcanedba.particle;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Custom particles for the whole mod. Rendering lives in the client DbaParticleProviders.
 *  Bespoke pass: weapon effects use ONLY these types, never vanilla ParticleTypes.
 *  Simple types are styled per type by DbaGlowParticle.Style; THEME_DUST carries a
 *  per-particle color and scale (the bespoke stand-in for DustParticleOptions) and
 *  BLOCK_CHUNKS carries a BlockState (the stand-in for ParticleTypes.BLOCK). */
public final class DbaParticles {
    private DbaParticles() {}

    public static final SimpleParticleType SPARK = FabricParticleTypes.simple(true);
    public static final SimpleParticleType EMBER = FabricParticleTypes.simple(true);
    public static final SimpleParticleType VOID_SMOKE = FabricParticleTypes.simple(true);
    public static final SimpleParticleType WIND = FabricParticleTypes.simple(true);
    public static final SimpleParticleType KI_MOTE = FabricParticleTypes.simple(true);
    public static final SimpleParticleType ASH = FabricParticleTypes.simple(true);
    public static final SimpleParticleType WISP = FabricParticleTypes.simple(true);
    public static final SimpleParticleType RAIN = FabricParticleTypes.simple(true);
    public static final SimpleParticleType DUST_PUFF = FabricParticleTypes.simple(true);
    public static final SimpleParticleType CRIT_SPARK = FabricParticleTypes.simple(true);
    public static final SimpleParticleType FLAME = FabricParticleTypes.simple(true);
    public static final SimpleParticleType BLAST = FabricParticleTypes.simple(true);

    // Scope 2 types: big soft sky clouds for the Otherworld cloud systems (D5),
    // and the tail feedback glyphs (D10): a jagged damage pip and a heal sparkle.
    public static final SimpleParticleType CLOUD_PUFF = FabricParticleTypes.simple(true);
    public static final SimpleParticleType DAMAGE_SPARK = FabricParticleTypes.simple(true);
    public static final SimpleParticleType HAPPY_SPARKLE = FabricParticleTypes.simple(true);

    /** Colored soft dust, tinted per particle. Replaces DustParticleOptions in weapon scope. */
    public static final ParticleType<DbaDustOptions> THEME_DUST = FabricParticleTypes.complex(
            DbaDustOptions.CODEC, DbaDustOptions.STREAM_CODEC);

    /** Block crumble chunks carrying a real BlockState. Replaces ParticleTypes.BLOCK in weapon scope. */
    public static final ParticleType<BlockParticleOption> BLOCK_CHUNKS = FabricParticleTypes.complex(
            type -> BlockParticleOption.codec(type), type -> BlockParticleOption.streamCodec(type));

    public static void register() {
        reg("spark", SPARK);
        reg("ember", EMBER);
        reg("void_smoke", VOID_SMOKE);
        reg("wind", WIND);
        reg("ki_mote", KI_MOTE);
        reg("ash", ASH);
        reg("wisp", WISP);
        reg("rain", RAIN);
        reg("dust_puff", DUST_PUFF);
        reg("crit_spark", CRIT_SPARK);
        reg("flame", FLAME);
        reg("blast", BLAST);
        reg("cloud_puff", CLOUD_PUFF);
        reg("damage_spark", DAMAGE_SPARK);
        reg("happy_sparkle", HAPPY_SPARKLE);
        reg("theme_dust", THEME_DUST);
        reg("block_chunks", BLOCK_CHUNKS);
    }

    private static void reg(String name, ParticleType<?> type) {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("dragonblockarcanedba", name), type);
    }
}
