package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C payload that broadcasts a player's active emote to nearby clients in multiplayer.
 */
public record EmoteBroadcastPayload(
    int entityId,
    String emote
) implements CustomPacketPayload {

    public static final Type<EmoteBroadcastPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("dragonblockarcanedba", "emote_broadcast")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, EmoteBroadcastPayload> CODEC = StreamCodec.of(
        (buf, value) -> {
            buf.writeVarInt(value.entityId());
            buf.writeUtf(value.emote());
        },
        buf -> new EmoteBroadcastPayload(
            buf.readVarInt(),
            buf.readUtf()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
