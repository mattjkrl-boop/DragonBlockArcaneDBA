package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.DbaDamage;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
    /** A cleave may leave a few lava sources. The rest of the path is fire. */
    private int lavaLeft = 4;

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

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = this.getDeltaMovement();
        if (this.tickCount > LIFE_TICKS) {
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
            this.lavaLeft -= LavaSwordGround.scorchSegment(server, start, end, 1.35, this.lavaLeft);
            if (blocked) {
                // The pile belongs on top of the block the cleave landed on.
                // The path scorch only covers the flight segment, whose last
                // midpoint sits inside the hit block, so land it explicitly.
                this.lavaLeft -= LavaSwordGround.scorchLanding(server, block.getBlockPos(), this.lavaLeft);
                Vec3 impact = Vec3.atCenterOf(block.getBlockPos());
                LayeredParticles.emitGroundBurst(server, impact, 1.4f, 0xFF5A00);
                DebrisSpawner.spawnField(server, owner instanceof LivingEntity living ? living : null,
                    impact, 12, 1.6, 1.6, 12, DEBRIS_PALETTE);
                this.discard();
                return;
            }
        }
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
    }
}
