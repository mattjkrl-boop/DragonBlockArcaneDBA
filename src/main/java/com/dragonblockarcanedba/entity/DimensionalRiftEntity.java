package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.dragonblockarcanedba.util.DbaDamage;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.dragonblockarcanedba.util.GroundScar;
import com.dragonblockarcanedba.util.LayeredParticles;
import com.dragonblockarcanedba.util.VortexPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
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
 * Dimensional Rift Entity: the Sickle of Sorrow's 15 block radius gravitational
 * vortex domain. Lifecycle runs on the shared FxEnvelope across 60 ticks:
 * expansion over the first 20 ticks, a 30 tick hold at full size, then collapse
 * from tick 50, where the implosion flings everything the vortex caught.
 * Pull is the shared VortexPhysics (weight classed, swirl plus core lift),
 * particles are the shared LayeredParticles three layer emitter, the cast
 * throws a DebrisSpawner field of soul themed blocks on helical orbits, and
 * the ground under the rift is temporarily scarred to soul soil via GroundScar.
 */
public class DimensionalRiftEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(DimensionalRiftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_COLLAPSING = SynchedEntityData.defineId(DimensionalRiftEntity.class, EntityDataSerializers.BOOLEAN);

    public static final double MAX_AOE_RADIUS = 15.0;
    public static final int MAX_LIFETIME = 60; // 3 seconds of active gravitational vortex
    public static final int EXPAND_TICKS = 20; // radius 3 to 15 across the build-up
    public static final int COLLAPSE_TICK = 50; // implosion and fling land here
    public static final float ENVELOPE_BUILD_FRAC = EXPAND_TICKS / (float) MAX_LIFETIME;
    public static final float ENVELOPE_DECAY_FRAC = (MAX_LIFETIME - COLLAPSE_TICK) / (float) MAX_LIFETIME;

    /** Theme color for the layered particle mid band: the rift's violet. */
    public static final int THEME_ARGB = 0x9900E6;

    private static final float TICK_DAMAGE = 60.0f;
    private static final int MELTING_MAX_AMPLIFIER = 3; // Melting IV at point blank
    private static final int MELTING_DURATION_TICKS = 200; // 10 seconds, refreshed while inside

    private float burstDamage = 500.0f;
    private boolean burstFired = false;
    private boolean fieldSpawned = false;

    public DimensionalRiftEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DimensionalRiftEntity(Level level, LivingEntity owner, Vec3 pos, float burstDamage) {
        super(DbaEntities.DIMENSIONAL_RIFT, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.burstDamage = burstDamage;
        this.noPhysics = true;
        this.setRadius(3.0f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 3.0f);
        builder.define(IS_COLLAPSING, false);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(RADIUS, Math.min(15.0f, Math.max(1.0f, radius)));
    }

    public boolean isCollapsing() {
        return this.entityData.get(IS_COLLAPSING);
    }

    /** Envelope shared by the pull, the particles, and the renderer, so all three breathe together. */
    public float envelope() {
        return FxEnvelope.envelope(this.tickCount, MAX_LIFETIME, ENVELOPE_BUILD_FRAC, ENVELOPE_DECAY_FRAC);
    }

    private float currentRadius() {
        if (this.tickCount < EXPAND_TICKS) {
            float t = FxEnvelope.smoothstep(this.tickCount / (float) EXPAND_TICKS);
            return 3.0f + t * 12.0f; // Expands 3.0 -> 15.0
        }
        if (this.tickCount >= COLLAPSE_TICK) {
            float t = FxEnvelope.smoothstep((this.tickCount - COLLAPSE_TICK) / (float) (MAX_LIFETIME - COLLAPSE_TICK));
            return 15.0f - t * 8.0f; // Implosion contracts 15.0 -> 7.0
        }
        return 15.0f;
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 center = this.position();
        Entity owner = this.getOwner();
        LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            float currentRadius = currentRadius();
            this.setRadius(currentRadius);
            float envelope = envelope();

            if (this.tickCount >= COLLAPSE_TICK) {
                this.entityData.set(IS_COLLAPSING, true);
            }

            // Cast beat (tick 1): base dust burst, the debris field, and the ground scar.
            if (this.tickCount == 1) {
                LayeredParticles.emitGroundBurst(serverLevel, center, 3.0f, THEME_ARGB);
                spawnDebrisField(serverLevel, center, livingOwner);
                scarGround(serverLevel, center);
            }

            // Layered particles every tick: bright core, violet helical mid band,
            // void smoke wisps at the rim, all scaled by the shared envelope.
            LayeredParticles.emitVortex(serverLevel, center, currentRadius * 0.85f, 6.5f,
                this.tickCount, MAX_LIFETIME, THEME_ARGB,
                com.dragonblockarcanedba.particle.DbaParticles.VOID_SMOKE);
            // Dissipation drizzle while the rift collapses: soul smoke raining back down.
            if (this.tickCount >= COLLAPSE_TICK) {
                LayeredParticles.emitRain(serverLevel, center, currentRadius, 6.5f,
                    this.tickCount, MAX_LIFETIME,
                    com.dragonblockarcanedba.particle.DbaParticles.VOID_SMOKE);
            }

            // The pull itself: shared vortex physics, weight classed so bosses
            // barely move and players keep some control. Swirl plus core lift
            // replaces the old straight line drag.
            VortexPhysics.applyVortex(serverLevel, center, currentRadius, 7.0,
                livingOwner, 0.5, 0.85, 0.55, envelope);

            // Collapse beat: the implosion flings everything the vortex caught.
            if (this.tickCount == COLLAPSE_TICK) {
                List<LivingEntity> caught = targetsIn(serverLevel, center, currentRadius, owner);
                for (LivingEntity target : caught) {
                    VortexPhysics.fling(target, center, 1.5, 0.85);
                }
                float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
                if (density > 0f) {
                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.VOID_SMOKE,
                        center.x, center.y + 0.8, center.z, Math.max(2, Math.round(14f * density)),
                        currentRadius * 0.5, 0.8, currentRadius * 0.5, 0.05);
                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                        center.x, center.y + 1.2, center.z, Math.max(2, Math.round(10f * density)),
                        currentRadius * 0.3, 0.8, currentRadius * 0.3, 0.08);
                }
                serverLevel.playSound(null, center.x, center.y, center.z,
                    DbaSounds.RELEASE_DARK, SoundSource.PLAYERS, 2.0f, 0.7f);
            }

            // Effects and damage on everything inside the current radius.
            List<LivingEntity> targets = targetsIn(serverLevel, center, currentRadius, owner);
            boolean burstNow = this.tickCount == EXPAND_TICKS && !this.burstFired;
            if (burstNow) this.burstFired = true;
            boolean tickDamageNow = this.tickCount % 10 == 5 && this.tickCount >= 15 && this.tickCount < COLLAPSE_TICK;

            for (LivingEntity target : targets) {
                double distance = center.distanceTo(target.position());

                // Distance-based Melting: closer is a higher tier, capped at
                // amplifier 3 (Melting IV) for a fixed 10 seconds, in family
                // with the rest of the set instead of up to 57 seconds.
                int amplifier = Math.max(0, MELTING_MAX_AMPLIFIER
                    - (int) Math.floor(distance * (MELTING_MAX_AMPLIFIER + 1) / MAX_AOE_RADIUS));
                target.addEffect(new MobEffectInstance(DbaEffects.MELTING_HOLDER, MELTING_DURATION_TICKS, amplifier, false, true), owner);

                // Sorrow Rift corruption and Cinematic Damage Lock
                target.addEffect(new MobEffectInstance(DbaEffects.SORROW_RIFT_HOLDER, 200, 0, false, true), owner);
                target.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 40, 0, false, false, false), owner);

                // MC 26.2 Physics Attributes: Friction and Air Drag
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    target,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("sickle_rift_friction"),
                    -0.80,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    target,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("sickle_rift_drag"),
                    3.0,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );

                // The burst lands at the visual's peak tick, when expansion
                // completes, on everything inside at that moment. Ability
                // damage uses the mod's void type (bespoke rule 4), with this
                // rift as the direct entity.
                if (burstNow) {
                    DamageSource damageSource = DbaDamage.voidDmg(this, owner);

                    target.hurtServer(serverLevel, damageSource, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(this.burstDamage));

                    // Per-target burst feedback so caught targets read as hit, not just hurt.
                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        10, 0.4, 0.5, 0.4, 0.05);
                    serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                        DbaSounds.SOUL_DRIFT, SoundSource.PLAYERS, 0.9f, 0.6f);

                    // Soul Rend heal from AOE: 1% of burst damage per target hit
                    if (owner instanceof Player playerOwner) {
                        playerOwner.heal(this.burstDamage * 0.01f);
                    }
                }

                // Sustained tick damage spread across the hold, never after the visual ends.
                if (tickDamageNow) {
                    DamageSource tickSource = DbaDamage.voidDmg(this, owner);
                    target.hurtServer(serverLevel, tickSource, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(TICK_DAMAGE));
                }
            }

            // Ambient audio cues (bespoke bank: sonic boom -> boom sonic,
            // anchor set spawn -> dark charge rise, portal -> rift ambient)
            if (this.tickCount == 1) {
                serverLevel.playSound(null, center.x, center.y, center.z,
                    DbaSounds.BOOM_SONIC, SoundSource.PLAYERS, 2.5f, 0.6f);
                serverLevel.playSound(null, center.x, center.y, center.z,
                    DbaSounds.WIND_GUST, SoundSource.PLAYERS, 2.5f, 0.6f);
                serverLevel.playSound(null, center.x, center.y, center.z,
                    DbaSounds.CHARGE_DARK, SoundSource.PLAYERS, 2.0f, 0.5f);
            }
            if (this.tickCount % 20 == 0) {
                serverLevel.playSound(null, center.x, center.y, center.z,
                    DbaSounds.RIFT_AMBIENT, SoundSource.PLAYERS, 1.2f, 0.7f + (currentRadius / 15.0f) * 0.3f);
            }

            if (this.tickCount >= MAX_LIFETIME) {
                this.discard();
            }
        }
    }

    private List<LivingEntity> targetsIn(ServerLevel serverLevel, Vec3 center, float radius, Entity owner) {
        AABB aoeBox = this.getBoundingBox().inflate(radius, 7.0, radius);
        return serverLevel.getEntitiesOfClass(
            LivingEntity.class, aoeBox,
            e -> e.isAlive() && e != owner && center.distanceTo(e.position()) <= radius
        );
    }

    /**
     * The debris field: soul themed blocks (soul sand, soul soil, basalt,
     * blackstone, crying obsidian) blended with blocks sampled from the ground
     * the rift opened on, thrown on helical orbits that release and rain back
     * down as the rift collapses.
     */
    private void spawnDebrisField(ServerLevel serverLevel, Vec3 center, LivingEntity owner) {
        if (this.fieldSpawned) return;
        this.fieldSpawned = true;
        float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
        if (density <= 0f) return;
        List<BlockState> palette = new ArrayList<>(List.of(
            Blocks.SOUL_SAND.defaultBlockState(),
            Blocks.SOUL_SOIL.defaultBlockState(),
            Blocks.BASALT.defaultBlockState(),
            Blocks.BLACKSTONE.defaultBlockState(),
            Blocks.CRYING_OBSIDIAN.defaultBlockState()
        ));
        for (BlockState ground : DebrisSpawner.sampleGroundPalette(serverLevel, center, 8.0)) {
            if (!palette.contains(ground)) palette.add(ground);
        }
        int count = Math.max(8, Math.round(64f * density));
        DebrisSpawner.spawnField(serverLevel, owner, center, count, 11.0, 6.5, 52, palette);
    }

    /**
     * Temporary ground scarring: the surface under the rift weeps into soul
     * soil for a while, then GroundScar restores the real ground. Finds the
     * surface below the rift center first; a rift opened high in the air
     * scars nothing.
     */
    private void scarGround(ServerLevel serverLevel, Vec3 center) {
        BlockPos base = BlockPos.containing(center.x, center.y, center.z);
        for (int dy = 0; dy <= 8; dy++) {
            BlockPos candidate = base.below(dy);
            BlockState state = serverLevel.getBlockState(candidate);
            if (state.isAir() || state.getCollisionShape(serverLevel, candidate).isEmpty()) continue;
            BlockState above = serverLevel.getBlockState(candidate.above());
            if (!above.isAir() && !above.getCollisionShape(serverLevel, candidate.above()).isEmpty()) continue;
            GroundScar.scarDisk(serverLevel, candidate, 4, Blocks.SOUL_SOIL.defaultBlockState(),
                300, 0.45f, serverLevel.getRandom());
            return;
        }
    }
}
