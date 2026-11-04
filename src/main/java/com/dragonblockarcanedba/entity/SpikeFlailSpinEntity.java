package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.item.SpikeFlailItem;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.dragonblockarcanedba.util.DbaCompat;
import com.dragonblockarcanedba.util.DbaDamage;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.dragonblockarcanedba.util.GroundScar;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ball orbits while the flail is held, then cracks outward once the channel ends.
 * Channel time is the charge: it scales the crack's damage, radius, knockback,
 * debris count, and scar size. The crack damage lands on the tick the expanding
 * visual disc reaches the full crack radius, never before (telegraph honesty).
 */
public class SpikeFlailSpinEntity extends Projectile {
    /** Ticks the release disc spends expanding from the orbit radius to the crack radius. */
    public static final int RELEASE_TICKS = 7;
    /** Ticks of dissipation after the crack lands, while debris rains and dust settles. */
    public static final int SETTLE_TICKS = 4;
    private static final float ORBIT_RADIUS = 2.55f;
    private static final int SCAR_RESTORE_TICKS = 100;
    /** Warm steel dust, the flail's particle theme color. */
    private static final int THEME_DUST = 0xD8C9A3;

    private static final EntityDataAccessor<Float> ANGLE = SynchedEntityData.defineId(SpikeFlailSpinEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(SpikeFlailSpinEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RELEASING = SynchedEntityData.defineId(SpikeFlailSpinEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> RELEASE_AGE = SynchedEntityData.defineId(SpikeFlailSpinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CHARGE = SynchedEntityData.defineId(SpikeFlailSpinEntity.class, EntityDataSerializers.FLOAT);

    private final Map<Integer, Integer> lastHit = new HashMap<>();
    private boolean cracked;
    private float releaseCharge;
    private float releaseRadius = ORBIT_RADIUS;

    public SpikeFlailSpinEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public SpikeFlailSpinEntity(Level level, LivingEntity owner) {
        super(DbaEntities.SPIKE_FLAIL_SPIN, level);
        this.setOwner(owner);
        this.noPhysics = true;
        if (owner != null) {
            this.setPos(owner.getX(), owner.getY(), owner.getZ());
            this.setYRot(owner.getYRot());
        }
        this.entityData.set(ANGLE, owner != null ? owner.getYRot() : 0.0f);
        this.entityData.set(RADIUS, ORBIT_RADIUS);
        this.entityData.set(RELEASING, false);
        this.entityData.set(RELEASE_AGE, 0);
        this.entityData.set(CHARGE, 0.0f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ANGLE, 0.0f);
        builder.define(RADIUS, ORBIT_RADIUS);
        builder.define(RELEASING, false);
        builder.define(RELEASE_AGE, 0);
        builder.define(CHARGE, 0.0f);
    }

    public float angle() {
        return this.entityData.get(ANGLE);
    }

    public float orbitRadius() {
        return this.entityData.get(RADIUS);
    }

    public boolean releasing() {
        return this.entityData.get(RELEASING);
    }

    public int releaseAge() {
        return this.entityData.get(RELEASE_AGE);
    }

    /** Channel charge 0..1, synced so the renderer builds with the same number the damage uses. */
    public float charge() {
        return this.entityData.get(CHARGE);
    }

    /** Crack radius for a charge: a tap barely clears the orbit, a full channel reaches 5.4. */
    public static float crackRadiusFor(float charge) {
        return 2.7f + 2.7f * charge;
    }

    public Vec3 ballPos() {
        double rad = Math.toRadians(this.angle());
        return new Vec3(Math.cos(rad) * this.orbitRadius(), 1.15, Math.sin(rad) * this.orbitRadius());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = this.getOwner();
        if (!(owner instanceof Player player) || !player.isAlive()) {
            if (owner != null || this.tickCount > 2) {
                this.discard();
            }
            return;
        }
        this.setPos(player.getX(), player.getY(), player.getZ());
        this.setYRot(player.getYRot());
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        boolean holding = player.isUsingItem() && player.getUseItem().getItem() instanceof SpikeFlailItem;
        if (!holding && !this.releasing()) {
            this.releaseCharge = this.charge();
            this.releaseRadius = crackRadiusFor(this.releaseCharge);
            this.entityData.set(RELEASING, true);
        }
        if (this.releasing()) {
            this.tickRelease(server, player);
            return;
        }
        // Orbit phase: the channel builds charge, presence, and contact damage.
        float charge = Math.min(1.0f, this.tickCount / (float) SpikeFlailItem.CHANNEL_TICKS);
        this.entityData.set(CHARGE, charge);
        this.entityData.set(ANGLE, this.angle() + 32.0f);
        Vec3 center = new Vec3(player.getX(), player.getY() + 0.1, player.getZ());
        LayeredParticles.emitVortex(server, center, ORBIT_RADIUS, 1.7f,
            this.tickCount, SpikeFlailItem.CHANNEL_TICKS, THEME_DUST, DbaParticles.WIND);
        if (this.tickCount % 4 != 0) {
            return;
        }
        // Dust kicked up at the ground under the whipping ball.
        Vec3 ball = this.position().add(this.ballPos());
        LayeredParticles.emitGroundBurst(server, new Vec3(ball.x, player.getY() + 0.12, ball.z), 0.8f, THEME_DUST);
        float strength = player instanceof PlayerStatsAccessor acc ? acc.dba$getStrength() : 0f;
        float damage = 200.0f + strength * 0.5f;
        this.hitNearBall(server, player, damage, 1.25, 8);
    }

    private void tickRelease(ServerLevel server, Player player) {
        int age = this.releaseAge() + 1;
        this.entityData.set(RELEASE_AGE, age);
        float t = Math.min(1.0f, age / (float) RELEASE_TICKS);
        float radius = ORBIT_RADIUS + (this.releaseRadius - ORBIT_RADIUS) * FxEnvelope.smoothstep(t);
        this.entityData.set(RADIUS, radius);
        if (age < RELEASE_TICKS) {
            // Dust rides the expanding edge so the telegraph reads as ground tearing open.
            RandomSource rand = server.getRandom();
            for (int i = 0; i < 3; i++) {
                double a = rand.nextDouble() * Math.PI * 2.0;
                server.sendParticles(LayeredParticles.dust(THEME_DUST, 1.1f),
                    player.getX() + Math.cos(a) * radius, player.getY() + 0.15, player.getZ() + Math.sin(a) * radius,
                    1, 0.1, 0.05, 0.1, 0.02);
            }
        } else if (age == RELEASE_TICKS && !this.cracked) {
            // The visual disc has reached the damage radius: now, and only now, the hit lands.
            this.cracked = true;
            this.crack(server, player);
            this.impactFx(server, player);
        } else if (age > RELEASE_TICKS) {
            // Settle: a thin drizzle of theme dust while the debris rains down.
            LayeredParticles.emitRain(server, new Vec3(player.getX(), player.getY() + 0.1, player.getZ()),
                this.releaseRadius * 0.9f, 1.6f, age - RELEASE_TICKS, SETTLE_TICKS,
                LayeredParticles.dust(THEME_DUST, 1.0f));
        }
        if (age > RELEASE_TICKS + SETTLE_TICKS) {
            this.discard();
        }
    }

    private void hitNearBall(ServerLevel server, Player player, float damage, double reach, int gap) {
        Vec3 local = this.ballPos();
        Vec3 ball = this.position().add(local);
        AABB box = new AABB(ball, ball).inflate(reach);
        List<LivingEntity> hits = server.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != player);
        for (LivingEntity target : hits) {
            int previous = this.lastHit.getOrDefault(target.getId(), -100);
            if (this.tickCount - previous < gap) {
                continue;
            }
            if (target.distanceToSqr(ball) > reach * reach) {
                continue;
            }
            this.lastHit.put(target.getId(), this.tickCount);
            target.hurtServer(server, DbaDamage.crush(this, player), DbaEffectsConfig.capDamage(damage));
            server.sendParticles(DbaParticles.SPARK, ball.x, ball.y, ball.z, 4, 0.18, 0.18, 0.18, 0.02);
            Vec3 push = target.position().subtract(player.position());
            push = new Vec3(push.x, 0.0, push.z);
            if (push.lengthSqr() > 0.01) {
                push = push.normalize().scale(0.35);
                target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.12, push.z));
                DbaCompat.markHurt(target);
            }
        }
    }

    private void crack(ServerLevel server, Player player) {
        server.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.GROUND_CRACK, SoundSource.PLAYERS, 0.9f, 1.0f);
        float strength = player instanceof PlayerStatsAccessor acc ? acc.dba$getStrength() : 0f;
        float charge = this.releaseCharge;
        float damage = 480.0f + 600.0f * charge + strength * (1.0f + 1.0f * charge);
        double radius = this.releaseRadius;
        AABB box = player.getBoundingBox().inflate(radius, 1.6, radius);
        List<LivingEntity> hits = server.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player);
        for (LivingEntity target : hits) {
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > radius || dist < 0.4) {
                continue;
            }
            target.hurtServer(server, DbaDamage.crush(this, player), DbaEffectsConfig.capDamage(damage));
            double speed = 0.55 + 0.45 * charge;
            double up = 0.30 + 0.16 * charge;
            target.setDeltaMovement(target.getDeltaMovement().add(dx / dist * speed, up, dz / dist * speed));
            DbaCompat.markHurt(target);
        }
    }

    /** World interaction on the impact tick: base burst, torn ground, and ejecta along the crack. */
    private void impactFx(ServerLevel server, Player player) {
        Vec3 center = new Vec3(player.getX(), player.getY() + 0.1, player.getZ());
        LayeredParticles.emitGroundBurst(server, center, this.releaseRadius * 0.75f, THEME_DUST);
        this.scarGround(server, player);
        int pieces = 24 + Math.round(8.0f * this.releaseCharge);
        DebrisSpawner.spawnField(server, player, new Vec3(player.getX(), player.getY() + 0.2, player.getZ()),
            pieces, this.releaseRadius, 2.4, 16);
        server.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.SLAM_THUD, SoundSource.PLAYERS, 0.7f, 0.7f);
    }

    /** Tears the surface inside the crack radius into gravel for a while, then the world restores it. */
    private void scarGround(ServerLevel server, Player player) {
        int r = (int) Math.ceil(this.releaseRadius);
        BlockPos base = player.blockPosition();
        RandomSource rand = server.getRandom();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > this.releaseRadius) {
                    continue;
                }
                float chance = dist <= 1.5 ? 1.0f : 0.6f;
                if (rand.nextFloat() > chance) {
                    continue;
                }
                // Find this column's floor near the player's level and scar that cell.
                // The floor test is isFaceSturdy: blocksMotion() was removed in 26.3.
                for (int dy = 2; dy >= -3; dy--) {
                    m.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    BlockState state = server.getBlockState(m);
                    if (state.isFaceSturdy(server, m, Direction.UP)) {
                        GroundScar.scar(server, m, Blocks.GRAVEL.defaultBlockState(), SCAR_RESTORE_TICKS);
                        break;
                    }
                }
            }
        }
    }
}
