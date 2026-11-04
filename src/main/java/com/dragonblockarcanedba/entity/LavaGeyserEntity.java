package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.dragonblockarcanedba.util.DbaDamage;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A lava geyser (redesign idea 2): a short, violent pillar blasted out of
 * the ground by eruption hits and cleave landings. The column hits once,
 * early, and throws everything in it upward; a beat later it rains three
 * lava blobs around itself, and each blob splats into a cooling pool.
 * The pillar itself changes no blocks, so it needs no field entry.
 */
public class LavaGeyserEntity extends Projectile {
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(LavaGeyserEntity.class, EntityDataSerializers.FLOAT);
    /** Shape seed for the bespoke renderer: synced, so every client draws the same pillar. */
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(LavaGeyserEntity.class, EntityDataSerializers.INT);
    /** Pillar life in ticks: hit at 2, blobs at 3, gone at 18. */
    public static final int LIFE_TICKS = 18;
    private static final double COLUMN_RADIUS = 1.3;
    private static final double COLUMN_HEIGHT = 4.5;

    public LavaGeyserEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public LavaGeyserEntity(Level level, LivingEntity owner, Vec3 pos, float damage) {
        super(DbaEntities.LAVA_GEYSER, level);
        this.setOwner(owner);
        this.noPhysics = true;
        this.entityData.set(DAMAGE, damage);
        this.entityData.set(SEED, level.getRandom().nextInt());
        this.setPos(pos.x, pos.y, pos.z);
    }

    /** Blast a geyser out of the surface cell at {@code surface}. */
    public static void erupt(ServerLevel level, LivingEntity owner, BlockPos surface, float damage) {
        if (surface == null) {
            return;
        }
        level.addFreshEntity(new LavaGeyserEntity(level, owner,
            new Vec3(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5), damage));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DAMAGE, 300.0f);
        builder.define(SEED, 0);
    }

    public float getDamage() {
        return this.entityData.get(DAMAGE);
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
        if (this.tickCount > LIFE_TICKS) {
            this.discard();
            return;
        }
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        Entity owner = this.getOwner();
        float damage = this.entityData.get(DAMAGE);
        if (this.tickCount == 1) {
            server.playSound(null, this.getX(), this.getY(), this.getZ(),
                DbaSounds.RELEASE_BLAST, SoundSource.BLOCKS, 0.8f, 0.7f);
            server.playSound(null, this.getX(), this.getY(), this.getZ(),
                DbaSounds.LAVA_BUBBLE, SoundSource.BLOCKS, 1.0f, 0.6f);
        }
        if (this.tickCount == 2) {
            // The column hit: everything standing in the pillar takes the
            // geyser damage and is thrown straight up out of it.
            AABB column = new AABB(
                this.getX() - COLUMN_RADIUS, this.getY() - 0.2, this.getZ() - COLUMN_RADIUS,
                this.getX() + COLUMN_RADIUS, this.getY() + COLUMN_HEIGHT, this.getZ() + COLUMN_RADIUS);
            List<LivingEntity> hits = server.getEntitiesOfClass(LivingEntity.class, column,
                e -> e.isAlive() && e != owner);
            for (LivingEntity living : hits) {
                living.hurtServer(server, DbaDamage.lava(this, owner),
                    com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(damage));
                living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 60));
                Vec3 motion = living.getDeltaMovement();
                living.setDeltaMovement(motion.x * 0.3, 0.95, motion.z * 0.3);
                com.dragonblockarcanedba.util.DbaCompat.markHurt(living);
            }
        }
        if (this.tickCount == 3) {
            // The rain: three blobs lobbed out to ring the pillar, each one
            // a small pool waiting to happen. Angles come off the synced
            // seed so the fall pattern is the same for every observer.
            LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;
            long seed = this.entityData.get(SEED);
            for (int i = 0; i < 3; i++) {
                double angle = ((seed >> (i * 7)) & 0xFF) / 255.0 * Math.PI * 2.0 + i * 2.1;
                double dist = 2.0 + ((seed >> (i * 5 + 3)) & 0x3F) / 63.0 * 1.6;
                int tx = (int) Math.floor(this.getX() + Math.cos(angle) * dist);
                int tz = (int) Math.floor(this.getZ() + Math.sin(angle) * dist);
                BlockPos surface = LavaField.findSurface(server, tx, tz, (int) Math.floor(this.getY()) + 1);
                Vec3 target = surface != null
                    ? new Vec3(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5)
                    : new Vec3(tx + 0.5, this.getY(), tz + 0.5);
                LavaBlobEntity.launch(server, livingOwner,
                    new Vec3(this.getX(), this.getY() + 3.6, this.getZ()), target, 13, damage * 0.35f);
            }
        }
        // Layered embers riding the pillar for its whole life.
        LayeredParticles.emitVortex(server, this.position(), 0.9f, 3.4f,
            this.tickCount, LIFE_TICKS, 0xFF5A00, DbaParticles.FLAME);
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Damage", getDamage());
        output.putInt("Seed", getSeed());
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DAMAGE, input.getFloatOr("Damage", 300.0f));
        this.entityData.set(SEED, input.getIntOr("Seed", 0));
    }
}
