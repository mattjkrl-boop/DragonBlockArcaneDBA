package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C payload that broadcasts a player's race, form, tail, and color state to nearby clients.
 * This allows other players to see accurate models, tails, aura particles, and customization.
 */
public record TransformBroadcastPayload(
    int entityId,
    String raceId,
    String activeFormId,
    boolean tailSevered,
    String skinColor,
    String hairColor
) implements CustomPacketPayload {

    public static final Type<TransformBroadcastPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("dragonblockarcanedba", "transform_broadcast")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TransformBroadcastPayload> CODEC = StreamCodec.of(
        (buf, value) -> {
            buf.writeVarInt(value.entityId());
            buf.writeUtf(value.raceId());
            buf.writeUtf(value.activeFormId());
            buf.writeBoolean(value.tailSevered());
            buf.writeUtf(value.skinColor());
            buf.writeUtf(value.hairColor());
        },
        buf -> new TransformBroadcastPayload(
            buf.readVarInt(),
            buf.readUtf(),
            buf.readUtf(),
            buf.readBoolean(),
            buf.readUtf(),
            buf.readUtf()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
