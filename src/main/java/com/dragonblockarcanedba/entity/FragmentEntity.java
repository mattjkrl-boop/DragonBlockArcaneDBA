package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.particle.DbaParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
import java.util.UUID;

/**
 * One physical fragment: a single irregular shard that exists in the
 * world as a real entity instead of renderer decoration (ENTITY-STANDARD,
 * audits/entity-audit.md). Spawned server side by the seven converted
 * hosts through the static helpers below; the renderer draws one
 * ComplexGeometry shard from the synced seed, colors, and size, so the
 * piece looks identical to the pieces layer it replaced while its
 * position, fall, landing, and rest are world truth for every client.
 *
 * Modes: BALLISTIC and BURST share one physics path with different
 * tuning; FROZEN and FIELD_HELD share one held path (fixed position, or
 * an anchor offset followed from a holder entity) that releases into
 * the same physics. Fragments deal no damage, cannot be picked or hurt,
 * and are never saved: shouldBeSaved is false, so a chunk unload or a
 * restart simply ends them, like the mod's other effect entities.
 */
public class FragmentEntity extends Projectile {
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(FragmentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ROOT_ARGB = SynchedEntityData.defineId(FragmentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TIP_ARGB = SynchedEntityData.defineId(FragmentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(FragmentEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GIRTH = SynchedEntityData.defineId(FragmentEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(FragmentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FADE_START = SynchedEntityData.defineId(FragmentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RELEASE_AT = SynchedEntityData.defineId(FragmentEntity.class, EntityDataSerializers.INT);

    public static final int MODE_BALLISTIC = 0;
    public static final int MODE_BURST = 1;
    public static final int MODE_FROZEN = 2;
    public static final int MODE_FIELD_HELD = 3;

    /** Fade length for every fragment, the DebrisEntity FADE_TICKS precedent. */
    public static final int FADE_TICKS = 12;
    /** Absolute backstop: no fragment outlives this, whatever state it is in. */
    public static final int HARD_LIFE_TICKS = 2400;
    /** Caps enforced inside the spawn helpers (ENTITY-STANDARD). */
    public static final int MAX_PER_CALL = 64;
    public static final int MAX_IN_AREA = 256;
    private static final double AREA_BOX = 96.0;
    private static final double GRAVITY = 0.045;
    private static final double TERMINAL = -1.8;

    // Server-only tuning, set by the spawn helpers. The client never
    // simulates fragment motion; position and rotation are server truth.
    private float gravityScale = 1.0f;
    private double drag = 0.99;
    private int restTicks = 12;
    private int lifeTicks = 300;
    private float spinDeg = 0f;
    private UUID holderUuid = null;
    private Vec3 anchorOffset = Vec3.ZERO;
    private boolean landed = false;
    private int restEndAge = -1;

    public FragmentEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SEED, 0);
        builder.define(ROOT_ARGB, 0xFFFFFF);
        builder.define(TIP_ARGB, 0xFFFFFF);
        builder.define(LENGTH, 0.8f);
        builder.define(GIRTH, 0.3f);
        builder.define(MODE, MODE_BURST);
        builder.define(FADE_START, -1);
        builder.define(RELEASE_AT, -1);
    }

    public int getSeed() { return this.entityData.get(SEED); }
    public int getRootArgb() { return this.entityData.get(ROOT_ARGB); }
    public int getTipArgb() { return this.entityData.get(TIP_ARGB); }
    public float getLength() { return this.entityData.get(LENGTH); }
    public float getGirth() { return this.entityData.get(GIRTH); }
    public int getMode() { return this.entityData.get(MODE); }
    public int getFadeStart() { return this.entityData.get(FADE_START); }
    public int getReleaseAt() { return this.entityData.get(RELEASE_AT); }

    /** Non-persistent by design: never written to chunk data, gone on unload. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    /** True once a held fragment's release age has been reached. */
    public boolean isReleased() {
        int rel = getReleaseAt();
        return rel >= 0 && this.tickCount >= rel;
    }

    /**
     * End the hold now: stamps RELEASE_AT with the current age, so the
     * fragment starts falling this tick. Idempotent, and the authored
     * release path for hosts (field fade, rift implosion); the orphan
     * rule in tick is only the safety net.
     */
    public void release() {
        if (!isReleased()) {
            this.entityData.set(RELEASE_AT, this.tickCount);
        }
    }

    private void stampFade(int age) {
        this.entityData.set(FADE_START, age);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        if (!(this.level() instanceof ServerLevel server)) return;
        int age = this.tickCount;

        if (getFadeStart() >= 0 && age >= getFadeStart() + FADE_TICKS) {
            this.discard();
            return;
        }

        int mode = getMode();
        boolean held = (mode == MODE_FROZEN || mode == MODE_FIELD_HELD) && !isReleased();
        if (held) {
            if (mode == MODE_FIELD_HELD) {
                Entity holder = this.holderUuid == null ? null : server.getEntity(this.holderUuid);
                if (holder == null || holder.isRemoved()) {
                    // Orphan rule: the holder is gone, so the piece drops
                    // from where it hangs, this same tick.
                    release();
                } else {
                    this.setPos(holder.position().add(this.anchorOffset));
                    if (this.spinDeg != 0f) this.setYRot(this.getYRot() + this.spinDeg);
                    if (age >= HARD_LIFE_TICKS - FADE_TICKS && getFadeStart() < 0) stampFade(age);
                    return;
                }
            } else {
                if (this.spinDeg != 0f) this.setYRot(this.getYRot() + this.spinDeg);
                if (age >= HARD_LIFE_TICKS - FADE_TICKS && getFadeStart() < 0) stampFade(age);
                return;
            }
        }

        // Ballistic path: BALLISTIC and BURST from spawn, FROZEN and
        // FIELD_HELD after release, all with the DebrisEntity physics.
        if (this.landed) {
            if (getFadeStart() < 0 && age >= this.restEndAge) stampFade(age);
        } else {
            Vec3 vel = this.getDeltaMovement();
            vel = new Vec3(vel.x * this.drag,
                Math.max(vel.y - GRAVITY * this.gravityScale, TERMINAL),
                vel.z * this.drag);
            this.setDeltaMovement(vel);
            updateHeading(vel);
            Vec3 next = this.position().add(vel);
            BlockPos below = BlockPos.containing(next.x, next.y - 0.06, next.z);
            if (!this.level().getBlockState(below).getCollisionShape(this.level(), below).isEmpty() && vel.y <= 0) {
                this.setPos(next.x, next.y, next.z);
                this.setDeltaMovement(Vec3.ZERO);
                this.landed = true;
                this.restEndAge = age + this.restTicks;
                server.sendParticles(DbaParticles.SHARD_CHIP,
                    next.x, next.y + 0.1, next.z, 2, 0.12, 0.05, 0.12, 0.02);
            } else {
                this.setPos(next.x, next.y, next.z);
            }
        }

        if (getFadeStart() < 0 && age >= this.lifeTicks - FADE_TICKS) stampFade(age);
        if (age >= HARD_LIFE_TICKS) this.discard();
    }

    /** Aim the shard's rotation along a direction, the TridentShardEntity math. */
    private void updateHeading(Vec3 motion) {
        if (motion.lengthSqr() > 0.0001) {
            double horiz = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            float yRot = (float) (Math.atan2(motion.z, motion.x) * (180.0 / Math.PI)) - 90.0f;
            float xRot = (float) -(Math.atan2(motion.y, horiz) * (180.0 / Math.PI));
            this.setYRot(yRot);
            this.setXRot(xRot);
        }
    }

    // ------------------------------------------------------------------
    // Spawn helpers. All server-guarded, all cap-enforced, all silent
    // past the caps. Per-fragment mesh seeds use the shardBurst salt,
    // baseSeed + index * 0x9E37L, so a fragment draws the identical
    // silhouette the renderer burst drew for that index.
    // ------------------------------------------------------------------

    private static FragmentEntity spawn(ServerLevel level, Entity owner, Vec3 pos, Vec3 vel, Vec3 facing,
                                        int mode, int seed, int rootArgb, int tipArgb, float length, float girth,
                                        float gravityScale, double drag, int restTicks, int lifeTicks,
                                        int releaseAt, UUID holderUuid, Vec3 anchorOffset, float spinDeg) {
        FragmentEntity frag = new FragmentEntity(DbaEntities.FRAGMENT, level);
        frag.setOwner(owner);
        frag.setPos(pos.x, pos.y, pos.z);
        frag.setDeltaMovement(vel);
        frag.updateHeading(facing);
        frag.entityData.set(SEED, seed);
        frag.entityData.set(ROOT_ARGB, rootArgb);
        frag.entityData.set(TIP_ARGB, tipArgb);
        frag.entityData.set(LENGTH, length);
        frag.entityData.set(GIRTH, girth);
        frag.entityData.set(MODE, mode);
        if (releaseAt >= 0) frag.entityData.set(RELEASE_AT, releaseAt);
        frag.gravityScale = gravityScale;
        frag.drag = drag;
        frag.restTicks = restTicks;
        frag.lifeTicks = lifeTicks;
        frag.holderUuid = holderUuid;
        frag.anchorOffset = anchorOffset;
        frag.spinDeg = spinDeg;
        level.addFreshEntity(frag);
        return frag;
    }

    /** How many of the wanted count fit under both caps right now. */
    private static int areaRoom(ServerLevel level, Vec3 center, int want) {
        if (want <= 0) return 0;
        int capped = Math.min(want, MAX_PER_CALL);
        int alive = level.getEntitiesOfClass(FragmentEntity.class,
            new AABB(center.x - AREA_BOX, center.y - AREA_BOX, center.z - AREA_BOX,
                     center.x + AREA_BOX, center.y + AREA_BOX, center.z + AREA_BOX),
            e -> e.isAlive()).size();
        return Math.max(0, Math.min(capped, MAX_IN_AREA - alive));
    }

    /** Shard length over the shardBurst size classes for the given unit. */
    private static float lengthFor(Random rand, float unit) {
        float roll = rand.nextFloat();
        if (roll < 0.55f) return unit * (0.30f + 0.25f * rand.nextFloat());
        if (roll < 0.85f) return unit * (0.55f + 0.50f * rand.nextFloat());
        return unit * (1.05f + 0.55f * rand.nextFloat());
    }

    private static float girthFor(Random rand, float length) {
        return length * (0.30f + 0.14f * rand.nextFloat());
    }

    private static Vec3 perpOf(Vec3 dir) {
        Vec3 ref = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 p = dir.cross(ref);
        return p.lengthSqr() < 1e-8 ? new Vec3(1, 0, 0) : p.normalize();
    }

    /**
     * Cone scatter matching shardBurst's distribution: cone angle by
     * sqrt roll, origins jittered within unit * 0.12 perpendicular, each
     * fragment with its own direction, size class, and mesh seed. Mode
     * BURST. Returns the number spawned.
     */
    public static int burst(ServerLevel level, Entity owner, Vec3 center, Vec3 axis, float spreadDeg,
                            float unit, int count, long burstSeed, int rootArgb, int tipArgb,
                            float speedMin, float speedMax, float gravityScale, int lifeTicks, int restTicks) {
        if (level.isClientSide()) return 0;
        int n = areaRoom(level, center, count);
        if (n <= 0) return 0;
        Random rand = new Random(burstSeed);
        Vec3 ax = axis.lengthSqr() < 1e-8 ? new Vec3(0, 1, 0) : axis.normalize();
        Vec3 u = perpOf(ax);
        Vec3 v = ax.cross(u).normalize();
        double spread = Math.toRadians(spreadDeg);
        int spawned = 0;
        for (int i = 0; i < n; i++) {
            double ang = spread * Math.sqrt(rand.nextFloat());
            double phi = rand.nextDouble() * Math.PI * 2.0;
            Vec3 dir = ax.scale(Math.cos(ang))
                .add(u.scale(Math.sin(ang) * Math.cos(phi)))
                .add(v.scale(Math.sin(ang) * Math.sin(phi)));
            float length = lengthFor(rand, unit);
            float girth = girthFor(rand, length);
            Vec3 origin = center
                .add(u.scale((rand.nextDouble() * 2.0 - 1.0) * unit * 0.12))
                .add(v.scale((rand.nextDouble() * 2.0 - 1.0) * unit * 0.12));
            double speed = speedMin + rand.nextDouble() * (speedMax - speedMin);
            spawn(level, owner, origin, dir.scale(speed), dir, MODE_BURST,
                (int) (burstSeed + i * 0x9E37L), rootArgb, tipArgb, length, girth,
                gravityScale, 0.96, restTicks, lifeTicks, -1, null, Vec3.ZERO, 0f);
            spawned++;
        }
        return spawned;
    }

    /**
     * Clustered fall for SkyCracks: cluster c's base seed is
     * hostSeed ^ (0x5A9D + c * 131), the renderer's own cluster seeds,
     * cluster centers at seeded disc offsets, shards raining at the
     * renderer's drop rate before gravity takes them. Mode BALLISTIC.
     * Returns the number spawned.
     */
    public static int rain(ServerLevel level, Entity owner, Vec3 center, float discRadius,
                           int[] clusterCounts, float unit, long hostSeed, int rootArgb, int tipArgb) {
        if (level.isClientSide()) return 0;
        int total = 0;
        for (int c : clusterCounts) total += c;
        int budget = areaRoom(level, center, total);
        if (budget <= 0) return 0;
        int spawned = 0;
        for (int c = 0; c < clusterCounts.length && spawned < budget; c++) {
            long baseSeed = hostSeed ^ (0x5A9DL + c * 131L);
            Random rand = new Random(baseSeed);
            double ang = rand.nextDouble() * Math.PI * 2.0;
            double rr = discRadius * Math.sqrt(rand.nextDouble());
            Vec3 clusterCenter = center.add(Math.cos(ang) * rr, 0, Math.sin(ang) * rr);
            for (int i = 0; i < clusterCounts[c] && spawned < budget; i++) {
                float length = lengthFor(rand, unit);
                float girth = girthFor(rand, length);
                Vec3 pos = clusterCenter.add(
                    (rand.nextDouble() * 2.0 - 1.0) * unit * 0.12, 0,
                    (rand.nextDouble() * 2.0 - 1.0) * unit * 0.12);
                Vec3 vel = new Vec3((rand.nextDouble() * 2.0 - 1.0) * 0.03, -0.35,
                    (rand.nextDouble() * 2.0 - 1.0) * 0.03);
                spawn(level, owner, pos, vel, vel, MODE_BALLISTIC,
                    (int) (baseSeed + i * 0x9E37L), rootArgb, tipArgb, length, girth,
                    1.0f, 0.99, 30, 220, -1, null, Vec3.ZERO, 0f);
                spawned++;
            }
        }
        return spawned;
    }

    /**
     * One FROZEN fragment hanging at a fixed position and pose.
     * releaseAtAge presets the drop tick (the TimeShatter pattern);
     * releaseAtAge < 0 holds until the host calls release() (the Void
     * Rift pattern). Returns the fragment, or null past the caps.
     */
    public static FragmentEntity hang(ServerLevel level, Entity owner, Vec3 pos, Vec3 axisDir,
                                      float length, float girth, int seed, int rootArgb, int tipArgb,
                                      int releaseAtAge, int restTicks) {
        if (level.isClientSide()) return null;
        if (areaRoom(level, pos, 1) <= 0) return null;
        int life = (releaseAtAge > 0 ? releaseAtAge : 600) + 300;
        return spawn(level, owner, pos, Vec3.ZERO, axisDir, MODE_FROZEN, seed, rootArgb, tipArgb,
            length, girth, 1.0f, 0.99, restTicks, life, releaseAtAge, null, Vec3.ZERO, 0f);
    }

    /**
     * One FIELD_HELD fragment anchored to a holder entity at a fixed
     * offset (the Valor Field pattern): it follows the holder for real
     * until release() is called or the holder is gone. Returns the
     * fragment, or null past the caps or without a holder.
     */
    public static FragmentEntity hold(ServerLevel level, Entity owner, Entity holder, Vec3 anchorOffset,
                                      Vec3 axisDir, float length, float girth, int seed,
                                      int rootArgb, int tipArgb, float spinDegPerTick, int restTicks) {
        if (level.isClientSide() || holder == null) return null;
        Vec3 pos = holder.position().add(anchorOffset);
        if (areaRoom(level, pos, 1) <= 0) return null;
        return spawn(level, owner, pos, Vec3.ZERO, axisDir, MODE_FIELD_HELD, seed, rootArgb, tipArgb,
            length, girth, 1.0f, 0.99, restTicks, HARD_LIFE_TICKS, -1,
            holder.getUUID(), anchorOffset, spinDegPerTick);
    }
}
