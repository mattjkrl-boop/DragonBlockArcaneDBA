package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
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
 * Bansho Wind Projectile Entity — Physical 3D Legendary Wind Drill / Tempest Gale Projectile.
 * Fired during Bansho Fan's Tempest Barrage, replacing vanilla WindCharges with custom high-speed
 * physical 3D wind drills, Bleeding III infliction, and physical impact shockwave bursts.
 */
public class BanshoWindProjectileEntity extends Projectile {
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(BanshoWindProjectileEntity.class, EntityDataSerializers.INT);
    private float damage = 300.0f;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public BanshoWindProjectileEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BanshoWindProjectileEntity(Level level, LivingEntity owner, float damage) {
        super(DbaEntities.BANSHO_WIND_PROJECTILE, level);
        this.setOwner(owner);
        if (owner != null) {
            Vec3 eye = owner.getEyePosition();
            Vec3 look = owner.getLookAngle();
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);
            Vec3 up = right.cross(look).normalize();

            boolean isRightHanded = (owner.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (owner.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.BanshoFanItem && 
                !(owner.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.BanshoFanItem));
            boolean onRight = isRightHanded ? !isOffhand : isOffhand;
            float sideSign = onRight ? 1.0f : -1.0f;

            Vec3 spawnPos = eye.add(look.scale(0.5)).add(right.scale(sideSign * 0.35)).add(up.scale(-0.25));
            this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
            this.entityData.set(CASTER_ID, owner.getId());
        }
        this.damage = damage;
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CASTER_ID, -1);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    public void shootFromRotation(Entity shooter, float pitch, float yaw, float roll, float speed, float inaccuracy) {
        float f = -Mth.sin(yaw * ((float)Math.PI / 180F)) * Mth.cos(pitch * ((float)Math.PI / 180F));
        float f1 = -Mth.sin((pitch + roll) * ((float)Math.PI / 180F));
        float f2 = Mth.cos(yaw * ((float)Math.PI / 180F)) * Mth.cos(pitch * ((float)Math.PI / 180F));
        this.shoot((double)f, (double)f1, (double)f2, speed, inaccuracy);
    }

    @Override
    public void shoot(double x, double y, double z, float speed, float inaccuracy) {
        Vec3 vec3 = (new Vec3(x, y, z)).normalize().scale((double)speed);
        this.setDeltaMovement(vec3);
        double d0 = vec3.horizontalDistance();
        this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * (double)(180F / (float)Math.PI)));
        this.setXRot((float)(Mth.atan2(vec3.y, d0) * (double)(180F / (float)Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 movement = this.getDeltaMovement();
        Vec3 currentPos = this.position();
        Vec3 nextPos = currentPos.add(movement);

        // Update rotation to match trajectory
        double horiz = movement.horizontalDistance();
        this.setYRot((float)(Mth.atan2(movement.x, movement.z) * (180.0 / Math.PI)));
        this.setXRot((float)(Mth.atan2(movement.y, horiz) * (180.0 / Math.PI)));

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Entity owner = this.getOwner();

            // Block collision raycast
            BlockHitResult blockHit = this.level().clip(new ClipContext(
                currentPos, nextPos,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this
            ));

            if (blockHit.getType() != HitResult.Type.MISS) {
                onHitImpact(serverLevel, blockHit.getLocation(), null);
                this.discard();
                return;
            }

            // Entity collision check
            AABB sweepBox = this.getBoundingBox().expandTowards(movement).inflate(0.8);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, sweepBox,
                e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
            );

            if (!targets.isEmpty()) {
                LivingEntity hit = targets.get(0);
                onHitImpact(serverLevel, hit.position().add(0, hit.getBbHeight() * 0.5, 0), hit);
                this.discard();
                return;
            }

            if (this.tickCount >= 50) { // 2.5s maximum range
                this.discard();
                return;
            }
        }

        this.setPos(nextPos.x, nextPos.y, nextPos.z);
    }

    private void onHitImpact(ServerLevel serverLevel, Vec3 hitPos, LivingEntity target) {
        Entity owner = this.getOwner();
        DamageSource damageSource = owner instanceof net.minecraft.world.entity.player.Player playerOwner
            ? serverLevel.damageSources().playerAttack(playerOwner)
            : (owner instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobProjectile(this, livingOwner) : serverLevel.damageSources().generic());

        if (target != null) {
            hitEntityIds.add(target.getId());

            // Deal impact damage
            target.hurtServer(serverLevel, damageSource, this.damage);

            // Apply Bleeding III (amplifier 2) for 20 seconds (400 ticks)
            target.addEffect(new MobEffectInstance(DbaEffects.BLEEDING_HOLDER, 400, 2, false, true), owner);

            // Knockback target away from impact trajectory
            Vec3 move = this.getDeltaMovement().normalize().scale(1.4);
            target.setDeltaMovement(target.getDeltaMovement().add(move.x, 0.45, move.z));
            target.hurtMarked = true;
        }

        // Spawn physical 3D impact shockwave entity (replacing particles)
        BanshoShockwaveEntity shockwave = new BanshoShockwaveEntity(
            serverLevel,
            owner instanceof LivingEntity living ? living : null,
            hitPos,
            this.getYRot(),
            this.getXRot(),
            target != null ? 2.6f : 2.0f,
            false // Radial impact burst
        );
        serverLevel.addFreshEntity(shockwave);

        // Tempest impact sound
        serverLevel.playSound(null, hitPos.x, hitPos.y, hitPos.z,
            SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.2f, 1.0f);
        serverLevel.playSound(null, hitPos.x, hitPos.y, hitPos.z,
            SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.8f, 1.4f);
    }
}
