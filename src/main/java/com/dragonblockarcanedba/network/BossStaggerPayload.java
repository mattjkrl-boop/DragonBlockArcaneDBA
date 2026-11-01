package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: a boss-class entity took a heavy DBA hit and staggered.
 * Carries the stagger strength and duration so the client can play the rock and
 * dip animation. Client visual only; the brief slowness is applied server side
 * by MobPhysics.
 */
public record BossStaggerPayload(int entityId, float strength, int durationTicks) implements CustomPacketPayload {
    public static final Type<BossStaggerPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "boss_stagger"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BossStaggerPayload> CODEC = StreamCodec.of(
        (buf, v) -> { buf.writeVarInt(v.entityId()); buf.writeFloat(v.strength()); buf.writeVarInt(v.durationTicks()); },
        buf -> new BossStaggerPayload(buf.readVarInt(), buf.readFloat(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
