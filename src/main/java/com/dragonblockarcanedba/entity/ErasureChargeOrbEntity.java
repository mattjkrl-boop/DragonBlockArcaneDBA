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

                // Layered charge particles (spectacle pass): this orb is the
                // group's only real build-up, so its particles climb with the
                // charge level in three layers. Core motes churn inside the
                // orb, mid sparks gather inward on a tightening spiral while
                // drifting up, and an outer dust wisp layer rings the whole
                // apparatus. Past 0.75 charge the hot layer the renderer's
                // shell shift promises actually appears as embers.
                if (this.level() instanceof ServerLevel serverLevel) {
                    float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
                    if (density > 0f) {
                        float charge = getChargeRatio();
                        net.minecraft.util.RandomSource rand = serverLevel.getRandom();

                        // Core: motes inside the orb, denser as charge rises.
                        int coreCount = Math.max(1, Math.round((1f + charge * 2f) * density));
                        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
                            this.getX(), this.getY(), this.getZ(), coreCount,
                            0.20 + charge * 0.30, 0.20 + charge * 0.30, 0.20 + charge * 0.30, 0.02);

                        // Mid: gathering sparks, count and inward pull both
                        // scale with charge, emitted every tick now.
                        double gatherRadius = 0.9 - charge * 0.45;
                        double sparkAngle = rand.nextDouble() * Math.PI * 2.0;
                        int midCount = Math.max(1, Math.round((1f + charge * 4f) * density));
                        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                            this.getX() + Math.cos(sparkAngle) * gatherRadius,
                            this.getY() + (rand.nextDouble() - 0.5) * 0.6,
                            this.getZ() + Math.sin(sparkAngle) * gatherRadius,
                            midCount, 0.05, 0.10, 0.05, 0.02);

                        // Outer: cyan dust wisps at a wider radius, every 2 ticks.
                        if (this.tickCount % 2 == 0) {
                            double wispAngle = rand.nextDouble() * Math.PI * 2.0;
                            double wispRadius = 1.25 - charge * 0.35;
                            serverLevel.sendParticles(com.dragonblockarcanedba.util.LayeredParticles.dust(0xFF37E6FF, 1.4f),
                                this.getX() + Math.cos(wispAngle) * wispRadius,
                                this.getY() + (rand.nextDouble() - 0.5) * 0.8,
                                this.getZ() + Math.sin(wispAngle) * wispRadius,
                                Math.max(1, Math.round(2f * density)), 0.06, 0.06, 0.06, 0.01);
                        }

                        // Hot layer: embers once the renderer shell goes hot.
                        if (charge >= 0.75f && this.tickCount % 3 == 0) {
                            serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.EMBER,
                                this.getX(), this.getY(), this.getZ(),
                                Math.max(1, Math.round(2f * density)), 0.25, 0.25, 0.25, 0.04);
                        }
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
