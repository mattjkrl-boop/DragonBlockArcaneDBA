package com.dragonblockarcanedba.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Azure Tornado Entity — Swirling dragon wind vortex that lifts and spins enemies.
 */
public class AzureTornadoEntity extends Projectile {
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(AzureTornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_GIANT = SynchedEntityData.defineId(AzureTornadoEntity.class, EntityDataSerializers.BOOLEAN);

    private int maxLifetime = 60; // 3s default for mini, 200 (10s) for giant

    public AzureTornadoEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public AzureTornadoEntity(Level level, LivingEntity owner, Vec3 pos, float scale, boolean isGiant) {
        super(DbaEntities.AZURE_TORNADO, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setScale(scale);
        this.entityData.set(IS_GIANT, isGiant);
        this.maxLifetime = isGiant ? 200 : 60;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SCALE, 1.0f);
        builder.define(IS_GIANT, false);
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public void setScale(float scale) {
        this.entityData.set(SCALE, scale);
    }

    public boolean isGiant() {
        return this.entityData.get(IS_GIANT);
    }

    @Override
    public void tick() {
        super.tick();

        float scale = getScale();
        boolean giant = isGiant();
        double radius = scale * (giant ? 8.0 : 3.0);
        double height = scale * (giant ? 16.0 : 6.0);
        Vec3 center = this.position();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Pull and swirl entities
            AABB aoe = new AABB(center.x - radius, center.y, center.z - radius,
                                center.x + radius, center.y + height, center.z + radius);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, aoe, e -> e.isAlive() && e != this.getOwner());

            for (LivingEntity target : targets) {
                Vec3 toTarget = target.position().subtract(center);
                double dist = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
                
                // Tangential vortex spin velocity
                double angle = Math.atan2(toTarget.z, toTarget.x) + 0.4;
                double speed = (giant ? 1.2 : 0.8) * Math.max(0.2, 1.0 - (dist / radius));
                double vx = -Math.sin(angle) * speed;
                double vz = Math.cos(angle) * speed;
                double vy = 0.35 + (giant ? 0.3 : 0.1);

                target.setDeltaMovement(target.getDeltaMovement().add(vx * 0.5, vy, vz * 0.5));
                target.hurtMarked = true;

                // Continuous wind damage
                if (this.tickCount % 10 == 0) {
                    target.hurtServer(serverLevel, serverLevel.damageSources().magic(), giant ? 60.0f : 25.0f);
                }
            }

            // Swirling wind particles
            int particleCount = giant ? 16 : 6;
            for (int i = 0; i < particleCount; i++) {
                double progress = serverLevel.getRandom().nextDouble();
                double curHeight = progress * height;
                double curRadius = (0.5 + progress * 0.8) * radius;
                double pAngle = this.tickCount * 0.4 + progress * Math.PI * 4 + i * (Math.PI * 2 / particleCount);

                double px = center.x + Math.cos(pAngle) * curRadius;
                double pz = center.z + Math.sin(pAngle) * curRadius;
                double py = center.y + curHeight;

                serverLevel.sendParticles(
                    new DustParticleOptions(0x00E5FF, 1.5F), // Cyan dragon wind
                    px, py, pz,
                    1, -Math.sin(pAngle) * 0.3, 0.2, Math.cos(pAngle) * 0.3, 0.05
                );
                serverLevel.sendParticles(
                    ParticleTypes.CLOUD,
                    px, py, pz,
                    1, 0.0, 0.1, 0.0, 0.02
                );
            }

            // Giant tornado periodic lightning (Tweak C)
            if (giant && this.tickCount % 20 == 0) {
                double rx = center.x + (serverLevel.getRandom().nextDouble() - 0.5) * radius * 1.5;
                double rz = center.z + (serverLevel.getRandom().nextDouble() - 0.5) * radius * 1.5;
                double ry = serverLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) rx, (int) rz);

                AzureLightningEntity lightning = new AzureLightningEntity(serverLevel, (LivingEntity) this.getOwner(), rx, ry, rz, 350.0f);
                serverLevel.addFreshEntity(lightning);

                serverLevel.playSound(null, rx, ry, rz,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.5f, 1.2f);
            }

            // Howling wind sound
            if (this.tickCount % 20 == 0) {
                serverLevel.playSound(null, center.x, center.y, center.z,
                    SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.5f, 0.8f);
            }

            if (this.tickCount >= this.maxLifetime) {
                this.discard();
            }
        }
    }
}
