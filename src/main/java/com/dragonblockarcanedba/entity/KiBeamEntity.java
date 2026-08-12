package com.dragonblockarcanedba.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import net.minecraft.world.entity.projectile.Projectile;

public class KiBeamEntity extends Projectile implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(KiBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(KiBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(KiBeamEntity.class, EntityDataSerializers.INT);

    public KiBeamEntity(EntityType<?> entityType, Level level) {
        super((EntityType<? extends Projectile>) entityType, level);
    }

    public KiBeamEntity(Level level, Entity owner, float length, int color) {
        super(DbaEntities.KI_BEAM, level);
        this.entityData.set(OWNER_ID, owner.getId());
        this.entityData.set(LENGTH, length);
        this.entityData.set(COLOR, color);
        
        // Initial position setup
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
    public void tick() {
        super.tick();

        // Beam lasts for 20 ticks (1 second)
        if (this.tickCount > 20) {
            if (!this.level().isClientSide()) {
                this.discard();
            }
            return;
        }

        // Stick to the owner
        int ownerId = getOwnerId();
        if (ownerId != -1) {
            Entity owner = this.level().getEntity(ownerId);
            if (owner != null) {
                Vec3 start = owner.getEyePosition();
                // Lower it slightly so it doesn't block the camera entirely
                this.setPos(start.x, start.y - 0.2, start.z);
                this.setYRot(owner.getYRot());
                this.setXRot(owner.getXRot());
            }
        }
    }
}
