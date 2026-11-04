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
public class BlasterGunItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
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
                com.dragonblockarcanedba.sound.DbaSounds.FIZZLE, SoundSource.PLAYERS, 1.0f, 1.8f);
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

        // Muzzle flash: a small spark snap at the barrel on every shot so the
        // barrage has a visible source, not just bolts appearing mid-air.
        Vec3 muzzle = player.getEyePosition().add(look.scale(0.7));
        level.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
            muzzle.x, muzzle.y, muzzle.z, 2, 0.08, 0.08, 0.08, 0.03);

        if (isOvercharged) {
            setHeat(stack, 0); // Reset heat after overcharged blast
            // Heat finally has a downside: venting. The barrage pauses for a second
            // after an overcharged shot (the receiver honors the item cooldown),
            // instead of heat being a pure buff that converts at 100 for free.
            player.getCooldowns().addCooldown(stack, 20);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.RELEASE_BLAST, SoundSource.PLAYERS, 1.5f, 1.2f);
        com.dragonblockarcanedba.fx.DbaImpactFx.heavy(level, player.getX(), player.getY(), player.getZ());
        } else {
            setHeat(stack, currentHeat + 8); // Build heat
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.FIRE_SHOT, SoundSource.PLAYERS, 1.0f, 1.6f + heatRatio * 0.4f);
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
            // Matches the rest of the mod: a failed cost gate returns FAIL and
            // reports on the action bar, not a chat message with PASS. The
            // action-bar overload only exists on ServerPlayer, hence the cast.
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal("§cNeed at least 10 Blaster Ammo for Erasure Cannon!"), true);
            }
            return InteractionResult.FAIL;
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
                    com.dragonblockarcanedba.sound.DbaSounds.BEAM_CHARGE, SoundSource.PLAYERS, 1.0f, 0.6f + chargeRatio * 0.8f);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.ABILITY_CHARGE, SoundSource.PLAYERS, 0.8f, 0.6f + chargeRatio * 0.9f);
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
            float chargeRatio = Math.min(1.0f, heldTicks / (float) MAX_RIGHT_CHARGE_TICKS);

            // Low charge fizzle (spectacle pass): the old 0.1 charge floor
            // meant an accidental click still fired an 11 ammo beam worth
            // over a thousand damage. Below 20 percent charge the shot now
            // fizzles at the muzzle: no beam, no ammo spent, no cooldown.
            if (chargeRatio < 0.2f) {
                Vec3 fizzlePos = player.getEyePosition().add(player.getLookAngle().scale(0.8));
                serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                    fizzlePos.x, fizzlePos.y, fizzlePos.z, 6, 0.20, 0.20, 0.20, 0.05);
                player.sendSystemMessage(Component.literal("§cErasure Cannon fizzled: hold the charge longer!"), true);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.FIZZLE, SoundSource.PLAYERS, 1.0f, 1.2f);
                return true;
            }

            // Ammo cost: 10 base, up to 20 at max charge. The full cost is required:
            // the shot used to fire at full damage while paying only whatever ammo
            // happened to remain.
            int ammoCost = 10 + (int) (chargeRatio * 10);
            int available = getAmmoCount(player);
            if (available < ammoCost && !player.isCreative()) {
                player.sendSystemMessage(Component.literal("§cNot enough Blaster Ammo for that charge level!"), true);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.FIZZLE, SoundSource.PLAYERS, 1.0f, 1.8f);
                return true;
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

            // Rebased so charge pays monotonically past the standard's 2x
            // rule: the weakest real shot (0.2 charge) is 1100 plus Spirit,
            // full charge stays 2300 plus Spirit, and the beam entity now
            // delivers the total as 6 pulses across its burn instead of one
            // tick 1 hit.
            float baseDamage = 800.0f + (chargeRatio * 1500.0f);
            float spiritBonus = accessor.dba$getSpirit() * 4.0f;
            float totalDamage = baseDamage + spiritBonus;

            // Spawn physical 3D Erasure Cannon beam entity
            ErasureCannonBeamEntity beam = new ErasureCannonBeamEntity(serverLevel, player, totalDamage, chargeRatio, beamLength);
            serverLevel.addFreshEntity(beam);

            // The biggest single hit in the group lands its shake and hitstop at the
            // impact point, not silently at the shooter.
            Vec3 impactPoint = start.add(look.scale(beamLength));
            com.dragonblockarcanedba.fx.DbaImpactFx.heavy(serverLevel, impactPoint.x, impactPoint.y, impactPoint.z);

            player.getCooldowns().addCooldown(stack, 120); // 6-second cooldown
        }
        return true;
    }

    /** Discards any charge or stance entities still tracked for a player who logged out. */
    public static void onPlayerDisconnect(UUID playerUuid) {
        net.minecraft.world.entity.Entity e0 = ACTIVE_CHARGE_MAP.remove(playerUuid);
        if (e0 != null && e0.isAlive()) e0.discard();
    }
}
