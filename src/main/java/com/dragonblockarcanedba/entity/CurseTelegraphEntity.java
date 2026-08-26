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
 * Curse Telegraph Entity — 3D Ground Runic Decal and Contracting Implosion Ring for Curse Lightning Strikes.
 * Replaces vanilla particle circles with a precise, high-fidelity 3D warning projector.
 */
public class CurseTelegraphEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(CurseTelegraphEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> REMAINING_TICKS = SynchedEntityData.defineId(CurseTelegraphEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAX_TICKS = SynchedEntityData.defineId(CurseTelegraphEntity.class, EntityDataSerializers.INT);

    public CurseTelegraphEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public CurseTelegraphEntity(Level level, LivingEntity owner, Vec3 pos, float radius, int durationTicks) {
        super(DbaEntities.CURSE_TELEGRAPH, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setRadius(radius);
        this.entityData.set(REMAINING_TICKS, durationTicks);
        this.entityData.set(MAX_TICKS, durationTicks);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 1.8f);
        builder.define(REMAINING_TICKS, 20);
        builder.define(MAX_TICKS, 20);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(RADIUS, radius);
    }

    public int getRemainingTicks() {
        return this.entityData.get(REMAINING_TICKS);
    }

    public int getMaxTicks() {
        return this.entityData.get(MAX_TICKS);
    }

    public float getProgress(float partialTicks) {
        int max = getMaxTicks();
        if (max <= 0) return 1.0f;
        float remaining = Math.max(0.0f, getRemainingTicks() - partialTicks);
        return 1.0f - (remaining / (float) max);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            int rem = getRemainingTicks() - 1;
            this.entityData.set(REMAINING_TICKS, rem);
            if (rem <= 0) {
                this.discard();
            }
        }
    }
}
