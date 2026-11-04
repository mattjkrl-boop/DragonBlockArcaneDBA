package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.BraveChargeEntity;
import com.dragonblockarcanedba.entity.BraveCrossSlashEntity;
import com.dragonblockarcanedba.entity.BraveRushTrailEntity;
import com.dragonblockarcanedba.entity.BraveSlashEntity;
import com.dragonblockarcanedba.movement.MovementPhysics;
import net.minecraft.core.component.DataComponents;
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

import com.dragonblockarcanedba.util.WeaponDrainHelper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brave Sword - Heroic Legendary Sword (Tapion's Sword).
 * 
 * Drain: Left click Ki 25% + Stamina 20% per minute, Right click Ki 15% per minute.
 */
public class BraveSwordItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
    public static final int MAX_RIGHT_CHARGE_TICKS = 100; // 5 seconds

    // Track active charging BraveChargeEntity per player
    public static final Map<UUID, BraveChargeEntity> ACTIVE_CHARGE_MAP = new ConcurrentHashMap<>();

    public BraveSwordItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    799.0, // 1 + 799 = 800 base damage
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -1.8, // Fluid hero strikes
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Physics: Heroic Agility and Low-Drag Glide
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("brave_hero_drag"), -0.50, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Assault strikes and the heroic dash already pay. A vanilla connect
        // used to spend ki and stamina again with no swing of its own.
    }

    // --- Combo Tracking ---

    public static int getCombo(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag().getIntOr("Combo", 0);
        }
        return 0;
    }

    public static long getComboTime(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag().getLongOr("ComboTime", 0L);
        }
        return 0L;
    }

    public static void setComboTime(ItemStack stack, long gameTime) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putLong("ComboTime", gameTime);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void setCombo(ItemStack stack, int combo) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putInt("Combo", combo);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // --- LEFT CLICK: Brave Sword Assault (Combo & Finisher) ---

    public static void onLeftClickAssaultTick(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        int combo = getCombo(stack);
        // Combo decays: 5 seconds without a landed hit and the banked combo is gone,
        // so a player cannot walk into a new fight one hit from a finisher.
        if (combo > 0 && level.getGameTime() - getComboTime(stack) > 100) {
            combo = 0;
            setCombo(stack, 0);
        }
        // Attack rate scales with combo (Tweak A: higher combo = faster strikes)
        int interval = Math.max(3, 8 - (combo / 2));
        if (chargeTicks % interval != 0) return;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 strikeTarget = eye.add(look.scale(3.5));

        AABB hitBox = new AABB(eye, strikeTarget).inflate(1.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(
            LivingEntity.class, hitBox,
            e -> e.isAlive() && e != player
        );

        if (!targets.isEmpty()) {
            // Drain Ki: 25% per min + Stamina: 20% per min (smooth) on a landed combo step.
            if (!WeaponDrainHelper.drainBothPerTick(player, 25.0, 20.0)) {
                return;
            }
            LivingEntity target = targets.get(0);
            combo++;
            setCombo(stack, combo);
            setComboTime(stack, level.getGameTime());

            float strength = accessor.dba$getStrength();

            if (combo >= 10) {
                // Brave Finisher (Tweak C): Dash forward & unleash massive physical 3D cross-slash
                Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
                Vec3 past = target.position().add(look.scale(2.0));
                // Never teleport the player into a wall: if the past-the-target
                // spot is solid at feet or head height, land on the target instead.
                net.minecraft.core.BlockPos pastPos = net.minecraft.core.BlockPos.containing(past.x, past.y, past.z);
                boolean feetBlocked = !level.getBlockState(pastPos).getCollisionShape(level, pastPos).isEmpty();
                boolean headBlocked = !level.getBlockState(pastPos.above()).getCollisionShape(level, pastPos.above()).isEmpty();
                if (feetBlocked || headBlocked) {
                    past = target.position();
                }
                MovementPhysics.teleport(player, past);

                float finisherDmg = 900.0f + (strength * 4.0f);
                target.hurtServer(level, level.damageSources().playerAttack(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (finisherDmg)));

                // MC 26.2 Physics: Finisher ricochet bounce
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    target,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("brave_finisher_bounce"),
                    0.85,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                );

                // Spawn physical 3D volumetric golden-cyan cross slash entity. The entity is
                // splash only: half finisher damage, and it never re-hits the primary target,
                // who already took the direct finisher hit above.
                BraveCrossSlashEntity crossSlash = new BraveCrossSlashEntity(
                    level, player, targetCenter, player.getYRot(), player.getXRot(), finisherDmg * 0.5f, 1.25f
                );
                crossSlash.excludeEntity(target.getId());
                level.addFreshEntity(crossSlash);

                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0f, 0.6f);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), com.dragonblockarcanedba.sound.DbaSounds.SLASH_WHOOSH, SoundSource.PLAYERS, 2.0f, 0.6f);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.8f, 1.2f);
                com.dragonblockarcanedba.fx.DbaImpactFx.heavy(level, target.getX(), target.getY(), target.getZ());

                setCombo(stack, 0); // Reset combo
                player.sendSystemMessage(Component.literal("§6★ BRAVE FINISHER! ★"), true);
            } else {
                // The straight cut is the hit. A fifth-hit cut is heavier. There is no
                // second invisible strike under it.
                float hitDmg = 400.0f + (strength * 2.0f);
                boolean heavy = combo % 5 == 0;
                BraveSlashEntity cut = new BraveSlashEntity(level, player, heavy ? hitDmg * 1.5f : hitDmg);
                cut.setDeltaMovement(look.scale(1.8));
                level.addFreshEntity(cut);
                // Combo particle feedback & sound. A heavy hit plays its own
                // louder pair only; before this pass it played both pairs in
                // the same tick, doubling the sweep and whoosh.
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
                if (heavy) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 1.4f);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), com.dragonblockarcanedba.sound.DbaSounds.SLASH_WHOOSH, SoundSource.PLAYERS, 1.5f, 1.4f);
                } else {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.0f + combo * 0.08f);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), com.dragonblockarcanedba.sound.DbaSounds.SLASH_WHOOSH, SoundSource.PLAYERS, 1.2f, 1.0f + combo * 0.08f);
                }
                player.sendSystemMessage(Component.literal(String.format("§6Brave Power: §e%d Hits", combo)), true);
            }
        } else {
            // An empty swing still throws the sword's own straight cut.
            if (!WeaponDrainHelper.drainBothPerTick(player, 25.0, 20.0)) {
                return;
            }
            float hitDmg = 400.0f + (accessor.dba$getStrength() * 2.0f);
            BraveSlashEntity cut = new BraveSlashEntity(level, player, hitDmg);
            cut.setDeltaMovement(look.scale(1.8));
            level.addFreshEntity(cut);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.35f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.SLASH_WHOOSH, SoundSource.PLAYERS, 1.0f, 1.35f);
        }
    }

    // --- RIGHT CLICK: Brave Sword Attack (Charged Piercing Heroic Dash) ---

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!WeaponDrainHelper.hasKi(player)) {
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
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingTicks) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            int heldTicks = getUseDuration(stack, living) - remainingTicks;
            // Drain Ki: 15% per minute (smooth), but only while the charge is
            // still growing. At full charge the ratio is capped, so holding
            // longer used to keep charging Ki for no extra damage, radius,
            // or visual growth; now a full charge is free to hold.
            if (heldTicks <= MAX_RIGHT_CHARGE_TICKS) {
                if (!WeaponDrainHelper.drainKiPerTick(player, 15.0)) {
                    player.stopUsingItem();
                    return;
                }
            }

            ServerLevel serverLevel = (ServerLevel) level;
            float chargeRatio = Math.min(1.0f, heldTicks / (float) MAX_RIGHT_CHARGE_TICKS);

            // Heroic Focus: courageous stance, armor & jump power
            player.addEffect(new MobEffectInstance(DbaEffects.HEROIC_FOCUS_HOLDER, 10, 0, false, false));

            // Manage physical 3D Brave Charge Entity. A fading entity is the
            // previous charge dissolving; a new charge replaces it at once.
            BraveChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.get(player.getUUID());
            if (chargeEntity == null || !chargeEntity.isAlive() || chargeEntity.isFading()) {
                chargeEntity = new BraveChargeEntity(serverLevel, player);
                serverLevel.addFreshEntity(chargeEntity);
                ACTIVE_CHARGE_MAP.put(player.getUUID(), chargeEntity);
            }
            chargeEntity.setChargeRatio(chargeRatio);

            if (heldTicks % 20 == 0) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9f, 0.8f + chargeRatio * 0.5f);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.ABILITY_CHARGE, SoundSource.PLAYERS, 0.9f, 0.8f + chargeRatio * 0.5f);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            ServerLevel serverLevel = (ServerLevel) level;
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

            int heldTicks = getUseDuration(stack, living) - timeLeft;
            // No free floor: an instant release is a short, weak dash and a
            // full 100 tick charge is worth 3x its distance and over 3x its
            // base damage, so charging honestly pays (standard numbers rule).
            float chargeRatio = Math.min(1.0f, heldTicks / (float) MAX_RIGHT_CHARGE_TICKS);

            // The charge construct dissolves over its own fade out instead of
            // being discarded at full alpha in this tick.
            BraveChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.remove(player.getUUID());
            if (chargeEntity != null && chargeEntity.isAlive()) {
                chargeEntity.startFade();
            }

            if (com.dragonblockarcanedba.util.MovementLimiterHelper.isMovementImmobilized(player)) {
                player.sendSystemMessage(Component.literal("§cImmobilized! Cannot dash."), true);
                return false;
            }

            double ccMult = com.dragonblockarcanedba.util.MovementLimiterHelper.getMovementMultiplier(player);

            // High-speed piercing sword dash, 8 blocks on a tap to 24 blocks at
            // full charge (scaled by CC / movement limiter)
            Vec3 start = player.position();
            Vec3 look = player.getLookAngle();
            double dashDist = (8.0 + (chargeRatio * 16.0)) * ccMult;
            Vec3 end = start.add(look.scale(dashDist));

            // Heroic finishing slash damage: 600.0 + (chargeRatio * 1400.0) + (Strength * 4.0 * chargeRatio).
            // The rush trail entity resolves it at its visual peak tick against
            // positions measured then, and fires the radial shockwave itself
            // on a kill or at charge 0.9 or more.
            float baseDamage = 600.0f + (chargeRatio * 1400.0f);
            float strDamage = (float) (accessor.dba$getStrength() * 4.0f * chargeRatio);
            float totalDamage = baseDamage + strDamage;

            // Spawn physical 3D supersonic rush flight trail connecting start to end
            Vec3 dashVec = end.subtract(start);
            float trailLen = (float) dashVec.length();
            if (trailLen > 0.1f) {
                double dx = dashVec.x, dy = dashVec.y, dz = dashVec.z;
                float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
                float pitch = (float) (-(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI)));
                BraveRushTrailEntity trail = new BraveRushTrailEntity(
                    serverLevel, player, start.add(0, 0.8, 0), yaw, pitch, trailLen, 1.0f + chargeRatio * 0.5f
                );
                trail.setDashPayload(totalDamage, chargeRatio, look);
                serverLevel.addFreshEntity(trail);
            }

            // Teleport player
            MovementPhysics.teleport(player, end);

            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0f, 0.7f);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.SLASH_WHOOSH, SoundSource.PLAYERS, 2.0f, 0.7f);

            player.getCooldowns().addCooldown(stack, 100); // 5-second cooldown
        }
        return true;
    }

    public static void onPlayerDisconnect(UUID playerUuid) {
        BraveChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.remove(playerUuid);
        if (chargeEntity != null && chargeEntity.isAlive()) {
            chargeEntity.discard();
        }
    }
}
