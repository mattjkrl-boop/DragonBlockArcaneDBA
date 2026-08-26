package com.dragonblockarcanedba.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ox Shockwave Entity — Expanding 360-degree ground shockwave unleashed by Ox King's Axe.
 * 
 * Features:
 * - 360-degree expanding ring radius scaling with charge (up to 24 blocks max - Tweak A).
 * - Violent upward and outward radial knockback.
 * - Tweak B: Concentric trailing echo waves at >= 50% charge.
 * - Tweak C: Max charge destroys weak terrain (blast resistance <= 2.0) and spawns ground fissures.
 */
public class OxShockwaveEntity extends Projectile {
    private static final EntityDataAccessor<Float> CHARGE_RATIO = SynchedEntityData.defineId(OxShockwaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_SUB_WAVE = SynchedEntityData.defineId(OxShockwaveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> CURRENT_RADIUS = SynchedEntityData.defineId(OxShockwaveEntity.class, EntityDataSerializers.FLOAT);

    private float damage = 400.0f;
    private float maxRadius = 12.0f;
    private int maxTicks = 20;
    private boolean hasSpawnedEcho1 = false;
    private boolean hasSpawnedEcho2 = false;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public OxShockwaveEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public OxShockwaveEntity(Level level, LivingEntity owner, float chargeRatio, float damage, boolean isSubWave) {
        super(DbaEntities.OX_SHOCKWAVE, level);
        this.setOwner(owner);
        if (owner != null) {
            this.setPos(owner.getX(), owner.getY() + 0.1, owner.getZ());
        }
        this.damage = damage;
        this.noPhysics = true;

        float clampedCharge = Math.max(0.05f, Math.min(1.0f, chargeRatio));
        this.entityData.set(CHARGE_RATIO, clampedCharge);
        this.entityData.set(IS_SUB_WAVE, isSubWave);
        this.entityData.set(CURRENT_RADIUS, 0.5f);

        // Max radius: 4 to 24 blocks (Tweak A)
        this.maxRadius = 4.0f + (clampedCharge * 20.0f);
        if (isSubWave) {
            this.maxRadius *= 0.75f;
        }
        // Expand speed: approx 0.8 blocks per tick
        this.maxTicks = Math.max(10, (int) (this.maxRadius * 1.3f));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CHARGE_RATIO, 1.0f);
        builder.define(IS_SUB_WAVE, false);
        builder.define(CURRENT_RADIUS, 0.5f);
    }

    public float getChargeRatio() {
        return this.entityData.get(CHARGE_RATIO);
    }

    public boolean isSubWave() {
        return this.entityData.get(IS_SUB_WAVE);
    }

    public float getCurrentRadius() {
        return this.entityData.get(CURRENT_RADIUS);
    }

    @Override
    public void tick() {
        super.tick();

        float progress = Math.min(1.0f, (float) this.tickCount / (float) this.maxTicks);
        float currentRadius = 0.5f + (progress * (this.maxRadius - 0.5f));
        this.entityData.set(CURRENT_RADIUS, currentRadius);

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            float charge = getChargeRatio();
            boolean subWave = isSubWave();
            Vec3 center = this.position();
            Entity owner = this.getOwner();

            // Initial burst sound & explosion at start
            if (this.tickCount == 1 && !subWave) {
                if (charge >= 0.95f) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 0.5, center.z, 2, 0, 0, 0, 0);
                    serverLevel.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5f, 0.7f);

                    // Tweak C: Spawn temporary ground fissures radiating outward
                    for (int f = 0; f < 6; f++) {
                        double fAngle = (f * (Math.PI / 3.0)) + (serverLevel.getRandom().nextDouble() * 0.3);
                        double fDist = 3.0 + serverLevel.getRandom().nextDouble() * (this.maxRadius * 0.6);
                        double fx = center.x + Math.cos(fAngle) * fDist;
                        double fz = center.z + Math.sin(fAngle) * fDist;
                        OxFissureEntity fissure = new OxFissureEntity(serverLevel, owner instanceof LivingEntity ? (LivingEntity) owner : null, new Vec3(fx, center.y, fz), 50.0f);
                        serverLevel.addFreshEntity(fissure);
                    }
                }
            }

            // Tweak B: Secondary concentric ring echo waves at >= 50% charge
            if (charge >= 0.50f && !subWave) {
                if (!hasSpawnedEcho1 && this.tickCount == 6) {
                    hasSpawnedEcho1 = true;
                    OxShockwaveEntity echo1 = new OxShockwaveEntity(serverLevel, owner instanceof LivingEntity ? (LivingEntity) owner : null, charge * 0.8f, this.damage * 0.40f, true);
                    echo1.setPos(center.x, center.y, center.z);
                    serverLevel.addFreshEntity(echo1);
                }
                if (!hasSpawnedEcho2 && this.tickCount == 12) {
                    hasSpawnedEcho2 = true;
                    OxShockwaveEntity echo2 = new OxShockwaveEntity(serverLevel, owner instanceof LivingEntity ? (LivingEntity) owner : null, charge * 0.6f, this.damage * 0.40f, true);
                    echo2.setPos(center.x, center.y, center.z);
                    serverLevel.addFreshEntity(echo2);
                }
            }

            // Terrain destruction along wave path at max charge
            if (charge >= 0.95f && !subWave) {
                int destroyPoints = Math.max(8, (int) (currentRadius * 2));
                for (int i = 0; i < destroyPoints; i++) {
                    double angle = (i / (double) destroyPoints) * Math.PI * 2.0;
                    double px = center.x + Math.cos(angle) * currentRadius;
                    double pz = center.z + Math.sin(angle) * currentRadius;
                    BlockPos bPos = BlockPos.containing(px, center.y, pz);
                    checkAndDestroyWeakBlock(serverLevel, bPos);
                    checkAndDestroyWeakBlock(serverLevel, bPos.above());
                }
            }

            // Hit detection in current annular ring
            double innerR = Math.max(0.0, currentRadius - 1.8);
            double outerR = currentRadius + 1.8;

            AABB box = new AABB(
                center.x - outerR, center.y - 2.0, center.z - outerR,
                center.x + outerR, center.y + 3.0, center.z + outerR
            );

            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
            );

            DamageSource damageSource = owner instanceof net.minecraft.world.entity.player.Player playerOwner
                ? serverLevel.damageSources().playerAttack(playerOwner)
                : (owner instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobAttack(livingOwner) : serverLevel.damageSources().generic());

            for (LivingEntity target : targets) {
                double dist = Math.sqrt(target.distanceToSqr(center.x, target.getY(), center.z));
                if (dist >= innerR && dist <= outerR) {
                    hitEntityIds.add(target.getId());

                    // Deal massive groundbreaker damage
                    target.hurtServer(serverLevel, damageSource, this.damage);

                    // Apply cinematic CC & tracking so Delayed Damage combo locks during knockup airtime
                    if (owner instanceof LivingEntity livingOwner) {
                        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 30, 0, false, false, false
                        ), livingOwner);
                        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            com.dragonblockarcanedba.effect.DbaEffects.MOVEMENT_CURSE_HOLDER, 30, 1, false, true
                        ), livingOwner);
                    }

                    // Violent radial and vertical knockback
                    Vec3 toTarget = target.position().subtract(center);
                    Vec3 flatDir = new Vec3(toTarget.x, 0, toTarget.z);
                    if (flatDir.lengthSqr() < 0.001) {
                        flatDir = new Vec3(1, 0, 0);
                    } else {
                        flatDir = flatDir.normalize();
                    }

                    // MC 26.2 Physics: Groundbreaker violent shockwave bounce and low drag
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        target,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("ox_shockwave_bounce"),
                        0.90,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    );
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        target,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("ox_shockwave_drag"),
                        -0.50,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    );

                    double horizontalPower = 1.4 + (charge * 1.8);
                    double verticalPower = 0.6 + (charge * 0.7);

                    Vec3 currentVel = target.getDeltaMovement();
                    target.setDeltaMovement(
                        currentVel.x + flatDir.x * horizontalPower,
                        Math.max(currentVel.y, 0.2) + verticalPower,
                        currentVel.z + flatDir.z * horizontalPower
                    );
                    target.hurtMarked = true;

                    // Impact particles on target
                    serverLevel.sendParticles(
                        ParticleTypes.EXPLOSION,
                        target.getX(), target.getY() + 0.5, target.getZ(),
                        2, 0.2, 0.2, 0.2, 0.0
                    );
                }
            }

            // Expiry
            if (this.tickCount >= this.maxTicks) {
                this.discard();
            }
        }
    }

    private void checkAndDestroyWeakBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;

        Block block = state.getBlock();
        // Check blast resistance <= 2.0 (dirt, grass, flowers, leaves, glass, ice, snow, etc.)
        float blastRes = block.getExplosionResistance();
        if (blastRes <= 2.0f && !state.is(Blocks.BEDROCK) && !state.is(Blocks.BARRIER)) {
            level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                6, 0.2, 0.2, 0.2, 0.05
            );
            level.destroyBlock(pos, true);
        }
    }
}
