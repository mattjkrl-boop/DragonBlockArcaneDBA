package com.dragonblockarcanedba.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Theme-colored dust options: an RGB color (low 24 bits) plus a scale, with the
 *  same semantics LayeredParticles.dust had on DustParticleOptions, but on the mod's
 *  own THEME_DUST type with the mod's own texture and soft additive rendering. */
public record DbaDustOptions(int color, float scale) implements ParticleOptions {

    public static final MapCodec<DbaDustOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("color").forGetter(DbaDustOptions::color),
            Codec.FLOAT.fieldOf("scale").forGetter(DbaDustOptions::scale)
    ).apply(instance, DbaDustOptions::new));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DbaDustOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, DbaDustOptions::color,
                    ByteBufCodecs.FLOAT, DbaDustOptions::scale,
                    DbaDustOptions::new);

    @Override
    public ParticleType<?> getType() {
        return DbaParticles.THEME_DUST;
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
