package com.dragonblockarcanedba.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Tall Namekian fighter. The stretched punch knocks the target back. */
public class NamekianWarriorEntity extends DbaHostileEntity {
    public NamekianWarriorEntity(EntityType<? extends NamekianWarriorEntity> type, Level level) {
        super(type, level);
        this.applyCombatStats(32.0, 0.25, 7.0, 24.0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return DbaHostileEntity.createHostileAttributes();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            Vec3 away = new Vec3(living.getX() - this.getX(), 0.0, living.getZ() - this.getZ());
            if (away.lengthSqr() > 1.0E-4) {
                Vec3 push = away.normalize().scale(0.55).add(0.0, 0.28, 0.0);
                living.setDeltaMovement(living.getDeltaMovement().add(push));
                living.hurtMarked = true;
            }
        }
        return hit;
    }

    @Override
    protected void playAttackSound() {
        this.playSound(SoundEvents.PLAYER_ATTACK_WEAK, 1.0f, 0.6f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VINDICATOR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VINDICATOR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VINDICATOR_HURT;
    }
}
