package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Spirit Sword's swing: a straight divine blade of light, not a crescent.
 * Targets inside melee reach are left to the sword's own hit so the swing
 * does not stack a second full cut on the body it already struck.
 *
 * Spectacle pass: the blade now lives long enough to read. It flies for
 * FLIGHT_TICKS shedding a three layer particle trail (bright mote core, cyan
 * dust mid, gold dust wisps), bursts and throws debris off any block it
 * strikes, and when its flight ends it stops and dissipates over FADE_TICKS
 * instead of vanishing mid-air. LIFE is synced so the renderer runs the
 * exact same envelope the server does.
 */
public class SpiritCleaveEntity extends Projectile {
    private static final double MELEE_SKIP = 3.0;
    public static final int FLIGHT_TICKS = 14;
    public static final int FADE_TICKS = 5;

    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(SpiritCleaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DYING = SynchedEntityData.defineId(SpiritCleaveEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int CYAN = 0x00E5FF;
    private static final int GOLD = 0xFFC83C;

    private float damage = 500.0f;
    private final Set<Integer> hitIds = new HashSet<>();
    private boolean dying = false;
    private int deathTick = -1;

    public SpiritCleaveEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SpiritCleaveEntity(Level level, LivingEntity owner, float damage) {
        super(DbaEntities.SPIRIT_CLEAVE, level);
        this.setOwner(owner);
        this.damage = damage;
        this.noPhysics = true;
        if (owner != null) {
            Vec3 look = owner.getLookAngle();
            Vec3 start = owner.getEyePosition().add(look.scale(0.6));
            this.setPos(start.x, start.y, start.z);
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LIFE, FLIGHT_TICKS + FADE_TICKS);
        builder.define(DYING, false);
    }

    /** Total lifetime in ticks, flight plus dissipation. Set early when a wall ends the flight. */
    public int getLifeTicks() {
        return this.entityData.get(LIFE);
    }

    private void startDying() {
        if (this.dying) return;
        this.dying = true;
        this.deathTick = this.tickCount;
        this.entityData.set(LIFE, this.tickCount + FADE_TICKS);
        this.entityData.set(DYING, true);
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            // The client mirrors the server's motion until the synced DYING
            // flag lands, then holds position for the dissipation like the server.
            if (!this.entityData.get(DYING)) {
                Vec3 motion = this.getDeltaMovement();
                this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
            }
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 motion = this.getDeltaMovement();

        // Dissipation phase: the blade hangs where its flight ended and
        // rains spirit motes down as the envelope closes. No movement, no hits.
        if (this.dying) {
            int elapsed = this.tickCount - this.deathTick;
            if (elapsed >= FADE_TICKS) {
                this.discard();
                return;
            }
            LayeredParticles.emitRain(serverLevel, this.position(), 0.8f, 1.6f,
                elapsed, FADE_TICKS, com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE);
            return;
        }

        if (this.tickCount > FLIGHT_TICKS) {
            startDying();
            return;
        }

        Vec3 start = this.position();
        Vec3 end = start.add(motion);
        BlockHitResult block = this.level().clip(new ClipContext(
            start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (block.getType() != HitResult.Type.MISS) {
            onBlockImpact(serverLevel, block);
            startDying();
            return;
        }

        Entity owner = this.getOwner();
        AABB box = this.getBoundingBox().expandTowards(motion).inflate(0.7, 0.5, 0.7);
        List<LivingEntity> hits = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != owner && !this.hitIds.contains(e.getId()));
        for (LivingEntity living : hits) {
            if (owner != null && living.distanceTo(owner) < MELEE_SKIP) {
                continue;
            }
            this.hitIds.add(living.getId());
            float percent = living.getMaxHealth() * 0.02f;
            var source = owner instanceof net.minecraft.world.entity.player.Player player
                ? serverLevel.damageSources().playerAttack(player)
                : serverLevel.damageSources().magic();
            living.hurtServer(serverLevel, source, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(this.damage + percent));
            living.addEffect(new MobEffectInstance(DbaEffects.SPIRIT_IMPALE_HOLDER, 120, 0, false, true), owner);
            boolean already = !serverLevel.getEntitiesOfClass(SpiritImpaleEntity.class,
                living.getBoundingBox().inflate(2.0), e -> e.getTargetId() == living.getId()).isEmpty();
            if (!already && owner instanceof LivingEntity livingOwner) {
                serverLevel.addFreshEntity(new SpiritImpaleEntity(serverLevel, livingOwner, living, 0.85f));
            }
            // Hit burst: the cut sheds light where it lands.
            serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(),
                5, 0.3, 0.4, 0.3, 0.10);
            serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
                living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(),
                4, 0.25, 0.35, 0.25, 0.05);
        }

        emitTrail(serverLevel, start, motion);
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
    }

    /**
     * Three layer trail with depth (standard point 1): bright motes on the
     * blade itself, cyan dust half a step behind and spread wider, gold wisps
     * a full step behind and spread widest. Counts ride the shared envelope
     * and the effects density config.
     */
    private void emitTrail(ServerLevel serverLevel, Vec3 pos, Vec3 motion) {
        float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
        if (density <= 0f) return;
        float env = FxEnvelope.envelope(this.tickCount, FLIGHT_TICKS + FADE_TICKS, 0.08f, 0.30f);
        if (env <= 0f) return;
        var rand = serverLevel.getRandom();

        int core = Math.max(1, Math.round(2f * density * env));
        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
            pos.x, pos.y, pos.z, core, 0.10, 0.10, 0.10, 0.02);

        Vec3 mid = pos.subtract(motion.scale(0.5));
        int midCount = Math.max(1, Math.round(2f * density * env));
        serverLevel.sendParticles(LayeredParticles.dust(CYAN, 1.3f),
            mid.x, mid.y, mid.z, midCount, 0.22, 0.22, 0.22, 0.01);

        if (this.tickCount % 2 == 0) {
            Vec3 outer = pos.subtract(motion.scale(1.0));
            int outerCount = Math.max(1, Math.round(1f * density * env));
            serverLevel.sendParticles(LayeredParticles.dust(GOLD, 1.6f),
                outer.x, outer.y, outer.z, outerCount, 0.35, 0.35, 0.35, 0.01);
            if (rand.nextFloat() < 0.5f) {
                serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                    outer.x, outer.y, outer.z, 1, 0.3, 0.3, 0.3, 0.04);
            }
        }
    }

    /**
     * The blade used to die silently on the first block it touched. Now the
     * strike bursts at the contact point, throws real debris sampled from
     * the block it hit, and leaves the blade to dissipate in place.
     */
    private void onBlockImpact(ServerLevel serverLevel, BlockHitResult block) {
        Vec3 hit = block.getLocation();
        this.setPos(hit.x, hit.y, hit.z);
        LayeredParticles.emitGroundBurst(serverLevel, hit, 1.2f, CYAN);
        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
            hit.x, hit.y, hit.z, 8, 0.3, 0.3, 0.3, 0.14);
        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
            hit.x, hit.y, hit.z, 6, 0.25, 0.25, 0.25, 0.06);
        BlockState struck = serverLevel.getBlockState(block.getBlockPos());
        Entity owner = this.getOwner();
        DebrisSpawner.spawnField(serverLevel, owner instanceof LivingEntity lo ? lo : null,
            hit, 10, 1.6, 2.4, 16, List.of(struck));
        serverLevel.playSound(null, hit.x, hit.y, hit.z,
            SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.35f);
    }
}
