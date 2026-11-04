package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.DbaDamage;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.dragonblockarcanedba.util.LayeredParticles;
import com.dragonblockarcanedba.util.VortexPhysics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Evil Spear Projectile Entity: Fired by Evil Spear (Evil Impale).
 * High-velocity spectral crimson spear that pierces entities, applies Marked by Evil, and pulls targets inward.
 *
 * Spectacle pass: the flight trail is three layered particle strata laid
 * along the tick's movement segment (bright core, crimson mid, ember wisps),
 * and the first hit opens a real vortex at the impact point (VortexPhysics,
 * radius 6.0, 10 ticks) that drags everything nearby toward where the spear
 * struck, the way the spear's theme promises. The old instant 0.8 tug on the
 * pierced target plus a contradictory outward push on its neighbours is gone.
 * If the spear pins into a wall while the vortex is still open, it stays
 * lodged until the vortex closes instead of discarding mid-pull.
 */
public class EvilSpearProjectileEntity extends Projectile {
    private float damage = 450.0f;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    // Impact vortex state (spectacle standard point 3). Honest numbers: the
    // pull covers a 6.0 block radius around the first hit for 10 ticks.
    private Vec3 vortexCenter = null;
    private int vortexTicksLeft = 0;
    private boolean pinned = false;

    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(EvilSpearProjectileEntity.class, EntityDataSerializers.INT);

    public EvilSpearProjectileEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public EvilSpearProjectileEntity(Level level, LivingEntity owner, float damage) {
        super(DbaEntities.EVIL_SPEAR_PROJECTILE, level);
        this.setOwner(owner);
        if (owner != null) {
            Vec3 eye = owner.getEyePosition();
            Vec3 look = owner.getLookAngle();
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);
            Vec3 up = right.cross(look).normalize();

            boolean isRightHanded = (owner.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (owner.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.EvilSpearItem && 
                !(owner.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.EvilSpearItem));
            boolean onRight = isRightHanded ? !isOffhand : isOffhand;
            float sideSign = onRight ? 1.0f : -1.0f;

            Vec3 spawnPos = eye.add(look.scale(0.8)).add(right.scale(sideSign * 0.35)).add(up.scale(-0.25));
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
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 movement = this.getDeltaMovement();
        Vec3 start = this.position();

        // Block collision: the spear is noPhysics and stepped manually with no clip check,
        // so it flew through walls for its full life and marked targets through terrain.
        if (!this.level().isClientSide() && !this.pinned) {
            Vec3 end = start.add(movement);
            net.minecraft.world.phys.HitResult blockHit = this.level().clip(new net.minecraft.world.level.ClipContext(
                start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this));
            if (blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                Vec3 hitPos = blockHit.getLocation();
                this.setPos(hitPos.x, hitPos.y, hitPos.z);
                this.level().playSound(null, hitPos.x, hitPos.y, hitPos.z,
                    com.dragonblockarcanedba.sound.DbaSounds.TRIDENT_HIT, net.minecraft.sounds.SoundSource.HOSTILE, 1.4f, 0.7f);
                if (this.level() instanceof ServerLevel serverLevel) {
                    LayeredParticles.emitGroundBurst(serverLevel, hitPos, 1.2f, HellHuntImpactEntity.THEME_ARGB);
                }
                if (this.vortexTicksLeft > 0) {
                    // The vortex is still open: lodge in the wall and keep
                    // dragging prey until it closes, then discard.
                    this.pinned = true;
                    this.setDeltaMovement(Vec3.ZERO);
                } else {
                    this.discard();
                }
                return;
            }
        }

        if (!this.pinned) {
            this.setPos(start.x + movement.x, start.y + movement.y, start.z + movement.z);
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            emitTrail(serverLevel, start, this.position());

            Entity owner = this.getOwner();
            // Bespoke pass (standard rule 4): the pierce is ability damage,
            // so it rides the mod's hellfire type with this spear as the
            // direct entity, whoever threw it. Numbers are unchanged.
            DamageSource damageSource = owner instanceof LivingEntity livingOwner
                ? DbaDamage.hellfire(this, livingOwner)
                : DbaDamage.hellfire(this, null);

            if (!this.pinned) {
                AABB hitbox = this.getBoundingBox().inflate(1.2, 1.2, 1.2);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, hitbox,
                    e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
                );

                for (LivingEntity target : targets) {
                    hitEntityIds.add(target.getId());

                    // Calculate damage with Marked by Evil bonus if already marked (+30%)
                    float finalDamage = this.damage;
                    if (target.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.MARKED_BY_EVIL_HOLDER)) {
                        finalDamage *= 1.3f;
                    }

                    // Deal damage
                    target.hurtServer(serverLevel, damageSource, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (finalDamage)));

                    // Apply Marked by Evil for 8 seconds (160 ticks)
                    target.addEffect(new MobEffectInstance(
                        com.dragonblockarcanedba.effect.DbaEffects.MARKED_BY_EVIL_HOLDER, 160, 0, false, true
                    ), owner);

                    // The dread rides the mark at the same duration:
                    // darkness and the watching eyes while the mark lasts.
                    target.addEffect(new MobEffectInstance(
                        com.dragonblockarcanedba.effect.DbaEffects.EVIL_DREAD_HOLDER, 160, 0, false, true
                    ), owner);

                    // MC 26.2 Physics: Spear impalement pin friction & drag
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        target,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("evil_spear_hit_friction"),
                        3.0,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    );
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        target,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("evil_spear_hit_drag"),
                        2.0,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    );

                    // Apply cinematic tracking holder
                    if (owner instanceof LivingEntity livingOwner) {
                        target.addEffect(new MobEffectInstance(
                            com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 25, 0, false, false, false
                        ), livingOwner);
                    }

                    // The first pierce opens the impact vortex at the struck
                    // target: a real, sustained drag toward the impact point
                    // instead of the old one-tick tug and outward push.
                    if (this.vortexCenter == null) {
                        this.vortexCenter = target.position().add(0, 1.0, 0);
                        this.vortexTicksLeft = 10;
                    }

                    // Spawn physical 3D impact shatter & spikes
                    HellHuntImpactEntity impact = new HellHuntImpactEntity(
                        serverLevel, owner instanceof LivingEntity l ? l : null, target.position().add(0, 0.05, 0), 2.6f, false
                    );
                    serverLevel.addFreshEntity(impact);

                    serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), com.dragonblockarcanedba.sound.DbaSounds.TRIDENT_HIT, SoundSource.PLAYERS, 1.8f, 0.6f);
                }
            }

            // Run the impact vortex while it is open.
            if (this.vortexTicksLeft > 0 && this.vortexCenter != null) {
                float env = FxEnvelope.envelope(11 - this.vortexTicksLeft, 11, 0.2f, 0.3f);
                VortexPhysics.applyVortex(serverLevel, this.vortexCenter, 6.0, 4.0,
                    owner instanceof LivingEntity living ? living : null,
                    0.5, 0.9, 0.30, env);
                this.vortexTicksLeft--;
            }

            if (this.vortexTicksLeft <= 0 && (this.pinned || this.tickCount > 40)) {
                this.discard();
            }
        }
    }

    /**
     * Layered flight trail (standard point 1): three particle strata laid at
     * substeps along this tick's movement segment so a 3.6 block per tick
     * spear leaves a continuous layered wake instead of dotted embers: a
     * bright KI mote core on the axis, crimson dust in the mid band, and
     * sparse ember wisps at the rim. The tail fades over the last 8 ticks to
     * match the renderer's body fade.
     */
    private void emitTrail(ServerLevel level, Vec3 from, Vec3 to) {
        float density = DbaEffectsConfig.effectDensity();
        if (density <= 0f) return;
        float fade = this.tickCount > 32
            ? 1.0f - FxEnvelope.smoothstep((this.tickCount - 32) / 8f)
            : 1.0f;
        if (fade <= 0f) return;
        int subSteps = Math.max(2, (int) Math.ceil(from.distanceTo(to)));
        var rand = level.getRandom();
        for (int i = 0; i <= subSteps; i++) {
            double t = i / (double) subSteps;
            double px = from.x + (to.x - from.x) * t;
            double py = from.y + (to.y - from.y) * t;
            double pz = from.z + (to.z - from.z) * t;
            if (rand.nextFloat() < density * fade) {
                level.sendParticles(DbaParticles.KI_MOTE, px, py, pz, 1, 0.03, 0.03, 0.03, 0.01);
            }
            if (rand.nextFloat() < density * fade) {
                level.sendParticles(LayeredParticles.dust(HellHuntImpactEntity.THEME_ARGB, 1.3f),
                    px, py, pz, 1, 0.16, 0.16, 0.16, 0.02);
            }
            if (i % 2 == 0 && rand.nextFloat() < density * fade) {
                level.sendParticles(DbaParticles.EMBER, px, py, pz, 1, 0.30, 0.30, 0.30, 0.02);
            }
        }
    }
}
