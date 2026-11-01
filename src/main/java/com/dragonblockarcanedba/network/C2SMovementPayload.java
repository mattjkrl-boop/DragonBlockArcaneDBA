package com.dragonblockarcanedba.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S payload: a shared movement verb from the Phase 5 movement layer.
 * Actions: 0 dash, 1 Ki step (mid-air refresh jump), 2 fast fall.
 * The server validates and applies all physics; the client only sends intent.
 */
public record C2SMovementPayload(int action) implements CustomPacketPayload {
    public static final int DASH = 0;
    public static final int KI_STEP = 1;
    public static final int FAST_FALL = 2;

    public static final CustomPacketPayload.Type<C2SMovementPayload> ID =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "movement"));

    public static final StreamCodec<FriendlyByteBuf, C2SMovementPayload> CODEC =
        StreamCodec.of(C2SMovementPayload::write, C2SMovementPayload::read);

    private static void write(FriendlyByteBuf buf, C2SMovementPayload payload) {
        buf.writeVarInt(payload.action);
    }

    private static C2SMovementPayload read(FriendlyByteBuf buf) {
        return new C2SMovementPayload(buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
