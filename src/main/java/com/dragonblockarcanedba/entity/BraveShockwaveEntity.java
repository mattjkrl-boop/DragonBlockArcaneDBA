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
 * Brave Shockwave Entity — Physical 3D geometric heroic shockwave, raised beveled ring, and crystalline valor needles on Brave Sword dash impact.
 */
public class BraveShockwaveEntity extends Projectile {
    private static final EntityDataAccessor<Float> MAX_RADIUS = SynchedEntityData.defineId(BraveShockwaveEntity.class, EntityDataSerializers.FLOAT);

    private int maxLifetime = 18;

    public BraveShockwaveEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BraveShockwaveEntity(Level level, LivingEntity owner, Vec3 pos, float maxRadius) {
        super(DbaEntities.BRAVE_SHOCKWAVE, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.entityData.set(MAX_RADIUS, maxRadius);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MAX_RADIUS, 7.5f);
    }

    public float getMaxRadius() {
        return this.entityData.get(MAX_RADIUS);
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
