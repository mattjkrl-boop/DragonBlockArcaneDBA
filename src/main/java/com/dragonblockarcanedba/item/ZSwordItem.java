package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.ZShockwaveEntity;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Z-Sword — Divine Heavy Weapon.
 * 
 * LEFT: Z Shockwave (Charge-to-Fire)
 * - Hold left click to channel power (up to 15s / 300 ticks).
 * - Drains ~5% Ki/sec, slows movement.
 * - Release sends gigantic horizontal golden energy wave.
 * - Recoil knocks player backward based on charge.
 * - Tweak A: Spawns trailing slower sub-waves at max charge.
 * - Tweak B: Ignores terrain and passes through blocks at max charge.
 * - Tweak C: Roots enemies 2 blocks into the ground, dealing head crush damage.
 * 
 * RIGHT: Katchin Weight (Divine Heavy Stance & Gravity Slam)
 * - Hold right click: Knockback immunity, near-total movement restriction, 80% damage reduction.
 * - Accumulates Weight Power with gravitational particles (Tweak B).
 * - Release unleashes massive circular gravity shockwave.
 * - Tweak A: Completely roots enemies for 2s, then violently launches them up and outward.
 */
public class ZSwordItem extends Item {
    public static final int MAX_LEFT_CHARGE_TICKS = 300; // 15 seconds

    public ZSwordItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    899.0, // 1 + 899 = 900 base damage
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -3.2, // Extremely heavy swing feel
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

    // --- LEFT CLICK: Z Shockwave Charging & Firing ---

    public static void onLeftClickChargeTick(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Drain Ki: ~5% max Ki per second (0.25% per tick)
        double maxKi = PlayerStats.getMaxKi(player);
        double drainPerTick = (maxKi * 0.05) / 20.0;
        double currentKi = accessor.dba$getCurrentKi();

        if (currentKi >= drainPerTick) {
            accessor.dba$addKi(-drainPerTick);
            accessor.dba$syncStats();
        } else {
            // Out of Ki! Force fire the shockwave with accumulated charge
            onLeftClickRelease(player, stack, chargeTicks);
            return;
        }

        // Heavy movement slowdown & grounding while charging
        player.addEffect(new MobEffectInstance(DbaEffects.ANCIENT_WEIGHT_HOLDER, 10, 0, false, false));

        // Visual charge effects: Golden spiraling dust into the sword
        float chargeRatio = Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS);
        int particleCount = 2 + (int) (chargeRatio * 8);

        for (int i = 0; i < particleCount; i++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2;
            double dist = 1.5 + (1.0 - chargeRatio) * 2.0;
            double px = player.getX() + Math.cos(angle) * dist;
            double pz = player.getZ() + Math.sin(angle) * dist;
            double py = player.getY() + 0.5 + level.getRandom().nextDouble() * 1.5;

            level.sendParticles(
                new DustParticleOptions(0xFFD700, 1.6F + chargeRatio * 0.8F),
                px, py, pz,
                1, (player.getX() - px) * 0.1, (player.getY() + 1.0 - py) * 0.1, (player.getZ() - pz) * 0.1, 0.08
            );
        }

        // Audio hum
        if (chargeTicks % 20 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.6f + chargeRatio * 0.8f);
        }
    }

    public static void onLeftClickRelease(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        float chargeRatio = Math.max(0.1f, Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS));

        // Stat-scaled damage: 300 base scaling up to 1200 + (Strength * 3.5)
        float baseDmg = 300.0f + (chargeRatio * 900.0f);
        float strengthBonus = (float) (accessor.dba$getStrength() * 3.5 * chargeRatio);
        float totalDamage = baseDmg + strengthBonus;

        // Spawn massive horizontal shockwave
        ZShockwaveEntity wave = new ZShockwaveEntity(level, player, chargeRatio, totalDamage, false);
        Vec3 look = player.getLookAngle();
        double speed = 1.4 + (chargeRatio * 1.4);
        wave.setDeltaMovement(look.x * speed, look.y * speed, look.z * speed);
        level.addFreshEntity(wave);

        // Player recoil: Launched backwards depending on charge
        Vec3 recoil = look.scale(-0.4 - (chargeRatio * 1.4));
        player.setDeltaMovement(player.getDeltaMovement().add(recoil.x, 0.15 + (chargeRatio * 0.2), recoil.z));
        player.hurtMarked = true;

        // Sounds & shockwave burst
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 1.2f);

        // Cooldown based on power
        int cooldown = 20 + (int) (chargeRatio * 40);
        player.getCooldowns().addCooldown(stack, cooldown);
    }

    // --- RIGHT CLICK: Katchin Weight (Divine Heavy Stance & Gravity Slam) ---

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
            float stanceRatio = Math.min(1.0f, heldTicks / 200.0f); // 10s max power

            // 1. Knockback immunity & movement lock
            Vec3 vel = player.getDeltaMovement();
            player.setDeltaMovement(0.0, vel.y < 0 ? vel.y : 0.0, 0.0);
            player.hurtMarked = true;

            // 2. Heavy divine damage reduction and grounding (Ancient Weight)
            player.addEffect(new MobEffectInstance(DbaEffects.ANCIENT_WEIGHT_HOLDER, 10, 1, false, false));

            // 3. Tweak B: Gravitational particles and screen atmosphere
            if (heldTicks % 2 == 0) {
                double radius = 2.0 + (stanceRatio * 4.0);
                for (int i = 0; i < (int) (6 + stanceRatio * 12); i++) {
                    double angle = serverLevel.getRandom().nextDouble() * Math.PI * 2;
                    double px = player.getX() + Math.cos(angle) * radius;
                    double pz = player.getZ() + Math.sin(angle) * radius;

                    // Purple/Black gravity suction pulling inward
                    serverLevel.sendParticles(
                        new DustParticleOptions(0x4B0082, 1.8F), // Indigo
                        px, player.getY() + 0.1, pz,
                        1, (player.getX() - px) * 0.15, 0.05, (player.getZ() - pz) * 0.15, 0.05
                    );
                }
            }

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

            if (heldTicks >= 10) {
                float powerRatio = Math.min(1.0f, heldTicks / 200.0f);
                double aoeRadius = 6.0 + (powerRatio * 12.0); // 6 to 18 blocks

                PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
                float baseSlamDamage = 400.0f + (powerRatio * 800.0f);
                float strengthBonus = (float) (accessor.dba$getStrength() * 3.0 * powerRatio);
                float totalSlamDamage = baseSlamDamage + strengthBonus;

                // Circular shockwave AOE
                AABB aoe = player.getBoundingBox().inflate(aoeRadius, 4.0, aoeRadius);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, aoe, e -> e.isAlive() && e != player);

                java.util.List<LivingEntity> launchTargets = new java.util.ArrayList<>();

                for (LivingEntity target : targets) {
                    double dist = player.distanceTo(target);
                    if (dist <= aoeRadius) {
                        // Slam damage
                        target.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), totalSlamDamage);

                        // Heavily slowed & shattered by shockwave
                        target.addEffect(new MobEffectInstance(DbaEffects.EARTH_SHATTER_HOLDER, 140, 0, false, true), player);

                        // Tweak A: Completely roots enemies in the ground for 2s (40 ticks), then launches them up and out
                        target.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 50, 0, false, false, false), player);
                        target.addEffect(new MobEffectInstance(DbaEffects.MOVEMENT_CURSE_HOLDER, 40, 9, false, true), player);
                        launchTargets.add(target);
                    }
                }

                if (!launchTargets.isEmpty()) {
                    com.dragonblockarcanedba.entity.DelayedLaunchEntity delayEntity = new com.dragonblockarcanedba.entity.DelayedLaunchEntity(serverLevel, player.position(), powerRatio, launchTargets);
                    serverLevel.addFreshEntity(delayEntity);
                }

                // Expanding ground shockwave rings
                for (double r = 2.0; r <= aoeRadius; r += 2.5) {
                    for (int i = 0; i < 60; i++) {
                        double angle = Math.toRadians(i * 6);
                        double px = player.getX() + Math.cos(angle) * r;
                        double pz = player.getZ() + Math.sin(angle) * r;

                        serverLevel.sendParticles(
                            new DustParticleOptions(0xFFD700, 2.5F),
                            px, player.getY() + 0.2, pz,
                            1, 0.0, 0.3, 0.0, 0.02
                        );
                        serverLevel.sendParticles(
                            ParticleTypes.EXPLOSION,
                            px, player.getY() + 0.1, pz,
                            1, 0.0, 0.0, 0.0, 0.0
                        );
                    }
                }

                // Sound
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.6f);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5f, 0.5f);

                // Cooldown
                player.getCooldowns().addCooldown(stack, 60 + (int) (powerRatio * 80));
            }
        }
        return true;
    }
}
