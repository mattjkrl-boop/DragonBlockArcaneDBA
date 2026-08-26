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
 * Swift Crescent Entity — Physical 3D volumetric crescent blade flash and cross-cleave speed cut
 * spawned on target during Katana Flashdraw execution strikes.
 */
public class SwiftCrescentEntity extends Projectile {
    private static final EntityDataAccessor<Float> SLASH_SCALE = SynchedEntityData.defineId(SwiftCrescentEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TILT_ANGLE = SynchedEntityData.defineId(SwiftCrescentEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MAX_LIFETIME = SynchedEntityData.defineId(SwiftCrescentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLASH_VARIANT = SynchedEntityData.defineId(SwiftCrescentEntity.class, EntityDataSerializers.INT);

    public SwiftCrescentEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SwiftCrescentEntity(Level level, LivingEntity owner, Vec3 pos, float yaw, float pitch, float tiltAngle, float scale, int variant, int maxLifetime) {
        super(DbaEntities.SWIFT_CRESCENT, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.noPhysics = true;

        this.entityData.set(SLASH_SCALE, scale);
        this.entityData.set(TILT_ANGLE, tiltAngle);
        this.entityData.set(SLASH_VARIANT, variant);
        this.entityData.set(MAX_LIFETIME, maxLifetime);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SLASH_SCALE, 1.0f);
        builder.define(TILT_ANGLE, 0.0f);
        builder.define(SLASH_VARIANT, 0);
        builder.define(MAX_LIFETIME, 10);
    }

    public float getSlashScale() {
        return this.entityData.get(SLASH_SCALE);
    }

    public float getTiltAngle() {
        return this.entityData.get(TILT_ANGLE);
    }

    public int getSlashVariant() {
        return this.entityData.get(SLASH_VARIANT);
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
