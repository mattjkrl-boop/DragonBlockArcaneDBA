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
 * Bansho Shockwave Entity — Physical 3D Emerald Tempest Shockwave.
 * Used for both the directional launch blast (conical compression wave) and radial impact bursts.
 */
public class BanshoShockwaveEntity extends Projectile {
    private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(BanshoShockwaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(BanshoShockwaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MAX_RADIUS = SynchedEntityData.defineId(BanshoShockwaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_CONE = SynchedEntityData.defineId(BanshoShockwaveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ORIENT_TO_IMPACT = SynchedEntityData.defineId(BanshoShockwaveEntity.class, EntityDataSerializers.BOOLEAN);

    private int maxLifetime = 14;

    public BanshoShockwaveEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BanshoShockwaveEntity(Level level, LivingEntity owner, Vec3 pos, float yaw, float pitch, float maxRadius, boolean isCone) {
        super(DbaEntities.BANSHO_SHOCKWAVE, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setEntityYaw(yaw);
        this.setEntityPitch(pitch);
        this.setMaxRadius(maxRadius);
        this.setIsCone(isCone);
        this.maxLifetime = 14;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(YAW, 0.0f);
        builder.define(PITCH, 0.0f);
        builder.define(MAX_RADIUS, 3.0f);
        builder.define(IS_CONE, false);
        builder.define(ORIENT_TO_IMPACT, false);
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

    public float getMaxRadius() {
        return this.entityData.get(MAX_RADIUS);
    }

    public void setMaxRadius(float radius) {
        this.entityData.set(MAX_RADIUS, radius);
    }

    public boolean isCone() {
        return this.entityData.get(IS_CONE);
    }

    public void setIsCone(boolean isCone) {
        this.entityData.set(IS_CONE, isCone);
    }

    public boolean isOrientToImpact() {
        return this.entityData.get(ORIENT_TO_IMPACT);
    }

    public void setOrientToImpact(boolean orient) {
        this.entityData.set(ORIENT_TO_IMPACT, orient);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Particle accent (SPECTACLE-STANDARD point 1): the mesh ring used
            // to be the whole effect. Radial bursts now throw wind wisps off
            // the expanding rim; cone waves keep their mesh-only read.
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                && !isCone() && this.tickCount % 2 == 0) {
                float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
                if (density > 0f) {
                    float progress = this.tickCount / (float) this.maxLifetime;
                    double rimRadius = Math.max(0.3, getMaxRadius() * progress);
                    int count = Math.max(1, Math.round(4f * density));
                    if (isOrientToImpact()) {
                        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.WIND,
                            this.getX(), this.getY(), this.getZ(),
                            count, 0.3, 0.3, 0.3, 0.05);
                    } else {
                        for (int i = 0; i < count; i++) {
                            double a = serverLevel.getRandom().nextDouble() * Math.PI * 2.0;
                            serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.WIND,
                                this.getX() + Math.cos(a) * rimRadius, this.getY() + 0.15, this.getZ() + Math.sin(a) * rimRadius,
                                1, 0.06, 0.04, 0.06, 0.03);
                        }
                    }
                }
            }
            if (this.tickCount >= this.maxLifetime) {
                this.discard();
            }
        }
    }
}
