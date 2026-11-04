package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.DbaDamage;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A lava crescent that travels, drips, and leaves a short trail behind it. */
public class LavaCleaveEntity extends Projectile {
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(LavaCleaveEntity.class, EntityDataSerializers.FLOAT);
    /** Shape seed for the bespoke renderer: synced, so every client draws the same crescent. */
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(LavaCleaveEntity.class, EntityDataSerializers.INT);
    /** Flight time in ticks. At 1.35 blocks per tick that is about 16 blocks of reach. */
    private static final int LIFE_TICKS = 12;
    /** Ember and rock palette thrown where the crescent lands. */
    private static final List<BlockState> DEBRIS_PALETTE = List.of(
        Blocks.NETHERRACK.defaultBlockState(),
        Blocks.MAGMA_BLOCK.defaultBlockState(),
        Blocks.BASALT.defaultBlockState(),
        Blocks.BLACKSTONE.defaultBlockState());
    private final Set<Integer> hitIds = new HashSet<>();
    /**
     * The magma wave (redesign idea 4): crust cells laid under the flight
     * path, deduped across ticks, with a hard budget per cleave. The
     * centerline is kept so the flight's end can carve the channel
     * (idea 1) back along it and downhill from it.
     */
    private final Set<Long> wavePlaced = new HashSet<>();
    private final List<BlockPos> groundPath = new ArrayList<>();
    private int waveBudget = 44;
    private boolean channeled;

    public LavaCleaveEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public LavaCleaveEntity(Level level, LivingEntity owner, float damage) {
        super(DbaEntities.LAVA_CLEAVE, level);
        this.setOwner(owner);
        this.noPhysics = true;
        this.entityData.set(DAMAGE, damage);
        this.entityData.set(SEED, level.getRandom().nextInt());
        if (owner != null) {
            Vec3 look = owner.getLookAngle();
            Vec3 start = owner.getEyePosition().add(look.scale(0.8));
            this.setPos(start.x, start.y - 0.2, start.z);
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DAMAGE, 740.0f);
        builder.define(SEED, 0);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
    }

    public int life() {
        return LIFE_TICKS;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    /** Carve the channel back along the flown path, once, however the flight ends. */
    private void finishChannel(ServerLevel server, Entity owner, float damage) {
        if (this.channeled) {
            return;
        }
        this.channeled = true;
        if (!this.groundPath.isEmpty()) {
            LavaField.carveChannel(server, this.groundPath, owner, damage * 0.15f);
        }
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = this.getDeltaMovement();
        if (this.tickCount > LIFE_TICKS) {
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel server) {
                this.finishChannel(server, this.getOwner(), this.entityData.get(DAMAGE));
            }
            this.discard();
            return;
        }
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel server) {
            Vec3 start = this.position();
            Vec3 end = start.add(motion);
            BlockHitResult block = this.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            boolean blocked = block.getType() != HitResult.Type.MISS;
            Entity owner = this.getOwner();
            AABB box = this.getBoundingBox().expandTowards(motion).inflate(1.3, 0.8, 1.3);
            List<LivingEntity> hits = server.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner && !this.hitIds.contains(e.getId()));
            float damage = this.entityData.get(DAMAGE);
            for (LivingEntity living : hits) {
                this.hitIds.add(living.getId());
                living.hurtServer(server, DbaDamage.lava(this, owner), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(damage));
                living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 60));
                // A shove along the flight line, about 0.35 horizontal: the
                // crescent carries weight now instead of passing through.
                living.setDeltaMovement(living.getDeltaMovement().add(motion.x * 0.26, 0.18, motion.z * 0.26));
                com.dragonblockarcanedba.util.DbaCompat.markHurt(living);
            }
            // Layered embers around the crescent (shared standard point 1):
            // bright motes at the core, orange dust mid, flame wisps outside,
            // all breathing with the flight envelope.
            LayeredParticles.emitVortex(server, start, 1.05f, 1.3f,
                this.tickCount, LIFE_TICKS, 0xFF5A00, DbaParticles.FLAME);
            if (this.tickCount >= LIFE_TICKS - 2) {
                // The crescent rains embers as it dies instead of vanishing.
                LayeredParticles.emitRain(server, start, 1.2f, 1.2f,
                    this.tickCount, LIFE_TICKS, DbaParticles.FLAME);
            }
            // The magma wave rolls along under the crescent: raised crust
            // cells three wide across the flight line, WAVE profile, so
            // they sink and cool fast behind the crest. The segment's
            // surface midpoint joins the centerline the channel carves.
            this.waveBudget -= LavaField.waveStep(server, start, end, owner,
                damage * 0.15f, this.wavePlaced, this.waveBudget);
            Vec3 mid = start.add(end).scale(0.5);
            BlockPos midSurface = LavaField.findSurface(server,
                Mth.floor(mid.x), Mth.floor(mid.z), Mth.floor(mid.y));
            if (midSurface != null && (this.groundPath.isEmpty()
                    || !this.groundPath.get(this.groundPath.size() - 1).equals(midSurface))) {
                this.groundPath.add(midSurface);
            }
            if (blocked) {
                Vec3 impact = Vec3.atCenterOf(block.getBlockPos());
                LayeredParticles.emitGroundBurst(server, impact, 1.4f, 0xFF5A00);
                DebrisSpawner.spawnField(server, owner instanceof LivingEntity living ? living : null,
                    impact, 12, 1.6, 1.6, 12, DEBRIS_PALETTE);
                this.finishChannel(server, owner, damage);
                // The landing blasts a mini geyser out of the ground the
                // crescent died on (idea 2, at cleave weight).
                BlockPos landing = LavaField.findSurface(server,
                    block.getBlockPos().getX(), block.getBlockPos().getZ(), block.getBlockPos().getY() + 1);
                LavaGeyserEntity.erupt(server, owner instanceof LivingEntity living ? living : null,
                    landing, damage * 0.45f);
                this.discard();
                return;
            }
        }
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
    }
}
