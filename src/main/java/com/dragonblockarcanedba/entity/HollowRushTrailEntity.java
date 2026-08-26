package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Hollow Rush Trail Entity — Physical 3D volumetric void corridor and dimensional puncture rings spanning dash trajectory.
 */
public class HollowRushTrailEntity extends Projectile {
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(HollowRushTrailEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_THIRD_DASH = SynchedEntityData.defineId(HollowRushTrailEntity.class, EntityDataSerializers.BOOLEAN);

    private int maxLifetime = 14;

    public HollowRushTrailEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public HollowRushTrailEntity(Level level, LivingEntity owner, Vec3 startPos, float yRot, float xRot, float length, boolean isThirdDash) {
        super(DbaEntities.HOLLOW_RUSH_TRAIL, level);
        this.setOwner(owner);
        this.setPos(startPos.x, startPos.y, startPos.z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.noPhysics = true;
        this.entityData.set(LENGTH, length);
        this.entityData.set(IS_THIRD_DASH, isThirdDash);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LENGTH, 5.0f);
        builder.define(IS_THIRD_DASH, false);
    }

    public float getTrailLength() {
        return this.entityData.get(LENGTH);
    }

    public boolean isThirdDash() {
        return this.entityData.get(IS_THIRD_DASH);
    }

    public int getMaxLifetime() {
        return this.maxLifetime;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (this.tickCount >= this.maxLifetime) {
                this.discard();
            }
        }
    }
}
