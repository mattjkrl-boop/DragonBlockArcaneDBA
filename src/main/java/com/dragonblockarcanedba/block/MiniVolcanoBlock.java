package com.dragonblockarcanedba.block;

import com.dragonblockarcanedba.block.entity.VolcanoBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Lava Sword redesign's volcano ultimate: a temporary cone of living
 * rock planted by the sword, driven by VolcanoBlockEntity. It is not
 * obtainable and not breakable (bedrock-grade properties in DbaBlocks);
 * it exists only while LavaField holds a site entry for its cell, and the
 * field restores the cell to its original block when the volcano ends,
 * on schedule, on shutdown, or on crash adoption. The block is dumb: all
 * eruption behavior lives in the block entity, all world-change safety
 * lives in LavaField.
 */
public class MiniVolcanoBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<MiniVolcanoBlock> CODEC =
        com.dragonblockarcanedba.util.DbaCompat.blockCodec(MiniVolcanoBlock::new);

    public MiniVolcanoBlock(Properties properties) {
        super(properties);
    }

    // No @Override: vanilla removed BlockBehaviour.codec() in 26.3; this still overrides on 26.2.
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, com.dragonblockarcanedba.block.entity.DbaBlockEntities.VOLCANO_BLOCK_ENTITY,
            VolcanoBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VolcanoBlockEntity(pos, state);
    }
}
