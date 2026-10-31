package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: shake the camera (degrees of jitter), for a number of ticks, with an optional hitstop hold. */
public record ScreenShakePayload(float intensity, int durationTicks, int hitstopTicks) implements CustomPacketPayload {
    public static final Type<ScreenShakePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "screen_shake"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScreenShakePayload> CODEC = StreamCodec.of(
        (buf, v) -> { buf.writeFloat(v.intensity()); buf.writeVarInt(v.durationTicks()); buf.writeVarInt(v.hitstopTicks()); },
        buf -> new ScreenShakePayload(buf.readFloat(), buf.readVarInt(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
