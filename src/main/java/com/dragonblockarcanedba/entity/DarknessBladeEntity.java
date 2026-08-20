package com.dragonblockarcanedba.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Darkness Blade Entity — Giant falling dark energy blade during Abyssal Eclipse execution.
 * Falls rapidly from the sky and detonates upon impacting the ground/enemies.
 */
public class DarknessBladeEntity extends Projectile {
    private float damage = 400.0f;

    public DarknessBladeEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DarknessBladeEntity(Level level, LivingEntity owner, Vec3 spawnPos, float damage) {
        super(DbaEntities.DARKNESS_BLADE, level);
        this.setOwner(owner);
        this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        this.damage = damage;
        this.noPhysics = true;
        this.setDeltaMovement(0, -1.8, 0); // Fast downward plunge
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 movement = this.getDeltaMovement();
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Dark particle trail as the blade falls
            for (int i = 0; i < 4; i++) {
                double py = this.getY() + (i * 1.5);
                serverLevel.sendParticles(
                    new DustParticleOptions(0x1A0033, 2.2f),
                    this.getX(), py, this.getZ(),
                    1, 0.1, 0.1, 0.1, 0.02
                );
            }

            // Impact check: ground or 40 ticks life
            if (this.onGround() || !this.level().getBlockState(this.blockPosition()).isAir() || this.tickCount > 40) {
                // Impact detonation!
                LivingEntity owner = (LivingEntity) this.getOwner();
                DamageSource damageSource = owner instanceof net.minecraft.world.entity.player.Player playerOwner
                    ? serverLevel.damageSources().playerAttack(playerOwner)
                    : serverLevel.damageSources().generic();

                AABB splashBox = this.getBoundingBox().inflate(4.0, 2.0, 4.0);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, splashBox, e -> e.isAlive() && e != owner);

                for (LivingEntity t : targets) {
                    t.hurtServer(serverLevel, damageSource, this.damage);
                }

                // Explosive dark impact particles and sound
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.5, this.getZ(), 2, 0, 0, 0, 0);
                serverLevel.sendParticles(new DustParticleOptions(0x3B0066, 2.5f), this.getX(), this.getY() + 0.5, this.getZ(), 20, 0.8, 0.5, 0.8, 0.1);
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5f, 0.6f);

                this.discard();
            }
        }
    }
}
