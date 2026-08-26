package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class DimensionalSlashEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> TILT = SynchedEntityData.defineId(DimensionalSlashEntity.class, EntityDataSerializers.BOOLEAN);
    private float damage = 750.0f;

    public DimensionalSlashEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DimensionalSlashEntity(Level level, LivingEntity owner, boolean tiltRight, float damage) {
        super(DbaEntities.DIMENSIONAL_SLASH, level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());
        this.entityData.set(TILT, tiltRight);
        this.damage = damage;
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TILT, false);
    }

    public boolean getTilt() {
        return this.entityData.get(TILT);
    }

    public float getDamage() {
        return this.damage;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.tickCount == 1) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.8f, 1.3f);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.2f, 1.6f);
        }

        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
        }

        Vec3 vec3 = this.getDeltaMovement();
        double d = this.getX() + vec3.x;
        double e = this.getY() + vec3.y;
        double f = this.getZ() + vec3.z;

        this.setPos(d, e, f);

        if (this.tickCount > 100 && !this.level().isClientSide()) {
            this.discard(); // Time out after 5 seconds
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Entity hitEntity = result.getEntity();
            Entity owner = this.getOwner();
            if (hitEntity instanceof LivingEntity living) {
                DamageSource source = owner instanceof Player playerOwner
                    ? serverLevel.damageSources().playerAttack(playerOwner)
                    : (owner instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobProjectile(this, livingOwner) : serverLevel.damageSources().magic());

                living.hurtServer(serverLevel, source, damage);
                living.addEffect(new MobEffectInstance(DbaEffects.MELTING_HOLDER, 300, 1, false, true), owner);

                // MC 26.2 Physics: Spatial rift warp friction & drag distortion
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    living,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("dimensional_slash_friction"),
                    -0.90,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    living,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("dimensional_slash_drag"),
                    -0.80,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );

                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.PLAYERS, 1.8f, 1.2f);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }
}
