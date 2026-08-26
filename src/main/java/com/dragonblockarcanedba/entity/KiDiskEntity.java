package com.dragonblockarcanedba.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.DustParticleOptions;

public class KiDiskEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(KiDiskEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(KiDiskEntity.class, EntityDataSerializers.FLOAT);

    public KiDiskEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xFFFFFF);
        builder.define(DAMAGE, 10.0f);
    }

    public void setColor(int color) { this.entityData.set(COLOR, color); }
    public int getColor() { return this.entityData.get(COLOR); }

    public void setDamage(float damage) { this.entityData.set(DAMAGE, damage); }
    public float getDamage() { return this.entityData.get(DAMAGE); }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.tickCount > 1) {
            this.level().addParticle(new DustParticleOptions(getColor(), 1.0f),
                    this.getX(), this.getY(), this.getZ(),
                    0, 0, 0);
        }
        if (this.tickCount > 200) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide()) {
            if (result.getEntity() instanceof LivingEntity target) {
                target.hurtServer((net.minecraft.server.level.ServerLevel) this.level(), 
                        this.damageSources().indirectMagic(this, this.getOwner()), getDamage());
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 1.5f);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.8f);
            }
        }
        // Destructo Disk DOES NOT discard on hit entity! It keeps going!
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide()) {
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(new DustParticleOptions(getColor(), 2.0f), 
                    this.getX(), this.getY(), this.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
            }
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.6f);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.ITEM_BREAK.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.2f);
            this.discard();
        }
    }
}
