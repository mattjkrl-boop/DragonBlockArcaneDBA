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
 * Curse Ground Shatter Entity — Physical 3D Tectonic Fissure Trenches, Erupting Obsidian Debris Slabs,
 * and Shockwave Impact Disks triggered by Abyssal Eclipse max-charge ground slams.
 */
public class CurseGroundShatterEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(CurseGroundShatterEntity.class, EntityDataSerializers.FLOAT);

    private int lifetime = 35; // 1.75 seconds

    public CurseGroundShatterEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public CurseGroundShatterEntity(Level level, LivingEntity owner, Vec3 pos, float radius) {
        super(DbaEntities.CURSE_GROUND_SHATTER, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setRadius(radius);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 3.5f);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(RADIUS, radius);
    }

    public int getLifetime() {
        return this.lifetime;
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
