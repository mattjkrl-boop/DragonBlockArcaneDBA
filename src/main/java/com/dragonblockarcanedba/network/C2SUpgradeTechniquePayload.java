package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record C2SUpgradeTechniquePayload(String techniqueId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2SUpgradeTechniquePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "c2s_upgrade_technique"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SUpgradeTechniquePayload> CODEC = CustomPacketPayload.codec(C2SUpgradeTechniquePayload::write, C2SUpgradeTechniquePayload::new);

    public C2SUpgradeTechniquePayload(RegistryFriendlyByteBuf buf) {
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
