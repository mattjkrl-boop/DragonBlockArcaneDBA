package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
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
 * Z Shockwave Entity — Massive horizontal energy wave unleashed by the Z Sword.
 *
 * Features:
 * - The hit test follows the visible crescent (span and chord match the
 *   renderer exactly), so the damage shape and the picture agree, and targets
 *   beside or behind the wave are no longer hit by a square box.
 * - Heavy damage and devastating outward knockback.
 * - Tweak A: Spawns slower trailing sub-waves at max charge.
 * - Tweak B: Ignores terrain and penetrates solid blocks at max charge.
 * - Tweak C: Direct max-charge hits bury enemies 2 blocks into the ground, dealing head-in-blocks crush damage.
 *   The bury only happens when both destination cells are actually solid;
 *   a target over air, water or a ledge keeps the root but is not teleported.
 */
public class ZShockwaveEntity extends Projectile {
    private static final EntityDataAccessor<Float> CHARGE_RATIO = SynchedEntityData.defineId(ZShockwaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_SUB_WAVE = SynchedEntityData.defineId(ZShockwaveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(ZShockwaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(ZShockwaveEntity.class, EntityDataSerializers.INT);

    private float damage = 500.0f;
    private boolean pierceBlocks = false;
    private boolean rootOnHit = false;
    private boolean hasSpawnedSubWaves = false;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public ZShockwaveEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public ZShockwaveEntity(Level level, LivingEntity owner, float chargeRatio, float damage, boolean isSubWave) {
        super(DbaEntities.Z_SHOCKWAVE, level);
        this.setOwner(owner);
        if (owner != null) {
            Vec3 eye = owner.getEyePosition();
            Vec3 look = owner.getLookAngle();
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);
            Vec3 up = right.cross(look).normalize();

            boolean isRightHanded = (owner.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (owner.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.ZSwordItem && 
                !(owner.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.ZSwordItem));
            boolean onRight = isRightHanded ? !isOffhand : isOffhand;
            float sideSign = onRight ? 1.0f : -1.0f;

            Vec3 spawnPos = eye.add(look.scale(1.2)).add(right.scale(sideSign * 0.35)).add(up.scale(-0.35));
            this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
            this.entityData.set(CASTER_ID, owner.getId());
        }
        this.damage = damage;
        this.noPhysics = true;

        this.entityData.set(CHARGE_RATIO, chargeRatio);
        this.entityData.set(IS_SUB_WAVE, isSubWave);
        this.entityData.set(SEED, level.getRandom().nextInt());

        // Tweak B & C are unlocked at maximum charge (>= 90%)
        if (chargeRatio >= 0.90f && !isSubWave) {
            this.pierceBlocks = true;
            this.rootOnHit = true;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CHARGE_RATIO, 0.0f);
        builder.define(IS_SUB_WAVE, false);
        builder.define(CASTER_ID, -1);
        builder.define(SEED, 0);
    }

    // Persist the combat fields: the synced charge is not saved by vanilla, and
    // damage, pierce and root live in plain fields, so a reloaded wave used to
    // come back with default damage and no tweaks.
    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("ChargeRatio", getChargeRatio());
        output.putBoolean("IsSubWave", isSubWave());
        output.putFloat("Damage", this.damage);
        output.putBoolean("PierceBlocks", this.pierceBlocks);
        output.putBoolean("RootOnHit", this.rootOnHit);
        output.putBoolean("HasSpawnedSubWaves", this.hasSpawnedSubWaves);
        output.putInt("Seed", getSeed());
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(CHARGE_RATIO, input.getFloatOr("ChargeRatio", 0.0f));
        this.entityData.set(IS_SUB_WAVE, input.getBooleanOr("IsSubWave", false));
        this.damage = input.getFloatOr("Damage", 500.0f);
        this.pierceBlocks = input.getBooleanOr("PierceBlocks", false);
        this.rootOnHit = input.getBooleanOr("RootOnHit", false);
        this.hasSpawnedSubWaves = input.getBooleanOr("HasSpawnedSubWaves", false);
        this.entityData.set(SEED, input.getIntOr("Seed", 0));
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    public float getChargeRatio() {
        return this.entityData.get(CHARGE_RATIO);
    }

    public boolean isSubWave() {
        return this.entityData.get(IS_SUB_WAVE);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 movement = this.getDeltaMovement();
        double nextX = this.getX() + movement.x;
        double nextY = this.getY() + movement.y;
        double nextZ = this.getZ() + movement.z;
        this.setPos(nextX, nextY, nextZ);

        float charge = getChargeRatio();
        boolean subWave = isSubWave();

        // Wave dimensions now match the renderer exactly (ZShockwaveRenderer):
        // span is the crescent half width, chord its forward bulge, height its
        // half height at the centre. The old box was 8 half width and 1.6 half
        // height against an 11 and 3.3 visual, and square on top of that.
        double span = subWave ? (3.2 + charge * 3.2) : (4.8 + charge * 6.2);
        double chord = subWave ? (1.0 + charge * 0.6) : (1.6 + charge * 1.4);
        double height = subWave ? (0.9 + charge * 0.8) : (1.5 + charge * 1.8);

        // Horizontal facing basis from the wave yaw (renderer rotates the same way).
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 fwdH = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 rightH = new Vec3(-fwdH.z, 0, fwdH.x);

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Travel feedback, layered every tick in the standard three bands so
            // the trail is a continuous band instead of puffs every 5.6 blocks:
            // Ki motes at the dense core, theme sparks along the crescent band,
            // wind wisps in the wake. Main waves shed sparks, sub-waves embers.
            float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
            if (density > 0f) {
                var rand = serverLevel.getRandom();
                double baseY = this.getY() + 0.6;
                // Core: bright motes clustered at the apex.
                int core = Math.max(1, Math.round(4f * density));
                for (int i = 0; i < core; i++) {
                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
                        this.getX() + fwdH.x * chord * 0.7, baseY, this.getZ() + fwdH.z * chord * 0.7,
                        1, span * 0.10, height * 0.3, span * 0.10, 0.02);
                }
                // Mid: theme particles sampled along the crescent curve itself.
                int mid = Math.max(2, Math.round((float) span * 1.2f * density));
                for (int i = 0; i < mid; i++) {
                    double t = rand.nextDouble() * 2.0 - 1.0;
                    double curveZ = (1.0 - t * t) * chord;
                    double px = this.getX() + rightH.x * t * span + fwdH.x * curveZ;
                    double pz = this.getZ() + rightH.z * t * span + fwdH.z * curveZ;
                    double py = baseY + (rand.nextDouble() - 0.5) * height * (1.0 - Math.pow(Math.abs(t), 1.8));
                    serverLevel.sendParticles(
                        subWave ? com.dragonblockarcanedba.particle.DbaParticles.EMBER : com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                        px, py, pz, 1, 0.05, 0.05, 0.05, 0.08);
                }
                // Outer: slow wind wisps trailing behind the band.
                if (this.tickCount % 2 == 0) {
                    int outer = Math.max(1, Math.round(3f * density));
                    for (int i = 0; i < outer; i++) {
                        double t = rand.nextDouble() * 2.0 - 1.0;
                        double px = this.getX() + rightH.x * t * span - fwdH.x * 1.2;
                        double pz = this.getZ() + rightH.z * t * span - fwdH.z * 1.2;
                        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.WIND,
                            px, baseY, pz, 1, 0.15, 0.2, 0.15, 0.03);
                    }
                }
            }
            if (this.tickCount % 15 == 0) {
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.SWING_HEAVY, net.minecraft.sounds.SoundSource.HOSTILE, 1.1f, 0.55f);
            }

            // Tweak A: Spawn trailing sub-waves at max charge
            if (charge >= 0.90f && !subWave && !hasSpawnedSubWaves && this.tickCount == 3) {
                this.hasSpawnedSubWaves = true;
                Vec3 look = movement.normalize();
                Vec3 basePos = this.position();

                for (int i = 1; i <= 2; i++) {
                    ZShockwaveEntity trailWave = new ZShockwaveEntity(this.level(), (LivingEntity) this.getOwner(), charge * 0.7f, this.damage * 0.45f, true);
                    trailWave.setPos(basePos.x - look.x * (i * 2.0), basePos.y, basePos.z - look.z * (i * 2.0));
                    trailWave.setDeltaMovement(look.scale(movement.length() * 0.65));
                    serverLevel.addFreshEntity(trailWave);
                }
            }

            // Hit detection follows the crescent: a generous box pre-filter, then
            // a per target test in wave space. A target counts only when it sits
            // on the crescent band: lateral offset within the span, forward offset
            // near the curve (1 - t^2) * chord, vertical offset within the
            // crescent height at that lateral position, which tapers to the tips
            // exactly like the renderer.
            AABB hitbox = this.getBoundingBox().inflate(span + 1.0, height + 1.0, span + 1.0);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, hitbox, e -> e.isAlive() && e != this.getOwner() && !hitEntityIds.contains(e.getId()));

            Entity owner = this.getOwner();
            for (LivingEntity target : targets) {
                double relX = target.getX() - this.getX();
                double relZ = target.getZ() - this.getZ();
                double lateral = relX * rightH.x + relZ * rightH.z;
                double forward = relX * fwdH.x + relZ * fwdH.z;
                double t = lateral / span;
                if (Math.abs(t) > 1.0) continue;
                double curveZ = (1.0 - t * t) * chord;
                if (Math.abs(forward - curveZ) > 1.4) continue;
                double heightAt = height * (1.0 - Math.pow(Math.abs(t), 1.8)) + 0.5;
                double centerY = target.getY() + target.getBbHeight() * 0.5;
                if (Math.abs(centerY - (this.getY() + 0.6)) > heightAt) continue;

                hitEntityIds.add(target.getId());

                // Deal massive damage
                target.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.gravity(this, owner instanceof LivingEntity ? (LivingEntity) owner : null), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (this.damage)));

                // Heavy knockback
                Vec3 knockback = movement.normalize().scale(1.8 + (charge * 2.2));
                target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.4 + (charge * 0.3), knockback.z));
                com.dragonblockarcanedba.util.DbaCompat.markHurt(target);

                // Tweak C: Maximum charge roots enemies into the ground 2 blocks down and crushes them
                if (this.rootOnHit && !subWave && charge >= 0.90f) {
                    // The teleport used to be unchecked: a target over air, water
                    // or a ledge was dropped 2 blocks and crushed anyway. Bury
                    // only when the two cells the body would occupy are solid;
                    // otherwise the root still applies without the teleport.
                    BlockPos feet = target.blockPosition();
                    boolean buriedCellsSolid = !serverLevel.getBlockState(feet.below(1)).getCollisionShape(serverLevel, feet.below(1)).isEmpty()
                        && !serverLevel.getBlockState(feet.below(2)).getCollisionShape(serverLevel, feet.below(2)).isEmpty();
                    if (buriedCellsSolid) {
                        target.teleportTo(target.getX(), target.getY() - 2.0, target.getZ());
                        // Suffocation / block crush damage from head embedded in terrain
                        target.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.crush(this, owner instanceof LivingEntity livingOwner ? livingOwner : null), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (250.0f)));
                    }
                    target.addEffect(new MobEffectInstance(DbaEffects.MOVEMENT_CURSE_HOLDER, 80, 9, false, true), owner instanceof LivingEntity livingOwner ? livingOwner : null);
                    // The crush used to be a silent teleport: give it debris, shake, and a crunch.
                    com.dragonblockarcanedba.fx.SlamDebris.burst(serverLevel, target.position(), 1.6, com.dragonblockarcanedba.fx.SlamDebris.Profile.Z_GRAVITY);
                    com.dragonblockarcanedba.fx.DbaImpactFx.medium(serverLevel, target.getX(), target.getY(), target.getZ());
                    serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                        com.dragonblockarcanedba.sound.DbaSounds.GROUND_CRACK, net.minecraft.sounds.SoundSource.HOSTILE, 1.2f, 0.7f);
                }
            }

            // Terrain check if not block-piercing (Tweak B)
            if (!this.pierceBlocks) {
                Vec3 start = this.position();
                Vec3 end = start.add(movement);
                HitResult blockHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (blockHit.getType() != HitResult.Type.MISS) {
                    // The wave used to discard at full opacity against a wall.
                    // A burst of sparks and dust at the impact point masks the end.
                    if (density > 0f && blockHit instanceof net.minecraft.world.phys.BlockHitResult blockResult) {
                        Vec3 hitPos = blockResult.getLocation();
                        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                            hitPos.x, hitPos.y, hitPos.z,
                            Math.max(4, Math.round(18f * density)), span * 0.4, height * 0.5, span * 0.4, 0.12);
                        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.DUST_PUFF,
                            hitPos.x, hitPos.y, hitPos.z,
                            Math.max(2, Math.round(8f * density)), span * 0.3, 0.4, span * 0.3, 0.05);
                    }
                    serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        com.dragonblockarcanedba.sound.DbaSounds.SLAM_THUD, net.minecraft.sounds.SoundSource.HOSTILE, 0.9f, 0.8f);
                    this.discard();
                }
            }

            // Discard after traveling for 4 seconds (80 ticks)
            if (this.tickCount > 80) {
                this.discard();
            }
        }
    }
}
