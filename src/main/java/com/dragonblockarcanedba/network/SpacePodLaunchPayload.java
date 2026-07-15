package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S packet: the client chose a planet destination in the Space Pod GUI.
 * The server will teleport the player to the corresponding dimension.
 */
public record SpacePodLaunchPayload(String destination) implements CustomPacketPayload {
    public static final Type<SpacePodLaunchPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "space_pod_launch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpacePodLaunchPayload> CODEC = StreamCodec.of(
        (buf, value) -> buf.writeUtf(value.destination()),
        buf -> new SpacePodLaunchPayload(buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
