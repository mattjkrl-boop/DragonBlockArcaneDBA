package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Swift Crescent Entity: physical 3D crescent blade flash spawned on a target
 * during Katana Flashdraw. A crescent spawned with a strike payload owns its
 * hit: the damage, impact burst, and sounds land at the strike tick inside
 * its own life, as the cut opens. Trail crescents carry no payload and are
 * pure visuals.
 */
public class SwiftCrescentEntity extends Projectile {
    private static final EntityDataAccessor<Float> SLASH_SCALE = SynchedEntityData.defineId(SwiftCrescentEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TILT_ANGLE = SynchedEntityData.defineId(SwiftCrescentEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MAX_LIFETIME = SynchedEntityData.defineId(SwiftCrescentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLASH_VARIANT = SynchedEntityData.defineId(SwiftCrescentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(SwiftCrescentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> STRIKE_DAMAGE = SynchedEntityData.defineId(SwiftCrescentEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> STRIKE_TICK = SynchedEntityData.defineId(SwiftCrescentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STRIKE_PRIMARY = SynchedEntityData.defineId(SwiftCrescentEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> STRIKE_PITCH = SynchedEntityData.defineId(SwiftCrescentEntity.class, EntityDataSerializers.FLOAT);

    public SwiftCrescentEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SwiftCrescentEntity(Level level, LivingEntity owner, Vec3 pos, float yaw, float pitch, float tiltAngle, float scale, int variant, int maxLifetime) {
        super(DbaEntities.SWIFT_CRESCENT, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.noPhysics = true;

        this.entityData.set(SLASH_SCALE, scale);
        this.entityData.set(TILT_ANGLE, tiltAngle);
        this.entityData.set(SLASH_VARIANT, variant);
        this.entityData.set(MAX_LIFETIME, maxLifetime);
    }

    /** Strike carrying constructor: the crescent hits targetId for strikeDamage at strikeTick. */
    public SwiftCrescentEntity(Level level, LivingEntity owner, Vec3 pos, float yaw, float pitch, float tiltAngle, float scale, int variant, int maxLifetime,
                               int targetId, float strikeDamage, int strikeTick, boolean strikePrimary, float strikePitch) {
        this(level, owner, pos, yaw, pitch, tiltAngle, scale, variant, maxLifetime);
        this.entityData.set(TARGET_ID, targetId);
        this.entityData.set(STRIKE_DAMAGE, strikeDamage);
        this.entityData.set(STRIKE_TICK, strikeTick);
        this.entityData.set(STRIKE_PRIMARY, strikePrimary);
        this.entityData.set(STRIKE_PITCH, strikePitch);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SLASH_SCALE, 1.0f);
        builder.define(TILT_ANGLE, 0.0f);
        builder.define(SLASH_VARIANT, 0);
        builder.define(MAX_LIFETIME, 10);
        builder.define(TARGET_ID, -1);
        builder.define(STRIKE_DAMAGE, 0.0f);
        builder.define(STRIKE_TICK, 5);
        builder.define(STRIKE_PRIMARY, false);
        builder.define(STRIKE_PITCH, 1.5f);
    }

    public float getSlashScale() {
        return this.entityData.get(SLASH_SCALE);
    }

    public float getTiltAngle() {
        return this.entityData.get(TILT_ANGLE);
    }

    public int getSlashVariant() {
        return this.entityData.get(SLASH_VARIANT);
    }

    public int getMaxLifetime() {
        return this.entityData.get(MAX_LIFETIME);
    }

    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    public float getStrikeDamage() {
        return this.entityData.get(STRIKE_DAMAGE);
    }

    public int getStrikeTick() {
        return this.entityData.get(STRIKE_TICK);
    }

    public boolean isStrikePrimary() {
        return this.entityData.get(STRIKE_PRIMARY);
    }

    public float getStrikePitch() {
        return this.entityData.get(STRIKE_PITCH);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            emitCrescentParticles(serverLevel);
            if (getStrikeDamage() > 0f && this.tickCount == getStrikeTick()) {
                strike(serverLevel);
            }
            if (this.tickCount >= getMaxLifetime()) {
                this.discard();
            }
        }
    }

    /**
     * The cut lands: damage through the shared cap, the heavy impact burst on
     * the chain's first target, a ground dust kick at the target's feet, and
     * the crit and shing sounds, all at the strike tick.
     */
    private void strike(ServerLevel level) {
        if (!(level.getEntity(getTargetId()) instanceof LivingEntity target)) return;
        if (!target.isAlive() || target.isRemoved()) return;
        if (this.getOwner() instanceof Player player) {
            target.hurtServer(level, level.damageSources().playerAttack(player),
                DbaEffectsConfig.capDamage(getStrikeDamage()));
        }
        if (isStrikePrimary()) {
            com.dragonblockarcanedba.fx.DbaImpactFx.medium(level, target.getX(), target.getY(), target.getZ());
            com.dragonblockarcanedba.fx.DbaImpactFx.burst(level, target.getX(), target.getY(), target.getZ());
        }
        LayeredParticles.emitGroundBurst(level,
            new Vec3(target.getX(), target.getY(), target.getZ()), 0.8f, 0x9FE8FF);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.6f, getStrikePitch());
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.BLADE_SHING, SoundSource.PLAYERS, 1.6f, getStrikePitch());
    }

    /**
     * Layered emission across the crescent's life (standard points 1 and 5):
     * bright core motes off the cut face, pale cyan mid dust tracing the arc,
     * and a spark drizzle while the envelope decays.
     */
    private void emitCrescentParticles(ServerLevel level) {
        float density = DbaEffectsConfig.effectDensity();
        if (density <= 0f) return;
        float env = FxEnvelope.envelope(this.tickCount, getMaxLifetime(), 0.25f, 0.35f);
        if (env <= 0f) return;
        net.minecraft.util.RandomSource rand = this.getRandom();
        float scale = getSlashScale();

        if (rand.nextFloat() < env * density) {
            level.sendParticles(DbaParticles.KI_MOTE,
                this.getX() + (rand.nextDouble() - 0.5) * 1.6 * scale,
                this.getY() + (rand.nextDouble() - 0.5) * 0.8,
                this.getZ() + (rand.nextDouble() - 0.5) * 1.6 * scale,
                1, 0.02, 0.02, 0.02, 0.01);
        }
        if (rand.nextFloat() < env * density) {
            level.sendParticles(LayeredParticles.dust(0x9FE8FF, 1.1f),
                this.getX() + (rand.nextDouble() - 0.5) * 2.2 * scale,
                this.getY() + (rand.nextDouble() - 0.5) * 1.0,
                this.getZ() + (rand.nextDouble() - 0.5) * 2.2 * scale,
                1, 0.03, 0.02, 0.03, 0.01);
        }
        float progress = this.tickCount / (float) getMaxLifetime();
        if (progress > 0.65f) {
            LayeredParticles.emitRain(level, this.position(), 1.0f * scale, 0.9f,
                this.tickCount, getMaxLifetime(), DbaParticles.SPARK);
        }
    }
}
