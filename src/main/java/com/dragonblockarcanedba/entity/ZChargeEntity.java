package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.item.ZSwordItem;
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
 * Z Charge Entity — Physical 3D geometric golden vortex, counter-rotating celestial energy rings,
 * levitating sacred golden prisms, and radiating divine ground mandala during Z Sword Left-Click charging.
 */
public class ZChargeEntity extends Projectile {
    private static final EntityDataAccessor<Float> CHARGE_RATIO = SynchedEntityData.defineId(ZChargeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(ZChargeEntity.class, EntityDataSerializers.INT);
    // Ticks since the fade out began, -1 while the charge is live. Synced so the
    // renderer can ease the whole construct out instead of popping on discard.
    private static final EntityDataAccessor<Integer> FADE_TICKS = SynchedEntityData.defineId(ZChargeEntity.class, EntityDataSerializers.INT);
    public static final int FADE_LENGTH = 8;

    private int idleTicksWithoutUpdate = 0;
    private boolean fading = false;

    public ZChargeEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public ZChargeEntity(Level level, LivingEntity caster) {
        super(DbaEntities.Z_CHARGE, level);
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
        builder.define(FADE_TICKS, -1);
    }

    public float getChargeRatio() {
        return this.entityData.get(CHARGE_RATIO);
    }

    public int getFadeTicks() {
        return this.entityData.get(FADE_TICKS);
    }

    /** Start the fade out; the entity discards itself FADE_LENGTH ticks later. */
    public void beginFade() {
        if (!this.fading) {
            this.fading = true;
            this.entityData.set(FADE_TICKS, 0);
        }
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
            // Fade out runs to completion even though the charge updates stopped.
            if (this.fading) {
                int ft = getFadeTicks() + 1;
                this.entityData.set(FADE_TICKS, ft);
                if (ft >= FADE_LENGTH) {
                    this.discard();
                }
                return;
            }

            Entity caster = this.getOwner();
            if (caster == null && getCasterId() != -1) {
                caster = serverLevel.getEntity(getCasterId());
            }

            if (caster instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                this.setPos(living.getX(), living.getY(), living.getZ());

                if (living instanceof Player player) {
                    boolean isHolding = player.getMainHandItem().getItem() instanceof ZSwordItem ||
                                        player.getOffhandItem().getItem() instanceof ZSwordItem;
                    if (!isHolding) {
                        this.discard();
                        return;
                    }
                }
            } else {
                this.discard();
                return;
            }

            // Spectacle pass: the charge used to be mesh only with zero particles.
            // Layered emission in the standard three bands: bright Ki motes in the
            // core, gold dust on a helical mid band, sparse sparks at the wispy rim.
            float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
            if (density > 0f) {
                float charge = getChargeRatio();
                double radius = 1.2 + charge * 2.2;
                double height = 2.4 + charge * 1.4;
                var rand = serverLevel.getRandom();
                // Core: dense bright motes spiralling up the middle.
                int core = Math.max(1, Math.round((1f + charge * 2f) * density));
                for (int i = 0; i < core; i++) {
                    double a = rand.nextDouble() * Math.PI * 2.0;
                    double r = radius * 0.18 * rand.nextDouble();
                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
                        this.getX() + Math.cos(a) * r, this.getY() + rand.nextDouble() * height, this.getZ() + Math.sin(a) * r,
                        1, 0.02, 0.06, 0.02, 0.01);
                }
                // Mid: gold dust on the funnel band, the layer that reads at range.
                int mid = Math.max(1, Math.round((2f + charge * 3f) * density));
                var midDust = com.dragonblockarcanedba.util.LayeredParticles.dust(0xFFD433, 1.3f);
                for (int i = 0; i < mid; i++) {
                    double a = rand.nextDouble() * Math.PI * 2.0 + this.tickCount * 0.15;
                    double r = radius * (0.55 + 0.25 * rand.nextDouble());
                    serverLevel.sendParticles(midDust,
                        this.getX() + Math.cos(a) * r, this.getY() + rand.nextDouble() * height, this.getZ() + Math.sin(a) * r,
                        1, 0.03, 0.05, 0.03, 0.01);
                }
                // Outer: sparse sparks so the rim dissolves instead of ending hard.
                if (this.tickCount % 2 == 0) {
                    int outer = Math.max(1, Math.round(2f * density));
                    for (int i = 0; i < outer; i++) {
                        double a = rand.nextDouble() * Math.PI * 2.0;
                        double r = radius * (0.9 + 0.3 * rand.nextDouble());
                        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                            this.getX() + Math.cos(a) * r, this.getY() + rand.nextDouble() * height * 0.8, this.getZ() + Math.sin(a) * r,
                            1, 0.05, 0.03, 0.05, 0.01);
                    }
                }
            }

            this.idleTicksWithoutUpdate++;
            // If no charge update received for 6 ticks (0.3s), the caster stopped
            // charging without a release: fade out instead of popping.
            if (this.idleTicksWithoutUpdate > 6) {
                beginFade();
            }
        }
    }
}
