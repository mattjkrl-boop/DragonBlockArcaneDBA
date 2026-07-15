package com.dragonblockarcanedba.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class OtherworldGuideEntity extends PathfinderMob {
    
    public OtherworldGuideEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        // No AI - sits still
    }

    @Override
    public boolean isNoAi() {
        return true;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new com.dragonblockarcanedba.network.ReviveUiOpenPayload());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }
}
