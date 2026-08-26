package com.dragonblockarcanedba.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * Darkness Wave Entity — Fired by Darkness Sword (Abyssal Slash).
 * Piercing crescent darkness wave traveling up to 32 blocks, passing through terrain and mobs.
 */
public class DarknessWaveEntity extends Projectile {
    private float damage = 400.0f;
    private final Set<Integer> hitEntityIds = new HashSet<>();
    private boolean isSecondary = false;

    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(DarknessWaveEntity.class, EntityDataSerializers.INT);

    public DarknessWaveEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DarknessWaveEntity(Level level, LivingEntity owner, float damage, boolean isSecondary) {
        super(DbaEntities.DARKNESS_WAVE, level);
        this.setOwner(owner);
        if (owner != null) {
            Vec3 eye = owner.getEyePosition();
            Vec3 look = owner.getLookAngle();
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);
            Vec3 up = right.cross(look).normalize();

            boolean isRightHanded = (owner.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (owner.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.DaburaSwordItem && 
                !(owner.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.DaburaSwordItem));
            boolean onRight = isRightHanded ? !isOffhand : isOffhand;
            float sideSign = onRight ? 1.0f : -1.0f;

            Vec3 spawnPos = eye.add(look.scale(0.8)).add(right.scale(sideSign * 0.35)).add(up.scale(-0.25));
            this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
            this.entityData.set(CASTER_ID, owner.getId());
        }
        this.damage = damage;
        this.isSecondary = isSecondary;
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CASTER_ID, -1);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    public boolean isSecondary() {
        return this.isSecondary;
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

            double width = this.isSecondary ? 2.2 : 3.5;
            double height = 1.2;
            AABB hitbox = this.getBoundingBox().inflate(width, height, width);

            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, hitbox,
                e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
            );

            for (LivingEntity target : targets) {
                hitEntityIds.add(target.getId());

                // Apply damage
                target.hurtServer(serverLevel, damageSource, this.damage);

                // Debuff: Petrification Curse & Dark Faded Darkness for 5s (100 ticks)
                target.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.PETRIFICATION_CURSE_HOLDER, 100, 0, false, true));
                target.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.DARK_FADED_HOLDER, 100, 0, false, true));

                // MC 26.2 Physics: Void gravitational drag and creeping darkness friction
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    target,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("darkness_wave_drag"),
                    3.0,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    target,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("darkness_wave_friction"),
                    2.5,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );

                // Cinematic tracking buffer
                if (owner instanceof LivingEntity livingOwner) {
                    target.addEffect(new MobEffectInstance(
                        com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 25, 0, false, false, false
                    ), livingOwner);
                }

                // Knockback
                Vec3 knockback = movement.normalize().scale(1.2);
                target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.25, knockback.z));
                target.hurtMarked = true;

                // Impact feedback
                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.6f);
            }

            // Wave passes through terrain, expires after 32 blocks / 40 ticks
            if (this.tickCount > 40) {
                this.discard();
            }
        }
    }
}
