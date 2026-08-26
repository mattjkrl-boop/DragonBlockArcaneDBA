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

    private int idleTicksWithoutUpdate = 0;

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
            Entity caster = this.getOwner();
            if (caster == null && getCasterId() != -1) {
                caster = serverLevel.getEntity(getCasterId());
            }

            if (caster instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
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

            this.idleTicksWithoutUpdate++;
            // If no stance update received for 6 ticks, stance ended
            if (this.idleTicksWithoutUpdate > 6) {
                this.discard();
            }
        }
    }
}
