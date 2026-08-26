package com.dragonblockarcanedba.entity;

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
 * Dimensional Warp Rift Entity — Physical 3D Spacetime Tear and Gravitational Void Portal
 * spawned at the departure origin and arrival destination during Dimensional Sword teleportation.
 */
public class DimensionalWarpRiftEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> IS_DESTINATION = SynchedEntityData.defineId(DimensionalWarpRiftEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(DimensionalWarpRiftEntity.class, EntityDataSerializers.FLOAT);

    public static final int MAX_LIFETIME = 24; // 1.2 seconds

    private float burstDamage = 750.0f;
    private final Set<Integer> damagedTargets = new HashSet<>();

    public DimensionalWarpRiftEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DimensionalWarpRiftEntity(Level level, LivingEntity owner, Vec3 pos, float radius, boolean isDestination, float damage) {
        super(DbaEntities.DIMENSIONAL_WARP_RIFT, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.burstDamage = damage;
        this.noPhysics = true;
        this.entityData.set(RADIUS, radius);
        this.entityData.set(IS_DESTINATION, isDestination);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IS_DESTINATION, false);
        builder.define(RADIUS, 3.5f);
    }

    public boolean isDestination() {
        return this.entityData.get(IS_DESTINATION);
    }

    public float getRiftRadius() {
        return this.entityData.get(RADIUS);
    }

    public int getMaxLifetime() {
        return MAX_LIFETIME;
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 center = this.position();
        Entity owner = this.getOwner();
        float radius = this.getRiftRadius();
        boolean isDest = this.isDestination();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Audio cue on initial spawn
            if (this.tickCount == 1) {
                if (!isDest) {
                    serverLevel.playSound(null, center.x, center.y, center.z,
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0f, 0.7f);
                    serverLevel.playSound(null, center.x, center.y, center.z,
                        SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.8f, 0.6f);
                    serverLevel.playSound(null, center.x, center.y, center.z,
                        SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.5f, 1.4f);
                } else {
                    serverLevel.playSound(null, center.x, center.y, center.z,
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.2f, 0.8f);
                    serverLevel.playSound(null, center.x, center.y, center.z,
                        SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.PLAYERS, 2.0f, 0.6f);
                    serverLevel.playSound(null, center.x, center.y, center.z,
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5f, 0.5f);
                }
            }

            // Damage and Physics distortion for entities in the rift area
            AABB aoeBox = new AABB(
                center.x - radius, center.y - 1.5, center.z - radius,
                center.x + radius, center.y + 2.5, center.z + radius
            );
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, aoeBox,
                e -> e.isAlive() && e != owner && center.distanceTo(e.position()) <= radius
            );

            for (LivingEntity target : targets) {
                if (!damagedTargets.contains(target.getId())) {
                    damagedTargets.add(target.getId());

                    DamageSource damageSource = owner instanceof Player playerOwner
                        ? serverLevel.damageSources().playerAttack(playerOwner)
                        : (owner instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobAttack(livingOwner) : serverLevel.damageSources().magic());

                    target.hurtServer(serverLevel, damageSource, this.burstDamage);
                    target.addEffect(new MobEffectInstance(DbaEffects.MELTING_HOLDER, 200, 1, false, true), owner);

                    // MC 26.2 Physics: Spatial rift warp friction & drag distortion
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        target,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("dimensional_wave_friction"),
                        -0.90,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    );
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        target,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("dimensional_wave_drag"),
                        -0.80,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    );

                    // Gravitational force: pull towards origin rift or push outward from destination rift
                    if (!isDest) {
                        Vec3 toCenter = center.subtract(target.position());
                        if (toCenter.length() > 0.3) {
                            Vec3 pull = toCenter.normalize().scale(0.35);
                            target.setDeltaMovement(target.getDeltaMovement().add(pull.x, 0.1, pull.z));
                            target.hurtMarked = true;
                        }
                    } else {
                        Vec3 fromCenter = target.position().subtract(center);
                        if (fromCenter.length() > 0.1) {
                            Vec3 push = fromCenter.normalize().scale(0.5);
                            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.2, push.z));
                            target.hurtMarked = true;
                        }
                    }
                }
            }

            if (this.tickCount >= MAX_LIFETIME) {
                this.discard();
            }
        }
    }
}
