package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record C2SToggleTechniquePayload(int slot) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2SToggleTechniquePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "c2s_toggle_technique"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SToggleTechniquePayload> CODEC = CustomPacketPayload.codec(C2SToggleTechniquePayload::write, C2SToggleTechniquePayload::new);

    public C2SToggleTechniquePayload(RegistryFriendlyByteBuf buf) {
        this(buf.readInt());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.slot);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
