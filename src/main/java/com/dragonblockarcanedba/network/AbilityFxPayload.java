package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C seeded effect packet (Rebuild Phase 3). One packet per
 * ability event, never one per particle: it carries the ability
 * id, the event kind (an AbilityDef.FxEvent ordinal), the origin,
 * a direction (zero when the event has no natural direction), the
 * seed, the cast power, and a repeat count for aggregated events
 * (a wave that hit four things in one tick sends one packet with
 * repeat 4, and the burst expands once per repeat with seed + r).
 * Each tracking client expands the packet locally through the
 * shared definition and the seed (client/fx/AbilityFxExpander), so
 * every observer renders the same burst and the server never
 * streams particles.
 *
 * Only burst events travel (cast_start, fire, carrier_hit,
 * carrier_end): they are moments, and their exact positions (a
 * strike point, an impact) exist only on the server. Continuous
 * events (carrier_tick, charge_tick) stay client-local, expanded
 * per frame from state the client already has synced: the carrier
 * entity itself, or the cast store.
 */
public record AbilityFxPayload(String abilityId, int event,
                               double ox, double oy, double oz,
                               float dx, float dy, float dz,
                               int seed, float power, int repeat)
        implements CustomPacketPayload {

    public static final Type<AbilityFxPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("dragonblockarcanedba", "ability_fx"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityFxPayload> CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUtf(value.abilityId());
                buf.writeVarInt(value.event());
                buf.writeDouble(value.ox());
                buf.writeDouble(value.oy());
                buf.writeDouble(value.oz());
                buf.writeFloat(value.dx());
                buf.writeFloat(value.dy());
                buf.writeFloat(value.dz());
                buf.writeInt(value.seed());
                buf.writeFloat(value.power());
                buf.writeVarInt(value.repeat());
            },
            buf -> new AbilityFxPayload(
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readInt(),
                    buf.readFloat(),
                    buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
