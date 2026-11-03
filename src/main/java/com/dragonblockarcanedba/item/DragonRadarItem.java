package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.block.DbaBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Points at the nearest placed or dropped Dragon Ball inside a short search box. */
public class DragonRadarItem extends Item {
    private static final int RADIUS = 32;
    private static final int VERTICAL = 24;

    public DragonRadarItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            Ping ping = scan(level, player);
            if (!ping.found) {
                player.sendOverlayMessage(Component.translatable("item.dragonblockarcanedba.dragon_radar.none"));
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.6f, 0.6f);
            } else {
                double dx = ping.x - player.getX();
                double dz = ping.z - player.getZ();
                int blocks = (int) Math.sqrt(ping.distSq);
                Component direction = Component.translatable("item.dragonblockarcanedba.dragon_radar.dir." + compass(dx, dz));
                player.sendOverlayMessage(Component.translatable("item.dragonblockarcanedba.dragon_radar.found", direction, blocks));
                float pitch = blocks < 8 ? 1.6f : blocks < 16 ? 1.2f : 0.8f;
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.8f, pitch);
            }
            player.getCooldowns().addCooldown(stack, 30);
        }
        return InteractionResult.SUCCESS;
    }

    private static Ping scan(Level level, Player player) {
        Ping ping = new Ping();
        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minY = Math.max(level.getMinY(), origin.getY() - VERTICAL);
        int maxY = Math.min(level.getMaxY(), origin.getY() + VERTICAL);
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int y = minY; y <= maxY; y++) {
                    cursor.set(origin.getX() + dx, y, origin.getZ() + dz);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(DbaBlocks.EARTH_DRAGON_BALL) || state.is(DbaBlocks.NAMEKIAN_DRAGON_BALL)) {
                        ping.consider(cursor.getX() + 0.5, cursor.getY() + 0.5, cursor.getZ() + 0.5, player);
                    }
                }
            }
        }
        AABB area = new AABB(
                player.getX() - RADIUS, minY, player.getZ() - RADIUS,
                player.getX() + RADIUS, maxY, player.getZ() + RADIUS);
        List<ItemEntity> dropped = level.getEntitiesOfClass(ItemEntity.class, area);
        for (ItemEntity entity : dropped) {
            Item carried = entity.getItem().getItem();
            if (carried == DbaItems.EARTH_DRAGON_BALL || carried == DbaItems.NAMEKIAN_DRAGON_BALL) {
                ping.consider(entity.getX(), entity.getY(), entity.getZ(), player);
            }
        }
        return ping;
    }

    private static String compass(double dx, double dz) {
        double ax = Math.abs(dx);
        double az = Math.abs(dz);
        if (ax < 0.5 && az < 0.5) {
            return "here";
        }
        if (ax > az * 2.0) {
            return dx > 0 ? "east" : "west";
        }
        if (az > ax * 2.0) {
            return dz > 0 ? "south" : "north";
        }
        if (dz > 0) {
            return dx > 0 ? "southeast" : "southwest";
        }
        return dx > 0 ? "northeast" : "northwest";
    }

    private static final class Ping {
        double distSq = Double.MAX_VALUE;
        double x;
        double z;
        boolean found;

        void consider(double x, double y, double z, Player player) {
            double dx = x - player.getX();
            double dy = y - player.getY();
            double dz = z - player.getZ();
            double dist = dx * dx + dy * dy + dz * dz;
            if (dist < this.distSq) {
                this.distSq = dist;
                this.x = x;
                this.z = z;
                this.found = true;
            }
        }
    }
}
