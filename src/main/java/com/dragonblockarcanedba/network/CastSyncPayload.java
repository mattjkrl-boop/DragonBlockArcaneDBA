package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C cast lifecycle signal for the Phase 2 animation layer, sent
 * by AbilityService to the caster and every player tracking them.
 * This is the minimal signal the pose layer cannot derive locally:
 * START announces a cast entering its startup phase (the wind-up
 * happens before any carrier entity exists, so nothing else is
 * server-visible yet); FIRE carries the exact game tick the cast
 * fired on, which for a charge-release cast is the release moment
 * and cannot be predicted by observers (the pose strike keyframe,
 * the carrier spawn, and the fire sound all share this tick);
 * CANCEL ends a wind-up that will never fire (weapon swapped,
 * charge replaced), which matters because a charge can be held
 * indefinitely. tick is level game time on the server clock, the
 * same clock clients read from their own level.
 */
public record CastSyncPayload(int entityId, String abilityId, int action, long tick)
        implements CustomPacketPayload {

    public static final int ACTION_START = 0;
    public static final int ACTION_FIRE = 1;
    public static final int ACTION_CANCEL = 2;

    public static final Type<CastSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("dragonblockarcanedba", "cast_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CastSyncPayload> CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.entityId());
                buf.writeUtf(value.abilityId());
                buf.writeVarInt(value.action());
                buf.writeVarLong(value.tick());
            },
            buf -> new CastSyncPayload(
                    buf.readVarInt(),
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readVarLong())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
