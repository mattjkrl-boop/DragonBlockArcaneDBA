package com.dragonblockarcanedba.entity;

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
    private final Set<Integer> hitIds = new HashSet<>();
    /** A cleave may leave a few lava sources. The rest of the path is fire. */
    private int lavaLeft = 3;

    public LavaCleaveEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public LavaCleaveEntity(Level level, LivingEntity owner, float damage) {
        super(DbaEntities.LAVA_CLEAVE, level);
        this.setOwner(owner);
        this.noPhysics = true;
        this.entityData.set(DAMAGE, damage);
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
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = this.getDeltaMovement();
        if (this.tickCount > 10) {
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
                var source = owner instanceof net.minecraft.world.entity.player.Player player
                    ? server.damageSources().playerAttack(player)
                    : server.damageSources().magic();
                living.hurtServer(server, source, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(damage));
                living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 60));
            }
            this.lavaLeft -= LavaSwordGround.scorchSegment(server, start, end, 1.35, this.lavaLeft);
            if (blocked) {
                this.discard();
                return;
            }
        }
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
    }
}
