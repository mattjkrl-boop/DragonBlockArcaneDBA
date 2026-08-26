package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.entity.AzureLightningEntity;
import com.dragonblockarcanedba.entity.AzureRushTrailEntity;
import com.dragonblockarcanedba.entity.AzureSonicQuakeEntity;
import com.dragonblockarcanedba.entity.AzureStormEntity;
import com.dragonblockarcanedba.entity.AzureTempestChannelEntity;
import com.dragonblockarcanedba.entity.AzureTornadoEntity;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Azure Dragon Sword — Wind, Flight & Tempest Manipulation Weapon.
 * 
 * LEFT: Azure Dragon Rush (Superman / Elytra Flight & Dragon Wind)
 * - Hold left click to fly freely with Elytra flight pose in look direction at high speed.
 * - Sneaking (Shift) slows down speed for precision (Tweak A).
 * - Leaves bright blue wind trails and blasts nearby enemies away.
 * - Fall damage immune during rush and on next landing.
 * - Landing / diving into ground creates Sonic Quake shockwave (Tweak B).
 * - Passing through enemies spawns miniature tornadoes launching them (Tweak C).
 * 
 * RIGHT: Call of the Tempest (Dragon Storm Domain)
 * - Hold right click to channel a massive storm domain (up to 15s / 300 ticks max charge).
 * - Causes rain, localized thunder, howling wind turbulence, and periodic cyan dragon lightning.
 * - Storm lasts up to 30s.
 * - Tweak A: Storm follows the player.
 * - Tweak B: Storm follows targeted enemy.
 * - Tweak C: At max charge, summons a giant tornado filled with lightning.
 */
public class AzureDragonSwordItem extends Item {
    public static final int MAX_TEMPEST_CHARGE_TICKS = 300; // 15 seconds

    // Track players currently rushing with Azure Dragon Sword
    public static final Map<UUID, Boolean> IS_RUSHING_MAP = new ConcurrentHashMap<>();
    // Track lock-on target UUID for Tweak B (using UUID to prevent object retention memory leaks)
    public static final Map<UUID, UUID> LOCKED_TARGET_MAP = new ConcurrentHashMap<>();

    public AzureDragonSwordItem(Properties properties) {
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
                    -2.0, // Swift fluid wind swings
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Physics: Aerial Dragon glide and springy wind landing
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("azure_dragon_air_drag"), -0.65, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("azure_dragon_bounciness"), 0.30, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    // --- LEFT CLICK: Azure Dragon Rush (Flight & Sonic Quake) ---

    public static final Map<UUID, Long> LAST_RUSH_START_TIME = new ConcurrentHashMap<>();
    public static final Map<UUID, Boolean> DOUBLE_RUSHING_MAP = new ConcurrentHashMap<>();

    public static void onDragonRushTick(ServerPlayer player, ItemStack stack, boolean isSneaking) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Double click detection
        boolean wasRushing = IS_RUSHING_MAP.getOrDefault(player.getUUID(), false);
        if (!wasRushing) {
            long now = System.currentTimeMillis();
            long lastStart = LAST_RUSH_START_TIME.getOrDefault(player.getUUID(), 0L);
            if (now - lastStart < 400) { // 400ms double click window
                DOUBLE_RUSHING_MAP.put(player.getUUID(), true);
            } else {
                DOUBLE_RUSHING_MAP.put(player.getUUID(), false);
            }
            LAST_RUSH_START_TIME.put(player.getUUID(), now);
        }

        boolean isDoubleRushing = DOUBLE_RUSHING_MAP.getOrDefault(player.getUUID(), false);

        // Drain Ki
        double maxKi = PlayerStats.getMaxKi(player);
        // Normal rush: 0 drain. Double Rush: 1% a sec.
        double drainPerTick = isDoubleRushing ? ((maxKi * 0.01) / 20.0) : 0.0;
        double currentKi = accessor.dba$getCurrentKi();

        if (drainPerTick > 0) {
            if (currentKi >= drainPerTick) {
                accessor.dba$addKi(-drainPerTick);
                accessor.dba$syncStats();
            } else {
                // If they run out of mana during double rush, drop from sky & 1 sec cooldown
                stopDragonRush(player);
                player.getCooldowns().addCooldown(stack, 20);
                return;
            }
        }

        IS_RUSHING_MAP.put(player.getUUID(), true);

        // Force Elytra / fall flying animation pose
        player.startFallFlying();

        // MC 26.2 Physics: Reduce air drag for hypersonic aerial flight
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
            player,
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
            com.dragonblockarcanedba.DragonBlockArcaneDBA.id("dragon_flight_drag"),
            isDoubleRushing ? -0.90 : -0.75,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        double movementMultiplier = com.dragonblockarcanedba.util.MovementLimiterHelper.getMovementMultiplier(player);
        if (movementMultiplier <= 0.0) {
            stopDragonRush(player);
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }

        Vec3 look = player.getLookAngle();
        // Speed: 1.85 normal, 0.75 if sneaking. Double rush = 3.7 (naturally scaled by all effects, attributes, & speed limiter)
        double baseSpeed = isDoubleRushing ? 3.7 : (isSneaking ? 0.75 : 1.85);
        double targetFlightSpeed = baseSpeed * movementMultiplier;
        Vec3 targetVelocity = look.scale(targetFlightSpeed);

        Vec3 currentVel = player.getDeltaMovement();

        // Natural Aerodynamic Thrust & Momentum Blending:
        // Smoothly accelerates towards target velocity while naturally preserving external forces (wind gusts, explosions, knockback).
        Vec3 velocity;
        if (currentVel.lengthSqr() < 0.01) {
            velocity = targetVelocity.scale(0.5);
        } else {
            Vec3 thrust = targetVelocity.subtract(currentVel).scale(0.40);
            velocity = currentVel.add(thrust);
        }

        player.setDeltaMovement(velocity);
        player.fallDistance = 0.0f;
        player.hurtMarked = true;

        // Physical 3D aerodynamic dragon wind trail and Mach shock cone
        Vec3 trailPos = player.position().add(0, 0.4, 0);
        AzureRushTrailEntity trail = new AzureRushTrailEntity(level, player, trailPos, player.getYRot(), player.getXRot(), isDoubleRushing, isDoubleRushing ? 1.5f : 1.0f);
        level.addFreshEntity(trail);

        // Pushes nearby enemies away and deals pass-through damage
        AABB rushBox = player.getBoundingBox().inflate(isDoubleRushing ? 4.0 : 2.5, 1.5, isDoubleRushing ? 4.0 : 2.5);
        List<LivingEntity> nearbyEnemies = level.getEntitiesOfClass(LivingEntity.class, rushBox, e -> e.isAlive() && e != player);

        for (LivingEntity enemy : nearbyEnemies) {
            // Wind push
            Vec3 push = enemy.position().subtract(player.position()).normalize().scale(isDoubleRushing ? 2.5 : 1.5).add(0, 0.4, 0);
            enemy.setDeltaMovement(enemy.getDeltaMovement().add(push));
            enemy.hurtMarked = true;

            // Damage
            float damage = (isDoubleRushing ? 600.0f : 350.0f) + (float) (accessor.dba$getStrength() * 2.0);
            enemy.hurtServer(level, level.damageSources().mobAttack(player), damage);

            // Tweak C: Passing through an enemy creates a miniature tornado that launches them
            if (level.getRandom().nextFloat() < (isDoubleRushing ? 0.8f : 0.4f)) {
                AzureTornadoEntity miniTornado = new AzureTornadoEntity(level, player, enemy.position(), 1.0f, false);
                level.addFreshEntity(miniTornado);
            }
        }

        // Check for ground impact while rushing -> Trigger Sonic Quake (Tweak B)
        if (player.onGround() || player.horizontalCollision) {
            triggerSonicQuake(player, stack);
            if (isDoubleRushing) {
                // "until u hit something" - ends double rush
                DOUBLE_RUSHING_MAP.put(player.getUUID(), false);
            }
        }

        // Flying sound hum
        if (player.tickCount % (isDoubleRushing ? 8 : 15) == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, isDoubleRushing ? 1.8f : 1.2f, isDoubleRushing ? 1.5f : 1.1f);
        }
    }

    public static void stopDragonRush(ServerPlayer player) {
        IS_RUSHING_MAP.remove(player.getUUID());
        DOUBLE_RUSHING_MAP.remove(player.getUUID());
        player.fallDistance = 0.0f; // Immune to fall damage
        player.stopFallFlying();
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.removeModifier(
            player,
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
            com.dragonblockarcanedba.DragonBlockArcaneDBA.id("dragon_flight_drag")
        );
    }

    public static void triggerSonicQuake(ServerPlayer player, ItemStack stack) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 pos = player.position();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        float quakeDamage = 450.0f + (float) (accessor.dba$getStrength() * 2.5);
        double radius = 8.0;

        AABB quakeBox = player.getBoundingBox().inflate(radius, 3.0, radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, quakeBox, e -> e.isAlive() && e != player && pos.distanceTo(e.position()) <= radius);

        for (LivingEntity target : targets) {
            target.hurtServer(level, level.damageSources().mobAttack(player), quakeDamage);
            // Launch high into the air with MC 26.2 elastic bounciness
            target.setDeltaMovement(target.getDeltaMovement().add(0, 1.4, 0));
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                target,
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                com.dragonblockarcanedba.DragonBlockArcaneDBA.id("sonic_quake_bounce"),
                0.85,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
            );
            target.hurtMarked = true;
        }

        // Physical 3D supersonic ground shockwave and kinetic shatter geometry
        AzureSonicQuakeEntity quake = new AzureSonicQuakeEntity(level, player, pos.add(0, 0.05, 0), (float) radius);
        level.addFreshEntity(quake);

        level.playSound(null, pos.x, pos.y, pos.z,
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.8f);
    }

    // --- RIGHT CLICK: Call of the Tempest (Dragon Storm Domain) ---

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
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
            float chargeRatio = Math.min(1.0f, heldTicks / (float) MAX_TEMPEST_CHARGE_TICKS);

            // Manage physical 3D tempest channeling wind tunnel entity
            List<AzureTempestChannelEntity> existingChannels = serverLevel.getEntitiesOfClass(
                AzureTempestChannelEntity.class,
                player.getBoundingBox().inflate(4.0),
                c -> c.getCasterId() == player.getId() && c.isAlive()
            );
            AzureTempestChannelEntity channel;
            if (existingChannels.isEmpty()) {
                channel = new AzureTempestChannelEntity(serverLevel, player);
                serverLevel.addFreshEntity(channel);
            } else {
                channel = existingChannels.get(0);
            }
            channel.setChargeRatio(chargeRatio);

            if (heldTicks % 20 == 0) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.5f, 0.5f + chargeRatio * 0.7f);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.0f, 0.8f + chargeRatio * 0.4f);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            ServerLevel serverLevel = (ServerLevel) level;
            int heldTicks = getUseDuration(stack, living) - timeLeft;

            // Discard tempest channeling entity
            List<AzureTempestChannelEntity> existingChannels = serverLevel.getEntitiesOfClass(
                AzureTempestChannelEntity.class,
                player.getBoundingBox().inflate(6.0),
                c -> c.getCasterId() == player.getId()
            );
            for (AzureTempestChannelEntity c : existingChannels) {
                c.discard();
            }

            if (heldTicks >= 10) {
                float chargeRatio = Math.min(1.0f, heldTicks / (float) MAX_TEMPEST_CHARGE_TICKS);
                float radius = 10.0f + (chargeRatio * 20.0f); // 10 to 30 blocks radius
                boolean isMaxCharge = (chargeRatio >= 0.95f);

                UUID targetUuid = LOCKED_TARGET_MAP.get(player.getUUID());
                LivingEntity target = null;
                if (targetUuid != null) {
                    net.minecraft.world.entity.Entity ent = serverLevel.getEntity(targetUuid);
                    if (ent instanceof LivingEntity le && le.isAlive()) {
                        target = le;
                    } else {
                        LOCKED_TARGET_MAP.remove(player.getUUID());
                    }
                }
                boolean followsPlayer = (target == null); // Tweak A (follows player) vs Tweak B (follows target)

                Vec3 spawnPos = target != null ? target.position() : player.position();
                AzureStormEntity storm = new AzureStormEntity(serverLevel, player, spawnPos, radius, followsPlayer, target, isMaxCharge);
                serverLevel.addFreshEntity(storm);

                player.sendSystemMessage(Component.literal("§b✦ Tempest Summoned! (" + (int) radius + "m domain) ✦"), true);

                // Sound
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 3.0f, 0.8f);

                player.getCooldowns().addCooldown(stack, 60 + (int) (chargeRatio * 60));
            }
        }
        return true;
    }

    public static void onPlayerDisconnect(UUID playerUuid) {
        IS_RUSHING_MAP.remove(playerUuid);
        DOUBLE_RUSHING_MAP.remove(playerUuid);
        LAST_RUSH_START_TIME.remove(playerUuid);
        LOCKED_TARGET_MAP.remove(playerUuid);
    }
}
