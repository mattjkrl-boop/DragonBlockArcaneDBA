package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C packet: tells the client to open the Space Pod destination picker screen.
 */
public record SpacePodOpenPayload() implements CustomPacketPayload {
    public static final Type<SpacePodOpenPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "space_pod_open"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpacePodOpenPayload> CODEC = StreamCodec.of(
        (buf, value) -> {},
        buf -> new SpacePodOpenPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
