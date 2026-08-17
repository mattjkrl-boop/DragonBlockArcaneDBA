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
 * Ox Fissure Entity — Temporary burning ground fissure left by max-charge Groundbreaker (Tweak C).
 * Lasts 5 seconds (100 ticks) and deals 50 damage/sec to enemies on the fissure.
 */
public class OxFissureEntity extends Projectile {
    private float damagePerSec = 50.0f;
    public static final int MAX_LIFETIME = 100; // 5 seconds

    public OxFissureEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public OxFissureEntity(Level level, LivingEntity owner, Vec3 pos, float damagePerSec) {
        super(DbaEntities.OX_FISSURE, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.damagePerSec = damagePerSec;
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = this.position();
            LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;

            // Particles: black dust ground crack & magma/flame embers
            if (this.tickCount % 2 == 0) {
                for (int i = 0; i < 4; i++) {
                    double ox = (serverLevel.getRandom().nextDouble() - 0.5) * 3.0;
                    double oz = (serverLevel.getRandom().nextDouble() - 0.5) * 3.0;

                    serverLevel.sendParticles(
                        new DustParticleOptions(0x111111, 2.2f),
                        pos.x + ox, pos.y + 0.1, pos.z + oz,
                        1, 0, 0.05, 0, 0.01
                    );
                    serverLevel.sendParticles(
                        ParticleTypes.FLAME,
                        pos.x + ox, pos.y + 0.1, pos.z + oz,
                        1, 0, 0.03, 0, 0.01
                    );
                }
            }

            if (this.tickCount % 10 == 0) {
                // Sizzle / crackle sound
                serverLevel.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LAVA_POP, SoundSource.PLAYERS, 0.6f, 0.8f);

                // Damage enemies standing on the fissure (50 dmg/sec = 25 per 10 ticks)
                AABB aoe = this.getBoundingBox().inflate(2.5, 1.5, 2.5);
                List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, aoe,
                    e -> e.isAlive() && e != owner
                );

                DamageSource damageSource = owner != null
                    ? serverLevel.damageSources().indirectMagic(this, owner)
                    : serverLevel.damageSources().hotFloor();

                float hitDamage = this.damagePerSec * 0.5f;
                for (LivingEntity enemy : enemies) {
                    enemy.hurtServer(serverLevel, damageSource, hitDamage);
                    enemy.igniteForSeconds(2.0f);
                }
            }

            if (this.tickCount >= MAX_LIFETIME) {
                this.discard();
            }
        }
    }
}
