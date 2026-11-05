package com.dragonblockarcanedba.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C payload carrying the full ability definition set as raw JSON
 * (the identical text the server loaded from the datapack). Clients
 * re-parse each entry with the shared AbilityDef parser into the
 * shared AbilityRegistry, so bindings, carrier parameters, and
 * visual parameters on the client are the server's own data, never a
 * client-side copy that could drift. Sent on datapack sync (join
 * and /reload).
 */
public record AbilitySyncPayload(List<Entry> defs) implements CustomPacketPayload {

    public record Entry(String id, String json) {}

    public static final CustomPacketPayload.Type<AbilitySyncPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "ability_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilitySyncPayload> CODEC =
            StreamCodec.of(AbilitySyncPayload::write, AbilitySyncPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, AbilitySyncPayload payload) {
        buf.writeVarInt(payload.defs.size());
        for (Entry entry : payload.defs) {
            buf.writeUtf(entry.id());
            buf.writeUtf(entry.json());
        }
    }

    private static AbilitySyncPayload read(RegistryFriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), 4096);
        List<Entry> defs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            defs.add(new Entry(buf.readUtf(), buf.readUtf()));
        }
        return new AbilitySyncPayload(defs);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
