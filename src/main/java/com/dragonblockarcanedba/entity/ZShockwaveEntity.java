package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
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
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(ZShockwaveEntity.class, EntityDataSerializers.INT);

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
            Vec3 eye = owner.getEyePosition();
            Vec3 look = owner.getLookAngle();
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);
            Vec3 up = right.cross(look).normalize();

            boolean isRightHanded = (owner.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (owner.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.ZSwordItem && 
                !(owner.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.ZSwordItem));
            boolean onRight = isRightHanded ? !isOffhand : isOffhand;
            float sideSign = onRight ? 1.0f : -1.0f;

            Vec3 spawnPos = eye.add(look.scale(1.2)).add(right.scale(sideSign * 0.35)).add(up.scale(-0.35));
            this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
            this.entityData.set(CASTER_ID, owner.getId());
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
        builder.define(CHARGE_RATIO, 0.0f);
        builder.define(IS_SUB_WAVE, false);
        builder.define(CASTER_ID, -1);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
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
                    target.addEffect(new MobEffectInstance(DbaEffects.MOVEMENT_CURSE_HOLDER, 80, 9, false, true), owner instanceof LivingEntity livingOwner ? livingOwner : null);
                    // Suffocation / block crush damage from head embedded in terrain
                    DamageSource crushSource = owner instanceof LivingEntity livingOwner
                        ? serverLevel.damageSources().indirectMagic(this, livingOwner)
                        : serverLevel.damageSources().inWall();
                    target.hurtServer(serverLevel, crushSource, 250.0f);
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
