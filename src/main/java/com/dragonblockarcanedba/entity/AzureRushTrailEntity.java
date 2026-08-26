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
 * Azure Rush Trail Entity — Physical 3D aerodynamic dragon wind trail and Mach shock cone.
 */
public class AzureRushTrailEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> IS_DOUBLE_RUSH = SynchedEntityData.defineId(AzureRushTrailEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> TRAIL_SCALE = SynchedEntityData.defineId(AzureRushTrailEntity.class, EntityDataSerializers.FLOAT);

    private int maxLifetime = 12;

    public AzureRushTrailEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public AzureRushTrailEntity(Level level, LivingEntity owner, Vec3 pos, float yRot, float xRot, boolean isDoubleRush, float scale) {
        super(DbaEntities.AZURE_RUSH_TRAIL, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.noPhysics = true;
        this.entityData.set(IS_DOUBLE_RUSH, isDoubleRush);
        this.entityData.set(TRAIL_SCALE, scale);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IS_DOUBLE_RUSH, false);
        builder.define(TRAIL_SCALE, 1.0f);
    }

    public boolean isDoubleRush() {
        return this.entityData.get(IS_DOUBLE_RUSH);
    }

    public float getTrailScale() {
        return this.entityData.get(TRAIL_SCALE);
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
