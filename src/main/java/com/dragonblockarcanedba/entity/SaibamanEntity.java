package com.dragonblockarcanedba.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Short plant fighter. Claws inflict poison, and a badly hurt saibaman bursts. */
public class SaibamanEntity extends DbaHostileEntity {
    private boolean bursting;

    public SaibamanEntity(EntityType<? extends SaibamanEntity> type, Level level) {
        super(type, level);
        this.applyCombatStats(18.0, 0.32, 4.0, 24.0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return DbaHostileEntity.createHostileAttributes();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.bursting) {
            return;
        }
        if (this.getHealth() > 5.0f || this.getTarget() == null) {
            return;
        }
        this.bursting = true;
        this.burst();
    }

    private void burst() {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        AABB area = this.getBoundingBox().inflate(2.5);
        List<LivingEntity> nearby = server.getEntities(EntityTypeTest.forClass(LivingEntity.class), area, this::canBurstHit);
        DamageSource source = server.damageSources().mobAttack(this);
        for (int i = 0; i < nearby.size(); i++) {
            LivingEntity hit = nearby.get(i);
            hit.hurtServer(server, source, 8.0f);
        }
        server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0f, 1.2f);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.ITEM_SLIME, this.getX(), this.getY() + 0.6, this.getZ(), 18, 0.4, 0.4, 0.4, 0.04);
        this.discard();
    }

    private boolean canBurstHit(LivingEntity entity) {
        return entity != this && entity.isAlive() && !(entity instanceof SaibamanEntity);
    }

    @Override
    protected void playAttackSound() {
        this.playSound(SoundEvents.SLIME_ATTACK, 1.0f, 0.8f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SLIME_SQUISH_SMALL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_HURT;
    }
}
