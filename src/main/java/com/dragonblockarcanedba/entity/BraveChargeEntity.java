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
 * Brave Charge Entity - tracks the caster and the live charge ratio while the
 * Brave Sword right click is held. All of the visuals (heroic focus runes,
 * converging valor shards, heroic core matrix) live in BraveChargeRenderer;
 * this class owns position, charge state, and the end of the lifecycle: when
 * charging stops for any reason the entity enters a 10 tick fade out driven
 * by the synced FADE factor instead of vanishing at full alpha.
 */
public class BraveChargeEntity extends Projectile {
    private static final EntityDataAccessor<Float> CHARGE_RATIO = SynchedEntityData.defineId(BraveChargeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(BraveChargeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> FADE = SynchedEntityData.defineId(BraveChargeEntity.class, EntityDataSerializers.FLOAT);

    public static final int FADE_TICKS = 10;

    private int idleTicksWithoutUpdate = 0;
    private int fadeTicksRemaining = -1;

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
        builder.define(FADE, 1.0f);
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

    /** 1.0 while charging, falling to 0 across the fade out. Read by the renderer. */
    public float getFadeFactor() {
        return this.entityData.get(FADE);
    }

    public boolean isFading() {
        return this.fadeTicksRemaining >= 0;
    }

    /** Begin the fade out. The construct stays where it is and dissolves. */
    public void startFade() {
        if (this.fadeTicksRemaining < 0) {
            this.fadeTicksRemaining = FADE_TICKS;
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            if (isFading()) {
                this.fadeTicksRemaining--;
                this.entityData.set(FADE, Math.max(0.0f, this.fadeTicksRemaining / (float) FADE_TICKS));
                if (this.fadeTicksRemaining <= 0) {
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
                    boolean isHolding = player.getMainHandItem().getItem() instanceof BraveSwordItem ||
                                        player.getOffhandItem().getItem() instanceof BraveSwordItem;
                    if (!isHolding) {
                        startFade();
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
                startFade();
            }
        }
    }
}
