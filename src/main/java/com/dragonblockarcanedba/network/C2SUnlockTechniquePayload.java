package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record C2SUnlockTechniquePayload(String techniqueId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2SUnlockTechniquePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "c2s_unlock_technique"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SUnlockTechniquePayload> CODEC = CustomPacketPayload.codec(C2SUnlockTechniquePayload::write, C2SUnlockTechniquePayload::new);

    public C2SUnlockTechniquePayload(RegistryFriendlyByteBuf buf) {
        this(buf.readUtf());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(this.techniqueId);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
