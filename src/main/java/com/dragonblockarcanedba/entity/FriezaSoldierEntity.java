package com.dragonblockarcanedba.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** Armored trooper. Closes to melee and fires a short ki shot while the target is still out of reach. */
public class FriezaSoldierEntity extends DbaHostileEntity {
    private int shotCooldown;

    public FriezaSoldierEntity(EntityType<? extends FriezaSoldierEntity> type, Level level) {
        super(type, level);
        this.applyCombatStats(26.0, 0.26, 4.0, 28.0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return DbaHostileEntity.createHostileAttributes();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.shotCooldown > 0) {
            this.shotCooldown--;
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        double distanceSqr = this.distanceToSqr(target);
        if (distanceSqr < 9.0 || distanceSqr > 196.0 || !this.hasLineOfSight(target)) {
            return;
        }
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        this.shotCooldown = 40;
        this.swing(InteractionHand.MAIN_HAND);
        target.hurtServer(server, server.damageSources().mobAttack(this), 5.0f);
        server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.7f, 1.5f);
        server.sendParticles(ParticleTypes.CRIT, target.getX(), target.getEyeY(), target.getZ(), 8, 0.2, 0.25, 0.2, 0.02);
    }

    @Override
    protected void playAttackSound() {
        this.playSound(SoundEvents.PLAYER_ATTACK_WEAK, 0.8f, 0.7f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PILLAGER_HURT;
    }
}
