package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record C2SMakeWishPayload(int shenronId, String wishType) implements CustomPacketPayload {
    public static final Type<C2SMakeWishPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "make_wish"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMakeWishPayload> CODEC = StreamCodec.of(
        (buf, value) -> {
            buf.writeInt(value.shenronId());
            buf.writeUtf(value.wishType());
        },
        buf -> new C2SMakeWishPayload(buf.readInt(), buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
