package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SenzuBeanItem extends Item {
    public SenzuBeanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (!player.getCooldowns().isOnCooldown(itemStack)) {
            if (!level.isClientSide()) {
                // Restore Health
                player.setHealth(player.getMaxHealth());
                
                // Restore Ki and Stamina
                PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
                accessor.dba$setCurrentKi(PlayerStats.getMaxKi(player));
                accessor.dba$setCurrentStamina(PlayerStats.getMaxStamina(player));
                
                // Apply 45 second cooldown (45 * 20 = 900 ticks)
                player.getCooldowns().addCooldown(itemStack, 900);
                
                // Consume the item if not in creative
                if (!player.getAbilities().instabuild) {
                    itemStack.shrink(1);
                }

                // Sound feedback
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_BURP, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.2f);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.5f);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 1.6f);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
