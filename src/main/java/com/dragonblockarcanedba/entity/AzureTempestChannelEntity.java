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
 * Azure Tempest Channel Entity: Physical 3D swirling wind tunnel and storm aura during Call of the Tempest channeling.
 *
 * Spectacle pass: the channel emits the shared layered vortex particles,
 * tugs nearby mobs toward the caster as the charge builds, and no longer
 * pops out in one tick on release. beginRelease() starts an 8 tick collapse
 * that the renderer fades with, then the entity discards itself.
 */
public class AzureTempestChannelEntity extends Projectile {
    private static final EntityDataAccessor<Float> CHARGE_RATIO = SynchedEntityData.defineId(AzureTempestChannelEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(AzureTempestChannelEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RELEASING = SynchedEntityData.defineId(AzureTempestChannelEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> RELEASE_AGE = SynchedEntityData.defineId(AzureTempestChannelEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> SEED = SynchedEntityData.defineId(AzureTempestChannelEntity.class, EntityDataSerializers.LONG);

    /** Ticks the collapse fade runs after beginRelease(). */
    public static final int RELEASE_TICKS = 8;

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
        // Renderer shape seed: fixed at spawn and synced. The channel is
        // transient (it lives only while the caster channels), so it is not
        // persisted like the longer lived Azure entities.
        this.entityData.set(SEED, this.getUUID().getMostSignificantBits() ^ this.getUUID().getLeastSignificantBits());
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CHARGE_RATIO, 0.0f);
        builder.define(CASTER_ID, -1);
        builder.define(RELEASING, false);
        builder.define(RELEASE_AGE, 0);
        builder.define(SEED, 0L);
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

    public boolean isReleasing() {
        return this.entityData.get(RELEASING);
    }

    public int getReleaseAge() {
        return this.entityData.get(RELEASE_AGE);
    }

    public long getRenderSeed() {
        return this.entityData.get(SEED);
    }

    /** Start the collapse: the renderer fades over RELEASE_TICKS, then the entity discards. */
    public void beginRelease() {
        if (!isReleasing()) {
            this.entityData.set(RELEASING, true);
            this.entityData.set(RELEASE_AGE, 0);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            Entity caster = this.getOwner();
            if (caster == null && getCasterId() != -1) {
                caster = serverLevel.getEntity(getCasterId());
            }

            // Collapse path: count the fade, then leave. The storm the caster
            // just summoned takes over the sky from here.
            if (isReleasing()) {
                int next = getReleaseAge() + 1;
                this.entityData.set(RELEASE_AGE, next);
                if (caster instanceof LivingEntity living && living.isAlive()) {
                    this.setPos(living.getX(), living.getY(), living.getZ());
                }
                if (next >= RELEASE_TICKS) {
                    this.discard();
                }
                return;
            }

            if (caster instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                this.setPos(living.getX(), living.getY(), living.getZ());
                float charge = getChargeRatio();

                // Point 1, layered particles: the shared vortex emitter at the
                // renderer's tunnel size, replacing the single flat WIND puff.
                // A fixed mid life age keeps the envelope at full while the
                // channel runs; the charge drives radius and height instead.
                com.dragonblockarcanedba.util.LayeredParticles.emitVortex(serverLevel,
                    living.position(), 1.2f + charge * 2.0f, 3.2f + charge * 2.4f,
                    50.0f, 100.0f, AzureStormEntity.AZURE_ARGB,
                    com.dragonblockarcanedba.particle.DbaParticles.WIND);

                // Point 3, the gathering storm tugs at what stands nearby,
                // harder as the charge builds. No damage while channeling.
                if (charge > 0.3f && this.tickCount % 2 == 0) {
                    com.dragonblockarcanedba.util.VortexPhysics.applyVortex(serverLevel,
                        living.position(), 2.5 + charge * 3.5, 4.0, living,
                        0.04 + 0.10 * charge, 1.2, 0.05 + 0.15 * charge, charge);
                }

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
