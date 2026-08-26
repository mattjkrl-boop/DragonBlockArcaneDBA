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
 * Brave Rush Trail Entity — Physical 3D heroic supersonic Mach cone, twin streamlines, and concentrated kinetic thrust beam.
 */
public class BraveRushTrailEntity extends Projectile {
    private static final EntityDataAccessor<Float> TRAIL_SCALE = SynchedEntityData.defineId(BraveRushTrailEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TRAIL_LENGTH = SynchedEntityData.defineId(BraveRushTrailEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(BraveRushTrailEntity.class, EntityDataSerializers.INT);

    private int maxLifetime = 14;

    public BraveRushTrailEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BraveRushTrailEntity(Level level, LivingEntity owner, Vec3 pos, float yRot, float xRot, float length, float scale) {
        super(DbaEntities.BRAVE_RUSH_TRAIL, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.noPhysics = true;
        this.entityData.set(TRAIL_SCALE, scale);
        this.entityData.set(TRAIL_LENGTH, length);
        if (owner != null) {
            this.entityData.set(CASTER_ID, owner.getId());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TRAIL_SCALE, 1.0f);
        builder.define(TRAIL_LENGTH, 10.0f);
        builder.define(CASTER_ID, -1);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    public float getTrailScale() {
        return this.entityData.get(TRAIL_SCALE);
    }

    public float getTrailLength() {
        return this.entityData.get(TRAIL_LENGTH);
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
