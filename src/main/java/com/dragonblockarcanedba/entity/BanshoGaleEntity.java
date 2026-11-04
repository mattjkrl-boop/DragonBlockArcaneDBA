package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
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

/**
 * Bansho Fan's swing: a folding fan of wind that opens as it travels.
 * The right-click barrage stays the five wind bolts. This is the fan itself.
 * Body hits inside melee reach stay on hurtEnemy so the gale does not launch twice.
 */
public class BanshoGaleEntity extends Projectile {
    private static final double MELEE_SKIP = 3.0;

    private float damage = 400.0f;
    private final Set<Integer> hitIds = new HashSet<>();

    public BanshoGaleEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BanshoGaleEntity(Level level, LivingEntity owner, float damage) {
        super(DbaEntities.BANSHO_GALE, level);
        this.setOwner(owner);
        this.damage = damage;
        this.noPhysics = true;
        if (owner != null) {
            Vec3 look = owner.getLookAngle();
            Vec3 start = owner.getEyePosition().add(look.scale(0.5));
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
            if (this.tickCount > 10) {
                this.discard();
                return;
            }
            // Block collision: the gale used to fly through solid walls for its
            // full travel while the fan's own wind projectile clips. Same world
            // rules as the projectile now: clip the step, burst on the face.
            Vec3 stepFrom = this.position();
            Vec3 stepTo = stepFrom.add(motion);
            net.minecraft.world.phys.BlockHitResult blockHit = this.level().clip(new net.minecraft.world.level.ClipContext(
                stepFrom, stepTo,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, this
            ));
            if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                Vec3 hitPos = blockHit.getLocation();
                this.setPos(hitPos.x, hitPos.y, hitPos.z);
                Entity owner = this.getOwner();
                BanshoShockwaveEntity burst = new BanshoShockwaveEntity(
                    serverLevel,
                    owner instanceof LivingEntity living ? living : null,
                    hitPos,
                    this.getYRot(),
                    this.getXRot(),
                    2.2f,
                    false // Radial impact burst
                );
                burst.setOrientToImpact(true);
                serverLevel.addFreshEntity(burst);
                com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel, hitPos, 1.6f, 0x26FFA6);
                serverLevel.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                    net.minecraft.sounds.SoundEvents.WIND_CHARGE_BURST.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 1.2f);
                this.discard();
                return;
            }
            Entity owner = this.getOwner();
            AABB box = this.getBoundingBox().expandTowards(motion).inflate(1.6, 0.8, 1.6);
            List<LivingEntity> hits = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner && !this.hitIds.contains(e.getId()));
            for (LivingEntity living : hits) {
                if (owner != null && living.distanceTo(owner) < MELEE_SKIP) {
                    continue;
                }
                Vec3 toTarget = living.position().subtract(this.position());
                if (toTarget.lengthSqr() > 1.0E-4 && motion.lengthSqr() > 1.0E-4) {
                    double dot = motion.normalize().dot(toTarget.normalize());
                    if (dot < 0.35) {
                        continue;
                    }
                }
                this.hitIds.add(living.getId());
                var source = owner instanceof net.minecraft.world.entity.player.Player player
                    ? serverLevel.damageSources().playerAttack(player)
                    : serverLevel.damageSources().magic();
                living.hurtServer(serverLevel, source, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(this.damage));
                living.addEffect(new MobEffectInstance(DbaEffects.BLEEDING_HOLDER, 300, 1, false, true), owner);
                Vec3 launch = motion.lengthSqr() < 1.0E-4 ? new Vec3(0, 0.4, 0) : motion.normalize().scale(1.6);
                living.setDeltaMovement(living.getDeltaMovement().add(launch.x, 0.55, launch.z));
                com.dragonblockarcanedba.util.DbaCompat.markHurt(living);
            }
            if (this.tickCount % 2 == 0) {
                serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.WIND,
                    this.getX(), this.getY(), this.getZ(), 6, 0.8, 0.3, 0.8, 0.04);
            }
        }
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
    }
}
