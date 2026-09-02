package com.dragonblockarcanedba.network;

import com.dragonblockarcanedba.config.RaceConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public record RaceConfigsSyncPayload(Map<String, RaceConfig> configs) implements CustomPacketPayload {

    public static final Type<RaceConfigsSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "race_configs_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RaceConfigsSyncPayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeInt(payload.configs().size());
            payload.configs().forEach((id, config) -> {
                buf.writeUtf(id);
                buf.writeFloat(config.hitboxWidth());
                buf.writeFloat(config.hitboxHeight());
                buf.writeFloat(config.eyeHeight());
                buf.writeFloat(config.eyeOffsetX());
                buf.writeFloat(config.eyeOffsetY());
                buf.writeFloat(config.eyeOffsetZ());
            });
        },
        buf -> {
            int size = buf.readInt();
            Map<String, RaceConfig> map = new HashMap<>();
            for (int i = 0; i < size; i++) {
                String id = buf.readUtf();
                float width = buf.readFloat();
                float height = buf.readFloat();
                float eye = buf.readFloat();
                float ox = buf.readFloat();
                float oy = buf.readFloat();
                float oz = buf.readFloat();
                map.put(id, new RaceConfig(width, height, eye, ox, oy, oz));
            }
            return new RaceConfigsSyncPayload(map);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
