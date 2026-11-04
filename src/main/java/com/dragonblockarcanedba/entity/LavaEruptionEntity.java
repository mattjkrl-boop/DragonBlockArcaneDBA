package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A wave of lava that bursts along the swing and fades. */
public class LavaEruptionEntity extends Projectile {
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(LavaEruptionEntity.class, EntityDataSerializers.FLOAT);
    /**
     * Frontal cone edge, as a look dot. This is cos(1.15 radians), the exact
     * half angle LavaEruptionRenderer draws its wave across (about 132
     * degrees in total), so the damage, the ground burn, and the drawn wave
     * all stop at the same edge. It used to be 0.15 (about 162 degrees), so
     * targets at the sides took full damage from a wave that never reached
     * them visually.
     */
    public static final double CONE_MIN_DOT = 0.4085;
    /** Ember and rock palette the eruption throws at cast. */
    private static final List<BlockState> DEBRIS_PALETTE = List.of(
        Blocks.NETHERRACK.defaultBlockState(),
        Blocks.MAGMA_BLOCK.defaultBlockState(),
        Blocks.BASALT.defaultBlockState(),
        Blocks.BLACKSTONE.defaultBlockState());
    private final Set<Integer> hitIds = new HashSet<>();
    /** An eruption may leave a few lava sources along the lip. The rest is fire. */
    private int lavaLeft = 8;

    public LavaEruptionEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public LavaEruptionEntity(Level level, LivingEntity owner, float damage) {
        super(DbaEntities.LAVA_ERUPTION, level);
        this.setOwner(owner);
        this.noPhysics = true;
        this.entityData.set(DAMAGE, damage);
        if (owner != null) {
            this.setPos(owner.getX(), owner.getY() + 0.15, owner.getZ());
            this.setYRot(owner.getYRot());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DAMAGE, 880.0f);
    }

    public int life() {
        return 12;
    }

    public float reachAt(float age) {
        float linear = Math.max(0.0f, Math.min(1.0f, age / this.life()));
        float eased = 1.0f - (1.0f - linear) * (1.0f - linear);
        return 1.1f + eased * 5.4f;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > this.life()) {
            this.discard();
            return;
        }
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        Entity owner = this.getOwner();
        float reach = this.reachAt(this.tickCount);
        float previous = this.reachAt(this.tickCount - 1);
        double yaw = Math.toRadians(this.getYRot());
        double lookX = -Math.sin(yaw);
        double lookZ = Math.cos(yaw);
        AABB box = new AABB(
            this.getX() - reach, this.getY() - 0.2, this.getZ() - reach,
            this.getX() + reach, this.getY() + 2.6, this.getZ() + reach);
        List<LivingEntity> hits = server.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != owner && !this.hitIds.contains(e.getId()));
        float damage = this.entityData.get(DAMAGE);
        for (LivingEntity living : hits) {
            double dx = living.getX() - this.getX();
            double dz = living.getZ() - this.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > reach + 0.4 || dist < previous - 0.7) {
                continue;
            }
            double dot = dist < 0.05 ? 1.0 : (dx * lookX + dz * lookZ) / dist;
            if (dot < CONE_MIN_DOT) {
                continue;
            }
            this.hitIds.add(living.getId());
            var source = owner instanceof net.minecraft.world.entity.player.Player player
                ? server.damageSources().playerAttack(player)
                : server.damageSources().magic();
            living.hurtServer(server, source, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(damage));
            living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 80));
            living.setDeltaMovement(living.getDeltaMovement().add(lookX * 0.60, 0.40, lookZ * 0.60));
            com.dragonblockarcanedba.util.DbaCompat.markHurt(living);
        }
        if (this.tickCount == 1) {
            // Cast beat (shared standard points 2 and 3): dust bursts at the
            // base and two dozen chunks of netherrack, magma, basalt, and
            // blackstone ride up, orbit, and rain back down.
            Vec3 center = this.position();
            LayeredParticles.emitGroundBurst(server, center, 2.0f, 0xFF5A00);
            DebrisSpawner.spawnField(server, owner instanceof LivingEntity living ? living : null,
                center, 24, 3.2, 2.6, 16, DEBRIS_PALETTE);
        }
        // Layered embers riding the wave (shared standard point 1): bright
        // motes at the core, orange dust on the lip, flame wisps outside,
        // breathing with the wave's own envelope.
        LayeredParticles.emitVortex(server, this.position().add(0, 0.2, 0), Math.max(1.2f, reach * 0.8f), 1.8f,
            this.tickCount, this.life(), 0xFF5A00, ParticleTypes.FLAME);
        this.lavaLeft -= LavaSwordGround.scorchArc(server, this.getX(), this.getY(), this.getZ(), this.getYRot(), previous, reach, this.lavaLeft, CONE_MIN_DOT);
    }
}
