package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.util.DbaDamage;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.dragonblockarcanedba.util.LayeredParticles;
import com.dragonblockarcanedba.util.TimeTracker;
import com.dragonblockarcanedba.util.VortexPhysics;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
 * Temporal Rift Entity: the material based time suspension field spawned on
 * Right-Click (Temporal Rift) with the Whis Staff. The client renderer
 * draws no geometry for it; the field reads as temporal plasma, soul
 * material, glass dust, mist, and darkness streaming inward.
 *
 * The rift is a time suspension field, not a storm. For its whole life it runs
 * the shared lifecycle envelope: layered particles in three layers, a debris
 * field caught hanging in the air on slow helical paths, and a gentle vortex
 * drift that makes everything inside orbit slowly instead of sitting dead
 * still. The item applies the first half of the burst and the freeze at cast;
 * this entity lands the second half of the burst at the envelope peak and
 * starts the time reversal at tick 60, the moment the freeze ends.
 */
public class TemporalRiftEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(TemporalRiftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(TemporalRiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAX_LIFE = SynchedEntityData.defineId(TemporalRiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> SEED = SynchedEntityData.defineId(TemporalRiftEntity.class, EntityDataSerializers.LONG);

    /** Tick at which the envelope peaks (15 percent build of a 100 tick life): the second burst half lands here. */
    public static final int PEAK_TICK = 15;
    /** Tick at which the item's 60 tick freeze ends and the reversal begins. */
    public static final int REVERSAL_TICK = 60;
    /** Reversal length once it starts: 100 ticks, the position history buffer maximum. */
    public static final int REVERSAL_TICKS = 100;
    /** Debris pieces caught in the suspension field (domain class count per the spectacle standard). */
    private static final int DEBRIS_COUNT = 60;

    /** Celestial cyan, the rift's theme color as a dust RGB int. */
    private static final int THEME_RGB = 0x61C2FF;

    private int maxLifetime = 60;
    private int lifetime = 60;
    private float peakDamage = 0f;
    private boolean debrisSpawned = false;

    public TemporalRiftEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public TemporalRiftEntity(Level level, LivingEntity caster, Vec3 pos, float radius, int lifetime, float peakDamage) {
        super(DbaEntities.TEMPORAL_RIFT, level);
        this.setOwner(caster);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setRadius(radius);
        this.maxLifetime = lifetime;
        this.lifetime = lifetime;
        this.peakDamage = peakDamage;
        this.entityData.set(CASTER_ID, caster != null ? caster.getId() : -1);
        this.entityData.set(MAX_LIFE, lifetime);
        // Material seed: rolled once at cast on the server and synced,
        // so the renderer's material variation is identical on every client.
        this.entityData.set(SEED, level.getRandom().nextLong());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 12.0f);
        builder.define(CASTER_ID, -1);
        builder.define(MAX_LIFE, 60);
        builder.define(SEED, 0L);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(RADIUS, radius);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    public int getMaxLifetime() {
        return this.entityData.get(MAX_LIFE);
    }

    /** Synced material seed (0 until the cast constructor sets it). */
    public long getSeed() {
        return this.entityData.get(SEED);
    }

    @Override
    public void tick() {
        super.tick();

        Entity owner = this.getOwner();
        if (owner == null && getCasterId() != -1 && this.level() instanceof ServerLevel serverLevel) {
            owner = serverLevel.getEntity(getCasterId());
            if (owner != null) {
                this.setOwner(owner);
            }
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            int life = Math.max(1, this.maxLifetime);
            float env = FxEnvelope.envelope(this.tickCount, life);
            float radius = getRadius();
            Vec3 center = this.position();
            LivingEntity ownerLiving = owner instanceof LivingEntity living ? living : null;
            // Final alias for the target filters below: owner itself is
            // reassigned by the re-fetch above, so lambdas cannot capture it.
            final Entity ownerRef = owner;

            // Ambient temporal hum sound
            if (this.tickCount % 20 == 0) {
                serverLevel.playSound(
                    null, this.getX(), this.getY() + 1.0, this.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.RIFT_AMBIENT, SoundSource.PLAYERS,
                    0.8f, 1.2f
                );
            }

            // Point 1: layered particles at standard density. The shared
            // emitter lays down a bright KI_MOTE core, a celestial cyan mid
            // layer on a helical path, and wispy motes at the rim, all scaled
            // by this rift's envelope, replacing the old single ring of about
            // 2 motes every 3 ticks (about 13 per second over a 24 block field).
            LayeredParticles.emitVortex(serverLevel, center, radius * 0.92f, radius * 0.85f,
                this.tickCount, life, THEME_RGB,
                com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE);

            // Point 3: time made visible. A gentle vortex drift, far below
            // storm strength, so bodies and players inside slowly orbit and
            // hover in suspension instead of freezing in dead air. The swirl
            // dominates the pull on purpose: things circle, they do not get
            // dragged to the middle.
            VortexPhysics.applyVortex(serverLevel, center, radius, radius * 1.05,
                ownerLiving, 0.030, 1.6, 0.05, env);

            // Point 2: the ground the rift stands on, caught mid-air. Sixty
            // pieces rise only a little and hang on slow helical paths for
            // most of the rift's life, then release and rain back down while
            // the field dissipates. Spawned once, just after the build starts.
            if (!this.debrisSpawned && this.tickCount >= 2) {
                this.debrisSpawned = true;
                DebrisSpawner.spawnField(serverLevel, ownerLiving, center,
                    DEBRIS_COUNT, radius * 0.8, 2.5, 80);

                // Entity objects pass: the temporal glass shards at the
                // tear heart are real FragmentEntity pieces now, burst on
                // this same beat with the renderer's old values (5 shards,
                // 150 degree upward cone, seed family riftSeed ^ 0x7E42,
                // unit max(0.5, radius * 0.09), glass palette). They drift
                // outward under slowed gravity (0.3, the suspension field)
                // and live about as long as the rift itself. No damage.
                FragmentEntity.burst(serverLevel, owner,
                    center.add(0.0, radius * 0.441, 0.0), new Vec3(0.0, 1.0, 0.0), 150f,
                    Math.max(0.5f, radius * 0.09f), 5, this.getSeed() ^ 0x7E42L,
                    0x2F6DB3, 0xDDF7FF, 0.10f, 0.30f, 0.3f, 110, 10);
            }

            // Point 5: the second half of the burst lands at the envelope
            // peak, inside the visual's active window, on everything the
            // field covers at that moment. The item dealt the first half at
            // cast; the total is unchanged.
            if (this.tickCount == PEAK_TICK && this.peakDamage > 0f) {
                AABB aoe = this.getBoundingBox().inflate(radius);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, aoe, e -> e != ownerRef && e.isAlive());
                for (LivingEntity target : targets) {
                    if (this.distanceTo(target) <= radius) {
                        target.hurtServer(serverLevel, DbaDamage.temporal(this, owner),
                            com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(this.peakDamage));
                    }
                }
                serverLevel.playSound(
                    null, this.getX(), this.getY() + 1.0, this.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.CRYSTAL_CHIME, SoundSource.PLAYERS,
                    0.8f, 1.8f
                );
            }

            // The reversal starts when the freeze ends, exactly as the item
            // describes it: the item freezes targets for 60 ticks at cast,
            // and at this tick the rift rewinds everything still inside it.
            if (this.tickCount == REVERSAL_TICK) {
                AABB aoe = this.getBoundingBox().inflate(radius);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, aoe, e -> e != ownerRef && e.isAlive());
                for (LivingEntity target : targets) {
                    if (this.distanceTo(target) <= radius && target instanceof TimeTracker tracker) {
                        tracker.dba$startReversing(REVERSAL_TICKS);
                    }
                }
            }

            // Point 5: dissipation drizzle of motes falling as the field dies.
            if (this.tickCount > life * 0.65f) {
                LayeredParticles.emitRain(serverLevel, center, radius * 0.9f, radius * 0.8f,
                    this.tickCount, life,
                    com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE);
            }

            this.lifetime--;
            if (this.lifetime <= 0) {
                this.discard();
            }
        }
    }
}
