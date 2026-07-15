package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C payload that broadcasts a player's transformation state to nearby clients.
 * This allows other players to see aura particles and transformation visuals.
 */
public record TransformBroadcastPayload(
    int entityId,
    String raceId,
    String activeFormId
) implements CustomPacketPayload {

    public static final Type<TransformBroadcastPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("dragonblockarcanedba", "transform_broadcast")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TransformBroadcastPayload> CODEC = StreamCodec.of(
        (buf, value) -> {
            buf.writeVarInt(value.entityId());
            buf.writeUtf(value.raceId());
            buf.writeUtf(value.activeFormId());
        },
        buf -> new TransformBroadcastPayload(
            buf.readVarInt(),
            buf.readUtf(),
            buf.readUtf()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
