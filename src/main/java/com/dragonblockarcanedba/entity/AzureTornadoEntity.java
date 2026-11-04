package com.dragonblockarcanedba.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Azure Tornado Entity: Swirling dragon wind vortex that lifts and spins enemies.
 *
 * Spectacle pass: the funnel runs on the shared blocks. VortexPhysics pulls
 * mobs in, lifts them at the core, and flings them out at release; a
 * DebrisSpawner field of real ground blocks orbits the funnel and rains back
 * down; LayeredParticles feed the three particle layers; the ground under
 * the funnel gets torn (top blocks stripped permanently, WEAK tier).
 * maxLifetime is persisted: a restored giant used to keep the 60 tick field
 * default while its renderer faded against 200, so it popped mid fade.
 */
public class AzureTornadoEntity extends Projectile {
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(AzureTornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_GIANT = SynchedEntityData.defineId(AzureTornadoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> SEED = SynchedEntityData.defineId(AzureTornadoEntity.class, EntityDataSerializers.LONG);

    /** Per tornado cap on stripped top blocks (destruction expansion). */
    private static final int TORN_CAP = 200;

    private int maxLifetime = 60; // 3s default for mini, 200 (10s) for giant
    private boolean debrisSpawned = false;
    private boolean flungOut = false;
    private int tornBlocks = 0;

    public AzureTornadoEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public AzureTornadoEntity(Level level, LivingEntity owner, Vec3 pos, float scale, boolean isGiant) {
        super(DbaEntities.AZURE_TORNADO, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setScale(scale);
        this.entityData.set(IS_GIANT, isGiant);
        // Renderer shape seed: fixed at spawn, synced and saved.
        this.entityData.set(SEED, this.getUUID().getMostSignificantBits() ^ this.getUUID().getLeastSignificantBits());
        this.maxLifetime = isGiant ? 200 : 60;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SCALE, 1.0f);
        builder.define(IS_GIANT, false);
        builder.define(SEED, 0L);
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("MaxLifetime", this.maxLifetime);
        output.putLong("RenderSeed", this.getRenderSeed());
        output.putBoolean("DebrisSpawned", this.debrisSpawned);
        output.putBoolean("FlungOut", this.flungOut);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        // Fall back to the giant flag for saves written before MaxLifetime existed.
        this.maxLifetime = input.getIntOr("MaxLifetime", this.isGiant() ? 200 : 60);
        this.entityData.set(SEED, input.getLongOr("RenderSeed", 0L));
        this.debrisSpawned = input.getBooleanOr("DebrisSpawned", false);
        this.flungOut = input.getBooleanOr("FlungOut", false);
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public void setScale(float scale) {
        this.entityData.set(SCALE, scale);
    }

    public boolean isGiant() {
        return this.entityData.get(IS_GIANT);
    }

    public int getMaxLifetime() {
        return this.maxLifetime;
    }

    public long getRenderSeed() {
        return this.entityData.get(SEED);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        float scale = getScale();
        boolean giant = isGiant();
        double radius = scale * (giant ? 8.0 : 3.0);
        double height = scale * (giant ? 16.0 : 6.0);
        Vec3 center = this.position();
        LivingEntity livingOwner = this.getOwner() instanceof LivingEntity ? (LivingEntity) this.getOwner() : null;
        float env = com.dragonblockarcanedba.util.FxEnvelope.envelope(this.tickCount, this.maxLifetime);

        // Point 3, base dust on touchdown.
        if (this.tickCount == 2) {
            com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel, center,
                (float) Math.min(radius, 10.0), AzureStormEntity.AZURE_ARGB);
        }

        // Point 2, debris: real ground blocks on helical orbits, sized by the
        // standard (24 pieces for the mini, 72 for the storm class giant).
        // Their orbit ends inside the decay window so they rain back down.
        if (!this.debrisSpawned && this.tickCount >= 3) {
            this.debrisSpawned = true;
            float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
            int count = Math.max(6, Math.round((giant ? 72f : 24f) * density));
            int orbitTicks = Math.max(15, this.maxLifetime - this.tickCount - Math.round(this.maxLifetime * 0.25f));
            com.dragonblockarcanedba.util.DebrisSpawner.spawnField(serverLevel, livingOwner, center,
                count, radius * 0.85, height * 0.7, orbitTicks);
        }

        // Point 3, forces through the shared vortex: inward pull, tangential
        // swirl, core lift, all weight classed and envelope scaled. The old
        // hand rolled version added its full lift every tick with no cap, so
        // victims accelerated upward without limit.
        com.dragonblockarcanedba.util.VortexPhysics.applyVortex(serverLevel, center,
            radius, height, livingOwner,
            giant ? 0.50 : 0.30, giant ? 1.5 : 1.3, giant ? 0.85 : 0.55, env);

        // Point 5, release: in the last 15 percent of life the funnel lets go
        // and throws everything it holds outward and up, once.
        if (!this.flungOut && this.tickCount >= this.maxLifetime - Math.round(this.maxLifetime * 0.15f)) {
            this.flungOut = true;
            AABB aoe = new AABB(center.x - radius, center.y - 1.0, center.z - radius,
                                center.x + radius, center.y + height, center.z + radius);
            List<LivingEntity> held = serverLevel.getEntitiesOfClass(LivingEntity.class, aoe,
                e -> e.isAlive() && e != this.getOwner());
            for (LivingEntity target : held) {
                com.dragonblockarcanedba.util.VortexPhysics.fling(target, center,
                    giant ? 1.7 : 1.1, giant ? 1.0 : 0.7);
            }
        }

        // Continuous wind damage, only inside the active window so nothing is
        // hurt while the funnel is still forming or already dissipating.
        if (this.tickCount % 10 == 0
            && com.dragonblockarcanedba.util.FxEnvelope.inActiveWindow(this.tickCount, this.maxLifetime,
                com.dragonblockarcanedba.util.FxEnvelope.STD_BUILD, com.dragonblockarcanedba.util.FxEnvelope.STD_DECAY)) {
            AABB aoe = new AABB(center.x - radius, center.y, center.z - radius,
                                center.x + radius, center.y + height, center.z + radius);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, aoe,
                e -> e.isAlive() && e != this.getOwner());
            net.minecraft.world.damagesource.DamageSource tornadoSource = livingOwner != null
                ? com.dragonblockarcanedba.util.DbaDamage.tempest(this, livingOwner)
                : com.dragonblockarcanedba.util.DbaDamage.tempest(this, null);
            for (LivingEntity target : targets) {
                target.hurtServer(serverLevel, tornadoSource,
                    com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (giant ? 60.0f : 25.0f)));
            }
        }

        // Point 3, ground tearing: the funnel strips the top block off the
        // columns under it (WEAK tier, drops on, capped per tornado life).
        // Surface height is read per column so slopes tear at their own
        // level, not at the funnel's base plane.
        if (this.tickCount % 10 == 0 && env > 0.3f) {
            scarGround(serverLevel, center, radius);
        }

        // Point 1, layered particles every tick, envelope scaled. No rain
        // emission from this entity: rain belongs to the AtmosphereClient
        // system (the giant tornado lives inside its storm's zone, where
        // that system already renders it).
        com.dragonblockarcanedba.util.LayeredParticles.emitVortex(serverLevel, center,
            (float) (radius * 0.8), (float) height, this.tickCount, this.maxLifetime,
            AzureStormEntity.AZURE_ARGB, com.dragonblockarcanedba.particle.DbaParticles.WIND);

        // Giant tornado telegraphed lightning at random ground points. The
        // owner cast is guarded now; it used to throw on any non living owner.
        if (giant && this.tickCount % 20 == 0
            && com.dragonblockarcanedba.util.FxEnvelope.inActiveWindow(this.tickCount, this.maxLifetime,
                com.dragonblockarcanedba.util.FxEnvelope.STD_BUILD, com.dragonblockarcanedba.util.FxEnvelope.STD_DECAY)) {
            double rx = center.x + (serverLevel.getRandom().nextDouble() - 0.5) * radius * 1.5;
            double rz = center.z + (serverLevel.getRandom().nextDouble() - 0.5) * radius * 1.5;
            double ry = serverLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) rx, (int) rz);
            AzureLightningEntity lightning = new AzureLightningEntity(serverLevel, livingOwner, rx, ry, rz, 350.0f);
            lightning.setTelegraphTicks(10);
            serverLevel.addFreshEntity(lightning);
        }

        // Howling wind sound
        if (this.tickCount % 20 == 0) {
            serverLevel.playSound(null, center.x, center.y, center.z,
                com.dragonblockarcanedba.sound.DbaSounds.FLIGHT_WIND, SoundSource.PLAYERS, 1.5f, 0.8f);
        }

        if (this.tickCount >= this.maxLifetime) {
            this.discard();
        }
    }

    /**
     * Strip the top block of each column in the inner disk: at WEAK tier
     * that is soil, sand, foliage, and plants, torn off with drops. The
     * strip is permanent and capped per tornado life so a long storm
     * cannot eat a whole hillside one column at a time forever.
     */
    private void scarGround(ServerLevel level, Vec3 center, double radius) {
        int r = Math.max(1, (int) Math.ceil(radius * 0.35));
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int cx = (int) Math.floor(center.x), cz = (int) Math.floor(center.z);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                if (this.tornBlocks >= TORN_CAP) return;
                if (level.getRandom().nextFloat() > 0.3f) continue;
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, cx + dx, cz + dz) - 1;
                m.set(cx + dx, y, cz + dz);
                if (com.dragonblockarcanedba.util.PermanentDestruction.destroy(level, m.immutable(), true,
                        com.dragonblockarcanedba.util.PermanentDestruction.TIER_WEAK)) {
                    this.tornBlocks++;
                }
            }
        }
    }
}
