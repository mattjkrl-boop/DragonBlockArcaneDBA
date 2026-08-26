package com.dragonblockarcanedba.block;

import com.dragonblockarcanedba.entity.DbaEntities;
import com.dragonblockarcanedba.entity.ShenronEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EarthDragonBallBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);

    public EarthDragonBallBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        checkForSummon(level, pos);
    }

    private void checkForSummon(Level level, BlockPos pos) {
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
            
            // Spawn Shenron at pos (exactly on the ground)
            if (level instanceof ServerLevel serverLevel) {
                BlockPos spawnPos = pos;
                ShenronEntity shenron = DbaEntities.SHENRON.create(serverLevel, null, spawnPos, EntitySpawnReason.EVENT, false, false);
                if (shenron != null) {
                    // Face the nearest player
                    Player closestPlayer = serverLevel.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 64.0, false);
                    if (closestPlayer != null) {
                        double dX = closestPlayer.getX() - shenron.getX();
                        double dZ = closestPlayer.getZ() - shenron.getZ();
                        float yaw = (float) (Math.atan2(dZ, dX) * (180 / Math.PI)) - 90.0F;
                        shenron.setYRot(yaw);
                        shenron.yRotO = yaw;
                        shenron.yBodyRot = yaw;
                        shenron.yHeadRot = yaw;
                    }

                    serverLevel.addFreshEntity(shenron);
                    
                    // Summon lightning
                    LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.EVENT);
                    if (lightning != null) {
                        lightning.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                        serverLevel.addFreshEntity(lightning);
                    }
                    
                    // Set weather to thunderstorm
                    serverLevel.getServer().setWeatherParameters(0, 12000, true, true);
                    
                    // Dragon roar sound
                    serverLevel.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                        net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL, net.minecraft.sounds.SoundSource.HOSTILE, 2.5f, 0.8f);

                    // Broadcast message
                    serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                        net.minecraft.network.chat.Component.literal("\u00a76Shenron: \u00a7eI am the Eternal Dragon Shenron. Speak your wish!"), false
                    );
                }
            }
        }
    }
}
