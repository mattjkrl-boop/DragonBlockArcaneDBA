package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;

public record StatsSyncPayload(CompoundTag nbtData) implements CustomPacketPayload {
    public static final Type<StatsSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "stats_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StatsSyncPayload> CODEC = StreamCodec.of(
        (buf, value) -> buf.writeNbt(value.nbtData()),
        buf -> new StatsSyncPayload(buf.readNbt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
