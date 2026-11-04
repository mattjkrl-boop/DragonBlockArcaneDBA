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
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(ErasureCannonBeamEntity.class, EntityDataSerializers.INT);

    public static final int MAX_LIFETIME = 24;
    public static final float DEFAULT_MAX_RANGE = 48.0f;

    // Damage pulses (spectacle pass): the beam used to pay its entire damage
    // on tick 1 of a 16 tick visual. The total is now split into PULSE_COUNT
    // equal pulses across the beam's active window, so standing in the beam
    // hurts for as long as the beam is visibly burning.
    public static final int FIRST_PULSE_TICK = 3;
    public static final int PULSE_INTERVAL = 3;
    public static final int PULSE_COUNT = 6;
    public static final int LAST_PULSE_TICK = FIRST_PULSE_TICK + PULSE_INTERVAL * (PULSE_COUNT - 1);

    /** Per beam cap on trench blocks carved at the ground contact. */
    private static final int TRENCH_CAP = 120;

    private float totalDamage = 900.0f;
    private int trenchUsed = 0;
    // Targets that already received the one-time attribute modifiers. Damage
    // itself is NOT once-only anymore: every pulse re-tests the beam line.
    private final Set<Integer> effectAppliedIds = new HashSet<>();

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
        this.entityData.set(SEED, level.getRandom().nextInt());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CASTER_ID, -1);
        builder.define(BEAM_LENGTH, DEFAULT_MAX_RANGE);
        builder.define(BEAM_Y_ROT, 0.0f);
        builder.define(BEAM_X_ROT, 0.0f);
        builder.define(CHARGE_RATIO, 1.0f);
        builder.define(SEED, 0);
    }

    /** Synced seed for the bespoke renderer: same seed, same silhouette, every client. */
    public int getSeed() {
        return this.entityData.get(SEED);
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

        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Entity caster = this.getOwner();
        if (caster == null && getCasterId() != -1) {
            caster = serverLevel.getEntity(getCasterId());
        }
        final Entity finalCaster = caster;

        final int age = this.tickCount;
        final float length = getBeamLength();
        final float charge = getChargeRatio();
        final Vec3 start = this.position();
        final Vec3 look = this.calculateViewVector(getBeamXRot(), getBeamYRot());
        final Vec3 end = start.add(look.scale(length));

        if (age == 1) {
            // Fire beat: the beam's own fire boom and zap at the muzzle, a
            // dust burst where the beam lands, and a real branched lightning
            // strike at the terminus so the zap finally has a visual
            // (point 4). Bespoke pass: weapon sounds come from the mod's
            // bank only; storm thunder belongs to the atmosphere system.
            serverLevel.playSound(null, start.x, start.y, start.z,
                com.dragonblockarcanedba.sound.DbaSounds.BEAM_FIRE, SoundSource.PLAYERS, 2.2f, 0.6f);
            serverLevel.playSound(null, start.x, start.y, start.z,
                com.dragonblockarcanedba.sound.DbaSounds.LIGHTNING_ZAP, SoundSource.PLAYERS, 2.2f, 0.6f);
            com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel, end, 1.6f, 0xFF37E6FF);
            com.dragonblockarcanedba.entity.BoltEntity strike = new com.dragonblockarcanedba.entity.BoltEntity(
                serverLevel, end, serverLevel.getRandom().nextInt(), 0xFF7DF4FF, 12.0f, 8);
            serverLevel.addFreshEntity(strike);
        }

        // Damage lands inside the visual's active window, spread across it:
        // one pulse every PULSE_INTERVAL ticks from FIRST_PULSE_TICK to
        // LAST_PULSE_TICK, each worth totalDamage / PULSE_COUNT.
        if (age >= FIRST_PULSE_TICK && age <= LAST_PULSE_TICK && (age - FIRST_PULSE_TICK) % PULSE_INTERVAL == 0) {
            applyDamagePulse(serverLevel, finalCaster, start, look, length, charge);
            carveTrench(serverLevel, start, look, length);
        }

        // Max charge endpoint detonation, moved from tick 1 to the final
        // pulse: the terminus builds all beam long and then blows, and a
        // target on the beam line no longer eats beam plus explosion on the
        // same tick.
        if (age == LAST_PULSE_TICK && charge >= 0.9f) {
            detonateEndpoint(serverLevel, finalCaster, end);
        }

        emitBeamParticles(serverLevel, start, look, end, length, charge, age);

        if (age >= MAX_LIFETIME) {
            this.discard();
        }
    }

    private void applyDamagePulse(ServerLevel serverLevel, Entity finalCaster, Vec3 start, Vec3 look, float length, float charge) {
        // Bespoke pass: ability damage uses the mod's own ki damage type
        // (rule 4), with this beam as the direct entity.
        DamageSource damageSource = com.dragonblockarcanedba.util.DbaDamage.ki(this,
            finalCaster instanceof LivingEntity livingOwner ? livingOwner : null);

        float pulseDamage = this.totalDamage / PULSE_COUNT;
        AABB beamBox = new AABB(start, start.add(look.scale(length))).inflate(1.5 + charge * 1.5);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
            LivingEntity.class, beamBox,
            e -> e.isAlive() && e != finalCaster
        );

        double beamRadius = 1.5 + charge * 1.5;
        for (LivingEntity t : targets) {
            // The candidate box is a fat diagonal AABB; without a real distance
            // test, targets several blocks off the beam line (or behind cover
            // near the box) took beam damage. Require the target center to
            // actually be within the beam radius of the beam segment.
            Vec3 targetCenter = t.position().add(0, t.getBbHeight() * 0.5, 0);
            double along = Math.max(0.0, Math.min(length, look.dot(targetCenter.subtract(start))));
            Vec3 closest = start.add(look.scale(along));
            if (targetCenter.distanceTo(closest) > beamRadius + t.getBbWidth() * 0.5) {
                continue;
            }

            if (finalCaster instanceof LivingEntity livingOwner) {
                t.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 25, 0, false, false, false
                ), livingOwner);
            }

            t.hurtServer(serverLevel, damageSource, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(pulseDamage));

            // One-time concussive modifiers per target, on its first pulse.
            if (effectAppliedIds.add(t.getId())) {
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
            }

            // Gravitational suction pulling enemies into the centerline, now
            // per pulse at reduced strength so the total pull across the burn
            // matches the old single tick without the one-tick yank.
            Vec3 toCenter = start.add(look.scale(look.dot(t.position().subtract(start)))).subtract(t.position()).normalize().scale(0.35);
            t.setDeltaMovement(t.getDeltaMovement().add(toCenter.x, 0.08, toCenter.z));
            com.dragonblockarcanedba.util.DbaCompat.markHurt(t);
        }
    }

    private void detonateEndpoint(ServerLevel serverLevel, Entity finalCaster, Vec3 end) {
        DamageSource damageSource = com.dragonblockarcanedba.util.DbaDamage.ki(this,
            finalCaster instanceof LivingEntity livingOwner ? livingOwner : null);

        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.BLAST, end.x, end.y, end.z, 3, 0.5, 0.5, 0.5, 0);
        serverLevel.playSound(null, end.x, end.y, end.z,
            com.dragonblockarcanedba.sound.DbaSounds.BOOM_BIG, SoundSource.PLAYERS, 1.6f, 0.9f);

        AABB endAoe = new AABB(end.subtract(4, 4, 4), end.add(4, 4, 4));
        List<LivingEntity> endTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, endAoe, e -> e.isAlive() && e != finalCaster);
        for (LivingEntity et : endTargets) {
            et.hurtServer(serverLevel, damageSource, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (this.totalDamage * 0.5f)));
        }

        // The detonation tears up the ground it lands on: a finisher-class
        // debris field sampled from the blocks around the terminus, plus a
        // second dust burst at full radius.
        LivingEntity owner = finalCaster instanceof LivingEntity living ? living : null;
        com.dragonblockarcanedba.util.DebrisSpawner.spawnField(serverLevel, owner, end, 24, 2.2, 3.0, 26);
        com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel, end, 2.2f, 0xFF37E6FF);

        // And a real crater where the charge detonates (radius 2.5,
        // STANDARD, cap 120), when the terminus actually rests on ground.
        net.minecraft.core.BlockPos endAir = com.dragonblockarcanedba.util.PermanentDestruction.findSurface(
            serverLevel, (int) Math.floor(end.x), (int) Math.floor(end.z), (int) Math.floor(end.y));
        net.minecraft.core.BlockPos craterCenter = endAir != null ? endAir.below()
            : !serverLevel.getBlockState(net.minecraft.core.BlockPos.containing(end.x, end.y, end.z)).isAir()
                ? net.minecraft.core.BlockPos.containing(end.x, end.y, end.z) : null;
        if (craterCenter != null) {
            com.dragonblockarcanedba.util.PermanentDestruction.crater(serverLevel, craterCenter, 2.5, 0.5, false,
                com.dragonblockarcanedba.util.PermanentDestruction.TIER_STANDARD, 120, serverLevel.getRandom());
        }
    }

    /**
     * While the beam holds, its ground contact carves a trench: each pulse
     * clips the beam line to its first block hit, and from the surface
     * there a fissure eats forward along the beam's heading (width 1, one
     * deep, STANDARD tier), up to a per beam cap. A beam that hits no
     * block, or whose contact is not ground, carves nothing.
     */
    private void carveTrench(ServerLevel serverLevel, Vec3 start, Vec3 look, float length) {
        if (this.trenchUsed >= TRENCH_CAP) return;
        Vec3 end = start.add(look.scale(length));
        net.minecraft.world.phys.HitResult hit = serverLevel.clip(new net.minecraft.world.level.ClipContext(
            start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK
            || !(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)) {
            return;
        }
        net.minecraft.core.BlockPos struck = blockHit.getBlockPos();
        net.minecraft.core.BlockPos air = com.dragonblockarcanedba.util.PermanentDestruction.findSurface(
            serverLevel, struck.getX(), struck.getZ(), struck.getY() + 1);
        if (air == null) return;
        this.trenchUsed += com.dragonblockarcanedba.util.PermanentDestruction.fissure(
            serverLevel, air, look, 3.0, 1, 1, false,
            com.dragonblockarcanedba.util.PermanentDestruction.TIER_STANDARD,
            TRENCH_CAP - this.trenchUsed);
    }

    /**
     * Layered particles for the beam's whole life (points 1 and 5): a dense
     * KI_MOTE core hugging the centerline, a SPARK mid layer that travels
     * muzzle to terminus like the renderer's flow bands, a sparse cyan dust
     * outer layer at the beam radius, and embers boiling off the terminus.
     * Counts ride the shared envelope so particles build and dissipate with
     * the mesh instead of stopping at tick 3 of 16 as before.
     */
    private void emitBeamParticles(ServerLevel serverLevel, Vec3 start, Vec3 look, Vec3 end, float length, float charge, int age) {
        float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
        if (density <= 0f) return;
        float env = com.dragonblockarcanedba.util.FxEnvelope.envelope(age, MAX_LIFETIME);
        if (env <= 0f) return;

        net.minecraft.util.RandomSource rand = serverLevel.getRandom();
        Vec3 perpA = look.cross(new Vec3(0, 1, 0));
        if (perpA.lengthSqr() < 0.001) perpA = new Vec3(1, 0, 0);
        perpA = perpA.normalize();
        Vec3 perpB = perpA.cross(look).normalize();
        double midRadius = 0.35 + charge * 0.45;

        // Core: bright motes on the centerline.
        int coreCount = Math.max(1, Math.round(4f * density * env));
        for (int i = 0; i < coreCount; i++) {
            Vec3 p = start.add(look.scale(rand.nextDouble() * length));
            serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
                p.x, p.y, p.z, 1, 0.12, 0.12, 0.12, 0.01);
        }

        // Mid: sparks on a disc around the line, their distance along the beam
        // advancing with age so the layer visibly flows toward the terminus.
        int midCount = Math.max(1, Math.round(5f * density * env));
        for (int i = 0; i < midCount; i++) {
            double along = (age * 2.2 + rand.nextDouble() * 6.0) % Math.max(1.0, length);
            double ang = rand.nextDouble() * Math.PI * 2.0;
            double rr = midRadius * Math.sqrt(rand.nextDouble());
            Vec3 p = start.add(look.scale(along))
                .add(perpA.scale(Math.cos(ang) * rr))
                .add(perpB.scale(Math.sin(ang) * rr));
            serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.03);
        }

        // Outer: wispy cyan dust at and beyond the beam radius, every 2 ticks.
        if (age % 2 == 0) {
            int outerCount = Math.max(1, Math.round(2f * density * env));
            for (int i = 0; i < outerCount; i++) {
                double along = rand.nextDouble() * length;
                double ang = rand.nextDouble() * Math.PI * 2.0;
                double rr = midRadius * (1.15 + rand.nextDouble() * 0.5);
                Vec3 p = start.add(look.scale(along))
                    .add(perpA.scale(Math.cos(ang) * rr))
                    .add(perpB.scale(Math.sin(ang) * rr));
                serverLevel.sendParticles(com.dragonblockarcanedba.util.LayeredParticles.dust(0xFF37E6FF, 1.5f),
                    p.x, p.y, p.z, 1, 0.06, 0.06, 0.06, 0.01);
            }
        }

        // Terminus: embers and sparks boiling off the impact point.
        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.EMBER,
            end.x, end.y, end.z,
            Math.max(1, Math.round(3f * density * env)),
            0.5, 0.5, 0.5, 0.07);
        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
            end.x, end.y, end.z,
            Math.max(1, Math.round(2f * density * env)),
            0.4, 0.4, 0.4, 0.10);

        // Dissipation: as the envelope falls, spent sparks rain down around
        // the terminus instead of the beam simply ending.
        if (age >= MAX_LIFETIME * 0.7f) {
            com.dragonblockarcanedba.util.LayeredParticles.emitRain(serverLevel, end, 2.0f, 3.0f,
                age, MAX_LIFETIME, com.dragonblockarcanedba.particle.DbaParticles.SPARK);
        }
    }
}
