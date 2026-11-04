package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The ball's wide arc. Facing is locked when the swing starts so a turn does not drag it. */
public class SpikeFlailSwingEntity extends Projectile {
    public static final int SWING_TICKS = 11;
    private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(SpikeFlailSwingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(SpikeFlailSwingEntity.class, EntityDataSerializers.FLOAT);
    private final Set<Integer> hitIds = new HashSet<>();

    public SpikeFlailSwingEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public SpikeFlailSwingEntity(Level level, LivingEntity owner, float damage) {
        super(DbaEntities.SPIKE_FLAIL_SWING, level);
        this.setOwner(owner);
        this.noPhysics = true;
        float yaw = owner != null ? owner.getYRot() : 0.0f;
        this.entityData.set(YAW, yaw);
        this.entityData.set(DAMAGE, damage);
        if (owner != null) {
            Vec3 ball = ballPos(owner, yaw, 0.0f);
            this.setPos(ball.x, ball.y, ball.z);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(YAW, 0.0f);
        builder.define(DAMAGE, 720.0f);
    }

    public float swingYaw() {
        return this.entityData.get(YAW);
    }

    public static float ease(float t) {
        float c = Mth.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
    }

    public static Vec3 ballPos(Entity owner, float yaw, float progress) {
        float eased = ease(progress);
        double side = -1.0 + 2.0 * eased;
        double forward = 1.85 + 0.95 * Math.sin(eased * Math.PI);
        double yawRad = Math.toRadians(yaw);
        double lookX = -Math.sin(yawRad);
        double lookZ = Math.cos(yawRad);
        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);
        double y = owner.getY() + 1.05 + 0.62 * Math.sin(eased * Math.PI);
        return new Vec3(
            owner.getX() + lookX * forward + rightX * side * 2.65,
            y,
            owner.getZ() + lookZ * forward + rightZ * side * 2.65);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > SWING_TICKS) {
            this.discard();
            return;
        }
        Entity owner = this.getOwner();
        if (!(owner instanceof LivingEntity living) || !living.isAlive()) {
            if (owner != null || this.tickCount > 2) {
                this.discard();
            }
            return;
        }
        float progress = this.tickCount / (float) SWING_TICKS;
        Vec3 ball = ballPos(living, this.swingYaw(), progress);
        this.setPos(ball.x, ball.y, ball.z);
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        float damage = this.entityData.get(DAMAGE);
        AABB box = this.getBoundingBox().inflate(1.15);
        List<LivingEntity> hits = server.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != owner && !this.hitIds.contains(e.getId()));
        for (LivingEntity target : hits) {
            if (target.distanceToSqr(this) > 1.7 * 1.7) {
                continue;
            }
            this.hitIds.add(target.getId());
            var source = living instanceof net.minecraft.world.entity.player.Player player
                ? server.damageSources().playerAttack(player)
                : server.damageSources().magic();
            target.hurtServer(server, source, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(damage));
            Vec3 push = ball.subtract(living.position()).normalize().scale(0.55);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.22, push.z));
            com.dragonblockarcanedba.util.DbaCompat.markHurt(target);
        }
    }
}
