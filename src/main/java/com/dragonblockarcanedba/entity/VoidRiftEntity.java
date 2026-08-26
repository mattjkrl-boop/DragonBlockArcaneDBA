package com.dragonblockarcanedba.entity;

import net.minecraft.world.damagesource.DamageSource;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Void Rift Entity — Dimensional gravitational vortex created by Hollow's Edge.
 */
public class VoidRiftEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(VoidRiftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_IMPLODING = SynchedEntityData.defineId(VoidRiftEntity.class, EntityDataSerializers.BOOLEAN);

    private float damage = 500.0f;
    private int implosionTimer = -1;

    public VoidRiftEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public VoidRiftEntity(Level level, LivingEntity owner, Vec3 pos, float startRadius, float damage) {
        super(DbaEntities.VOID_RIFT, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.damage = damage;
        this.noPhysics = true;
        this.setRadius(startRadius);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 2.5f);
        builder.define(IS_IMPLODING, false);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(RADIUS, Math.min(12.0f, Math.max(1.5f, radius)));
    }

    public boolean isImploding() {
        return this.entityData.get(IS_IMPLODING);
    }

    public void triggerImplosion() {
        this.entityData.set(IS_IMPLODING, true);
        this.implosionTimer = 10; // 0.5s dramatic suction before explosion
    }

    @Override
    public void tick() {
        super.tick();

        float radius = getRadius();
        Vec3 center = this.position();
        Entity owner = this.getOwner();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Check if implosion countdown is active
            if (this.implosionTimer > 0) {
                this.implosionTimer--;

                // Phase 1: Violent Inward Vacuum
                double suctionRadius = radius * 2.5;
                AABB pullBox = this.getBoundingBox().inflate(suctionRadius);
                List<Entity> suckTargets = serverLevel.getEntitiesOfClass(Entity.class, pullBox, e -> e.isAlive() && e != this && e != owner);

                for (Entity e : suckTargets) {
                    Vec3 toCenter = center.subtract(e.position());
                    double dist = toCenter.length();
                    if (dist > 0.3) {
                        e.setDeltaMovement(toCenter.normalize().scale(1.2));
                        e.hurtMarked = true;
                    }
                }

                if (this.implosionTimer == 0) {
                    // Phase 2: Massive Outward Implosion Burst!
                    AABB blastBox = this.getBoundingBox().inflate(radius * 2.2);
                    List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, blastBox, e -> e.isAlive() && e != owner);

                    for (LivingEntity victim : victims) {
                        DamageSource source = owner instanceof net.minecraft.world.entity.player.Player p 
                            ? serverLevel.damageSources().playerAttack(p) 
                            : serverLevel.damageSources().mobAttack(owner instanceof LivingEntity ? (LivingEntity) owner : victim);
                        victim.hurtServer(serverLevel, source, this.damage);

                        // Violent outward launch
                        Vec3 push = victim.position().subtract(center).normalize().scale(2.2).add(0, 0.8, 0);
                        victim.setDeltaMovement(victim.getDeltaMovement().add(push));
                        victim.hurtMarked = true;

                        // Apply Dark Faded effect
                        victim.addEffect(new MobEffectInstance(DbaEffects.DARK_FADED_HOLDER, 100, 1, false, true), owner);
                    }

                    serverLevel.playSound(null, center.x, center.y, center.z,
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5f, 0.6f);
                    serverLevel.playSound(null, center.x, center.y, center.z,
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0f, 0.8f);

                    this.discard();
                    return;
                }
            } else if (!isImploding()) {
                // Continuous Rift Pulling & Damage
                AABB aoe = this.getBoundingBox().inflate(radius * 1.8);
                List<Entity> nearby = serverLevel.getEntitiesOfClass(Entity.class, aoe, e -> e.isAlive() && e != this && e != owner);

                for (Entity e : nearby) {
                    Vec3 toCenter = center.subtract(e.position());
                    double dist = toCenter.length();

                    // Gravity pull on mobs & players
                    if (dist > 0.5) {
                        double pullPower = Math.max(0.1, 0.6 * (1.0 - dist / (radius * 1.8)));
                        Vec3 vel = toCenter.normalize().scale(pullPower);
                        e.setDeltaMovement(e.getDeltaMovement().add(vel.x, vel.y * 0.5, vel.z));
                        e.hurtMarked = true;
                    }

                    // Projectiles (arrows, ki blasts) get sucked in
                    if (e instanceof Projectile proj && proj != this) {
                        proj.setDeltaMovement(toCenter.normalize().scale(0.8));
                    }

                    // Damage, slow, and hold enemies inside
                    if (e instanceof LivingEntity living) {
                        living.addEffect(new MobEffectInstance(DbaEffects.RIFTED_HOLDER, 20, 0, false, false), owner);
                        if (this.tickCount % 10 == 0) {
                            DamageSource source = owner instanceof net.minecraft.world.entity.player.Player p 
                                ? serverLevel.damageSources().playerAttack(p) 
                                : serverLevel.damageSources().mobAttack(owner instanceof LivingEntity ? (LivingEntity) owner : living);
                            living.hurtServer(serverLevel, source, 25.0f);
                        }
                    }
                }

                // Sound pulse
                if (this.tickCount % 25 == 0) {
                    serverLevel.playSound(null, center.x, center.y, center.z,
                        SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.7f + (radius / 10.0f) * 0.5f);
                }

                // Auto-expire after 20s if owner drops it
                if (this.tickCount > 400) {
                    triggerImplosion();
                }
            }
        }
    }
}
