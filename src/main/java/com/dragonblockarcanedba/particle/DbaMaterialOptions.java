package com.dragonblockarcanedba.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Options for the no-shapes material particles. One options record serves
 * the whole material family: the material id selects the registered type,
 * the flipbook set, and the client behavior profile, while color, scale,
 * and seed carry the owning effect's theme and stable variation.
 */
public record DbaMaterialOptions(int material, int color, float scale, int seed) implements ParticleOptions {
    public static final int PLASMA = 0;
    public static final int SMOKE = 1;
    public static final int VAPOR = 2;
    public static final int DUST = 3;
    public static final int MIST = 4;
    public static final int SOUL = 5;
    public static final int DARKNESS = 6;
    public static final int GLASS_DUST = 7;
    public static final int MATERIAL_COUNT = 8;

    public static final MapCodec<DbaMaterialOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("material").forGetter(DbaMaterialOptions::material),
            Codec.INT.fieldOf("color").forGetter(DbaMaterialOptions::color),
            Codec.FLOAT.fieldOf("scale").forGetter(DbaMaterialOptions::scale),
            Codec.INT.fieldOf("seed").forGetter(DbaMaterialOptions::seed)
    ).apply(instance, DbaMaterialOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DbaMaterialOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, DbaMaterialOptions::material,
                    ByteBufCodecs.INT, DbaMaterialOptions::color,
                    ByteBufCodecs.FLOAT, DbaMaterialOptions::scale,
                    ByteBufCodecs.INT, DbaMaterialOptions::seed,
                    DbaMaterialOptions::new);

    public DbaMaterialOptions {
        if (material < 0 || material >= MATERIAL_COUNT) {
            throw new IllegalArgumentException("Unknown DBA material id " + material);
        }
        color &= 0xFFFFFF;
        scale = Math.max(0.05f, scale);
    }

    public static DbaMaterialOptions of(int material, int argb, float scale, int seed) {
        return new DbaMaterialOptions(material, argb, scale, seed);
    }

    @Override
    public ParticleType<?> getType() {
        return switch (material) {
            case PLASMA -> DbaParticles.PLASMA;
            case SMOKE -> DbaParticles.SMOKE;
            case VAPOR -> DbaParticles.VAPOR;
            case DUST -> DbaParticles.DUST;
            case MIST -> DbaParticles.MIST;
            case SOUL -> DbaParticles.SOUL;
            case DARKNESS -> DbaParticles.DARKNESS;
            case GLASS_DUST -> DbaParticles.GLASS_DUST;
            default -> throw new IllegalStateException("Unreachable material id " + material);
        };
    }

    public float red() {
        return ((color >> 16) & 0xFF) / 255.0f;
    }

    public float green() {
        return ((color >> 8) & 0xFF) / 255.0f;
    }

    public float blue() {
        return (color & 0xFF) / 255.0f;
    }
}
