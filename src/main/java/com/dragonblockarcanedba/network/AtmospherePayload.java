package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: an atmosphere zone (weapon storm) is active at a center.
 * Bespoke pass (BESPOKE-STANDARD rule 5): the storm is a tracked volume only;
 * the world's real weather is never touched. The client renders the storm
 * itself: rain particles, gloom through the post grade, and thunder sounds
 * for THUNDER kind. kind is the AtmosphereServer.Kind ordinal (0 RAIN,
 * 1 THUNDER). The gloom lerps in and out on the client and expires ticks
 * after the last refresh, so the sky never cuts.
 */
public record AtmospherePayload(double x, double y, double z, float radius, float gloom, int ticks, int kind) implements CustomPacketPayload {
    public static final int KIND_RAIN = 0;
    public static final int KIND_THUNDER = 1;

    public static final Type<AtmospherePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "atmosphere"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AtmospherePayload> CODEC = StreamCodec.of(
        (buf, v) -> {
            buf.writeDouble(v.x()); buf.writeDouble(v.y()); buf.writeDouble(v.z());
            buf.writeFloat(v.radius()); buf.writeFloat(v.gloom()); buf.writeVarInt(v.ticks());
            buf.writeVarInt(v.kind());
        },
        buf -> new AtmospherePayload(buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
