package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.KingsSlamEntity;
import com.dragonblockarcanedba.movement.MovementPhysics;
import com.dragonblockarcanedba.entity.OxChargeEntity;
import com.dragonblockarcanedba.entity.OxShockwaveEntity;
import com.dragonblockarcanedba.entity.OxStanceAuraEntity;
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
 * Ox King's Axe: Immovable powerhouse: knockback, ground destruction, massive AoE, and battlefield denial.
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
 * - Hold right click to become an immovable object (knockback immunity, stationary, Ox Brace poise and damage reduction).
 * - Physical 3D King's Colossal Aura: Ethereal Titan Aegis avatar, 12-block repulsion boundary seal, and swirling heat dome.
 * - 12-block King's Force repulsion aura: applies Fissure Stun to enemies, pressures them with damage, and pushes them outward. Tamed pets are exempt from the aura.
 * - Continuously builds King's Force; takes 3% current HP pressure damage/sec after 10s.
 * - Normal release (0.5s–14.0s) deals scaled AoE ground slam with physical 3D KingsSlamEntity shockwave.
 * - Critical Peak Window (14.0s–15.0s): Flawless King's Slam deals 2000.0 + Strength * 5.0 in 20-block AoE with massive titan crags and vertical magma geysers.
 * - Failure State (> 15.0s): Stance collapses, deals 25% built-up damage as recoil self-damage, and inflicts 10s cooldown.
 */
public class OxKingsAxeItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
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
        // Royal weight: a plain connect used to be a bare vanilla hit with no
        // effect, no cost, and no visual of its own, on the heaviest weapon in
        // the set. It now lands like the weapon it is: a heavy shove scaled by
        // the target's weight class, a brief fissure stun tremor, embers, and
        // a heavy thunk. Still no stamina or Ki cost: the abilities pay those.
        if (!(attacker.level() instanceof ServerLevel serverLevel)) return;

        Vec3 away = target.position().subtract(attacker.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        if (flat.lengthSqr() < 0.001) {
            Vec3 look = attacker.getLookAngle();
            flat = new Vec3(look.x, 0, look.z);
        }
        if (flat.lengthSqr() < 0.001) return;
        flat = flat.normalize();

        // Weight classes replicated from MobPhysics (its weightOf is private):
        // knockback resistance >= 0.9 or max health >= 400 is BOSS class.
        double kbRes = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        float maxHealth = target.getMaxHealth();
        double weightScale;
        if (kbRes >= 0.9 || maxHealth >= 400f) {
            weightScale = 0.12;
        } else if (kbRes >= 0.45 || maxHealth >= 90f) {
            weightScale = 0.45;
        } else {
            weightScale = 1.0;
        }

        Vec3 vel = target.getDeltaMovement();
        target.setDeltaMovement(
            vel.x * 0.3 + flat.x * 0.85 * weightScale,
            Math.max(vel.y, 0.42 * weightScale),
            vel.z * 0.3 + flat.z * 0.85 * weightScale
        );
        com.dragonblockarcanedba.util.DbaCompat.markHurt(target);

        target.addEffect(new MobEffectInstance(DbaEffects.FISSURE_STUN_HOLDER, 12, 0, false, true));

        float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
        if (density > 0f) {
            serverLevel.sendParticles(
                com.dragonblockarcanedba.particle.DbaParticles.EMBER,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                Math.max(1, Math.round(6f * density)), 0.3, 0.3, 0.3, 0.04);
        }
        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.THUNK, SoundSource.PLAYERS, 0.5f, 0.9f);
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
            MovementPhysics.rootInPlace(player);
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
                com.dragonblockarcanedba.sound.DbaSounds.CHARGE_HUM, SoundSource.PLAYERS, 0.9f, 0.4f + chargeRatio * 0.4f);
        }

        // Full-charge ready cue: without it a maxed charge felt identical to 95 percent.
        if (chargeTicks == MAX_LEFT_CHARGE_TICKS - 10) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.CHARGE_FULL, SoundSource.PLAYERS, 1.2f, 1.6f);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6Groundbreaker fully charged!"), true);
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

        // Downward swing sounds & impact, scaled by charge so a tap release and a
        // full 10-second charge no longer sound identical.
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.SWING_HEAVY, SoundSource.PLAYERS, 1.0f + chargeRatio, 0.4f + chargeRatio * 0.3f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.SLAM_THUD, SoundSource.PLAYERS, 1.0f + chargeRatio * 0.8f, 0.6f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.RELEASE_BLAST, SoundSource.PLAYERS, 0.8f + chargeRatio * 0.7f, 0.6f + chargeRatio * 0.3f);
        com.dragonblockarcanedba.fx.DbaImpactFx.heavy(level, player.getX(), player.getY(), player.getZ());

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
            MovementPhysics.rootInPlace(player);

            // Ox Brace (Mountain poise & damage reduction)
            player.addEffect(new MobEffectInstance(DbaEffects.OX_BRACE_HOLDER, 10, 1, false, false));

            // 2. 12-block King's Force Battlefield Denial Aura
            double auraRadius = 12.0;
            AABB auraBox = player.getBoundingBox().inflate(auraRadius);
            List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(
                LivingEntity.class, auraBox,
                // The aura is battlefield denial pressure, not a pet punisher: tamed
                // animals standing near their owner are exempt (the slams stay full AoE).
                e -> e.isAlive() && e != player
                    && !(e instanceof net.minecraft.world.entity.TamableAnimal tamable && tamable.isTame())
            );

            for (LivingEntity enemy : enemies) {
                double dist = Math.sqrt(enemy.distanceToSqr(player));
                if (dist <= auraRadius) {
                    // Distance falloff: the King's Force is crushing at the
                    // center and a shove at the rim, instead of one flat push
                    // and flat damage everywhere inside 12 blocks. The stun
                    // stays full strength across the radius: it is the denial.
                    double rimT = Math.min(1.0, dist / auraRadius);
                    double pushScale = 0.30 + 0.70 * (1.0 - rimT);
                    double damageScale = 1.0 - 0.5 * rimT;

                    // Fissure Stun (Volcanic tremor slow)
                    enemy.addEffect(new MobEffectInstance(DbaEffects.FISSURE_STUN_HOLDER, 20, 0, false, true));

                    // Deal pressure damage every 5 ticks (caught by Delayed Damage Mixin)
                    if (heldTicks % 5 == 0) {
                        PlayerStatsAccessor acc = (PlayerStatsAccessor) player;
                        float auraPressureDmg = (45.0f + (float) (acc.dba$getStrength() * 0.4f)) * (float) damageScale;
                        enemy.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.crush(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (auraPressureDmg)));
                        enemy.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 25, 0, false, false, false), player);
                    }

                    // Outward repulsion push, falling off toward the rim
                    Vec3 toEnemy = enemy.position().subtract(player.position());
                    Vec3 pushDir = new Vec3(toEnemy.x, 0, toEnemy.z);
                    if (pushDir.lengthSqr() > 0.001) {
                        pushDir = pushDir.normalize().scale(0.35 * pushScale);
                        enemy.setDeltaMovement(enemy.getDeltaMovement().add(pushDir.x, 0.05 * pushScale, pushDir.z));
                        com.dragonblockarcanedba.util.DbaCompat.markHurt(enemy);
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
                player.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.crush(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (pressureDmg)));
            }

            // 4. Critical Peak Window (14.0s – 15.0s / 280–300 ticks)
            if (isPeak) {
                if (heldTicks % 5 == 0) {
                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        com.dragonblockarcanedba.sound.DbaSounds.CHARGE_FULL, SoundSource.PLAYERS, 1.2f, 1.8f);
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

                player.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.crush(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (recoilDamage)));
                player.stopUsingItem();

                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.ITEM_SHATTER, SoundSource.PLAYERS, 2.5f, 0.5f);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.BOOM_BIG, SoundSource.PLAYERS, 2.0f, 0.6f);
        com.dragonblockarcanedba.fx.DbaImpactFx.heavy(serverLevel, player.getX(), player.getY(), player.getZ());

                serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.BLAST, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0, 0, 0, 0);

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

                    // The slam entity owns the damage now, timed to its visual:
                    // targets are hit as the growing dome rim reaches them (rim
                    // tick 31 of 55) instead of everything taking 2000+ damage
                    // instantly at the release tick. Same damage, same effects,
                    // same launch, applied by KingsSlamEntity.applyRimDamage.
                    KingsSlamEntity slam = new KingsSlamEntity(serverLevel, player, player.position(), (float) slamRadius, true, peakDamage);
                    serverLevel.addFreshEntity(slam);

                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.BLAST, player.getX(), player.getY() + 0.5, player.getZ(), 3, 0.5, 0.2, 0.5, 0);

                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        com.dragonblockarcanedba.sound.DbaSounds.BOOM_BIG, SoundSource.PLAYERS, 3.0f, 0.5f);
        com.dragonblockarcanedba.fx.DbaImpactFx.heavy(serverLevel, player.getX(), player.getY(), player.getZ());
                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        com.dragonblockarcanedba.sound.DbaSounds.BOOM_SONIC, SoundSource.PLAYERS, 2.5f, 0.6f);
                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        com.dragonblockarcanedba.sound.DbaSounds.WIND_GUST, SoundSource.PLAYERS, 2.5f, 0.6f);

                    player.getCooldowns().addCooldown(stack, 100); // 5 seconds cooldown on flawless slam
                } else {
                    // --- NORMAL SLAM (0.5s - 13.95s) ---
                    float holdRatio = Math.min(1.0f, heldTicks / 200.0f);
                    float baseSlamDmg = 400.0f + (holdRatio * 900.0f);
                    float strBonus = (float) (accessor.dba$getStrength() * 3.0f * holdRatio);
                    float totalSlamDmg = baseSlamDmg + strBonus;
                    // Radius scales with the hold like the damage does. It used
                    // to be frozen at 10.0, so a 0.5 second hold and a capped
                    // hold covered identical ground.
                    double slamRadius = 6.0 + holdRatio * 8.0;

                    // Damage is applied by the slam entity as its dome rim
                    // reaches each target (rim tick 22 of 40), not instantly.
                    KingsSlamEntity slam = new KingsSlamEntity(serverLevel, player, player.position(), (float) slamRadius, false, totalSlamDmg);
                    serverLevel.addFreshEntity(slam);

                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        com.dragonblockarcanedba.sound.DbaSounds.RELEASE_BLAST, SoundSource.PLAYERS, 2.0f, 0.7f);
        com.dragonblockarcanedba.fx.DbaImpactFx.heavy(serverLevel, player.getX(), player.getY(), player.getZ());
                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        com.dragonblockarcanedba.sound.DbaSounds.SLAM_THUD, SoundSource.PLAYERS, 1.5f, 0.6f);

                    player.getCooldowns().addCooldown(stack, 40 + (int) (holdRatio * 60));
                }
            }
        }
        return true;
    }

    /** Discards any charge or stance entities still tracked for a player who logged out. */
    public static void onPlayerDisconnect(UUID playerUuid) {
        net.minecraft.world.entity.Entity e0 = ACTIVE_CHARGE_MAP.remove(playerUuid);
        if (e0 != null && e0.isAlive()) e0.discard();
        net.minecraft.world.entity.Entity e1 = ACTIVE_STANCE_MAP.remove(playerUuid);
        if (e1 != null && e1.isAlive()) e1.discard();
    }
}
