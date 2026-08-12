package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record WishMenuOpenPayload(int shenronId) implements CustomPacketPayload {
    public static final Type<WishMenuOpenPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "wish_menu_open"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WishMenuOpenPayload> CODEC = StreamCodec.of(
        (buf, value) -> buf.writeInt(value.shenronId()),
        buf -> new WishMenuOpenPayload(buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
