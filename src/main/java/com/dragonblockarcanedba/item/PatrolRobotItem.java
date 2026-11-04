package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.entity.DbaEntities;
import com.dragonblockarcanedba.entity.PatrolRobotEntity;
import com.dragonblockarcanedba.sound.DbaSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Places a patrol robot that follows and fights for the player who deployed it. */
public class PatrolRobotItem extends Item {
    public PatrolRobotItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        PatrolRobotEntity robot = DbaEntities.PATROL_ROBOT.create(level, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
        if (robot == null) {
            return InteractionResult.PASS;
        }
        Vec3 look = player.getLookAngle();
        robot.setPos(player.getX() + look.x * 1.5, player.getY(), player.getZ() + look.z * 1.5);
        robot.setYRot(player.getYRot());
        robot.setYHeadRot(player.getYRot());
        robot.assignOwner(player, !player.getAbilities().instabuild);
        level.addFreshEntity(robot);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, robot.getX(), robot.getY(), robot.getZ(),
                DbaSounds.REPAIR, SoundSource.NEUTRAL, 0.8f, 1.3f);
        return InteractionResult.SUCCESS;
    }
}
