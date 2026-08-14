package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record C2SWeaponLeftClickPayload() implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "c2s_weapon_left_click");
    public static final Type<C2SWeaponLeftClickPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SWeaponLeftClickPayload> CODEC = StreamCodec.unit(new C2SWeaponLeftClickPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
