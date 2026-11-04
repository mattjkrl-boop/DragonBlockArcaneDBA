package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.entity.AzureRushTrailEntity;
import com.dragonblockarcanedba.entity.AzureSonicQuakeEntity;
import com.dragonblockarcanedba.entity.AzureStormEntity;
import com.dragonblockarcanedba.entity.AzureTempestChannelEntity;
import com.dragonblockarcanedba.entity.AzureTornadoEntity;
import com.dragonblockarcanedba.movement.MovementPhysics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
 * Azure Dragon Sword — Wind, Flight & Tempest Manipulation Weapon.
 * 
 * Ki Drain: 20% per minute for normal flight & Right Tempest ability; 40% per minute for Double Rush.
 */
public class AzureDragonSwordItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
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
    // Game tick of each player's last rush spawned mini tornado (proc rate limit).
    public static final Map<UUID, Long> LAST_MINI_TORNADO_TICK = new ConcurrentHashMap<>();
    // Game tick of each player's last Sonic Quake (impact rate limit).
    public static final Map<UUID, Long> LAST_QUAKE_TICK = new ConcurrentHashMap<>();

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Basic Azure melee was bare damage with no wind identity at all. A hit now kicks
        // up a gust at the victim, shoves them lightly along the swing, and sips Ki like
        // its sibling blades do. The gust is a paid effect: if the Ki drain fails the
        // plain sword hit still lands, but no wind rides on it.
        if (attacker instanceof Player player && !player.level().isClientSide()) {
            boolean paid = WeaponDrainHelper.drainKiDiscrete(player, 15.0, 7);
            if (!paid) return;
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.WIND,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    10, 0.4, 0.3, 0.4, 0.2);
            }
            Vec3 away = target.position().subtract(player.position());
            away = new Vec3(away.x, 0, away.z);
            if (away.lengthSqr() > 0.001) {
                away = away.normalize().scale(0.3);
                target.setDeltaMovement(target.getDeltaMovement().add(away.x, 0.12, away.z));
                com.dragonblockarcanedba.util.DbaCompat.markHurt(target);
            }
        }
    }

    public static void onDragonRushTick(ServerPlayer player, ItemStack stack, boolean isSneaking) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Double click detection, in game ticks (8 ticks = the old 400ms window).
        // Wall clock used to decide this, so a pause or a lag spike could turn a
        // fresh rush into a double rush or eat a real one.
        boolean wasRushing = IS_RUSHING_MAP.getOrDefault(player.getUUID(), false);
        if (!wasRushing) {
            long now = level.getGameTime();
            long lastStart = LAST_RUSH_START_TIME.getOrDefault(player.getUUID(), -1000L);
            if (now - lastStart < 8) { // 8 tick double click window
                DOUBLE_RUSHING_MAP.put(player.getUUID(), true);
            } else {
                DOUBLE_RUSHING_MAP.put(player.getUUID(), false);
            }
            LAST_RUSH_START_TIME.put(player.getUUID(), now);
        }

        boolean isDoubleRushing = DOUBLE_RUSHING_MAP.getOrDefault(player.getUUID(), false);

        // Drain Ki: 20% per min (normal) or 40% per min (double rush)
        double kiPercentPerMin = isDoubleRushing ? 40.0 : 20.0;
        if (!WeaponDrainHelper.drainKiPerTick(player, kiPercentPerMin)) {
            // Ran out of Ki, drop from sky & apply 1 sec cooldown
            stopDragonRush(player);
            player.getCooldowns().addCooldown(stack, 20);
            return;
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
            MovementPhysics.setVelocity(player, Vec3.ZERO);
            return;
        }

        Vec3 look = player.getLookAngle();
        // Speed: 1.85 normal, 0.75 if sneaking. Double rush = 3.7 (naturally scaled by all effects, attributes, & speed limiter)
        double baseSpeed = isDoubleRushing ? 3.7 : (isSneaking ? 0.75 : 1.85);
        double targetFlightSpeed = baseSpeed * movementMultiplier;
        Vec3 targetVelocity = look.scale(targetFlightSpeed);

        // Shared flight model (MovementPhysics): spool from rest, then blend toward
        // the target velocity, preserving momentum from wind, explosions, and knockback.
        MovementPhysics.applyFlightVelocity(player, targetVelocity, 0.40);
        player.fallDistance = 0.0f;

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
            com.dragonblockarcanedba.util.DbaCompat.markHurt(enemy);

            // Damage
            float damage = (isDoubleRushing ? 600.0f : 350.0f) + (float) (accessor.dba$getStrength() * 2.0);
            enemy.hurtServer(level, level.damageSources().mobAttack(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (damage)));
            // Pass-through hits were silent and weightless. Fresh full hits (invulnerableTime
            // just reset to 20) get a sweep sound and a spark burst at the victim.
            if (com.dragonblockarcanedba.util.DbaCompat.getInvulnerableTime(enemy) >= 20) {
                level.playSound(null, enemy.getX(), enemy.getY(), enemy.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.3f);
                level.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                    enemy.getX(), enemy.getY() + enemy.getBbHeight() * 0.5, enemy.getZ(),
                    6, 0.3, 0.3, 0.3, 0.15);
            }

            // Tweak C: Passing through an enemy creates a miniature tornado that launches them.
            // The roll used to run per enemy per rush tick, so one pass through a crowd
            // could stack several tornadoes at once. It now rolls only on a fresh hit
            // and at most once per 25 ticks for this rusher.
            boolean freshHit = com.dragonblockarcanedba.util.DbaCompat.getInvulnerableTime(enemy) >= 20;
            long lastProc = LAST_MINI_TORNADO_TICK.getOrDefault(player.getUUID(), -1000L);
            if (freshHit && level.getGameTime() - lastProc >= 25
                && level.getRandom().nextFloat() < (isDoubleRushing ? 0.8f : 0.4f)) {
                LAST_MINI_TORNADO_TICK.put(player.getUUID(), level.getGameTime());
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

        // The quake used to refire on every rush tick that touched ground or a
        // wall, free. It now fires at most once per 30 ticks and pays Ki: a
        // 60 percent per minute rate over 20 ticks, 1 percent of max Ki.
        long now = level.getGameTime();
        if (now - LAST_QUAKE_TICK.getOrDefault(player.getUUID(), -1000L) < 30) {
            return;
        }
        if (!WeaponDrainHelper.drainKiDiscrete(player, 60.0, 20)) {
            return;
        }
        LAST_QUAKE_TICK.put(player.getUUID(), now);

        float quakeDamage = 450.0f + (float) (accessor.dba$getStrength() * 2.5);
        double radius = 8.0;

        // Physical 3D supersonic ground shockwave and kinetic shatter geometry.
        // Spawned before the hit loop so every bounced victim registers with it:
        // the quake entity owns removing their bounce modifier when it expires.
        AzureSonicQuakeEntity quake = new AzureSonicQuakeEntity(level, player, pos.add(0, 0.05, 0), (float) radius);
        level.addFreshEntity(quake);

        AABB quakeBox = player.getBoundingBox().inflate(radius, 3.0, radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, quakeBox, e -> e.isAlive() && e != player && pos.distanceTo(e.position()) <= radius);

        for (LivingEntity target : targets) {
            // Damage falls off with distance: full at the center, half at the rim.
            // A target at 7.9 blocks used to take the same hit as one at zero.
            double distFrac = Math.min(1.0, pos.distanceTo(target.position()) / radius);
            float falloffDamage = quakeDamage * (float) (1.0 - 0.5 * distFrac);
            target.hurtServer(level, level.damageSources().mobAttack(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (falloffDamage)));
            // Launch high into the air with MC 26.2 elastic bounciness
            target.setDeltaMovement(target.getDeltaMovement().add(0, 1.4, 0));
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                target,
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                com.dragonblockarcanedba.DragonBlockArcaneDBA.id("sonic_quake_bounce"),
                0.85,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
            );
            quake.addBounceTarget(target.getUUID());
            com.dragonblockarcanedba.util.DbaCompat.markHurt(target);
        }

        level.playSound(null, pos.x, pos.y, pos.z,
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.8f);
        com.dragonblockarcanedba.fx.DbaImpactFx.heavy(level, pos.x, pos.y, pos.z);
    }

    // --- RIGHT CLICK: Call of the Tempest (Dragon Storm Domain) ---

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
        return ItemUseAnimation.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingTicks) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            // Drain Ki: 20% per minute for Tempest channeling
            if (!WeaponDrainHelper.drainKiPerTick(player, 20.0)) {
                player.stopUsingItem();
                return;
            }

            ServerLevel serverLevel = (ServerLevel) level;
            int heldTicks = getUseDuration(stack, living) - remainingTicks;
            float chargeRatio = Math.min(1.0f, heldTicks / (float) MAX_TEMPEST_CHARGE_TICKS);

            // Manage physical 3D tempest channeling wind tunnel entity. The search
            // box matches the release search (6.0); a narrower box here could miss
            // the live channel and spawn a second one. A channel that is already
            // collapsing after a release does not count as the active channel.
            List<AzureTempestChannelEntity> existingChannels = serverLevel.getEntitiesOfClass(
                AzureTempestChannelEntity.class,
                player.getBoundingBox().inflate(6.0),
                c -> c.getCasterId() == player.getId() && c.isAlive() && !c.isReleasing()
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
                    com.dragonblockarcanedba.sound.DbaSounds.LIGHTNING_ZAP, SoundSource.WEATHER, 1.5f, 0.5f + chargeRatio * 0.7f);
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

            // Collapse the tempest channel instead of discarding it in one tick:
            // it fades out over 8 ticks while the storm takes over.
            List<AzureTempestChannelEntity> existingChannels = serverLevel.getEntitiesOfClass(
                AzureTempestChannelEntity.class,
                player.getBoundingBox().inflate(6.0),
                c -> c.getCasterId() == player.getId()
            );
            for (AzureTempestChannelEntity c : existingChannels) {
                c.beginRelease();
            }

            if (heldTicks >= 10) {
                float chargeRatio = Math.min(1.0f, heldTicks / (float) MAX_TEMPEST_CHARGE_TICKS);
                float radius = 10.0f + (chargeRatio * 20.0f); // 10 to 30 blocks radius
                boolean isMaxCharge = (chargeRatio >= 0.95f);
                // Storm life scales with the charge spent: 120 ticks for a bare
                // release up to 340 at full charge. A 10 tick tap used to buy the
                // same flat 220 ticks as a 15 second channel.
                int lifeTicks = 120 + (int) (chargeRatio * 220.0f);

                UUID targetUuid = LOCKED_TARGET_MAP.get(player.getUUID());
                LivingEntity target = null;
                if (targetUuid != null) {
                    net.minecraft.world.entity.Entity ent = serverLevel.getEntity(targetUuid);
                    if (ent instanceof LivingEntity le && le.isAlive() && le.distanceToSqr(player) <= 64.0 * 64.0) {
                        target = le;
                    } else {
                        // Dead, removed, or long gone: the lock goes stale and the storm follows the player.
                        LOCKED_TARGET_MAP.remove(player.getUUID());
                    }
                }
                boolean followsPlayer = (target == null); // Tweak A (follows player) vs Tweak B (follows target)

                Vec3 spawnPos = target != null ? target.position() : player.position();
                AzureStormEntity storm = new AzureStormEntity(serverLevel, player, spawnPos, radius, followsPlayer, target, isMaxCharge, lifeTicks, chargeRatio);
                serverLevel.addFreshEntity(storm);

                player.sendSystemMessage(Component.literal("§b✦ Tempest Summoned! (" + (int) radius + "m domain) ✦"), true);

                // Sound and summon impact: a 30 block storm domain should land with weight.
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 3.0f, 0.8f);
                com.dragonblockarcanedba.fx.DbaImpactFx.heavy(serverLevel, spawnPos.x, spawnPos.y, spawnPos.z);

                // The cooldown outlives the storm by 100 ticks at every charge
                // level, so a second domain can never stack on the first. The old
                // 60 to 120 tick cooldown expired while a 220 tick storm still ran.
                player.getCooldowns().addCooldown(stack, lifeTicks + 100);
            }
        }
        return true;
    }

    public static void onPlayerDisconnect(UUID playerUuid) {
        IS_RUSHING_MAP.remove(playerUuid);
        DOUBLE_RUSHING_MAP.remove(playerUuid);
        LAST_RUSH_START_TIME.remove(playerUuid);
        LAST_MINI_TORNADO_TICK.remove(playerUuid);
        LAST_QUAKE_TICK.remove(playerUuid);
        LOCKED_TARGET_MAP.remove(playerUuid);
    }
}
