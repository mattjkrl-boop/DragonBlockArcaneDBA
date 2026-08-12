package com.dragonblockarcanedba.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record C2SSetGravityPayload(int gravity) implements CustomPacketPayload {
    public static final Type<C2SSetGravityPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "set_gravity"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSetGravityPayload> CODEC = StreamCodec.of(
        (buf, value) -> {
            buf.writeInt(value.gravity());
        },
        buf -> new C2SSetGravityPayload(buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
