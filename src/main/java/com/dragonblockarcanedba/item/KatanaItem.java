package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.HeavenSplitterEntity;
import com.dragonblockarcanedba.entity.HollowAfterimageEntity;
import com.dragonblockarcanedba.entity.KatanaAimGuideEntity;
import com.dragonblockarcanedba.entity.KatanaChargeEntity;
import com.dragonblockarcanedba.entity.SwiftCrescentEntity;
import com.dragonblockarcanedba.movement.MovementPhysics;
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
 * Katana: Supreme Speed Weapon.
 *
 * Plain swings deal attribute damage only and cost no Ki. Ki is spent by the
 * two charged abilities: Flashdraw (left click) drains 40 percent per minute
 * while charging, and Heaven Splitter (right click) drains 60 percent per
 * minute while charging. If Ki runs out mid charge the ability cancels: it
 * never fires and takes no cooldown.
 */
public class KatanaItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
    public static final int MAX_LEFT_CHARGE_TICKS = 100; // 5 seconds
    public static final int MAX_RIGHT_CHARGE_TICKS = 100; // 5 seconds

    // Track active charging KatanaChargeEntity per player
    public static final Map<UUID, KatanaChargeEntity> ACTIVE_CHARGE_MAP = new ConcurrentHashMap<>();
    // Track active aim guide KatanaAimGuideEntity per player
    public static final Map<UUID, KatanaAimGuideEntity> ACTIVE_AIM_MAP = new ConcurrentHashMap<>();

    // Players whose Flashdraw charge died from Ki exhaustion. Their next
    // release is swallowed instead of firing.
    private static final Set<UUID> FLASHDRAW_KI_CANCELLED = ConcurrentHashMap.newKeySet();
    // Players whose Heaven Splitter hold died from Ki exhaustion. Their
    // releaseUsing call is swallowed instead of firing.
    private static final Set<UUID> SPLITTER_KI_CANCELLED = ConcurrentHashMap.newKeySet();

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

    // --- LEFT CLICK: Flashdraw (Multi-target Instant Dash Execution) ---

    public static void onLeftClickChargeTick(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();

        // A charge that already died from Ki exhaustion stays dead until the
        // release arrives: no drain, no charge visual, no second chance.
        if (FLASHDRAW_KI_CANCELLED.contains(player.getUUID())) {
            if (chargeTicks <= 1) {
                // A fresh charge is starting, so the old cancel is spent.
                FLASHDRAW_KI_CANCELLED.remove(player.getUUID());
            } else {
                return;
            }
        }

        // Drain Ki: 40% per minute (smooth). Running dry cancels the draw:
        // Flashdraw does NOT fire on an empty tank.
        if (!WeaponDrainHelper.drainKiPerTick(player, 40.0)) {
            cancelFlashdrawCharge(player);
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

    /** Ends a Flashdraw charge without firing it: the stance dissolves, no cooldown. */
    private static void cancelFlashdrawCharge(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        KatanaChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.remove(player.getUUID());
        if (chargeEntity != null && chargeEntity.isAlive()) {
            chargeEntity.beginFadeOut();
        }
        FLASHDRAW_KI_CANCELLED.add(player.getUUID());
        player.sendSystemMessage(Component.literal("§cOut of Ki! Flashdraw cancelled."), true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 0.9f);
    }

    public static void onLeftClickRelease(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Ki exhaustion during the charge cancels the release entirely:
        // no dash, no damage, no cooldown.
        boolean kiCancelled = FLASHDRAW_KI_CANCELLED.remove(player.getUUID());

        // Fade the drawing stance out instead of discarding it mid-frame.
        KatanaChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.remove(player.getUUID());
        if (chargeEntity != null && chargeEntity.isAlive()) {
            chargeEntity.beginFadeOut();
        }

        if (kiCancelled) return;
        if (player.getCooldowns().isOnCooldown(stack)) return;

        // Charge matters: a tap fires at 55% power, a full 5 second draw at 130%,
        // and the freeform dash stretches from 8 to 14 blocks with charge.
        float chargeRatio = Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS);
        float chargeDamageMult = 0.55f + 0.75f * chargeRatio;

        // Find living targets within 16 blocks to chain through (up to 5, Tweak A).
        // The dash cuts anything living in its path; there is no faction filter.
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        if (com.dragonblockarcanedba.util.MovementLimiterHelper.isMovementImmobilized(player)) {
            // The move never fired, so the Ki spent charging is refunded.
            double refund = com.dragonblockarcanedba.attribute.PlayerStats.getMaxKi(player)
                * (40.0 / 100.0) * (chargeTicks / WeaponDrainHelper.TICKS_PER_MINUTE);
            if (refund > 0) accessor.dba$addKi(refund);
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

            // Chain to up to 4 more targets, nearest first, so the streak
            // bonus, tilt, and scale progressions follow the path of the dash
            // instead of the entity query order.
            for (int i = 0; i < 4; i++) {
                AABB nearBox = current.getBoundingBox().inflate(12.0);
                List<LivingEntity> nearby = level.getEntitiesOfClass(
                    LivingEntity.class, nearBox,
                    e -> e.isAlive() && e != player && !visited.contains(e.getId())
                );
                if (nearby.isEmpty()) break;
                LivingEntity next = null;
                double bestDist = Double.MAX_VALUE;
                for (LivingEntity cand : nearby) {
                    double d = cand.distanceToSqr(current);
                    if (d < bestDist) {
                        bestDist = d;
                        next = cand;
                    }
                }
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
                MovementPhysics.teleport(player, past);

                // Apply Silent Mark
                t.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.SILENT_MARK_HOLDER, 20, 0, false, false));
                t.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 25, 0, false, false, false), player);

                // Tweak B: +20% damage per consecutive target crossed
                float streakBonus = 1.0f + (i * 0.2f);
                float damage = (500.0f + (float) (accessor.dba$getStrength() * 2.5f)) * streakBonus * chargeDamageMult;

                // The crescent carries the strike: damage, impact burst, and
                // sounds land at tick 4 + 2 per chain step inside the
                // crescent's own life, as the cut opens on the target.
                Vec3 impactPos = t.position().add(0, t.getBbHeight() * 0.5, 0);
                float tiltAngle = (targetIndex % 2 == 0 ? 35.0f : -35.0f) + (targetIndex * 15.0f);
                float slashScale = 1.0f + (targetIndex * 0.15f);
                int variant = targetIndex % 2;
                int strikeTick = 4 + targetIndex * 2;
                float strikePitch = 1.5f + (targetIndex * 0.1f);

                SwiftCrescentEntity crescent = new SwiftCrescentEntity(
                    level, player, impactPos,
                    player.getYRot(), player.getXRot(),
                    tiltAngle, slashScale, variant, 16,
                    t.getId(), damage, strikeTick, targetIndex == 0, strikePitch
                );
                level.addFreshEntity(crescent);
            }
        } else {
            // Freeform straight flash dash forward (scaled by CC / movement limiter, 8 to 14 blocks by charge)
            double dashDist = (8.0 + 6.0 * chargeRatio) * ccMult;
            Vec3 targetPos = startPos.add(look.scale(dashDist));

            // Leave a ghost at the launch point, like the chained branch does
            // at every step, and kick dust where the dash leaves and lands.
            HollowAfterimageEntity dashGhost = new HollowAfterimageEntity(level, player);
            dashGhost.setPos(startPos.x, startPos.y, startPos.z);
            level.addFreshEntity(dashGhost);
            com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(level, startPos, 1.0f, 0x9FE8FF);

            MovementPhysics.teleport(player, targetPos);
            level.playSound(null, startPos.x, startPos.y, startPos.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 1.7f);
            com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(level, targetPos, 1.0f, 0x9FE8FF);
            for (int i = 1; i <= 3; i++) {
                Vec3 trailPos = startPos.add(look.scale(dashDist * i / 4.0)).add(0, player.getBbHeight() * 0.5, 0);
                SwiftCrescentEntity trail = new SwiftCrescentEntity(
                    level, player, trailPos, player.getYRot(), player.getXRot(),
                    i % 2 == 0 ? 35.0f : -35.0f, 0.8f, i % 2, 12
                );
                level.addFreshEntity(trail);
            }
        }

        // Blade sheathing ring sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 1.8f);

        player.getCooldowns().addCooldown(stack, 60); // 3-second cooldown
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
            // Drain Ki: 60% per minute (smooth). Running dry cancels the
            // stance: stopUsingItem leads into releaseUsing, which sees the
            // cancel flag and does NOT fire Heaven Splitter.
            if (!WeaponDrainHelper.drainKiPerTick(player, 60.0)) {
                SPLITTER_KI_CANCELLED.add(player.getUUID());
                player.stopUsingItem();
                return;
            }

            ServerLevel serverLevel = (ServerLevel) level;
            int heldTicks = getUseDuration(stack, living) - remainingTicks;
            float chargeRatio = Math.min(1.0f, heldTicks / (float) MAX_RIGHT_CHARGE_TICKS);

            // Stationary stillness focus
            MovementPhysics.setVelocity(player, new Vec3(0, player.getDeltaMovement().y > 0 ? 0 : player.getDeltaMovement().y * 0.5, 0));

            // Maintain the physical 3D laser guide, drawn to the exact dash
            // distance for the current charge: 16 blocks at a tap, 24 at full.
            KatanaAimGuideEntity aimGuide = ACTIVE_AIM_MAP.get(player.getUUID());
            if (aimGuide == null || !aimGuide.isAlive()) {
                aimGuide = new KatanaAimGuideEntity(serverLevel, player, 16.0f);
                serverLevel.addFreshEntity(aimGuide);
                ACTIVE_AIM_MAP.put(player.getUUID(), aimGuide);
            }
            aimGuide.setChargeRatio(chargeRatio);
            aimGuide.setMaxRange((float) (16.0 + chargeRatio * 8.0));

            if (heldTicks % 20 == 0) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.2f + chargeRatio * 0.6f);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.ABILITY_CHARGE, SoundSource.PLAYERS, 0.8f, 1.2f + chargeRatio * 0.6f);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            ServerLevel serverLevel = (ServerLevel) level;
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

            // Fade the aim guide out instead of discarding it mid-frame.
            KatanaAimGuideEntity aimGuide = ACTIVE_AIM_MAP.remove(player.getUUID());
            if (aimGuide != null && aimGuide.isAlive()) {
                aimGuide.beginFadeOut();
            }

            // Ki exhaustion during the hold cancels the move: no dash, no
            // damage, no cooldown.
            if (SPLITTER_KI_CANCELLED.remove(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§cOut of Ki! Heaven Splitter cancelled."), true);
                return true;
            }

            int heldTicks = getUseDuration(stack, living) - timeLeft;
            float chargeRatio = Math.max(0.1f, Math.min(1.0f, heldTicks / (float) MAX_RIGHT_CHARGE_TICKS));

            // Dash straight along the aim line: 16 blocks at the tap floor,
            // 24 at full charge. The end point is pure look direction; the
            // cut hits whatever living targets the wall passes through.
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

            // Teleport player to destination
            MovementPhysics.teleport(player, end);

            // The wall entity owns the cut: its damage lands at tick 10 of
            // its 40 tick life, once the wall has fully risen, with falloff
            // toward the far end of the path. Corridor half width grows with
            // charge (Tweak A): 2.2 at the tap floor, 4.0 at full charge.
            float baseDamage = 600.0f + (chargeRatio * 1400.0f);
            float strDamage = (float) (accessor.dba$getStrength() * 4.0f * chargeRatio);
            float totalDamage = baseDamage + strDamage;
            float corridorHalf = 2.0f + chargeRatio * 2.0f;

            HeavenSplitterEntity heavenSplitter = new HeavenSplitterEntity(
                serverLevel, player, start.add(0, 0.5, 0), look,
                (float) dashDist, 1.0f + chargeRatio * 0.5f, 40,
                totalDamage, corridorHalf
            );
            serverLevel.addFreshEntity(heavenSplitter);

            // Audio at the dash: origin cut sweep, speed strike, and katana
            // sheathing ring. The thunder crack plays in the wall entity at
            // the tick the cut lands.
            serverLevel.playSound(null, start.x, start.y, start.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8f, 1.6f);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 2.0f, 1.4f);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.BLADE_SHING, SoundSource.PLAYERS, 2.0f, 1.4f);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.9f, 1.9f);

            player.getCooldowns().addCooldown(stack, 120); // 6-second cooldown
        }
        return true;
    }

    /** Discards any charge or stance entities still tracked for a player who logged out. */
    public static void onPlayerDisconnect(UUID playerUuid) {
        net.minecraft.world.entity.Entity e0 = ACTIVE_CHARGE_MAP.remove(playerUuid);
        if (e0 != null && e0.isAlive()) e0.discard();
        net.minecraft.world.entity.Entity e1 = ACTIVE_AIM_MAP.remove(playerUuid);
        if (e1 != null && e1.isAlive()) e1.discard();
        FLASHDRAW_KI_CANCELLED.remove(playerUuid);
        SPLITTER_KI_CANCELLED.remove(playerUuid);
    }
}
