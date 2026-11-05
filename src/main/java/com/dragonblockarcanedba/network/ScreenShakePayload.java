package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: shake the camera (degrees of jitter), for a
 * number of ticks, with an optional hitstop hold.
 *
 * Phase 5 added the origin and radius: an ability shake names
 * where it happened and how far it reaches, and each client
 * scales the strength by the distance-squared falloff from its
 * own position (ability/FeedbackMath) before the shake enters the
 * client shake store. A radius of 0 means a direct shake with no
 * falloff; that is what the original per-player sends (movement
 * landings, the MobPhysics finisher) use, with the origin set to
 * the receiving player's own position for completeness.
 */
public record ScreenShakePayload(float intensity, int durationTicks, int hitstopTicks,
                                 double ox, double oy, double oz, float radius)
        implements CustomPacketPayload {
    public static final Type<ScreenShakePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "screen_shake"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScreenShakePayload> CODEC = StreamCodec.of(
        (buf, v) -> {
            buf.writeFloat(v.intensity()); buf.writeVarInt(v.durationTicks()); buf.writeVarInt(v.hitstopTicks());
            buf.writeDouble(v.ox()); buf.writeDouble(v.oy()); buf.writeDouble(v.oz()); buf.writeFloat(v.radius());
        },
        buf -> new ScreenShakePayload(buf.readFloat(), buf.readVarInt(), buf.readVarInt(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
