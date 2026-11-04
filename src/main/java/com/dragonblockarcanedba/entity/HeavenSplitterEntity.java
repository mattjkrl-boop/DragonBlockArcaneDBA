package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Heaven Splitter Entity: towering physical 3D dimensional slash model and
 * vertical reality slice rendered across the entire dash path during Katana
 * Iaijutsu: Heaven Splitter. The entity owns the cut: damage lands at
 * DAMAGE_TICK, once the wall has fully risen, with falloff toward the far
 * end of the path, plus ground dust, a debris field where the cut meets the
 * ground, and branched bolts overhead.
 */
public class HeavenSplitterEntity extends Projectile {
    private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(HeavenSplitterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(HeavenSplitterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(HeavenSplitterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DASH_LENGTH = SynchedEntityData.defineId(HeavenSplitterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SLASH_SCALE = SynchedEntityData.defineId(HeavenSplitterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MAX_LIFETIME = SynchedEntityData.defineId(HeavenSplitterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(HeavenSplitterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CORRIDOR_HALF = SynchedEntityData.defineId(HeavenSplitterEntity.class, EntityDataSerializers.FLOAT);

    /** Tick the cut lands, at 25 percent of the 40 tick life the item spawns. */
    public static final int DAMAGE_TICK = 10;

    public HeavenSplitterEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public HeavenSplitterEntity(Level level, LivingEntity owner, Vec3 startPos, Vec3 direction, float dashLength, float scale, int maxLifetime) {
        super(DbaEntities.HEAVEN_SPLITTER, level);
        this.setOwner(owner);
        this.setPos(startPos.x, startPos.y, startPos.z);
        this.noPhysics = true;

        Vec3 normDir = direction.normalize();
        this.entityData.set(DIR_X, (float) normDir.x);
        this.entityData.set(DIR_Y, (float) normDir.y);
        this.entityData.set(DIR_Z, (float) normDir.z);
        this.entityData.set(DASH_LENGTH, dashLength);
        this.entityData.set(SLASH_SCALE, scale);
        this.entityData.set(MAX_LIFETIME, maxLifetime);
    }

    /** Damage carrying constructor: the wall strikes for damage inside a corridor of corridorHalf at DAMAGE_TICK. */
    public HeavenSplitterEntity(Level level, LivingEntity owner, Vec3 startPos, Vec3 direction, float dashLength, float scale, int maxLifetime,
                                float damage, float corridorHalf) {
        this(level, owner, startPos, direction, dashLength, scale, maxLifetime);
        this.entityData.set(DAMAGE, damage);
        this.entityData.set(CORRIDOR_HALF, corridorHalf);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DIR_X, 1.0f);
        builder.define(DIR_Y, 0.0f);
        builder.define(DIR_Z, 0.0f);
        builder.define(DASH_LENGTH, 16.0f);
        builder.define(SLASH_SCALE, 1.0f);
        builder.define(MAX_LIFETIME, 20);
        builder.define(DAMAGE, 0.0f);
        builder.define(CORRIDOR_HALF, 3.0f);
    }

    public Vec3 getDashDirection() {
        return new Vec3(this.entityData.get(DIR_X), this.entityData.get(DIR_Y), this.entityData.get(DIR_Z));
    }

    public float getDashLength() {
        return this.entityData.get(DASH_LENGTH);
    }

    public float getSlashScale() {
        return this.entityData.get(SLASH_SCALE);
    }

    public int getMaxLifetime() {
        return this.entityData.get(MAX_LIFETIME);
    }

    public float getDamage() {
        return this.entityData.get(DAMAGE);
    }

    public float getCorridorHalf() {
        return this.entityData.get(CORRIDOR_HALF);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            emitWallParticles(serverLevel);

            if (this.tickCount == DAMAGE_TICK && getDamage() > 0f) {
                strike(serverLevel);
            }

            if (this.tickCount >= getMaxLifetime()) {
                this.discard();
            }
        }
    }

    /**
     * Layered particles along the cut (standard point 1), all scaled by the
     * shared lifecycle envelope: bright core motes hugging the spine, a pale
     * cyan mid dust sheet, and outer sparks spraying off the wall.
     */
    private void emitWallParticles(ServerLevel level) {
        float density = DbaEffectsConfig.effectDensity();
        if (density <= 0f) return;
        float env = FxEnvelope.envelope(this.tickCount, getMaxLifetime());
        if (env <= 0f) return;
        net.minecraft.util.RandomSource rand = this.getRandom();
        Vec3 dir = getDashDirection();
        double along = rand.nextDouble() * getDashLength();
        double px = this.getX() + dir.x * along;
        double py = this.getY() + dir.y * along;
        double pz = this.getZ() + dir.z * along;

        if (rand.nextFloat() < env * density) {
            level.sendParticles(DbaParticles.KI_MOTE,
                px, py + 0.4 + rand.nextDouble() * 3.2, pz,
                1, 0.05, 0.10, 0.05, 0.01);
        }
        if (rand.nextFloat() < env * density) {
            level.sendParticles(LayeredParticles.dust(0x7FD8FF, 1.5f),
                px, py + rand.nextDouble() * 4.5, pz,
                1, 0.25, 0.30, 0.25, 0.01);
        }
        if (this.tickCount % 2 == 0) {
            level.sendParticles(DbaParticles.SPARK,
                px, py + 1.2, pz,
                Math.max(1, Math.round(3f * density * env)),
                0.3, 1.4, 0.3, 0.05);
        }
    }

    /**
     * The cut lands: every living target in the corridor takes the damage
     * with distance falloff, the first target takes the heavy impact burst,
     * and a kill with the cut removes the katana cooldown. Then the world
     * answers: dust at both ends of the path, a 28 piece debris field where
     * the cut meets the ground, three branched bolts along the path, and the
     * thunder crack.
     */
    private void strike(ServerLevel level) {
        if (!(this.getOwner() instanceof ServerPlayer player)) return;
        Vec3 dir = getDashDirection();
        Vec3 start = this.position();
        Vec3 end = start.add(dir.scale(getDashLength()));
        AABB box = new AABB(start, end).inflate(getCorridorHalf());
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != player);
        targets.sort(Comparator.comparingDouble(e -> e.position().subtract(start).dot(dir)));

        boolean first = true;
        for (LivingEntity t : targets) {
            double alongFrac = Math.max(0.0, Math.min(1.0,
                t.position().subtract(start).dot(dir) / getDashLength()));
            float falloff = (float) (1.0 - 0.30 * alongFrac);
            t.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 20, 0, false, false, false), player);
            t.hurtServer(level, level.damageSources().playerAttack(player),
                DbaEffectsConfig.capDamage(getDamage() * falloff));
            if (first) {
                com.dragonblockarcanedba.fx.DbaImpactFx.heavy(level, t.getX(), t.getY(), t.getZ());
                first = false;
            }
            // Tweak C: a kill with the cut resets the katana cooldown.
            if (!t.isAlive()) {
                player.getCooldowns().removeCooldown(com.dragonblockarcanedba.item.DbaItems.KATANA_KEY.identifier());
            }
        }

        LayeredParticles.emitGroundBurst(level, start, 1.2f, 0x7FD8FF);
        LayeredParticles.emitGroundBurst(level, end, 1.2f, 0x7FD8FF);

        Vec3 mid = start.add(dir.scale(getDashLength() * 0.5));
        BlockPos ground = findGround(level, mid);
        if (ground != null) {
            Vec3 groundCenter = new Vec3(ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5);
            DebrisSpawner.spawnField(level, player, groundCenter, 28, 3.0, 3.5, 34);
        }

        net.minecraft.util.RandomSource rand = this.getRandom();
        double[] boltSpots = {0.25, 0.55, 0.85};
        for (double frac : boltSpots) {
            Vec3 at = start.add(dir.scale(getDashLength() * frac));
            BlockPos g = findGround(level, at);
            Vec3 strikePoint = g != null
                ? new Vec3(g.getX() + 0.5, g.getY() + 1.0, g.getZ() + 0.5)
                : at;
            level.addFreshEntity(new BoltEntity(level, strikePoint, rand.nextInt(), 0xFFBFEFFF, 11.0f, 8));
        }

        level.playSound(null, mid.x, mid.y, mid.z,
            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 1.8f);
        level.playSound(null, mid.x, mid.y, mid.z,
            com.dragonblockarcanedba.sound.DbaSounds.LIGHTNING_ZAP, SoundSource.PLAYERS, 2.0f, 1.8f);
    }

    /** First non-air block scanning down from just above the point, or null over open air or void. */
    private static BlockPos findGround(Level level, Vec3 at) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int x = (int) Math.floor(at.x), z = (int) Math.floor(at.z);
        int top = (int) Math.floor(at.y) + 3;
        int bottom = (int) Math.floor(at.y) - 10;
        for (int y = top; y >= bottom; y--) {
            m.set(x, y, z);
            if (!level.getBlockState(m).isAir()) {
                return m.immutable();
            }
        }
        return null;
    }
}
