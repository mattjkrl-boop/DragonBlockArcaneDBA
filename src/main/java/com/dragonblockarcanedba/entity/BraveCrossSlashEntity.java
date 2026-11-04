package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.dragonblockarcanedba.util.LayeredParticles;
import com.dragonblockarcanedba.util.VortexPhysics;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Brave Cross Slash Entity - Physical 3D cruciform golden-cyan cross slash unleashed on Brave Finisher (10-hit combo).
 * Slices through the target and epicenter with intersecting volumetric curved crescent blades.
 * Life is 40 ticks. The splash damage is dealt in four pulses of a quarter of
 * the total at ticks 8, 14, 20, and 26, inside the visual's active window
 * (build ends at tick 8, dissipation starts at tick 28), so the damage lives
 * as long as the cross does instead of resolving on tick 2 of 18. A target
 * standing in the cross for the whole window takes the full total; a target
 * that leaves early takes only the pulses it stood in.
 */
public class BraveCrossSlashEntity extends Projectile {
    private static final EntityDataAccessor<Float> SLASH_SCALE = SynchedEntityData.defineId(BraveCrossSlashEntity.class, EntityDataSerializers.FLOAT);

    private static final int[] PULSE_TICKS = {8, 14, 20, 26};
    private static final float PULSE_FRACTION = 0.25f;

    private float damage = 900.0f;
    private int maxLifetime = 40;
    private final Set<Integer> hitEntityIds = new HashSet<>();
    private final Set<Integer> pulseHitEntityIds = new HashSet<>();
    private final Set<Integer> flungEntityIds = new HashSet<>();
    private boolean debrisSpawned = false;

    public BraveCrossSlashEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BraveCrossSlashEntity(Level level, LivingEntity owner, Vec3 pos, float yRot, float xRot, float damage, float scale) {
        super(DbaEntities.BRAVE_CROSS_SLASH, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.damage = damage;
        this.noPhysics = true;
        this.entityData.set(SLASH_SCALE, scale);
    }

    /** Seeds the hit set so this entity never re-hits the given entity (the finisher's primary target). */
    public void excludeEntity(int entityId) {
        hitEntityIds.add(entityId);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SLASH_SCALE, 1.0f);
    }

    public float getSlashScale() {
        return this.entityData.get(SLASH_SCALE);
    }

    public int getMaxLifetime() {
        return this.maxLifetime;
    }

    private boolean isPulseTick(int tick) {
        for (int pulseTick : PULSE_TICKS) {
            if (tick == pulseTick) return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Entity owner = this.getOwner();
            float scale = getSlashScale();
            double radius = 3.5 * scale;
            Vec3 center = this.position();

            if (this.tickCount == 1) {
                // Base dust where the finisher lands (standard point 3).
                LayeredParticles.emitGroundBurst(serverLevel, center.add(0, -0.6, 0), 1.6f, 0xFFD700);
            }

            // Layered particle depth for the whole life (standard point 1):
            // mote core, gold mid band, wind wisps, envelope driven.
            LayeredParticles.emitVortex(serverLevel, center.add(0, -1.0, 0),
                (float) (radius * 0.62), 2.6f, this.tickCount, this.maxLifetime,
                0xFFD700, DbaParticles.WIND);

            if (this.tickCount == PULSE_TICKS[0] && !debrisSpawned) {
                debrisSpawned = true;
                // Finisher debris (standard point 2): 24 ground sampled pieces
                // thrown up at the first damage pulse, the visual's peak beat.
                LivingEntity livingOwner = owner instanceof LivingEntity lo ? lo : null;
                DebrisSpawner.spawnField(serverLevel, livingOwner, center,
                    24, radius * 0.85, 3.0, 30);
            }

            if (isPulseTick(this.tickCount)
                && FxEnvelope.inActiveWindow(this.tickCount, this.maxLifetime, 0.2f, 0.3f)) {
                DamageSource damageSource = owner instanceof net.minecraft.world.entity.player.Player playerOwner
                    ? serverLevel.damageSources().playerAttack(playerOwner)
                    : (owner instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobProjectile(this, livingOwner) : serverLevel.damageSources().generic());

                pulseHitEntityIds.clear();
                AABB hitbox = this.getBoundingBox().inflate(radius, 2.0, radius);

                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, hitbox,
                    e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
                );

                for (LivingEntity target : targets) {
                    if (!pulseHitEntityIds.add(target.getId())) continue;
                    target.hurtServer(serverLevel, damageSource,
                        com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(this.damage * PULSE_FRACTION));

                    if (owner instanceof LivingEntity livingOwner) {
                        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 25, 0, false, false, false
                        ), livingOwner);
                    }

                    // The cross throws what it catches, once per target, on the
                    // first pulse that reaches it (standard point 3).
                    if (flungEntityIds.add(target.getId())) {
                        VortexPhysics.fling(target, center, 0.9, 0.45);
                    }
                }
            }

            if (this.tickCount >= this.maxLifetime) {
                this.discard();
            }
        }
    }
}
