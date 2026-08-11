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
                if (!player.getInventory().add(bone)) {
                    player.drop(bone, false);
                }
            }
        }
        return itemStack;
    }
}
