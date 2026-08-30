package com.dragonblockarcanedba.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class NamekianDragonBallBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    public NamekianDragonBallBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        checkForSummon(level, pos);
    }

    private void checkForSummon(net.minecraft.world.level.Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        
        java.util.List<BlockPos> ballPositions = new java.util.ArrayList<>();
        int radius = 5;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    if (level.getBlockState(p).is(this)) {
                        ballPositions.add(p);
                    }
                }
            }
        }
        
        if (ballPositions.size() >= 7) {
            // Remove the 7 blocks
            for (int i = 0; i < 7; i++) {
                level.destroyBlock(ballPositions.get(i), false);
            }
            
            // Spawn Porunga
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                com.dragonblockarcanedba.entity.PorungaEntity porunga = com.dragonblockarcanedba.entity.DbaEntities.PORUNGA.create(serverLevel, null, pos, net.minecraft.world.entity.EntitySpawnReason.EVENT, false, false);
                if (porunga != null) {
                    net.minecraft.world.entity.player.Player closestPlayer = serverLevel.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 64.0, false);
                    if (closestPlayer != null) {
                        double dX = closestPlayer.getX() - porunga.getX();
                        double dZ = closestPlayer.getZ() - porunga.getZ();
                        float yaw = (float) (Math.atan2(dZ, dX) * (180 / Math.PI)) - 90.0F;
                        porunga.setYRot(yaw);
                        porunga.yRotO = yaw;
                        porunga.yBodyRot = yaw;
                        porunga.yHeadRot = yaw;
                    }

                    serverLevel.addFreshEntity(porunga);
                    
                    net.minecraft.world.entity.LightningBolt lightning = net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.EVENT);
                    if (lightning != null) {
                        lightning.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                        serverLevel.addFreshEntity(lightning);
                    }
                    
                    serverLevel.getServer().setWeatherParameters(0, 12000, true, true);
                    
                    serverLevel.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                        net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL, net.minecraft.sounds.SoundSource.HOSTILE, 2.5f, 0.5f);

                    serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                        net.minecraft.network.chat.Component.literal("\u00a76Porunga: \u00a7eI am the Dragon of Dreams. Speak your 3 wishes!"), false
                    );
                }
            }
        }
    }
}
