package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.KingsSlamEntity;
import com.dragonblockarcanedba.entity.OxChargeEntity;
import com.dragonblockarcanedba.entity.OxShockwaveEntity;
import com.dragonblockarcanedba.entity.OxStanceAuraEntity;
import net.minecraft.core.particles.ParticleTypes;
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
 * Ox King's Axe — Immovable powerhouse: knockback, ground destruction, massive AoE, and battlefield denial.
 * 
 * Stamina Drain: 125% per minute (smooth).
 * 
 * LEFT: Groundbreaker (Hold Left Click to Charge)
 * - Hold left click to repeatedly charge the axe (up to 10s / 200 ticks).
 * - Player becomes increasingly rooted: 0–2s (50%), 2–5s (80%), 5–10s (100% rooted).
 * - Physical 3D ground shatter decal, radiating magma fissure trenches, and levitating 3D basalt rock debris.
 * - Drains 125% stamina per minute.
 * - Release unleashes an enormous downward strike and 360-degree expanding ground shockwave (up to 24 blocks - Tweak A).
 * - Enemies hit are launched violently upward and outward.
 * - Tweak B: Secondary concentric echo waves at >= 50% charge.
 * - Tweak C: Max charge spawns explosion, destroys weak terrain (blast resistance <= 2.0), and leaves damaging ground fissures (5s).
 * 
 * RIGHT: Colossal Stance (Hold Right Click)
 * - Hold right click to become an immovable object (knockback immunity, stationary, Resistance IV).
 * - Physical 3D King's Colossal Aura: Ethereal Titan Aegis avatar, 12-block repulsion boundary seal, and swirling heat dome.
 * - 12-block King's Force repulsion aura: heavily slows enemies (Slowness IV) and pushes them outward.
 * - Continuously builds King's Force; takes 3% current HP pressure damage/sec after 10s.
 * - Normal release (0.5s–14.0s) deals scaled AoE ground slam with physical 3D KingsSlamEntity shockwave.
 * - Critical Peak Window (14.0s–15.0s): Flawless King's Slam deals 2000.0 + Strength * 5.0 in 20-block AoE with massive titan crags and vertical magma geysers.
 * - Failure State (> 15.0s): Stance collapses, deals 25% built-up damage as recoil self-damage, and inflicts 10s cooldown.
 */
public class OxKingsAxeItem extends Item {
    public static final int MAX_LEFT_CHARGE_TICKS = 200; // 10 seconds

    public static final Map<UUID, OxChargeEntity> ACTIVE_CHARGE_MAP = new ConcurrentHashMap<>();
    public static final Map<UUID, OxStanceAuraEntity> ACTIVE_STANCE_MAP = new ConcurrentHashMap<>();

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
                    -2.8, // Heavy, colossal strikes
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Physics: Titan mass & immovable ground friction
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("ox_ground_grip"), 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !player.level().isClientSide()) {
            WeaponDrainHelper.drainStaminaDiscrete(player, 125.0, 15);
        }
    }

    // --- LEFT CLICK: Groundbreaker Charging & Downward Shockwave Strike ---

    public static void onLeftClickChargeTick(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Drain Stamina: 125% per minute (smooth)
        if (!WeaponDrainHelper.drainStaminaPerTick(player, 125.0)) {
            // Out of Stamina! Force release with accumulated charge
            onLeftClickRelease(player, stack, chargeTicks);
            return;
        }
        if (chargeTicks % 5 == 0) {
            accessor.dba$syncStats();
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

        // Spawn / update physical 3D OxChargeEntity (dynamic ground shatter & levitating rock debris)
        OxChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.get(player.getUUID());
        if (chargeEntity == null || chargeEntity.isRemoved()) {
            chargeEntity = new OxChargeEntity(level, player);
            level.addFreshEntity(chargeEntity);
            ACTIVE_CHARGE_MAP.put(player.getUUID(), chargeEntity);
        }
        chargeEntity.setChargeRatio(chargeRatio);

        // Audio rumble
        if (chargeTicks % 20 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.9f, 0.4f + chargeRatio * 0.4f);
        }
    }

    public static void onLeftClickRelease(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Discard 3D charge entity
        OxChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.remove(player.getUUID());
        if (chargeEntity != null && !chargeEntity.isRemoved()) {
            chargeEntity.discard();
        }

        float chargeRatio = Math.max(0.05f, Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS));

        // Damage formula: 350.0 + (chargeRatio * 850.0) + (Strength * 3.5 * chargeRatio)
        float baseDmg = 350.0f + (chargeRatio * 850.0f);
        float strengthBonus = (float) (accessor.dba$getStrength() * 3.5f * chargeRatio);
        float totalDamage = baseDmg + strengthBonus;

        // Spawn 360-degree expanding ground shockwave (towering 3D geometry)
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
            // Drain Stamina: 125% per minute
            if (!WeaponDrainHelper.drainStaminaPerTick(player, 125.0)) {
                player.stopUsingItem();
                return;
            }

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

            // Update physical 3D King's Colossal Aura Entity (translucent titan avatar, repulsion boundary & heat dome)
            boolean isPeak = heldTicks >= 280 && heldTicks <= 300;
            OxStanceAuraEntity stanceEntity = ACTIVE_STANCE_MAP.get(player.getUUID());
            if (stanceEntity == null || stanceEntity.isRemoved()) {
                stanceEntity = new OxStanceAuraEntity(serverLevel, player);
                serverLevel.addFreshEntity(stanceEntity);
                ACTIVE_STANCE_MAP.put(player.getUUID(), stanceEntity);
            }
            stanceEntity.updateStance(heldTicks, isPeak);

            // 3. Pressure damage after 10s (heldTicks >= 200): 3% current HP per second
            if (heldTicks >= 200) {
                float pressureDmg = Math.max(1.0f, player.getHealth() * 0.03f / 20.0f);
                player.hurtServer(serverLevel, serverLevel.damageSources().generic(), pressureDmg);
            }

            // 4. Critical Peak Window (14.0s – 15.0s / 280–300 ticks)
            if (isPeak) {
                if (heldTicks % 5 == 0) {
                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.2f, 1.8f);
                }
            }

            // 5. Failure State (> 15.0s / > 300 ticks): Overload recoil
            if (heldTicks > 300) {
                OxStanceAuraEntity rem = ACTIVE_STANCE_MAP.remove(player.getUUID());
                if (rem != null && !rem.isRemoved()) {
                    rem.discard();
                }

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

            // Discard 3D stance entity
            OxStanceAuraEntity stanceEntity = ACTIVE_STANCE_MAP.remove(player.getUUID());
            if (stanceEntity != null && !stanceEntity.isRemoved()) {
                stanceEntity.discard();
            }

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

                    // Spawn physical 3D KingsSlamEntity (20-block radiating canyon trenches, colossal basalt crags & vertical magma geysers)
                    KingsSlamEntity slam = new KingsSlamEntity(serverLevel, player, player.position(), (float) slamRadius, true);
                    serverLevel.addFreshEntity(slam);

                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 0.5, player.getZ(), 3, 0.5, 0.2, 0.5, 0);

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

                    // Spawn physical 3D KingsSlamEntity (10-block ground shatter, basalt monoliths & volcanic shockwave dome)
                    KingsSlamEntity slam = new KingsSlamEntity(serverLevel, player, player.position(), (float) slamRadius, false);
                    serverLevel.addFreshEntity(slam);

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
