package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Azure Storm Entity: Tempest Weather Domain from Azure Dragon Sword.
 *
 * Spectacle pass: the storm is a real atmosphere now. For its whole life it
 * drives server thunder weather plus client gloom through AtmosphereServer,
 * its lifetime scales with the charge that summoned it, its damage rides on
 * telegraphed AzureLightningEntity strikes (BoltEntity visuals), a debris
 * field orbits it and rains down as it dissipates, and a shared vortex gives
 * everything inside a directed pull, swirl, and lift instead of random jitter.
 * All of its state is saved: radius, follow mode, target, charge, lifetime,
 * and age, so a storm that survives a chunk unload comes back as itself.
 */
public class AzureStormEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(AzureStormEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> FOLLOWS_PLAYER = SynchedEntityData.defineId(AzureStormEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(AzureStormEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_MAX_CHARGED = SynchedEntityData.defineId(AzureStormEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LIFE_TICKS = SynchedEntityData.defineId(AzureStormEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CHARGE = SynchedEntityData.defineId(AzureStormEntity.class, EntityDataSerializers.FLOAT);

    /** Azure theme color for the layered particle emitters (r 0.31, g 0.78, b 1.0). */
    public static final int AZURE_ARGB = 0xFF4FC8FF;

    private boolean hasSpawnedGiantTornado = false;
    private boolean debrisSpawned = false;
    private int ageTicks = 0;
    private int nextStrikeTick = -1;
    private UUID targetUuid = null;

    public AzureStormEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public AzureStormEntity(Level level, LivingEntity owner, Vec3 pos, float radius, boolean followsPlayer,
                            LivingEntity target, boolean isMaxCharged, int lifeTicks, float chargeRatio) {
        super(DbaEntities.AZURE_STORM, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setRadius(radius);
        this.entityData.set(FOLLOWS_PLAYER, followsPlayer);
        this.entityData.set(TARGET_ID, target != null ? target.getId() : -1);
        this.entityData.set(IS_MAX_CHARGED, isMaxCharged);
        this.entityData.set(LIFE_TICKS, Math.max(20, lifeTicks));
        this.entityData.set(CHARGE, chargeRatio);
        this.targetUuid = target != null ? target.getUUID() : null;
    }

    public boolean isMaxCharged() {
        return this.entityData.get(IS_MAX_CHARGED);
    }

    public int getLifeTicks() {
        return this.entityData.get(LIFE_TICKS);
    }

    public float getChargeRatio() {
        return this.entityData.get(CHARGE);
    }

    public int getAgeTicks() {
        return this.ageTicks;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 12.0f);
        builder.define(FOLLOWS_PLAYER, true);
        builder.define(TARGET_ID, -1);
        builder.define(IS_MAX_CHARGED, false);
        builder.define(LIFE_TICKS, 220);
        builder.define(CHARGE, 0.0f);
    }

    // Persist ALL storm state. The old save wrote only the max charge flag and
    // the giant tornado flag, so a restored storm silently reverted to the
    // synched defaults: 12 block radius, following the player, no target, and
    // whatever lifetime the field happened to hold.
    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Radius", this.getRadius());
        output.putBoolean("FollowsPlayer", this.followsPlayer());
        output.putBoolean("IsMaxCharged", this.isMaxCharged());
        output.putBoolean("HasSpawnedGiantTornado", this.hasSpawnedGiantTornado);
        output.putBoolean("DebrisSpawned", this.debrisSpawned);
        output.putInt("LifeTicks", this.getLifeTicks());
        output.putFloat("Charge", this.getChargeRatio());
        output.putInt("AgeTicks", this.ageTicks);
        output.putBoolean("HasTarget", this.targetUuid != null);
        if (this.targetUuid != null) {
            output.putLong("TargetUuidMost", this.targetUuid.getMostSignificantBits());
            output.putLong("TargetUuidLeast", this.targetUuid.getLeastSignificantBits());
        }
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setRadius(input.getFloatOr("Radius", 12.0f));
        this.entityData.set(FOLLOWS_PLAYER, input.getBooleanOr("FollowsPlayer", true));
        this.entityData.set(IS_MAX_CHARGED, input.getBooleanOr("IsMaxCharged", false));
        this.hasSpawnedGiantTornado = input.getBooleanOr("HasSpawnedGiantTornado", false);
        this.debrisSpawned = input.getBooleanOr("DebrisSpawned", false);
        this.entityData.set(LIFE_TICKS, input.getIntOr("LifeTicks", 220));
        this.entityData.set(CHARGE, input.getFloatOr("Charge", 0.0f));
        this.ageTicks = input.getIntOr("AgeTicks", 0);
        if (input.getBooleanOr("HasTarget", false)) {
            this.targetUuid = new UUID(input.getLongOr("TargetUuidMost", 0L), input.getLongOr("TargetUuidLeast", 0L));
        }
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(RADIUS, radius);
    }

    public boolean followsPlayer() {
        return this.entityData.get(FOLLOWS_PLAYER);
    }

    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.ageTicks++;
        float radius = getRadius();
        int life = getLifeTicks();
        float charge = getChargeRatio();
        Entity owner = this.getOwner();
        LivingEntity livingOwner = owner instanceof LivingEntity ? (LivingEntity) owner : null;
        float env = com.dragonblockarcanedba.util.FxEnvelope.envelope(this.ageTicks, life);

        // Follow the locked target while it lives; if the target died or was
        // removed, fall back to following the owner instead of freezing at the
        // last position for the rest of the storm.
        Entity followTarget = null;
        if (!followsPlayer()) {
            if (getTargetId() != -1) {
                followTarget = serverLevel.getEntity(getTargetId());
            }
            if (followTarget == null && this.targetUuid != null) {
                followTarget = serverLevel.getEntity(this.targetUuid);
                if (followTarget != null) {
                    this.entityData.set(TARGET_ID, followTarget.getId());
                }
            }
            if (followTarget != null && !followTarget.isAlive()) {
                followTarget = null;
            }
        }
        if (followTarget != null) {
            this.setPos(followTarget.getX(), followTarget.getY(), followTarget.getZ());
        } else if (owner != null && owner.isAlive()) {
            this.setPos(owner.getX(), owner.getY(), owner.getZ());
        }

        Vec3 center = this.position();

        // Point 4, atmosphere: real thunder weather and gloom for the storm's
        // life, refreshed as the storm moves so the gloom center tracks it.
        if (this.ageTicks == 1 || this.ageTicks % 20 == 0) {
            int remaining = Math.max(20, life - this.ageTicks + 20);
            com.dragonblockarcanedba.util.AtmosphereServer.begin(serverLevel, center,
                radius + 24.0f, 0.7f,
                com.dragonblockarcanedba.util.AtmosphereServer.Kind.THUNDER, remaining);
        }

        // Point 3, base dust where the domain meets the ground.
        if (this.ageTicks == 2) {
            com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel, center,
                Math.min(radius, 12.0f), AZURE_ARGB);
        }

        // Point 2, debris: one field of real ground blocks lifted into helical
        // orbits as the storm reaches full strength. Their orbit length ends
        // inside the dissipation window, so they rain back down as it dies.
        int buildEnd = Math.max(2, Math.round(life * 0.2f));
        if (!this.debrisSpawned && this.ageTicks >= buildEnd) {
            this.debrisSpawned = true;
            float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
            int count = Math.max(8, Math.round((28f + 44f * charge) * density));
            int orbitTicks = Math.max(20, life - this.ageTicks - Math.round(life * 0.30f));
            com.dragonblockarcanedba.util.DebrisSpawner.spawnField(serverLevel, livingOwner, center,
                count, radius * 0.8, 7.0 + radius * 0.35, orbitTicks);
        }

        // Point 3, mob forces: a directed pull, swirl, and core lift through
        // the shared vortex, scaled by the lifecycle envelope. This replaces
        // the old random jitter that shook enemies without moving them anywhere.
        com.dragonblockarcanedba.util.VortexPhysics.applyVortex(serverLevel, center,
            radius, 14.0, livingOwner, 0.05 + 0.06 * charge, 1.3, 0.12, env);

        // Point 1, layered particles: core, mid, and outer bands scaled by the
        // same envelope the renderer fades with, plus dissipation drizzle.
        if (this.ageTicks % 2 == 0) {
            com.dragonblockarcanedba.util.LayeredParticles.emitVortex(serverLevel, center,
                radius * 0.6f, 9.0f + charge * 5.0f, this.ageTicks, life,
                AZURE_ARGB, com.dragonblockarcanedba.particle.DbaParticles.WIND);
            if (this.ageTicks > life * 0.65f) {
                com.dragonblockarcanedba.util.LayeredParticles.emitRain(serverLevel, center,
                    radius * 0.8f, 10.0f, this.ageTicks, life,
                    com.dragonblockarcanedba.particle.DbaParticles.WIND);
            }
        }

        // Giant tornado finisher at the storm's peak, once the build window
        // completes, instead of on the first tick at partial strength.
        if (this.isMaxCharged() && !this.hasSpawnedGiantTornado && this.ageTicks >= buildEnd) {
            this.hasSpawnedGiantTornado = true;
            AzureTornadoEntity giantTornado = new AzureTornadoEntity(serverLevel, livingOwner, center, 1.8f, true);
            serverLevel.addFreshEntity(giantTornado);
        }

        // Telegraph strikes: the cadence tightens at peak density. A strike
        // prefers a random enemy inside; an empty storm still strikes the
        // ground, so the weather reads even with nothing to hit.
        if (this.nextStrikeTick < 0) {
            this.nextStrikeTick = this.ageTicks + 14;
        }
        if (this.ageTicks >= this.nextStrikeTick
            && com.dragonblockarcanedba.util.FxEnvelope.inActiveWindow(this.ageTicks, life, 0.12f, 0.30f)) {
            AABB stormBox = this.getBoundingBox().inflate(radius, 12.0, radius);
            List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(LivingEntity.class, stormBox,
                e -> e.isAlive() && e != owner && center.distanceTo(e.position()) <= radius);
            double lx, lz;
            if (!enemies.isEmpty()) {
                LivingEntity picked = enemies.get(serverLevel.getRandom().nextInt(enemies.size()));
                lx = picked.getX();
                lz = picked.getZ();
            } else {
                double ang = serverLevel.getRandom().nextDouble() * Math.PI * 2.0;
                double rr = radius * 0.8 * Math.sqrt(serverLevel.getRandom().nextDouble());
                lx = center.x + Math.cos(ang) * rr;
                lz = center.z + Math.sin(ang) * rr;
            }
            double ly = serverLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) lx, (int) lz);

            AzureLightningEntity lightning = new AzureLightningEntity(serverLevel, livingOwner, lx, ly, lz, 450.0f);
            lightning.setTelegraphTicks(12);
            if (serverLevel.getRandom().nextFloat() < 0.2f) {
                lightning.setRare(true);
            }
            serverLevel.addFreshEntity(lightning);

            this.nextStrikeTick = this.ageTicks + Math.round(30f - 14f * env);
        }

        // Ambient storm sound
        if (this.ageTicks % 40 == 0) {
            serverLevel.playSound(null, center.x, center.y, center.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 1.0f, 0.9f);
        }

        if (this.ageTicks >= life) {
            this.discard();
        }
    }
}
