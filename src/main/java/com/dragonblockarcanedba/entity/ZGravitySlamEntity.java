package com.dragonblockarcanedba.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Z Gravity Slam Entity — Physical 3D ground shatter, radiating tectonic canyon fracture trenches,
 * colossal erupted Katchin monolith slabs, and expanding gravitational shockwave dome.
 * Spawned on Right-Click release for the Z Sword Gravity Slam (6 to 18 block radius,
 * 12 to 36 blocks across).
 *
 * Spectacle pass: this entity owns the slam damage now. The hit lands at the
 * visual peak tick (30 percent of life) with distance falloff, not in the
 * release tick before the visual starts. It also throws a real debris field,
 * converts the ground along its trenches to permanent magma, punches a real
 * crater at the strike tick, pins nearby mobs with a light vortex for the
 * launch window, and persists its state so a chunk unload cannot skip or
 * double the strike.
 */
public class ZGravitySlamEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(ZGravitySlamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> POWER_RATIO = SynchedEntityData.defineId(ZGravitySlamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(ZGravitySlamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(ZGravitySlamEntity.class, EntityDataSerializers.INT);

    private float damage = 0.0f;
    private int pinTicks = 40;
    private boolean struck = false;

    public ZGravitySlamEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public ZGravitySlamEntity(Level level, LivingEntity caster, Vec3 pos, float radius, float powerRatio,
                              float damage, int pinTicks) {
        super(DbaEntities.Z_GRAVITY_SLAM, level);
        this.setOwner(caster);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.damage = damage;
        this.pinTicks = pinTicks;

        int life = 80 + (int) (powerRatio * 40); // 80 to 120 ticks: room for build, peak, and a real dissipation
        this.entityData.set(RADIUS, radius);
        this.entityData.set(POWER_RATIO, powerRatio);
        this.entityData.set(LIFETIME, life);
        this.entityData.set(SEED, level.getRandom().nextInt());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 10.0f);
        builder.define(POWER_RATIO, 1.0f);
        builder.define(LIFETIME, 50);
        builder.define(SEED, 0);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public float getPowerRatio() {
        return this.entityData.get(POWER_RATIO);
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
    }

    /** The tick the dome reaches full size and the damage lands: 30 percent of life. */
    public int strikeTick() {
        return Math.max(6, (int) (getLifetime() * 0.30f));
    }

    // Persist everything the strike depends on: the synced values are not saved
    // by vanilla, and a reloaded slam that lost its damage or double struck
    // would be worse than one that never fired.
    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Radius", getRadius());
        output.putFloat("PowerRatio", getPowerRatio());
        output.putInt("Lifetime", getLifetime());
        output.putFloat("Damage", this.damage);
        output.putInt("PinTicks", this.pinTicks);
        output.putBoolean("Struck", this.struck);
        output.putInt("Seed", getSeed());
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(RADIUS, input.getFloatOr("Radius", 10.0f));
        this.entityData.set(POWER_RATIO, input.getFloatOr("PowerRatio", 1.0f));
        this.entityData.set(LIFETIME, input.getIntOr("Lifetime", 50));
        this.damage = input.getFloatOr("Damage", 0.0f);
        this.pinTicks = input.getIntOr("PinTicks", 40);
        this.struck = input.getBooleanOr("Struck", false);
        this.entityData.set(SEED, input.getIntOr("Seed", 0));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            float power = getPowerRatio();
            float radius = getRadius();
            int life = getLifetime();

            if (this.tickCount == 1) {
                // Base interaction on the cast tick: shared slam debris burst,
                // a layered ground burst, and permanent magma along the trenches.
                com.dragonblockarcanedba.fx.SlamDebris.burst(serverLevel, this.position(), radius, com.dragonblockarcanedba.fx.SlamDebris.Profile.Z_GRAVITY);
                com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel, this.position(), radius * 0.7f, 0xFFD433);
                convertTrenches(serverLevel, radius);
            }
            if (this.tickCount == 2) {
                // Debris field (standard point 2): individual ground sampled
                // pieces on helical orbits that rain back down. 26 pieces at a
                // tap hold, 62 at full power.
                Entity owner = this.getOwner();
                int count = 26 + (int) (power * 36);
                com.dragonblockarcanedba.util.DebrisSpawner.spawnField(serverLevel,
                    owner instanceof LivingEntity livingOwner ? livingOwner : null,
                    this.position(), count, radius * 0.85, 5.0 + power * 4.0, 30 + (int) (power * 20));
            }

            // Gravity pin for the launch window: a light inward drag on top of
            // the MOVEMENT_CURSE root, so things that wander in also get held.
            if (this.tickCount <= this.pinTicks) {
                Entity owner = this.getOwner();
                float env = com.dragonblockarcanedba.util.FxEnvelope.envelope(this.tickCount, life, 0.10f, 0.30f);
                com.dragonblockarcanedba.util.VortexPhysics.applyVortex(serverLevel, this.position(),
                    radius, 3.0, owner instanceof LivingEntity livingOwner ? livingOwner : null,
                    0.05, 0.4, 0.0, Math.max(0.25f, env));
            }

            // The strike: damage lands at the visual peak, with falloff. Full
            // damage inside 45 percent of the radius, easing to 30 percent at
            // the rim, so standing at the edge is survivable and the centre is not.
            if (!this.struck && this.tickCount >= strikeTick()) {
                this.struck = true;
                strike(serverLevel, radius);
            }

            // Layered particles for the whole life: vortex bands of gold dust and
            // sparks scaled by the envelope, then ember rain while it dissipates.
            if (this.tickCount % 2 == 0) {
                com.dragonblockarcanedba.util.LayeredParticles.emitVortex(serverLevel, this.position(),
                    radius * 0.8f, 2.5f + power * 2.0f, this.tickCount, life, 0xFFD433,
                    com.dragonblockarcanedba.particle.DbaParticles.SPARK);
                float progress = this.tickCount / (float) life;
                if (progress > 0.65f) {
                    com.dragonblockarcanedba.util.LayeredParticles.emitRain(serverLevel, this.position(),
                        radius * 0.8f, 3.0f, this.tickCount, life,
                        com.dragonblockarcanedba.particle.DbaParticles.EMBER);
                }
            }
            if (this.tickCount >= life) {
                this.discard();
            }
        }
    }

    private void strike(ServerLevel serverLevel, float radius) {
        Entity owner = this.getOwner();
        DamageSource source = com.dragonblockarcanedba.util.DbaDamage.gravity(this,
            owner instanceof LivingEntity livingOwner ? livingOwner : null);

        AABB box = this.getBoundingBox().inflate(radius, 4.0, radius);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != owner);
        for (LivingEntity target : targets) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > radius) continue;
            double full = radius * 0.45;
            double factor = dist <= full ? 1.0 : 1.0 - 0.7 * ((dist - full) / (radius - full));
            target.hurtServer(serverLevel, source,
                com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (this.damage * factor)));
        }

        // The payoff sound sits on the strike tick now, not front loaded on release.
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.SLAM_THUD, net.minecraft.sounds.SoundSource.HOSTILE, 1.6f, 0.5f);
        com.dragonblockarcanedba.fx.DbaImpactFx.heavy(serverLevel, this.getX(), this.getY(), this.getZ());

        // The strike punches a real crater at the slam's heart (radius 2.5,
        // STANDARD tier, cap 120): the dome does not just push mobs, it
        // breaks the ground they were standing on.
        BlockPos heartAir = com.dragonblockarcanedba.util.PermanentDestruction.findSurface(serverLevel,
            (int) Math.floor(this.getX()), (int) Math.floor(this.getZ()), (int) Math.floor(this.getY()));
        if (heartAir != null) {
            com.dragonblockarcanedba.util.PermanentDestruction.crater(serverLevel, heartAir.below(), 2.5, 0.5, false,
                com.dragonblockarcanedba.util.PermanentDestruction.TIER_STANDARD, 120, serverLevel.getRandom());
        }
    }

    /**
     * Convert the surface along radial trench lines to magma permanently,
     * the way the renderer draws its glowing fissures. Each column finds
     * its own surface, and protected blocks are refused inside
     * PermanentDestruction.
     */
    private void convertTrenches(ServerLevel serverLevel, float radius) {
        var rand = serverLevel.getRandom();
        int trenchCount = 14 + (int) (getPowerRatio() * 6);
        BlockPos center = this.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int t = 0; t < trenchCount; t++) {
            double angle = (t / (double) trenchCount) * Math.PI * 2.0 + rand.nextDouble() * 0.25;
            double len = radius * (0.75 + rand.nextDouble() * 0.40);
            int steps = (int) Math.ceil(len / 1.4);
            for (int s = 1; s <= steps; s++) {
                double d = len * s / (double) steps;
                int x = center.getX() + (int) Math.floor(Math.cos(angle) * d);
                int z = center.getZ() + (int) Math.floor(Math.sin(angle) * d);
                // Find this column's surface near the slam height.
                for (int dy = 3; dy >= -6; dy--) {
                    m.set(x, center.getY() + dy, z);
                    var state = serverLevel.getBlockState(m);
                    if (!state.isAir() && state.getFluidState().isEmpty()) {
                        com.dragonblockarcanedba.util.PermanentDestruction.convert(serverLevel, m.immutable(),
                            Blocks.MAGMA_BLOCK.defaultBlockState());
                        break;
                    }
                }
            }
        }
    }
}
