package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class KiBlastEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.INT);

    public KiBlastEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
        this.entityData.set(SEED, level.getRandom().nextInt());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xFFFFFF);
        builder.define(DAMAGE, 10.0f);
        builder.define(SCALE, 1.0f);
        builder.define(SEED, 0);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
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

    public void setScale(float scale) {
        if (scale <= 0f || Float.isNaN(scale)) {
            scale = 1.0f;
        }
        this.entityData.set(SCALE, scale);
        this.refreshDimensions();
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        EntityDimensions base = super.getDimensions(pose);
        float scale = this.getScale();
        if (scale == 1.0f) {
            return base;
        }
        return base.scale(scale);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Theme particles (redesign T5): a sparse spark trail so the bolt's
        // path reads in flight; shared by every weapon that fires this bolt.
        if (!this.level().isClientSide() && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
            if (density > 0f) {
                float scale = this.getScale();
                serverLevel.sendParticles(
                    com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                    this.getX(), this.getY(), this.getZ(),
                    Math.max(1, Math.round(1f * density)),
                    0.08 * scale, 0.08 * scale, 0.08 * scale, 0.01);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide()) {
            if (result.getEntity() instanceof LivingEntity target) {
                target.hurtServer((net.minecraft.server.level.ServerLevel) this.level(),
                        com.dragonblockarcanedba.util.DbaDamage.ki(this, this.getOwner()), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (getDamage())));
            }
            explode();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide()) {
            // A charged blast (fired above the default technique size,
            // the only charge signal this entity carries) dents the ground
            // it strikes: a small crater, radius 1.5, one deep, WEAK,
            // cap 40. Plain blasts stay terrain-free.
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                && this.getScale() > 1.0f) {
                net.minecraft.core.BlockPos struck = result.getBlockPos();
                net.minecraft.core.BlockPos air = com.dragonblockarcanedba.util.PermanentDestruction.findSurface(
                    serverLevel, struck.getX(), struck.getZ(), struck.getY() + 1);
                net.minecraft.core.BlockPos center = air != null ? air.below() : struck;
                com.dragonblockarcanedba.util.PermanentDestruction.crater(serverLevel, center,
                    1.5, 0.5, false,
                    com.dragonblockarcanedba.util.PermanentDestruction.TIER_WEAK, 40, serverLevel.getRandom());
            }
            explode();
        }
    }

    private void explode() {
        if (!this.level().isClientSide()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.BOOM_BIG, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.4f);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.LIGHTNING_ZAP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.6f);
            // Impact burst (redesign T5): the hit used to be sound only.
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
                if (density > 0f) {
                    float scale = this.getScale();
                    serverLevel.sendParticles(
                        com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                        this.getX(), this.getY(), this.getZ(),
                        Math.max(2, Math.round(10f * density)),
                        0.3 * scale, 0.3 * scale, 0.3 * scale, 0.12);
                }
            }
        }
        this.discard();
    }
}
