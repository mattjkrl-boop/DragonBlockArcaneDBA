package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Grand Blade Shard Entity — Sharp short blade fired by Grand Sword (Left Click).
 * 
 * Behavior:
 * - Flies in an arc with trailing golden sparks.
 * - Embeds itself into the ground/blocks upon impact.
 * - Acts as a persistent TRIPPING HAZARD on the ground for up to 10 seconds (200 ticks).
 * - When an enemy steps on the blade:
 *     1. Deals stacking damage (full damage per blade).
 *     2. Stacks Slowness AMPLIFIER level (Duration stays fixed at 3 seconds / 60 ticks, but amplifier increases by +1 per blade).
 *     3. Applies Movement Curse CC so Delayed Damage combo pops smoothly.
 *     4. Shatters with golden crit particles and blade sound.
 */
public class GrandBladeShardEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> EMBEDDED = SynchedEntityData.defineId(GrandBladeShardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> EMBEDDED_YAW = SynchedEntityData.defineId(GrandBladeShardEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EMBEDDED_PITCH = SynchedEntityData.defineId(GrandBladeShardEntity.class, EntityDataSerializers.FLOAT);

    private float damage = 80.0f;
    public static final int MAX_EMBEDDED_TICKS = 200; // 10 seconds

    public GrandBladeShardEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = false;
    }

    public GrandBladeShardEntity(Level level, LivingEntity owner, float damage) {
        super(DbaEntities.GRAND_BLADE_SHARD, level);
        this.setOwner(owner);
        if (owner != null) {
            Vec3 eyePos = owner.getEyePosition().subtract(0, 0.2, 0);
            this.setPos(eyePos.x, eyePos.y, eyePos.z);
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
            this.entityData.set(EMBEDDED_YAW, owner.getYRot());
            this.entityData.set(EMBEDDED_PITCH, owner.getXRot());
        }
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(EMBEDDED, false);
        builder.define(EMBEDDED_YAW, 0.0f);
        builder.define(EMBEDDED_PITCH, 0.0f);
    }

    public boolean isEmbedded() {
        return this.entityData.get(EMBEDDED);
    }

    public float getEmbeddedYaw() {
        return this.entityData.get(EMBEDDED_YAW);
    }

    public float getEmbeddedPitch() {
        return this.entityData.get(EMBEDDED_PITCH);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isEmbedded()) {
            // --- EMBEDDED STATE (Tripping Hazard) ---
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                Entity owner = this.getOwner();

                // Hazard detection radius: 0.9m horizontal, 1.0m vertical
                AABB triggerBox = this.getBoundingBox().inflate(0.85, 0.4, 0.85);
                List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, triggerBox,
                    e -> e.isAlive() && e != owner && !e.isSpectator()
                );

                if (!enemies.isEmpty()) {
                    LivingEntity victim = enemies.get(0);
                    DamageSource damageSource = owner instanceof net.minecraft.world.entity.player.Player playerOwner
                        ? serverLevel.damageSources().playerAttack(playerOwner)
                        : (owner instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobAttack(livingOwner) : serverLevel.damageSources().generic());

                    // 1. Stackable damage
                    victim.hurtServer(serverLevel, damageSource, this.damage);

                    // 2. Valor Stun: Stacks AMPLIFIER (effect level), pinning the target at high stacks
                    int currentAmp = 0;
                    if (victim.hasEffect(DbaEffects.VALOR_STUN_HOLDER)) {
                        currentAmp = victim.getEffect(DbaEffects.VALOR_STUN_HOLDER).getAmplifier() + 1;
                        currentAmp = Math.min(5, currentAmp);
                    }
                    // Fixed 60-tick duration (3 seconds)
                    victim.addEffect(new MobEffectInstance(DbaEffects.VALOR_STUN_HOLDER, 60, currentAmp, false, true, true));

                    // 3. Cinematic Delayed Damage invisible tracking (no HUD icon, no particles, holds during slowness)
                    if (owner instanceof LivingEntity livingOwner) {
                        victim.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 60, 0, false, false, false), livingOwner);
                    }

                    // Sound
                    serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.6f);
                    serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ARMOR_STAND_HIT, SoundSource.PLAYERS, 1.2f, 1.2f);

                    this.discard();
                    return;
                }

                // Disappear over time (10 seconds)
                if (this.tickCount >= MAX_EMBEDDED_TICKS) {
                    this.discard();
                }
            }
        } else {
            // --- FLYING STATE ---
            Vec3 movement = this.getDeltaMovement();
            Vec3 start = this.position();
            Vec3 end = start.add(movement);

            // Direct entity hit detection while airborne
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                Entity owner = this.getOwner();
                AABB hitBox = this.getBoundingBox().inflate(0.5);
                List<LivingEntity> hits = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, hitBox,
                    e -> e.isAlive() && e != owner && !e.isSpectator()
                );

                if (!hits.isEmpty()) {
                    LivingEntity victim = hits.get(0);
                    DamageSource damageSource = owner instanceof net.minecraft.world.entity.player.Player playerOwner
                        ? serverLevel.damageSources().playerAttack(playerOwner)
                        : (owner instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobAttack(livingOwner) : serverLevel.damageSources().generic());

                    victim.hurtServer(serverLevel, damageSource, this.damage);

                    int currentAmp = 0;
                    if (victim.hasEffect(DbaEffects.VALOR_STUN_HOLDER)) {
                        currentAmp = Math.min(5, victim.getEffect(DbaEffects.VALOR_STUN_HOLDER).getAmplifier() + 1);
                    }
                    victim.addEffect(new MobEffectInstance(DbaEffects.VALOR_STUN_HOLDER, 60, currentAmp, false, true, true));

                    if (owner instanceof LivingEntity livingOwner) {
                        victim.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 60, 0, false, false, false), livingOwner);
                    }

                    serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.4f);
                    this.discard();
                    return;
                }
            }

            // Block collision clip
            HitResult hit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
                Vec3 hitPos = blockHit.getLocation();
                this.setPos(hitPos.x, hitPos.y, hitPos.z);
                this.setDeltaMovement(0, 0, 0);
                this.entityData.set(EMBEDDED, true);
                this.entityData.set(EMBEDDED_YAW, (float) Math.toDegrees(Math.atan2(-movement.x, movement.z)));
                this.entityData.set(EMBEDDED_PITCH, (float) Math.toDegrees(Math.atan2(-movement.y, Math.sqrt(movement.x * movement.x + movement.z * movement.z))));
                this.tickCount = 0; // Reset tick count for embedded duration

                if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 0.8f, 1.4f);
                }
                return;
            }

            // Apply gravity & velocity
            this.setPos(end.x, end.y, end.z);
            this.setDeltaMovement(movement.x * 0.98, movement.y - 0.04, movement.z * 0.98);

            if (this.tickCount > 60) {
                this.discard();
            }
        }
    }
}
