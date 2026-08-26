package com.dragonblockarcanedba.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Grand Crescent Wave Entity — Fired upon releasing Grand Cyclone (Tweak B).
 * Piercing glowing golden-white crescent energy wave that cuts through lines of enemies.
 */
public class GrandCrescentWaveEntity extends Projectile {
    private float damage = 600.0f;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public GrandCrescentWaveEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public GrandCrescentWaveEntity(Level level, LivingEntity owner, float damage) {
        super(DbaEntities.GRAND_CRESCENT_WAVE, level);
        this.setOwner(owner);
        if (owner != null) {
            this.setPos(owner.getX(), owner.getY() + 1.0, owner.getZ());
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
        }
        this.damage = damage;
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
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

            // Broad horizontal piercing hitbox
            double halfWidth = 3.2;
            double halfHeight = 1.2;
            AABB hitbox = this.getBoundingBox().inflate(halfWidth, halfHeight, halfWidth);

            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, hitbox,
                e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
            );

            for (LivingEntity target : targets) {
                hitEntityIds.add(target.getId());

                // Pierce and damage
                target.hurtServer(serverLevel, damageSource, this.damage);

                // Apply invisible cinematic tracking effect so Delayed Damage combo locks during wave sweep
                if (owner instanceof LivingEntity livingOwner) {
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 30, 0, false, false, false
                    ), livingOwner);
                }

                // Knockback in wave trajectory
                Vec3 knockback = movement.normalize().scale(1.5);
                target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.3, knockback.z));
                target.hurtMarked = true;
            }

            // Check solid block collision
            Vec3 start = this.position();
            Vec3 end = start.add(movement);
            HitResult blockHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (blockHit.getType() != HitResult.Type.MISS) {
                this.discard();
            }

            if (this.tickCount > 80) {
                this.discard();
            }
        }
    }
}
