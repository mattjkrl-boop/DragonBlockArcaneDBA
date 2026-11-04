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
 * Abyssal Domain Entity: Physical 3D Geometric Void Storm Domain for Curse Blade (Abyssal Eclipse).
 * Tracks the expanding domain radius (10.0 to 30.0 blocks) and renders the volumetric void dome,
 * quad-tier ground whirlpools, orbiting void spires, central vortex column, and atmospheric arcs.
 */
public class AbyssalDomainEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(AbyssalDomainEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(AbyssalDomainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FADE_REMAINING = SynchedEntityData.defineId(AbyssalDomainEntity.class, EntityDataSerializers.INT);

    /** Ticks the dome spends collapsing once the channel stops refreshing it. */
    public static final int FADE_TICKS = 30;

    private int lifetime = 10; // Matches refreshLifetime: alive only while channeled

    public AbyssalDomainEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public AbyssalDomainEntity(Level level, LivingEntity caster, Vec3 pos, float radius) {
        super(DbaEntities.ABYSSAL_DOMAIN, level);
        this.setOwner(caster);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setRadius(radius);
        this.entityData.set(CASTER_ID, caster != null ? caster.getId() : -1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 10.0f);
        builder.define(CASTER_ID, -1);
        builder.define(FADE_REMAINING, -1);
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
        // A re-channel during the collapse revives the dome instead of
        // fighting the fade.
        if (getFadeRemaining() >= 0) {
            this.entityData.set(FADE_REMAINING, -1);
        }
    }

    /** Remaining dissipation ticks, or -1 while the storm is standing. */
    public int getFadeRemaining() {
        return this.entityData.get(FADE_REMAINING);
    }

    /** 1.0 while standing, falling to 0 across the dissipation. */
    public float getFadeFraction() {
        int f = getFadeRemaining();
        return f < 0 ? 1.0f : Math.max(0.0f, f / (float) FADE_TICKS);
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

        // Layered storm particles (point 1): the shared three layer vortex
        // emitter replaces the old flat void smoke puffs, and the dying storm
        // winds down through the same envelope while theme rain falls.
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 2 == 0) {
            float height = Math.min(24.0f, getRadius() * 1.30f);
            Vec3 center = new Vec3(this.getX(), this.getY() + 0.4, this.getZ());
            int fade = getFadeRemaining();
            if (fade >= 0) {
                float age = 70.0f + (1.0f - fade / (float) FADE_TICKS) * 30.0f;
                com.dragonblockarcanedba.util.LayeredParticles.emitVortex(serverLevel, center, getRadius() * 0.8f, height, age, 100.0f, 0xFFE61230,
                    com.dragonblockarcanedba.particle.DbaParticles.VOID_SMOKE);
                com.dragonblockarcanedba.util.LayeredParticles.emitRain(serverLevel, center, getRadius(), height, age, 100.0f,
                    com.dragonblockarcanedba.particle.DbaParticles.VOID_SMOKE);
            } else {
                // The synthetic age and life pin the shared envelope at its
                // peak; the channel build up is carried by the growing radius.
                com.dragonblockarcanedba.util.LayeredParticles.emitVortex(serverLevel, center, getRadius() * 0.8f, height, 50.0f, 100.0f, 0xFFE61230,
                    com.dragonblockarcanedba.particle.DbaParticles.VOID_SMOKE);
            }
        }

        if (!this.level().isClientSide()) {
            if (getFadeRemaining() >= 0) {
                // Dissipation (point 5): collapse over FADE_TICKS instead of
                // vanishing the tick the channel ends.
                int f = getFadeRemaining() - 1;
                this.entityData.set(FADE_REMAINING, f);
                if (f <= 0) {
                    this.discard();
                }
            } else {
                this.lifetime--;
                if (this.lifetime <= 0 || (owner != null && !owner.isAlive())) {
                    this.entityData.set(FADE_REMAINING, FADE_TICKS);
                }
            }
        }
    }
}
