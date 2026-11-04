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
 * Z Stance Aura Entity — Physical 3D gravity field, inward dimensional distortion funnel,
 * vibrating Katchin weight monoliths, and contracting event horizon rings during Katchin Weight Stance (Right-Click).
 */
public class ZStanceAuraEntity extends Projectile {
    private static final EntityDataAccessor<Integer> HELD_TICKS = SynchedEntityData.defineId(ZStanceAuraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER_RATIO = SynchedEntityData.defineId(ZStanceAuraEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(ZStanceAuraEntity.class, EntityDataSerializers.INT);
    // Ticks since the fade out began, -1 while the stance is live. Synced so the
    // renderer can ease the field out instead of popping on discard.
    private static final EntityDataAccessor<Integer> FADE_TICKS = SynchedEntityData.defineId(ZStanceAuraEntity.class, EntityDataSerializers.INT);
    public static final int FADE_LENGTH = 8;

    private int idleTicksWithoutUpdate = 0;
    private boolean fading = false;

    public ZStanceAuraEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public ZStanceAuraEntity(Level level, LivingEntity caster) {
        super(DbaEntities.Z_STANCE_AURA, level);
        this.setOwner(caster);
        if (caster != null) {
            this.setPos(caster.getX(), caster.getY(), caster.getZ());
            this.entityData.set(CASTER_ID, caster.getId());
        }
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HELD_TICKS, 0);
        builder.define(POWER_RATIO, 0.0f);
        builder.define(CASTER_ID, -1);
        builder.define(FADE_TICKS, -1);
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

    public int getHeldTicks() {
        return this.entityData.get(HELD_TICKS);
    }

    public float getPowerRatio() {
        return this.entityData.get(POWER_RATIO);
    }

    public void updateStance(int ticks, float ratio) {
        this.entityData.set(HELD_TICKS, ticks);
        this.entityData.set(POWER_RATIO, ratio);
        this.idleTicksWithoutUpdate = 0;
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            // Fade out runs to completion even though the stance updates stopped.
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

            LivingEntity livingCaster = null;
            if (caster instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                livingCaster = living;
                this.setPos(living.getX(), living.getY(), living.getZ());

                if (living instanceof Player player) {
                    boolean isUsing = player.isUsingItem() && (
                        player.getUseItem().getItem() instanceof ZSwordItem
                    );
                    if (!isUsing) {
                        this.discard();
                        return;
                    }
                }
            } else {
                this.discard();
                return;
            }

            float power = getPowerRatio();

            // Spectacle pass: the stance calls itself a gravity suction field and
            // draws inward rings, but nothing was ever pulled. It is a real vortex
            // now: mobs inside the rim are dragged toward the holder every tick,
            // weight scaled, so the slam that follows has a gathered crowd in it.
            // No lift: Katchin weight pins down, it does not levitate.
            com.dragonblockarcanedba.util.VortexPhysics.applyVortex(serverLevel, this.position(),
                3.0 + power * 5.0, 3.0, livingCaster,
                0.06 + power * 0.10, 0.6, 0.0, 0.4f + power * 0.6f);

            // Layered particles in the standard three bands: Ki motes sinking at
            // the core, violet dust contracting on the rim band, void smoke wisps
            // outside. Replaces the old single type trickle of 3 smoke per 4 ticks.
            if (this.tickCount % 2 == 0) {
                float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
                if (density > 0f) {
                    double rim = 2.0 + power * 3.5;
                    var rand = serverLevel.getRandom();
                    // Core: bright motes pulled down into the singularity.
                    int core = Math.max(1, Math.round(2f * density));
                    for (int i = 0; i < core; i++) {
                        double a = rand.nextDouble() * Math.PI * 2.0;
                        double r = rim * 0.2 * rand.nextDouble();
                        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
                            this.getX() + Math.cos(a) * r, this.getY() + 0.6 + rand.nextDouble() * 1.2, this.getZ() + Math.sin(a) * r,
                            1, 0.02, -0.05, 0.02, 0.01);
                    }
                    // Mid: violet dust riding the contracting rim inward.
                    int mid = Math.max(1, Math.round((2f + power * 2f) * density));
                    var midDust = com.dragonblockarcanedba.util.LayeredParticles.dust(0x7A1FD6, 1.3f);
                    double contract = rim * (1.0 - (this.tickCount % 20) / 40.0);
                    for (int i = 0; i < mid; i++) {
                        double a = rand.nextDouble() * Math.PI * 2.0;
                        serverLevel.sendParticles(midDust,
                            this.getX() + Math.cos(a) * contract, this.getY() + 0.25, this.getZ() + Math.sin(a) * contract,
                            1, -Math.cos(a) * 0.03, 0.02, -Math.sin(a) * 0.03, 0.01);
                    }
                    // Outer: the old void smoke, kept as the wispy edge layer.
                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.VOID_SMOKE,
                        this.getX(), this.getY() + 0.4, this.getZ(),
                        Math.max(1, Math.round(3f * density)),
                        rim * 0.7, 0.7, rim * 0.7, 0.02);
                }
            }

            this.idleTicksWithoutUpdate++;
            // If no stance update received for 6 ticks, the stance ended without
            // a release: fade out instead of popping.
            if (this.idleTicksWithoutUpdate > 6) {
                beginFade();
            }
        }
    }
}
