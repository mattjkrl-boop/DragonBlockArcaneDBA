package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.item.SpiritSwordItem;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Spirit Cannon Beam Entity — Physical 3D continuous geometric energy beam,
 * rotating helical drill spirals, orbital Ki ring nodes, and muzzle/terminus geometry.
 */
public class SpiritCannonBeamEntity extends Projectile {
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(SpiritCannonBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(SpiritCannonBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BEAM_Y_ROT = SynchedEntityData.defineId(SpiritCannonBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BEAM_X_ROT = SynchedEntityData.defineId(SpiritCannonBeamEntity.class, EntityDataSerializers.FLOAT);

    public static final float MAX_BEAM_RANGE = 32.0f;

    public SpiritCannonBeamEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SpiritCannonBeamEntity(Level level, LivingEntity caster) {
        super(DbaEntities.SPIRIT_CANNON_BEAM, level);
        this.setOwner(caster);
        this.noPhysics = true;
        if (caster != null) {
            this.entityData.set(CASTER_ID, caster.getId());
            Vec3 eye = caster.getEyePosition();
            this.setPos(eye.x, eye.y, eye.z);
            this.setYRot(caster.getYRot());
            this.setXRot(caster.getXRot());
            this.entityData.set(BEAM_Y_ROT, caster.getYRot());
            this.entityData.set(BEAM_X_ROT, caster.getXRot());
            this.entityData.set(LENGTH, MAX_BEAM_RANGE);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CASTER_ID, -1);
        builder.define(LENGTH, MAX_BEAM_RANGE);
        builder.define(BEAM_Y_ROT, 0.0f);
        builder.define(BEAM_X_ROT, 0.0f);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    public float getBeamLength() {
        return this.entityData.get(LENGTH);
    }

    public void setBeamLength(float length) {
        this.entityData.set(LENGTH, length);
    }

    public float getBeamYRot() {
        return this.entityData.get(BEAM_Y_ROT);
    }

    public float getBeamXRot() {
        return this.entityData.get(BEAM_X_ROT);
    }

    @Override
    public void tick() {
        super.tick();

        Entity caster = this.getOwner();
        if (caster == null && getCasterId() != -1) {
            caster = this.level().getEntity(getCasterId());
        }

        if (caster instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
            Vec3 eye = living.getEyePosition();
            this.setPos(eye.x, eye.y, eye.z);
            this.setYRot(living.getYRot());
            this.setXRot(living.getXRot());

            if (!this.level().isClientSide()) {
                this.entityData.set(BEAM_Y_ROT, living.getYRot());
                this.entityData.set(BEAM_X_ROT, living.getXRot());

                // Raycast against solid blocks to find physical beam contact point
                Vec3 look = living.getViewVector(1.0f);
                Vec3 end = eye.add(look.scale(MAX_BEAM_RANGE));
                BlockHitResult blockHit = this.level().clip(new ClipContext(
                    eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, living
                ));

                float currentLength = MAX_BEAM_RANGE;
                if (blockHit.getType() != HitResult.Type.MISS) {
                    currentLength = (float) eye.distanceTo(blockHit.getLocation());
                }
                this.setBeamLength(currentLength);

                if (living instanceof Player player) {
                    boolean isHolding = player.getMainHandItem().getItem() instanceof SpiritSwordItem ||
                                        player.getOffhandItem().getItem() instanceof SpiritSwordItem;
                    if (!isHolding || (this.tickCount > 3 && !player.isUsingItem())) {
                        this.discard();
                    }
                }
            }
        } else {
            if (!this.level().isClientSide()) {
                this.discard();
            }
        }
    }
}
