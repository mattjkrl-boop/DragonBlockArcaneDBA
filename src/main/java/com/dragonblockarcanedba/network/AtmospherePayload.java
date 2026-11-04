package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: an atmosphere zone (storm gloom) is active at a center.
 * The client refreshes its local gloom source; the gloom itself lerps in and
 * out on the client and expires ticks after the last refresh, so the sky
 * never cuts. Weather itself is real server weather via AtmosphereServer.
 */
public record AtmospherePayload(double x, double y, double z, float radius, float gloom, int ticks) implements CustomPacketPayload {
    public static final Type<AtmospherePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "atmosphere"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AtmospherePayload> CODEC = StreamCodec.of(
        (buf, v) -> {
            buf.writeDouble(v.x()); buf.writeDouble(v.y()); buf.writeDouble(v.z());
            buf.writeFloat(v.radius()); buf.writeFloat(v.gloom()); buf.writeVarInt(v.ticks());
        },
        buf -> new AtmospherePayload(buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readFloat(), buf.readFloat(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
