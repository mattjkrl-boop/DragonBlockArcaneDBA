package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Spirit Impale Entity — Physical 3D divine celestial swords, rotating judgement runic arrays,
 * ascending divine pillar, and crystalline shockwave spikes skewering and suspending the target.
 */
public class SpiritImpaleEntity extends Projectile {
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(SpiritImpaleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(SpiritImpaleEntity.class, EntityDataSerializers.INT);

    private int maxLifetime = 28;
    private int lifetime = 28;

    public SpiritImpaleEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SpiritImpaleEntity(Level level, LivingEntity owner, LivingEntity target, float scale) {
        super(DbaEntities.SPIRIT_IMPALE, level);
        this.setOwner(owner);
        this.noPhysics = true;
        this.setScale(scale);
        if (target != null) {
            this.setTargetId(target.getId());
            this.setPos(target.getX(), target.getY(), target.getZ());
        } else {
            this.setTargetId(-1);
            if (owner != null) {
                this.setPos(owner.getX(), owner.getY(), owner.getZ());
            }
        }
    }

    public SpiritImpaleEntity(Level level, LivingEntity owner, Vec3 pos, float scale) {
        super(DbaEntities.SPIRIT_IMPALE, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setScale(scale);
        this.setTargetId(-1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SCALE, 1.0f);
        builder.define(TARGET_ID, -1);
    }

    public float getImpaleScale() {
        return this.entityData.get(SCALE);
    }

    public void setScale(float scale) {
        this.entityData.set(SCALE, scale);
    }

    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    public void setTargetId(int id) {
        this.entityData.set(TARGET_ID, id);
    }

    public int getMaxLifetime() {
        return this.maxLifetime;
    }

    @Override
    public void tick() {
        super.tick();

        int targetId = getTargetId();
        if (targetId != -1) {
            Entity target = this.level().getEntity(targetId);
            if (target instanceof LivingEntity living && living.isAlive()) {
                this.setPos(living.getX(), living.getY(), living.getZ());
            }
        }

        if (!this.level().isClientSide()) {
            this.lifetime--;
            if (this.lifetime <= 0) {
                this.discard();
            }
        }
    }
}
