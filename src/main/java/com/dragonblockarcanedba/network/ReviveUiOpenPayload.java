package com.dragonblockarcanedba.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ReviveUiOpenPayload() implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "revive_ui_open");
    public static final CustomPacketPayload.Type<ReviveUiOpenPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, ReviveUiOpenPayload> CODEC = StreamCodec.unit(new ReviveUiOpenPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
