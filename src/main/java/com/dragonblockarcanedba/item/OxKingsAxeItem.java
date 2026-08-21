package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.OxShockwaveEntity;
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
 * Ox King's Axe — Immovable powerhouse: knockback, ground destruction, massive AoE, and battlefield denial.
 * 
 * LEFT: Groundbreaker (Hold Left Click to Charge)
 * - Hold left click to repeatedly charge the axe (up to 10s / 200 ticks).
 * - Player becomes increasingly rooted: 0–2s (50%), 2–5s (80%), 5–10s (100% rooted).
 * - Black ground crack visuals beneath the player.
 * - Drains 2.5% max Ki per second.
 * - Release unleashes an enormous downward strike and 360-degree expanding ground shockwave (up to 24 blocks - Tweak A).
 * - Enemies hit are launched violently upward and outward.
 * - Tweak B: Secondary concentric echo waves at >= 50% charge.
 * - Tweak C: Max charge spawns explosion, destroys weak terrain (blast resistance <= 2.0), and leaves damaging ground fissures (5s).
 * 
 * RIGHT: Colossal Stance (Hold Right Click)
 * - Hold right click to become an immovable object (knockback immunity, stationary, Resistance IV).
 * - 12-block King's Force repulsion aura: heavily slows enemies (Slowness IV) and pushes them outward.
 * - Continuously builds King's Force; takes 3% current HP pressure damage/sec after 10s.
 * - Normal release (0.5s–14.0s) deals scaled AoE ground slam.
 * - Critical Peak Window (14.0s–15.0s): Flawless King's Slam deals 2000.0 + Strength * 5.0 in 20-block AoE.
 * - Failure State (> 15.0s): Stance collapses, deals 25% built-up damage as recoil self-damage, and inflicts 10s cooldown.
 */
public class OxKingsAxeItem extends Item {
    public static final int MAX_LEFT_CHARGE_TICKS = 200; // 10 seconds

    public OxKingsAxeItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    949.0, // 1 + 949 = 950 base damage
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -3.3, // Heavy swing feel
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Physics: Titan Ground Grip (increased friction against knockback)
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("ox_ground_grip"), 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    // --- LEFT CLICK: Groundbreaker Charging & Downward Shockwave Strike ---

    public static void onLeftClickChargeTick(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Drain Ki: 2.5% max Ki per second (0.125% per tick)
        double maxKi = PlayerStats.getMaxKi(player);
        double drainPerTick = (maxKi * 0.025) / 20.0;
        double currentKi = accessor.dba$getCurrentKi();

        if (currentKi >= drainPerTick) {
            accessor.dba$addKi(-drainPerTick);
            if (chargeTicks % 5 == 0) {
                accessor.dba$syncStats();
            }
        } else {
            // Out of Ki! Force release with accumulated charge
            onLeftClickRelease(player, stack, chargeTicks);
            return;
        }

        // Progressive Rooting & Poise: Ox Brace
        if (chargeTicks < 40) {
            player.addEffect(new MobEffectInstance(DbaEffects.OX_BRACE_HOLDER, 10, 0, false, false));
        } else if (chargeTicks < 100) {
            player.addEffect(new MobEffectInstance(DbaEffects.OX_BRACE_HOLDER, 10, 1, false, false));
        } else {
            // Completely stationary / 100% rooted
            Vec3 vel = player.getDeltaMovement();
            player.setDeltaMovement(0.0, vel.y < 0 ? vel.y : 0.0, 0.0);
            player.hurtMarked = true;
            player.addEffect(new MobEffectInstance(DbaEffects.OX_BRACE_HOLDER, 10, 2, false, false));
        }

        float chargeRatio = Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS);

        // Visual black ground cracks beneath player
        double crackRadius = 1.0 + (chargeRatio * 2.5);
        int points = 8 + (int) (chargeRatio * 16);
        for (int i = 0; i < points; i++) {
            double angle = (i / (double) points) * Math.PI * 2.0;
            double px = player.getX() + Math.cos(angle) * crackRadius;
            double pz = player.getZ() + Math.sin(angle) * crackRadius;

            level.sendParticles(
                new DustParticleOptions(0x000000, 2.0f),
                px, player.getY() + 0.08, pz,
                1, 0, 0.02, 0, 0.01
            );
        }

        // Audio rumble
        if (chargeTicks % 20 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.9f, 0.4f + chargeRatio * 0.4f);
        }
    }

    public static void onLeftClickRelease(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        float chargeRatio = Math.max(0.05f, Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS));

        // Damage formula: 350.0 + (chargeRatio * 850.0) + (Strength * 3.5 * chargeRatio)
        float baseDmg = 350.0f + (chargeRatio * 850.0f);
        float strengthBonus = (float) (accessor.dba$getStrength() * 3.5f * chargeRatio);
        float totalDamage = baseDmg + strengthBonus;

        // Spawn 360-degree expanding ground shockwave
        OxShockwaveEntity wave = new OxShockwaveEntity(level, player, chargeRatio, totalDamage, false);
        level.addFreshEntity(wave);

        // Downward swing sounds & impact
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0f, 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.8f, 0.6f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5f, 0.8f);

        // Cooldown based on power
        int cooldown = 20 + (int) (chargeRatio * 40);
        player.getCooldowns().addCooldown(stack, cooldown);
    }

    // --- RIGHT CLICK: Colossal Stance ---

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

            // 1. Knockback immunity & stationary lock
            Vec3 vel = player.getDeltaMovement();
            player.setDeltaMovement(0.0, vel.y < 0 ? vel.y : 0.0, 0.0);
            player.hurtMarked = true;

            // Ox Brace (Mountain poise & damage reduction)
            player.addEffect(new MobEffectInstance(DbaEffects.OX_BRACE_HOLDER, 10, 1, false, false));

            // 2. 12-block King's Force Battlefield Denial Aura
            double auraRadius = 12.0;
            AABB auraBox = player.getBoundingBox().inflate(auraRadius);
            List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(
                LivingEntity.class, auraBox,
                e -> e.isAlive() && e != player
            );

            for (LivingEntity enemy : enemies) {
                double dist = Math.sqrt(enemy.distanceToSqr(player));
                if (dist <= auraRadius) {
                    // Fissure Stun (Volcanic tremor slow)
                    enemy.addEffect(new MobEffectInstance(DbaEffects.FISSURE_STUN_HOLDER, 20, 0, false, true));

                    // Deal pressure damage every 5 ticks (caught by Delayed Damage Mixin)
                    if (heldTicks % 5 == 0) {
                        PlayerStatsAccessor acc = (PlayerStatsAccessor) player;
                        float auraPressureDmg = 45.0f + (float) (acc.dba$getStrength() * 0.4f);
                        enemy.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), auraPressureDmg);
                        enemy.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 25, 0, false, false, false), player);
                    }

                    // Outward repulsion push
                    Vec3 toEnemy = enemy.position().subtract(player.position());
                    Vec3 pushDir = new Vec3(toEnemy.x, 0, toEnemy.z);
                    if (pushDir.lengthSqr() > 0.001) {
                        pushDir = pushDir.normalize().scale(0.35);
                        enemy.setDeltaMovement(enemy.getDeltaMovement().add(pushDir.x, 0.05, pushDir.z));
                        enemy.hurtMarked = true;
                    }
                }
            }

            // Fiery orange-red aura particles
            if (heldTicks % 2 == 0) {
                for (int i = 0; i < 14; i++) {
                    double angle = serverLevel.getRandom().nextDouble() * Math.PI * 2.0;
                    double dist = 2.0 + serverLevel.getRandom().nextDouble() * (auraRadius - 2.0);
                    double px = player.getX() + Math.cos(angle) * dist;
                    double pz = player.getZ() + Math.sin(angle) * dist;

                    serverLevel.sendParticles(
                        new DustParticleOptions(0xFF4500, 2.2f),
                        px, player.getY() + 0.1, pz,
                        1, 0, 0.3, 0, 0.02
                    );
                    if (i % 3 == 0) {
                        serverLevel.sendParticles(
                            ParticleTypes.FLAME,
                            px, player.getY() + 0.1, pz,
                            1, 0, 0.2, 0, 0.01
                        );
                    }
                }
            }

            // 3. Pressure damage after 10s (heldTicks >= 200): 3% current HP per second
            if (heldTicks >= 200) {
                float pressureDmg = Math.max(1.0f, player.getHealth() * 0.03f / 20.0f);
                player.hurtServer(serverLevel, serverLevel.damageSources().generic(), pressureDmg);
            }

            // 4. Critical Peak Window (14.0s – 15.0s / 280–300 ticks)
            if (heldTicks >= 280 && heldTicks <= 300) {
                if (heldTicks % 5 == 0) {
                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.2f, 1.8f);
                }
            }

            // 5. Failure State (> 15.0s / > 300 ticks): Overload recoil
            if (heldTicks > 300) {
                PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
                float maxSlamDamage = 2000.0f + (float) (accessor.dba$getStrength() * 5.0f);
                float recoilDamage = maxSlamDamage * 0.25f;

                player.hurtServer(serverLevel, serverLevel.damageSources().generic(), recoilDamage);
                player.stopUsingItem();

                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BREAK.value(), SoundSource.PLAYERS, 2.5f, 0.5f);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.6f);

                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0, 0, 0, 0);

                // 10-second penalty cooldown
                player.getCooldowns().addCooldown(stack, 200);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            ServerLevel serverLevel = (ServerLevel) level;
            int heldTicks = getUseDuration(stack, living) - timeLeft;

            if (heldTicks >= 10 && heldTicks <= 300) {
                PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

                if (heldTicks >= 280 && heldTicks <= 300) {
                    // --- FLAWLESS KING'S SLAM (Critical Peak Window 14s-15s) ---
                    float peakDamage = 2000.0f + (float) (accessor.dba$getStrength() * 5.0f);
                    double slamRadius = 20.0;

                    AABB slamBox = player.getBoundingBox().inflate(slamRadius);
                    List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                        LivingEntity.class, slamBox,
                        e -> e.isAlive() && e != player
                    );

                    for (LivingEntity target : targets) {
                        double dist = Math.sqrt(target.distanceToSqr(player));
                        if (dist <= slamRadius) {
                            target.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), peakDamage);
                            target.addEffect(new MobEffectInstance(DbaEffects.FISSURE_STUN_HOLDER, 140, 1, false, true));
                            target.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 60, 0, false, false, false), player);
                            target.addEffect(new MobEffectInstance(DbaEffects.MOVEMENT_CURSE_HOLDER, 60, 2, false, true), player);

                            Vec3 toTarget = target.position().subtract(player.position()).normalize().scale(2.5);
                            target.setDeltaMovement(target.getDeltaMovement().add(toTarget.x, 0.9, toTarget.z));
                            target.hurtMarked = true;
                        }
                    }

                    // Enormous visual explosion & ground shockwave rings
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 0.5, player.getZ(), 5, 1.0, 0.2, 1.0, 0);
                    for (double r = 2.0; r <= slamRadius; r += 2.0) {
                        for (int i = 0; i < 48; i++) {
                            double angle = Math.toRadians(i * 7.5);
                            double px = player.getX() + Math.cos(angle) * r;
                            double pz = player.getZ() + Math.sin(angle) * r;

                            serverLevel.sendParticles(
                                new DustParticleOptions(0xFF2200, 2.5f),
                                px, player.getY() + 0.2, pz,
                                1, 0, 0.4, 0, 0.05
                            );
                        }
                    }

                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0f, 0.5f);
                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.5f, 0.6f);

                    player.getCooldowns().addCooldown(stack, 100); // 5 seconds cooldown on flawless slam
                } else {
                    // --- NORMAL SLAM (0.5s - 13.95s) ---
                    float holdRatio = Math.min(1.0f, heldTicks / 200.0f);
                    float baseSlamDmg = 400.0f + (holdRatio * 900.0f);
                    float strBonus = (float) (accessor.dba$getStrength() * 3.0f * holdRatio);
                    float totalSlamDmg = baseSlamDmg + strBonus;
                    double slamRadius = 10.0;

                    AABB slamBox = player.getBoundingBox().inflate(slamRadius);
                    List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                        LivingEntity.class, slamBox,
                        e -> e.isAlive() && e != player
                    );

                    for (LivingEntity target : targets) {
                        double dist = Math.sqrt(target.distanceToSqr(player));
                        if (dist <= slamRadius) {
                            target.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), totalSlamDmg);
                            target.addEffect(new MobEffectInstance(DbaEffects.FISSURE_STUN_HOLDER, 80, 0, false, true));
                            target.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 40, 0, false, false, false), player);
                            target.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.MOVEMENT_CURSE_HOLDER, 40, 1, false, true), player);

                            Vec3 toTarget = target.position().subtract(player.position()).normalize().scale(1.5);
                            target.setDeltaMovement(target.getDeltaMovement().add(toTarget.x, 0.5, toTarget.z));
                            target.hurtMarked = true;
                        }
                    }

                    // Expanding dust rings
                    for (double r = 2.0; r <= slamRadius; r += 2.5) {
                        for (int i = 0; i < 36; i++) {
                            double angle = Math.toRadians(i * 10);
                            double px = player.getX() + Math.cos(angle) * r;
                            double pz = player.getZ() + Math.sin(angle) * r;

                            serverLevel.sendParticles(
                                new DustParticleOptions(0xFF6600, 2.0f),
                                px, player.getY() + 0.2, pz,
                                1, 0, 0.2, 0, 0.02
                            );
                        }
                    }

                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.7f);
                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5f, 0.6f);

                    player.getCooldowns().addCooldown(stack, 40 + (int) (holdRatio * 60));
                }
            }
        }
        return true;
    }
}
