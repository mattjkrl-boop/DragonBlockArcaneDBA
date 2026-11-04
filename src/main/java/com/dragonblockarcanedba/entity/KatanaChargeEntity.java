package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.item.KatanaItem;
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
 * Katana Charge Entity: physical 3D iaido drawing stance rings, hilt focal spark matrix,
 * and converging crystalline silver motes during Katana Flashdraw charge-up.
 */
public class KatanaChargeEntity extends Projectile {
    private static final EntityDataAccessor<Float> CHARGE_RATIO = SynchedEntityData.defineId(KatanaChargeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(KatanaChargeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FADE_START = SynchedEntityData.defineId(KatanaChargeEntity.class, EntityDataSerializers.INT);

    /** Ticks the stance takes to dissolve once the charge ends or is cancelled. */
    public static final int FADE_TICKS = 6;

    private int idleTicksWithoutUpdate = 0;

    public KatanaChargeEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public KatanaChargeEntity(Level level, LivingEntity caster) {
        super(DbaEntities.KATANA_CHARGE, level);
        this.setOwner(caster);
        if (caster != null) {
            this.setPos(caster.getX(), caster.getY(), caster.getZ());
            this.setYRot(caster.getYRot());
            this.setXRot(caster.getXRot());
            this.entityData.set(CASTER_ID, caster.getId());
        }
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CHARGE_RATIO, 0.0f);
        builder.define(CASTER_ID, -1);
        builder.define(FADE_START, -1);
    }

    /** Tick the fade out began, or -1 while the charge is live. */
    public int getFadeStart() {
        return this.entityData.get(FADE_START);
    }

    /** Starts the dissolve: the stance stops following the caster and expires after FADE_TICKS. */
    public void beginFadeOut() {
        if (getFadeStart() < 0) {
            this.entityData.set(FADE_START, this.tickCount);
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
            int fadeStart = getFadeStart();
            if (fadeStart >= 0) {
                // Dissolving where the stance ended: theme drizzle, then expire.
                com.dragonblockarcanedba.util.LayeredParticles.emitRain(serverLevel,
                    this.position(), 1.2f, 1.4f, this.tickCount - fadeStart, FADE_TICKS,
                    com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE);
                if (this.tickCount >= fadeStart + FADE_TICKS) {
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
                this.setYRot(living.getYRot());
                this.setXRot(living.getXRot());

                if (living instanceof Player player) {
                    boolean isHolding = player.getMainHandItem().getItem() instanceof KatanaItem ||
                                        player.getOffhandItem().getItem() instanceof KatanaItem;
                    if (!isHolding) {
                        this.discard();
                        return;
                    }
                }
            } else {
                this.discard();
                return;
            }

            // Dust ring at the feet as the stance settles in.
            if (this.tickCount == 2) {
                com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel,
                    this.position(), 1.0f, 0x9FE8FF);
            }
            emitChargeParticles(serverLevel);

            this.idleTicksWithoutUpdate++;
            // If no charge update received for 6 ticks (0.3s), caster stopped charging
            if (this.idleTicksWithoutUpdate > 6) {
                this.discard();
            }
        }
    }

    /**
     * Three particle layers around the stance (standard point 1): bright core
     * motes rising through the middle, a pale cyan mid ring in the theme
     * color, and sparse outer spark wisps. Counts scale with the lifecycle
     * build up and the charge ratio.
     */
    private void emitChargeParticles(ServerLevel level) {
        float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
        if (density <= 0f) return;
        float charge = getChargeRatio();
        float build = com.dragonblockarcanedba.util.FxEnvelope.smoothstep(
            Math.min(1f, this.tickCount / 20f));
        float intensity = build * (0.45f + 0.55f * charge) * density;
        if (intensity <= 0f) return;
        net.minecraft.util.RandomSource rand = this.getRandom();
        double cx = this.getX(), cy = this.getY(), cz = this.getZ();

        // Core: bright motes rising through the stance.
        if (rand.nextFloat() < intensity) {
            level.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
                cx + (rand.nextDouble() - 0.5) * 0.4, cy + rand.nextDouble() * 1.3, cz + (rand.nextDouble() - 0.5) * 0.4,
                1, 0.01, 0.05, 0.01, 0.01);
        }
        // Mid: pale cyan dust orbiting at ring radius.
        if (rand.nextFloat() < intensity) {
            double ang = this.tickCount * 0.35 + rand.nextDouble() * 0.6;
            double r = 0.9 + charge * 0.4;
            level.sendParticles(com.dragonblockarcanedba.util.LayeredParticles.dust(0x9FE8FF, 1.2f),
                cx + Math.cos(ang) * r, cy + 0.15 + rand.nextDouble() * 0.5, cz + Math.sin(ang) * r,
                1, 0.02, 0.03, 0.02, 0.01);
        }
        // Outer: a stray spark wisp at the rim every few ticks.
        if (this.tickCount % 3 == 0 && rand.nextFloat() < intensity) {
            double ang = rand.nextDouble() * Math.PI * 2.0;
            level.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                cx + Math.cos(ang) * 1.35, cy + 0.3 + rand.nextDouble() * 0.8, cz + Math.sin(ang) * 1.35,
                1, 0.03, 0.02, 0.03, 0.01);
        }
    }
}
