package com.dragonblockarcanedba.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Azure Storm Entity — Tempest Weather Domain from Azure Dragon Sword.
 */
public class AzureStormEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(AzureStormEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> FOLLOWS_PLAYER = SynchedEntityData.defineId(AzureStormEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(AzureStormEntity.class, EntityDataSerializers.INT);

    private int maxLifetime = 220; // 11 seconds (Fast, intense, punchy combat duration)
    private boolean isMaxCharged = false;
    private boolean hasSpawnedGiantTornado = false;

    public AzureStormEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public AzureStormEntity(Level level, LivingEntity owner, Vec3 pos, float radius, boolean followsPlayer, LivingEntity target, boolean isMaxCharged) {
        super(DbaEntities.AZURE_STORM, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setRadius(radius);
        this.entityData.set(FOLLOWS_PLAYER, followsPlayer);
        this.entityData.set(TARGET_ID, target != null ? target.getId() : -1);
        this.isMaxCharged = isMaxCharged;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 12.0f);
        builder.define(FOLLOWS_PLAYER, true);
        builder.define(TARGET_ID, -1);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(RADIUS, radius);
    }

    public boolean followsPlayer() {
        return this.entityData.get(FOLLOWS_PLAYER);
    }

    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    @Override
    public void tick() {
        super.tick();

        float radius = getRadius();
        Entity owner = this.getOwner();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Update position to follow player (Tweak A) or follow target (Tweak B)
            if (followsPlayer() && owner != null && owner.isAlive()) {
                this.setPos(owner.getX(), owner.getY(), owner.getZ());
            } else if (getTargetId() != -1) {
                Entity target = serverLevel.getEntity(getTargetId());
                if (target != null && target.isAlive()) {
                    this.setPos(target.getX(), target.getY(), target.getZ());
                }
            }

            Vec3 center = this.position();

            // Tweak C: Spawn giant tornado at storm center at max charge
            if (this.isMaxCharged && !this.hasSpawnedGiantTornado) {
                this.hasSpawnedGiantTornado = true;
                AzureTornadoEntity giantTornado = new AzureTornadoEntity(serverLevel, owner instanceof LivingEntity ? (LivingEntity) owner : null, center, 1.8f, true);
                serverLevel.addFreshEntity(giantTornado);
            }

            // Wind turbulence on all enemies in radius
            AABB stormBox = this.getBoundingBox().inflate(radius, 12.0, radius);
            List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(LivingEntity.class, stormBox, e -> e.isAlive() && e != owner && center.distanceTo(e.position()) <= radius);

            for (LivingEntity enemy : enemies) {
                // Rapid back-and-forth wind push
                if (this.tickCount % 4 == 0) {
                    double wx = (serverLevel.getRandom().nextDouble() - 0.5) * 1.6;
                    double wz = (serverLevel.getRandom().nextDouble() - 0.5) * 1.6;
                    enemy.setDeltaMovement(enemy.getDeltaMovement().add(wx, 0.25, wz));
                    enemy.hurtMarked = true;
                }
            }

            // Periodic Azure Lightning strikes on enemies inside storm
            if (!enemies.isEmpty() && this.tickCount % 25 == 0) {
                LivingEntity lightningTarget = enemies.get(serverLevel.getRandom().nextInt(enemies.size()));
                double lx = lightningTarget.getX();
                double lz = lightningTarget.getZ();
                double ly = serverLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) lx, (int) lz);

                AzureLightningEntity lightning = new AzureLightningEntity(serverLevel, owner instanceof LivingEntity ? (LivingEntity) owner : null, lx, ly, lz, 450.0f);
                if (serverLevel.getRandom().nextFloat() < 0.2f) {
                    lightning.setRare(true);
                }
                serverLevel.addFreshEntity(lightning);

                serverLevel.playSound(null, lx, ly, lz,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.5f, 1.0f);
            }

            // Swirling storm cloud particles
            for (int i = 0; i < (int) (15 + radius * 1.5); i++) {
                double angle = serverLevel.getRandom().nextDouble() * Math.PI * 2;
                double r = serverLevel.getRandom().nextDouble() * radius;
                double px = center.x + Math.cos(angle) * r;
                double pz = center.z + Math.sin(angle) * r;
                double py = center.y + 2.0 + serverLevel.getRandom().nextDouble() * 10.0;

                // Rain
                serverLevel.sendParticles(
                    ParticleTypes.RAIN,
                    px, py, pz,
                    3, 0.0, -0.5, 0.0, 0.1
                );
                
                // Deep sky blue wind
                serverLevel.sendParticles(
                    new DustParticleOptions(0x00BFFF, 1.8F), 
                    px, py, pz,
                    2, -Math.sin(angle) * 0.4, -0.1, Math.cos(angle) * 0.4, 0.05
                );

                // Dark storm clouds
                if (serverLevel.getRandom().nextFloat() < 0.2f) {
                    serverLevel.sendParticles(
                        ParticleTypes.LARGE_SMOKE,
                        px, py, pz,
                        1, 0.1, 0.0, 0.1, 0.02
                    );
                }
            }

            // Ambient storm sound
            if (this.tickCount % 40 == 0) {
                serverLevel.playSound(null, center.x, center.y, center.z,
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 1.0f, 0.9f);
            }

            if (this.tickCount >= this.maxLifetime) {
                this.discard();
            }
        }
    }
}
