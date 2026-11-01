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
 * Devil Slam Shockwave Entity — Physical 3D Demonic Ground-Shatter Fissure Trenches,
 * Erupting Jagged Brimstone / Obsidian Impalement Spikes, Expanding Demonic Impact Seals,
 * and Surging Vertical Hellfire Eruption Column triggered by Devil Trident ground slam deployment.
 */
public class DevilSlamShockwaveEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(DevilSlamShockwaveEntity.class, EntityDataSerializers.FLOAT);

    private int lifetime = 35; // 1.75 seconds

    public DevilSlamShockwaveEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DevilSlamShockwaveEntity(Level level, LivingEntity owner, Vec3 pos, float radius) {
        super(DbaEntities.DEVIL_SLAM_SHOCKWAVE, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setRadius(radius);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 5.0f);
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
            // Theme particles (redesign T5): hellfire embers vent while the
            // pillar stands, with a spark burst on the first tick.
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
                if (density > 0f) {
                    if (this.tickCount == 1) {
                        serverLevel.sendParticles(
                            com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                            this.getX(), this.getY() + 0.3, this.getZ(),
                            Math.max(2, Math.round(10f * density)),
                            getRadius() * 0.5, 0.4, getRadius() * 0.5, 0.10);
                    }
                    if (this.tickCount <= 12) {
                        serverLevel.sendParticles(
                            com.dragonblockarcanedba.particle.DbaParticles.EMBER,
                            this.getX(), this.getY() + 0.2, this.getZ(),
                            Math.max(1, Math.round(3f * density)),
                            getRadius() * 0.45, 0.5, getRadius() * 0.45, 0.06);
                    }
                }
            }

            this.lifetime--;
            if (this.lifetime <= 0) {
                this.discard();
            }
        }
    }
}
