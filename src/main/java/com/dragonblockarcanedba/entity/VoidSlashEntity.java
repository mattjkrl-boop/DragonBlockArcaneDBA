package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Void Slash Entity — Large crescent blade wave unleashed on every 3rd Hollow Rush teleport.
 */
public class VoidSlashEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> TILT_RIGHT = SynchedEntityData.defineId(VoidSlashEntity.class, EntityDataSerializers.BOOLEAN);

    private float damage = 600.0f;
    private final Set<Integer> hitEntities = new HashSet<>();

    public VoidSlashEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public VoidSlashEntity(Level level, LivingEntity owner, float damage, boolean tiltRight) {
        super(DbaEntities.VOID_SLASH, level);
        this.setOwner(owner);
        if (owner != null) {
            this.setPos(owner.getX(), owner.getY() + 0.8, owner.getZ());
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
        }
        this.damage = damage;
        this.noPhysics = true;
        this.entityData.set(TILT_RIGHT, tiltRight);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TILT_RIGHT, false);
    }

    public boolean getTilt() {
        return this.entityData.get(TILT_RIGHT);
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 movement = this.getDeltaMovement();
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Hit detection
            AABB hitbox = this.getBoundingBox().inflate(2.5, 1.2, 2.5);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, hitbox, e -> e.isAlive() && e != this.getOwner() && !hitEntities.contains(e.getId()));

            Entity owner = this.getOwner();
            for (LivingEntity target : targets) {
                hitEntities.add(target.getId());

                // Massive void damage
                target.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(owner instanceof LivingEntity ? (LivingEntity) owner : target), this.damage);

                // Inflict Dark Faded
                target.addEffect(new MobEffectInstance(DbaEffects.DARK_FADED_HOLDER, 80, 1, false, true));

                // Sound
                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.6f);
            }

            if (this.tickCount > 40) {
                this.discard();
            }
        }
    }
}
