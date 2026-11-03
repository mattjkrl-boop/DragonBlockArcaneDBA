package com.dragonblockarcanedba.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Tells clients a seven-ball ritual has started, so they can move the balls
 * into the ring while the server waits to summon the dragon.
 */
public record WishRitualStartPayload(boolean namek, long gameTime, BlockPos[] balls) implements CustomPacketPayload {
    public static final Type<WishRitualStartPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "wish_ritual_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WishRitualStartPayload> CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeBoolean(value.namek());
                buf.writeLong(value.gameTime());
                for (int i = 0; i < 7; i++) {
                    buf.writeBlockPos(value.balls()[i]);
                }
            },
            buf -> {
                boolean namek = buf.readBoolean();
                long gameTime = buf.readLong();
                BlockPos[] balls = new BlockPos[7];
                for (int i = 0; i < 7; i++) {
                    balls[i] = buf.readBlockPos();
                }
                return new WishRitualStartPayload(namek, gameTime, balls);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
