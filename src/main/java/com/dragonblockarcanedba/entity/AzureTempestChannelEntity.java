package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.item.AzureDragonSwordItem;
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
 * Azure Tempest Channel Entity — Physical 3D swirling wind tunnel and storm aura during Call of the Tempest channeling.
 */
public class AzureTempestChannelEntity extends Projectile {
    private static final EntityDataAccessor<Float> CHARGE_RATIO = SynchedEntityData.defineId(AzureTempestChannelEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(AzureTempestChannelEntity.class, EntityDataSerializers.INT);

    public AzureTempestChannelEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public AzureTempestChannelEntity(Level level, LivingEntity caster) {
        super(DbaEntities.AZURE_TEMPEST_CHANNEL, level);
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
    }

    public float getChargeRatio() {
        return this.entityData.get(CHARGE_RATIO);
    }

    public void setChargeRatio(float ratio) {
        this.entityData.set(CHARGE_RATIO, ratio);
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
                    boolean isHolding = player.getMainHandItem().getItem() instanceof AzureDragonSwordItem ||
                                        player.getOffhandItem().getItem() instanceof AzureDragonSwordItem;
                    if (!isHolding || (this.tickCount > 5 && !player.isUsingItem())) {
                        this.discard();
                    }
                }
            } else {
                this.discard();
            }
        }
    }
}
