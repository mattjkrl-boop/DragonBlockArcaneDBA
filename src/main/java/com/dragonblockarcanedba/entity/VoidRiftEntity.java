package com.dragonblockarcanedba.entity;

import net.minecraft.world.damagesource.DamageSource;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.DbaCompat;
import com.dragonblockarcanedba.util.DbaDamage;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.dragonblockarcanedba.util.PermanentDestruction;
import com.dragonblockarcanedba.util.LayeredParticles;
import com.dragonblockarcanedba.util.VortexPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
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
 * Void Rift Entity, a dimensional gravitational vortex created by Hollow's Edge.
 *
 * Spectacle pass rebuild: the pull, lift, and final fling run on the shared
 * VortexPhysics language instead of per entity velocity overrides, particles
 * come from LayeredParticles in three layers, the collapse is a staged 30 tick
 * suction (was a 10 tick snap) that drags a DebrisSpawner field of real ground
 * blocks up into the tear before the burst, and the burst converts the
 * ground under the rift to permanent obsidian. Channel tick damage is distance checked
 * with falloff; standing at the rim is no longer the same as standing at the
 * core. Loose items and projectiles are still eaten, but the owner's own
 * projectiles are exempt so the rift cannot sabotage its caster.
 */
public class VoidRiftEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(VoidRiftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_IMPLODING = SynchedEntityData.defineId(VoidRiftEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> IMPLODE_START_TICK = SynchedEntityData.defineId(VoidRiftEntity.class, EntityDataSerializers.INT);

    /** Staged collapse length in ticks. The renderer phases its collapse from this same number. */
    public static final int IMPLOSION_TICKS = 30;
    /** Hard cap on the channel phase before the rift implodes on its own (was 400). */
    public static final int MAX_CHANNEL_TICKS = 240;
    /** Void violet, matching the tear lip color in VoidRiftRenderer. */
    private static final int THEME_ARGB = 0x800DE0;
    /** Channel tick damage per 10 tick pulse, as a fraction of burst damage at the core. */
    private static final float TICK_DAMAGE_FRACTION = 0.08f;

    private float damage = 500.0f;
    private int implosionTimer = -1;
    private boolean debrisWaveSpawned = false;
    private boolean spawnBurstDone = false;

    public VoidRiftEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public VoidRiftEntity(Level level, LivingEntity owner, Vec3 pos, float startRadius, float damage) {
        super(DbaEntities.VOID_RIFT, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.damage = damage;
        this.noPhysics = true;
        this.setRadius(startRadius);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 2.5f);
        builder.define(IS_IMPLODING, false);
        builder.define(IMPLODE_START_TICK, -1);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(RADIUS, Math.min(12.0f, Math.max(1.5f, radius)));
    }

    public boolean isImploding() {
        return this.entityData.get(IS_IMPLODING);
    }

    public void triggerImplosion() {
        this.entityData.set(IS_IMPLODING, true);
        // Synced so the client can phase the collapse animation from the real
        // trigger moment instead of total age modulo the window.
        this.entityData.set(IMPLODE_START_TICK, this.tickCount);
        this.implosionTimer = IMPLOSION_TICKS;
        this.debrisWaveSpawned = false;
    }

    public int getImplodeStartTick() {
        return this.entityData.get(IMPLODE_START_TICK);
    }

    private DamageSource damageSource() {
        // Bespoke pass: the channel pulses and the burst are ability damage,
        // so they use the mod's own void type with this rift as the direct
        // entity and the caster as the attacker.
        return DbaDamage.voidDmg(this, this.getOwner());
    }

    private static Vec3 centerOf(Entity e) {
        return e.position().add(0, e.getBbHeight() * 0.5, 0);
    }

    /** First solid block at or below the rift center column, or null when the drop is too deep. */
    private static BlockPos findGround(ServerLevel level, Vec3 center, int maxDrop) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int x = (int) Math.floor(center.x), z = (int) Math.floor(center.z);
        int startY = (int) Math.floor(center.y);
        for (int y = startY; y >= Math.max(level.getMinY(), startY - maxDrop); y--) {
            m.set(x, y, z);
            if (!level.getBlockState(m).getCollisionShape(level, m).isEmpty()) {
                return m.immutable();
            }
        }
        return null;
    }

    /**
     * Drag loose non living things (dropped items, projectiles) into the tear.
     * The owner's own projectiles are exempt, debris already has its own
     * helical motion, and living targets are VortexPhysics' job.
     */
    private void pullLooseEntities(ServerLevel level, Vec3 center, double radius, double power) {
        Entity owner = this.getOwner();
        AABB box = this.getBoundingBox().inflate(radius);
        List<Entity> loose = level.getEntitiesOfClass(Entity.class, box, e -> e.isAlive() && e != this && e != owner
            && !(e instanceof LivingEntity)
            && !(e instanceof DebrisEntity)
            && !(e instanceof Projectile proj && proj.getOwner() == owner));
        for (Entity e : loose) {
            Vec3 toCenter = center.subtract(e.position());
            double dist = toCenter.length();
            if (dist > radius || dist < 0.3) continue;
            e.setDeltaMovement(e.getDeltaMovement().add(toCenter.normalize().scale(power * (1.0 - 0.5 * dist / radius))));
            DbaCompat.markHurt(e);
        }
    }

    /**
     * The world being eaten: real ground blocks torn up below the tear, sent
     * orbiting up into it on helical paths by DebrisSpawner, then left to rain
     * back down once their orbit ends.
     */
    private void spawnRiftDebris(ServerLevel level, Vec3 center, float radius, LivingEntity owner, boolean big) {
        float density = DbaEffectsConfig.effectDensity();
        if (density <= 0f) return;
        BlockPos ground = findGround(level, center, 20);
        if (ground == null) return;
        Vec3 groundCenter = new Vec3(center.x, ground.getY() + 1.0, center.z);
        int count = Math.max(2, Math.round((big ? 60 : 12) * density));
        double rise = Math.max(2.0, center.y - groundCenter.y) + (big ? radius * 0.4 : 1.0);
        int orbitTicks = big ? 70 : 45;
        DebrisSpawner.spawnField(level, owner, groundCenter, count,
            Math.max(2.0, radius * 0.85), rise, orbitTicks,
            DebrisSpawner.sampleGroundPalette(level, groundCenter, radius));
    }

    @Override
    public void tick() {
        super.tick();

        float radius = getRadius();
        Vec3 center = this.position();
        Entity owner = this.getOwner();
        LivingEntity livingOwner = owner instanceof LivingEntity le ? le : null;

        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // The vortex force box is centered on the tear but spans its full height.
        Vec3 vortexCenter = new Vec3(center.x, center.y - radius, center.z);

        if (this.implosionTimer > 0) {
            int implodeAge = IMPLOSION_TICKS - this.implosionTimer;
            this.implosionTimer--;

            // Layered particles ride the collapse envelope: build fast, die with the burst.
            if (this.tickCount % 2 == 0) {
                LayeredParticles.emitVortex(serverLevel,
                    new Vec3(center.x, center.y - radius * 0.4, center.z),
                    radius, radius * 0.8f, implodeAge, IMPLOSION_TICKS,
                    THEME_ARGB, DbaParticles.VOID_SMOKE);
            }

            // Phase 1: staged suction on the shared vortex language, strengthening
            // through the window instead of one flat velocity override.
            float stage = 0.45f + 0.55f * (implodeAge / (float) IMPLOSION_TICKS);
            VortexPhysics.applyVortex(serverLevel, vortexCenter, radius * 1.6, radius * 2.0,
                livingOwner, 0.5 * stage, 0.85, 0.18, 1.0f);
            pullLooseEntities(serverLevel, center, radius * 1.6, 0.7 * stage);

            // The collapse eats the ground once, in a storm class debris wave.
            if (!this.debrisWaveSpawned) {
                this.debrisWaveSpawned = true;
                spawnRiftDebris(serverLevel, center, radius, livingOwner, true);
            }

            if (this.implosionTimer == 0) {
                burst(serverLevel, center, radius, livingOwner);
                this.discard();
            }
            return;
        }

        if (isImploding()) {
            return;
        }

        // Channel phase. The envelope builds over the first fifth of the maximum
        // channel and starts to thin in the last third, telegraphing the forced
        // implosion at MAX_CHANNEL_TICKS instead of arriving as a surprise.
        float envelope = FxEnvelope.envelope(this.tickCount, MAX_CHANNEL_TICKS,
            FxEnvelope.STD_BUILD, FxEnvelope.STD_DECAY);

        // Layered particles: bright core motes, a violet helical mid band at the
        // rim where the tear's size reads, and void smoke wisps outside it.
        if (this.tickCount % 2 == 0) {
            LayeredParticles.emitVortex(serverLevel,
                new Vec3(center.x, center.y - radius * 0.4, center.z),
                radius, radius * 0.8f, this.tickCount, MAX_CHANNEL_TICKS,
                THEME_ARGB, DbaParticles.VOID_SMOKE);
        }

        // One dust burst where the tear meets the ground, shortly after it opens.
        if (!this.spawnBurstDone) {
            this.spawnBurstDone = true;
            BlockPos ground = findGround(serverLevel, center, 16);
            if (ground != null) {
                LayeredParticles.emitGroundBurst(serverLevel,
                    new Vec3(center.x, ground.getY() + 1.0, center.z), radius * 0.6f, THEME_ARGB);
            }
        }

        // Continuous pull on the shared vortex language: inward drag, tangential
        // swirl, and a core lift, all weight classed by VortexPhysics.
        VortexPhysics.applyVortex(serverLevel, vortexCenter, radius * 1.35, radius * 2.0,
            livingOwner, 0.30, 0.6, 0.10, Math.max(0.2f, envelope));
        pullLooseEntities(serverLevel, center, radius * 1.35, 0.25);

        // Dark Faded refresh and tick damage, both distance checked. The old
        // code tested damage against the bounding box with no distance check,
        // so a target at a box corner (about 1.41x the radius, visibly outside
        // the tear) took full core damage. Damage now falls off with distance:
        // full TICK_DAMAGE_FRACTION at the core, 35 percent of it at the rim.
        if (this.tickCount % 5 == 0) {
            boolean damageTick = this.tickCount % 10 == 0;
            AABB aoe = this.getBoundingBox().inflate(radius * 1.35);
            List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, aoe,
                e -> e.isAlive() && e != owner);
            for (LivingEntity living : nearby) {
                double dist = centerOf(living).distanceTo(center);
                if (dist > radius * 1.35) continue;
                living.addEffect(new MobEffectInstance(DbaEffects.DARK_FADED_HOLDER, 30, 0, false, false), owner);
                if (damageTick && dist <= radius) {
                    double falloff = 1.0 - 0.65 * (dist / radius);
                    living.hurtServer(serverLevel, damageSource(),
                        DbaEffectsConfig.capDamage((float) (this.damage * TICK_DAMAGE_FRACTION * falloff)));
                }
            }
        }

        // A mature rift keeps eating the ground in small bites while it channels.
        if (this.tickCount % 40 == 20 && radius >= 4.0f) {
            spawnRiftDebris(serverLevel, center, radius, livingOwner, false);
        }

        // Sound pulse
        if (this.tickCount % 25 == 0) {
            serverLevel.playSound(null, center.x, center.y, center.z,
                com.dragonblockarcanedba.sound.DbaSounds.RIFT_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.7f + (radius / 10.0f) * 0.5f);
        }

        // Auto implode when the channel runs past the cap.
        if (this.tickCount > MAX_CHANNEL_TICKS) {
            triggerImplosion();
        }
    }

    /** Phase 2 of the collapse: the burst everything was gathered for. */
    private void burst(ServerLevel serverLevel, Vec3 center, float radius, LivingEntity livingOwner) {
        double blastRadius = radius * 1.5;
        AABB blastBox = this.getBoundingBox().inflate(blastRadius);
        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, blastBox,
            e -> e.isAlive() && e != this.getOwner());

        for (LivingEntity victim : victims) {
            double dist = centerOf(victim).distanceTo(center);
            if (dist > blastRadius) continue;
            // Gentle falloff to the edge of the honest blast sphere; the old box
            // test paid full damage at the corners, about 1.41x the radius out.
            float falloff = (float) (1.0 - 0.4 * (dist / blastRadius));
            victim.hurtServer(serverLevel, damageSource(),
                DbaEffectsConfig.capDamage(this.damage * falloff));

            // The fling is the shared ejection beat, weight classed like the pull.
            VortexPhysics.fling(victim, center, 2.2, 0.8);

            victim.addEffect(new MobEffectInstance(DbaEffects.DARK_FADED_HOLDER, 100, 1, false, true), this.getOwner());
        }

        // The burst marks the ground it ate from: a permanent obsidian
        // conversion, plus dust at both levels. A void rift consumes, it
        // does not blast, so there is no crater here.
        BlockPos ground = findGround(serverLevel, center, 16);
        if (ground != null) {
            PermanentDestruction.convertDisk(serverLevel, ground,
                Math.min(6, Math.max(2, Math.round(radius * 0.6f))),
                Blocks.OBSIDIAN.defaultBlockState(), 0.45f, serverLevel.getRandom());
            LayeredParticles.emitGroundBurst(serverLevel,
                new Vec3(center.x, ground.getY() + 1.0, center.z), radius * 0.7f, THEME_ARGB);
        }
        LayeredParticles.emitGroundBurst(serverLevel, center, radius * 0.7f, THEME_ARGB);

        // The implosion is the weapon's biggest moment: give it camera weight.
        com.dragonblockarcanedba.fx.DbaImpactFx.heavy(serverLevel, center.x, center.y, center.z);
        serverLevel.playSound(null, center.x, center.y, center.z,
            com.dragonblockarcanedba.sound.DbaSounds.RELEASE_DARK, SoundSource.PLAYERS, 2.5f, 0.6f);
        serverLevel.playSound(null, center.x, center.y, center.z,
            com.dragonblockarcanedba.sound.DbaSounds.BOOM_SONIC, SoundSource.PLAYERS, 2.0f, 0.8f);
        serverLevel.playSound(null, center.x, center.y, center.z,
            com.dragonblockarcanedba.sound.DbaSounds.WIND_GUST, SoundSource.PLAYERS, 2.0f, 0.8f);
    }
}
