package com.dragonblockarcanedba.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Blaster Bolt Entity — Laser projectile fired by the Blaster Gun.
 * Rapid projectile with piercing, heat scaling, and explosive Overcharged Blast.
 */
public class BlasterBoltEntity extends Projectile {
    private float damage = 250.0f;
    private boolean isOvercharged = false;
    private float heatRatio = 0.0f;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public BlasterBoltEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BlasterBoltEntity(Level level, LivingEntity owner, float damage, boolean isOvercharged, float heatRatio) {
        super(DbaEntities.BLASTER_BOLT, level);
        this.setOwner(owner);
        if (owner != null) {
            Vec3 eye = owner.getEyePosition();
            Vec3 look = owner.getLookAngle();
            this.setPos(eye.x + look.x * 0.5, eye.y + look.y * 0.5 - 0.1, eye.z + look.z * 0.5);
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
        }
        this.damage = damage;
        this.isOvercharged = isOvercharged;
        this.heatRatio = heatRatio;
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    public boolean isOvercharged() {
        return this.isOvercharged;
    }

    public float getHeatRatio() {
        return this.heatRatio;
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 movement = this.getDeltaMovement();
        double nextX = this.getX() + movement.x;
        double nextY = this.getY() + movement.y;
        double nextZ = this.getZ() + movement.z;
        this.setPos(nextX, nextY, nextZ);

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Entity owner = this.getOwner();
            DamageSource damageSource = owner instanceof net.minecraft.world.entity.player.Player playerOwner
                ? serverLevel.damageSources().playerAttack(playerOwner)
                : (owner instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobProjectile(this, livingOwner) : serverLevel.damageSources().generic());

            double hitRadius = isOvercharged ? 1.5 : (0.6 + heatRatio * 0.4);
            AABB hitbox = this.getBoundingBox().inflate(hitRadius, hitRadius, hitRadius);

            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, hitbox,
                e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
            );

            for (LivingEntity target : targets) {
                hitEntityIds.add(target.getId());

                target.hurtServer(serverLevel, damageSource, this.damage);

                if (owner instanceof LivingEntity livingOwner) {
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 15, 0, false, false, false
                    ), livingOwner);
                }

                // Knockback
                Vec3 knockback = movement.normalize().scale(isOvercharged ? 1.5 : 0.6);
                target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.2, knockback.z));
                target.hurtMarked = true;

                if (isOvercharged) {
                    // Overcharged explosive detonation (Tweak C)
                    AABB aoe = target.getBoundingBox().inflate(3.0);
                    List<LivingEntity> splashTargets = serverLevel.getEntitiesOfClass(
                        LivingEntity.class, aoe, e -> e.isAlive() && e != owner && e != target
                    );
                    for (LivingEntity st : splashTargets) {
                        st.hurtServer(serverLevel, damageSource, this.damage * 0.6f);
                    }
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);
                    serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 1.4f);
                }
            }

            // Trailing laser particles (Yellow/Orange for normal, Bright Red/White for overcharged)
            int color = isOvercharged ? 0xFF0033 : (heatRatio > 0.6f ? 0xFF4500 : 0xFFD700);
            float scale = isOvercharged ? 2.5f : (1.2f + heatRatio * 0.8f);

            Vec3 dir = movement.normalize();
            for (double d = 0; d < movement.length(); d += 0.8) {
                Vec3 p = this.position().subtract(dir.scale(d));
                serverLevel.sendParticles(
                    new DustParticleOptions(color, scale),
                    p.x, p.y, p.z,
                    1, 0, 0, 0, 0
                );
            }

            if (this.tickCount > 30) {
                this.discard();
            }
        }
    }
}
