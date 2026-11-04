package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.dragonblockarcanedba.util.DbaDamage;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * One lobbed blob of living lava (redesign ideas 2 and 6): thrown by
 * geysers and by the mini volcano. It flies a real ballistic arc under
 * manual gravity (the DebrisEntity pattern, noPhysics with server
 * authoritative motion), hits one target hard on a direct strike, and
 * wherever it lands it splashes and leaves one cooling crust pool.
 */
public class LavaBlobEntity extends Projectile {
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(LavaBlobEntity.class, EntityDataSerializers.FLOAT);
    /** Shape seed for the bespoke renderer: synced, so every client draws the same blob. */
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(LavaBlobEntity.class, EntityDataSerializers.INT);
    private static final double GRAVITY = 0.06;
    private static final int HARD_LIFE = 200;

    public LavaBlobEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public LavaBlobEntity(Level level, LivingEntity owner, Vec3 pos, Vec3 velocity, float damage) {
        super(DbaEntities.LAVA_BLOB, level);
        this.setOwner(owner);
        this.noPhysics = true;
        this.entityData.set(DAMAGE, damage);
        this.entityData.set(SEED, level.getRandom().nextInt());
        this.setPos(pos.x, pos.y, pos.z);
        this.setDeltaMovement(velocity);
    }

    /**
     * Lob a blob from {@code from} so it lands on {@code target} after
     * {@code flightTicks} ticks of the manual gravity above. The vertical
     * term solves the discrete sum, so the arc lands where it was aimed
     * instead of drifting long.
     */
    public static LavaBlobEntity launch(ServerLevel level, LivingEntity owner, Vec3 from, Vec3 target,
                                        int flightTicks, float damage) {
        double vx = (target.x - from.x) / flightTicks;
        double vz = (target.z - from.z) / flightTicks;
        double vy = (target.y - from.y) / flightTicks + 0.5 * GRAVITY * (flightTicks + 1);
        LavaBlobEntity blob = new LavaBlobEntity(level, owner, from, new Vec3(vx, vy, vz), damage);
        level.addFreshEntity(blob);
        return blob;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DAMAGE, 40.0f);
        builder.define(SEED, 0);
    }

    public float getDamage() {
        return this.entityData.get(DAMAGE);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (this.tickCount > HARD_LIFE) {
            this.discard();
            return;
        }
        // A dripping trail of the sword's own flame while it flies.
        server.sendParticles(DbaParticles.FLAME, this.getX(), this.getY(), this.getZ(),
            1, 0.08, 0.08, 0.08, 0.01);
        Vec3 vel = this.getDeltaMovement();
        vel = new Vec3(vel.x, vel.y - GRAVITY, vel.z);
        this.setDeltaMovement(vel);
        Vec3 next = this.position().add(vel);

        Entity owner = this.getOwner();
        float damage = this.entityData.get(DAMAGE);
        // Direct strike: the first living thing in the blob's path takes
        // the full hit, then the blob splats where it struck.
        AABB box = this.getBoundingBox().move(vel).inflate(0.35);
        List<LivingEntity> direct = server.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != owner);
        if (!direct.isEmpty()) {
            LivingEntity hit = direct.get(0);
            hit.hurtServer(server, DbaDamage.lava(this, owner),
                com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(damage));
            hit.setRemainingFireTicks(Math.max(hit.getRemainingFireTicks(), 60));
            com.dragonblockarcanedba.util.DbaCompat.markHurt(hit);
            this.splat(server, hit.position());
            this.discard();
            return;
        }
        // Ground strike: the block below catches the fall.
        var below = net.minecraft.core.BlockPos.containing(next.x, next.y - 0.06, next.z);
        if (vel.y <= 0 && !this.level().getBlockState(below).getCollisionShape(this.level(), below).isEmpty()) {
            this.setPos(next.x, below.getY() + 1.0, next.z);
            this.splat(server, this.position());
            this.discard();
            return;
        }
        this.setPos(next.x, next.y, next.z);
    }

    /** The landing: splash damage around the point and one crust pool left behind. */
    private void splat(ServerLevel server, Vec3 at) {
        Entity owner = this.getOwner();
        float damage = this.entityData.get(DAMAGE);
        AABB splash = new AABB(at.x - 1.3, at.y - 0.5, at.z - 1.3, at.x + 1.3, at.y + 1.5, at.z + 1.3);
        for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class, splash,
                e -> e.isAlive() && e != owner)) {
            living.hurtServer(server, DbaDamage.lava(this, owner),
                com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(damage * 0.6f));
            living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 40));
            com.dragonblockarcanedba.util.DbaCompat.markHurt(living);
        }
        LayeredParticles.emitGroundBurst(server, at, 0.9f, 0xFF5A00);
        server.playSound(null, at.x, at.y, at.z, DbaSounds.LAVA_BUBBLE, SoundSource.BLOCKS, 0.7f, 0.9f);
        // The splat stays as a pool that cools through the crust stages.
        LavaField.crustColumn(server, Mth.floor(at.x), Mth.floor(at.z), Mth.floor(at.y),
            LavaField.PROFILE_POOL, owner instanceof LivingEntity living ? living : null, damage * 0.5f);
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Damage", getDamage());
        output.putInt("Seed", getSeed());
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DAMAGE, input.getFloatOr("Damage", 40.0f));
        this.entityData.set(SEED, input.getIntOr("Seed", 0));
    }
}
