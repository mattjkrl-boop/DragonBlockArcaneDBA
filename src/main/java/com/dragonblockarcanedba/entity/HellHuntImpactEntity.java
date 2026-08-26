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
 * Hell Hunt Impact Entity — Physical 3D demonic ground-shatter fissure trenches, erupting cursed blood/obsidian spikes,
 * expanding shockwave seals, and surging execution pillars triggered during Hell Hunt chain executions.
 */
public class HellHuntImpactEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(HellHuntImpactEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_FINAL = SynchedEntityData.defineId(HellHuntImpactEntity.class, EntityDataSerializers.BOOLEAN);

    private int lifetime = 35; // 1.75 seconds

    public HellHuntImpactEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public HellHuntImpactEntity(Level level, LivingEntity owner, Vec3 pos, float radius, boolean isFinal) {
        super(DbaEntities.HELL_HUNT_IMPACT, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setRadius(radius);
        this.setFinal(isFinal);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 3.0f);
        builder.define(IS_FINAL, false);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(RADIUS, radius);
    }

    public boolean isFinal() {
        return this.entityData.get(IS_FINAL);
    }

    public void setFinal(boolean isFinal) {
        this.entityData.set(IS_FINAL, isFinal);
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
