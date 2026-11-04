package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.BoltEntity;
import com.dragonblockarcanedba.entity.DarknessBladeEntity;
import com.dragonblockarcanedba.movement.MovementPhysics;
import com.dragonblockarcanedba.entity.DarknessChargeEntity;
import com.dragonblockarcanedba.entity.DarknessDomainEntity;
import com.dragonblockarcanedba.entity.DarknessShatterEntity;
import com.dragonblockarcanedba.entity.DarknessWaveEntity;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.dragonblockarcanedba.util.AtmosphereServer;
import com.dragonblockarcanedba.util.DbaDamage;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.LayeredParticles;
import com.dragonblockarcanedba.util.PermanentDestruction;
import com.dragonblockarcanedba.util.VortexPhysics;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.dragonblockarcanedba.util.WeaponDrainHelper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Darkness Sword (Dabura Sword): Abyssal Darkness Weapon.
 * 
 * Ki Drain: 35% per minute (smooth).
 */
public class DaburaSwordItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
    public static final int MAX_LEFT_CHARGE_TICKS = 160; // 8 seconds
    public static final int MAX_RIGHT_CHANNEL_TICKS = 300; // 15 seconds

    /** Demon violet theme color for dust bursts and bolt strikes. */
    private static final int THEME_ARGB = 0xA605F2;
    private static final int BOLT_ARGB = 0xFFA605F2;
    /** Dark stone palette for eclipse debris: void shards, not torn up dirt. */
    private static final java.util.List<net.minecraft.world.level.block.state.BlockState> DEBRIS_PALETTE = java.util.List.of(
        Blocks.OBSIDIAN.defaultBlockState(),
        Blocks.BLACKSTONE.defaultBlockState(),
        Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
        Blocks.BASALT.defaultBlockState()
    );

    private static final Map<UUID, DarknessChargeEntity> ACTIVE_CHARGE_MAP = new ConcurrentHashMap<>();

    public DaburaSwordItem(Properties properties) {
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
                    -2.4, // Standard two-handed blade speed
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Stealth: Darkness shrouds player nameplate
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.NAME_PLATE_DIST_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("dabura_nameplate_stealth"), -64.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("dabura_heavy_friction"), 0.50, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !player.level().isClientSide()) {
            WeaponDrainHelper.drainKiDiscrete(player, 35.0, 10);
            // A basic hit from the petrification sword should petrify, briefly.
            target.addEffect(new MobEffectInstance(DbaEffects.PETRIFICATION_CURSE_HOLDER, 40, 0, false, true), player);
        }
    }

    // --- LEFT CLICK: Abyssal Slash (Charging & Crescent Wave) ---

    public static void onLeftClickChargeTick(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Drain Ki: 35% per minute (smooth)
        if (!WeaponDrainHelper.drainKiPerTick(player, 35.0)) {
            onLeftClickRelease(player, stack, chargeTicks);
            return;
        }
        if (chargeTicks % 5 == 0) {
            accessor.dba$syncStats();
        }

        // Slow player while charging (shared movement layer)
        MovementPhysics.dampenHorizontal(player, 0.4);

        float chargeRatio = Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS);

        // Spawn / update physical 3D Darkness Charge Entity (no particle spam)
        DarknessChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.get(player.getUUID());
        if (chargeEntity == null || !chargeEntity.isAlive()) {
            chargeEntity = new DarknessChargeEntity(level, player);
            level.addFreshEntity(chargeEntity);
            ACTIVE_CHARGE_MAP.put(player.getUUID(), chargeEntity);
        }
        chargeEntity.setChargeRatio(chargeRatio);

        if (chargeTicks % 20 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                DbaSounds.CHARGE_HUM, SoundSource.PLAYERS, 0.8f, 0.4f + chargeRatio * 0.5f);
        }
    }

    public static void onLeftClickRelease(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Discard active charge entity
        DarknessChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.remove(player.getUUID());
        if (chargeEntity != null && chargeEntity.isAlive()) {
            chargeEntity.discard();
        }

        float chargeRatio = Math.max(0.1f, Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS));

        // Damage formula: 400.0 + (chargeRatio * 800.0) + (Strength * 3.0 * chargeRatio)
        float baseDmg = 400.0f + (chargeRatio * 800.0f);
        float strBonus = (float) (accessor.dba$getStrength() * 3.0f * chargeRatio);
        float totalDamage = baseDmg + strBonus;

        Vec3 look = player.getLookAngle();
        double speed = 1.6 + (chargeRatio * 1.0);

        // Primary darkness wave
        DarknessWaveEntity primaryWave = new DarknessWaveEntity(level, player, totalDamage, false);
        primaryWave.setDeltaMovement(look.x * speed, look.y * speed, look.z * speed);
        level.addFreshEntity(primaryWave);

        // Tweak A: Fully charged slash creates 2 additional trailing darkness waves.
        // The old code "delayed" them with server.execute, which runs next tick for both,
        // so all three waves left together. They now spawn together but fly slower
        // (0.78x and 0.58x), so the sequence actually arrives staggered.
        if (chargeRatio >= 0.95f) {
            double[] speedFactors = {0.78, 0.58};
            for (int i = 0; i < speedFactors.length; i++) {
                DarknessWaveEntity subWave = new DarknessWaveEntity(level, player, totalDamage * 0.5f, true);
                double back = 1.5 * (i + 1);
                subWave.setPos(player.getX() - look.x * back, player.getY() + 1.0, player.getZ() - look.z * back);
                double subSpeed = speed * speedFactors[i];
                subWave.setDeltaMovement(look.x * subSpeed, look.y * subSpeed, look.z * subSpeed);
                level.addFreshEntity(subWave);
            }
        }

        // Downward swing audio
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.SWING_HEAVY, SoundSource.PLAYERS, 1.8f, 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.RELEASE_DARK, SoundSource.PLAYERS, 1.2f, 1.2f);
        com.dragonblockarcanedba.fx.DbaImpactFx.heavy(level, player.getX(), player.getY(), player.getZ());

        // Base dust where the slash leaves the caster (point 3).
        LayeredParticles.emitGroundBurst(level, player.position(), 2.5f, THEME_ARGB);

        int cooldown = 20 + (int) (chargeRatio * 40);
        player.getCooldowns().addCooldown(stack, cooldown);
    }

    // --- RIGHT CLICK: World of Darkness & Abyssal Eclipse Execution ---

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
            // Drain Ki: 35% per minute
            if (!WeaponDrainHelper.drainKiPerTick(player, 35.0)) {
                player.stopUsingItem();
                return;
            }

            ServerLevel serverLevel = (ServerLevel) level;
            int heldTicks = getUseDuration(stack, living) - remainingTicks;
            float chargeRatio = Math.min(1.0f, heldTicks / (float) MAX_RIGHT_CHANNEL_TICKS);

            // Expanding domain radius from 8 to 30 blocks (follows player)
            double domainRadius = 8.0 + (chargeRatio * 22.0);

            // Manage Physical 3D Darkness Domain Entity (Volumetric Void Dome & Whirlpools).
            // A domain that is already dissipating is left to die; a fresh
            // channel raises a fresh dome beside it.
            List<DarknessDomainEntity> existingDomains = serverLevel.getEntitiesOfClass(
                DarknessDomainEntity.class,
                player.getBoundingBox().inflate(6.0),
                d -> d.getCasterId() == player.getId() && d.isAlive() && !d.isTearingDown()
            );
            DarknessDomainEntity domain;
            if (existingDomains.isEmpty()) {
                domain = new DarknessDomainEntity(serverLevel, player, player.position(), (float) domainRadius);
                serverLevel.addFreshEntity(domain);
            } else {
                domain = existingDomains.get(0);
                domain.setRadius((float) domainRadius);
                domain.refreshLifetime();
            }

            // Continuous debuffs on enemies inside domain: Petrification Curse
            AABB domainBox = player.getBoundingBox().inflate(domainRadius, 8.0, domainRadius);
            List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(
                LivingEntity.class, domainBox,
                e -> e.isAlive() && e != player && player.distanceTo(e) <= domainRadius
            );

            for (LivingEntity enemy : enemies) {
                enemy.addEffect(new MobEffectInstance(DbaEffects.PETRIFICATION_CURSE_HOLDER, 40, 0, false, true));
            }

            // Slow / suspend hostile enemy projectiles entering domain
            List<Projectile> projectiles = serverLevel.getEntitiesOfClass(
                Projectile.class, domainBox,
                p -> p.isAlive() && p.getOwner() != player && player.distanceTo(p) <= domainRadius
            );
            for (Projectile p : projectiles) {
                p.setDeltaMovement(p.getDeltaMovement().scale(0.05));
                com.dragonblockarcanedba.util.DbaCompat.markHurt(p);
            }

            // The vortex is a force now, not just a picture (point 3):
            // everything inside is dragged toward the center, swirled, and
            // lifted near the core, growing stronger as the domain grows.
            // Weight classes apply inside VortexPhysics, so bosses barely
            // move and players keep most of their control.
            VortexPhysics.applyVortex(serverLevel, player.position(), domainRadius,
                Math.min(26.0, domainRadius * 1.35), player,
                0.055, 0.85, 0.05, 0.35f + 0.65f * chargeRatio);

            // Point 4: the domain IS the eclipse. The shared atmosphere
            // system runs a custom storm volume (its own rain, thunder, and
            // gloom grade, never vanilla weather) for the channel's life,
            // refreshed in steps so the broadcast stays alive; when the
            // channel ends the volume simply expires.
            if (heldTicks % 20 == 0) {
                AtmosphereServer.begin(serverLevel, player.position(),
                    (float) domainRadius + 14.0f, 0.85f, AtmosphereServer.Kind.THUNDER, 45);
            }

            // Sound cue
            if (heldTicks % 30 == 0) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    DbaSounds.RIFT_AMBIENT, SoundSource.PLAYERS, 0.9f, 0.5f + chargeRatio * 0.4f);
            }

            // Max Charge Trigger: Abyssal Eclipse (at 15s / 300 ticks)
            if (heldTicks == MAX_RIGHT_CHANNEL_TICKS) {
                triggerAbyssalEclipse(player, stack, enemies, domainRadius);
                player.stopUsingItem();
            }
        }
    }

    private static void triggerAbyssalEclipse(ServerPlayer player, ItemStack stack, List<LivingEntity> enemies, double radius) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        float burstDamage = 1200.0f + (float) (accessor.dba$getStrength() * 4.0f);

        // Suspend enemies in mid-air & apply cinematic lock
        for (LivingEntity enemy : enemies) {
            enemy.setDeltaMovement(0, 0.4, 0);
            enemy.addEffect(new MobEffectInstance(DbaEffects.PETRIFICATION_CURSE_HOLDER, 60, 1, false, false));
            enemy.addEffect(new MobEffectInstance(
                DbaEffects.CINEMATIC_TRACKING_HOLDER, 60, 0, false, false, false
            ), player);
            enemy.hurtServer(level, DbaDamage.darkness(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (burstDamage)));
        }

        // Blade rain scales with the domain it falls across: a flat 10
        // blades read empty in a radius 30 dome. 350 damage each, unchanged.
        spawnBladeRain(player, radius, bladeCountFor(radius, 1.0f));

        // Physical 3D Void Shatter Shockwave & Fissure Eruption
        DarknessShatterEntity shatter = new DarknessShatterEntity(
            level, player, player.position().add(0, 0.05, 0), (float) Math.min(12.0, radius * 0.5)
        );
        level.addFreshEntity(shatter);

        // World interaction at the peak: the eclipse converts the ground it
        // stands on to permanent obsidian (reduced disk under the new rule),
        // eats a crater at the domain center, throws the domain's orbiting
        // debris outward to rain down, and rings itself with real strikes.
        BlockPos ground = findGround(level, player.position());
        if (ground != null) {
            PermanentDestruction.convertDisk(level, ground, (int) Math.min(8.0, radius * 0.5),
                Blocks.OBSIDIAN.defaultBlockState(), 0.45f, level.getRandom());
            PermanentDestruction.crater(level, ground, 2.0, 0.5, false,
                PermanentDestruction.TIER_WEAK, 60, level.getRandom());
        }
        LayeredParticles.emitGroundBurst(level, player.position(),
            (float) Math.min(12.0, radius * 0.5), THEME_ARGB);
        DebrisSpawner.spawnField(level, player, player.position(), 48, radius * 0.7, 9.0, 90, DEBRIS_PALETTE);
        for (int i = 0; i < 5; i++) {
            double angle = (i / 5.0) * Math.PI * 2.0 + level.getRandom().nextDouble() * 0.6;
            double r = radius * 0.75;
            BoltEntity bolt = new BoltEntity(level, new Vec3(
                player.getX() + Math.cos(angle) * r, player.getY(), player.getZ() + Math.sin(angle) * r),
                level.getRandom().nextInt(), BOLT_ARGB, 16.0f, 8);
            bolt.setOwner(player);
            level.addFreshEntity(bolt);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.BOOM_SONIC, SoundSource.PLAYERS, 2.5f, 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.WIND_GUST, SoundSource.PLAYERS, 2.5f, 0.5f);
        com.dragonblockarcanedba.fx.DbaImpactFx.heavy(level, player.getX(), player.getY(), player.getZ());

        // 8-second cooldown: the domain lived 15 seconds, and the raised
        // spectacle (scar, debris, strikes) needs room before it repeats.
        player.getCooldowns().addCooldown(stack, 160);
    }

    /** Eclipse blade count for a domain radius, scaled by the charge fraction paid. */
    private static int bladeCountFor(double radius, float scale) {
        return Math.max(4, Math.round((10 + (int) (radius * 0.5)) * scale));
    }

    /** Darkness blades raining from 18 to 23 blocks up across the domain. */
    private static void spawnBladeRain(ServerPlayer player, double radius, int count) {
        ServerLevel level = (ServerLevel) player.level();
        for (int i = 0; i < count; i++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2;
            double r = level.getRandom().nextDouble() * (radius * 0.8);
            double bx = player.getX() + Math.cos(angle) * r;
            double bz = player.getZ() + Math.sin(angle) * r;
            double by = player.getY() + 18.0 + level.getRandom().nextDouble() * 5.0;

            DarknessBladeEntity blade = new DarknessBladeEntity(level, player, new Vec3(bx, by, bz), 350.0f);
            level.addFreshEntity(blade);
        }
    }

    /** First solid block at or below a position, scanning a few blocks down. Null when there is no floor. */
    private static BlockPos findGround(ServerLevel level, Vec3 center) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int cx = (int) Math.floor(center.x), cz = (int) Math.floor(center.z);
        for (int dy = 2; dy >= -6; dy--) {
            m.set(cx, (int) Math.floor(center.y) + dy, cz);
            if (!level.getBlockState(m).isAir()) {
                return m.immutable();
            }
        }
        return null;
    }

    /** End the player's domain gracefully: the dome dissipates over its teardown instead of popping out. */
    private static void endDomain(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        List<DarknessDomainEntity> domains = level.getEntitiesOfClass(
            DarknessDomainEntity.class,
            player.getBoundingBox().inflate(40.0),
            d -> d.getCasterId() == player.getId() && d.isAlive() && !d.isTearingDown()
        );
        for (DarknessDomainEntity d : domains) {
            d.beginTeardown();
        }
    }

    /** A channel released at two thirds charge or more still detonates, weakened. */
    private static void triggerPartialEclipse(ServerPlayer player, float chargeRatio) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        double radius = 8.0 + chargeRatio * 22.0;
        float burstDamage = (1200.0f + (float) (accessor.dba$getStrength() * 4.0f)) * 0.6f * chargeRatio;

        java.util.List<LivingEntity> enemies = level.getEntitiesOfClass(
            LivingEntity.class, player.getBoundingBox().inflate(radius),
            e -> e.isAlive() && e != player && e.distanceToSqr(player) <= radius * radius
        );
        for (LivingEntity enemy : enemies) {
            enemy.setDeltaMovement(0, 0.4, 0);
            enemy.addEffect(new MobEffectInstance(DbaEffects.PETRIFICATION_CURSE_HOLDER, 40, 1, false, false));
            enemy.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 40, 0, false, false, false), player);
            enemy.hurtServer(level, DbaDamage.darkness(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (burstDamage)));
        }

        // The unfinished eclipse still rains blades, scaled by what was
        // paid: the full eclipse's signature visual is no longer all or
        // nothing at exactly tick 300.
        spawnBladeRain(player, radius, bladeCountFor(radius, chargeRatio));

        DarknessShatterEntity shatter = new DarknessShatterEntity(
            level, player, player.position().add(0, 0.05, 0), (float) Math.min(12.0, radius * 0.5)
        );
        level.addFreshEntity(shatter);

        BlockPos ground = findGround(level, player.position());
        if (ground != null) {
            PermanentDestruction.convertDisk(level, ground, (int) Math.min(6.0, radius * 0.4),
                Blocks.OBSIDIAN.defaultBlockState(), 0.45f, level.getRandom());
            PermanentDestruction.crater(level, ground, 2.0, 0.5, false,
                PermanentDestruction.TIER_WEAK, 60, level.getRandom());
        }
        LayeredParticles.emitGroundBurst(level, player.position(),
            (float) Math.min(8.0, radius * 0.4), THEME_ARGB);
        DebrisSpawner.spawnField(level, player, player.position(), 24, radius * 0.6, 7.0, 70, DEBRIS_PALETTE);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.BOOM_SONIC, SoundSource.PLAYERS, 2.0f, 0.7f);
        com.dragonblockarcanedba.fx.DbaImpactFx.heavy(level, player.getX(), player.getY(), player.getZ());
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§5✦ Abyssal Eclipse (unfinished) ✦"), true);
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            int heldTicks = getUseDuration(stack, living) - timeLeft;
            // Whatever ended the channel, the dome dissipates gracefully
            // instead of vanishing the tick the refresh stops.
            endDomain(player);
            if (heldTicks >= 200 && heldTicks < MAX_RIGHT_CHANNEL_TICKS) {
                // Two thirds of a 15 second channel used to pay out exactly nothing.
                triggerPartialEclipse(player, heldTicks / (float) MAX_RIGHT_CHANNEL_TICKS);
                player.getCooldowns().addCooldown(stack, 100);
            } else if (heldTicks < MAX_RIGHT_CHANNEL_TICKS) {
                player.getCooldowns().addCooldown(stack, 40);
            }
        }
        return true;
    }

    public static void onPlayerDisconnect(UUID playerUuid) {
        DarknessChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.remove(playerUuid);
        if (chargeEntity != null && chargeEntity.isAlive()) {
            chargeEntity.discard();
        }
    }
}
