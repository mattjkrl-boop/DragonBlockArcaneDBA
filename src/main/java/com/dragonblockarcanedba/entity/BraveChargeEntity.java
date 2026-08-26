package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.item.BraveSwordItem;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * Brave Charge Entity — Physical 3D heroic focus runes, converging valor shards, and heroic core matrix during Brave Sword Right-Click charging.
 */
public class BraveChargeEntity extends Projectile {
    private static final EntityDataAccessor<Float> CHARGE_RATIO = SynchedEntityData.defineId(BraveChargeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(BraveChargeEntity.class, EntityDataSerializers.INT);

    private int idleTicksWithoutUpdate = 0;

    public BraveChargeEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BraveChargeEntity(Level level, LivingEntity caster) {
        super(DbaEntities.BRAVE_CHARGE, level);
        this.setOwner(caster);
        if (caster != null) {
            this.setPos(caster.getX(), caster.getY(), caster.getZ());
            this.entityData.set(CASTER_ID, caster.getId());
        }
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CHARGE_RATIO, 0.0f);
        builder.define(CASTER_ID, -1);
    }

    public float getChargeRatio() {
        return this.entityData.get(CHARGE_RATIO);
    }

    public void setChargeRatio(float ratio) {
        this.entityData.set(CHARGE_RATIO, ratio);
        this.idleTicksWithoutUpdate = 0;
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            Entity caster = this.getOwner();
            if (caster == null && getCasterId() != -1) {
                caster = serverLevel.getEntity(getCasterId());
            }

            if (caster instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                this.setPos(living.getX(), living.getY(), living.getZ());

                if (living instanceof Player player) {
                    boolean isHolding = player.getMainHandItem().getItem() instanceof BraveSwordItem ||
                                        player.getOffhandItem().getItem() instanceof BraveSwordItem;
                    if (!isHolding) {
                        this.discard();
                        return;
                    }
                }
            } else {
                this.discard();
                return;
            }

            this.idleTicksWithoutUpdate++;
            // If no charge update received for 6 ticks (0.3s), caster stopped charging
            if (this.idleTicksWithoutUpdate > 6) {
                this.discard();
            }
        }
    }
}
