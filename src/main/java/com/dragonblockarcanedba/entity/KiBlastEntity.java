package com.dragonblockarcanedba.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class KiBlastEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.FLOAT);

    public KiBlastEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xFFFFFF);
        builder.define(DAMAGE, 10.0f);
    }

    public void setColor(int color) {
        this.entityData.set(COLOR, color);
    }

    public int getColor() {
        return this.entityData.get(COLOR);
    }

    public void setDamage(float damage) {
        this.entityData.set(DAMAGE, damage);
    }

    public float getDamage() {
        return this.entityData.get(DAMAGE);
    }

    @Override
    public void tick() {
        super.tick();
        
        // Spawn trail particles on client side
        if (this.level().isClientSide() && this.tickCount > 1) {
            int c = getColor();
            this.level().addParticle(new DustParticleOptions(c, 1.0f),
                    this.getX(), this.getY(), this.getZ(),
                    0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide()) {
            if (result.getEntity() instanceof LivingEntity target) {
                target.hurtServer((net.minecraft.server.level.ServerLevel) this.level(), 
                        this.damageSources().indirectMagic(this, this.getOwner()), getDamage());
            }
            explode();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide()) {
            explode();
        }
    }

    private void explode() {
        int c = getColor();
        // Spawn explosion particles
        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(new DustParticleOptions(c, 2.0f), 
                this.getX(), this.getY(), this.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
        }
        
        this.discard();
    }
}
