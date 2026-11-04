package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.item.SpikeFlailItem;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ball orbits while the flail is held, then cracks outward once the channel ends.
 */
public class SpikeFlailSpinEntity extends Projectile {
    private static final EntityDataAccessor<Float> ANGLE = SynchedEntityData.defineId(SpikeFlailSpinEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(SpikeFlailSpinEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RELEASING = SynchedEntityData.defineId(SpikeFlailSpinEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> RELEASE_AGE = SynchedEntityData.defineId(SpikeFlailSpinEntity.class, EntityDataSerializers.INT);

    private final Map<Integer, Integer> lastHit = new HashMap<>();
    private boolean cracked;

    public SpikeFlailSpinEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public SpikeFlailSpinEntity(Level level, LivingEntity owner) {
        super(DbaEntities.SPIKE_FLAIL_SPIN, level);
        this.setOwner(owner);
        this.noPhysics = true;
        if (owner != null) {
            this.setPos(owner.getX(), owner.getY(), owner.getZ());
            this.setYRot(owner.getYRot());
        }
        this.entityData.set(ANGLE, owner != null ? owner.getYRot() : 0.0f);
        this.entityData.set(RADIUS, 2.55f);
        this.entityData.set(RELEASING, false);
        this.entityData.set(RELEASE_AGE, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ANGLE, 0.0f);
        builder.define(RADIUS, 2.55f);
        builder.define(RELEASING, false);
        builder.define(RELEASE_AGE, 0);
    }

    public float angle() {
        return this.entityData.get(ANGLE);
    }

    public float orbitRadius() {
        return this.entityData.get(RADIUS);
    }

    public boolean releasing() {
        return this.entityData.get(RELEASING);
    }

    public int releaseAge() {
        return this.entityData.get(RELEASE_AGE);
    }

    public Vec3 ballPos() {
        double rad = Math.toRadians(this.angle());
        return new Vec3(Math.cos(rad) * this.orbitRadius(), 1.15, Math.sin(rad) * this.orbitRadius());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = this.getOwner();
        if (!(owner instanceof Player player) || !player.isAlive()) {
            if (owner != null || this.tickCount > 2) {
                this.discard();
            }
            return;
        }
        this.setPos(player.getX(), player.getY(), player.getZ());
        this.setYRot(player.getYRot());
        if (this.level().isClientSide()) {
            return;
        }
        boolean holding = player.isUsingItem() && player.getUseItem().getItem() instanceof SpikeFlailItem;
        if (!holding && !this.releasing()) {
            this.entityData.set(RELEASING, true);
        }
        if (this.releasing()) {
            int age = this.releaseAge() + 1;
            this.entityData.set(RELEASE_AGE, age);
            float grow = 2.55f + age * 0.48f;
            this.entityData.set(RADIUS, grow);
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel server && !this.cracked) {
                this.cracked = true;
                this.crack(server, player);
            }
            if (age > 8) {
                this.discard();
            }
            return;
        }
        this.entityData.set(ANGLE, this.angle() + 32.0f);
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (this.tickCount % 4 != 0) {
            return;
        }
        float strength = player instanceof PlayerStatsAccessor acc ? acc.dba$getStrength() : 0f;
        float damage = 160.0f + strength * 0.35f;
        this.hitNearBall(server, player, damage, 1.25, 8);
    }

    private void hitNearBall(ServerLevel server, Player player, float damage, double reach, int gap) {
        Vec3 local = this.ballPos();
        Vec3 ball = this.position().add(local);
        AABB box = new AABB(ball, ball).inflate(reach);
        List<LivingEntity> hits = server.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != player);
        for (LivingEntity target : hits) {
            int previous = this.lastHit.getOrDefault(target.getId(), -100);
            if (this.tickCount - previous < gap) {
                continue;
            }
            if (target.distanceToSqr(ball) > reach * reach) {
                continue;
            }
            this.lastHit.put(target.getId(), this.tickCount);
            target.hurtServer(server, server.damageSources().playerAttack(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(damage));
            Vec3 push = target.position().subtract(player.position());
            push = new Vec3(push.x, 0.0, push.z);
            if (push.lengthSqr() > 0.01) {
                push = push.normalize().scale(0.35);
                target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.12, push.z));
                com.dragonblockarcanedba.util.DbaCompat.markHurt(target);
            }
        }
    }

    private void crack(ServerLevel server, Player player) {
        float strength = player instanceof PlayerStatsAccessor acc ? acc.dba$getStrength() : 0f;
        float damage = 780.0f + strength * 2.0f;
        AABB box = player.getBoundingBox().inflate(5.4, 1.4, 5.4);
        List<LivingEntity> hits = server.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player);
        for (LivingEntity target : hits) {
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 5.4 || dist < 0.4) {
                continue;
            }
            target.hurtServer(server, server.damageSources().playerAttack(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(damage));
            double scale = 0.85 / dist;
            target.setDeltaMovement(target.getDeltaMovement().add(dx * scale, 0.38, dz * scale));
            com.dragonblockarcanedba.util.DbaCompat.markHurt(target);
        }
    }
}
