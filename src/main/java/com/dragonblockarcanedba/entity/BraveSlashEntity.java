package com.dragonblockarcanedba.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
 * Brave Slash Entity — Fired by the Brave Sword on combo crescents and heroic strikes.
 * Radiant golden-cyan energy crescent piercing lines of enemies.
 */
public class BraveSlashEntity extends Projectile {
    private float damage = 600.0f;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public BraveSlashEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BraveSlashEntity(Level level, LivingEntity owner, float damage) {
        super(DbaEntities.BRAVE_SLASH, level);
        this.setOwner(owner);
        if (owner != null) {
            this.setPos(owner.getX(), owner.getY() + 1.0, owner.getZ());
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
        }
        this.damage = damage;
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
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

            double width = 3.0;
            double height = 1.2;
            AABB hitbox = this.getBoundingBox().inflate(width, height, width);

            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, hitbox,
                e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
            );

            for (LivingEntity target : targets) {
                hitEntityIds.add(target.getId());

                target.hurtServer(serverLevel, damageSource, this.damage);

                if (owner instanceof LivingEntity livingOwner) {
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 25, 0, false, false, false
                    ), livingOwner);
                }

                // Knockback
                Vec3 knockback = movement.normalize().scale(1.4);
                target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.25, knockback.z));
                target.hurtMarked = true;

                // Golden critical sparks
                serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.3, 0.5, 0.3, 0.15);
            }

            // Trailing golden and cyan sparks
            if (this.tickCount % 2 == 0) {
                Vec3 look = movement.normalize();
                Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();

                for (int i = -5; i <= 5; i++) {
                    double offset = (i / 5.0) * width;
                    Vec3 pPos = this.position().add(right.scale(offset));

                    serverLevel.sendParticles(
                        new DustParticleOptions(0xFFD700, 1.8f),
                        pPos.x, pPos.y, pPos.z,
                        1, 0, 0.02, 0, 0.01
                    );
                    serverLevel.sendParticles(
                        new DustParticleOptions(0x00FFFF, 1.4f),
                        pPos.x, pPos.y, pPos.z,
                        1, 0, 0.01, 0, 0.01
                    );
                }
            }

            if (this.tickCount > 40) {
                this.discard();
            }
        }
    }
}
