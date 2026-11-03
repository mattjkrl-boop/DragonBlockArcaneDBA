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
 * This is not a ki blast. One hit is the whole trident, 750.
 */
public class TridentProngEntity extends Projectile {
    private static final EntityDataAccessor<Integer> DIE_TICK =
        SynchedEntityData.defineId(TridentProngEntity.class, EntityDataSerializers.INT);

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

    @Override
    public void tick() {
        super.tick();
        int die = this.getDieTick();
        if (die > 0) {
            this.setDeltaMovement(Vec3.ZERO);
            if (!this.level().isClientSide() && this.tickCount - die > 6) {
                this.discard();
            }
            return;
        }
        Vec3 motion = this.getDeltaMovement();
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if (this.tickCount > 28) {
                this.beginDying();
                return;
            }
            Vec3 start = this.position();
            Vec3 end = start.add(motion);
            BlockHitResult block = this.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (block.getType() != HitResult.Type.MISS) {
                this.beginDying();
                return;
            }
            AABB box = this.getBoundingBox().expandTowards(motion).inflate(0.45);
            List<LivingEntity> hits = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != this.getOwner() && !this.hitIds.contains(e.getId()));
            for (LivingEntity living : hits) {
                this.hitIds.add(living.getId());
                Entity owner = this.getOwner();
                var source = owner instanceof net.minecraft.world.entity.player.Player player
                    ? serverLevel.damageSources().playerAttack(player)
                    : serverLevel.damageSources().magic();
                living.hurtServer(serverLevel, source, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(this.damage));
                this.beginDying();
                return;
            }
        }
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
    }
}
