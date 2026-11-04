package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Darkness Domain Entity: Physical 3D Geometric Void-Sphere Dome for Darkness Sword (World of Darkness & Abyssal Eclipse).
 * Tracks the expanding domain radius (8.0 to 30.0 blocks) and renders the volumetric void dome,
 * quad-tier ground seals, orbiting 3D obsidian spires, and central vortex column.
 *
 * Spectacle pass: the dome runs the shared FxEnvelope lifecycle. It builds in
 * over BUILD_TICKS, holds at full fade while the channel refreshes it, and
 * when the channel ends (clean release or interruption) it tears down over
 * TEARDOWN_TICKS instead of vanishing mid frame. The synced FADE value drives
 * every alpha in the renderer so both sides agree.
 *
 * The atmosphere is real now, not just a comment: while the domain lives it
 * emits layered particles through LayeredParticles, throws orbiting debris
 * fields through DebrisSpawner, and fires genuine branched BoltEntity
 * strikes inside the dome. The eclipse weather and gloom are driven by the
 * item through AtmosphereServer while the channel holds.
 */
public class DarknessDomainEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(DarknessDomainEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(DarknessDomainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> FADE = SynchedEntityData.defineId(DarknessDomainEntity.class, EntityDataSerializers.FLOAT);

    /** Ticks the dome takes to build from nothing to full alpha. */
    public static final int BUILD_TICKS = 30;
    /** Ticks the dome takes to dissipate once the channel ends. */
    public static final int TEARDOWN_TICKS = 25;
    /** Debris waves after the first: one fresh field every this many ticks. */
    private static final int DEBRIS_WAVE_TICKS = 200;
    /** Demon violet for bolt strikes, matching the dome palette. */
    private static final int BOLT_COLOR = 0xFFA605F2;
    /** Theme color for the layered particle mid band. */
    private static final int THEME_ARGB = 0xA605F2;
    /** Dark stone palette for the domain's orbiting debris: void shards, not torn up dirt. */
    private static final java.util.List<net.minecraft.world.level.block.state.BlockState> DEBRIS_PALETTE = java.util.List.of(
        net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(),
        net.minecraft.world.level.block.Blocks.BLACKSTONE.defaultBlockState(),
        net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
        net.minecraft.world.level.block.Blocks.BASALT.defaultBlockState()
    );

    private int lifetime = 10; // Matches refreshLifetime: alive only while channeled
    private int teardownTicks = -1; // -1 while channeled, counts down once tearing down

    public DarknessDomainEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DarknessDomainEntity(Level level, LivingEntity caster, Vec3 pos, float radius) {
        super(DbaEntities.DARKNESS_DOMAIN, level);
        this.setOwner(caster);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setRadius(radius);
        this.entityData.set(CASTER_ID, caster != null ? caster.getId() : -1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 8.0f);
        builder.define(CASTER_ID, -1);
        builder.define(FADE, 0.0f);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(RADIUS, radius);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    /** Lifecycle fade 0..1, computed server side through FxEnvelope and synced. */
    public float getFade() {
        return this.entityData.get(FADE);
    }

    public boolean isTearingDown() {
        return this.teardownTicks >= 0;
    }

    /** Start the dissipation phase. Once started it never reverses: a fresh channel spawns a fresh domain. */
    public void beginTeardown() {
        if (this.teardownTicks < 0) {
            this.teardownTicks = TEARDOWN_TICKS;
        }
    }

    public void refreshLifetime() {
        if (!isTearingDown()) {
            this.lifetime = 10; // Keep alive as long as refreshed each tick
        }
    }

    private float buildFade() {
        return FxEnvelope.smoothstep(Math.min(1.0f, this.tickCount / (float) BUILD_TICKS));
    }

    @Override
    public void tick() {
        super.tick();

        Entity owner = this.getOwner();
        if (owner == null && getCasterId() != -1 && this.level() instanceof ServerLevel serverLevel) {
            owner = serverLevel.getEntity(getCasterId());
            if (owner != null) {
                this.setOwner(owner);
            }
        }

        boolean tearingDown = isTearingDown();

        // The dome tracks its caster only while the channel holds. During
        // teardown it stays where the eclipse left it and dissipates.
        if (!tearingDown && owner != null && owner.isAlive()) {
            this.setPos(owner.getX(), owner.getY(), owner.getZ());
        }

        if (this.level().isClientSide()) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) this.level();

        // Lifecycle state machine: refresh expiry or a dead owner starts the
        // teardown instead of an instant discard, so the dome never pops out.
        if (!tearingDown) {
            this.lifetime--;
            if (this.lifetime <= 0 || (owner != null && !owner.isAlive())) {
                beginTeardown();
                tearingDown = true;
            }
        }

        float fade;
        if (tearingDown) {
            fade = buildFade() * FxEnvelope.smoothstep(this.teardownTicks / (float) TEARDOWN_TICKS);
            this.teardownTicks--;
            if (this.teardownTicks < 0) {
                this.entityData.set(FADE, 0.0f);
                this.discard();
                return;
            }
        } else {
            fade = buildFade();
        }
        this.entityData.set(FADE, fade);
        if (fade <= 0.01f) {
            return;
        }

        float radius = getRadius();
        float domeHeight = Math.min(26.0f, radius * 1.35f);
        Vec3 center = this.position();

        // Point 1: layered particles, core motes, theme mid band, wispy rim,
        // gated by the fade so emission breathes with the lifecycle.
        if (serverLevel.getRandom().nextFloat() < fade) {
            LayeredParticles.emitVortex(serverLevel, center, radius, domeHeight,
                100.0f, 200.0f, THEME_ARGB, DbaParticles.VOID_SMOKE);
        }

        // Point 5: as the dome dies, its suspended darkness rains back down.
        if (tearingDown) {
            int elapsed = TEARDOWN_TICKS - this.teardownTicks;
            LayeredParticles.emitRain(serverLevel, center, radius, domeHeight,
                elapsed, TEARDOWN_TICKS, DbaParticles.VOID_SMOKE);
            return;
        }

        // Point 4: real branched strikes inside the dome, roughly one every
        // 14 ticks, striking the ground at a random point under the dome.
        if (serverLevel.getRandom().nextFloat() < 0.07f) {
            double angle = serverLevel.getRandom().nextDouble() * Math.PI * 2.0;
            double r = radius * (0.25 + 0.65 * serverLevel.getRandom().nextDouble());
            Vec3 strike = new Vec3(
                center.x + Math.cos(angle) * r, center.y, center.z + Math.sin(angle) * r);
            BoltEntity bolt = new BoltEntity(serverLevel, strike,
                serverLevel.getRandom().nextInt(), BOLT_COLOR,
                10.0f + serverLevel.getRandom().nextFloat() * 8.0f, 8);
            if (owner instanceof LivingEntity living) {
                bolt.setOwner(living);
            }
            serverLevel.addFreshEntity(bolt);
        }

        // Point 2: debris fields of real ground blocks orbiting the vortex.
        // The opening field is domain class (60 plus pieces at full radius);
        // later waves keep the storm fed while the channel holds.
        if (this.tickCount == 3 || this.tickCount % DEBRIS_WAVE_TICKS == 0) {
            int count = this.tickCount == 3
                ? 44 + (int) (radius * 0.7f)
                : 28;
            LivingEntity debrisOwner = owner instanceof LivingEntity living ? living : null;
            DebrisSpawner.spawnField(serverLevel, debrisOwner, center, count,
                radius * 0.92, domeHeight * 0.5, 170, DEBRIS_PALETTE);
        }
    }
}
