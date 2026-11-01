package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Bansho Cyclone Entity — Physical 3D Emerald/Jade Tempest Vortex.
 * Spawns on Bansho Fan Cyclone Slash proc, creating a raging physical cyclone that pulls, lifts, and shreds nearby enemies.
 */
public class BanshoCycloneEntity extends Projectile {
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(BanshoCycloneEntity.class, EntityDataSerializers.FLOAT);

    private float slashDamage = 200.0f;
    private int maxLifetime = 25; // 1.25s active vortex

    public BanshoCycloneEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BanshoCycloneEntity(Level level, LivingEntity owner, Vec3 pos, float damage) {
        super(DbaEntities.BANSHO_CYCLONE, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.slashDamage = damage;
        this.noPhysics = true;
        this.setScale(1.0f);
        this.maxLifetime = 25;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SCALE, 1.0f);
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public void setScale(float scale) {
        this.entityData.set(SCALE, scale);
    }

    @Override
    public void tick() {
        super.tick();

        float scale = getScale();
        double radius = scale * 4.0;
        double height = scale * 6.5;
        Vec3 center = this.position();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Pull, lift, and shred entities in the vortex
            AABB aoe = new AABB(center.x - radius, center.y - 0.5, center.z - radius,
                                center.x + radius, center.y + height, center.z + radius);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, aoe,
                e -> e.isAlive() && e != this.getOwner()
            );

            for (LivingEntity target : targets) {
                Vec3 toTarget = target.position().subtract(center);
                double dist = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);

                // Tangential vortex spin velocity + inward vacuum
                double angle = Math.atan2(toTarget.z, toTarget.x) + 0.5;
                double speed = 0.95 * Math.max(0.2, 1.0 - (dist / radius));
                double vx = -Math.sin(angle) * speed;
                double vz = Math.cos(angle) * speed;
                double vy = 0.32;

                // Inward suction vector
                if (dist > 0.6) {
                    vx -= (toTarget.x / dist) * 0.25;
                    vz -= (toTarget.z / dist) * 0.25;
                }

                target.setDeltaMovement(target.getDeltaMovement().add(vx * 0.45, vy, vz * 0.45));
                com.dragonblockarcanedba.util.DbaCompat.markHurt(target);

                // Continuous cyclone wind damage (every 5 ticks)
                if (this.tickCount % 5 == 0) {
                    net.minecraft.world.damagesource.DamageSource source = this.getOwner() instanceof net.minecraft.world.entity.player.Player playerOwner
                        ? serverLevel.damageSources().playerAttack(playerOwner)
                        : (this.getOwner() instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobAttack(livingOwner) : serverLevel.damageSources().magic());

                    target.hurtServer(serverLevel, source, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (this.slashDamage * 0.35f)));

                    // Apply Bleeding I (amplifier 0) for 5 seconds (100 ticks)
                    target.addEffect(new MobEffectInstance(DbaEffects.BLEEDING_HOLDER, 100, 0, false, true), this.getOwner());
                }
            }

            // Howling tempest sound
            if (this.tickCount == 1 || this.tickCount % 12 == 0) {
                serverLevel.playSound(null, center.x, center.y, center.z,
                    SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.4f, 1.1f);
                serverLevel.playSound(null, center.x, center.y, center.z,
                    SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0f, 1.3f);
            }

            // Theme particles (T5): wind wisps spiral around the funnel wall.
            // The cyclone emitted no particles before.
            if (this.tickCount % 2 == 0) {
                float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
                if (density > 0f) {
                    double wispAngle = serverLevel.getRandom().nextDouble() * Math.PI * 2.0;
                    double wispRadius = radius * (0.4 + serverLevel.getRandom().nextDouble() * 0.5);
                    double wispY = serverLevel.getRandom().nextDouble() * height * 0.8;
                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.WIND,
                        center.x + Math.cos(wispAngle) * wispRadius,
                        center.y + wispY,
                        center.z + Math.sin(wispAngle) * wispRadius,
                        Math.max(1, Math.round(2f * density)),
                        0.15, 0.3, 0.15, 0.06);
                }
            }

            if (this.tickCount >= this.maxLifetime) {
                this.discard();
            }
        }
    }
}
