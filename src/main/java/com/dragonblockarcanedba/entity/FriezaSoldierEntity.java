package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.dragonblockarcanedba.util.DbaDamage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
        target.hurtServer(server, DbaDamage.ki(this, this), 5.0f);
        server.playSound(null, this.getX(), this.getY(), this.getZ(), DbaSounds.FIRE_SHOT, SoundSource.HOSTILE, 0.7f, 1.5f);
        server.sendParticles(DbaParticles.CRIT_SPARK, target.getX(), target.getEyeY(), target.getZ(), 8, 0.2, 0.25, 0.2, 0.02);
    }

    @Override
    protected void playAttackSound() {
        this.playSound(DbaSounds.SWING_LIGHT, 0.8f, 0.7f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return DbaSounds.SOLDIER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return DbaSounds.SOLDIER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DbaSounds.SOLDIER_DEATH;
    }
}
