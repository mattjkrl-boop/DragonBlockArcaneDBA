package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Temporal Rift Entity — Physical 3D Translucent Celestial Dome & Rotating Ground Astrolabe
 * spawned on Right-Click (Temporal Rift) with the Whis Staff.
 */
public class TemporalRiftEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(TemporalRiftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(TemporalRiftEntity.class, EntityDataSerializers.INT);

    private int maxLifetime = 60;
    private int lifetime = 60;

    public TemporalRiftEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public TemporalRiftEntity(Level level, LivingEntity caster, Vec3 pos, float radius, int lifetime) {
        super(DbaEntities.TEMPORAL_RIFT, level);
        this.setOwner(caster);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setRadius(radius);
        this.maxLifetime = lifetime;
        this.lifetime = lifetime;
        this.entityData.set(CASTER_ID, caster != null ? caster.getId() : -1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 12.0f);
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

    public int getMaxLifetime() {
        return this.maxLifetime;
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

        if (this.level() instanceof ServerLevel serverLevel) {
            // Ambient temporal hum sound
            if (this.tickCount % 20 == 0) {
                serverLevel.playSound(
                    null, this.getX(), this.getY() + 1.0, this.getZ(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                    0.8f, 1.2f
                );
            }

            this.lifetime--;
            if (this.lifetime <= 0) {
                this.discard();
            }
        }
    }
}
