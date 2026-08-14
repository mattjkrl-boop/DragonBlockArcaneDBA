package com.dragonblockarcanedba.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;

public class CurseLightningEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> IS_RARE = SynchedEntityData.defineId(CurseLightningEntity.class, EntityDataSerializers.BOOLEAN);
    
    public int life;

    public CurseLightningEntity(EntityType<? extends CurseLightningEntity> entityType, Level level) {
        super(entityType, level);
        this.life = 20; // 1 second
    }

    public void setRare(boolean isRare) {
        this.entityData.set(IS_RARE, isRare);
    }

    public boolean isRare() {
        return this.entityData.get(IS_RARE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IS_RARE, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.life > 0) {
            this.life--;
            if (this.life == 0 && !this.level().isClientSide()) {
                this.discard();
            }
        }
    }
}
