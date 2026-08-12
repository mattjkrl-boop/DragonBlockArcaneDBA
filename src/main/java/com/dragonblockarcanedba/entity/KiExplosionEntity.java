package com.dragonblockarcanedba.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.projectile.Projectile;

public class KiExplosionEntity extends Projectile {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(KiExplosionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(KiExplosionEntity.class, EntityDataSerializers.FLOAT);

    public KiExplosionEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xFFFFFF);
        builder.define(RADIUS, 1.0f);
    }

    public void setColor(int color) { this.entityData.set(COLOR, color); }
    public int getColor() { return this.entityData.get(COLOR); }

    public void setRadius(float radius) { this.entityData.set(RADIUS, radius); }
    public float getRadius() { return this.entityData.get(RADIUS); }

    @Override
    public void tick() {
        super.tick();
        // Explosions last for 40 ticks, fading out and expanding
        if (this.tickCount > 40 && !this.level().isClientSide()) {
            this.discard();
        }
    }
}
