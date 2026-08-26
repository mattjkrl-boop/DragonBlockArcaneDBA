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
 * Saber Void Tear Entity — Physical 3D void warp tear & expanding sonic boom shock rings spawned at
 * Flash Step teleport origin and destination positions.
 */
public class SaberVoidTearEntity extends Projectile {
    private static final EntityDataAccessor<Float> TEAR_SCALE = SynchedEntityData.defineId(SaberVoidTearEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_DESTINATION = SynchedEntityData.defineId(SaberVoidTearEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MAX_LIFETIME = SynchedEntityData.defineId(SaberVoidTearEntity.class, EntityDataSerializers.INT);

    public SaberVoidTearEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SaberVoidTearEntity(Level level, LivingEntity owner, Vec3 pos, float yRot, float xRot, float scale, boolean isDestination, int maxLifetime) {
        super(DbaEntities.SABER_VOID_TEAR, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.noPhysics = true;

        this.entityData.set(TEAR_SCALE, scale);
        this.entityData.set(IS_DESTINATION, isDestination);
        this.entityData.set(MAX_LIFETIME, maxLifetime);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TEAR_SCALE, 1.0f);
        builder.define(IS_DESTINATION, false);
        builder.define(MAX_LIFETIME, 12);
    }

    public float getTearScale() {
        return this.entityData.get(TEAR_SCALE);
    }

    public boolean isDestination() {
        return this.entityData.get(IS_DESTINATION);
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
