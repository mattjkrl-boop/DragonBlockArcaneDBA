package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.dragonblockarcanedba.util.DbaDamage;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
 * A lava meteor (redesign idea 5): the finisher's falling rock. It drops
 * fast on a nearly straight line with a slight seeded drift, and on
 * impact it bursts: real area damage through the lava family, a ring of
 * the legacy ground scorch (fire cells), up to five cooling crust pools
 * at the crater, and a throw of hot debris. Every world change it makes
 * goes through LavaSwordGround or LavaField, so the crash-safe restore
 * contract covers all of it.
 */
public class LavaMeteorEntity extends Projectile {
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(LavaMeteorEntity.class, EntityDataSerializers.FLOAT);
    /** Shape and drift seed for the bespoke renderer: synced to all clients. */
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(LavaMeteorEntity.class, EntityDataSerializers.INT);
    private static final double IMPACT_RADIUS = 3.2;
    private static final int MAX_POOLS = 5;
    private static final int HARD_LIFE = 240;
    /** Crater debris palette, the same hot rock the cleave and eruption throw. */
    private static final List<BlockState> DEBRIS_PALETTE = List.of(
        Blocks.NETHERRACK.defaultBlockState(),
        Blocks.MAGMA_BLOCK.defaultBlockState(),
        Blocks.BASALT.defaultBlockState(),
        Blocks.BLACKSTONE.defaultBlockState());

    public LavaMeteorEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public LavaMeteorEntity(Level level, LivingEntity owner, Vec3 pos, float damage) {
        super(DbaEntities.LAVA_METEOR, level);
        this.setOwner(owner);
        this.noPhysics = true;
        this.entityData.set(DAMAGE, damage);
        int seed = level.getRandom().nextInt();
        this.entityData.set(SEED, seed);
        this.setPos(pos.x, pos.y, pos.z);
        // Mostly straight down, with a small seeded drift so a volley of
        // meteors staggers across the target point instead of stacking.
        double driftX = (((seed >> 4) & 0xFF) / 255.0 - 0.5) * 0.22;
        double driftZ = (((seed >> 12) & 0xFF) / 255.0 - 0.5) * 0.22;
        this.setDeltaMovement(driftX, -1.9, driftZ);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DAMAGE, 520.0f);
        builder.define(SEED, 0);
    }

    public float getDamage() {
        return this.entityData.get(DAMAGE);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (this.tickCount > HARD_LIFE) {
            this.discard();
            return;
        }
        server.sendParticles(DbaParticles.FLAME, this.getX(), this.getY() + 0.4, this.getZ(),
            3, 0.3, 0.3, 0.3, 0.02);
        if (this.tickCount % 3 == 0) {
            server.sendParticles(DbaParticles.BLAST, this.getX(), this.getY() + 0.9, this.getZ(),
                1, 0.25, 0.25, 0.25, 0.01);
        }
        Vec3 vel = this.getDeltaMovement();
        Vec3 next = this.position().add(vel);
        var below = net.minecraft.core.BlockPos.containing(next.x, next.y - 0.1, next.z);
        if (vel.y <= 0 && !this.level().getBlockState(below).getCollisionShape(this.level(), below).isEmpty()) {
            this.impact(server, new Vec3(next.x, below.getY() + 1.0, next.z));
            this.discard();
            return;
        }
        this.setPos(next.x, next.y, next.z);
    }

    private void impact(ServerLevel server, Vec3 at) {
        Entity owner = this.getOwner();
        float damage = this.entityData.get(DAMAGE);
        AABB blast = new AABB(
            at.x - IMPACT_RADIUS, at.y - 1.0, at.z - IMPACT_RADIUS,
            at.x + IMPACT_RADIUS, at.y + 2.6, at.z + IMPACT_RADIUS);
        for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class, blast,
                e -> e.isAlive() && e != owner)) {
            living.hurtServer(server, DbaDamage.lava(this, owner),
                com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(damage));
            living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 80));
            double dx = living.getX() - at.x;
            double dz = living.getZ() - at.z;
            double len = Math.max(0.2, Math.sqrt(dx * dx + dz * dz));
            living.setDeltaMovement(living.getDeltaMovement().add(dx / len * 0.5, 0.32, dz / len * 0.5));
            com.dragonblockarcanedba.util.DbaCompat.markHurt(living);
        }
        server.playSound(null, at.x, at.y, at.z, DbaSounds.BOOM_BIG, SoundSource.BLOCKS, 1.2f, 0.8f);
        server.playSound(null, at.x, at.y, at.z, DbaSounds.GROUND_CRACK, SoundSource.BLOCKS, 1.0f, 0.7f);
        LayeredParticles.emitGroundBurst(server, at, 2.2f, 0xFF5A00);
        LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;
        DebrisSpawner.spawnField(server, livingOwner, at, 10, 2.4, 2.2, 14, DEBRIS_PALETTE);
        // The scorch ring: the legacy ground system's burn, ring radius 3,
        // with a small real lava budget of 3 sources. Under the destruction
        // expansion that lava is real and permanent; the fire cells still
        // burn out on the shared 80 tick clock.
        LavaSwordGround.scorchDisk(server, at.x, at.y, at.z, 3.0, 3);
        // The crater pools: center first, then a seeded ring, capped so
        // one meteor can never paint more than five crust cells.
        float touch = damage * 0.12f;
        int cx = Mth.floor(at.x);
        int cz = Mth.floor(at.z);
        int yHint = Mth.floor(at.y);
        int placed = 0;
        if (LavaField.crustColumn(server, cx, cz, yHint, LavaField.PROFILE_POOL, owner, touch)) {
            placed++;
        }
        int seed = this.entityData.get(SEED);
        for (int i = 0; i < 8 && placed < MAX_POOLS; i++) {
            double angle = ((seed >> (i * 3)) & 0x3F) / 63.0 * Math.PI * 2.0 + i * 0.8;
            double dist = 1.2 + ((seed >> (i * 4 + 2)) & 0x1F) / 31.0 * 1.4;
            int px = Mth.floor(at.x + Math.cos(angle) * dist);
            int pz = Mth.floor(at.z + Math.sin(angle) * dist);
            if (LavaField.crustColumn(server, px, pz, yHint, LavaField.PROFILE_POOL, owner, touch)) {
                placed++;
            }
        }
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
        this.entityData.set(DAMAGE, input.getFloatOr("Damage", 520.0f));
        this.entityData.set(SEED, input.getIntOr("Seed", 0));
    }
}
