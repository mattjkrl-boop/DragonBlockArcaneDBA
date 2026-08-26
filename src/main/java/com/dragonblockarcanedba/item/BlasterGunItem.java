package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.BlasterBoltEntity;
import com.dragonblockarcanedba.entity.ErasureCannonBeamEntity;
import com.dragonblockarcanedba.entity.ErasureChargeOrbEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blaster Gun — Energy Firearm.
 * Real ammunition economy, rapid energy projectiles, heat buildup, charged shots, and devastating beam fire.
 */
public class BlasterGunItem extends Item {
    public static final int MAX_RIGHT_CHARGE_TICKS = 100; // 5 seconds
    public static final Map<UUID, ErasureChargeOrbEntity> ACTIVE_CHARGE_MAP = new ConcurrentHashMap<>();

    public BlasterGunItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    449.0, // 1 + 449 = 450 base damage
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -2.0,
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Target Acquisition: Tactical Recon Scope nameplate spotting range
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.NAME_PLATE_DIST_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("blaster_recon_nameplate"), 64.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.MINI_NAME_PLATE_DIST_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("blaster_recon_mini_nameplate"), 30.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    // --- Ammo & Heat Utilities ---

    public static int getAmmoCount(Player player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof BlasterAmmoItem) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static boolean consumeAmmo(Player player, int amount) {
        if (player.isCreative()) return true;
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof BlasterAmmoItem) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
                if (remaining <= 0) break;
            }
        }
        return remaining <= 0;
    }

    public static int getHeat(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag().getIntOr("Heat", 0);
        }
        return 0;
    }

    public static void setHeat(ItemStack stack, int heat) {
        heat = Math.max(0, Math.min(100, heat));
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putInt("Heat", heat);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // --- LEFT CLICK: Blaster Barrage (Continuous Rapid Laser Fire) ---

    public static void onLeftClickBarrageTick(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Fire every 3 ticks
        if (chargeTicks % 3 != 0) return;

        int currentAmmo = getAmmoCount(player);
        int currentHeat = getHeat(stack);
        boolean isOvercharged = (currentHeat >= 100);

        int ammoNeeded = isOvercharged ? 3 : 1;
        if (currentAmmo < ammoNeeded && !player.isCreative()) {
            player.sendSystemMessage(Component.literal("§cOut of Blaster Ammo!"), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0f, 1.8f);
            return;
        }

        consumeAmmo(player, ammoNeeded);

        float spirit = accessor.dba$getSpirit();
        float heatRatio = currentHeat / 100.0f;
        float baseDmg = isOvercharged
            ? (700.0f + spirit * 3.0f)
            : (250.0f + (spirit * 1.5f) * (1.0f + heatRatio * 0.5f));

        Vec3 look = player.getLookAngle();
        double speed = 2.4;

        BlasterBoltEntity bolt = new BlasterBoltEntity(level, player, baseDmg, isOvercharged, heatRatio);
        bolt.setDeltaMovement(look.x * speed, look.y * speed, look.z * speed);
        level.addFreshEntity(bolt);

        if (isOvercharged) {
            setHeat(stack, 0); // Reset heat after overcharged blast
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5f, 1.2f);
        } else {
            setHeat(stack, currentHeat + 8); // Build heat
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.6f + heatRatio * 0.4f);
        }

        // Actionbar HUD update
        player.sendSystemMessage(
            Component.literal(String.format("§eAmmo: §f%d §8| §cHeat: §f%d%%", getAmmoCount(player), getHeat(stack))),
            true
        );
    }

    // --- RIGHT CLICK: Erasure Cannon ---

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int ammo = getAmmoCount(player);
        if (ammo < 10 && !player.isCreative()) {
            player.sendSystemMessage(Component.literal("§cNeed at least 10 Blaster Ammo for Erasure Cannon!"));
            return InteractionResult.PASS;
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

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingTicks) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            ServerLevel serverLevel = (ServerLevel) level;
            int heldTicks = getUseDuration(stack, living) - remainingTicks;
            float chargeRatio = Math.min(1.0f, heldTicks / (float) MAX_RIGHT_CHARGE_TICKS);

            // Energy Overcharge: recoil stabilization & aiming focus
            player.addEffect(new MobEffectInstance(DbaEffects.ENERGY_OVERCHARGE_HOLDER, 10, 0, false, false));

            // Physical 3D geometric expanding energy orb at the muzzle
            ErasureChargeOrbEntity chargeEntity = ACTIVE_CHARGE_MAP.get(player.getUUID());
            if (chargeEntity == null || !chargeEntity.isAlive()) {
                chargeEntity = new ErasureChargeOrbEntity(serverLevel, player);
                serverLevel.addFreshEntity(chargeEntity);
                ACTIVE_CHARGE_MAP.put(player.getUUID(), chargeEntity);
            }
            chargeEntity.setChargeRatio(chargeRatio);

            if (heldTicks % 20 == 0) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.6f + chargeRatio * 0.8f);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            ServerLevel serverLevel = (ServerLevel) level;
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

            // Discard active muzzle charge orb
            ErasureChargeOrbEntity chargeEntity = ACTIVE_CHARGE_MAP.remove(player.getUUID());
            if (chargeEntity != null && chargeEntity.isAlive()) {
                chargeEntity.discard();
            }

            int heldTicks = getUseDuration(stack, living) - timeLeft;
            float chargeRatio = Math.max(0.1f, Math.min(1.0f, heldTicks / (float) MAX_RIGHT_CHARGE_TICKS));

            // Ammo cost: 10 base, up to 20 at max charge
            int ammoCost = 10 + (int) (chargeRatio * 10);
            int available = getAmmoCount(player);
            if (available < ammoCost && !player.isCreative()) {
                ammoCost = available; // consume remaining
            }
            consumeAmmo(player, ammoCost);

            // Erasure Cannon straight 48-meter piercing beam
            Vec3 start = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            double maxBeamLength = 48.0;
            Vec3 end = start.add(look.scale(maxBeamLength));

            // Raycast against solid blocks to find physical beam contact point
            BlockHitResult blockHit = serverLevel.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
            ));
            float beamLength = (float) maxBeamLength;
            if (blockHit.getType() != HitResult.Type.MISS) {
                beamLength = (float) start.distanceTo(blockHit.getLocation());
            }

            float baseDamage = 900.0f + (chargeRatio * 1400.0f);
            float spiritBonus = accessor.dba$getSpirit() * 4.0f;
            float totalDamage = baseDamage + spiritBonus;

            // Spawn physical 3D Erasure Cannon beam entity
            ErasureCannonBeamEntity beam = new ErasureCannonBeamEntity(serverLevel, player, totalDamage, chargeRatio, beamLength);
            serverLevel.addFreshEntity(beam);

            player.getCooldowns().addCooldown(stack, 120); // 6-second cooldown
        }
        return true;
    }
}
