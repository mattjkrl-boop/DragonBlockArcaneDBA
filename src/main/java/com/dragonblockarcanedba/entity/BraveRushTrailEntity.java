package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.dragonblockarcanedba.util.LayeredParticles;
import com.dragonblockarcanedba.util.VortexPhysics;
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
 * Brave Rush Trail Entity - Physical 3D heroic supersonic Mach cone, twin streamlines, and concentrated kinetic thrust beam.
 * Life is 30 ticks, long enough for a 24 block dash to read, peak, and settle.
 * The entity also resolves the dash: at tick 6, when the cone has opened to
 * its peak, every living target in the path box (measured fresh, at damage
 * time) takes the dash damage, is launched off the path line, and if any
 * target dies, or the dash was charged to 0.9 or more, the radial heroic
 * shockwave fires from the dash destination. Before this pass the item dealt
 * all of that instantly in the release tick, under a comment that called it
 * a delayed finishing slash; now the delay is real.
 */
public class BraveRushTrailEntity extends Projectile {
    private static final EntityDataAccessor<Float> TRAIL_SCALE = SynchedEntityData.defineId(BraveRushTrailEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TRAIL_LENGTH = SynchedEntityData.defineId(BraveRushTrailEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(BraveRushTrailEntity.class, EntityDataSerializers.INT);

    public static final int DAMAGE_TICK = 6;

    private int maxLifetime = 30;
    private float damage = 0.0f;
    private float chargeRatio = 0.0f;
    private Vec3 direction = null;
    private boolean resolved = false;

    public BraveRushTrailEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BraveRushTrailEntity(Level level, LivingEntity owner, Vec3 pos, float yRot, float xRot, float length, float scale) {
        super(DbaEntities.BRAVE_RUSH_TRAIL, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.noPhysics = true;
        this.entityData.set(TRAIL_SCALE, scale);
        this.entityData.set(TRAIL_LENGTH, length);
        if (owner != null) {
            this.entityData.set(CASTER_ID, owner.getId());
        }
    }

    /** Dash payload set by the item before spawn: the damage to resolve at the peak tick. */
    public void setDashPayload(float damage, float chargeRatio, Vec3 direction) {
        this.damage = damage;
        this.chargeRatio = chargeRatio;
        this.direction = direction;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TRAIL_SCALE, 1.0f);
        builder.define(TRAIL_LENGTH, 10.0f);
        builder.define(CASTER_ID, -1);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    public float getTrailScale() {
        return this.entityData.get(TRAIL_SCALE);
    }

    public float getTrailLength() {
        return this.entityData.get(TRAIL_LENGTH);
    }

    public int getMaxLifetime() {
        return this.maxLifetime;
    }

    private Vec3 dashDirection() {
        if (this.direction != null && this.direction.lengthSqr() > 0.0001) {
            return this.direction.normalize();
        }
        return Vec3.directionFromRotation(this.getXRot(), this.getYRot());
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Vec3 start = this.position();
            Vec3 dir = dashDirection();
            float length = getTrailLength();
            Vec3 end = start.add(dir.scale(length));

            if (this.tickCount == 1) {
                // Launch dust where the dash leaves the ground (standard point 3).
                LayeredParticles.emitGroundBurst(serverLevel, start.add(0, -0.75, 0), 1.5f, 0xFFD700);
            }

            // Layered particles along the flight path for the whole life
            // (standard point 1): mote core on the line, gold mid dust around
            // it, cyan wisps at the rim, envelope driven.
            float env = FxEnvelope.envelope(this.tickCount, this.maxLifetime, 0.25f, 0.4f);
            float density = DbaEffectsConfig.effectDensity();
            if (env > 0f && density > 0f) {
                int samples = Math.max(1, Math.round(3f * density * env));
                for (int i = 0; i < samples; i++) {
                    double t = serverLevel.getRandom().nextDouble();
                    Vec3 p = start.add(dir.scale(length * t));
                    serverLevel.sendParticles(DbaParticles.KI_MOTE, p.x, p.y, p.z,
                        1, 0.06, 0.06, 0.06, 0.01);
                    serverLevel.sendParticles(LayeredParticles.dust(0xFFD700, 1.2f), p.x, p.y, p.z,
                        1, 0.3, 0.3, 0.3, 0.02);
                    if (i % 2 == 0) {
                        serverLevel.sendParticles(LayeredParticles.dust(0x00F2FF, 1.5f), p.x, p.y, p.z,
                            1, 0.55, 0.55, 0.55, 0.02);
                    }
                }
            }

            if (!resolved && this.damage > 0.0f && this.tickCount >= DAMAGE_TICK) {
                resolved = true;
                resolveDash(serverLevel, start, end, dir);
            }

            if (this.tickCount >= this.maxLifetime) {
                this.discard();
            }
        }
    }

    private void resolveDash(ServerLevel serverLevel, Vec3 start, Vec3 end, Vec3 dir) {
        Entity owner = this.getOwner();
        AABB pathBox = new AABB(start, end).inflate(2.0 + chargeRatio * 2.5);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
            LivingEntity.class, pathBox,
            e -> e.isAlive() && e != owner
        );

        boolean anyKilled = false;
        for (LivingEntity t : targets) {
            if (owner instanceof LivingEntity livingOwner) {
                t.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 20, 0, false, false, false
                ), livingOwner);
            }
            if (owner instanceof net.minecraft.world.entity.player.Player playerOwner) {
                t.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(playerOwner),
                    DbaEffectsConfig.capDamage(this.damage));
            } else if (owner instanceof LivingEntity livingOwner) {
                t.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(livingOwner),
                    DbaEffectsConfig.capDamage(this.damage));
            }

            // MC 26.2 Physics: Piercing dash launch and bounce
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                t,
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                com.dragonblockarcanedba.DragonBlockArcaneDBA.id("brave_dash_bounce"),
                0.80,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
            );
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                t,
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                com.dragonblockarcanedba.DragonBlockArcaneDBA.id("brave_dash_drag"),
                -0.40,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );

            // The dash throws what it pierces off the path line (standard point 3).
            Vec3 closest = closestPointOnSegment(start, end, t.position());
            VortexPhysics.fling(t, closest, 0.85, 0.5);

            if (!t.isAlive()) {
                anyKilled = true;
            }
        }

        // Landing dust at the destination.
        LayeredParticles.emitGroundBurst(serverLevel, end.add(0, -0.75, 0), 1.8f, 0xFFD700);

        // Secondary radial heroic shockwave: on a kill, or at near full charge.
        // The shockwave entity owns its own damage and debris from here.
        if ((anyKilled || chargeRatio >= 0.9f) && owner instanceof LivingEntity livingOwner) {
            float shockRadius = (float) (5.0 + chargeRatio * 3.5);
            Vec3 shockPos = new Vec3(end.x, end.y - 0.75 + 0.05, end.z);
            BraveShockwaveEntity shockwave = new BraveShockwaveEntity(
                serverLevel, livingOwner, shockPos, shockRadius, this.damage * 0.4f);
            serverLevel.addFreshEntity(shockwave);

            serverLevel.playSound(null, end.x, end.y, end.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.9f);
            com.dragonblockarcanedba.fx.DbaImpactFx.heavy(serverLevel, end.x, end.y, end.z);
        }
    }

    private static Vec3 closestPointOnSegment(Vec3 a, Vec3 b, Vec3 p) {
        Vec3 ab = b.subtract(a);
        double lenSq = ab.lengthSqr();
        if (lenSq < 0.0001) return a;
        double t = Math.max(0.0, Math.min(1.0, p.subtract(a).dot(ab) / lenSq));
        return a.add(ab.scale(t));
    }
}
