package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.entity.SpikeFlailSpinEntity;
import com.dragonblockarcanedba.entity.SpikeFlailSwingEntity;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.dragonblockarcanedba.util.WeaponDrainHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spike flail. One handle, one chain, one spiked ball.
 * Stamina, same family as the mace: the swing pays 100% per minute for a short swing,
 * and the spin channels 150% per minute until the crack. Cooldown starts when the channel ends.
 */
public class SpikeFlailItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
    public static final int CHANNEL_TICKS = 32;
    private static final Map<UUID, Long> LAST_SWING_TICK = new ConcurrentHashMap<>();

    public SpikeFlailItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        return ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 819.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.7, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            )
            .build();
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // The swinging ball already pays. Melee attribute damage is the close hit.
    }

    public static void performSwing(ServerPlayer player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack) || player.isUsingItem()) {
            return;
        }
        long now = player.level().getGameTime();
        UUID id = player.getUUID();
        Long last = LAST_SWING_TICK.get(id);
        if (last != null && last == now) {
            return;
        }
        LAST_SWING_TICK.put(id, now);
        if (!WeaponDrainHelper.hasStamina(player)) {
            return;
        }
        if (!WeaponDrainHelper.drainStaminaDiscrete(player, 100.0, 20)) {
            return;
        }
        player.getCooldowns().addCooldown(stack, 8);
        ServerLevel level = (ServerLevel) player.level();
        float strength = player instanceof PlayerStatsAccessor acc ? acc.dba$getStrength() : 0f;
        float damage = 720.0f + strength * 2.0f;
        level.addFreshEntity(new SpikeFlailSwingEntity(level, player, damage));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.CHAIN_RATTLE, SoundSource.PLAYERS, 0.9f, 0.8f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.SWING_HEAVY, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack) || !WeaponDrainHelper.hasStamina(player)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            beginSpin(serverPlayer);
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    public static void beginSpin(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        boolean already = !level.getEntitiesOfClass(SpikeFlailSpinEntity.class, player.getBoundingBox().inflate(3.0),
            e -> e.isAlive() && !e.releasing() && e.getOwner() == player).isEmpty();
        if (!already) {
            level.addFreshEntity(new SpikeFlailSpinEntity(level, player));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                DbaSounds.CHAIN_RATTLE, SoundSource.PLAYERS, 0.8f, 0.6f);
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingTicks) {
        if (level.isClientSide() || !(living instanceof ServerPlayer player)) {
            return;
        }
        if (!WeaponDrainHelper.drainStaminaPerTick(player, 150.0)) {
            player.stopUsingItem();
            return;
        }
        int held = getUseDuration(stack, living) - remainingTicks;
        if (held >= CHANNEL_TICKS) {
            player.stopUsingItem();
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            player.getCooldowns().addCooldown(stack, 36);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                DbaSounds.CHAIN_RATTLE, SoundSource.PLAYERS, 1.2f, 1.15f);
        }
        return true;
    }

    public static void onPlayerDisconnect(UUID playerUuid) {
        LAST_SWING_TICK.remove(playerUuid);
    }
}
