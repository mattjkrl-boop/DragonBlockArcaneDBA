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
 * Curse Ground Shatter Entity: Physical 3D Tectonic Fissure Trenches, Erupting Obsidian Debris Slabs,
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

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Theme particles (T5): the shatter vents void smoke while the
            // fissures are fresh, then settles.
            if (this.tickCount <= 8 && this.tickCount % 2 == 0) {
                float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
                if (density > 0f) {
                    serverLevel.sendParticles(
                        com.dragonblockarcanedba.particle.DbaParticles.VOID_SMOKE,
                        this.getX(), this.getY() + 0.3, this.getZ(),
                        Math.max(1, Math.round(4f * density)),
                        getRadius() * 0.5, 0.4, getRadius() * 0.5, 0.04);
                }
            }
            this.lifetime--;
            if (this.lifetime <= 0) {
                this.discard();
            }
        }
    }
}
