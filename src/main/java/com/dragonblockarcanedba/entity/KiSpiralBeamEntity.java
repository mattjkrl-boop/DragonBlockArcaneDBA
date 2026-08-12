package com.dragonblockarcanedba.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import com.geckolib.animatable.GeoEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

public class KiSpiralBeamEntity extends Projectile implements GeoEntity {
    private final com.geckolib.animatable.instance.AnimatableInstanceCache cache = com.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(KiSpiralBeamEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> LENGTH =
            SynchedEntityData.defineId(KiSpiralBeamEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(KiSpiralBeamEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    public KiSpiralBeamEntity(EntityType<?> entityType, Level level) {
        super((EntityType<? extends Projectile>) entityType, level);
    }

    public KiSpiralBeamEntity(Level level, Entity owner, float length, int color) {
        super(DbaEntities.KI_SPIRAL_BEAM, level);
        this.entityData.set(OWNER_ID, owner.getId());
        this.entityData.set(LENGTH, length);
        this.entityData.set(COLOR, color);
        Vec3 start = owner.getEyePosition();
        this.setPos(start.x, start.y, start.z);
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xFFFFFF);
        builder.define(LENGTH, 50.0f);
        builder.define(OWNER_ID, -1);
    }

    public int getColor() { return this.entityData.get(COLOR); }
    public float getLength() { return this.entityData.get(LENGTH); }
    public int getOwnerId() { return this.entityData.get(OWNER_ID); }

    @Override
    public void registerControllers(com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public com.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > 20 && !this.level().isClientSide()) {
            this.discard();
        }
        int ownerId = getOwnerId();
        if (ownerId != -1) {
            Entity owner = this.level().getEntity(ownerId);
            if (owner != null) {
                Vec3 start = owner.getEyePosition();
                this.setPos(start.x, start.y - 0.2, start.z);
                this.setYRot(owner.getYRot());
                this.setXRot(owner.getXRot());
            }
        }
    }
}
