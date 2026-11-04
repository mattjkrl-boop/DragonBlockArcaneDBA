package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.ZChargeEntity;
import com.dragonblockarcanedba.movement.MovementPhysics;
import com.dragonblockarcanedba.entity.ZGravitySlamEntity;
import com.dragonblockarcanedba.entity.ZShockwaveEntity;
import com.dragonblockarcanedba.entity.ZStanceAuraEntity;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Z-Sword — Divine Heavy Weapon.
 * 
 * Drain: Left click Ki 75% + Stamina 50% per minute (1.25% Ki and 0.83% stamina
 * per second), Right click Stamina 75% per minute (1.25% per second).
 *
 * LEFT: Z Shockwave (Charge-to-Fire)
 * - Hold left click to channel power (up to 15s / 300 ticks, then it fires on its own).
 * - Drains Ki and stamina as above, slows movement.
 * - Spawns 3D ZChargeEntity (golden geometric vortex, counter-rotating energy rings & sacred crystal prisms).
 * - Release sends gigantic 3D volumetric horizontal golden energy wave (ZShockwaveEntity).
 * - Recoil knocks player backward based on charge.
 * - Tweak A: Spawns trailing slower sub-waves at max charge.
 * - Tweak B: Ignores terrain and passes through blocks at max charge.
 * - Tweak C: Roots enemies 2 blocks into the ground, dealing head crush damage.
 *
 * RIGHT: Katchin Weight (Divine Heavy Stance & Gravity Slam)
 * - Hold right click: Knockback immunity, near-total movement restriction, heavy damage reduction.
 * - The stance is a real gravity well: nearby mobs are dragged toward the holder while it lasts.
 * - Hold caps at 10s (200 ticks); the slam then fires on its own.
 * - Spawns 3D ZStanceAuraEntity (gravity suction field, Katchin weight monoliths & contracting event horizon rings).
 * - Release unleashes massive physical 3D ZGravitySlamEntity (6 to 18 block RADIUS ground shatter,
 *   12 to 36 blocks across, canyon fissures & erupted monoliths). The slam entity itself deals the
 *   damage at the visual peak tick, with distance falloff, real ground scarring and a debris field.
 * - Tweak A: Roots enemies for the launch delay (40 ticks plus 40 per full power), then launches them.
 */
public class ZSwordItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
    public static final int MAX_LEFT_CHARGE_TICKS = 300; // 15 seconds
    public static final int MAX_STANCE_TICKS = 200; // 10 seconds to full slam power

    private static final Map<UUID, ZChargeEntity> ACTIVE_CHARGE_MAP = new ConcurrentHashMap<>();
    private static final Map<UUID, ZStanceAuraEntity> ACTIVE_STANCE_MAP = new ConcurrentHashMap<>();
    // Players whose held charge already auto-fired at max during the current hold,
    // so the rest of the hold and its release packet do not fire a second wave.
    private static final java.util.Set<UUID> MAX_CHARGE_FIRED = ConcurrentHashMap.newKeySet();

    public ZSwordItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    999.0, // 1 + 999 = 1000 base damage
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -3.0, // Slowest, most devastating swing speed
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Physics: Immovable Sacred Blade Weight (increased friction)
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("z_sword_weight_friction"), 0.80, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // The katchin wave and the gravity slam already pay, so a vanilla connect
        // spends no Ki and no stamina. Spectacle pass: the connect itself now reads
        // on screen, a small ground debris burst and gold sparks at the target,
        // particles only, no extra damage.
        if (attacker.level() instanceof ServerLevel serverLevel) {
            float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
            if (density > 0f) {
                com.dragonblockarcanedba.fx.SlamDebris.burst(serverLevel, target.position(), 1.2,
                    com.dragonblockarcanedba.fx.SlamDebris.Profile.Z_GRAVITY);
                serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    Math.max(2, Math.round(8f * density)), 0.5, 0.6, 0.5, 0.10);
            }
        }
    }

    // --- LEFT CLICK: Z Shockwave Charging & Firing ---

    public static void onLeftClickChargeTick(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Hard stop at maximum charge: the wave fires itself at 300 ticks instead
        // of draining forever at full ratio, and the rest of this hold stays quiet.
        if (chargeTicks >= MAX_LEFT_CHARGE_TICKS) {
            if (MAX_CHARGE_FIRED.add(player.getUUID())) {
                fireShockwave(player, stack, chargeTicks);
            }
            return;
        }
        MAX_CHARGE_FIRED.remove(player.getUUID());

        // Drain Ki: 75% per min + Stamina: 50% per min (smooth)
        if (!WeaponDrainHelper.drainBothPerTick(player, 75.0, 50.0)) {
            // Out of Ki or Stamina! Force fire the shockwave with accumulated charge
            onLeftClickRelease(player, stack, chargeTicks);
            return;
        }
        if (chargeTicks % 5 == 0) {
            accessor.dba$syncStats();
        }

        // Heavy movement slowdown & grounding while charging
        player.addEffect(new MobEffectInstance(DbaEffects.ANCIENT_WEIGHT_HOLDER, 10, 0, false, false));

        float chargeRatio = Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS);

        // Physical 3D ZChargeEntity (dynamic golden vortex, counter-rotating energy rings & sacred crystal prisms)
        ZChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.get(player.getUUID());
        if (chargeEntity == null || chargeEntity.isRemoved()) {
            chargeEntity = new ZChargeEntity(level, player);
            level.addFreshEntity(chargeEntity);
            ACTIVE_CHARGE_MAP.put(player.getUUID(), chargeEntity);
        }
        chargeEntity.setChargeRatio(chargeRatio);

        // Audio hum
        if (chargeTicks % 20 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.6f + chargeRatio * 0.8f);
        }
    }

    public static void onLeftClickRelease(ServerPlayer player, ItemStack stack, int chargeTicks) {
        // A release packet that arrives after the max charge auto-fire, still at
        // max ticks, is the tail of the same hold: the wave already went out.
        if (chargeTicks >= MAX_LEFT_CHARGE_TICKS && MAX_CHARGE_FIRED.remove(player.getUUID())) {
            return;
        }
        MAX_CHARGE_FIRED.remove(player.getUUID());
        fireShockwave(player, stack, chargeTicks);
    }

    private static void fireShockwave(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Fade the 3D charge entity out over a few ticks instead of discarding it
        // at full brightness in one tick.
        ZChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.remove(player.getUUID());
        if (chargeEntity != null && !chargeEntity.isRemoved()) {
            chargeEntity.beginFade();
        }

        float chargeRatio = Math.max(0.1f, Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS));

        // Stat-scaled damage: 150 base scaling up to 1200 + (Strength * 3.5).
        // A bare tap used to pay 330 base; it now pays 255, and a full charge is
        // worth about 4.7x a tap on the base number (standard: charge must pay).
        float baseDmg = 150.0f + (chargeRatio * 1050.0f);
        float strengthBonus = (float) (accessor.dba$getStrength() * 3.5 * chargeRatio);
        float totalDamage = baseDmg + strengthBonus;

        // Spawn massive 3D volumetric horizontal shockwave
        ZShockwaveEntity wave = new ZShockwaveEntity(level, player, chargeRatio, totalDamage, false);
        Vec3 look = player.getLookAngle();
        double speed = 1.4 + (chargeRatio * 1.4);
        wave.setDeltaMovement(look.x * speed, look.y * speed, look.z * speed);
        level.addFreshEntity(wave);

        // Player recoil: Launched backwards depending on charge
        Vec3 recoil = look.scale(-0.4 - (chargeRatio * 1.4));
        MovementPhysics.addVelocity(player, new Vec3(recoil.x, 0.15 + (chargeRatio * 0.2), recoil.z));
        com.dragonblockarcanedba.util.DbaCompat.markHurt(player);

        // Sounds & shockwave burst
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 1.2f);
        com.dragonblockarcanedba.fx.DbaImpactFx.heavy(level, player.getX(), player.getY(), player.getZ());

        // Cooldown based on power. A full charge wave lives 80 ticks in flight,
        // so the cooldown now runs 44 ticks at a tap to 120 at full charge: a
        // max wave cannot be re-fired before the first one has finished.
        int cooldown = 40 + (int) (chargeRatio * 80);
        player.getCooldowns().addCooldown(stack, cooldown);
    }

    // --- RIGHT CLICK: Katchin Weight (Divine Heavy Stance & Gravity Slam) ---

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!WeaponDrainHelper.hasStamina(player)) {
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
            // Drain: Right click Stamina 75% per minute (smooth)
            if (!WeaponDrainHelper.drainStaminaPerTick(player, 75.0)) {
                player.stopUsingItem();
                return;
            }

            ServerLevel serverLevel = (ServerLevel) level;
            int heldTicks = getUseDuration(stack, living) - remainingTicks;
            float stanceRatio = Math.min(1.0f, heldTicks / 200.0f); // 10s max power

            // Hard stop at full power: the slam fires itself at 200 ticks instead
            // of draining stamina forever at a clamped ratio. stopUsingItem routes
            // through releaseUsing with heldTicks at the cap.
            if (heldTicks >= MAX_STANCE_TICKS) {
                player.stopUsingItem();
                return;
            }

            // 1. Knockback immunity & movement lock
            MovementPhysics.rootInPlace(player);

            // 2. Heavy divine damage reduction and grounding (Ancient Weight)
            player.addEffect(new MobEffectInstance(DbaEffects.ANCIENT_WEIGHT_HOLDER, 10, 1, false, false));

            // 3. Physical 3D ZStanceAuraEntity (gravity suction field, Katchin weight monoliths & contracting event horizon rings)
            ZStanceAuraEntity stanceEntity = ACTIVE_STANCE_MAP.get(player.getUUID());
            if (stanceEntity == null || stanceEntity.isRemoved()) {
                stanceEntity = new ZStanceAuraEntity(serverLevel, player);
                serverLevel.addFreshEntity(stanceEntity);
                ACTIVE_STANCE_MAP.put(player.getUUID(), stanceEntity);
            }
            stanceEntity.updateStance(heldTicks, stanceRatio);

            // Gravity pulse sound
            if (heldTicks % 30 == 0) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.8f, 0.5f + stanceRatio * 0.5f);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            ServerLevel serverLevel = (ServerLevel) level;
            int heldTicks = getUseDuration(stack, living) - timeLeft;

            // Fade the 3D stance entity out over a few ticks instead of discarding
            // it at full brightness in one tick.
            ZStanceAuraEntity stanceEntity = ACTIVE_STANCE_MAP.remove(player.getUUID());
            if (stanceEntity != null && !stanceEntity.isRemoved()) {
                stanceEntity.beginFade();
            }

            if (heldTicks >= 10) {
                float powerRatio = Math.min(1.0f, heldTicks / 200.0f);
                double aoeRadius = 6.0 + (powerRatio * 12.0); // 6 to 18 block radius, 12 to 36 across

                PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
                float baseSlamDamage = 400.0f + (powerRatio * 800.0f);
                float strengthBonus = (float) (accessor.dba$getStrength() * 3.0 * powerRatio);
                float totalSlamDamage = baseSlamDamage + strengthBonus;

                // Launch delay scales with power now: 40 ticks at a tap hold up to
                // 80 at full power, so a 10 second charge buys a 4 second pin
                // window instead of the same flat 2 seconds a half second hold got.
                int launchDelay = 40 + (int) (powerRatio * 40);

                // Root and shatter land on release, but the slam DAMAGE no longer
                // lands here. ZGravitySlamEntity deals it at its visual peak tick
                // with distance falloff, so the hit and the picture agree.
                AABB aoe = player.getBoundingBox().inflate(aoeRadius, 4.0, aoeRadius);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, aoe, e -> e.isAlive() && e != player);

                java.util.List<LivingEntity> launchTargets = new java.util.ArrayList<>();

                for (LivingEntity target : targets) {
                    double dist = player.distanceTo(target);
                    if (dist <= aoeRadius) {
                        // Shatter now ends near the launch instead of outliving it
                        // by 100 ticks: 60 ticks flat.
                        target.addEffect(new MobEffectInstance(DbaEffects.EARTH_SHATTER_HOLDER, 60, 0, false, true), player);

                        // Tweak A: rooted for the whole launch delay, then launched.
                        target.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, launchDelay, 0, false, false, false), player);
                        target.addEffect(new MobEffectInstance(DbaEffects.MOVEMENT_CURSE_HOLDER, launchDelay, 9, false, true), player);
                        launchTargets.add(target);
                    }
                }

                if (!launchTargets.isEmpty()) {
                    com.dragonblockarcanedba.entity.DelayedLaunchEntity delayEntity = new com.dragonblockarcanedba.entity.DelayedLaunchEntity(
                        serverLevel, player, player.position(), powerRatio, launchTargets, (float) aoeRadius, launchDelay);
                    serverLevel.addFreshEntity(delayEntity);
                }

                // Spawn physical 3D ZGravitySlamEntity (radiating canyon trenches, erupted Katchin monoliths & expanding shockwave dome)
                ZGravitySlamEntity slam = new ZGravitySlamEntity(
                    serverLevel, player, player.position(), (float) aoeRadius, powerRatio, totalSlamDamage, launchDelay
                );
                serverLevel.addFreshEntity(slam);

                // Sound
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.6f);
        com.dragonblockarcanedba.fx.DbaImpactFx.heavy(serverLevel, player.getX(), player.getY(), player.getZ());
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5f, 0.5f);

                // Cooldown: at least the slam visual life (80 to 120 ticks), so a
                // second slam can never stack on a still burning first one.
                player.getCooldowns().addCooldown(stack, 100 + (int) (powerRatio * 100));
            } else {
                // Releasing too early used to waste the stance drain with no feedback at all.
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7f, 0.6f);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7The Z Sword is too heavy: hold the stance a moment longer."), true);
            }
        }
        return true;
    }

    /** Discards any charge or stance entities still tracked for a player who logged out. */
    public static void onPlayerDisconnect(UUID playerUuid) {
        MAX_CHARGE_FIRED.remove(playerUuid);
        net.minecraft.world.entity.Entity e0 = ACTIVE_CHARGE_MAP.remove(playerUuid);
        if (e0 != null && e0.isAlive()) e0.discard();
        net.minecraft.world.entity.Entity e1 = ACTIVE_STANCE_MAP.remove(playerUuid);
        if (e1 != null && e1.isAlive()) e1.discard();
    }
}
