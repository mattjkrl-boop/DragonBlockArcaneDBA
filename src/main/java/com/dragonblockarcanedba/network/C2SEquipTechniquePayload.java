package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record C2SEquipTechniquePayload(int slot, String techniqueId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2SEquipTechniquePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "c2s_equip_technique"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SEquipTechniquePayload> CODEC = CustomPacketPayload.codec(C2SEquipTechniquePayload::write, C2SEquipTechniquePayload::new);

    public C2SEquipTechniquePayload(RegistryFriendlyByteBuf buf) {
        this(buf.readInt(), buf.readUtf());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.slot);
        buf.writeUtf(this.techniqueId);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
