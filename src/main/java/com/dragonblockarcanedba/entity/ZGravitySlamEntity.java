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
 * Z Gravity Slam Entity — Physical 3D ground shatter, radiating tectonic canyon fracture trenches,
 * colossal erupted Katchin monolith slabs, and expanding gravitational shockwave dome.
 * Spawned on Right-Click release for the Z Sword Gravity Slam (6 to 18 blocks).
 */
public class ZGravitySlamEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(ZGravitySlamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> POWER_RATIO = SynchedEntityData.defineId(ZGravitySlamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(ZGravitySlamEntity.class, EntityDataSerializers.INT);

    public ZGravitySlamEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public ZGravitySlamEntity(Level level, LivingEntity caster, Vec3 pos, float radius, float powerRatio) {
        super(DbaEntities.Z_GRAVITY_SLAM, level);
        this.setOwner(caster);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;

        int life = 45 + (int) (powerRatio * 15); // 45 to 60 ticks
        this.entityData.set(RADIUS, radius);
        this.entityData.set(POWER_RATIO, powerRatio);
        this.entityData.set(LIFETIME, life);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 10.0f);
        builder.define(POWER_RATIO, 1.0f);
        builder.define(LIFETIME, 50);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public float getPowerRatio() {
        return this.entityData.get(POWER_RATIO);
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            // Shared slam debris (SlamDebris): one ground burst on the first tick.
            if (this.tickCount == 1) {
                com.dragonblockarcanedba.fx.SlamDebris.burst(serverLevel, this.position(), getRadius(), com.dragonblockarcanedba.fx.SlamDebris.Profile.Z_GRAVITY);
            }
            // Theme particles: gold sparks and embers keep rising off the
            // trenches for the whole slam instead of only the first tick.
            if (this.tickCount % 3 == 0) {
                float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
                if (density > 0f) {
                    double spread = getRadius() * 0.6;
                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                        this.getX(), this.getY() + 0.3, this.getZ(),
                        Math.max(2, Math.round((2f + getPowerRatio() * 3f) * density)),
                        spread, 0.4, spread, 0.06);
                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.EMBER,
                        this.getX(), this.getY() + 0.3, this.getZ(),
                        Math.max(1, Math.round(2f * density)),
                        spread, 0.4, spread, 0.04);
                }
            }
            if (this.tickCount >= getLifetime()) {
                this.discard();
            }
        }
    }
}
