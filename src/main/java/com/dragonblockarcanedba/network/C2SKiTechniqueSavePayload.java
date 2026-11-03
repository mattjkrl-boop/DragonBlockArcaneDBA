package com.dragonblockarcanedba.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S payload: saves a Ki technique configuration to a player slot (0–2).
 */
public record C2SKiTechniqueSavePayload(int slot, String techType, int usedPercent, int color, boolean isBarrage, int chargeTicks, double size) implements CustomPacketPayload {
    /** Craft saves that have not chosen a charge or a size keep the live attack unchanged. */
    public C2SKiTechniqueSavePayload(int slot, String techType, int usedPercent, int color, boolean isBarrage) {
        this(slot, techType, usedPercent, color, isBarrage, 0, 1.0);
    }

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
        buf.writeVarInt(payload.chargeTicks);
        buf.writeDouble(payload.size);
    }

    private static C2SKiTechniqueSavePayload read(FriendlyByteBuf buf) {
        return new C2SKiTechniqueSavePayload(
            buf.readVarInt(),
            buf.readUtf(),
            buf.readVarInt(),
            buf.readInt(),
            buf.readBoolean(),
            buf.readVarInt(),
            buf.readDouble()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
