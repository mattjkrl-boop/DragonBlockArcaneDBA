package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record C2SWeaponLeftClickPayload(int actionType, int chargeTicks) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "c2s_weapon_left_click");
    public static final Type<C2SWeaponLeftClickPayload> TYPE = new Type<>(ID);

    public static final int ACTION_CLICK = 0;
    public static final int ACTION_CHARGE_TICK = 1;
    public static final int ACTION_RELEASE = 2;

    public C2SWeaponLeftClickPayload() {
        this(ACTION_CLICK, 0);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SWeaponLeftClickPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, C2SWeaponLeftClickPayload::actionType,
        ByteBufCodecs.VAR_INT, C2SWeaponLeftClickPayload::chargeTicks,
        C2SWeaponLeftClickPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
