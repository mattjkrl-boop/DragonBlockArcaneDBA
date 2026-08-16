package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.entity.GrandCrescentWaveEntity;
import com.dragonblockarcanedba.entity.ValorFieldEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Grand Sword — Balanced legendary greatsword: AoE, defense, ranged energy, and team utility.
 * 
 * LEFT: Grand Cyclone (Hold Left Click)
 * - Continuously spins the player (accelerating from 15°/tick to 35°/tick).
 * - Damages enemies repeatedly (every 3 ticks), knocks them away, and expands in piercing range.
 * - Deflects/destroys incoming hostile projectiles.
 * - Slowly moves player forward in facing direction.
 * - Drains 4% max Ki per second.
 * - Release executes an outward finisher slash + fires a glowing Grand Crescent Wave (Tweak B).
 * 
 * RIGHT: Valor Field (Hold Right Click)
 * - Creates a 9-block golden protective dome following the player (Tweak C).
 * - Allies receive Strength II and Resistance II.
 * - Caster receives Strength III, Resistance III, and Haste II / Attack Speed Boost (Tweak B).
 * - Enemies entering the field are slowed (Slowness II).
 * - Enemy projectiles entering the field hang suspended in mid-air for up to 4s (detonating on contact, resuming trajectory on exit).
 * - Drains 3.5% max Ki per second.
 */
public class GrandSwordItem extends Item {

    public static final Map<UUID, ValorFieldEntity> ACTIVE_VALOR_FIELDS = new ConcurrentHashMap<>();

    public GrandSwordItem(Properties properties) {
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
                        -2.4, // Balanced heavy swing
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .build()
        ));
    }

    // --- LEFT CLICK: Grand Cyclone Spin, Blade Shards & Finisher Slash ---

    public static void onLeftClickSpinTick(ServerPlayer player, ItemStack stack, int spinTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Drain Ki: 3.5% max Ki per second (0.175% per tick)
        double maxKi = PlayerStats.getMaxKi(player);
        double drainPerTick = (maxKi * 0.035) / 20.0;
        double currentKi = accessor.dba$getCurrentKi();

        if (currentKi >= drainPerTick) {
            accessor.dba$addKi(-drainPerTick);
            if (spinTicks % 5 == 0) {
                accessor.dba$syncStats();
            }
        } else {
            // Out of Ki: force release
            onLeftClickSpinRelease(player, stack, spinTicks);
            return;
        }

        // Server-side player rotation sync (accelerating from 15° to 35° per tick)
        float spinProgress = Math.min(1.0f, spinTicks / 100.0f);
        float spinSpeed = 15.0f + (spinProgress * 20.0f);
        player.setYRot(player.getYRot() + spinSpeed);
        player.yRotO = player.getYRot();
        player.yHeadRot = player.getYRot();
        player.yBodyRot = player.getYRot();

        // Continuous forward drift in current facing direction as player rotates
        Vec3 look = player.getLookAngle();
        Vec3 drift = new Vec3(look.x, 0, look.z).normalize().scale(0.06);
        player.setDeltaMovement(player.getDeltaMovement().add(drift.x, 0, drift.z));
        player.hurtMarked = true;

        // Fire sharp blade shards every 3 ticks outward in the current rotated facing direction
        if (spinTicks % 3 == 0) {
            double spreadX = (level.getRandom().nextDouble() - 0.5) * 0.08;
            double spreadY = (level.getRandom().nextDouble() - 0.5) * 0.08;
            double spreadZ = (level.getRandom().nextDouble() - 0.5) * 0.08;
            Vec3 velocity = new Vec3(look.x + spreadX, look.y + spreadY, look.z + spreadZ).normalize().scale(1.5);

            float shardDamage = 80.0f + (float) (accessor.dba$getStrength() * 0.5f);
            com.dragonblockarcanedba.entity.GrandBladeShardEntity shard =
                new com.dragonblockarcanedba.entity.GrandBladeShardEntity(level, player, shardDamage);
            shard.setDeltaMovement(velocity);
            level.addFreshEntity(shard);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 1.6f);
        }

        // Continuous tracking lock on all slowed enemies within range while spinning
        AABB spinBox = player.getBoundingBox().inflate(14.0);
        List<LivingEntity> trackedEnemies = level.getEntitiesOfClass(
            LivingEntity.class, spinBox,
            e -> e.isAlive() && e != player
        );
        for (LivingEntity e : trackedEnemies) {
            if (e.hasEffect(MobEffects.SLOWNESS)) {
                e.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 40, 0, false, false, false
                ), player);
            }
        }

        // Projectile & Ki Attack Interception (within 4.0 blocks)
        AABB projBox = player.getBoundingBox().inflate(4.0);
        List<Projectile> projectiles = level.getEntitiesOfClass(
            Projectile.class, projBox,
            p -> p.isAlive() && p.getOwner() != player && !(p instanceof com.dragonblockarcanedba.entity.GrandBladeShardEntity) && !(p instanceof com.dragonblockarcanedba.entity.GrandCrescentWaveEntity)
        );

        for (Projectile p : projectiles) {
            p.setOwner(player); // Take ownership so deflected projectiles/ki damage enemies!
            p.setDeltaMovement(p.getDeltaMovement().scale(-1.4));
            p.hurtMarked = true;
            level.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY(), p.getZ(), 4, 0.2, 0.2, 0.2, 0.1);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 0.8f, 1.5f);
        }
    }

    public static void onLeftClickSpinRelease(ServerPlayer player, ItemStack stack, int spinTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // 1. Decisive outward finisher slash
        float finisherDamage = 450.0f + (float) (accessor.dba$getStrength() * 2.0f);
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double finisherRadius = 6.0;

        AABB box = player.getBoundingBox().inflate(finisherRadius);
        List<LivingEntity> enemies = level.getEntitiesOfClass(
            LivingEntity.class, box,
            e -> e.isAlive() && e != player
        );

        for (LivingEntity enemy : enemies) {
            Vec3 toEnemy = enemy.getBoundingBox().getCenter().subtract(eyePos);
            double dist = toEnemy.length();
            if (dist <= finisherRadius) {
                double dot = look.dot(toEnemy.normalize());
                if (dot > 0.35) { // Frontal sweep arc
                    enemy.hurtServer(level, level.damageSources().playerAttack(player), finisherDamage);

                    // Apply invisible cinematic tracking effect so Delayed Damage combo locks during sweep
                    enemy.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 20, 0, false, false, false
                    ), player);

                    Vec3 knock = look.scale(1.4);
                    enemy.setDeltaMovement(enemy.getDeltaMovement().add(knock.x, 0.3, knock.z));
                    enemy.hurtMarked = true;
                }
            }
        }

        // 2. Tweak B: Fire glowing golden-white crescent energy wave
        float waveDamage = 600.0f + (float) (accessor.dba$getStrength() * 2.5f);
        GrandCrescentWaveEntity wave = new GrandCrescentWaveEntity(level, player, waveDamage);
        wave.setDeltaMovement(look.scale(2.2));
        level.addFreshEntity(wave);

        // Sound & particles
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0f, 0.6f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 1.4f);

        // Cooldown: 2 seconds (40 ticks)
        player.getCooldowns().addCooldown(stack, 40);
    }

    // --- RIGHT CLICK: Valor Field ---

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
            ValorFieldEntity field = ACTIVE_VALOR_FIELDS.get(player.getUUID());
            if (field == null || !field.isAlive()) {
                ValorFieldEntity newField = new ValorFieldEntity(serverLevel, player);
                serverLevel.addFreshEntity(newField);
                ACTIVE_VALOR_FIELDS.put(player.getUUID(), newField);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            ValorFieldEntity field = ACTIVE_VALOR_FIELDS.remove(player.getUUID());
            if (field != null && field.isAlive()) {
                field.discard();
            }
            player.getCooldowns().addCooldown(stack, 20);
        }
        return true;
    }
}
