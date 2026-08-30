package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.HeavenSplitterEntity;
import com.dragonblockarcanedba.entity.HollowAfterimageEntity;
import com.dragonblockarcanedba.entity.KatanaAimGuideEntity;
import com.dragonblockarcanedba.entity.KatanaChargeEntity;
import com.dragonblockarcanedba.entity.SwiftCrescentEntity;
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
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.dragonblockarcanedba.util.WeaponDrainHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Katana — Supreme Speed Weapon.
 * 
 * Ki Drain: 30% per minute (smooth).
 */
public class KatanaItem extends Item {
    public static final int MAX_LEFT_CHARGE_TICKS = 100; // 5 seconds
    public static final int MAX_RIGHT_CHARGE_TICKS = 100; // 5 seconds

    // Track active charging KatanaChargeEntity per player
    public static final Map<UUID, KatanaChargeEntity> ACTIVE_CHARGE_MAP = new ConcurrentHashMap<>();
    // Track active aim guide KatanaAimGuideEntity per player
    public static final Map<UUID, KatanaAimGuideEntity> ACTIVE_AIM_MAP = new ConcurrentHashMap<>();

    public KatanaItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    749.0, // 1 + 749 = 750 base damage
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -1.0, // Fast fluid iaido strikes
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Stealth: Silent Blade Concealment
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.NAME_PLATE_DIST_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("katana_nameplate_stealth"), -48.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("katana_air_drag"), -0.50, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !player.level().isClientSide()) {
            WeaponDrainHelper.drainKiDiscrete(player, 30.0, 7);
        }
    }

    // --- LEFT CLICK: Flashdraw (Multi-target Instant Dash Execution) ---

    public static void onLeftClickChargeTick(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();

        // Drain Ki: 30% per minute (smooth)
        if (!WeaponDrainHelper.drainKiPerTick(player, 30.0)) {
            onLeftClickRelease(player, stack, chargeTicks);
            return;
        }
        if (chargeTicks % 5 == 0) {
            ((PlayerStatsAccessor) player).dba$syncStats();
        }

        float chargeRatio = Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS);

        // Maintain physical 3D Katana Charge Entity (iaido stance ground focus rings & hilt sparks)
        KatanaChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.get(player.getUUID());
        if (chargeEntity == null || !chargeEntity.isAlive()) {
            chargeEntity = new KatanaChargeEntity(level, player);
            level.addFreshEntity(chargeEntity);
            ACTIVE_CHARGE_MAP.put(player.getUUID(), chargeEntity);
        }
        chargeEntity.setChargeRatio(chargeRatio);

        if (chargeTicks % 20 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7f, 1.4f + chargeRatio * 0.4f);
        }
    }

    public static void onLeftClickRelease(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Discard active drawing charge entity
        KatanaChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.remove(player.getUUID());
        if (chargeEntity != null && chargeEntity.isAlive()) {
            chargeEntity.discard();
        }

        if (player.getCooldowns().isOnCooldown(stack)) return;

        // Find enemies within 16 blocks to chain through (up to 5 targets, Tweak A)
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        if (com.dragonblockarcanedba.util.MovementLimiterHelper.isMovementImmobilized(player)) {
            player.sendSystemMessage(Component.literal("§cImmobilized! Cannot flash thrust."), true);
            return;
        }

        double ccMult = com.dragonblockarcanedba.util.MovementLimiterHelper.getMovementMultiplier(player);
        AABB searchBox = player.getBoundingBox().inflate(16.0 * ccMult);
        List<LivingEntity> potential = level.getEntitiesOfClass(
            LivingEntity.class, searchBox,
            e -> e.isAlive() && e != player
        );

        Set<Integer> visited = new HashSet<>();
        List<LivingEntity> chain = new ArrayList<>();

        // Start with best target in look direction
        LivingEntity current = null;
        double bestScore = 0.4;
        for (LivingEntity e : potential) {
            Vec3 to = e.getEyePosition().subtract(eye).normalize();
            double dot = look.dot(to);
            if (dot > bestScore) {
                bestScore = dot;
                current = e;
            }
        }

        if (current != null) {
            chain.add(current);
            visited.add(current.getId());

            // Chain to up to 4 more nearby enemies
            for (int i = 0; i < 4; i++) {
                AABB nearBox = current.getBoundingBox().inflate(12.0);
                List<LivingEntity> nearby = level.getEntitiesOfClass(
                    LivingEntity.class, nearBox,
                    e -> e.isAlive() && e != player && !visited.contains(e.getId())
                );
                if (nearby.isEmpty()) break;
                LivingEntity next = nearby.get(0);
                chain.add(next);
                visited.add(next.getId());
                current = next;
            }
        }

        // Blade Guard: Invulnerability stance during dash
        player.addEffect(new MobEffectInstance(DbaEffects.BLADE_GUARD_HOLDER, 20, 0, false, false));

        Vec3 startPos = player.position();

        if (!chain.isEmpty()) {
            for (int i = 0; i < chain.size(); i++) {
                LivingEntity t = chain.get(i);
                final int targetIndex = i;

                // Leave high-quality 3D ghost afterimage at intermediate spot
                HollowAfterimageEntity afterimage = new HollowAfterimageEntity(level, player);
                afterimage.setPos(player.getX(), player.getY(), player.getZ());
                level.addFreshEntity(afterimage);

                // Teleport past target
                Vec3 past = t.position().add(player.getLookAngle().scale(1.5));
                player.teleportTo(level, past.x, past.y, past.z, java.util.Collections.emptySet(), player.getYRot(), player.getXRot(), false);

                // Apply Silent Mark
                t.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.SILENT_MARK_HOLDER, 20, 0, false, false));
                t.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 25, 0, false, false, false), player);

                // Tweak B: +20% damage per consecutive target crossed
                float streakBonus = 1.0f + (i * 0.2f);
                float damage = (500.0f + (float) (accessor.dba$getStrength() * 2.5f)) * streakBonus;

                final LivingEntity finalTarget = t;
                final float finalDamage = damage;
                // Delayed simultaneous strike 0.3s (6 ticks) later
                level.getServer().execute(() -> {
                    finalTarget.hurtServer(level, level.damageSources().playerAttack(player), finalDamage);

                    // Spawn physical 3D Swift Crescent model & cross-cleave speed cuts on target
                    Vec3 impactPos = finalTarget.position().add(0, finalTarget.getBbHeight() * 0.5, 0);
                    float tiltAngle = (targetIndex % 2 == 0 ? 35.0f : -35.0f) + (targetIndex * 15.0f);
                    float slashScale = 1.0f + (targetIndex * 0.15f);
                    int variant = targetIndex % 2;

                    SwiftCrescentEntity crescent = new SwiftCrescentEntity(
                        level, player, impactPos,
                        player.getYRot(), player.getXRot(),
                        tiltAngle, slashScale, variant, 10
                    );
                    level.addFreshEntity(crescent);

                    level.playSound(null, finalTarget.getX(), finalTarget.getY(), finalTarget.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.6f, 1.5f + (targetIndex * 0.1f));
                });
            }
        } else {
            // Freeform straight flash dash forward (scaled by CC / movement limiter)
            Vec3 targetPos = startPos.add(look.scale(14.0 * ccMult));
            player.teleportTo(level, targetPos.x, targetPos.y, targetPos.z, java.util.Collections.emptySet(), player.getYRot(), player.getXRot(), false);
        }

        // Blade sheathing ring sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 1.8f);

        player.getCooldowns().addCooldown(stack, 30);
    }

    // --- RIGHT CLICK: Iaijutsu: Heaven Splitter ---

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
            // Drain Ki: 30% per minute (smooth)
            if (!WeaponDrainHelper.drainKiPerTick(player, 30.0)) {
                player.stopUsingItem();
                return;
            }

            ServerLevel serverLevel = (ServerLevel) level;
            int heldTicks = getUseDuration(stack, living) - remainingTicks;
            float chargeRatio = Math.min(1.0f, heldTicks / (float) MAX_RIGHT_CHARGE_TICKS);

            // Stationary stillness focus
            player.setDeltaMovement(0, player.getDeltaMovement().y > 0 ? 0 : player.getDeltaMovement().y * 0.5, 0);
            player.hurtMarked = true;

            // Maintain sleek, physical 3D laser guide & geometric targeting beam up to 24 blocks
            KatanaAimGuideEntity aimGuide = ACTIVE_AIM_MAP.get(player.getUUID());
            if (aimGuide == null || !aimGuide.isAlive()) {
                aimGuide = new KatanaAimGuideEntity(serverLevel, player, 24.0f);
                serverLevel.addFreshEntity(aimGuide);
                ACTIVE_AIM_MAP.put(player.getUUID(), aimGuide);
            }
            aimGuide.setChargeRatio(chargeRatio);

            if (heldTicks % 20 == 0) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.2f + chargeRatio * 0.6f);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            ServerLevel serverLevel = (ServerLevel) level;
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

            // Discard active aim guide entity
            KatanaAimGuideEntity aimGuide = ACTIVE_AIM_MAP.remove(player.getUUID());
            if (aimGuide != null && aimGuide.isAlive()) {
                aimGuide.discard();
            }

            int heldTicks = getUseDuration(stack, living) - timeLeft;
            float chargeRatio = Math.max(0.1f, Math.min(1.0f, heldTicks / (float) MAX_RIGHT_CHARGE_TICKS));

            // Instant dash along aim line up to 24 blocks (Tweak B: snaps to nearest enemy)
            Vec3 start = player.position();
            Vec3 look = player.getLookAngle();
            double dashDist = 16.0 + (chargeRatio * 8.0);
            Vec3 end = start.add(look.scale(dashDist));

            // Blade Guard: Brief invulnerability during execution
            player.addEffect(new MobEffectInstance(DbaEffects.BLADE_GUARD_HOLDER, 20, 0, false, false));

            // Leave ghost afterimage at original starting location
            HollowAfterimageEntity originAfterimage = new HollowAfterimageEntity(serverLevel, player);
            originAfterimage.setPos(start.x, start.y, start.z);
            serverLevel.addFreshEntity(originAfterimage);

            // Hit entities along the line
            AABB lineBox = new AABB(start, end).inflate(2.0 + chargeRatio * 2.0); // Tweak A: wider with charge
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, lineBox,
                e -> e.isAlive() && e != player
            );

            // Teleport player to destination
            player.teleportTo(serverLevel, end.x, end.y, end.z, java.util.Collections.emptySet(), player.getYRot(), player.getXRot(), false);

            // Spawn cascading physical 3D Swift Crescent slashes all along the teleport path from start to end
            int slashCount = Math.max(7, (int) (dashDist / 2.2));
            float slashScale = 1.35f + chargeRatio * 0.55f;
            float[] tiltAngles = { 45.0f, -40.0f, 75.0f, -65.0f, 20.0f, -80.0f, 60.0f, -30.0f, 90.0f, -50.0f };
            Vec3 rightVec = look.cross(new Vec3(0, 1, 0)).normalize();

            for (int i = 0; i <= slashCount; i++) {
                float fraction = i / (float) slashCount;
                Vec3 slashPos = start.add(look.scale(dashDist * fraction)).add(0, player.getBbHeight() * 0.5, 0);

                // Add subtle lateral zig-zag offset for anime-style dimensional iaido slashes
                float lateralOffset = ((i % 3) - 1) * 0.35f;
                slashPos = slashPos.add(rightVec.scale(lateralOffset));

                float tilt = tiltAngles[i % tiltAngles.length];
                int variant = i % 2;
                int lifetime = 14 + (i % 4);

                SwiftCrescentEntity crescent = new SwiftCrescentEntity(
                    serverLevel, player, slashPos,
                    player.getYRot(), player.getXRot(),
                    tilt, slashScale * (0.95f + (i % 3) * 0.15f), variant, lifetime
                );
                serverLevel.addFreshEntity(crescent);
            }

            // Delayed massive vertical energy cut (0.5s / 10 ticks)
            float baseDamage = 1000.0f + (chargeRatio * 1000.0f);
            float strDamage = (float) (accessor.dba$getStrength() * 4.0f * chargeRatio);
            float totalDamage = baseDamage + strDamage;

            for (LivingEntity t : targets) {
                t.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 20, 0, false, false, false), player);
                t.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), totalDamage);

                // Cross-cleave physical crescent slashes centered on the target
                Vec3 targetPos = t.position().add(0, t.getBbHeight() * 0.5, 0);
                SwiftCrescentEntity targetCrescent1 = new SwiftCrescentEntity(
                    serverLevel, player, targetPos,
                    player.getYRot(), player.getXRot(),
                    40.0f, slashScale * 1.25f, 0, 14
                );
                SwiftCrescentEntity targetCrescent2 = new SwiftCrescentEntity(
                    serverLevel, player, targetPos,
                    player.getYRot(), player.getXRot(),
                    -40.0f, slashScale * 1.25f, 1, 14
                );
                serverLevel.addFreshEntity(targetCrescent1);
                serverLevel.addFreshEntity(targetCrescent2);

                // Tweak C: If target dies, reset Flashdraw cooldown
                if (!t.isAlive()) {
                    player.getCooldowns().removeCooldown(com.dragonblockarcanedba.item.DbaItems.KATANA_KEY.identifier());
                }
            }

            // Spawn towering physical 3D dimensional slash entity across raycast path
            HeavenSplitterEntity heavenSplitter = new HeavenSplitterEntity(
                serverLevel, player, start.add(0, 0.5, 0), look,
                (float) dashDist, 1.0f + chargeRatio * 0.5f, 20
            );
            serverLevel.addFreshEntity(heavenSplitter);

            // Multi-layered audio feedback: origin cut sweep, speed strike, thunder crack, and katana sheathing ring
            serverLevel.playSound(null, start.x, start.y, start.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8f, 1.6f);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 2.0f, 1.4f);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 1.8f);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.9f, 1.9f);

            player.getCooldowns().addCooldown(stack, 80); // 4-second cooldown
        }
        return true;
    }
}
