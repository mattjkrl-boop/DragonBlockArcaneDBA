package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;

public record ActionPayload(CompoundTag nbtData) implements CustomPacketPayload {
    public static final Type<ActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC = StreamCodec.of(
        (buf, value) -> buf.writeNbt(value.nbtData()),
        buf -> new ActionPayload(buf.readNbt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
