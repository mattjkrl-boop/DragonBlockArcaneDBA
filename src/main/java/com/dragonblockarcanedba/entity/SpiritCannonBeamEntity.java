package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.item.SpiritSwordItem;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Spirit Cannon Beam Entity: Physical 3D continuous geometric energy beam,
 * rotating helical drill spirals, orbital Ki ring nodes, and muzzle/terminus geometry.
 *
 * Spectacle pass: the beam no longer vanishes in one tick when the channel
 * ends. beginFadeOut runs a FADE_TICKS dissipation: the synced FADE value
 * drives the renderer's alpha and retraction while motes rain off the dying
 * beam. While it channels it sheds layered particles along its whole length
 * (mote core, cyan dust mid, gold wisps near the terminus), not just a
 * trickle at the two ends.
 */
public class SpiritCannonBeamEntity extends Projectile {
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(SpiritCannonBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(SpiritCannonBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BEAM_Y_ROT = SynchedEntityData.defineId(SpiritCannonBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BEAM_X_ROT = SynchedEntityData.defineId(SpiritCannonBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FADE = SynchedEntityData.defineId(SpiritCannonBeamEntity.class, EntityDataSerializers.FLOAT);

    public static final float MAX_BEAM_RANGE = 32.0f;
    public static final int FADE_TICKS = 6;

    private static final int CYAN = 0x00E5FF;
    private static final int GOLD = 0xFFC83C;

    private int fadeTicksLeft = -1;

    public SpiritCannonBeamEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SpiritCannonBeamEntity(Level level, LivingEntity caster) {
        super(DbaEntities.SPIRIT_CANNON_BEAM, level);
        this.setOwner(caster);
        this.noPhysics = true;
        if (caster != null) {
            this.entityData.set(CASTER_ID, caster.getId());
            Vec3 eye = caster.getEyePosition();
            this.setPos(eye.x, eye.y, eye.z);
            this.setYRot(caster.getYRot());
            this.setXRot(caster.getXRot());
            this.entityData.set(BEAM_Y_ROT, caster.getYRot());
            this.entityData.set(BEAM_X_ROT, caster.getXRot());
            this.entityData.set(LENGTH, MAX_BEAM_RANGE);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CASTER_ID, -1);
        builder.define(LENGTH, MAX_BEAM_RANGE);
        builder.define(BEAM_Y_ROT, 0.0f);
        builder.define(BEAM_X_ROT, 0.0f);
        builder.define(FADE, 1.0f);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    public float getBeamLength() {
        return this.entityData.get(LENGTH);
    }

    public void setBeamLength(float length) {
        this.entityData.set(LENGTH, length);
    }

    public float getBeamYRot() {
        return this.entityData.get(BEAM_Y_ROT);
    }

    public float getBeamXRot() {
        return this.entityData.get(BEAM_X_ROT);
    }

    /** 1.0 while channeling, easing to 0 across the fade out. The renderer multiplies alpha and drawn length by it. */
    public float getFade() {
        return this.entityData.get(FADE);
    }

    public boolean isFading() {
        return this.fadeTicksLeft >= 0;
    }

    /** Start the dissipation phase. Idempotent: a fading beam keeps fading. */
    public void beginFadeOut() {
        if (this.fadeTicksLeft < 0) {
            this.fadeTicksLeft = FADE_TICKS;
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            // Mirror the caster locally so the beam origin never lags a tick
            // behind the eye in first person; the server stays authoritative.
            Entity clientCaster = this.getOwner();
            if (clientCaster == null && getCasterId() != -1) {
                clientCaster = this.level().getEntity(getCasterId());
            }
            if (clientCaster instanceof LivingEntity livingCaster && livingCaster.isAlive()) {
                Vec3 eye = livingCaster.getEyePosition();
                this.setPos(eye.x, eye.y, eye.z);
                this.setYRot(livingCaster.getYRot());
                this.setXRot(livingCaster.getXRot());
            }
            return;
        }
        ServerLevel serverLevel = this.level() instanceof ServerLevel sl ? sl : null;

        // Dissipation phase: hold the last transform, shed mote rain along
        // the beam line, count the synced FADE down, then discard at zero.
        if (this.fadeTicksLeft >= 0) {
            this.fadeTicksLeft--;
            float fade = Math.max(0f, this.fadeTicksLeft / (float) FADE_TICKS);
            this.entityData.set(FADE, fade);
            if (serverLevel != null) {
                Vec3 dir = Vec3.directionFromRotation(this.getBeamXRot(), this.getBeamYRot());
                Vec3 mid = this.position().add(dir.scale(this.getBeamLength() * 0.5f));
                LayeredParticles.emitRain(serverLevel, mid, this.getBeamLength() * 0.4f, 1.2f,
                    FADE_TICKS - this.fadeTicksLeft, FADE_TICKS,
                    com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE);
            }
            if (this.fadeTicksLeft <= 0) {
                this.discard();
            }
            return;
        }

        Entity caster = this.getOwner();
        if (caster == null && getCasterId() != -1) {
            caster = this.level().getEntity(getCasterId());
        }

        if (caster instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
            Vec3 eye = living.getEyePosition();
            this.setPos(eye.x, eye.y, eye.z);
            this.setYRot(living.getYRot());
            this.setXRot(living.getXRot());

            this.entityData.set(BEAM_Y_ROT, living.getYRot());
            this.entityData.set(BEAM_X_ROT, living.getXRot());

            // Raycast against solid blocks to find physical beam contact point
            Vec3 look = living.getViewVector(1.0f);
            Vec3 end = eye.add(look.scale(MAX_BEAM_RANGE));
            BlockHitResult blockHit = this.level().clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, living
            ));

            float currentLength = MAX_BEAM_RANGE;
            if (blockHit.getType() != HitResult.Type.MISS) {
                currentLength = (float) eye.distanceTo(blockHit.getLocation());
            }
            this.setBeamLength(currentLength);

            if (serverLevel != null) {
                emitBeamParticles(serverLevel, eye, look, currentLength,
                    blockHit.getType() != HitResult.Type.MISS);
            }

            if (living instanceof Player player) {
                boolean isHolding = player.getMainHandItem().getItem() instanceof SpiritSwordItem ||
                                    player.getOffhandItem().getItem() instanceof SpiritSwordItem;
                if (!isHolding || (this.tickCount > 3 && !player.isUsingItem())) {
                    this.beginFadeOut();
                }
            }
        } else {
            this.beginFadeOut();
        }
    }

    /**
     * Layered particles along the beam (standard point 1): bright motes on
     * the axis, a cyan dust sheath at mid radius, gold wisps drifting off
     * the terminus third, plus the terminus spark shed and muzzle motes.
     * Density rides the effects config and the 5 tick fade in ramp, so the
     * particle life agrees with the renderer's fade in.
     */
    private void emitBeamParticles(ServerLevel serverLevel, Vec3 eye, Vec3 look, float length, boolean hitBlock) {
        float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
        if (density <= 0f) return;
        float env = Math.min(1f, this.tickCount / 5f);
        var rand = serverLevel.getRandom();

        // Perpendicular basis for the sheath offsets.
        Vec3 perpA = look.cross(new Vec3(0, 1, 0));
        if (perpA.lengthSqr() < 0.001) perpA = new Vec3(1, 0, 0);
        perpA = perpA.normalize();
        Vec3 perpB = look.cross(perpA).normalize();

        // Core: motes riding the axis.
        int core = Math.max(1, Math.round(2f * density * env));
        for (int i = 0; i < core; i++) {
            Vec3 p = eye.add(look.scale(length * rand.nextDouble()));
            serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
                p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.01);
        }

        // Mid: cyan dust sheath at beam radius.
        int mid = Math.max(1, Math.round(2f * density * env));
        for (int i = 0; i < mid; i++) {
            double ang = rand.nextDouble() * Math.PI * 2.0;
            Vec3 off = perpA.scale(Math.cos(ang) * 0.30).add(perpB.scale(Math.sin(ang) * 0.30));
            Vec3 p = eye.add(look.scale(length * rand.nextDouble())).add(off);
            serverLevel.sendParticles(LayeredParticles.dust(CYAN, 1.2f),
                p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.01);
        }

        // Outer: gold wisps off the terminus third, every second tick.
        if (this.tickCount % 2 == 0) {
            double ang = rand.nextDouble() * Math.PI * 2.0;
            Vec3 off = perpA.scale(Math.cos(ang) * 0.52).add(perpB.scale(Math.sin(ang) * 0.52));
            Vec3 p = eye.add(look.scale(length * (0.6 + 0.4 * rand.nextDouble()))).add(off);
            serverLevel.sendParticles(LayeredParticles.dust(GOLD, 1.5f),
                p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.01);

            // Terminus spark shed at the contact point.
            Vec3 hitPoint = eye.add(look.scale(length));
            serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                hitPoint.x, hitPoint.y, hitPoint.z,
                Math.max(1, Math.round(3f * density * env)),
                0.25, 0.25, 0.25, 0.06);

            // Dust where the beam meets the ground (point 3), on a slow beat.
            if (hitBlock && this.tickCount % 10 == 0) {
                LayeredParticles.emitGroundBurst(serverLevel, hitPoint, 0.9f, CYAN);
            }
        }

        // Muzzle motes, tightened from every 6 ticks to every 3.
        if (this.tickCount % 3 == 0) {
            serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
                eye.x, eye.y, eye.z,
                Math.max(1, Math.round(2f * density * env)),
                0.3, 0.3, 0.3, 0.02);
        }
    }
}
