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
 * Darkness Shatter Entity — Physical 3D Void Shatter Shockwave, Tectonic Fissure Trenches,
 * Erupting Obsidian Monolith Slabs, and Vertical Void Shock Pillars triggered during
 * Darkness Sword (Abyssal Eclipse) execution and falling blade detonations.
 */
public class DarknessShatterEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(DarknessShatterEntity.class, EntityDataSerializers.FLOAT);

    private int lifetime = 40; // 2.0 seconds

    public DarknessShatterEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DarknessShatterEntity(Level level, LivingEntity owner, Vec3 pos, float radius) {
        super(DbaEntities.DARKNESS_SHATTER, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setRadius(radius);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 4.0f);
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

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Theme particles (T5): the shatter vents void smoke and sparks
            // while the fissures are fresh, then settles.
            if (this.tickCount <= 8 && this.tickCount % 2 == 0) {
                float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
                if (density > 0f) {
                    serverLevel.sendParticles(
                        com.dragonblockarcanedba.particle.DbaParticles.VOID_SMOKE,
                        this.getX(), this.getY() + 0.3, this.getZ(),
                        Math.max(1, Math.round(4f * density)),
                        getRadius() * 0.5, 0.4, getRadius() * 0.5, 0.04);
                    serverLevel.sendParticles(
                        com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                        this.getX(), this.getY() + 0.3, this.getZ(),
                        Math.max(1, Math.round(2f * density)),
                        getRadius() * 0.4, 0.3, getRadius() * 0.4, 0.06);
                }
            }
            this.lifetime--;
            if (this.lifetime <= 0) {
                this.discard();
            }
        }
    }
}
