package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.FxEnvelope;
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
 * Grand Clash Spark Entity — Physical 3D energy clash, deflection rings, and explosive golden sparks
 * spawned when the Grand Sword's Grand Cyclone deflects enemy projectiles and Ki attacks.
 *
 * Life is 24 ticks under the shared FxEnvelope: the clash builds over the
 * first fifth, peaks, then dissipates over the last 45 percent, with server
 * particles scaled by the same envelope the renderer draws with. It used to
 * be a 10 tick flash at full alpha from tick one.
 */
public class GrandClashSparkEntity extends Projectile {
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(GrandClashSparkEntity.class, EntityDataSerializers.FLOAT);

    public static final int MAX_LIFETIME = 24;

    private int lifetime = MAX_LIFETIME;

    public GrandClashSparkEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public GrandClashSparkEntity(Level level, LivingEntity owner, Vec3 pos, float scale) {
        super(DbaEntities.GRAND_CLASH_SPARK, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setScale(scale);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SCALE, 1.0f);
    }

    public float getClashScale() {
        return this.entityData.get(SCALE);
    }

    public void setScale(float scale) {
        this.entityData.set(SCALE, scale);
    }

    public int getMaxLifetime() {
        return MAX_LIFETIME;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            int age = MAX_LIFETIME - this.lifetime + 1;
            float env = FxEnvelope.envelope(age, MAX_LIFETIME, 0.20f, 0.45f);
            float density = DbaEffectsConfig.effectDensity();
            if (density > 0f && env > 0f) {
                // Bright core motes at the clash point, density riding the envelope.
                int sparks = Math.max(1, Math.round(3f * density * env));
                serverLevel.sendParticles(DbaParticles.SPARK,
                    this.getX(), this.getY(), this.getZ(),
                    sparks, 0.30, 0.30, 0.30, 0.10);
                if (age % 3 == 0) {
                    serverLevel.sendParticles(DbaParticles.KI_MOTE,
                        this.getX(), this.getY(), this.getZ(),
                        Math.max(1, Math.round(2f * density * env)), 0.18, 0.18, 0.18, 0.04);
                }
            }

            this.lifetime--;
            if (this.lifetime <= 0) {
                this.discard();
            }
        }
    }
}
