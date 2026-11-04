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

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.FxEnvelope;

/**
 * Power Pole Extension Entity — Physical 3D Stretching Staff.
 * Spawns upon Right-Click (Extended Reach), rendering a solid stretching 3D crimson pole,
 * ornate metallic gold caps/fittings, golden dragon spiral coils, and supersonic Mach compression shock cones.
 */
public class PowerPoleExtensionEntity extends Projectile {
    private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(PowerPoleExtensionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(PowerPoleExtensionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MAX_LENGTH = SynchedEntityData.defineId(PowerPoleExtensionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(PowerPoleExtensionEntity.class, EntityDataSerializers.INT);

    private int maxLifetime = 12;

    public PowerPoleExtensionEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public PowerPoleExtensionEntity(Level level, LivingEntity owner, Vec3 pos, float yaw, float pitch, float maxLength) {
        super(DbaEntities.POWER_POLE_EXTENSION, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setEntityYaw(yaw);
        this.setEntityPitch(pitch);
        this.setMaxLength(maxLength);
        this.maxLifetime = 12;
        if (owner != null) {
            this.entityData.set(CASTER_ID, owner.getId());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(YAW, 0.0f);
        builder.define(PITCH, 0.0f);
        builder.define(MAX_LENGTH, 30.0f);
        builder.define(CASTER_ID, -1);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    public void setCasterId(int id) {
        this.entityData.set(CASTER_ID, id);
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

    public float getMaxLength() {
        return this.entityData.get(MAX_LENGTH);
    }

    public void setMaxLength(float length) {
        this.entityData.set(MAX_LENGTH, length);
    }

    public int getMaxLifetime() {
        return this.maxLifetime;
    }

    /** Extension fraction 0..1 matching the renderer's ease: out over 3 ticks, hold, back over the last 3. */
    private float extensionFraction() {
        float age = this.tickCount;
        if (age < 3.0f) {
            float t = age / 3.0f;
            return 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
        }
        if (age > 9.0f) {
            return 1.0f - FxEnvelope.smoothstep(Math.min(1.0f, (age - 9.0f) / 3.0f));
        }
        return 1.0f;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getOwner() instanceof LivingEntity owner && owner.isAlive()) {
            Vec3 eyePos = owner.getEyePosition();
            this.setPos(eyePos.x, eyePos.y, eyePos.z);
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Layered trail (SPECTACLE-STANDARD point 1): wind streaming off the
            // shaft along its whole current length, ki motes crowding the tip,
            // so the strike reads as energy in motion, not a bare mesh.
            float density = DbaEffectsConfig.effectDensity();
            if (density > 0f) {
                Vec3 look = this.calculateViewVector(this.getEntityPitch(), this.getEntityYaw());
                double len = Math.max(0.8, getMaxLength() * extensionFraction());
                var rand = serverLevel.getRandom();
                int shaftCount = Math.max(1, Math.round(2f * density));
                for (int i = 0; i < shaftCount; i++) {
                    double t = 0.15 + 0.8 * rand.nextDouble();
                    Vec3 p = this.position().add(look.scale(len * t));
                    serverLevel.sendParticles(DbaParticles.WIND,
                        p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.02);
                }
                Vec3 tip = this.position().add(look.scale(len));
                serverLevel.sendParticles(DbaParticles.KI_MOTE,
                    tip.x, tip.y, tip.z,
                    Math.max(1, Math.round(2f * density)), 0.08, 0.08, 0.08, 0.02);
            }
            if (this.tickCount >= this.maxLifetime) {
                this.discard();
            }
        }
    }
}
