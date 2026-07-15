package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.network.SpacePodOpenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The Space Pod item. When right-clicked, sends a packet to the client
 * telling it to open the destination picker GUI.
 */
public class SpacePodItem extends Item {
    public SpacePodItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // Send S2C packet telling the client to open the destination screen
            ServerPlayNetworking.send(serverPlayer, new SpacePodOpenPayload());
        }
        return InteractionResult.SUCCESS;
    }
}
