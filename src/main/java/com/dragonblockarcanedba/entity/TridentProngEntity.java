package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The devil trident's shot: three barbed crimson needles flying as one formation.
 * This is not a ki blast. The formation pierces: the first body takes the full
 * trident (750), the second and third take falling shares, and the shot dies on
 * the third hit, on a block, or at max range. hitIds is the pierce ledger: one
 * entry per body pierced, so no target is ever billed twice by one shot.
 */
public class TridentProngEntity extends Projectile {
    private static final EntityDataAccessor<Integer> DIE_TICK =
        SynchedEntityData.defineId(TridentProngEntity.class, EntityDataSerializers.INT);

    /** Pierce falloff per body struck: full, two thirds, just under half. */
    private static final float[] PIERCE_SHARES = {1.0f, 0.66f, 0.45f};
    private static final int MAX_PIERCES = PIERCE_SHARES.length;
    /** Crimson of the shard blades, as a dust color for the trail layers. */
    private static final int TRAIL_ARGB = 0xFFFF3319;

    private float damage = 750.0f;
    private final Set<Integer> hitIds = new HashSet<>();

    /** Tick the shot began to dissolve. 0 while it is still flying. */
    public int getDieTick() {
        return this.entityData.get(DIE_TICK);
    }

    private void beginDying() {
        if (this.entityData.get(DIE_TICK) == 0) {
            this.entityData.set(DIE_TICK, Math.max(1, this.tickCount));
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    public TridentProngEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public TridentProngEntity(Level level, LivingEntity owner, float damage) {
        super(DbaEntities.TRIDENT_PRONG, level);
        this.setOwner(owner);
        this.damage = damage;
        this.noPhysics = true;
        if (owner != null) {
            Vec3 look = owner.getLookAngle();
            Vec3 right = look.cross(new Vec3(0, 1, 0));
            if (right.lengthSqr() < 1.0E-4) {
                right = new Vec3(1, 0, 0);
            }
            right = right.normalize();
            Vec3 up = right.cross(look).normalize();
            Vec3 start = owner.getEyePosition().add(look.scale(0.9)).add(up.scale(-0.2));
            this.setPos(start.x, start.y, start.z);
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
            this.setDeltaMovement(look.scale(2.5));
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DIE_TICK, 0);
    }

    /** Layered flight trail (standard point 1): bright mote core, crimson dust mid, ember wisps. */
    private void emitTrail(ServerLevel serverLevel) {
        float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
        if (density <= 0f) {
            return;
        }
        serverLevel.sendParticles(
            com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
            this.getX(), this.getY(), this.getZ(),
            Math.max(1, Math.round(2f * density)), 0.10, 0.10, 0.10, 0.01);
        serverLevel.sendParticles(
            com.dragonblockarcanedba.util.LayeredParticles.dust(TRAIL_ARGB, 1.2f),
            this.getX(), this.getY(), this.getZ(),
            Math.max(1, Math.round(2f * density)), 0.16, 0.16, 0.16, 0.01);
        if (this.tickCount % 2 == 0) {
            serverLevel.sendParticles(
                com.dragonblockarcanedba.particle.DbaParticles.EMBER,
                this.getX(), this.getY(), this.getZ(),
                1, 0.20, 0.20, 0.20, 0.01);
        }
    }

    /** Hit feedback: a crimson spark burst where a prong bites, so hits read at range. */
    private void emitHitBurst(ServerLevel serverLevel, Vec3 at) {
        float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
        if (density <= 0f) {
            return;
        }
        serverLevel.sendParticles(
            com.dragonblockarcanedba.particle.DbaParticles.SPARK,
            at.x, at.y, at.z,
            Math.max(2, Math.round(6f * density)), 0.25, 0.25, 0.25, 0.12);
        serverLevel.sendParticles(
            com.dragonblockarcanedba.util.LayeredParticles.dust(TRAIL_ARGB, 1.3f),
            at.x, at.y, at.z,
            Math.max(1, Math.round(4f * density)), 0.3, 0.3, 0.3, 0.05);
    }

    @Override
    public void tick() {
        super.tick();
        int die = this.getDieTick();
        if (die > 0) {
            this.setDeltaMovement(Vec3.ZERO);
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                // Dissolve drizzle: the needles shed embers as they fade, nothing pops out.
                serverLevel.sendParticles(
                    com.dragonblockarcanedba.particle.DbaParticles.EMBER,
                    this.getX(), this.getY(), this.getZ(),
                    2, 0.15, 0.15, 0.15, 0.02);
                if (this.tickCount - die > 6) {
                    this.discard();
                }
            }
            return;
        }
        Vec3 motion = this.getDeltaMovement();
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if (this.tickCount > 28) {
                this.beginDying();
                return;
            }
            this.emitTrail(serverLevel);
            Vec3 start = this.position();
            Vec3 end = start.add(motion);
            BlockHitResult block = this.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (block.getType() != HitResult.Type.MISS) {
                // The shot ends on the block it struck, with a burst on the struck face.
                this.setPos(block.getLocation().x, block.getLocation().y, block.getLocation().z);
                this.emitHitBurst(serverLevel, block.getLocation());
                serverLevel.playSound(null, block.getLocation().x, block.getLocation().y, block.getLocation().z,
                    com.dragonblockarcanedba.sound.DbaSounds.TRIDENT_HIT, net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, 0.8f);
                this.beginDying();
                return;
            }
            AABB box = this.getBoundingBox().expandTowards(motion).inflate(0.45);
            List<LivingEntity> hits = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != this.getOwner() && !this.hitIds.contains(e.getId()));
            for (LivingEntity living : hits) {
                int pierceIndex = this.hitIds.size();
                this.hitIds.add(living.getId());
                Entity owner = this.getOwner();
                var source = com.dragonblockarcanedba.util.DbaDamage.hellfire(this, owner);
                float dealt = this.damage * PIERCE_SHARES[pierceIndex];
                living.hurtServer(serverLevel, source, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(dealt));
                // A prong hit marks the victim for the devil, the same mark the
                // swarm targeting applies, so the shot and the swarm speak one language.
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    com.dragonblockarcanedba.effect.DbaEffects.MARKED_BY_EVIL_HOLDER, 60, 0, false, false), owner);
                this.emitHitBurst(serverLevel, living.position().add(0, living.getBbHeight() * 0.5, 0));
                serverLevel.playSound(null, living.getX(), living.getY(), living.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.TRIDENT_HIT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.9f);
                if (this.hitIds.size() >= MAX_PIERCES) {
                    this.beginDying();
                    return;
                }
            }
        }
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
    }
}
