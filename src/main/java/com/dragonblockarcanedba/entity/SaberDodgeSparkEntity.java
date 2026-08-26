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
 * Saber Dodge Spark Entity — Physical 3D geometric golden deflection shield shatter & clash spark
 * burst spawned on Perfect Dodge counter.
 */
public class SaberDodgeSparkEntity extends Projectile {
    private static final EntityDataAccessor<Float> SPARK_SCALE = SynchedEntityData.defineId(SaberDodgeSparkEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MAX_LIFETIME = SynchedEntityData.defineId(SaberDodgeSparkEntity.class, EntityDataSerializers.INT);

    public SaberDodgeSparkEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SaberDodgeSparkEntity(Level level, LivingEntity owner, Vec3 pos, float scale, int maxLifetime) {
        super(DbaEntities.SABER_DODGE_SPARK, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;

        this.entityData.set(SPARK_SCALE, scale);
        this.entityData.set(MAX_LIFETIME, maxLifetime);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SPARK_SCALE, 1.0f);
        builder.define(MAX_LIFETIME, 12);
    }

    public float getSparkScale() {
        return this.entityData.get(SPARK_SCALE);
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
