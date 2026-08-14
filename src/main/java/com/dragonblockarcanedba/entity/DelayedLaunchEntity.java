package com.dragonblockarcanedba.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.projectile.Projectile;

public class DelayedLaunchEntity extends Projectile {
    private final List<LivingEntity> targets = new ArrayList<>();
    private Vec3 centerPos = Vec3.ZERO;
    private float powerRatio = 1.0f;
    private int delayTicks = 40;

    public DelayedLaunchEntity(EntityType<?> entityType, Level level) {
        super((EntityType<? extends Projectile>) entityType, level);
        this.noPhysics = true;
    }

    public DelayedLaunchEntity(Level level, Vec3 center, float powerRatio, List<LivingEntity> targets) {
        super(DbaEntities.DELAYED_LAUNCH, level);
        this.setPos(center.x, center.y, center.z);
        this.centerPos = center;
        this.powerRatio = powerRatio;
        this.targets.addAll(targets);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) return;

        if (this.tickCount >= this.delayTicks) {
            for (LivingEntity target : targets) {
                if (target != null && target.isAlive()) {
                    Vec3 launchDir = target.position().subtract(this.centerPos).normalize().scale(1.2 + powerRatio * 1.8);
                    target.setDeltaMovement(target.getDeltaMovement().add(launchDir.x, 1.0 + powerRatio * 0.8, launchDir.z));
                    target.hurtMarked = true;
                }
            }
            this.discard();
        }
    }
}
