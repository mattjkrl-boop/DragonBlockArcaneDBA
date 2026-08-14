package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

public class DimensionalSlashEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> TILT = SynchedEntityData.defineId(DimensionalSlashEntity.class, EntityDataSerializers.BOOLEAN);
    private float damage = 100.0f;

    public DimensionalSlashEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    public DimensionalSlashEntity(Level level, LivingEntity owner, boolean tiltRight, float damage) {
        super(DbaEntities.DIMENSIONAL_SLASH, level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());
        this.entityData.set(TILT, tiltRight);
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TILT, false);
    }

    public boolean getTilt() {
        return this.entityData.get(TILT);
    }

    @Override
    public void tick() {
        super.tick();

        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
        }

        // this.checkInsideBlocks(); // Removed
        Vec3 vec3 = this.getDeltaMovement();
        double d = this.getX() + vec3.x;
        double e = this.getY() + vec3.y;
        double f = this.getZ() + vec3.z;

        this.setPos(d, e, f);
        
        if (this.tickCount > 100 && !this.level().isClientSide()) {
            this.discard(); // Time out after 5 seconds
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide()) {
            Entity hitEntity = result.getEntity();
            Entity owner = this.getOwner();
            if (hitEntity instanceof LivingEntity living) {
                living.hurtServer((net.minecraft.server.level.ServerLevel) this.level(), this.damageSources().mobProjectile(this, (LivingEntity) owner), damage);
                living.addEffect(new MobEffectInstance(DbaEffects.MELTING_HOLDER, 300, 1, false, true));
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }
}
