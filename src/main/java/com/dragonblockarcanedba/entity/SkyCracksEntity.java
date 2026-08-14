package com.dragonblockarcanedba.entity;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;

public class SkyCracksEntity extends Projectile {
    public int life = 100; // 5 seconds

    public SkyCracksEntity(EntityType<?> entityType, Level level) {
        super((EntityType<? extends Projectile>) entityType, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            this.life--;
            if (this.life <= 0) {
                this.discard();
            }
        }
    }
}
