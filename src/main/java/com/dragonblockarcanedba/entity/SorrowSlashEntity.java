package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sorrow Slash Entity — Physical 3D scythe crescent slash unleashed by the Sickle of Sorrow on Left-Click impact and air swings.
 * Deals massive stat-scaled damage, applies Melting III, Sorrow Rift, and triggers Soul Rend lifesteal.
 */
public class SorrowSlashEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> TILT_RIGHT = SynchedEntityData.defineId(SorrowSlashEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_IMPACT_BURST = SynchedEntityData.defineId(SorrowSlashEntity.class, EntityDataSerializers.BOOLEAN);

    private float damage = 750.0f;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public SorrowSlashEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    /**
     * Creates a Sorrow Slash entity.
     * @param level The level
     * @param owner The player/entity casting the slash
     * @param spawnPos The starting position
     * @param yRot The yaw rotation
     * @param xRot The pitch rotation
     * @param damage Total stat-scaled damage
     * @param tiltRight Whether to tilt right or left
     * @param isImpactBurst True if spawned directly on a melee target impact
     */
    public SorrowSlashEntity(Level level, LivingEntity owner, Vec3 spawnPos, float yRot, float xRot, float damage, boolean tiltRight, boolean isImpactBurst) {
        super(DbaEntities.SORROW_SLASH, level);
        this.setOwner(owner);
        this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.damage = damage;
        this.noPhysics = true;
        this.entityData.set(TILT_RIGHT, tiltRight);
        this.entityData.set(IS_IMPACT_BURST, isImpactBurst);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TILT_RIGHT, false);
        builder.define(IS_IMPACT_BURST, false);
    }

    public boolean getTilt() {
        return this.entityData.get(TILT_RIGHT);
    }

    public boolean isImpactBurst() {
        return this.entityData.get(IS_IMPACT_BURST);
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 movement = this.getDeltaMovement();
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Entity owner = this.getOwner();
            DamageSource damageSource = owner instanceof Player playerOwner
                ? serverLevel.damageSources().playerAttack(playerOwner)
                : (owner instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobAttack(livingOwner) : serverLevel.damageSources().generic());

            double hitWidth = isImpactBurst() ? 2.5 : 3.8;
            double hitHeight = isImpactBurst() ? 2.0 : 1.6;
            AABB hitbox = this.getBoundingBox().inflate(hitWidth, hitHeight, hitWidth);

            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, hitbox,
                e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
            );

            for (LivingEntity target : targets) {
                hitEntityIds.add(target.getId());

                // Apply damage
                target.hurtServer(serverLevel, damageSource, this.damage);

                // Apply Melting III (amplifier 2) for 15s (300 ticks)
                target.addEffect(new MobEffectInstance(DbaEffects.MELTING_HOLDER, 300, 2, false, true), owner);

                // Apply Sorrow Rift for 4s (80 ticks) — weeping shadow corruption
                target.addEffect(new MobEffectInstance(DbaEffects.SORROW_RIFT_HOLDER, 80, 0, false, true), owner);

                // MC 26.2 Physics: Sorrowful void ice friction distortion
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    target,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("sorrow_slash_friction"),
                    -0.60,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );

                // Soul Rend: Steal 3% of total damage dealt as healing for the owner
                if (owner instanceof Player playerOwner) {
                    float healAmount = this.damage * 0.03f;
                    playerOwner.heal(healAmount);
                }

                // Audio impact
                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.6f, 0.5f);
                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2f, 0.7f);
            }

            // Lifetime limits (16 ticks for impact burst, 30 ticks for wave)
            int maxTicks = isImpactBurst() ? 16 : 30;
            if (this.tickCount > maxTicks) {
                this.discard();
            }
        }
    }
}
