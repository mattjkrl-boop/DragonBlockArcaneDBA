package com.dragonblockarcanedba.block;

import com.dragonblockarcanedba.item.DbaItems;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class SenzuPlantBlock extends CropBlock {
    public SenzuPlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return DbaItems.SENZU_SPROUT;
    }
}
