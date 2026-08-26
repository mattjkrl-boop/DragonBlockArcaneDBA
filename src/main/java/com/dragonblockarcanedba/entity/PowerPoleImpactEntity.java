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
 * Power Pole Impact Entity — Physical 3D Kinetic Shatter & Impact Shockwave.
 * Spawns upon Power Pole thrust impact, rendering expanding dual golden/crimson shockwave rings,
 * radial kinetic compression dome, 3D golden/ruby shatter fragments, and starburst impact spikes.
 */
public class PowerPoleImpactEntity extends Projectile {
    private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(PowerPoleImpactEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(PowerPoleImpactEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(PowerPoleImpactEntity.class, EntityDataSerializers.FLOAT);

    private int maxLifetime = 14;

    public PowerPoleImpactEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public PowerPoleImpactEntity(Level level, LivingEntity owner, Vec3 pos, float yaw, float pitch, float scale) {
        super(DbaEntities.POWER_POLE_IMPACT, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setEntityYaw(yaw);
        this.setEntityPitch(pitch);
        this.setImpactScale(scale);
        this.maxLifetime = 14;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(YAW, 0.0f);
        builder.define(PITCH, 0.0f);
        builder.define(SCALE, 2.2f);
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

    public float getImpactScale() {
        return this.entityData.get(SCALE);
    }

    public void setImpactScale(float scale) {
        this.entityData.set(SCALE, scale);
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
