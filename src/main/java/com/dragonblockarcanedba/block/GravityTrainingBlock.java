package com.dragonblockarcanedba.block;

import com.dragonblockarcanedba.block.entity.GravityTrainingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class GravityTrainingBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<GravityTrainingBlock> CODEC = simpleCodec(GravityTrainingBlock::new);

    public GravityTrainingBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, com.dragonblockarcanedba.block.entity.DbaBlockEntities.GRAVITY_TRAINING_BLOCK_ENTITY, com.dragonblockarcanedba.block.entity.GravityTrainingBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GravityTrainingBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        System.out.println("GravityTrainingBlock useWithoutItem called! isClientSide=" + level.isClientSide());
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            System.out.println("BlockEntity at pos: " + blockEntity);
            if (blockEntity instanceof GravityTrainingBlockEntity gravityEntity) {
                player.openMenu(gravityEntity);
                System.out.println("Opened menu for player");
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        System.out.println("GravityTrainingBlock useItemOn called! isClientSide=" + level.isClientSide());
        InteractionResult result = this.useWithoutItem(state, level, pos, player, hitResult);
        return result == InteractionResult.PASS ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }


}
