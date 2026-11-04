package com.dragonblockarcanedba.entity;

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
 * Ox Fissure Entity: temporary burning ground fissure left by max-charge Groundbreaker (Tweak C).
 * Lasts 5 seconds (100 ticks) and deals its constructor damagePerSec (150 from
 * the Groundbreaker) to enemies standing on the drawn trench.
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

            // Shared slam debris (SlamDebris): one ground burst on the first tick.
            if (this.tickCount == 1) {
                com.dragonblockarcanedba.fx.SlamDebris.burst(serverLevel, pos, 2.5, com.dragonblockarcanedba.fx.SlamDebris.Profile.OX_FISSURE);
            }
            LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;

            // Ambient magma/flame embers, scaled by effect density (redesign T5)
            float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
            if (density > 0f && this.tickCount % 4 == 0) {
                double ox = (serverLevel.getRandom().nextDouble() - 0.5) * 2.5;
                double oz = (serverLevel.getRandom().nextDouble() - 0.5) * 2.5;
                serverLevel.sendParticles(
                    com.dragonblockarcanedba.particle.DbaParticles.EMBER,
                    pos.x + ox, pos.y + 0.1, pos.z + oz,
                    Math.max(1, Math.round(1f * density)), 0, 0.03, 0, 0.01
                );
            }

            // Dissipation (point 5): embers rain back down over the cooling
            // crack during the final 30 ticks instead of the fissure simply
            // stopping at full brightness.
            if (this.tickCount > MAX_LIFETIME - 30 && this.tickCount % 2 == 0) {
                com.dragonblockarcanedba.util.LayeredParticles.emitRain(serverLevel, pos, 1.8f, 1.2f,
                    this.tickCount, MAX_LIFETIME, com.dragonblockarcanedba.particle.DbaParticles.EMBER);
            }

            if (this.tickCount % 10 == 0) {
                // Sizzle / crackle sound
                serverLevel.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LAVA_POP, SoundSource.PLAYERS, 0.6f, 0.8f);

                // Damage enemies standing on the fissure (damagePerSec per
                // second, paid as half a second per 10 tick hit).
                AABB aoe = this.getBoundingBox().inflate(2.5, 1.5, 2.5);
                List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, aoe,
                    e -> e.isAlive() && e != owner
                );

                DamageSource damageSource = owner != null
                    ? serverLevel.damageSources().indirectMagic(this, owner)
                    : serverLevel.damageSources().hotFloor();

                // The damage zone now shares the drawn trench's shape and
                // orientation: the renderer draws a 3.6 block crack along the
                // entity's yaw axis, so the hit test runs in that local frame
                // (about 4.2 long, 2.0 wide) instead of the old unrotated
                // 5 x 3 x 5 square that ignored the yaw entirely.
                double yawRad = Math.toRadians(this.getYRot());
                double axisX = -Math.sin(yawRad);
                double axisZ = Math.cos(yawRad);
                double perpX = Math.cos(yawRad);
                double perpZ = Math.sin(yawRad);

                float hitDamage = this.damagePerSec * 0.5f;
                for (LivingEntity enemy : enemies) {
                    double relX = enemy.getX() - pos.x;
                    double relZ = enemy.getZ() - pos.z;
                    double relY = enemy.getY() - pos.y;
                    double localZ = relX * axisX + relZ * axisZ;
                    double localX = relX * perpX + relZ * perpZ;
                    if (Math.abs(localZ) > 2.1 || Math.abs(localX) > 1.0 || relY < -1.5 || relY > 2.0) {
                        continue;
                    }
                    enemy.hurtServer(serverLevel, damageSource, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (hitDamage)));
                    enemy.igniteForSeconds(2.0f);
                }
            }

            if (this.tickCount >= MAX_LIFETIME) {
                this.discard();
            }
        }
    }
}
