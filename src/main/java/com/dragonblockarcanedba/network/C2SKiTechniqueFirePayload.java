package com.dragonblockarcanedba.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S payload: fire the Ki technique equipped in a slot (0–2).
 */
public record C2SKiTechniqueFirePayload(int slot) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2SKiTechniqueFirePayload> ID =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "ki_tech_fire"));

    public static final StreamCodec<FriendlyByteBuf, C2SKiTechniqueFirePayload> CODEC =
        StreamCodec.of(C2SKiTechniqueFirePayload::write, C2SKiTechniqueFirePayload::read);

    private static void write(FriendlyByteBuf buf, C2SKiTechniqueFirePayload payload) {
        buf.writeVarInt(payload.slot);
    }

    private static C2SKiTechniqueFirePayload read(FriendlyByteBuf buf) {
        return new C2SKiTechniqueFirePayload(buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
