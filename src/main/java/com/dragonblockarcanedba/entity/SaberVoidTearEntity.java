package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.LayeredParticles;
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
 * Saber Void Tear Entity: Physical 3D void warp tear & expanding sonic boom shock rings spawned at
 * Flash Step teleport origin and destination positions.
 */
public class SaberVoidTearEntity extends Projectile {
    private static final EntityDataAccessor<Float> TEAR_SCALE = SynchedEntityData.defineId(SaberVoidTearEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_DESTINATION = SynchedEntityData.defineId(SaberVoidTearEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MAX_LIFETIME = SynchedEntityData.defineId(SaberVoidTearEntity.class, EntityDataSerializers.INT);

    public SaberVoidTearEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SaberVoidTearEntity(Level level, LivingEntity owner, Vec3 pos, float yRot, float xRot, float scale, boolean isDestination, int maxLifetime) {
        super(DbaEntities.SABER_VOID_TEAR, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.noPhysics = true;

        this.entityData.set(TEAR_SCALE, scale);
        this.entityData.set(IS_DESTINATION, isDestination);
        this.entityData.set(MAX_LIFETIME, maxLifetime);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TEAR_SCALE, 1.0f);
        builder.define(IS_DESTINATION, false);
        builder.define(MAX_LIFETIME, 20);
    }

    public float getTearScale() {
        return this.entityData.get(TEAR_SCALE);
    }

    public boolean isDestination() {
        return this.entityData.get(IS_DESTINATION);
    }

    public int getMaxLifetime() {
        return this.entityData.get(MAX_LIFETIME);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Layered void atmosphere around the tear: bright motes in the
            // core, a violet mid band, void smoke wisps at the rim, all riding
            // the shared envelope; smoke drizzle while the tear seals.
            LayeredParticles.emitVortex(serverLevel, this.position().add(0, -1.0, 0),
                0.95f * getTearScale(), 2.2f, this.tickCount, getMaxLifetime(),
                0xFF7A4DFF, DbaParticles.VOID_SMOKE);
            float progress = (float) this.tickCount / (float) getMaxLifetime();
            if (progress > 0.65f && this.tickCount % 2 == 0) {
                LayeredParticles.emitRain(serverLevel, this.position(),
                    1.1f * getTearScale(), 1.4f, this.tickCount, getMaxLifetime(),
                    DbaParticles.VOID_SMOKE);
            }
            if (this.tickCount >= getMaxLifetime()) {
                this.discard();
            }
        }
    }
}
