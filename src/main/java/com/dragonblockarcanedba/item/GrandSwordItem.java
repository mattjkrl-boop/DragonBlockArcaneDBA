package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.entity.GrandCrescentWaveEntity;
import com.dragonblockarcanedba.movement.MovementPhysics;
import com.dragonblockarcanedba.entity.ValorFieldEntity;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.dragonblockarcanedba.util.DbaDamage;
import com.dragonblockarcanedba.util.WeaponDrainHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
 * - Damages enemies repeatedly (every 3 ticks), pulls them into the blade arc,
 *   and the contact radius and shard speed grow as the spin builds.
 * - Deflects (reverses) incoming hostile projectiles back at their owners.
 * - Slowly moves player forward in facing direction.
 * - Release executes an outward finisher slash + fires a glowing Grand Crescent Wave (Tweak B).
 * Grand Sword — Knight / Crusader AoE, Defense & Battlefield Control Weapon.
 *
 * Ki Drain: 90% of max Ki per minute (1.5% per second, smooth), shared by the
 * spin and the Valor Field. A full 140 tick spin pays spinMult 2.4 on the
 * finisher and the wave against 0.8 for a tap release, and the release
 * cooldown is 160 ticks so the finisher cannot outpace the charge that fed it.
 */
public class GrandSwordItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
    public static final int MAX_LEFT_SPIN_TICKS = 140; // 7 seconds
    public static final int FINISHER_COOLDOWN_TICKS = 160; // 8 seconds
    public static final int VALOR_FIELD_COOLDOWN_TICKS = 60; // 3 seconds
    private static final int VALOR_GOLD_ARGB = 0xFFD833;

    // Track active Valor Field entities per player
    public static final Map<UUID, ValorFieldEntity> ACTIVE_VALOR_FIELDS = new ConcurrentHashMap<>();

    public GrandSwordItem(Properties properties) {
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
                    -2.4, // Heavy, deliberate two-handed strikes
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Physics: Unshakable Valor Stance & Fortified Poise
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("grand_sword_valor_friction"), 0.75, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // No Ki cost and no extra damage: the spin and the flying blade already
        // pay. But a 900 damage connect used to land with zero feedback, so a
        // melee hit now bursts gold sparks at the point of contact.
        if (attacker.level() instanceof ServerLevel serverLevel) {
            float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
            if (density > 0f) {
                serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                    target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                    Math.max(2, Math.round(10f * density)), 0.35, 0.35, 0.35, 0.12);
                serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
                    target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                    Math.max(1, Math.round(3f * density)), 0.25, 0.25, 0.25, 0.05);
            }
        }
    }

    // --- LEFT CLICK: Grand Cyclone Spin, Blade Shards & Finisher Slash ---

    public static void onLeftClickSpinTick(ServerPlayer player, ItemStack stack, int spinTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Drain Ki: 90% per minute (smooth)
        if (!WeaponDrainHelper.drainKiPerTick(player, 90.0)) {
            // Out of Ki: force release
            onLeftClickSpinRelease(player, stack, spinTicks);
            return;
        }
        if (spinTicks % 5 == 0) {
            accessor.dba$syncStats();
        }

        // Server-side player rotation sync (accelerating from 15° to 35° per tick)
        float spinProgress = Math.min(1.0f, spinTicks / 100.0f);
        float spinSpeed = 15.0f + (spinProgress * 20.0f);
        player.setYRot(player.getYRot() + spinSpeed);
        player.yRotO = player.getYRot();
        player.yHeadRot = player.getYRot();
        player.yBodyRot = player.getYRot();

        // Continuous forward drift in current facing direction as player rotates (scaled by CC)
        double ccMult = com.dragonblockarcanedba.util.MovementLimiterHelper.getMovementMultiplier(player);
        Vec3 look = player.getLookAngle();
        Vec3 drift = new Vec3(look.x, 0, look.z).normalize().scale(0.06 * ccMult);
        MovementPhysics.addVelocity(player, new Vec3(drift.x, 0, drift.z));

        // Spin contact: a cyclone pulls victims into the blade arc and holds
        // them there; the release finisher is what flings them out. Contact
        // damage ramps with the spin instead of landing at full value on the
        // first tick, and the contact radius grows from 2.6 to 3.5 blocks as
        // the spin builds, which is the range growth the header promises.
        float contactRamp = 0.4f + 0.6f * Math.min(1.0f, spinTicks / 60.0f);
        if (spinTicks % 3 == 0) {
            float contactDamage = (100.0f + (float) (accessor.dba$getStrength() * 0.5f)) * contactRamp;
            double contactRadius = 2.6 + 0.9 * spinProgress;
            AABB contactBox = player.getBoundingBox().inflate(contactRadius, 1.2, contactRadius);
            List<LivingEntity> contactTargets = level.getEntitiesOfClass(
                LivingEntity.class, contactBox,
                e -> e.isAlive() && e != player
            );
            for (LivingEntity contact : contactTargets) {
                contact.hurtServer(level, DbaDamage.valor(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (contactDamage)));
                Vec3 toward = player.position().subtract(contact.position());
                toward = new Vec3(toward.x, 0, toward.z);
                if (toward.lengthSqr() > 0.001) {
                    toward = toward.normalize().scale(0.45);
                    contact.setDeltaMovement(contact.getDeltaMovement().add(toward.x, 0.12, toward.z));
                    com.dragonblockarcanedba.util.DbaCompat.markHurt(contact);
                }
            }
        }

        // Layered cyclone particles (shared LayeredParticles block): a bright
        // core, a gold mid band and wispy outer sparks around the spinning
        // player, replacing the old single flat ring of 10 SPARK points. The
        // age is capped so the envelope holds at peak for an open ended spin.
        if (spinTicks % 2 == 0) {
            com.dragonblockarcanedba.util.LayeredParticles.emitVortex(level, player.position(),
                2.6f, 2.4f, Math.min(spinTicks, 20), 28, VALOR_GOLD_ARGB,
                com.dragonblockarcanedba.particle.DbaParticles.SPARK);
        }
        // Ground churn: a dust burst at the feet when the spin starts, then a
        // smaller one every 30 ticks while the cyclone tears at the ground.
        if (spinTicks == 1 || spinTicks % 30 == 0) {
            com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(level, player.position(), 2.2f, VALOR_GOLD_ARGB);
        }

        // Fire sharp blade shards every 3 ticks outward in the current rotated facing direction
        if (spinTicks % 3 == 0) {
            double spreadX = (level.getRandom().nextDouble() - 0.5) * 0.08;
            double spreadY = (level.getRandom().nextDouble() - 0.5) * 0.08;
            double spreadZ = (level.getRandom().nextDouble() - 0.5) * 0.08;
            Vec3 velocity = new Vec3(look.x + spreadX, look.y + spreadY, look.z + spreadZ).normalize().scale(1.5 + 0.4 * spinProgress);

            float shardDamage = 80.0f + (float) (accessor.dba$getStrength() * 0.5f);
            com.dragonblockarcanedba.entity.GrandBladeShardEntity shard =
                new com.dragonblockarcanedba.entity.GrandBladeShardEntity(level, player, shardDamage);
            shard.setDeltaMovement(velocity);
            level.addFreshEntity(shard);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                DbaSounds.SWING_HEAVY, SoundSource.PLAYERS, 0.9f, 1.6f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.SLASH_WHOOSH, SoundSource.PLAYERS, 0.9f, 1.6f);
        }

        // Continuous tracking lock on all slowed enemies within range while spinning
        AABB spinBox = player.getBoundingBox().inflate(14.0);
        List<LivingEntity> trackedEnemies = level.getEntitiesOfClass(
            LivingEntity.class, spinBox,
            e -> e.isAlive() && e != player
        );
        for (LivingEntity e : trackedEnemies) {
            if (e.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.VALOR_STUN_HOLDER)) {
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
            com.dragonblockarcanedba.util.DbaCompat.markHurt(p);
            com.dragonblockarcanedba.entity.GrandClashSparkEntity clash = new com.dragonblockarcanedba.entity.GrandClashSparkEntity(
                level, player, p.position(), 1.0f
            );
            level.addFreshEntity(clash);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), DbaSounds.SHIELD_CLANK, SoundSource.PLAYERS, 0.8f, 1.5f);
        }
    }

    public static void onLeftClickSpinRelease(ServerPlayer player, ItemStack stack, int spinTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // 1. Decisive outward finisher slash. Charge scaling now pays per the
        // spectacle standard: a tap release pays 0.8 and a full 140 tick spin
        // pays 2.4, so a full charge is worth 3x a tap on both the finisher
        // and the wave (was 0.7 to 1.3, only 1.86x, for the same 7 seconds).
        float spinRatio = Math.min(1.0f, spinTicks / (float) MAX_LEFT_SPIN_TICKS);
        float spinMult = 0.8f + 1.6f * spinRatio;
        float finisherDamage = (450.0f + (float) (accessor.dba$getStrength() * 2.0f)) * spinMult;
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
                    enemy.hurtServer(level, DbaDamage.valor(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (finisherDamage)));

                    // Apply invisible cinematic tracking effect so Delayed Damage combo locks during sweep
                    enemy.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 20, 0, false, false, false
                    ), player);

                    // MC 26.2 Physics: Grand sweep knockback bounce. This used
                    // to be a permanent applyModifier with no removal anywhere;
                    // it is a timed modifier now and expires after 100 ticks.
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyTimedModifier(
                        enemy,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("grand_sweep_bounce"),
                        0.85,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE,
                        100
                    );

                    // Fling: forward along the look and radially out of the
                    // cyclone, so what the spin pulled in gets thrown clear.
                    Vec3 radial = enemy.position().subtract(player.position());
                    radial = new Vec3(radial.x, 0, radial.z);
                    radial = radial.lengthSqr() > 0.001 ? radial.normalize().scale(0.6) : Vec3.ZERO;
                    Vec3 knock = look.scale(1.4).add(radial);
                    enemy.setDeltaMovement(enemy.getDeltaMovement().add(knock.x, 0.35, knock.z));
                    com.dragonblockarcanedba.util.DbaCompat.markHurt(enemy);
                }
            }
        }

        // 2. Tweak B: Fire glowing golden-white crescent energy wave
        float waveDamage = (600.0f + (float) (accessor.dba$getStrength() * 2.5f)) * spinMult;
        GrandCrescentWaveEntity wave = new GrandCrescentWaveEntity(level, player, waveDamage);
        wave.setDeltaMovement(look.scale(2.2));
        level.addFreshEntity(wave);

        // Sound & particles
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.SWING_HEAVY, SoundSource.PLAYERS, 2.0f, 0.6f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.BOOM_HOLY, SoundSource.PLAYERS, 1.2f, 1.4f);
        com.dragonblockarcanedba.fx.DbaImpactFx.heavy(level, player.getX(), player.getY(), player.getZ());

        // World interaction at the release point: a ground dust burst, a
        // finisher class debris field of torn up ground blocks that orbit and
        // rain back down, a permanent coarse dirt conversion under the
        // player, a shallow spin crater, and a short dissipation drizzle so
        // the cyclone dies instead of popping.
        Vec3 feet = player.position();
        com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(level, feet, 3.0f, VALOR_GOLD_ARGB);
        com.dragonblockarcanedba.util.DebrisSpawner.spawnField(level, player, feet,
            28, 3.2, 3.2, 34);
        net.minecraft.core.BlockPos groundPos = player.blockPosition().below();
        if (!level.getBlockState(groundPos).isAir()) {
            com.dragonblockarcanedba.util.PermanentDestruction.convertDisk(level, groundPos,
                spinRatio > 0.66f ? 2 : 1, net.minecraft.world.level.block.Blocks.COARSE_DIRT.defaultBlockState(),
                0.45f, level.getRandom());
            com.dragonblockarcanedba.util.PermanentDestruction.crater(level, groundPos,
                2.0, 0.5, false,
                com.dragonblockarcanedba.util.PermanentDestruction.TIER_WEAK, 60, level.getRandom());
        }
        com.dragonblockarcanedba.util.LayeredParticles.emitRain(level, feet, 3.0f, 2.5f,
            19f, 20f, com.dragonblockarcanedba.particle.DbaParticles.SPARK);

        // Cooldown: 160 ticks (8 seconds). Longer than the 140 tick charge
        // that feeds the finisher and longer than the wave's 80 tick life, so
        // short spins can no longer spam the finisher plus wave pair.
        player.getCooldowns().addCooldown(stack, FINISHER_COOLDOWN_TICKS);
    }

    // --- RIGHT CLICK: Valor Field ---

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
                // Fade out over 12 ticks instead of vanishing in one tick.
                field.beginFadeOut();
            }
            // 60 ticks (3 seconds) of real downtime after dropping the dome,
            // up from 20, so the field reads as an event ending.
            player.getCooldowns().addCooldown(stack, VALOR_FIELD_COOLDOWN_TICKS);
        }
        return true;
    }

    public static void onPlayerDisconnect(UUID playerUuid) {
        ValorFieldEntity field = ACTIVE_VALOR_FIELDS.remove(playerUuid);
        if (field != null && field.isAlive()) {
            field.beginFadeOut();
        }
    }
}
