package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.BlasterBoltEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Blaster Gun — Energy Firearm.
 * Real ammunition economy, rapid energy projectiles, heat buildup, charged shots, and devastating beam fire.
 */
public class BlasterGunItem extends Item {
    public static final int MAX_RIGHT_CHARGE_TICKS = 100; // 5 seconds

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

            // Expanding energy sphere particles around the muzzle
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 muzzle = eye.add(look.scale(1.2));

            int count = 2 + (int) (chargeRatio * 10);
            for (int i = 0; i < count; i++) {
                double ox = (serverLevel.getRandom().nextDouble() - 0.5) * (0.4 + chargeRatio * 0.8);
                double oy = (serverLevel.getRandom().nextDouble() - 0.5) * (0.4 + chargeRatio * 0.8);
                double oz = (serverLevel.getRandom().nextDouble() - 0.5) * (0.4 + chargeRatio * 0.8);

                serverLevel.sendParticles(
                    new DustParticleOptions(0x00FFFF, 1.5f + chargeRatio * 1.0f),
                    muzzle.x + ox, muzzle.y + oy, muzzle.z + oz,
                    1, 0, 0, 0, 0
                );
            }

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
            double beamLength = 48.0;
            Vec3 end = start.add(look.scale(beamLength));

            float baseDamage = 900.0f + (chargeRatio * 1400.0f);
            float spiritBonus = accessor.dba$getSpirit() * 4.0f;
            float totalDamage = baseDamage + spiritBonus;

            // Hitbox along the beam
            AABB beamBox = new AABB(start, end).inflate(1.5 + chargeRatio * 1.5);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, beamBox,
                e -> e.isAlive() && e != player
            );

            for (LivingEntity t : targets) {
                t.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 25, 0, false, false, false), player);
                t.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), totalDamage);

                // MC 26.2 Physics: Concussive beam impulse
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    t,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("blaster_beam_bounce"),
                    0.85,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                );
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    t,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("blaster_beam_drag"),
                    -0.40,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );

                // Tweak B: Gravitational suction pulling enemies into centerline
                Vec3 toCenter = start.add(look.scale(look.dot(t.position().subtract(start)))).subtract(t.position()).normalize().scale(0.8);
                t.setDeltaMovement(t.getDeltaMovement().add(toCenter.x, 0.2, toCenter.z));
                t.hurtMarked = true;
            }

            // Beam line particles (Bright Cyan & Pure White core)
            for (double d = 0; d <= beamLength; d += 0.6) {
                Vec3 p = start.add(look.scale(d));
                serverLevel.sendParticles(
                    new DustParticleOptions(0x00FFFF, 2.5f + chargeRatio * 1.5f),
                    p.x, p.y, p.z,
                    1, 0, 0, 0, 0
                );
                serverLevel.sendParticles(
                    new DustParticleOptions(0xFFFFFF, 1.8f),
                    p.x, p.y, p.z,
                    1, 0, 0, 0, 0
                );
            }

            // Tweak C: Max charge endpoint explosion
            if (chargeRatio >= 0.9f) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, end.x, end.y, end.z, 3, 0.5, 0.5, 0.5, 0);
                AABB endAoe = new AABB(end.subtract(4, 4, 4), end.add(4, 4, 4));
                List<LivingEntity> endTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, endAoe, e -> e.isAlive() && e != player);
                for (LivingEntity et : endTargets) {
                    et.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), totalDamage * 0.5f);
                }
            }

            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.2f, 0.6f);

            player.getCooldowns().addCooldown(stack, 120); // 6-second cooldown
        }
        return true;
    }
}
