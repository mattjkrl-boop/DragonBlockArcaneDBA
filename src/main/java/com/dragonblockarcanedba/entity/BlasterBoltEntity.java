package com.dragonblockarcanedba.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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
 * Blaster Bolt Entity — Laser projectile fired by the Blaster Gun.
 * Rapid physical 3D energy projectile with piercing, heat scaling, and explosive Overcharged Blast.
 */
public class BlasterBoltEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> IS_OVERCHARGED = SynchedEntityData.defineId(BlasterBoltEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> HEAT_RATIO = SynchedEntityData.defineId(BlasterBoltEntity.class, EntityDataSerializers.FLOAT);

    private float damage = 250.0f;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public BlasterBoltEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BlasterBoltEntity(Level level, LivingEntity owner, float damage, boolean isOvercharged, float heatRatio) {
        super(DbaEntities.BLASTER_BOLT, level);
        this.setOwner(owner);
        if (owner != null) {
            Vec3 eye = owner.getEyePosition();
            Vec3 look = owner.getLookAngle();
            this.setPos(eye.x + look.x * 0.5, eye.y + look.y * 0.5 - 0.1, eye.z + look.z * 0.5);
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
        }
        this.damage = damage;
        this.entityData.set(IS_OVERCHARGED, isOvercharged);
        this.entityData.set(HEAT_RATIO, heatRatio);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IS_OVERCHARGED, false);
        builder.define(HEAT_RATIO, 0.0f);
    }

    public boolean isOvercharged() {
        return this.entityData.get(IS_OVERCHARGED);
    }

    public float getHeatRatio() {
        return this.entityData.get(HEAT_RATIO);
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 movement = this.getDeltaMovement();
        double nextX = this.getX() + movement.x;
        double nextY = this.getY() + movement.y;
        double nextZ = this.getZ() + movement.z;
        this.setPos(nextX, nextY, nextZ);

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Entity owner = this.getOwner();
            DamageSource damageSource = owner instanceof net.minecraft.world.entity.player.Player playerOwner
                ? serverLevel.damageSources().playerAttack(playerOwner)
                : (owner instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobProjectile(this, livingOwner) : serverLevel.damageSources().generic());

            boolean overcharged = isOvercharged();
            float heat = getHeatRatio();
            double hitRadius = overcharged ? 1.5 : (0.6 + heat * 0.4);
            AABB hitbox = this.getBoundingBox().inflate(hitRadius, hitRadius, hitRadius);

            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, hitbox,
                e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
            );

            for (LivingEntity target : targets) {
                hitEntityIds.add(target.getId());

                target.hurtServer(serverLevel, damageSource, this.damage);

                if (owner instanceof LivingEntity livingOwner) {
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 15, 0, false, false, false
                    ), livingOwner);
                }

                // Knockback
                Vec3 knockback = movement.normalize().scale(overcharged ? 1.5 : 0.6);
                target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.2, knockback.z));
                target.hurtMarked = true;

                if (overcharged) {
                    // Overcharged explosive detonation
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        target,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("blaster_bolt_bounce"),
                        0.75,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    );
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        target,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("blaster_bolt_drag"),
                        -0.30,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    );

                    AABB aoe = target.getBoundingBox().inflate(3.0);
                    List<LivingEntity> splashTargets = serverLevel.getEntitiesOfClass(
                        LivingEntity.class, aoe, e -> e.isAlive() && e != owner && e != target
                    );
                    for (LivingEntity st : splashTargets) {
                        st.hurtServer(serverLevel, damageSource, this.damage * 0.6f);
                    }
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);
                    serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 1.4f);
                }
            }

            if (this.tickCount > 30) {
                this.discard();
            }
        }
    }
}
