package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A single branched lightning strike (SPECTACLE-STANDARD point 4): position
 * this at the strike point, the renderer grows the bolt upward from the
 * synced seed, so every client draws the same trunk and forks. Short life
 * with an envelope flicker; it is a visual only, damage belongs to the
 * weapon ability that spawned it.
 */
public class BoltEntity extends Projectile {
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(BoltEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(BoltEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(BoltEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(BoltEntity.class, EntityDataSerializers.INT);

    public BoltEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** colorArgb is the theme color; the core always renders near white. */
    public BoltEntity(Level level, Vec3 strikePoint, int seed, int colorArgb, float length, int lifeTicks) {
        super(DbaEntities.BOLT, level);
        this.setPos(strikePoint.x, strikePoint.y, strikePoint.z);
        this.noPhysics = true;
        this.entityData.set(SEED, seed);
        this.entityData.set(COLOR, colorArgb);
        this.entityData.set(LENGTH, length);
        this.entityData.set(LIFE, Math.max(2, lifeTicks));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SEED, 0);
        builder.define(COLOR, 0xFF9FD8FF);
        builder.define(LENGTH, 16.0f);
        builder.define(LIFE, 8);
    }

    public int getSeed() { return this.entityData.get(SEED); }
    public int getColor() { return this.entityData.get(COLOR); }
    public float getLength() { return this.entityData.get(LENGTH); }
    public int getLife() { return this.entityData.get(LIFE); }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount >= getLife()) {
            this.discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Seed", getSeed());
        output.putInt("Color", getColor());
        output.putFloat("Length", getLength());
        output.putInt("Life", getLife());
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(SEED, input.getIntOr("Seed", 0));
        this.entityData.set(COLOR, input.getIntOr("Color", 0xFF9FD8FF));
        this.entityData.set(LENGTH, input.getFloatOr("Length", 16.0f));
        this.entityData.set(LIFE, input.getIntOr("Life", 8));
    }
}
