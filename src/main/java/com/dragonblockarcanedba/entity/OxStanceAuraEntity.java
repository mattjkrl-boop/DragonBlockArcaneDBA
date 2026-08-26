package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.item.OxKingsAxeItem;
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
 * Ox Stance Aura Entity — Physical 3D King's Colossal Aura, ethereal titan aegis avatar,
 * 12-block repulsion boundary disc, and swirling heat dome during Colossal Stance (Right-Click).
 */
public class OxStanceAuraEntity extends Projectile {
    private static final EntityDataAccessor<Integer> HELD_TICKS = SynchedEntityData.defineId(OxStanceAuraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_PEAK = SynchedEntityData.defineId(OxStanceAuraEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(OxStanceAuraEntity.class, EntityDataSerializers.INT);

    private int idleTicksWithoutUpdate = 0;

    public OxStanceAuraEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public OxStanceAuraEntity(Level level, LivingEntity caster) {
        super(DbaEntities.OX_STANCE_AURA, level);
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
        builder.define(IS_PEAK, false);
        builder.define(CASTER_ID, -1);
    }

    public int getHeldTicks() {
        return this.entityData.get(HELD_TICKS);
    }

    public boolean isPeak() {
        return this.entityData.get(IS_PEAK);
    }

    public void updateStance(int ticks, boolean peak) {
        this.entityData.set(HELD_TICKS, ticks);
        this.entityData.set(IS_PEAK, peak);
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
                        player.getUseItem().getItem() instanceof OxKingsAxeItem
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
