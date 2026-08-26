package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
 * Brave Cross Slash Entity — Physical 3D cruciform golden-cyan cross slash unleashed on Brave Finisher (10-hit combo).
 * Slices through the target and epicenter with intersecting volumetric curved crescent blades.
 */
public class BraveCrossSlashEntity extends Projectile {
    private static final EntityDataAccessor<Float> SLASH_SCALE = SynchedEntityData.defineId(BraveCrossSlashEntity.class, EntityDataSerializers.FLOAT);

    private float damage = 900.0f;
    private int maxLifetime = 18;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public BraveCrossSlashEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BraveCrossSlashEntity(Level level, LivingEntity owner, Vec3 pos, float yRot, float xRot, float damage, float scale) {
        super(DbaEntities.BRAVE_CROSS_SLASH, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.damage = damage;
        this.noPhysics = true;
        this.entityData.set(SLASH_SCALE, scale);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SLASH_SCALE, 1.0f);
    }

    public float getSlashScale() {
        return this.entityData.get(SLASH_SCALE);
    }

    public int getMaxLifetime() {
        return this.maxLifetime;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Apply area damage and cinematic tracking in the crossfire zone on early ticks
            if (this.tickCount <= 2) {
                Entity owner = this.getOwner();
                DamageSource damageSource = owner instanceof net.minecraft.world.entity.player.Player playerOwner
                    ? serverLevel.damageSources().playerAttack(playerOwner)
                    : (owner instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobProjectile(this, livingOwner) : serverLevel.damageSources().generic());

                float scale = getSlashScale();
                double radius = 3.5 * scale;
                AABB hitbox = this.getBoundingBox().inflate(radius, 2.0, radius);

                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, hitbox,
                    e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
                );

                for (LivingEntity target : targets) {
                    hitEntityIds.add(target.getId());
                    target.hurtServer(serverLevel, damageSource, this.damage);

                    if (owner instanceof LivingEntity livingOwner) {
                        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 25, 0, false, false, false
                        ), livingOwner);
                    }
                }
            }

            if (this.tickCount >= this.maxLifetime) {
                this.discard();
            }
        }
    }
}
