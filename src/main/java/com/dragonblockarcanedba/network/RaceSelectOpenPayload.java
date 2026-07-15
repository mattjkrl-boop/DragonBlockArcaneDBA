package com.dragonblockarcanedba.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Payload sent from server to client to open the Race Selection Screen.
 */
public record RaceSelectOpenPayload() implements CustomPacketPayload {
    public static final Type<RaceSelectOpenPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "race_select_open"));

    public static final StreamCodec<FriendlyByteBuf, RaceSelectOpenPayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            // No data needed
        },
        buf -> new RaceSelectOpenPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
