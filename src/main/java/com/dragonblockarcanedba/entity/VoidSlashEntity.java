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

    // No default damage: the item always sets it from its own formula
    // (dash damage x 1.8 on a third dash, x 1.5 into the slash). The old 600
    // default matched no formula in the weapon and was dead weight that a
    // stray spawn could have dealt by accident, so an unset slash now
    // deals no damage at all instead of phantom damage.
    private float damage = 0.0f;
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
            // Theme particles (T5): the crescent sheds layered particles along
            // its flight path so the cut reads between the aura layers: bright
            // ki motes at the core of the cut, void smoke as the wispy outer.
            float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
            if (density > 0f) {
                serverLevel.sendParticles(
                    com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
                    this.getX(), this.getY(), this.getZ(),
                    Math.max(1, Math.round(1f * density)),
                    0.35, 0.35, 0.35, 0.01);
                if (this.tickCount % 2 == 0) {
                    serverLevel.sendParticles(
                        com.dragonblockarcanedba.particle.DbaParticles.VOID_SMOKE,
                        this.getX(), this.getY(), this.getZ(),
                        Math.max(1, Math.round(2f * density)),
                        0.8, 0.5, 0.8, 0.02);
                }
            }

            // Hit detection
            AABB hitbox = this.getBoundingBox().inflate(2.5, 1.2, 2.5);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, hitbox, e -> e.isAlive() && e != this.getOwner() && !hitEntities.contains(e.getId()));

            Entity owner = this.getOwner();
            for (LivingEntity target : targets) {
                hitEntities.add(target.getId());

                // Massive void damage, only when the item actually set it.
                if (this.damage > 0.0f) {
                    target.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(owner instanceof LivingEntity ? (LivingEntity) owner : target), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (this.damage)));
                }

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
