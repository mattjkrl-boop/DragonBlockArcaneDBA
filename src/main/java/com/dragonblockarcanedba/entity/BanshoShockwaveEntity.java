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
 * Bansho Shockwave Entity — Physical 3D Emerald Tempest Shockwave.
 * Used for both the directional launch blast (conical compression wave) and radial impact bursts.
 */
public class BanshoShockwaveEntity extends Projectile {
    private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(BanshoShockwaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(BanshoShockwaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MAX_RADIUS = SynchedEntityData.defineId(BanshoShockwaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_CONE = SynchedEntityData.defineId(BanshoShockwaveEntity.class, EntityDataSerializers.BOOLEAN);

    private int maxLifetime = 14;

    public BanshoShockwaveEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BanshoShockwaveEntity(Level level, LivingEntity owner, Vec3 pos, float yaw, float pitch, float maxRadius, boolean isCone) {
        super(DbaEntities.BANSHO_SHOCKWAVE, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setEntityYaw(yaw);
        this.setEntityPitch(pitch);
        this.setMaxRadius(maxRadius);
        this.setIsCone(isCone);
        this.maxLifetime = 14;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(YAW, 0.0f);
        builder.define(PITCH, 0.0f);
        builder.define(MAX_RADIUS, 3.0f);
        builder.define(IS_CONE, false);
    }

    public float getEntityYaw() {
        return this.entityData.get(YAW);
    }

    public void setEntityYaw(float yaw) {
        this.entityData.set(YAW, yaw);
    }

    public float getEntityPitch() {
        return this.entityData.get(PITCH);
    }

    public void setEntityPitch(float pitch) {
        this.entityData.set(PITCH, pitch);
    }

    public float getMaxRadius() {
        return this.entityData.get(MAX_RADIUS);
    }

    public void setMaxRadius(float radius) {
        this.entityData.set(MAX_RADIUS, radius);
    }

    public boolean isCone() {
        return this.entityData.get(IS_CONE);
    }

    public void setIsCone(boolean isCone) {
        this.entityData.set(IS_CONE, isCone);
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
