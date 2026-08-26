package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dimensional Rift Entity — Massive 15-block radius gravitational vortex domain created by the Sickle of Sorrow.
 * Renders 3D concentric ground accretion shockwaves, vertical jagged spatial fissures, orbiting crystalline void shards, and a singularity core.
 */
public class DimensionalRiftEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(DimensionalRiftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_COLLAPSING = SynchedEntityData.defineId(DimensionalRiftEntity.class, EntityDataSerializers.BOOLEAN);

    public static final double MAX_AOE_RADIUS = 15.0;
    public static final int MAX_LIFETIME = 60; // 3 seconds of active gravitational vortex

    private float burstDamage = 500.0f;
    private final Set<Integer> burstDamagedTargets = new HashSet<>();

    public DimensionalRiftEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DimensionalRiftEntity(Level level, LivingEntity owner, Vec3 pos, float burstDamage) {
        super(DbaEntities.DIMENSIONAL_RIFT, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.burstDamage = burstDamage;
        this.noPhysics = true;
        this.setRadius(3.0f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 3.0f);
        builder.define(IS_COLLAPSING, false);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(RADIUS, Math.min(15.0f, Math.max(1.0f, radius)));
    }

    public boolean isCollapsing() {
        return this.entityData.get(IS_COLLAPSING);
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 center = this.position();
        Entity owner = this.getOwner();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            float progress = Math.min(1.0f, this.tickCount / 40.0f);
            float currentRadius = 3.0f + (progress * 12.0f); // Expands 3.0 -> 15.0
            this.setRadius(currentRadius);

            if (this.tickCount >= MAX_LIFETIME - 10) {
                this.entityData.set(IS_COLLAPSING, true);
            }

            // Scan living entities in expanding radius
            AABB aoeBox = this.getBoundingBox().inflate(currentRadius, 6.0, currentRadius);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, aoeBox,
                e -> e.isAlive() && e != owner && center.distanceTo(e.position()) <= currentRadius
            );

            for (LivingEntity target : targets) {
                double distance = center.distanceTo(target.position());

                // 1. Gravity Well Pull
                Vec3 toCenter = center.subtract(target.position());
                if (toCenter.length() > 0.5) {
                    double pullStrength = Math.max(0.3, 1.4 * (1.0 - (distance / MAX_AOE_RADIUS)));
                    Vec3 pullVel = toCenter.normalize().scale(pullStrength);
                    target.setDeltaMovement(target.getDeltaMovement().add(pullVel.x, 0.15, pullVel.z));
                    target.hurtMarked = true;
                }

                // 2. Distance-based Melting (Closer = Higher tier, up to Level 10 / amplifier 9)
                int amplifier = Math.max(0, 9 - (int) Math.floor(distance * 9.0 / MAX_AOE_RADIUS));
                int durationSec = 30 + (amplifier * 3);
                target.addEffect(new MobEffectInstance(DbaEffects.MELTING_HOLDER, durationSec * 20, amplifier, false, true), owner);

                // 3. Sorrow Rift corruption & Cinematic Damage Lock
                target.addEffect(new MobEffectInstance(DbaEffects.SORROW_RIFT_HOLDER, 200, 0, false, true), owner);
                target.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 40, 0, false, false, false), owner);

                // 4. MC 26.2 Physics Attributes: Friction & Air Drag
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    target,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("sickle_rift_friction"),
                    -0.80,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    target,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("sickle_rift_drag"),
                    3.0,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );

                // 5. Initial Burst Damage Application
                if (!burstDamagedTargets.contains(target.getId())) {
                    burstDamagedTargets.add(target.getId());

                    DamageSource damageSource = owner instanceof Player playerOwner
                        ? serverLevel.damageSources().playerAttack(playerOwner)
                        : (owner instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobAttack(livingOwner) : serverLevel.damageSources().magic());
                    
                    target.hurtServer(serverLevel, damageSource, this.burstDamage);

                    // Soul Rend heal from AOE: 1% of burst damage per target hit
                    if (owner instanceof Player playerOwner) {
                        float aoeHeal = this.burstDamage * 0.01f;
                        playerOwner.heal(aoeHeal);
                    }
                }

                // Periodic tick damage inside the singularity
                if (this.tickCount % 10 == 0) {
                    DamageSource tickSource = owner instanceof Player playerOwner
                        ? serverLevel.damageSources().playerAttack(playerOwner)
                        : serverLevel.damageSources().magic();
                    target.hurtServer(serverLevel, tickSource, 35.0f);
                }
            }

            // Ambient audio cues
            if (this.tickCount == 1) {
                serverLevel.playSound(null, center.x, center.y, center.z,
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.5f, 0.6f);
                serverLevel.playSound(null, center.x, center.y, center.z,
                    SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 2.0f, 0.5f);
            }
            if (this.tickCount % 20 == 0) {
                serverLevel.playSound(null, center.x, center.y, center.z,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 1.2f, 0.7f + (currentRadius / 15.0f) * 0.3f);
            }

            if (this.tickCount >= MAX_LIFETIME) {
                serverLevel.playSound(null, center.x, center.y, center.z,
                    SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.PLAYERS, 2.0f, 0.7f);
                this.discard();
            }

        }
    }
}
