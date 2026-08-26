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
 * Heaven Splitter Entity — Towering physical 3D dimensional slash model and vertical reality slice
 * rendered across the entire dash path during Katana Iaijutsu: Heaven Splitter execution.
 */
public class HeavenSplitterEntity extends Projectile {
    private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(HeavenSplitterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(HeavenSplitterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(HeavenSplitterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DASH_LENGTH = SynchedEntityData.defineId(HeavenSplitterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SLASH_SCALE = SynchedEntityData.defineId(HeavenSplitterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MAX_LIFETIME = SynchedEntityData.defineId(HeavenSplitterEntity.class, EntityDataSerializers.INT);

    public HeavenSplitterEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public HeavenSplitterEntity(Level level, LivingEntity owner, Vec3 startPos, Vec3 direction, float dashLength, float scale, int maxLifetime) {
        super(DbaEntities.HEAVEN_SPLITTER, level);
        this.setOwner(owner);
        this.setPos(startPos.x, startPos.y, startPos.z);
        this.noPhysics = true;

        Vec3 normDir = direction.normalize();
        this.entityData.set(DIR_X, (float) normDir.x);
        this.entityData.set(DIR_Y, (float) normDir.y);
        this.entityData.set(DIR_Z, (float) normDir.z);
        this.entityData.set(DASH_LENGTH, dashLength);
        this.entityData.set(SLASH_SCALE, scale);
        this.entityData.set(MAX_LIFETIME, maxLifetime);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DIR_X, 1.0f);
        builder.define(DIR_Y, 0.0f);
        builder.define(DIR_Z, 0.0f);
        builder.define(DASH_LENGTH, 16.0f);
        builder.define(SLASH_SCALE, 1.0f);
        builder.define(MAX_LIFETIME, 20);
    }

    public Vec3 getDashDirection() {
        return new Vec3(this.entityData.get(DIR_X), this.entityData.get(DIR_Y), this.entityData.get(DIR_Z));
    }

    public float getDashLength() {
        return this.entityData.get(DASH_LENGTH);
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
