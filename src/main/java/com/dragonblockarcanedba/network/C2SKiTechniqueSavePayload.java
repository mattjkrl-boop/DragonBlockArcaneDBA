package com.dragonblockarcanedba.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S payload: saves a Ki technique configuration to a player slot (0–2).
 */
public record C2SKiTechniqueSavePayload(int slot, String techType, int usedPercent, int color, boolean isBarrage) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2SKiTechniqueSavePayload> ID =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "ki_tech_save"));

    public static final StreamCodec<FriendlyByteBuf, C2SKiTechniqueSavePayload> CODEC =
        StreamCodec.of(C2SKiTechniqueSavePayload::write, C2SKiTechniqueSavePayload::read);

    private static void write(FriendlyByteBuf buf, C2SKiTechniqueSavePayload payload) {
        buf.writeVarInt(payload.slot);
        buf.writeUtf(payload.techType);
        buf.writeVarInt(payload.usedPercent);
        buf.writeInt(payload.color);
        buf.writeBoolean(payload.isBarrage);
    }

    private static C2SKiTechniqueSavePayload read(FriendlyByteBuf buf) {
        return new C2SKiTechniqueSavePayload(
            buf.readVarInt(),
            buf.readUtf(),
            buf.readVarInt(),
            buf.readInt(),
            buf.readBoolean()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
