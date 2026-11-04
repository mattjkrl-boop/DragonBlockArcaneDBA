package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.movement.MovementPhysics;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.sound.DbaSounds;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
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
 * Dimensional Warp Rift Entity: a physical 3D spacetime tear spawned at the
 * departure origin and arrival destination of the Dimensional Sword warp.
 *
 * Lifecycle (SPECTACLE-STANDARD point 5): the tear builds first, dragging the
 * world toward itself with VortexPhysics, and the burst (damage, and at the
 * destination the fling, debris field, ground scar, and camera weight) lands
 * at the burst tick, inside the visual's peak window, never on tick 1. The
 * origin rift carries the warp itself: at its burst tick it teleports the
 * owner and opens the destination rift, so crossing up to 1024 blocks has a
 * visible build-up at both ends instead of an instant pop.
 */
public class DimensionalWarpRiftEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> IS_DESTINATION = SynchedEntityData.defineId(DimensionalWarpRiftEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(DimensionalWarpRiftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(DimensionalWarpRiftEntity.class, EntityDataSerializers.INT);
    /** Bespoke geometry seed, rolled once at spawn and synced, so the torn shapes draw identically on every client. */
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(DimensionalWarpRiftEntity.class, EntityDataSerializers.INT);

    /** Origin: a shorter tear, it only has to let the player out. */
    public static final int ORIGIN_LIFE = 34;
    public static final int ORIGIN_BURST_TICK = 10;
    /** Destination: the arrival is the spectacle, so it lives and dissipates longer. */
    public static final int DEST_LIFE = 46;
    public static final int DEST_BURST_TICK = 12;
    /** Kept for compatibility with older call sites; per-instance life is in LIFE. */
    public static final int MAX_LIFETIME = DEST_LIFE;

    /** Theme dust color: the sword's violet magenta. */
    public static final int THEME_ARGB = 0xCC26D9;

    /** Shards of torn ground and space at the origin, where the world gets pulled apart. */
    private static final List<BlockState> VOID_PALETTE = List.of(
        Blocks.OBSIDIAN.defaultBlockState(),
        Blocks.CRYING_OBSIDIAN.defaultBlockState(),
        Blocks.AMETHYST_BLOCK.defaultBlockState()
    );

    private float burstDamage = 750.0f;
    private boolean burstDone = false;

    // Warp payload, origin rifts only: where the owner lands, where the
    // destination tear opens, and how hard that end hits.
    private boolean warpArmed = false;
    private Vec3 warpLanding = null;
    private Vec3 warpRiftPos = null;
    private float warpDestDamage = 0f;
    private float warpDestRadius = 0f;

    public DimensionalWarpRiftEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DimensionalWarpRiftEntity(Level level, LivingEntity owner, Vec3 pos, float radius, boolean isDestination, float damage) {
        super(DbaEntities.DIMENSIONAL_WARP_RIFT, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.burstDamage = damage;
        this.noPhysics = true;
        this.entityData.set(RADIUS, radius);
        this.entityData.set(IS_DESTINATION, isDestination);
        this.entityData.set(LIFE, isDestination ? DEST_LIFE : ORIGIN_LIFE);
        this.entityData.set(SEED, level.getRandom().nextInt());
    }

    /** Arm this origin rift to carry a warp. Called by the item before the rift enters the world. */
    public void armWarp(Vec3 landing, Vec3 riftPos, float destDamage, float destRadius) {
        this.warpArmed = true;
        this.warpLanding = landing;
        this.warpRiftPos = riftPos;
        this.warpDestDamage = destDamage;
        this.warpDestRadius = destRadius;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IS_DESTINATION, false);
        builder.define(RADIUS, 3.5f);
        builder.define(LIFE, ORIGIN_LIFE);
        builder.define(SEED, 0);
    }

    public boolean isDestination() {
        return this.entityData.get(IS_DESTINATION);
    }

    public float getRiftRadius() {
        return this.entityData.get(RADIUS);
    }

    public int getMaxLifetime() {
        return this.entityData.get(LIFE);
    }

    /** Synced bespoke geometry seed for this rift. */
    public int getSeed() {
        return this.entityData.get(SEED);
    }

    public int getBurstTick() {
        return this.isDestination() ? DEST_BURST_TICK : ORIGIN_BURST_TICK;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 center = this.position();
        Entity owner = this.getOwner();
        float radius = this.getRiftRadius();
        boolean isDest = this.isDestination();
        int life = this.getMaxLifetime();
        int burstTick = this.getBurstTick();
        LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;

        // The shard fan is real (ENTITY-STANDARD): on the first tick, four
        // torn-space fragments burst from the tear's heart as entities and
        // scatter, sink, and settle, matching the fan the renderer used to
        // draw (same seed family, unit, cone, and colors). Origin rifts get
        // 60 ticks of fragment life, destination rifts 72, host plus fallout.
        if (this.tickCount == 1) {
            FragmentEntity.burst(serverLevel, owner, center.add(0, 1.4, 0), new Vec3(0, 1, 0),
                68f, 0.70f, 4, this.getSeed() ^ 0x5A17L, 0x1A0533, 0xE8CCFF,
                0.12f, 0.32f, 0.6f, isDest ? 72 : 60, 8);
        }

        // Audio cue on initial spawn, all from the mod's own bank
        // (BESPOKE-STANDARD rule 3).
        if (this.tickCount == 1) {
            if (!isDest) {
                serverLevel.playSound(null, center.x, center.y, center.z,
                    DbaSounds.TELEPORT_WHOOSH, SoundSource.PLAYERS, 2.0f, 0.7f);
                serverLevel.playSound(null, center.x, center.y, center.z,
                    DbaSounds.DARK_PULSE, SoundSource.PLAYERS, 2.0f, 0.7f);
                serverLevel.playSound(null, center.x, center.y, center.z,
                    DbaSounds.CHARGE_DARK, SoundSource.PLAYERS, 1.8f, 0.6f);
                serverLevel.playSound(null, center.x, center.y, center.z,
                    DbaSounds.RIFT_OPEN, SoundSource.PLAYERS, 1.5f, 1.4f);
            } else {
                serverLevel.playSound(null, center.x, center.y, center.z,
                    DbaSounds.BOOM_SONIC, SoundSource.PLAYERS, 2.2f, 0.8f);
                serverLevel.playSound(null, center.x, center.y, center.z,
                    DbaSounds.WIND_GUST, SoundSource.PLAYERS, 2.2f, 0.8f);
                serverLevel.playSound(null, center.x, center.y, center.z,
                    DbaSounds.RELEASE_DARK, SoundSource.PLAYERS, 2.0f, 0.6f);
                serverLevel.playSound(null, center.x, center.y, center.z,
                    DbaSounds.BOOM_BIG, SoundSource.PLAYERS, 1.5f, 0.5f);
            }
        }

        // Layered particles for the whole life (point 1): bright core motes,
        // a theme mid band, void smoke wisps at the rim, all breathing with
        // the shared envelope, plus a spark drizzle while the tear dissipates.
        LayeredParticles.emitVortex(serverLevel, center.add(0, 0.1, 0), radius * 0.8f, 3.2f,
            this.tickCount, life, THEME_ARGB, DbaParticles.VOID_SMOKE);
        float progress = (float) this.tickCount / (float) life;
        if (progress > 0.65f) {
            LayeredParticles.emitRain(serverLevel, center, radius, 2.6f,
                this.tickCount, life, DbaParticles.SPARK);
        }

        // Build-up and aftershock forces (point 3): before the burst the tear
        // drags everything toward itself, the destination harder to escape
        // because the arrival is coming; after the burst only a fading swirl
        // remains. Weight classes apply, so bosses barely move.
        if (!burstDone) {
            float build = Math.min(1.0f, this.tickCount / (float) burstTick);
            VortexPhysics.applyVortex(serverLevel, center, radius, 3.0, livingOwner,
                isDest ? 0.20 : 0.32, 0.8, isDest ? 0.02 : 0.05, build);
        } else {
            float env = FxEnvelope.envelope(this.tickCount, life);
            if (env > 0f) {
                VortexPhysics.applyVortex(serverLevel, center, radius, 3.0, livingOwner,
                    0.10, 0.8, 0.02, env);
            }
        }

        if (!burstDone && this.tickCount >= burstTick) {
            burstDone = true;
            doBurst(serverLevel, center, radius, isDest, livingOwner);
        }

        if (this.tickCount >= life) {
            this.discard();
        }
    }

    /**
     * The burst: the one damaging moment, fired at the visual's peak tick.
     * Damage, Melting, and the friction and drag distortion land together;
     * the destination also flings its victims out, throws a debris field of
     * the ground it tore open, scars that ground, and carries camera weight.
     * An armed origin rift teleports its owner at this same tick and opens
     * the destination rift.
     */
    private void doBurst(ServerLevel serverLevel, Vec3 center, float radius, boolean isDest, LivingEntity livingOwner) {
        Entity owner = this.getOwner();
        AABB aoeBox = new AABB(
            center.x - radius, center.y - 1.5, center.z - radius,
            center.x + radius, center.y + 2.5, center.z + radius
        );
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
            LivingEntity.class, aoeBox,
            e -> e.isAlive() && e != owner && center.distanceTo(e.position()) <= radius
        );

        for (LivingEntity target : targets) {
            // Ability damage uses the mod's void type (BESPOKE-STANDARD
            // rule 4); only the plain melee hit keeps the vanilla source.
            DamageSource damageSource = DbaDamage.voidDmg(this, owner);

            target.hurtServer(serverLevel, damageSource, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(this.burstDamage));
            target.addEffect(new MobEffectInstance(DbaEffects.MELTING_HOLDER, 200, 1, false, true), owner);

            // MC 26.2 Physics: Spatial rift warp friction & drag distortion
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                target,
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID,
                com.dragonblockarcanedba.DragonBlockArcaneDBA.id("dimensional_wave_friction"),
                -0.90,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                target,
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                com.dragonblockarcanedba.DragonBlockArcaneDBA.id("dimensional_wave_drag"),
                -0.80,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );

            if (isDest) {
                // Arrival ejects: everything the tear dragged in gets thrown out.
                VortexPhysics.fling(target, center, 0.95, 0.5);
            }
        }

        if (isDest) {
            // The arrival tears the ground it lands on: debris field of the
            // real terrain, a permanent conversion disk, a shallow crater
            // the rift mouth eats at its heart, a base burst, camera weight.
            DebrisSpawner.spawnField(serverLevel, livingOwner, center, 24, radius, 3.5, 36);
            convertGroundDisk(serverLevel, center, 2);
            BlockPos mouthAir = PermanentDestruction.findSurface(serverLevel,
                (int) Math.floor(center.x), (int) Math.floor(center.z), (int) Math.floor(center.y));
            if (mouthAir != null) {
                PermanentDestruction.crater(serverLevel, mouthAir.below(), 2.0, 0.5, false,
                    PermanentDestruction.TIER_WEAK, 50, serverLevel.getRandom());
            }
            LayeredParticles.emitGroundBurst(serverLevel, center, radius, THEME_ARGB);
            com.dragonblockarcanedba.fx.DbaImpactFx.medium(serverLevel, center.x, center.y, center.z);
        } else {
            // Departure: the ground around the tear cracks as the player is
            // pulled out of the world, and void shards swirl into the closing cut.
            DebrisSpawner.spawnField(serverLevel, livingOwner, center, 12, radius * 0.8, 2.4, 28, VOID_PALETTE);
            convertGroundDisk(serverLevel, center, 1);
            LayeredParticles.emitGroundBurst(serverLevel, center, radius * 0.7f, THEME_ARGB);
        }

        // The warp itself: the owner crosses at the burst tick, exactly when
        // the departure shockwave fires, and the destination tear opens at the
        // far end to build toward its own burst. RIFTED marks the crossing;
        // the ability no longer DARK_FADEDs its own user.
        if (warpArmed && owner instanceof ServerPlayer serverPlayer
            && serverPlayer.isAlive() && serverPlayer.level() == serverLevel) {
            MovementPhysics.teleport(serverPlayer, warpLanding);
            serverPlayer.addEffect(new MobEffectInstance(DbaEffects.RIFTED_HOLDER, 60, 0, false, false));
            DimensionalWarpRiftEntity destRift = new DimensionalWarpRiftEntity(
                serverLevel, serverPlayer, warpRiftPos, warpDestRadius, true, warpDestDamage
            );
            serverLevel.addFreshEntity(destRift);
            serverLevel.playSound(null, warpLanding.x, warpLanding.y, warpLanding.z,
                DbaSounds.TELEPORT_WHOOSH, SoundSource.PLAYERS, 1.2f, 0.8f);
        }
    }

    /** The top solid block under a point, scanning down a short way; null when the point hangs over open air or void. */
    private BlockPos findSurface(ServerLevel level, double x, double y, double z) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        int startY = (int) Math.floor(y) + 2;
        for (int yy = startY; yy >= startY - 8 && yy > level.getMinY(); yy--) {
            m.set(bx, yy, bz);
            BlockState state = level.getBlockState(m);
            if (!state.isAir() && state.getFluidState().isEmpty()
                && !state.getCollisionShape(level, m).isEmpty()) {
                return m.immutable();
            }
        }
        return null;
    }

    /**
     * Convert a disk of real ground cells to crying obsidian permanently,
     * following the terrain column by column so nothing floats on slopes.
     * PermanentDestruction refuses protected blocks itself.
     */
    private void convertGroundDisk(ServerLevel level, Vec3 center, int radius) {
        BlockState scarState = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                if (level.getRandom().nextFloat() > 0.6f) continue;
                BlockPos surface = findSurface(level, center.x + dx, center.y, center.z + dz);
                if (surface != null) {
                    PermanentDestruction.convert(level, surface, scarState);
                }
            }
        }
    }
}
