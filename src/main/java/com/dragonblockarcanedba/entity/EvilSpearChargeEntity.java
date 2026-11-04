package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.item.EvilSpearItem;
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
 * Evil Spear Charge Entity — Physical 3D demonic crimson ground seals, contracting energy torus rings,
 * orbiting blood shards, and pulsating demonic core matrix during Evil Spear Left-Click charging.
 */
public class EvilSpearChargeEntity extends Projectile {
    private static final EntityDataAccessor<Float> CHARGE_RATIO = SynchedEntityData.defineId(EvilSpearChargeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(EvilSpearChargeEntity.class, EntityDataSerializers.INT);
    // Ticks since the throw released, or -1 while still charging. Synced so
    // the renderer can dissipate the charge visual instead of cutting it.
    private static final EntityDataAccessor<Integer> RELEASE_TICKS = SynchedEntityData.defineId(EvilSpearChargeEntity.class, EntityDataSerializers.INT);
    /** How long the released charge visual takes to dissipate. */
    public static final int RELEASE_FADE_TICKS = 6;

    private int idleTicksWithoutUpdate = 0;

    public EvilSpearChargeEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public EvilSpearChargeEntity(Level level, LivingEntity caster) {
        super(DbaEntities.EVIL_SPEAR_CHARGE, level);
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
        builder.define(RELEASE_TICKS, -1);
    }

    /** Ticks since release, or -1 while the charge is still being held. */
    public int getReleaseTicks() {
        return this.entityData.get(RELEASE_TICKS);
    }

    /**
     * The throw has released: stop following the caster and dissipate over
     * RELEASE_FADE_TICKS instead of vanishing the frame the spear leaves.
     */
    public void beginRelease() {
        if (this.entityData.get(RELEASE_TICKS) < 0) {
            this.entityData.set(RELEASE_TICKS, 0);
        }
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
            // Released: hold position, rain the gathered energy back down,
            // and discard when the renderer's dissipation window closes.
            int releaseTicks = getReleaseTicks();
            if (releaseTicks >= 0) {
                this.entityData.set(RELEASE_TICKS, releaseTicks + 1);
                com.dragonblockarcanedba.util.LayeredParticles.emitRain(serverLevel,
                    this.position().add(0, 0.1, 0), 1.2f + getChargeRatio(), 2.2f,
                    releaseTicks + 1, RELEASE_FADE_TICKS,
                    com.dragonblockarcanedba.particle.DbaParticles.EMBER);
                if (releaseTicks + 1 >= RELEASE_FADE_TICKS) {
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

                // Layered charge particles (standard point 1): a bright mote
                // core at the seal heart, the blood ember mid band spiralling
                // up as the charge gathers, and void smoke wisps at the rim.
                if (this.tickCount % 2 == 0) {
                    float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
                    if (density > 0f) {
                        float charge = getChargeRatio();
                        serverLevel.sendParticles(
                            com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
                            this.getX(), this.getY() + 0.9, this.getZ(),
                            Math.max(1, Math.round((1f + charge * 2f) * density)),
                            0.30, 0.30, 0.30, 0.02);
                        serverLevel.sendParticles(
                            com.dragonblockarcanedba.particle.DbaParticles.EMBER,
                            this.getX(), this.getY() + 0.3, this.getZ(),
                            Math.max(1, Math.round((2f + charge * 4f) * density)),
                            1.0 + charge * 1.2, 0.8, 1.0 + charge * 1.2, 0.04);
                        serverLevel.sendParticles(
                            com.dragonblockarcanedba.particle.DbaParticles.VOID_SMOKE,
                            this.getX(), this.getY() + 0.2, this.getZ(),
                            Math.max(1, Math.round((1f + charge * 2f) * density)),
                            1.4 + charge * 1.4, 0.5, 1.4 + charge * 1.4, 0.02);
                    }
                }

                if (living instanceof Player player) {
                    boolean isHolding = player.getMainHandItem().getItem() instanceof EvilSpearItem ||
                                        player.getOffhandItem().getItem() instanceof EvilSpearItem;
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
