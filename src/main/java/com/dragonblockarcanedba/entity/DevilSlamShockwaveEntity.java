package com.dragonblockarcanedba.entity;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Devil Slam Shockwave Entity: physical 3D demonic ground-shatter fissure trenches,
 * erupting jagged brimstone and obsidian impalement spikes, expanding demonic impact
 * seals, and a surging vertical hellfire eruption column, triggered by the Devil
 * Trident ground slam deployment.
 *
 * Spectacle pass: the slam now does what its art always promised. On the impact
 * tick it bursts dust and throws real ground debris; as the seal forms it tears a
 * permanent brimstone scar into the ground and punches a real crater at its
 * heart; and at tick 8, when the spikes stand and the seal has grown in, the
 * damage lands with a launch instead of the old downward pin. Nothing is
 * instant: the telegraph grows first, the hit lands at the visual's peak, and
 * the embers fade after.
 */
public class DevilSlamShockwaveEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(DevilSlamShockwaveEntity.class, EntityDataSerializers.FLOAT);

    /** Full visual life. The renderer fades across all of it. */
    public static final int MAX_LIFE = 35;
    /** Tick the slam's damage and launch land: spikes up (8 ticks), seal grown in. */
    private static final int HIT_TICK = 8;
    /** Tick the ground scar tears in, as the trenches open. */
    private static final int SCAR_TICK = 3;
    /** Headline slam damage at point blank, before falloff. */
    private static final float SLAM_DAMAGE = 750.0f;
    /** Crater the slam punches at its heart (destruction expansion). */
    private static final int CRATER_CAP = 160;
    /** Crimson of the pillar and seal, as a dust color for the layered particles. */
    private static final int THEME_ARGB = 0xFFFF3319;

    private boolean hitDone = false;
    private boolean scarDone = false;
    private boolean burstDone = false;

    public DevilSlamShockwaveEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DevilSlamShockwaveEntity(Level level, LivingEntity owner, Vec3 pos, float radius) {
        super(DbaEntities.DEVIL_SLAM_SHOCKWAVE, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setRadius(radius);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 5.0f);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(RADIUS, radius);
    }

    /** The full life of the effect, not the ticks remaining: the renderer fades against this. */
    public int getLifetime() {
        return MAX_LIFE;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int age = this.tickCount;
        Vec3 center = this.position();
        float radius = getRadius();

        if (!this.burstDone && age >= 1) {
            this.burstDone = true;
            // Point 3, base contact: dust burst where the slam meets the ground,
            // and a field of real ground debris thrown up on helical paths.
            com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel, center, radius, THEME_ARGB);
            List<BlockState> palette = new ArrayList<>(
                com.dragonblockarcanedba.util.DebrisSpawner.sampleGroundPalette(serverLevel, center, radius));
            BlockState magma = Blocks.MAGMA_BLOCK.defaultBlockState();
            BlockState blackstone = Blocks.BLACKSTONE.defaultBlockState();
            if (!palette.contains(magma)) palette.add(magma);
            if (!palette.contains(blackstone)) palette.add(blackstone);
            LivingEntity livingOwner = this.getOwner() instanceof LivingEntity le ? le : null;
            com.dragonblockarcanedba.util.DebrisSpawner.spawnField(serverLevel, livingOwner, center,
                30, radius * 0.85, 3.4, 26, palette);
        }

        if (!this.scarDone && age >= SCAR_TICK) {
            this.scarDone = true;
            this.tearGround(serverLevel, center, radius);
        }

        if (!this.hitDone && age >= HIT_TICK) {
            this.hitDone = true;
            this.applySlamHit(serverLevel, center, radius);
        }

        // Points 1 and 5: the hellfire column as layered particles on the shared
        // envelope, core motes, crimson mid band, ember wisps, for the whole life,
        // with ember rain while the slam dissipates.
        com.dragonblockarcanedba.util.LayeredParticles.emitVortex(serverLevel, center,
            radius * 0.8f, 5.5f, age, MAX_LIFE, THEME_ARGB,
            com.dragonblockarcanedba.particle.DbaParticles.EMBER);
        if (age > MAX_LIFE - 12) {
            com.dragonblockarcanedba.util.LayeredParticles.emitRain(serverLevel, center,
                radius, 5.0f, age, MAX_LIFE,
                com.dragonblockarcanedba.particle.DbaParticles.EMBER);
        }

        if (age >= MAX_LIFE) {
            this.discard();
        }
    }

    /**
     * Brimstone scar (standard point 3): the fissure art made real. A torn disk
     * under the slam, magma near the heart, blackstone mid, soul soil at the rim,
     * thinner with distance, all of it permanent, with a real crater punched
     * through the heart first so the conversions pave the crater floor and
     * rim. The cell the owner stands on is never scarred, so the slam never
     * burns its own caster.
     */
    private void tearGround(ServerLevel level, Vec3 center, float radius) {
        BlockPos heartAir = com.dragonblockarcanedba.util.PermanentDestruction.findSurface(level,
            (int) Math.floor(center.x), (int) Math.floor(center.z), (int) Math.floor(center.y));
        if (heartAir != null) {
            com.dragonblockarcanedba.util.PermanentDestruction.crater(level, heartAir.below(), 3.0, 0.5, false,
                com.dragonblockarcanedba.util.PermanentDestruction.TIER_STANDARD, CRATER_CAP, level.getRandom());
        }
        int r = (int) Math.ceil(radius);
        int baseY = (int) Math.floor(center.y);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius || dist < 1.2) {
                    continue;
                }
                BlockState state;
                float chance;
                if (dist <= 3.0) {
                    state = Blocks.MAGMA_BLOCK.defaultBlockState();
                    chance = 0.50f;
                } else if (dist <= 4.6) {
                    state = Blocks.BLACKSTONE.defaultBlockState();
                    chance = 0.38f;
                } else {
                    state = Blocks.SOUL_SOIL.defaultBlockState();
                    chance = 0.22f;
                }
                if (level.getRandom().nextFloat() > chance) {
                    continue;
                }
                BlockPos surface = findSurface(level, m, center.x + dx, center.z + dz, baseY);
                if (surface != null) {
                    com.dragonblockarcanedba.util.PermanentDestruction.convert(level, surface, state);
                }
            }
        }
    }

    /** The top solid block of a column near the slam height, or null if none reads as ground. */
    private BlockPos findSurface(ServerLevel level, BlockPos.MutableBlockPos m, double x, double z, int baseY) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        for (int dy = 3; dy >= -6; dy--) {
            m.set(bx, baseY + dy, bz);
            BlockState state = level.getBlockState(m);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }
            if (!level.getBlockState(m.above()).isAir()) {
                continue;
            }
            return m.immutable();
        }
        return null;
    }

    /**
     * The slam's hit, moved off the cast tick: damage and launch land together at
     * the visual's peak tick, over the same radius the seal draws. Targets are
     * thrown up and out (the standard's launch language, weight scaled through
     * VortexPhysics.fling) instead of pinned into the ground.
     */
    private void applySlamHit(ServerLevel serverLevel, Vec3 center, float radius) {
        Entity ownerEntity = this.getOwner();
        Player owner = ownerEntity instanceof Player p ? p : null;
        AABB aoe = new AABB(
            center.x - radius, center.y - 2.0, center.z - radius,
            center.x + radius, center.y + 4.0, center.z + radius);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, aoe,
            e -> e != ownerEntity && e.isAlive());
        for (LivingEntity t : targets) {
            double dist = Math.sqrt(
                (t.getX() - center.x) * (t.getX() - center.x)
                + (t.getZ() - center.z) * (t.getZ() - center.z));
            if (dist > radius) {
                continue;
            }
            // Falloff: full to half radius, then linear down to a 40 percent floor at the rim.
            double falloff = dist <= radius * 0.5
                ? 1.0
                : Math.max(0.4, 1.0 - (dist - radius * 0.5) / (radius * 0.5) * 0.6);
            var source = com.dragonblockarcanedba.util.DbaDamage.hellfire(this, owner);
            t.hurtServer(serverLevel, source,
                com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (SLAM_DAMAGE * falloff)));
            t.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                com.dragonblockarcanedba.effect.DbaEffects.DEVILS_HANDS_HOLDER, 300, 2, false, true), ownerEntity);
            t.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 300, 0, false, false, false), ownerEntity);

            // MC 26.2 Physics: demonic slam kinetic bounce, unchanged from the old deploy.
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                t,
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                com.dragonblockarcanedba.DragonBlockArcaneDBA.id("devil_slam_bounce"),
                0.85,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
            );
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                t,
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                com.dragonblockarcanedba.DragonBlockArcaneDBA.id("devil_slam_drag"),
                -0.40,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );

            // The launch: up and out, weight scaled. Bosses shrug most of it off,
            // light mobs get thrown clear of the seal.
            com.dragonblockarcanedba.util.VortexPhysics.fling(t, center, 0.85, 1.05);
        }
        if (!targets.isEmpty()) {
            serverLevel.playSound(null, center.x, center.y, center.z,
                com.dragonblockarcanedba.sound.DbaSounds.BOOM_BIG,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 0.55f);
        }
    }
}
