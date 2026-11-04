package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.dragonblockarcanedba.util.LayeredParticles;
import com.dragonblockarcanedba.util.VortexPhysics;

/**
 * Power Pole Whirlwind Entity: ONE sustained vortex for the whole Wind Spin.
 *
 * Lifecycle (SPECTACLE-STANDARD point 5): the funnel builds over BUILD_TICKS,
 * sustains at full envelope while the item keeps refreshing it (the network
 * handler calls performWindSpin every 2 ticks while held), then dissipates
 * over DECAY_TICKS once refreshes stop. The renderer reads the same phase
 * data (age plus the synced DECAY_START) so mesh alpha, particle counts, the
 * damage window, and the vortex forces all agree. Nothing pops in and
 * nothing discards at visible alpha.
 *
 * While it sustains, the spin is a real vortex: VortexPhysics swirls and
 * lifts mobs around the wielder and along the cone, DebrisSpawner throws
 * ground debris caught in the spin, and when the spin dies it flings what
 * it was holding outward before the debris rains back down.
 */
public class PowerPoleWhirlwindEntity extends Projectile {
    private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(PowerPoleWhirlwindEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(PowerPoleWhirlwindEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RANGE = SynchedEntityData.defineId(PowerPoleWhirlwindEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CONE_ANGLE = SynchedEntityData.defineId(PowerPoleWhirlwindEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(PowerPoleWhirlwindEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DECAY_START = SynchedEntityData.defineId(PowerPoleWhirlwindEntity.class, EntityDataSerializers.INT);

    /** Ticks the funnel takes to build to full strength. Damage waits for this. */
    public static final int BUILD_TICKS = 10;
    /** Ticks the funnel takes to dissipate once the spin stops being refreshed. */
    public static final int DECAY_TICKS = 24;
    /** Missed-refresh window before dissipation begins (refresh arrives every 2 ticks). */
    public static final int REFRESH_GRACE_TICKS = 6;
    /** Theme color for the layered particle mid band: the whirlwind emerald. */
    private static final int THEME_ARGB = 0x26FFA6;

    // One live whirlwind per spinning player, server side. The item refreshes
    // the existing entity instead of respawning a fresh 8 tick one every 6
    // ticks, which used to restart the fade envelope forever.
    private static final Map<UUID, PowerPoleWhirlwindEntity> ACTIVE = new ConcurrentHashMap<>();

    /** Nominal minimum life when tapped once: build + grace + decay. */
    private int maxLifetime = BUILD_TICKS + REFRESH_GRACE_TICKS + DECAY_TICKS;

    // Server side sustain state.
    private int lastRefreshAge = 0;
    private Vec3 vortexBase = null;

    public PowerPoleWhirlwindEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public PowerPoleWhirlwindEntity(Level level, LivingEntity owner, Vec3 pos, float yaw, float pitch, float range, float coneAngle) {
        super(DbaEntities.POWER_POLE_WHIRLWIND, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setEntityYaw(yaw);
        this.setEntityPitch(pitch);
        this.setRange(range);
        this.setConeAngle(coneAngle);
        if (owner != null) {
            this.entityData.set(CASTER_ID, owner.getId());
            this.vortexBase = owner.position();
            if (!level.isClientSide()) {
                ACTIVE.put(owner.getUUID(), this);
            }
        }
    }

    /** The live whirlwind for this owner, or null. Stale entries are dropped. */
    public static PowerPoleWhirlwindEntity findActive(LivingEntity owner) {
        if (owner == null) return null;
        PowerPoleWhirlwindEntity active = ACTIVE.get(owner.getUUID());
        if (active == null) return null;
        if (active.isRemoved() || active.level() != owner.level() || active.getOwner() != owner) {
            ACTIVE.remove(owner.getUUID(), active);
            return null;
        }
        return active;
    }

    /** Called by the item on every spin tick while held: keeps the funnel sustaining. */
    public void refreshSpin() {
        this.lastRefreshAge = this.tickCount;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(YAW, 0.0f);
        builder.define(PITCH, 0.0f);
        builder.define(RANGE, 25.0f);
        builder.define(CONE_ANGLE, 35.0f);
        builder.define(CASTER_ID, -1);
        builder.define(DECAY_START, -1);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    public float getEntityYaw() {
        return this.entityData.get(YAW);
    }

    public void setEntityYaw(float yaw) {
        this.entityData.set(YAW, yaw);
    }

    public float getEntityPitch() {
        return this.entityData.get(PITCH);
    }

    public void setEntityPitch(float pitch) {
        this.entityData.set(PITCH, pitch);
    }

    public float getRange() {
        return this.entityData.get(RANGE);
    }

    public void setRange(float range) {
        this.entityData.set(RANGE, range);
    }

    public float getConeAngle() {
        return this.entityData.get(CONE_ANGLE);
    }

    public void setConeAngle(float coneAngle) {
        this.entityData.set(CONE_ANGLE, coneAngle);
    }

    /** Tick at which dissipation began, or -1 while sustaining. Synced for the renderer. */
    public int getDecayStart() {
        return this.entityData.get(DECAY_START);
    }

    public int getMaxLifetime() {
        return this.maxLifetime;
    }

    /**
     * The shared phase envelope, 0..1: smoothstep build over BUILD_TICKS, full
     * while sustaining, smoothstep out over DECAY_TICKS after DECAY_START.
     * The renderer evaluates the identical formula from age + DECAY_START.
     */
    public float currentEnvelope() {
        float age = this.tickCount;
        int decayStart = getDecayStart();
        if (decayStart >= 0) {
            // Decay falls from whatever height the build had reached when the
            // spin stopped (an interrupted build never jumps up to full).
            float peak = decayStart < BUILD_TICKS
                ? FxEnvelope.smoothstep(decayStart / (float) BUILD_TICKS)
                : 1.0f;
            if (age <= decayStart) return peak;
            return peak * (1.0f - FxEnvelope.smoothstep((age - decayStart) / (float) DECAY_TICKS));
        }
        if (age < BUILD_TICKS) return FxEnvelope.smoothstep(age / (float) BUILD_TICKS);
        return 1.0f;
    }

    /** Damage and deflection live inside the visual's active window: built, not yet spent. */
    public boolean isInActiveWindow() {
        return this.tickCount >= BUILD_TICKS && currentEnvelope() > 0.25f;
    }

    private void beginDecay(ServerLevel serverLevel) {
        this.entityData.set(DECAY_START, this.tickCount);
        // Ejection beat: the dying spin throws what it was holding outward and
        // up, the way the storm in the reference throws its debris ring.
        if (this.vortexBase != null) {
            AABB box = new AABB(
                this.vortexBase.x - 5.0, this.vortexBase.y - 1.0, this.vortexBase.z - 5.0,
                this.vortexBase.x + 5.0, this.vortexBase.y + 4.0, this.vortexBase.z + 5.0);
            List<LivingEntity> held = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != this.getOwner());
            for (LivingEntity target : held) {
                VortexPhysics.fling(target, this.vortexBase, 0.9, 0.5);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            // Client keeps the funnel locked to the wielder's eye and aim
            // between server updates while it sustains, exactly as the old
            // 8 tick entity did; it freezes where it was once decaying.
            if (getDecayStart() < 0 && this.getOwner() instanceof LivingEntity owner && owner.isAlive()) {
                Vec3 eyePos = owner.getEyePosition();
                this.setPos(eyePos.x, eyePos.y, eyePos.z);
                this.setEntityYaw(owner.getYRot());
                this.setEntityPitch(owner.getXRot());
            }
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
        boolean ownerValid = owner != null && owner.isAlive();
        boolean decaying = getDecayStart() >= 0;

        if (!decaying) {
            if (!ownerValid || this.tickCount - this.lastRefreshAge > REFRESH_GRACE_TICKS) {
                beginDecay(serverLevel);
                decaying = true;
            } else {
                // Follow the wielder's eye and aim while the spin sustains.
                Vec3 eyePos = owner.getEyePosition();
                this.setPos(eyePos.x, eyePos.y, eyePos.z);
                this.setEntityYaw(owner.getYRot());
                this.setEntityPitch(owner.getXRot());
                this.vortexBase = owner.position();
            }
        }
        if (decaying && this.tickCount - getDecayStart() >= DECAY_TICKS) {
            this.discard();
            return;
        }
        if (this.vortexBase == null) {
            this.vortexBase = this.position().add(0, -1.62, 0);
        }

        float env = currentEnvelope();
        if (env <= 0f) return;

        // Effective (age, life) for the shared particle envelope: while
        // sustaining, the notional life sits one decay window ahead so the
        // envelope holds at peak; once decaying, the true end is known.
        int decayStart = getDecayStart();
        float life = decayStart >= 0 ? decayStart + DECAY_TICKS : this.tickCount + DECAY_TICKS;

        // Layered particles, wielder column: the spin around the player.
        LayeredParticles.emitVortex(serverLevel, this.vortexBase, 2.0f, 2.8f,
            this.tickCount, life, THEME_ARGB, DbaParticles.WIND);

        Vec3 look = this.calculateViewVector(this.getEntityPitch(), this.getEntityYaw());

        // Layered particles, cone body: a swirling volume partway down the cone.
        if (this.tickCount % 2 == 0) {
            Vec3 mid = this.position().add(look.scale(getRange() * 0.45));
            LayeredParticles.emitVortex(serverLevel, new Vec3(mid.x, mid.y - 2.0, mid.z), 2.8f, 4.5f,
                this.tickCount, life, THEME_ARGB, DbaParticles.WIND);
        }

        // Vortex forces: mobs caught in the spin swirl, lift, and get held in
        // the effect instead of only being repelled on damage ticks.
        VortexPhysics.applyVortex(serverLevel, this.vortexBase, 4.5, 3.2,
            ownerValid ? owner : null, 0.16, 1.6, 0.30, env);
        Vec3 coneMid = this.position().add(look.scale(9.0));
        VortexPhysics.applyVortex(serverLevel, new Vec3(coneMid.x, coneMid.y - 2.2, coneMid.z), 5.0, 5.5,
            ownerValid ? owner : null, 0.10, 1.2, 0.18, env);

        if (!decaying) {
            // Debris caught in the spin: a fresh handful of ground pieces joins
            // the orbit every second at peak, orbits, then rains back down.
            if (this.tickCount >= BUILD_TICKS && this.tickCount % 20 == 0 && ownerValid) {
                DebrisSpawner.spawnField(serverLevel, owner, this.vortexBase, 8, 3.0, 3.5, 26);
            }
            // Sustained gust audio under the item's spawn sounds.
            if (this.tickCount % 16 == 0 && env > 0.5f) {
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.1f, 1.3f);
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.WIND_GUST, SoundSource.PLAYERS, 0.8f, 1.1f);
            }
        }
    }
}
