package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Darkness Domain Entity — Physical 3D Geometric Void-Sphere Dome for Darkness Sword (World of Darkness & Abyssal Eclipse).
 * Tracks the expanding domain radius (8.0 to 30.0 blocks) and renders the volumetric void dome,
 * quad-tier ground seals, orbiting 3D obsidian spires, central vortex column, and atmospheric dark lightning.
 */
public class DarknessDomainEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(DarknessDomainEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(DarknessDomainEntity.class, EntityDataSerializers.INT);

    private int lifetime = 40; // Keeps alive while channeled

    public DarknessDomainEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DarknessDomainEntity(Level level, LivingEntity caster, Vec3 pos, float radius) {
        super(DbaEntities.DARKNESS_DOMAIN, level);
        this.setOwner(caster);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setRadius(radius);
        this.entityData.set(CASTER_ID, caster != null ? caster.getId() : -1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 8.0f);
        builder.define(CASTER_ID, -1);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(RADIUS, radius);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    public void refreshLifetime() {
        this.lifetime = 10; // Keep alive as long as refreshed each tick
    }

    @Override
    public void tick() {
        super.tick();

        Entity owner = this.getOwner();
        if (owner == null && getCasterId() != -1 && this.level() instanceof ServerLevel serverLevel) {
            owner = serverLevel.getEntity(getCasterId());
            if (owner != null) {
                this.setOwner(owner);
            }
        }

        if (owner != null && owner.isAlive()) {
            this.setPos(owner.getX(), owner.getY(), owner.getZ());
        }

        if (!this.level().isClientSide()) {
            this.lifetime--;
            if (this.lifetime <= 0 || (owner != null && !owner.isAlive())) {
                this.discard();
            }
        }
    }
}
