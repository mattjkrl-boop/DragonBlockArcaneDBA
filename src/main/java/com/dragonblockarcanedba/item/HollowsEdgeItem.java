package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.HollowAfterimageEntity;
import com.dragonblockarcanedba.entity.VoidRiftEntity;
import com.dragonblockarcanedba.entity.VoidSlashEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hollow's Edge — Void Assassin Weapon.
 * 
 * LEFT: Hollow Rush (Charge & Phasing Multi-Dash)
 * - Hold left click to charge (up to 5s / 100 ticks).
 * - Release grants an active sequence window (heldSec * 2.0s).
 * - During active window, left click repeatedly to teleport 5 blocks (or backward if holding S - Tweak B).
 * - Phasing raycast teleports past obstacles into open gaps without getting stuck in solid blocks.
 * - Passing through enemies inflicts Void Damage, Dark Faded effect, and spawns an Afterimage clone.
 * - Grants Hollowed effect for 3s (translucency, speed, no mob aggro, block passing - Tweak C).
 * - Tweak A: Every 3rd teleport unleashes a massive Void Slash wave.
 * 
 * RIGHT: Void Rift (Dimensional Vortex & Implosion)
 * - Right click opens a stationary Void Rift at target location.
 * - Holding right click enlarges the rift and pulls enemies + projectiles inward.
 * - Tweak A: Follows crosshair while charging.
 * - Release triggers final implosion (violent inward pull -> massive outward explosion).
 * - Tweak C: Releasing while aiming at the rift teleports the player inside and grants Rifted effect.
 */
public class HollowsEdgeItem extends Item {
    public static final int MAX_LEFT_CHARGE_TICKS = 100; // 5 seconds

    // Track active window expiration per player: UUID -> expiration game time in ticks
    public static final Map<UUID, Long> ACTIVE_RUSH_EXPIRE_TIME = new ConcurrentHashMap<>();
    // Track dash counts in current sequence for Tweak A (every 3rd dash)
    public static final Map<UUID, Integer> DASH_COUNT_MAP = new ConcurrentHashMap<>();
    // Track active charging VoidRiftEntity per player
    public static final Map<UUID, VoidRiftEntity> ACTIVE_RIFT_MAP = new ConcurrentHashMap<>();

    public HollowsEdgeItem(Properties properties) {
        super(properties.attributes(
            ItemAttributeModifiers.builder()
                .add(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        849.0, // 1 + 849 = 850 base damage
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .add(
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(
                        BASE_ATTACK_SPEED_ID,
                        -1.8, // Fluid assassin strikes
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .build()
        ));
    }

    // --- LEFT CLICK: Hollow Rush Charging, Release & Dash ---

    public static void onLeftClickChargeTick(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Drain Ki while charging (~3% max Ki per second)
        double maxKi = PlayerStats.getMaxKi(player);
        double drainPerTick = (maxKi * 0.03) / 20.0;
        double currentKi = accessor.dba$getCurrentKi();

        if (currentKi >= drainPerTick) {
            accessor.dba$addKi(-drainPerTick);
            accessor.dba$syncStats();
        } else {
            onLeftClickRelease(player, stack, chargeTicks);
            return;
        }

        // Heavy movement slowdown while charging
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 3, false, false));

        // Dark void particles converging into blade
        float chargeRatio = Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS);
        int particleCount = 2 + (int) (chargeRatio * 6);

        for (int i = 0; i < particleCount; i++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2;
            double dist = 1.2 + (1.0 - chargeRatio) * 1.5;
            double px = player.getX() + Math.cos(angle) * dist;
            double pz = player.getZ() + Math.sin(angle) * dist;
            double py = player.getY() + 0.5 + level.getRandom().nextDouble() * 1.2;

            level.sendParticles(
                new DustParticleOptions(0x4B0082, 1.4F),
                px, py, pz,
                1, (player.getX() - px) * 0.15, (player.getY() + 0.8 - py) * 0.15, (player.getZ() - pz) * 0.15, 0.05
            );
        }

        if (chargeTicks % 20 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_AMBIENT, SoundSource.PLAYERS, 0.7f, 0.5f + chargeRatio * 0.6f);
        }
    }

    public static void onLeftClickRelease(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        float chargeRatio = Math.max(0.1f, Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS));
        
        // Active window duration = chargeRatio * 10 seconds (200 ticks max)
        int durationTicks = Math.max(40, (int) (chargeRatio * 200));
        long expireTime = level.getGameTime() + durationTicks;
        ACTIVE_RUSH_EXPIRE_TIME.put(player.getUUID(), expireTime);
        DASH_COUNT_MAP.put(player.getUUID(), 0);

        // Notify player of active sequence
        int seconds = durationTicks / 20;
        player.sendSystemMessage(Component.literal("§d✦ Hollow Rush Active (" + seconds + "s) — Left-Click to Dash! ✦"), true);

        // Sound & void surge particles
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2f, 1.4f);
        level.sendParticles(
            ParticleTypes.PORTAL,
            player.getX(), player.getY() + 1.0, player.getZ(),
            30, 0.5, 0.5, 0.5, 0.2
        );
    }

    public static void onLeftClickDash(ServerPlayer player, ItemStack stack, boolean holdingBackward) {
        ServerLevel level = (ServerLevel) player.level();
        UUID uuid = player.getUUID();
        long now = level.getGameTime();

        Long expireTime = ACTIVE_RUSH_EXPIRE_TIME.get(uuid);
        if (expireTime == null || now > expireTime) {
            // Not in active rush sequence — treat as standard instant short blink (3 blocks)
            performBlink(player, stack, 3.0, holdingBackward, false);
            return;
        }

        // We are in active rush!
        int dashCount = DASH_COUNT_MAP.getOrDefault(uuid, 0) + 1;
        DASH_COUNT_MAP.put(uuid, dashCount);

        boolean isThirdDash = (dashCount % 3 == 0); // Tweak A
        performBlink(player, stack, 5.0, holdingBackward, isThirdDash);
    }

    private static void performBlink(ServerPlayer player, ItemStack stack, double maxDistance, boolean holdingBackward, boolean isThirdDash) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 startPos = player.position();
        Vec3 look = player.getLookAngle();
        Vec3 dashDir = holdingBackward ? look.scale(-1.0) : look; // Tweak B: move backward if holding backward

        // Raycast to check ground or terrain
        double targetDist = maxDistance;
        Vec3 rayEnd = startPos.add(dashDir.scale(maxDistance + 1.0));
        BlockHitResult hit = level.clip(new ClipContext(startPos.add(0, 0.5, 0), rayEnd.add(0, 0.5, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        
        Vec3 destination;
        if (hit.getType() == HitResult.Type.BLOCK) {
            // Check if there is air / gap past the obstacle (phasing bypass)
            Vec3 pastObstacle = hit.getLocation().add(dashDir.scale(1.5));
            BlockPos pastPos = BlockPos.containing(pastObstacle);
            if (level.getBlockState(pastPos).isAir() && level.getBlockState(pastPos.above()).isAir()) {
                destination = pastObstacle;
            } else {
                // Land right in front of the wall
                destination = hit.getLocation().subtract(dashDir.scale(0.6));
            }
        } else {
            destination = startPos.add(dashDir.scale(targetDist));
        }

        // Ensure Y level is reasonable (find ground if close to ground)
        BlockPos checkGround = BlockPos.containing(destination);
        if (level.getBlockState(checkGround).isAir() && !level.getBlockState(checkGround.below()).isAir()) {
            destination = new Vec3(destination.x, checkGround.getY(), destination.z);
        }

        // Spawn Hollow Afterimage clone at starting position
        HollowAfterimageEntity afterimage = new HollowAfterimageEntity(level, player);
        level.addFreshEntity(afterimage);

        // Disappearing particles at origin
        level.sendParticles(
            ParticleTypes.PORTAL,
            startPos.x, startPos.y + 1.0, startPos.z,
            20, 0.3, 0.5, 0.3, 0.1
        );

        // Perform Teleport
        player.teleportTo(destination.x, destination.y, destination.z);

        // Grant Hollowed custom effect for 3s (60 ticks)
        player.addEffect(new MobEffectInstance(DbaEffects.HOLLOWED_HOLDER, 60, 0, false, false));

        // Damage all enemies along the teleport path & apply Dark Faded
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        float baseDmg = 450.0f + (float) (accessor.dba$getStrength() * 2.5);
        if (isThirdDash) baseDmg *= 1.8f;

        AABB pathBox = new AABB(startPos, destination).inflate(1.5, 1.0, 1.5);
        List<LivingEntity> enemiesPassed = level.getEntitiesOfClass(LivingEntity.class, pathBox, e -> e.isAlive() && e != player);

        for (LivingEntity enemy : enemiesPassed) {
            enemy.hurtServer(level, level.damageSources().mobAttack(player), baseDmg);
            enemy.addEffect(new MobEffectInstance(DbaEffects.DARK_FADED_HOLDER, 80, 0, false, true));
            enemy.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 80, 0, false, false, false), player);

            level.sendParticles(
                new DustParticleOptions(0x4B0082, 1.8F),
                enemy.getX(), enemy.getY() + 1.0, enemy.getZ(),
                12, 0.3, 0.5, 0.3, 0.05
            );
        }

        // Tweak A: Every 3rd dash unleashes a massive Void Slash wave at destination
        if (isThirdDash) {
            VoidSlashEntity slash = new VoidSlashEntity(level, player, baseDmg * 1.5f, level.getRandom().nextBoolean());
            slash.setDeltaMovement(look.scale(1.8));
            level.addFreshEntity(slash);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0f, 0.5f);
        }

        // Reappearing particles & sounds
        level.sendParticles(
            ParticleTypes.REVERSE_PORTAL,
            destination.x, destination.y + 1.0, destination.z,
            25, 0.4, 0.6, 0.4, 0.05
        );
        level.playSound(null, destination.x, destination.y, destination.z,
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 1.1f);
    }

    // --- RIGHT CLICK: Void Rift Channeling & Implosion ---

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = (ServerLevel) level;
            // Spawn / track Void Rift at crosshair location
            Vec3 eyePos = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 targetPos = eyePos.add(look.scale(12.0));

            BlockHitResult hit = level.clip(new ClipContext(eyePos, eyePos.add(look.scale(32.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                targetPos = hit.getLocation().add(0, 0.5, 0);
            }

            VoidRiftEntity rift = new VoidRiftEntity(serverLevel, serverPlayer, targetPos, 2.5f, 550.0f);
            serverLevel.addFreshEntity(rift);
            ACTIVE_RIFT_MAP.put(player.getUUID(), rift);
        }

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
            float chargeRatio = Math.min(1.0f, heldTicks / 100.0f); // 5s max rift growth

            VoidRiftEntity rift = ACTIVE_RIFT_MAP.get(player.getUUID());
            if (rift != null && rift.isAlive() && !rift.isImploding()) {
                // Tweak A: Rift follows crosshair while charging
                Vec3 eyePos = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                Vec3 targetPos = eyePos.add(look.scale(14.0));

                BlockHitResult hit = level.clip(new ClipContext(eyePos, eyePos.add(look.scale(48.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    targetPos = hit.getLocation().add(0, 0.5, 0);
                }

                // Smoothly lerp rift position to target crosshair
                Vec3 curPos = rift.position();
                Vec3 newPos = curPos.lerp(targetPos, 0.35);
                rift.setPos(newPos.x, newPos.y, newPos.z);

                // Enlarge rift radius (from 2.5 up to 9.0 blocks)
                rift.setRadius(2.5f + chargeRatio * 6.5f);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            int heldTicks = getUseDuration(stack, living) - timeLeft;
            VoidRiftEntity rift = ACTIVE_RIFT_MAP.remove(player.getUUID());

            if (rift != null && rift.isAlive()) {
                // Tweak C: Check if player is aiming towards the rift upon release to teleport inside
                Vec3 eyePos = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                Vec3 toRift = rift.position().subtract(eyePos);
                double dot = look.dot(toRift.normalize());

                if (dot > 0.85 && toRift.length() <= 40.0) {
                    // Teleport player directly into rift center!
                    player.teleportTo(rift.getX(), rift.getY(), rift.getZ());
                    // Grant Rifted custom status effect
                    player.addEffect(new MobEffectInstance(DbaEffects.RIFTED_HOLDER, 200, 0, false, true));
                    player.sendSystemMessage(Component.literal("§d✦ Teleported into Void Rift! [Rifted Active] ✦"), true);
                }

                // Trigger massive final implosion!
                rift.triggerImplosion();
            }

            player.getCooldowns().addCooldown(stack, 60);
        }
        return true;
    }
}
