package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.sound.DbaSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A field capsule. Restores part of health, ki, and stamina, then goes on a short cooldown. */
public class RecoveryCapsuleItem extends Item {
    public RecoveryCapsuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.heal(8.0f);
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
            double maxKi = PlayerStats.getMaxKi(player);
            double maxStamina = PlayerStats.getMaxStamina(player);
            accessor.dba$setCurrentKi(Math.min(maxKi, accessor.dba$getCurrentKi() + maxKi * 0.4));
            accessor.dba$setCurrentStamina(Math.min(maxStamina, accessor.dba$getCurrentStamina() + maxStamina * 0.4));
            accessor.dba$syncStats();
            player.getCooldowns().addCooldown(stack, 160);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    DbaSounds.THUNK, SoundSource.PLAYERS, 0.7f, 1.4f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    DbaSounds.FIZZLE, SoundSource.PLAYERS, 0.6f, 1.2f);
        }
        return InteractionResult.SUCCESS;
    }
}
