package com.dragonblockarcanedba.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Erasure Cannon Beam Entity — Physical 3D continuous geometric energy beam
 * spanning up to 48 meters with multi-layered synchrotron laser core, helical drill ribbons,
 * focusing aperture rings, and terminus vaporization dome.
 */
public class ErasureCannonBeamEntity extends Projectile {
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(ErasureCannonBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BEAM_LENGTH = SynchedEntityData.defineId(ErasureCannonBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BEAM_Y_ROT = SynchedEntityData.defineId(ErasureCannonBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BEAM_X_ROT = SynchedEntityData.defineId(ErasureCannonBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CHARGE_RATIO = SynchedEntityData.defineId(ErasureCannonBeamEntity.class, EntityDataSerializers.FLOAT);

    public static final int MAX_LIFETIME = 16;
    public static final float DEFAULT_MAX_RANGE = 48.0f;

    private float totalDamage = 900.0f;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public ErasureCannonBeamEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public ErasureCannonBeamEntity(Level level, LivingEntity caster, float damage, float chargeRatio, float length) {
        super(DbaEntities.ERASURE_CANNON_BEAM, level);
        this.setOwner(caster);
        this.noPhysics = true;
        this.totalDamage = damage;

        if (caster != null) {
            this.entityData.set(CASTER_ID, caster.getId());
            Vec3 eye = caster.getEyePosition();
            this.setPos(eye.x, eye.y, eye.z);
            this.setYRot(caster.getYRot());
            this.setXRot(caster.getXRot());
            this.entityData.set(BEAM_Y_ROT, caster.getYRot());
            this.entityData.set(BEAM_X_ROT, caster.getXRot());
        }

        this.entityData.set(CHARGE_RATIO, chargeRatio);
        this.entityData.set(BEAM_LENGTH, length);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CASTER_ID, -1);
        builder.define(BEAM_LENGTH, DEFAULT_MAX_RANGE);
        builder.define(BEAM_Y_ROT, 0.0f);
        builder.define(BEAM_X_ROT, 0.0f);
        builder.define(CHARGE_RATIO, 1.0f);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    public float getBeamLength() {
        return this.entityData.get(BEAM_LENGTH);
    }

    public float getBeamYRot() {
        return this.entityData.get(BEAM_Y_ROT);
    }

    public float getBeamXRot() {
        return this.entityData.get(BEAM_X_ROT);
    }

    public float getChargeRatio() {
        return this.entityData.get(CHARGE_RATIO);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Entity caster = this.getOwner();
            if (caster == null && getCasterId() != -1) {
                caster = serverLevel.getEntity(getCasterId());
            }
            final Entity finalCaster = caster;

            // On first tick, execute devastating piercing damage & impulse along the 48m beam
            if (this.tickCount == 1) {
                float length = getBeamLength();
                float charge = getChargeRatio();
                Vec3 start = this.position();
                Vec3 look = this.calculateViewVector(getBeamXRot(), getBeamYRot());
                Vec3 end = start.add(look.scale(length));

                DamageSource damageSource = finalCaster instanceof net.minecraft.world.entity.player.Player playerOwner
                    ? serverLevel.damageSources().playerAttack(playerOwner)
                    : (finalCaster instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobProjectile(this, livingOwner) : serverLevel.damageSources().generic());

                AABB beamBox = new AABB(start, end).inflate(1.5 + charge * 1.5);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, beamBox,
                    e -> e.isAlive() && e != finalCaster && !hitEntityIds.contains(e.getId())
                );

                for (LivingEntity t : targets) {
                    hitEntityIds.add(t.getId());

                    if (finalCaster instanceof LivingEntity livingOwner) {
                        t.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 25, 0, false, false, false
                        ), livingOwner);
                    }

                    t.hurtServer(serverLevel, damageSource, this.totalDamage);

                    // Concussive impulse & air drag modification
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        t,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("blaster_beam_bounce"),
                        0.85,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    );
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        t,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("blaster_beam_drag"),
                        -0.40,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    );

                    // Gravitational suction pulling enemies into centerline
                    Vec3 toCenter = start.add(look.scale(look.dot(t.position().subtract(start)))).subtract(t.position()).normalize().scale(0.8);
                    t.setDeltaMovement(t.getDeltaMovement().add(toCenter.x, 0.2, toCenter.z));
                    t.hurtMarked = true;
                }

                // Max charge endpoint explosion
                if (charge >= 0.9f) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, end.x, end.y, end.z, 3, 0.5, 0.5, 0.5, 0);
                    AABB endAoe = new AABB(end.subtract(4, 4, 4), end.add(4, 4, 4));
                    List<LivingEntity> endTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, endAoe, e -> e.isAlive() && e != finalCaster);
                    for (LivingEntity et : endTargets) {
                        et.hurtServer(serverLevel, damageSource, this.totalDamage * 0.5f);
                    }
                }

                serverLevel.playSound(null, start.x, start.y, start.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.2f, 0.6f);
            }

            if (this.tickCount >= MAX_LIFETIME) {
                this.discard();
            }
        }
    }
}
