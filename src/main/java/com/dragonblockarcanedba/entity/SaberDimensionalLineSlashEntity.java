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
 * Saber Dimensional Line Slash Entity — Massive physical 3D reality cut beam drawn along the
 * computed 3D Best-Fit Line Snap Finisher vector.
 */
public class SaberDimensionalLineSlashEntity extends Projectile {
    private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(SaberDimensionalLineSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(SaberDimensionalLineSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(SaberDimensionalLineSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LINE_LENGTH = SynchedEntityData.defineId(SaberDimensionalLineSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SLASH_SCALE = SynchedEntityData.defineId(SaberDimensionalLineSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MAX_LIFETIME = SynchedEntityData.defineId(SaberDimensionalLineSlashEntity.class, EntityDataSerializers.INT);

    public SaberDimensionalLineSlashEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SaberDimensionalLineSlashEntity(Level level, LivingEntity owner, Vec3 centroid, Vec3 direction, float lineLength, float scale, int maxLifetime) {
        super(DbaEntities.SABER_LINE_SLASH, level);
        this.setOwner(owner);
        this.setPos(centroid.x, centroid.y, centroid.z);
        this.noPhysics = true;

        Vec3 normDir = direction.normalize();
        this.entityData.set(DIR_X, (float) normDir.x);
        this.entityData.set(DIR_Y, (float) normDir.y);
        this.entityData.set(DIR_Z, (float) normDir.z);
        this.entityData.set(LINE_LENGTH, lineLength);
        this.entityData.set(SLASH_SCALE, scale);
        this.entityData.set(MAX_LIFETIME, maxLifetime);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DIR_X, 1.0f);
        builder.define(DIR_Y, 0.0f);
        builder.define(DIR_Z, 0.0f);
        builder.define(LINE_LENGTH, 10.0f);
        builder.define(SLASH_SCALE, 1.0f);
        builder.define(MAX_LIFETIME, 20);
    }

    public Vec3 getLineDirection() {
        return new Vec3(this.entityData.get(DIR_X), this.entityData.get(DIR_Y), this.entityData.get(DIR_Z));
    }

    public float getLineLength() {
        return this.entityData.get(LINE_LENGTH);
    }

    public float getSlashScale() {
        return this.entityData.get(SLASH_SCALE);
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
