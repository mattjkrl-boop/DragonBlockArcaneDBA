package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.dragonblockarcanedba.util.GroundScar;
import com.dragonblockarcanedba.util.LayeredParticles;
import com.dragonblockarcanedba.util.VortexPhysics;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Brave Shockwave Entity - Physical 3D geometric heroic shockwave, raised beveled ring, and crystalline valor needles on Brave Sword dash impact.
 * Life is 44 ticks, long enough for the ring to cross its full radius at a
 * readable speed and dissipate instead of vanishing. The entity owns its
 * damage: each target is hit once, on the tick the expanding ring front
 * (the same sine ease the renderer draws) reaches it, and thrown outward.
 * At ground contact it throws 28 debris pieces sampled from the ground it
 * stands on, which orbit out and rain back down.
 */
public class BraveShockwaveEntity extends Projectile {
    private static final EntityDataAccessor<Float> MAX_RADIUS = SynchedEntityData.defineId(BraveShockwaveEntity.class, EntityDataSerializers.FLOAT);

    private int maxLifetime = 44;
    private float damage = 0.0f;
    private float prevFrontRadius = 0.0f;
    private boolean debrisSpawned = false;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public BraveShockwaveEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BraveShockwaveEntity(Level level, LivingEntity owner, Vec3 pos, float maxRadius) {
        this(level, owner, pos, maxRadius, 0.0f);
    }

    public BraveShockwaveEntity(Level level, LivingEntity owner, Vec3 pos, float maxRadius, float damage) {
        super(DbaEntities.BRAVE_SHOCKWAVE, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.entityData.set(MAX_RADIUS, maxRadius);
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MAX_RADIUS, 7.5f);
    }

    public float getMaxRadius() {
        return this.entityData.get(MAX_RADIUS);
    }

    public int getMaxLifetime() {
        return this.maxLifetime;
    }

    /** Ring front radius at a progress fraction, the same ease the renderer draws. */
    private float frontRadius(float progress) {
        return (float) (Math.sin(progress * (Math.PI / 2.0)) * getMaxRadius());
    }

    /** Find the surface under the impact and scorch a small disk of it. */
    private void scarGround(ServerLevel serverLevel, Vec3 center) {
        BlockPos base = BlockPos.containing(center.x, center.y, center.z);
        for (int dy = 0; dy <= 8; dy++) {
            BlockPos candidate = base.below(dy);
            BlockState state = serverLevel.getBlockState(candidate);
            if (state.isAir() || state.getCollisionShape(serverLevel, candidate).isEmpty()) continue;
            BlockState above = serverLevel.getBlockState(candidate.above());
            if (!above.isAir() && !above.getCollisionShape(serverLevel, candidate.above()).isEmpty()) continue;
            GroundScar.scarDisk(serverLevel, candidate, 2, Blocks.MAGMA_BLOCK.defaultBlockState(),
                300, 0.6f, serverLevel.getRandom());
            return;
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Vec3 center = this.position();
            float maxRadius = getMaxRadius();
            float progress = Math.min(1.0f, this.tickCount / (float) this.maxLifetime);
            float front = frontRadius(progress);

            if (this.tickCount == 1) {
                // Base dust at the impact point (standard point 3).
                LayeredParticles.emitGroundBurst(serverLevel, center, 2.0f, 0xFFD700);
            }

            if (this.tickCount == 2) {
                // The dive strikes earth here: scorch the ground under the
                // impact into magma for a while (standard point 3). GroundScar
                // records the real ground and restores it, crash safe. A
                // shockwave that fires high in the air scars nothing.
                scarGround(serverLevel, center);
            }

            if (this.tickCount == 2 && !debrisSpawned) {
                debrisSpawned = true;
                // Debris at the shockwave's ground contact (standard point 2):
                // the ring tears up the ground it is standing on.
                Entity owner = this.getOwner();
                LivingEntity livingOwner = owner instanceof LivingEntity lo ? lo : null;
                DebrisSpawner.spawnField(serverLevel, livingOwner, center,
                    28, maxRadius * 0.55, 2.5, 26);
            }

            // Damage rides the ring front: a target is hit on the tick the
            // visible front reaches its distance, never before, never after.
            if (this.damage > 0.0f) {
                Entity owner = this.getOwner();
                DamageSource damageSource = owner instanceof net.minecraft.world.entity.player.Player playerOwner
                    ? serverLevel.damageSources().playerAttack(playerOwner)
                    : (owner instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobProjectile(this, livingOwner) : serverLevel.damageSources().generic());

                AABB box = this.getBoundingBox().inflate(maxRadius + 1.0, 2.5, maxRadius + 1.0);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, box,
                    e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
                );
                float bandLow = prevFrontRadius - 0.75f;
                float bandHigh = front + 0.75f;
                for (LivingEntity target : targets) {
                    double dx = target.getX() - center.x;
                    double dz = target.getZ() - center.z;
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist < bandLow || dist > bandHigh) continue;
                    hitEntityIds.add(target.getId());
                    target.hurtServer(serverLevel, damageSource,
                        DbaEffectsConfig.capDamage(this.damage));
                    if (owner instanceof LivingEntity livingOwner) {
                        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 15, 0, false, false, false
                        ), livingOwner);
                    }
                    VortexPhysics.fling(target, center, 1.1, 0.55);
                }
            }
            prevFrontRadius = front;

            // Layered dust riding the ring front (standard point 1): gold mid
            // band on the front itself, cyan wisps just behind it, mote core
            // at the epicenter, all scaled by the shared envelope.
            float env = FxEnvelope.envelope(this.tickCount, this.maxLifetime, 0.15f, 0.35f);
            float density = DbaEffectsConfig.effectDensity();
            if (env > 0f && density > 0f && front > 0.2f) {
                int points = Math.max(2, Math.round(5f * density * env));
                for (int i = 0; i < points; i++) {
                    double a = serverLevel.getRandom().nextDouble() * Math.PI * 2.0;
                    double fx = center.x + Math.cos(a) * front;
                    double fz = center.z + Math.sin(a) * front;
                    serverLevel.sendParticles(LayeredParticles.dust(0xFFD700, 1.3f),
                        fx, center.y + 0.25, fz, 1, 0.1, 0.12, 0.1, 0.03);
                    if (i % 2 == 0) {
                        double bx = center.x + Math.cos(a) * front * 0.88;
                        double bz = center.z + Math.sin(a) * front * 0.88;
                        serverLevel.sendParticles(LayeredParticles.dust(0x00F2FF, 1.5f),
                            bx, center.y + 0.15, bz, 1, 0.08, 0.06, 0.08, 0.02);
                    }
                }
                serverLevel.sendParticles(DbaParticles.KI_MOTE, center.x, center.y + 0.4, center.z,
                    Math.max(1, Math.round(2f * density * env)), 0.5, 0.2, 0.5, 0.01);
            }

            if (this.tickCount >= this.maxLifetime) {
                this.discard();
            }
        }
    }
}
