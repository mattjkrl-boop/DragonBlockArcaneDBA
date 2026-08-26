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
 * Power Pole Whirlwind Entity — Physical 3D Conical Hurricane Vortex.
 * Spawns upon swinging the Power Pole (Whirlwind Staff), projecting a multi-layered aerodynamic
 * gale cone, quad helical wind ribbons, orbiting crescent gale blades, and a staff-origin wind sweep disc.
 */
public class PowerPoleWhirlwindEntity extends Projectile {
    private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(PowerPoleWhirlwindEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(PowerPoleWhirlwindEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RANGE = SynchedEntityData.defineId(PowerPoleWhirlwindEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CONE_ANGLE = SynchedEntityData.defineId(PowerPoleWhirlwindEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(PowerPoleWhirlwindEntity.class, EntityDataSerializers.INT);

    private int maxLifetime = 8;

    public PowerPoleWhirlwindEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public PowerPoleWhirlwindEntity(Level level, LivingEntity owner, Vec3 pos, float yaw, float pitch, float range, float coneAngle) {
        super(DbaEntities.POWER_POLE_WHIRLWIND, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setEntityYaw(yaw);
        this.setEntityPitch(pitch);
        this.setRange(range);
        this.setConeAngle(coneAngle);
        if (owner != null) {
            this.entityData.set(CASTER_ID, owner.getId());
        }
        this.maxLifetime = 8;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(YAW, 0.0f);
        builder.define(PITCH, 0.0f);
        builder.define(RANGE, 25.0f);
        builder.define(CONE_ANGLE, 35.0f);
        builder.define(CASTER_ID, -1);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    public float getEntityYaw() {
        return this.entityData.get(YAW);
    }

    public void setEntityYaw(float yaw) {
        this.entityData.set(YAW, yaw);
    }

    public float getEntityPitch() {
        return this.entityData.get(PITCH);
    }

    public void setEntityPitch(float pitch) {
        this.entityData.set(PITCH, pitch);
    }

    public float getRange() {
        return this.entityData.get(RANGE);
    }

    public void setRange(float range) {
        this.entityData.set(RANGE, range);
    }

    public float getConeAngle() {
        return this.entityData.get(CONE_ANGLE);
    }

    public void setConeAngle(float coneAngle) {
        this.entityData.set(CONE_ANGLE, coneAngle);
    }

    public int getMaxLifetime() {
        return this.maxLifetime;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getOwner() instanceof LivingEntity owner && owner.isAlive()) {
            Vec3 eyePos = owner.getEyePosition();
            this.setPos(eyePos.x, eyePos.y, eyePos.z);
            this.setEntityYaw(owner.getYRot());
            this.setEntityPitch(owner.getXRot());
        }

        if (!this.level().isClientSide()) {
            if (this.tickCount >= this.maxLifetime) {
                this.discard();
            }
        }
    }
}
