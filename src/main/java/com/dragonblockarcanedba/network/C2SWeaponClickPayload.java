package com.dragonblockarcanedba.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S payload for the rebuilt weapon input: one packet for both
 * buttons. button 0 is left click, 1 is right click; action 0 is
 * press, 1 is release; heldTicks is the client-counted hold duration
 * on release (0 on press). The server re-derives the ability from
 * the item actually in the player's main hand, so this payload can
 * only ever trigger what the held weapon legitimately has.
 */
public record C2SWeaponClickPayload(int button, int action, int heldTicks) implements CustomPacketPayload {
    public static final int BUTTON_LEFT = 0;
    public static final int BUTTON_RIGHT = 1;
    public static final int ACTION_PRESS = 0;
    public static final int ACTION_RELEASE = 1;

    public static final CustomPacketPayload.Type<C2SWeaponClickPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "weapon_click"));

    public static final StreamCodec<FriendlyByteBuf, C2SWeaponClickPayload> CODEC =
            StreamCodec.of(C2SWeaponClickPayload::write, C2SWeaponClickPayload::read);

    private static void write(FriendlyByteBuf buf, C2SWeaponClickPayload payload) {
        buf.writeVarInt(payload.button);
        buf.writeVarInt(payload.action);
        buf.writeVarInt(payload.heldTicks);
    }

    private static C2SWeaponClickPayload read(FriendlyByteBuf buf) {
        return new C2SWeaponClickPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
