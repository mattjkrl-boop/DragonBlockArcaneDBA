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
 * Time Shatter Entity — Physical 3D Prismatic Glass & Chrono-Mirror Shatter
 * spawned on Left-Click (Temporal Shatter) impact with the Whis Staff.
 */
public class TimeShatterEntity extends Projectile {
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(TimeShatterEntity.class, EntityDataSerializers.FLOAT);

    private int maxLifetime = 18;
    private int lifetime = 18;

    public TimeShatterEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public TimeShatterEntity(Level level, LivingEntity owner, Vec3 pos, float scale) {
        super(DbaEntities.TIME_SHATTER, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setScale(scale);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SCALE, 1.2f);
    }

    public float getShatterScale() {
        return this.entityData.get(SCALE);
    }

    public void setScale(float scale) {
        this.entityData.set(SCALE, scale);
    }

    public int getMaxLifetime() {
        return this.maxLifetime;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            this.lifetime--;
            if (this.lifetime <= 0) {
                this.discard();
            }
        }
    }
}
