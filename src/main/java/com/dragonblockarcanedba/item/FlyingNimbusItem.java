package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.entity.DbaEntities;
import com.dragonblockarcanedba.entity.FlyingNimbusEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FlyingNimbusItem extends Item {
    public FlyingNimbusItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (!level.isClientSide() && player != null) {
            Vec3 clickPos = context.getClickLocation();
            FlyingNimbusEntity nimbus = DbaEntities.FLYING_NIMBUS.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (nimbus != null) {
                nimbus.setPos(clickPos.x, clickPos.y + 0.1, clickPos.z);
                nimbus.setYRot(player.getYRot());
                level.addFreshEntity(nimbus);
                
                player.startRiding(nimbus);
                
                if (!player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.WOOL_PLACE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.4f);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.ELYTRA_FLYING, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.5f);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            FlyingNimbusEntity nimbus = DbaEntities.FLYING_NIMBUS.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (nimbus != null) {
                Vec3 look = player.getLookAngle();
                nimbus.setPos(player.getX() + look.x * 0.5, player.getY() + 0.1, player.getZ() + look.z * 0.5);
                nimbus.setYRot(player.getYRot());
                level.addFreshEntity(nimbus);
                
                player.startRiding(nimbus);
                
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.WOOL_PLACE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.4f);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.ELYTRA_FLYING, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.5f);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
