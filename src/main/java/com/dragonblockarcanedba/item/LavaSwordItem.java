package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.entity.LavaCleaveEntity;
import com.dragonblockarcanedba.entity.LavaEruptionEntity;
import com.dragonblockarcanedba.entity.LavaSwordGround;
import com.dragonblockarcanedba.particle.DbaParticles;
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
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lava sword. Ki, matching Grand Sword's 90% per minute and Spirit Sword's discrete swing.
 * The eruption cooldown is Grand Sword's finisher length. The cleave cooldown only
 * swallows the swing packet that follows the same press.
 */
public class LavaSwordItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
    private static final double KI_PERCENT = 90.0;
    private static final Map<UUID, Long> LAST_CLEAVE_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_ERUPT_TICK = new ConcurrentHashMap<>();

    static {
        // Item registration loads this class at startup, so this is where the
        // ground burn's restore ticker hooks in: crash recovery then runs from
        // the first server tick instead of waiting for the first swing.
        LavaSwordGround.register();
    }

    public LavaSwordItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        return ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 859.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.1, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            )
            .build();
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // The blade is living lava, so a plain hit burns too: the same 60
        // ticks the cleave applies, plus a spit of flame off the target. The
        // abilities still pay their own way on top of the melee hit.
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 60));
        if (target.level() instanceof ServerLevel server) {
            server.sendParticles(DbaParticles.FLAME,
                target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                6, 0.25, 0.3, 0.25, 0.02);
        }
    }

    public static void performCleave(ServerPlayer player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        long now = player.level().getGameTime();
        UUID id = player.getUUID();
        Long last = LAST_CLEAVE_TICK.get(id);
        if (last != null && last == now) {
            return;
        }
        LAST_CLEAVE_TICK.put(id, now);
        if (!WeaponDrainHelper.drainKiDiscrete(player, KI_PERCENT, 14)) {
            return;
        }
        player.getCooldowns().addCooldown(stack, 8);
        ServerLevel level = (ServerLevel) player.level();
        float strength = player instanceof PlayerStatsAccessor acc ? acc.dba$getStrength() : 0f;
        float damage = 740.0f + strength * 2.0f;
        Vec3 look = player.getLookAngle();
        LavaCleaveEntity cleave = new LavaCleaveEntity(level, player, damage);
        cleave.setDeltaMovement(look.scale(1.35));
        level.addFreshEntity(cleave);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.FIRE_SHOT, SoundSource.PLAYERS, 0.85f, 0.7f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.SLASH_WHOOSH, SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack) || !WeaponDrainHelper.hasKi(player)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            performEruption(serverPlayer, stack);
        }
        return InteractionResult.CONSUME;
    }

    public static void performEruption(ServerPlayer player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        long now = player.level().getGameTime();
        UUID id = player.getUUID();
        Long last = LAST_ERUPT_TICK.get(id);
        if (last != null && last == now) {
            return;
        }
        LAST_ERUPT_TICK.put(id, now);
        if (!WeaponDrainHelper.drainKiDiscrete(player, KI_PERCENT, 40)) {
            return;
        }
        player.getCooldowns().addCooldown(stack, 40);
        ServerLevel level = (ServerLevel) player.level();
        float strength = player instanceof PlayerStatsAccessor acc ? acc.dba$getStrength() : 0f;
        float damage = 880.0f + strength * 2.2f;
        level.addFreshEntity(new LavaEruptionEntity(level, player, damage));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.LAVA_BUBBLE, SoundSource.PLAYERS, 1.1f, 0.6f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.FIRE_SHOT, SoundSource.PLAYERS, 1.0f, 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.FIRE_IGNITE, SoundSource.PLAYERS, 0.9f, 0.8f);
    }

    public static void onPlayerDisconnect(UUID playerUuid) {
        LAST_CLEAVE_TICK.remove(playerUuid);
        LAST_ERUPT_TICK.remove(playerUuid);
    }
}
