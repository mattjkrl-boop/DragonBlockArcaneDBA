package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.sound.DbaSounds;
import com.dragonblockarcanedba.util.DbaDamage;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Grand Crescent Wave Entity — Fired upon releasing Grand Cyclone (Tweak B).
 * Piercing glowing golden-white crescent energy wave that cuts through lines of enemies.
 *
 * On block contact the wave no longer discards at full brightness: it bursts
 * into a finisher class debris field with a ground burst and a permanent
 * conversion, then hangs for DISSIPATE_TICKS while the renderer fades it out.
 * Flying low over ground it sheds ground bursts along its path and carves a
 * real fissure into the ground track under it.
 */
public class GrandCrescentWaveEntity extends Projectile {
    private float damage = 600.0f;
    private final Set<Integer> hitEntityIds = new HashSet<>();
    /** Per wave cap on fissure blocks carved along the ground track. */
    private static final int CARVE_CAP = 90;
    private int carvedBlocks = 0;

    public static final int MAX_LIFE_TICKS = 80;
    public static final int DISSIPATE_TICKS = 12;

    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(GrandCrescentWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HIT_TICK = SynchedEntityData.defineId(GrandCrescentWaveEntity.class, EntityDataSerializers.INT);

    public GrandCrescentWaveEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public GrandCrescentWaveEntity(Level level, LivingEntity owner, float damage) {
        super(DbaEntities.GRAND_CRESCENT_WAVE, level);
        this.setOwner(owner);
        if (owner != null) {
            Vec3 eye = owner.getEyePosition();
            Vec3 look = owner.getLookAngle();
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);
            Vec3 up = right.cross(look).normalize();

            boolean isRightHanded = (owner.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (owner.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.GrandSwordItem && 
                !(owner.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.GrandSwordItem));
            boolean onRight = isRightHanded ? !isOffhand : isOffhand;
            float sideSign = onRight ? 1.0f : -1.0f;

            Vec3 spawnPos = eye.add(look.scale(1.0)).add(right.scale(sideSign * 0.35)).add(up.scale(-0.30));
            this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
            this.entityData.set(CASTER_ID, owner.getId());
        }
        this.damage = damage;
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CASTER_ID, -1);
        builder.define(HIT_TICK, -1);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    /** Tick at which the wave struck a block, or -1 while it is still flying. */
    public int getHitTick() {
        return this.entityData.get(HIT_TICK);
    }

    public boolean hasHit() {
        return this.getHitTick() >= 0;
    }

    @Override
    public void tick() {
        super.tick();

        // Dissipating after a block strike: hang in place while the renderer
        // fades the crescent out, then discard.
        if (this.hasHit()) {
            if (!this.level().isClientSide() && this.tickCount - this.getHitTick() >= DISSIPATE_TICKS) {
                this.discard();
            }
            return;
        }

        Vec3 movement = this.getDeltaMovement();
        double nextX = this.getX() + movement.x;
        double nextY = this.getY() + movement.y;
        double nextZ = this.getZ() + movement.z;
        this.setPos(nextX, nextY, nextZ);

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Theme particles: gold spark trail shed along the flight path,
            // mirroring the Z Shockwave travel feedback pattern.
            if (this.tickCount % 2 == 0) {
                float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
                if (density > 0f) {
                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                        this.getX(), this.getY() + 0.4, this.getZ(),
                        Math.max(2, Math.round(6f * density)),
                        1.6, 0.8, 1.6, 0.06);
                }
            }

            // Ground contact: flying low over solid ground, the wave's
            // underside tears dust off the surface as it passes, and every
            // second tick its ground track is carved into a real fissure
            // (width 1, one deep, WEAK tier, capped per wave).
            if (this.tickCount % 4 == 0) {
                net.minecraft.core.BlockPos belowPos = net.minecraft.core.BlockPos.containing(
                    this.getX(), this.getY() - 1.4, this.getZ());
                if (!serverLevel.getBlockState(belowPos).isAir()
                    && serverLevel.getBlockState(belowPos.above()).isAir()) {
                    com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel,
                        new Vec3(this.getX(), belowPos.getY() + 1.0, this.getZ()),
                        1.6f, 0xFFD833);
                }
            }
            if (this.tickCount % 2 == 0 && this.carvedBlocks < CARVE_CAP && movement.lengthSqr() > 0.001) {
                net.minecraft.core.BlockPos belowPos = net.minecraft.core.BlockPos.containing(
                    this.getX(), this.getY() - 1.4, this.getZ());
                if (!serverLevel.getBlockState(belowPos).isAir()
                    && serverLevel.getBlockState(belowPos.above()).isAir()) {
                    this.carvedBlocks += com.dragonblockarcanedba.util.PermanentDestruction.fissure(
                        serverLevel, belowPos.above(), movement, movement.length() * 2.0,
                        1, 1, false,
                        com.dragonblockarcanedba.util.PermanentDestruction.TIER_WEAK,
                        CARVE_CAP - this.carvedBlocks);
                }
            }

            Entity owner = this.getOwner();
            LivingEntity waveOwner = owner instanceof LivingEntity living ? living : null;
            DamageSource damageSource = DbaDamage.valor(this, waveOwner);

            // Broad horizontal piercing hitbox
            double halfWidth = 3.2;
            double halfHeight = 1.2;
            AABB hitbox = this.getBoundingBox().inflate(halfWidth, halfHeight, halfWidth);

            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, hitbox,
                e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
            );

            for (LivingEntity target : targets) {
                hitEntityIds.add(target.getId());

                // Pierce and damage
                target.hurtServer(serverLevel, damageSource, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (this.damage)));

                // Apply invisible cinematic tracking effect so Delayed Damage combo locks during wave sweep
                if (owner instanceof LivingEntity livingOwner) {
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 30, 0, false, false, false
                    ), livingOwner);
                }

                // Knockback in wave trajectory
                Vec3 knockback = movement.normalize().scale(1.5);
                target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.3, knockback.z));
                com.dragonblockarcanedba.util.DbaCompat.markHurt(target);
            }

            // Block contact: burst into debris, convert the struck ground,
            // and dissipate over DISSIPATE_TICKS instead of discarding at
            // full brightness in one tick.
            Vec3 start = this.position();
            Vec3 end = start.add(movement);
            HitResult blockHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (blockHit.getType() != HitResult.Type.MISS) {
                this.entityData.set(HIT_TICK, this.tickCount);
                this.setDeltaMovement(Vec3.ZERO);
                Vec3 hitPos = blockHit instanceof net.minecraft.world.phys.BlockHitResult bhr
                    ? bhr.getLocation() : this.position();
                com.dragonblockarcanedba.util.DebrisSpawner.spawnField(serverLevel, waveOwner,
                    hitPos, 26, 2.6, 2.8, 30);
                com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel, hitPos, 2.4f, 0xFFD833);
                net.minecraft.core.BlockPos scarPos = net.minecraft.core.BlockPos.containing(hitPos.x, hitPos.y - 1.0, hitPos.z);
                if (!serverLevel.getBlockState(scarPos).isAir()) {
                    com.dragonblockarcanedba.util.PermanentDestruction.convertDisk(serverLevel, scarPos, 1,
                        net.minecraft.world.level.block.Blocks.COARSE_DIRT.defaultBlockState(),
                        0.5f, serverLevel.getRandom());
                }
                serverLevel.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                    DbaSounds.RELEASE_BLAST, net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, 1.3f);
                return;
            }

            if (this.tickCount > MAX_LIFE_TICKS) {
                this.discard();
            }
        }
    }
}
