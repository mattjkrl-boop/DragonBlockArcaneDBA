package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.item.BlasterGunItem;
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
import net.minecraft.world.phys.Vec3;

/**
 * Erasure Charge Orb Entity — Physical 3D expanding geometric energy orb model
 * that grows at the Blaster Gun muzzle during right-click charge-up.
 */
public class ErasureChargeOrbEntity extends Projectile {
    private static final EntityDataAccessor<Float> CHARGE_RATIO = SynchedEntityData.defineId(ErasureChargeOrbEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(ErasureChargeOrbEntity.class, EntityDataSerializers.INT);

    private int idleTicksWithoutUpdate = 0;

    public ErasureChargeOrbEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public ErasureChargeOrbEntity(Level level, LivingEntity caster) {
        super(DbaEntities.ERASURE_CHARGE_ORB, level);
        this.setOwner(caster);
        this.noPhysics = true;
        if (caster != null) {
            this.entityData.set(CASTER_ID, caster.getId());
            Vec3 eye = caster.getEyePosition();
            Vec3 look = caster.getLookAngle();
            Vec3 muzzle = eye.add(look.scale(1.2));
            this.setPos(muzzle.x, muzzle.y, muzzle.z);
            this.setYRot(caster.getYRot());
            this.setXRot(caster.getXRot());
        }
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

        Entity caster = this.getOwner();
        if (caster == null && getCasterId() != -1 && this.level() instanceof ServerLevel serverLevel) {
            caster = serverLevel.getEntity(getCasterId());
        }

        if (caster instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
            Vec3 eye = living.getEyePosition();
            Vec3 look = living.getLookAngle();
            Vec3 muzzle = eye.add(look.scale(1.2));
            this.setPos(muzzle.x, muzzle.y, muzzle.z);
            this.setYRot(living.getYRot());
            this.setXRot(living.getXRot());

            if (!this.level().isClientSide()) {
                if (living instanceof Player player) {
                    boolean isHolding = player.getMainHandItem().getItem() instanceof BlasterGunItem ||
                                        player.getOffhandItem().getItem() instanceof BlasterGunItem;
                    if (!isHolding || !player.isUsingItem()) {
                        this.discard();
                        return;
                    }
                }

                this.idleTicksWithoutUpdate++;
                if (this.idleTicksWithoutUpdate > 6) {
                    this.discard();
                }
            }
        } else {
            if (!this.level().isClientSide()) {
                this.discard();
            }
        }
    }
}
