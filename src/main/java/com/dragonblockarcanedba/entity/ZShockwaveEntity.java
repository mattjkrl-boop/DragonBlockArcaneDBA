package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Z Shockwave Entity — Massive horizontal energy wave unleashed by the Z Sword.
 * 
 * Features:
 * - Extremely wide hitbox that scales with charge.
 * - Heavy damage and devastating outward knockback.
 * - Tweak A: Spawns slower trailing sub-waves at max charge.
 * - Tweak B: Ignores terrain and penetrates solid blocks at max charge.
 * - Tweak C: Direct max-charge hits bury enemies 2 blocks into the ground, dealing head-in-blocks crush damage.
 */
public class ZShockwaveEntity extends Projectile {
    private static final EntityDataAccessor<Float> CHARGE_RATIO = SynchedEntityData.defineId(ZShockwaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_SUB_WAVE = SynchedEntityData.defineId(ZShockwaveEntity.class, EntityDataSerializers.BOOLEAN);

    private float damage = 500.0f;
    private boolean pierceBlocks = false;
    private boolean rootOnHit = false;
    private boolean hasSpawnedSubWaves = false;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public ZShockwaveEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public ZShockwaveEntity(Level level, LivingEntity owner, float chargeRatio, float damage, boolean isSubWave) {
        super(DbaEntities.Z_SHOCKWAVE, level);
        this.setOwner(owner);
        if (owner != null) {
            this.setPos(owner.getX(), owner.getY() + 0.8, owner.getZ());
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
        }
        this.damage = damage;
        this.noPhysics = true;

        this.entityData.set(CHARGE_RATIO, chargeRatio);
        this.entityData.set(IS_SUB_WAVE, isSubWave);

        // Tweak B & C are unlocked at maximum charge (>= 90%)
        if (chargeRatio >= 0.90f && !isSubWave) {
            this.pierceBlocks = true;
            this.rootOnHit = true;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CHARGE_RATIO, 1.0f);
        builder.define(IS_SUB_WAVE, false);
    }

    public float getChargeRatio() {
        return this.entityData.get(CHARGE_RATIO);
    }

    public boolean isSubWave() {
        return this.entityData.get(IS_SUB_WAVE);
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 movement = this.getDeltaMovement();
        double nextX = this.getX() + movement.x;
        double nextY = this.getY() + movement.y;
        double nextZ = this.getZ() + movement.z;
        this.setPos(nextX, nextY, nextZ);

        float charge = getChargeRatio();
        boolean subWave = isSubWave();

        // Calculate wave width (scaled up to 16 blocks wide for max charge main wave)
        double halfWidth = subWave ? (2.0 + charge * 3.0) : (3.0 + charge * 5.0);
        double halfHeight = 1.6;

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Tweak A: Spawn trailing sub-waves at max charge
            if (charge >= 0.90f && !subWave && !hasSpawnedSubWaves && this.tickCount == 3) {
                this.hasSpawnedSubWaves = true;
                Vec3 look = movement.normalize();
                Vec3 basePos = this.position();

                for (int i = 1; i <= 2; i++) {
                    ZShockwaveEntity trailWave = new ZShockwaveEntity(this.level(), (LivingEntity) this.getOwner(), charge * 0.7f, this.damage * 0.45f, true);
                    trailWave.setPos(basePos.x - look.x * (i * 2.0), basePos.y, basePos.z - look.z * (i * 2.0));
                    trailWave.setDeltaMovement(look.scale(movement.length() * 0.65));
                    serverLevel.addFreshEntity(trailWave);
                }
            }

            // Hit detection along wide horizontal box
            AABB hitbox = this.getBoundingBox().inflate(halfWidth, halfHeight, halfWidth);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, hitbox, e -> e.isAlive() && e != this.getOwner() && !hitEntityIds.contains(e.getId()));

            Entity owner = this.getOwner();
            for (LivingEntity target : targets) {
                hitEntityIds.add(target.getId());

                // Deal massive damage
                target.hurtServer(serverLevel, serverLevel.damageSources().mobProjectile(this, owner instanceof LivingEntity ? (LivingEntity) owner : null), this.damage);

                // Heavy knockback
                Vec3 knockback = movement.normalize().scale(1.8 + (charge * 2.2));
                target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.4 + (charge * 0.3), knockback.z));
                target.hurtMarked = true;

                // Tweak C: Maximum charge roots enemies into the ground 2 blocks down and crushes them
                if (this.rootOnHit && !subWave && charge >= 0.90f) {
                    BlockPos targetPos = target.blockPosition();
                    BlockPos below2 = targetPos.below(2);
                    
                    // Teleport 2 blocks down into the ground
                    target.teleportTo(target.getX(), target.getY() - 2.0, target.getZ());
                    target.addEffect(new MobEffectInstance(DbaEffects.MOVEMENT_CURSE_HOLDER, 80, 9, false, true));
                    // Suffocation / block crush damage from head embedded in terrain
                    target.hurtServer(serverLevel, serverLevel.damageSources().inWall(), 250.0f);
                }
            }

            // Particle wave front on server
            if (this.tickCount % 2 == 0) {
                Vec3 look = movement.normalize();
                Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
                int particleCount = (int) (halfWidth * 3);

                for (int i = -particleCount; i <= particleCount; i++) {
                    double offset = (i / (double) particleCount) * halfWidth;
                    Vec3 pPos = this.position().add(right.scale(offset));

                    // Golden & Divine White Sparks
                    serverLevel.sendParticles(
                        subWave ? new DustParticleOptions(0xFFA500, 1.5F) : new DustParticleOptions(0xFFD700, 2.2F),
                        pPos.x, pPos.y, pPos.z,
                        1, 0.05, 0.1, 0.05, 0.02
                    );
                }
            }

            // Terrain check if not block-piercing (Tweak B)
            if (!this.pierceBlocks) {
                Vec3 start = this.position();
                Vec3 end = start.add(movement);
                HitResult blockHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (blockHit.getType() != HitResult.Type.MISS) {
                    this.discard();
                }
            }

            // Discard after traveling for 4 seconds (80 ticks)
            if (this.tickCount > 80) {
                this.discard();
            }
        }
    }
}
