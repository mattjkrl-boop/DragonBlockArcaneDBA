package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.util.DbaCompat;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.GroundScar;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * King's Slam Entity: physical 3D ground shatter, radiating tectonic canyon trenches,
 * colossal erupted basalt monoliths, and expanding volcanic shockwave dome.
 * Spawned on Right-Click release for the Normal Slam (radius scales with hold,
 * 6 to 14 blocks) and the Flawless King's Slam (20 blocks).
 *
 * The entity owns the slam damage now, timed to its own visual: the dome grows
 * across the first 55 percent of life (the same ease the renderer uses), and
 * each target is hit on the tick the expanding rim reaches it. The old item
 * code applied all damage instantly at the release tick, so rim targets were
 * launched by a dome that had not visually arrived yet. When the rim reaches
 * full radius the slam also scars the ground at its heart through GroundScar.
 */
public class KingsSlamEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(KingsSlamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_FLAWLESS = SynchedEntityData.defineId(KingsSlamEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(KingsSlamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(KingsSlamEntity.class, EntityDataSerializers.FLOAT);

    private final Set<Integer> hitEntityIds = new HashSet<>();
    private boolean scarred = false;

    public KingsSlamEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public KingsSlamEntity(Level level, LivingEntity caster, Vec3 pos, float radius, boolean isFlawless, float damage) {
        super(DbaEntities.KINGS_SLAM, level);
        this.setOwner(caster);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;

        int life = isFlawless ? 55 : 40;
        this.entityData.set(RADIUS, radius);
        this.entityData.set(IS_FLAWLESS, isFlawless);
        this.entityData.set(LIFETIME, life);
        this.entityData.set(DAMAGE, damage);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 10.0f);
        builder.define(IS_FLAWLESS, false);
        builder.define(LIFETIME, 40);
        builder.define(DAMAGE, 0.0f);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public boolean isFlawless() {
        return this.entityData.get(IS_FLAWLESS);
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    public float getDamage() {
        return this.entityData.get(DAMAGE);
    }

    /** Visual radius at the current age, using the renderer's exact growth curve. */
    private float visualRadius() {
        float lifeFrac = this.tickCount / (float) Math.max(1, getLifetime());
        float growT = Math.min(1.0f, lifeFrac / 0.55f);
        float grow = 1.0f - (1.0f - growT) * (1.0f - growT) * (1.0f - growT);
        return getRadius() * grow;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            // Shared slam debris (SlamDebris): one ground burst on the first tick.
            if (this.tickCount == 1) {
                com.dragonblockarcanedba.fx.SlamDebris.burst(serverLevel, this.position(), getRadius(), com.dragonblockarcanedba.fx.SlamDebris.Profile.KINGS_SLAM);
                // Base dust where the slam meets the ground (point 3).
                LayeredParticles.emitGroundBurst(serverLevel, this.position(), getRadius() * 0.5f, 0xFF6A00);
            }

            // Debris field (point 2): real ground blocks torn up by the slam,
            // orbiting helically, then raining back down. 30 pieces for the
            // normal slam, 48 for the flawless finisher.
            if (this.tickCount == 2) {
                float density = DbaEffectsConfig.effectDensity();
                int count = Math.round((isFlawless() ? 48f : 30f) * density);
                if (count > 0) {
                    Entity owner = this.getOwner();
                    DebrisSpawner.spawnField(serverLevel,
                        owner instanceof LivingEntity livingOwner ? livingOwner : null,
                        this.position(), count, getRadius() * 0.8,
                        isFlawless() ? 5.0 : 3.5, 26);
                }
            }

            // Theme particles (redesign T5): embers ride the expanding crest
            // while the dome is young, so the slam front keeps burning.
            if (this.tickCount <= 21 && this.tickCount % 3 == 0) {
                float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
                if (density > 0f) {
                    double shockR = Math.min(getRadius() * 1.35, this.tickCount * (isFlawless() ? 0.95 : 0.70));
                    int emberCount = Math.max(2, Math.round(8f * density));
                    for (int i = 0; i < emberCount; i++) {
                        double ang = serverLevel.getRandom().nextDouble() * Math.PI * 2.0;
                        serverLevel.sendParticles(
                            com.dragonblockarcanedba.particle.DbaParticles.EMBER,
                            this.getX() + Math.cos(ang) * shockR, this.getY() + 0.3, this.getZ() + Math.sin(ang) * shockR,
                            1, 0.1, 0.25, 0.1, 0.03);
                    }
                }
            }

            applyRimDamage(serverLevel);

            // Ground scarring (point 3) lands on the rim tick: the moment the
            // dome first reaches full radius (tick 22 normal, tick 31 flawless),
            // the ground at the slam's heart is torn up temporarily.
            if (!this.scarred) {
                float lifeFrac = this.tickCount / (float) Math.max(1, getLifetime());
                if (lifeFrac / 0.55f >= 1.0f) {
                    this.scarred = true;
                    scarGround(serverLevel);
                }
            }

            if (this.tickCount >= getLifetime()) {
                this.discard();
            }
        }
    }

    /**
     * Damage lands inside the visual's active window: a target is hit on the
     * first tick the growing dome rim reaches it, never at the release tick
     * and never after the visual ends. Effect profile matches what the item
     * used to apply instantly (stun, tracking, curse, launch).
     */
    private void applyRimDamage(ServerLevel serverLevel) {
        float damage = getDamage();
        if (damage <= 0f || this.tickCount > getLifetime()) return;
        boolean flawless = isFlawless();
        float visualR = visualRadius();
        Vec3 center = this.position();
        Entity owner = this.getOwner();

        AABB box = new AABB(
            center.x - getRadius() - 1.0, center.y - 2.0, center.z - getRadius() - 1.0,
            center.x + getRadius() + 1.0, center.y + 3.0, center.z + getRadius() + 1.0
        );
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
            LivingEntity.class, box,
            e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
        );

        DamageSource damageSource = owner instanceof net.minecraft.world.entity.player.Player playerOwner
            ? serverLevel.damageSources().playerAttack(playerOwner)
            : (owner instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobAttack(livingOwner) : serverLevel.damageSources().generic());
        LivingEntity effectSource = owner instanceof LivingEntity livingOwner ? livingOwner : null;

        for (LivingEntity target : targets) {
            double dist = Math.sqrt(target.distanceToSqr(center.x, target.getY(), center.z));
            if (dist > visualR + 0.5) continue;
            hitEntityIds.add(target.getId());

            target.hurtServer(serverLevel, damageSource, DbaEffectsConfig.capDamage(damage));

            if (flawless) {
                target.addEffect(new MobEffectInstance(DbaEffects.FISSURE_STUN_HOLDER, 140, 1, false, true));
                if (effectSource != null) {
                    target.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 60, 0, false, false, false), effectSource);
                    target.addEffect(new MobEffectInstance(DbaEffects.MOVEMENT_CURSE_HOLDER, 60, 2, false, true), effectSource);
                }
            } else {
                target.addEffect(new MobEffectInstance(DbaEffects.FISSURE_STUN_HOLDER, 80, 0, false, true));
                if (effectSource != null) {
                    target.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 40, 0, false, false, false), effectSource);
                    target.addEffect(new MobEffectInstance(DbaEffects.MOVEMENT_CURSE_HOLDER, 40, 1, false, true), effectSource);
                }
            }

            Vec3 toTarget = target.position().subtract(center);
            Vec3 flatDir = new Vec3(toTarget.x, 0, toTarget.z);
            if (flatDir.lengthSqr() < 0.001) {
                flatDir = new Vec3(1, 0, 0);
            } else {
                flatDir = flatDir.normalize();
            }
            double horizontalPower = flawless ? 2.5 : 1.5;
            double verticalPower = flawless ? 0.9 : 0.5;
            Vec3 currentVel = target.getDeltaMovement();
            target.setDeltaMovement(
                currentVel.x + flatDir.x * horizontalPower,
                currentVel.y + verticalPower,
                currentVel.z + flatDir.z * horizontalPower
            );
            DbaCompat.markHurt(target);
        }
    }

    /** Temporary magma and basalt scarring under the slam's heart, restored by GroundScar. */
    private void scarGround(ServerLevel serverLevel) {
        int scarRadius = isFlawless() ? 7 : 5;
        int baseY = (int) Math.floor(this.getY());
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        for (int dx = -scarRadius; dx <= scarRadius; dx++) {
            for (int dz = -scarRadius; dz <= scarRadius; dz++) {
                if (dx * dx + dz * dz > scarRadius * scarRadius) continue;
                if (serverLevel.getRandom().nextFloat() > 0.7f) continue;
                // Find the surface cell of this column, scanning down from feet level.
                for (int y = baseY; y >= baseY - 3; y--) {
                    mPos.set((int) Math.floor(this.getX()) + dx, y, (int) Math.floor(this.getZ()) + dz);
                    BlockState surface = serverLevel.getBlockState(mPos);
                    if (!surface.isAir() && surface.isFaceSturdy(serverLevel, mPos, net.minecraft.core.Direction.UP)) {
                        BlockState scarState = serverLevel.getRandom().nextFloat() < 0.45f
                            ? Blocks.MAGMA_BLOCK.defaultBlockState()
                            : Blocks.BASALT.defaultBlockState();
                        GroundScar.scar(serverLevel, mPos, scarState, 300);
                        break;
                    }
                }
            }
        }
    }
}
