package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: a living entity died to a DBA hit; give its death a ragdoll-lite
 * tumble scaled by strength. Client visual only, server state stays vanilla.
 */
public record DeathTumblePayload(int entityId, float strength) implements CustomPacketPayload {
    public static final Type<DeathTumblePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "death_tumble"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeathTumblePayload> CODEC = StreamCodec.of(
        (buf, v) -> { buf.writeVarInt(v.entityId()); buf.writeFloat(v.strength()); },
        buf -> new DeathTumblePayload(buf.readVarInt(), buf.readFloat())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
