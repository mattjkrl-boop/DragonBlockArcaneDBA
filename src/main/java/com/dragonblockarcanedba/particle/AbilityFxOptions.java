package com.dragonblockarcanedba.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Options for the ONE universal ability particle type
 * (DbaParticles.ABILITY_FX), Phase 3 of the rebuild. Every instance
 * is fully parameterized at spawn: the texture entry (an
 * AbilityFxEntries id selecting the sprite block and the lit or
 * additive behavior), the tint, the size at birth and at death (the
 * size-over-life curve is the straight line between them), the exact
 * lifetime in ticks, the gravity and drag the particle flies with,
 * and a seed for cosmetic variation (roll direction and phase).
 * Initial position and velocity travel in the standard addParticle
 * arguments, as with every particle.
 *
 * Nothing here is rolled on the client: recipe expansion
 * (client/fx/AbilityFxExpander) derives every field from the effect
 * seed before spawning, so two clients expanding the same event
 * produce the same instances. Gravity uses the same units as the
 * material particle profiles (the value handed to Particle.gravity:
 * negative floats rise, 0.16 is falling glass); drag is the
 * per-tick velocity retention handed to Particle.friction.
 */
public record AbilityFxOptions(int entry, int color, float sizeFrom, float sizeTo,
                               int lifetime, float gravity, float drag, int seed)
        implements ParticleOptions {
    public static final MapCodec<AbilityFxOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("entry").forGetter(AbilityFxOptions::entry),
            Codec.INT.fieldOf("color").forGetter(AbilityFxOptions::color),
            Codec.FLOAT.fieldOf("size_from").forGetter(AbilityFxOptions::sizeFrom),
            Codec.FLOAT.fieldOf("size_to").forGetter(AbilityFxOptions::sizeTo),
            Codec.INT.fieldOf("lifetime").forGetter(AbilityFxOptions::lifetime),
            Codec.FLOAT.fieldOf("gravity").forGetter(AbilityFxOptions::gravity),
            Codec.FLOAT.fieldOf("drag").forGetter(AbilityFxOptions::drag),
            Codec.INT.fieldOf("seed").forGetter(AbilityFxOptions::seed)
    ).apply(instance, AbilityFxOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityFxOptions> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public AbilityFxOptions decode(RegistryFriendlyByteBuf buf) {
                    return new AbilityFxOptions(buf.readInt(), buf.readInt(),
                            buf.readFloat(), buf.readFloat(), buf.readInt(),
                            buf.readFloat(), buf.readFloat(), buf.readInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, AbilityFxOptions value) {
                    buf.writeInt(value.entry());
                    buf.writeInt(value.color());
                    buf.writeFloat(value.sizeFrom());
                    buf.writeFloat(value.sizeTo());
                    buf.writeInt(value.lifetime());
                    buf.writeFloat(value.gravity());
                    buf.writeFloat(value.drag());
                    buf.writeInt(value.seed());
                }
            };

    public AbilityFxOptions {
        if (entry < 0 || entry >= AbilityFxEntries.COUNT) {
            throw new IllegalArgumentException("Unknown ability fx entry id " + entry);
        }
        sizeFrom = Math.max(0.01f, sizeFrom);
        sizeTo = Math.max(0.01f, sizeTo);
        lifetime = Math.max(1, lifetime);
        if (Float.isNaN(gravity)) gravity = 0f;
        gravity = Math.max(-1f, Math.min(1f, gravity));
        if (Float.isNaN(drag)) drag = 0.98f;
        drag = Math.max(0f, Math.min(1f, drag));
    }

    /** Alpha byte of the tint (the high byte of the ARGB color). */
    public float alpha() {
        return ((color >>> 24) & 0xFF) / 255.0f;
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

    @Override
    public ParticleType<?> getType() {
        return DbaParticles.ABILITY_FX;
    }
}
