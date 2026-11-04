package com.dragonblockarcanedba.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Options for the no-shapes material particles. One options record serves
 * the whole material family: the material id selects the registered type,
 * the flipbook set, and the client behavior profile, while color, scale,
 * and seed carry the owning effect's theme and stable variation.
 *
 * Lifetime factor and gravity scale are opt-in behavior modifiers added
 * at the no-shapes merge. Both default to 1.0, which reproduces the
 * Phase 1 profile behavior exactly. The lifetime factor scales the
 * profile's rolled lifetime, so a short-lived host effect can keep its
 * material from lingering as an afterglow tail after the host ends. The
 * gravity scale multiplies the profile gravity, so an effect can ask
 * for genuinely suspended material (TimeShatter glass dust) without a
 * separate profile. DbaMaterialParticle honors both.
 */
public record DbaMaterialOptions(int material, int color, float scale, int seed,
                                 float lifetimeFactor, float gravityScale) implements ParticleOptions {
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
            Codec.INT.fieldOf("seed").forGetter(DbaMaterialOptions::seed),
            Codec.FLOAT.optionalFieldOf("lifetime_factor", 1.0f).forGetter(DbaMaterialOptions::lifetimeFactor),
            Codec.FLOAT.optionalFieldOf("gravity_scale", 1.0f).forGetter(DbaMaterialOptions::gravityScale)
    ).apply(instance, DbaMaterialOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DbaMaterialOptions> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public DbaMaterialOptions decode(RegistryFriendlyByteBuf buf) {
                    int material = buf.readInt();
                    int color = buf.readInt();
                    float scale = buf.readFloat();
                    int seed = buf.readInt();
                    float lifetimeFactor = buf.readFloat();
                    float gravityScale = buf.readFloat();
                    return new DbaMaterialOptions(material, color, scale, seed, lifetimeFactor, gravityScale);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, DbaMaterialOptions value) {
                    buf.writeInt(value.material());
                    buf.writeInt(value.color());
                    buf.writeFloat(value.scale());
                    buf.writeInt(value.seed());
                    buf.writeFloat(value.lifetimeFactor());
                    buf.writeFloat(value.gravityScale());
                }
            };

    public DbaMaterialOptions {
        if (material < 0 || material >= MATERIAL_COUNT) {
            throw new IllegalArgumentException("Unknown DBA material id " + material);
        }
        color &= 0xFFFFFF;
        scale = Math.max(0.05f, scale);
        if (Float.isNaN(lifetimeFactor)) lifetimeFactor = 1.0f;
        lifetimeFactor = Math.max(0.1f, Math.min(3.0f, lifetimeFactor));
        if (Float.isNaN(gravityScale)) gravityScale = 1.0f;
        gravityScale = Math.max(0.0f, Math.min(3.0f, gravityScale));
    }

    /** Four-argument form kept for every Phase 1 and Phase 2 call site: both modifiers default to 1.0. */
    public DbaMaterialOptions(int material, int color, float scale, int seed) {
        this(material, color, scale, seed, 1.0f, 1.0f);
    }

    public static DbaMaterialOptions of(int material, int argb, float scale, int seed) {
        return new DbaMaterialOptions(material, argb, scale, seed);
    }

    /** Copy with a lifetime factor: below 1.0 shortens the material's rolled lifetime. */
    public DbaMaterialOptions withLifetimeFactor(float factor) {
        return new DbaMaterialOptions(material, color, scale, seed, factor, gravityScale);
    }

    /** Copy with a gravity scale: 0.0 suspends the material against its profile gravity. */
    public DbaMaterialOptions withGravityScale(float factor) {
        return new DbaMaterialOptions(material, color, scale, seed, lifetimeFactor, factor);
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
