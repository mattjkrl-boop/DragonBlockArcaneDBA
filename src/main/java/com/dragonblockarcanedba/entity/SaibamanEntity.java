package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.particle.DbaMaterialOptions;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.dragonblockarcanedba.util.DbaDamage;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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
        DamageSource source = DbaDamage.ki(this, this);
        for (int i = 0; i < nearby.size(); i++) {
            LivingEntity hit = nearby.get(i);
            hit.hurtServer(server, source, 8.0f);
        }
        server.playSound(null, this.getX(), this.getY(), this.getZ(), DbaSounds.BOOM_BIG, SoundSource.HOSTILE, 1.0f, 1.2f);
        server.sendParticles(new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, Blocks.SLIME_BLOCK.defaultBlockState()), this.getX(), this.getY() + 0.6, this.getZ(), 18, 0.4, 0.4, 0.4, 0.04);
        server.sendParticles(DbaMaterialOptions.of(DbaMaterialOptions.VAPOR, 0x69B84A, 0.9f, this.getId() * 31 + 23),
                this.getX(), this.getY() + 0.6, this.getZ(), 10, 0.4, 0.4, 0.4, 0.04);
        server.sendParticles(DbaMaterialOptions.of(DbaMaterialOptions.DUST, 0x86A860, 0.85f, this.getId() * 31 + 29),
                this.getX(), this.getY() + 0.6, this.getZ(), 8, 0.4, 0.4, 0.4, 0.04);
        // Complex sprite layer (class S): pale wisp curls and gray ash
        // flakes at the same burst point, spread, and speed as the
        // material burst above. 6 plus 6 stays inside the class S
        // burst budget of 12. The physical slime chunks above stay
        // physical and are not replaced.
        server.sendParticles(DbaParticles.WISP_CURL,
                this.getX(), this.getY() + 0.6, this.getZ(), 6, 0.4, 0.4, 0.4, 0.04);
        server.sendParticles(DbaParticles.ASH_FLAKE,
                this.getX(), this.getY() + 0.6, this.getZ(), 6, 0.4, 0.4, 0.4, 0.04);
        this.discard();
    }

    private boolean canBurstHit(LivingEntity entity) {
        return entity != this && entity.isAlive() && !(entity instanceof SaibamanEntity);
    }

    @Override
    protected void playAttackSound() {
        this.playSound(DbaSounds.HIT_FLESH, 1.0f, 0.8f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return DbaSounds.SAIBAMAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return DbaSounds.SAIBAMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DbaSounds.SAIBAMAN_DEATH;
    }
}
