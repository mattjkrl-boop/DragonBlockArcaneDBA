package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Saber Slash Entity — Physical 3D volumetric crescent slash spawned during Saber Blitz Flurry hits,
 * Flash Step path damage, and rapid-chaining strikes.
 */
public class SaberSlashEntity extends Projectile {
    private static final EntityDataAccessor<Float> SLASH_SCALE = SynchedEntityData.defineId(SaberSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TILT_ANGLE = SynchedEntityData.defineId(SaberSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_STRONG = SynchedEntityData.defineId(SaberSlashEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MAX_LIFETIME = SynchedEntityData.defineId(SaberSlashEntity.class, EntityDataSerializers.INT);

    public SaberSlashEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SaberSlashEntity(Level level, LivingEntity owner, Vec3 pos, float yRot, float xRot, float tiltAngle, float scale, boolean isStrong, int maxLifetime) {
        super(DbaEntities.SABER_SLASH, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.noPhysics = true;

        this.entityData.set(SLASH_SCALE, scale);
        this.entityData.set(TILT_ANGLE, tiltAngle);
        this.entityData.set(IS_STRONG, isStrong);
        this.entityData.set(MAX_LIFETIME, maxLifetime);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SLASH_SCALE, 1.0f);
        builder.define(TILT_ANGLE, 0.0f);
        builder.define(IS_STRONG, false);
        builder.define(MAX_LIFETIME, 10);
    }

    public float getSlashScale() {
        return this.entityData.get(SLASH_SCALE);
    }

    public float getTiltAngle() {
        return this.entityData.get(TILT_ANGLE);
    }

    public boolean isStrong() {
        return this.entityData.get(IS_STRONG);
    }

    public int getMaxLifetime() {
        return this.entityData.get(MAX_LIFETIME);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel) {
            if (this.tickCount >= getMaxLifetime()) {
                this.discard();
            }
        }
    }
}
