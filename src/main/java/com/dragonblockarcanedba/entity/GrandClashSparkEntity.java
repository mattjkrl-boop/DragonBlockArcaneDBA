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
 * Grand Clash Spark Entity — Physical 3D energy clash, deflection rings, and explosive golden sparks
 * spawned when the Grand Sword's Grand Cyclone deflects enemy projectiles and Ki attacks.
 */
public class GrandClashSparkEntity extends Projectile {
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(GrandClashSparkEntity.class, EntityDataSerializers.FLOAT);

    private int maxLifetime = 10;
    private int lifetime = 10;

    public GrandClashSparkEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public GrandClashSparkEntity(Level level, LivingEntity owner, Vec3 pos, float scale) {
        super(DbaEntities.GRAND_CLASH_SPARK, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setScale(scale);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SCALE, 1.0f);
    }

    public float getClashScale() {
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
