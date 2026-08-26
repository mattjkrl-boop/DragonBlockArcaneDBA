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
 * King's Slam Entity — Physical 3D ground shatter, radiating tectonic canyon trenches,
 * colossal erupted basalt monoliths, and expanding volcanic shockwave dome.
 * Spawned on Right-Click release for Normal Slam (10-block) and Flawless King's Slam (20-block).
 */
public class KingsSlamEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(KingsSlamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_FLAWLESS = SynchedEntityData.defineId(KingsSlamEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(KingsSlamEntity.class, EntityDataSerializers.INT);

    public KingsSlamEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public KingsSlamEntity(Level level, LivingEntity caster, Vec3 pos, float radius, boolean isFlawless) {
        super(DbaEntities.KINGS_SLAM, level);
        this.setOwner(caster);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;

        int life = isFlawless ? 55 : 40;
        this.entityData.set(RADIUS, radius);
        this.entityData.set(IS_FLAWLESS, isFlawless);
        this.entityData.set(LIFETIME, life);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 10.0f);
        builder.define(IS_FLAWLESS, false);
        builder.define(LIFETIME, 40);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public boolean isFlawless() {
        return this.entityData.get(IS_FLAWLESS);
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.tickCount >= getLifetime()) {
                this.discard();
            }
        }
    }
}
