package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.entity.DarknessBladeEntity;
import com.dragonblockarcanedba.entity.DarknessWaveEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Darkness Sword (Dabura Sword) — Abyssal Darkness Weapon.
 * Overwhelming darkness manipulation, void slashes, enemy suppression, and a devastating eclipse domain.
 */
public class DaburaSwordItem extends Item {
    public static final int MAX_LEFT_CHARGE_TICKS = 160; // 8 seconds
    public static final int MAX_RIGHT_CHANNEL_TICKS = 300; // 15 seconds

    public DaburaSwordItem(Properties properties) {
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
                        -1.8, // Fluid dark heavy swings
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .build()
        ));
    }

    // --- LEFT CLICK: Abyssal Slash (Charging & Crescent Wave) ---

    public static void onLeftClickChargeTick(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Drain Ki: ~2.5% max Ki per second
        double maxKi = PlayerStats.getMaxKi(player);
        double drainPerTick = (maxKi * 0.025) / 20.0;
        double currentKi = accessor.dba$getCurrentKi();

        if (currentKi >= drainPerTick) {
            accessor.dba$addKi(-drainPerTick);
            if (chargeTicks % 5 == 0) {
                accessor.dba$syncStats();
            }
        } else {
            onLeftClickRelease(player, stack, chargeTicks);
            return;
        }

        // Slow player while charging
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 2, false, false));

        float chargeRatio = Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS);

        // Surrounding black/purple darkness particles converging onto the sword
        int particleCount = 2 + (int) (chargeRatio * 8);
        for (int i = 0; i < particleCount; i++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2;
            double dist = 1.2 + (1.0 - chargeRatio) * 1.5;
            double px = player.getX() + Math.cos(angle) * dist;
            double pz = player.getZ() + Math.sin(angle) * dist;
            double py = player.getY() + 0.5 + level.getRandom().nextDouble() * 1.2;

            level.sendParticles(
                new DustParticleOptions(0x1A0033, 1.6F + chargeRatio * 0.6F),
                px, py, pz,
                1, (player.getX() - px) * 0.1, (player.getY() + 0.8 - py) * 0.1, (player.getZ() - pz) * 0.1, 0.05
            );
        }

        if (chargeTicks % 20 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.4f + chargeRatio * 0.5f);
        }
    }

    public static void onLeftClickRelease(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

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

        // Tweak A: Fully charged slash creates 2 additional delayed darkness waves
        if (chargeRatio >= 0.95f) {
            for (int i = 1; i <= 2; i++) {
                final int delay = i * 4; // 4 and 8 ticks delay
                final float subDmg = totalDamage * 0.5f;
                level.getServer().execute(() -> {
                    // Spawn trailing waves slightly behind
                    DarknessWaveEntity subWave = new DarknessWaveEntity(level, player, subDmg, true);
                    subWave.setPos(player.getX() - look.x * 0.5 * delay, player.getY() + 1.0, player.getZ() - look.z * 0.5 * delay);
                    subWave.setDeltaMovement(look.x * speed * 0.9, look.y * speed * 0.9, look.z * speed * 0.9);
                    level.addFreshEntity(subWave);
                });
            }
        }

        // Downward swing audio & particles
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8f, 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 1.2f);

        int cooldown = 20 + (int) (chargeRatio * 40);
        player.getCooldowns().addCooldown(stack, cooldown);
    }

    // --- RIGHT CLICK: World of Darkness & Abyssal Eclipse Execution ---

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
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingTicks) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            ServerLevel serverLevel = (ServerLevel) level;
            int heldTicks = getUseDuration(stack, living) - remainingTicks;
            float chargeRatio = Math.min(1.0f, heldTicks / (float) MAX_RIGHT_CHANNEL_TICKS);

            // Expanding domain radius from 8 to 30 blocks (Tweak A: follows player)
            double domainRadius = 8.0 + (chargeRatio * 22.0);

            // Swirling black/purple darkness particles across the domain
            for (int i = 0; i < (int) (8 + chargeRatio * 16); i++) {
                double angle = serverLevel.getRandom().nextDouble() * Math.PI * 2;
                double r = serverLevel.getRandom().nextDouble() * domainRadius;
                double px = player.getX() + Math.cos(angle) * r;
                double pz = player.getZ() + Math.sin(angle) * r;
                double py = player.getY() + serverLevel.getRandom().nextDouble() * 4.0;

                serverLevel.sendParticles(
                    new DustParticleOptions(0x1A0033, 2.0f),
                    px, py, pz,
                    1, -Math.sin(angle) * 0.3, 0.05, Math.cos(angle) * 0.3, 0.02
                );
                if (i % 2 == 0) {
                    serverLevel.sendParticles(
                        new DustParticleOptions(0x3B0066, 1.6f),
                        px, py, pz,
                        1, 0, 0.05, 0, 0.01
                    );
                }
            }

            // Continuous debuffs on enemies inside domain
            AABB domainBox = player.getBoundingBox().inflate(domainRadius, 8.0, domainRadius);
            List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(
                LivingEntity.class, domainBox,
                e -> e.isAlive() && e != player && player.distanceTo(e) <= domainRadius
            );

            for (LivingEntity enemy : enemies) {
                enemy.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, true));
                enemy.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0, false, true));
                enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 3, false, true));
                enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, true));
            }

            // Tweak B: Slow / suspend hostile enemy projectiles entering domain
            List<Projectile> projectiles = serverLevel.getEntitiesOfClass(
                Projectile.class, domainBox,
                p -> p.isAlive() && p.getOwner() != player && player.distanceTo(p) <= domainRadius
            );
            for (Projectile p : projectiles) {
                p.setDeltaMovement(p.getDeltaMovement().scale(0.05));
                p.hurtMarked = true;
            }

            // Sound cue
            if (heldTicks % 30 == 0) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 0.9f, 0.5f + chargeRatio * 0.4f);
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
            enemy.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 1, false, false));
            enemy.addEffect(new MobEffectInstance(
                com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 60, 0, false, false, false
            ), player);
            enemy.hurtServer(level, level.damageSources().playerAttack(player), burstDamage);
        }

        // Spawn falling darkness blades raining from the sky across domain
        for (int i = 0; i < 10; i++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2;
            double r = level.getRandom().nextDouble() * (radius * 0.8);
            double bx = player.getX() + Math.cos(angle) * r;
            double bz = player.getZ() + Math.sin(angle) * r;
            double by = player.getY() + 18.0 + level.getRandom().nextDouble() * 5.0;

            DarknessBladeEntity blade = new DarknessBladeEntity(level, player, new Vec3(bx, by, bz), 350.0f);
            level.addFreshEntity(blade);
        }

        // Enormous dark eclipse explosion burst & sound
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1.0, player.getZ(), 5, 1.0, 0.5, 1.0, 0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.5f, 0.5f);

        // 5-second cooldown
        player.getCooldowns().addCooldown(stack, 100);
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            int heldTicks = getUseDuration(stack, living) - timeLeft;
            if (heldTicks < MAX_RIGHT_CHANNEL_TICKS) {
                player.getCooldowns().addCooldown(stack, 40);
            }
        }
        return true;
    }
}
