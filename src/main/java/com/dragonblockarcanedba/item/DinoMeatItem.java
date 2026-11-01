package com.dragonblockarcanedba.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class DinoMeatItem extends Item {
    public DinoMeatItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        ItemStack itemStack = super.finishUsingItem(stack, level, livingEntity);
        if (livingEntity instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                ItemStack bone = new ItemStack(Items.BONE);
                if (itemStack.isEmpty()) {
                    return bone;
                }
                if (!player.getInventory().add(bone) && !level.isClientSide()) {
                    // Vanilla drop() gained a Prediction parameter in 26.3; spawn the drop directly so one source serves both.
                    net.minecraft.world.entity.item.ItemEntity boneDrop = new net.minecraft.world.entity.item.ItemEntity(
                        level, player.getX(), player.getEyeY() - 0.3, player.getZ(), bone);
                    boneDrop.setDefaultPickUpDelay();
                    level.addFreshEntity(boneDrop);
                }
            }
        }
        return itemStack;
    }
}
