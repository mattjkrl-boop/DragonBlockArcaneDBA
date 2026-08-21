package com.dragonblockarcanedba.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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

    public DarknessWaveEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DarknessWaveEntity(Level level, LivingEntity owner, float damage, boolean isSecondary) {
        super(DbaEntities.DARKNESS_WAVE, level);
        this.setOwner(owner);
        if (owner != null) {
            this.setPos(owner.getX(), owner.getY() + 1.0, owner.getZ());
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
        }
        this.damage = damage;
        this.isSecondary = isSecondary;
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
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

                // Debuff: Petrification Curse for 5s (100 ticks)
                target.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.PETRIFICATION_CURSE_HOLDER, 100, 0, false, true));

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

                // Impact dark void particles
                serverLevel.sendParticles(
                    new DustParticleOptions(0x1A0033, 2.0f),
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    15, 0.3, 0.5, 0.3, 0.1
                );
            }

            // Trailing darkness & purple smoke particles along the crescent
            if (this.tickCount % 2 == 0) {
                Vec3 look = movement.normalize();
                Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();

                for (int i = -5; i <= 5; i++) {
                    double offset = (i / 5.0) * width;
                    Vec3 pPos = this.position().add(right.scale(offset));

                    serverLevel.sendParticles(
                        new DustParticleOptions(0x3B0066, 1.8f),
                        pPos.x, pPos.y, pPos.z,
                        1, 0.0, 0.02, 0.0, 0.01
                    );
                    serverLevel.sendParticles(
                        ParticleTypes.SMOKE,
                        pPos.x, pPos.y, pPos.z,
                        1, 0.0, 0.01, 0.0, 0.01
                    );
                }
            }

            // Wave passes through terrain (Tweak B), expires after 32 blocks / 40 ticks
            if (this.tickCount > 40) {
                this.discard();
            }
        }
    }
}
