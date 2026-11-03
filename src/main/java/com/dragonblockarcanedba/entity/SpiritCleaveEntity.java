package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
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

/**
 * Spirit Sword's swing: a straight divine blade of light, not a crescent.
 * Targets inside melee reach are left to the sword's own hit so the swing
 * does not stack a second full cut on the body it already struck.
 */
public class SpiritCleaveEntity extends Projectile {
    private static final double MELEE_SKIP = 3.0;

    private float damage = 500.0f;
    private final Set<Integer> hitIds = new HashSet<>();

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
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = this.getDeltaMovement();
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if (this.tickCount > 8) {
                this.discard();
                return;
            }
            Vec3 start = this.position();
            Vec3 end = start.add(motion);
            BlockHitResult block = this.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (block.getType() != HitResult.Type.MISS) {
                this.discard();
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
            }
        }
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
    }
}
