package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
 * Blaster Bolt Entity — Laser projectile fired by the Blaster Gun.
 * Rapid physical 3D energy projectile with piercing, heat scaling, and explosive Overcharged Blast.
 */
public class BlasterBoltEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> IS_OVERCHARGED = SynchedEntityData.defineId(BlasterBoltEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> HEAT_RATIO = SynchedEntityData.defineId(BlasterBoltEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(BlasterBoltEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> IMPACT_AGE = SynchedEntityData.defineId(BlasterBoltEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(BlasterBoltEntity.class, EntityDataSerializers.INT);

    private float damage = 250.0f;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public BlasterBoltEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BlasterBoltEntity(Level level, LivingEntity owner, float damage, boolean isOvercharged, float heatRatio) {
        super(DbaEntities.BLASTER_BOLT, level);
        this.setOwner(owner);
        if (owner != null) {
            Vec3 eye = owner.getEyePosition();
            Vec3 look = owner.getLookAngle();
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);
            Vec3 up = right.cross(look).normalize();

            boolean isRightHanded = (owner.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (owner.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.BlasterGunItem && 
                !(owner.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.BlasterGunItem));
            boolean onRight = isRightHanded ? !isOffhand : isOffhand;
            float sideSign = onRight ? 1.0f : -1.0f;

            Vec3 spawnPos = eye.add(look.scale(0.5)).add(right.scale(sideSign * 0.32)).add(up.scale(-0.25));
            this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
            this.entityData.set(CASTER_ID, owner.getId());
        }
        this.damage = damage;
        this.entityData.set(IS_OVERCHARGED, isOvercharged);
        this.entityData.set(HEAT_RATIO, heatRatio);
        this.entityData.set(SEED, level.getRandom().nextInt());
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IS_OVERCHARGED, false);
        builder.define(HEAT_RATIO, 0.0f);
        builder.define(CASTER_ID, -1);
        builder.define(IMPACT_AGE, -1);
        builder.define(SEED, 0);
    }

    /** Synced seed for the bespoke renderer: same seed, same silhouette, every client. */
    public int getSeed() {
        return this.entityData.get(SEED);
    }

    public int getImpactAge() {
        return this.entityData.get(IMPACT_AGE);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    public boolean isOvercharged() {
        return this.entityData.get(IS_OVERCHARGED);
    }

    public float getHeatRatio() {
        return this.entityData.get(HEAT_RATIO);
    }

    @Override
    public void tick() {
        super.tick();

        int impactAge = this.getImpactAge();
        if (impactAge >= 0) {
            this.setDeltaMovement(Vec3.ZERO);
            if (!this.level().isClientSide()) {
                if (impactAge >= 5) {
                    this.discard();
                } else {
                    this.entityData.set(IMPACT_AGE, impactAge + 1);
                }
            }
            return;
        }

        Vec3 movement = this.getDeltaMovement();
        double nextX = this.getX() + movement.x;
        double nextY = this.getY() + movement.y;
        double nextZ = this.getZ() + movement.z;

        // Bolts pierce entities by design, but they used to fly through solid blocks
        // for their entire 30-tick life. Terrain now stops them, like the beam does.
        if (movement.lengthSqr() > 0.0) {
            Vec3 startPos = this.position();
            Vec3 endPos = new Vec3(nextX, nextY, nextZ);
            net.minecraft.world.phys.HitResult blockHit = this.level().clip(new net.minecraft.world.level.ClipContext(
                startPos, endPos, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this));
            if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                Vec3 hitPoint = blockHit.getLocation();
                this.setPos(hitPoint.x, hitPoint.y, hitPoint.z);
                this.setDeltaMovement(Vec3.ZERO);
                if (!this.level().isClientSide() && this.level() instanceof ServerLevel impactLevel) {
                    impactLevel.playSound(null, hitPoint.x, hitPoint.y, hitPoint.z,
                        com.dragonblockarcanedba.sound.DbaSounds.EXTINGUISH, SoundSource.PLAYERS, 0.7f, 1.6f);
                    this.entityData.set(IMPACT_AGE, 0);
                    emitImpactBurst(impactLevel, hitPoint);
                }
                return;
            }
        }
        this.setPos(nextX, nextY, nextZ);

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Entity owner = this.getOwner();

            // Flight trail (spectacle pass): the bolt used to shed no particles
            // at all, so a 72 block flight was a bare mesh. Sparks stream off
            // the tail every tick and a mote rides the core; the overcharged
            // bolt also sheds embers.
            float trailDensity = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
            if (trailDensity > 0f && movement.lengthSqr() > 0.0) {
                Vec3 back = movement.normalize();
                Vec3 trailPos = this.position().add(back.scale(-0.5));
                int sparkCount = Math.max(1, Math.round((isOvercharged() ? 3f : 2f) * trailDensity));
                serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                    trailPos.x, trailPos.y, trailPos.z, sparkCount, 0.08, 0.08, 0.08, 0.02);
                serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
                    this.getX(), this.getY(), this.getZ(), 1, 0.05, 0.05, 0.05, 0.01);
                if (isOvercharged() && this.tickCount % 2 == 0) {
                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.EMBER,
                        trailPos.x, trailPos.y, trailPos.z, 1, 0.10, 0.10, 0.10, 0.03);
                }
            }

            // Bespoke pass: ability damage uses the mod's own ki damage type
            // (rule 4), with this bolt as the direct entity. The plain melee
            // hit of the gun itself is untouched and stays player attack.
            DamageSource damageSource = com.dragonblockarcanedba.util.DbaDamage.ki(this,
                owner instanceof LivingEntity livingOwner ? livingOwner : null);

            boolean overcharged = isOvercharged();
            float heat = getHeatRatio();
            double hitRadius = overcharged ? 1.5 : (0.6 + heat * 0.4);
            AABB hitbox = this.getBoundingBox().inflate(hitRadius, hitRadius, hitRadius);

            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, hitbox,
                e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
            );

            for (LivingEntity target : targets) {
                hitEntityIds.add(target.getId());

                target.hurtServer(serverLevel, damageSource, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (this.damage)));

                // Hit spark burst so a piercing hit reads at the target, not
                // only in its health bar.
                float hitDensity = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
                if (hitDensity > 0f) {
                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        Math.max(2, Math.round(4f * hitDensity)), 0.25, 0.25, 0.25, 0.10);
                }

                if (owner instanceof LivingEntity livingOwner) {
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 15, 0, false, false, false
                    ), livingOwner);
                }

                // Knockback
                Vec3 knockback = movement.normalize().scale(overcharged ? 1.5 : 0.6);
                target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.2, knockback.z));
                com.dragonblockarcanedba.util.DbaCompat.markHurt(target);

                if (overcharged) {
                    // Overcharged explosive detonation
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        target,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("blaster_bolt_bounce"),
                        0.75,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    );
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        target,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("blaster_bolt_drag"),
                        -0.30,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    );

                    AABB aoe = target.getBoundingBox().inflate(3.0);
                    List<LivingEntity> splashTargets = serverLevel.getEntitiesOfClass(
                        LivingEntity.class, aoe, e -> e.isAlive() && e != owner && e != target
                    );
                    for (LivingEntity st : splashTargets) {
                        st.hurtServer(serverLevel, damageSource, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (this.damage * 0.6f)));
                    }
                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.BLAST, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);
                    serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), com.dragonblockarcanedba.sound.DbaSounds.BOOM_BIG, SoundSource.PLAYERS, 1.2f, 1.4f);
                }
            }

            if (this.tickCount > 30) {
                // Range death used to be a hard discard in mid flight. The bolt
                // now fizzles out through the same 5 tick collapse the renderer
                // already plays on block impact, so nothing pops out of the sky.
                this.entityData.set(IMPACT_AGE, 0);
            }
        }
    }

    /**
     * Impact burst where a bolt meets a block (points 1, 2, 3): a spark burst,
     * heat colored dust in the bolt's own palette, and a small debris field of
     * real ground blocks kicked up from the impact point. Counts stay small:
     * the barrage fires a bolt every 3 ticks, so a heavy field per bolt would
     * flood the world.
     */
    private void emitImpactBurst(ServerLevel level, Vec3 hitPoint) {
        boolean overcharged = isOvercharged();
        float heat = getHeatRatio();
        float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
        if (density > 0f) {
            level.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                hitPoint.x, hitPoint.y, hitPoint.z,
                Math.max(3, Math.round(8f * density)), 0.25, 0.25, 0.25, 0.12);
            int green = (int) (200f - heat * 110f);
            int dustArgb = overcharged ? 0xFFFF2A4D : (0xFF000000 | (0xFF << 16) | (green << 8) | 0x1F);
            level.sendParticles(com.dragonblockarcanedba.util.LayeredParticles.dust(dustArgb, 1.3f),
                hitPoint.x, hitPoint.y, hitPoint.z,
                Math.max(2, Math.round(6f * density)), 0.30, 0.30, 0.30, 0.05);
        }
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
        com.dragonblockarcanedba.util.DebrisSpawner.spawnField(level, owner, hitPoint,
            overcharged ? 10 : 5, 1.1, 1.5, 18);
    }
}
