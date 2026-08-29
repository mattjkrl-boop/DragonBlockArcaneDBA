package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlayerKiBroadcastPayload(int entityId, float currentKi, float maxKi) implements CustomPacketPayload {
    public static final Type<PlayerKiBroadcastPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("dragonblockarcanedba", "player_ki_broadcast")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerKiBroadcastPayload> CODEC = StreamCodec.of(
        (buf, value) -> {
            buf.writeVarInt(value.entityId());
            buf.writeFloat(value.currentKi());
            buf.writeFloat(value.maxKi());
        },
        buf -> new PlayerKiBroadcastPayload(
            buf.readVarInt(),
            buf.readFloat(),
            buf.readFloat()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
