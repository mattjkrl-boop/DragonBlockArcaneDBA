package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.ability.AbilityDamage;
import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.ability.AbilityFeedbackService;
import com.dragonblockarcanedba.ability.AbilityFxService;
import com.dragonblockarcanedba.ability.AbilityTerrain;
import com.dragonblockarcanedba.ability.RewindService;
import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Cloud carrier: a stationary domain that grows from
 * radiusStart to radiusEnd across its life and, on a fixed interval,
 * calls a telegraphed lightning strike inside itself. The pending
 * strike is the one piece of gameplay information every observer
 * must see identically, so it is synced (position, the entity age at
 * which it fires, a shape seed): five extra fields, one strike at a
 * time, scheduled strictly sequentially for exactly that reason.
 * The renderer and the strike picker read the same radius formula,
 * so what you see is what can hit you.
 */
public class CloudCarrierEntity extends CarrierEntity {
    private static final EntityDataAccessor<Float> STRIKE_X =
            SynchedEntityData.defineId(CloudCarrierEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STRIKE_Y =
            SynchedEntityData.defineId(CloudCarrierEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STRIKE_Z =
            SynchedEntityData.defineId(CloudCarrierEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> STRIKE_AGE =
            SynchedEntityData.defineId(CloudCarrierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STRIKE_SEED =
            SynchedEntityData.defineId(CloudCarrierEntity.class, EntityDataSerializers.INT);

    private int nextStrikeAge = -1;
    private int pendingStrikeAge = -1;
    private Vec3 pendingPoint;

    // Phase 6 module state (server only, never synced).
    private UUID targetUuid;
    private final Set<UUID> latched = new HashSet<>();
    private final Map<Integer, Vec3> frozenProjectiles = new HashMap<>();
    private boolean rewindDone;
    /** Mobs whose AI this cloud paused during its freeze window (restored after or on removal). */
    private final Set<UUID> aiPaused = new HashSet<>();
    private boolean finaleDone;
    private boolean spawnBurstDone;
    private int fieldConverted;

    /** The entity this cloud is anchored to (anchor "entity"), set by AbilityService. */
    public void setTargetUuid(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public UUID getTargetUuid() {
        return this.targetUuid;
    }

    /** The currently latched chain targets (Phase 6 chains module), server side. */
    public Set<UUID> latched() {
        return this.latched;
    }

    public CloudCarrierEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STRIKE_X, 0f);
        builder.define(STRIKE_Y, 0f);
        builder.define(STRIKE_Z, 0f);
        builder.define(STRIKE_AGE, -1);
        builder.define(STRIKE_SEED, 0);
    }

    /** Current domain radius at the given fractional age, shared by server logic and renderers. */
    public static double radiusAt(AbilityDef def, float ageTicks) {
        AbilityDef.CloudParams cloud = def.cloud();
        float frac = def.phases().active() <= 0 ? 1f
                : Math.max(0f, Math.min(1f, ageTicks / def.phases().active()));
        return cloud.radiusStart() + (cloud.radiusEnd() - cloud.radiusStart()) * frac;
    }

    public boolean hasPendingStrike() {
        return this.entityData.get(STRIKE_AGE) >= 0;
    }

    public Vec3 strikePos() {
        return new Vec3(this.entityData.get(STRIKE_X), this.entityData.get(STRIKE_Y), this.entityData.get(STRIKE_Z));
    }

    public int strikeAge() {
        return this.entityData.get(STRIKE_AGE);
    }

    public int strikeSeed() {
        return this.entityData.get(STRIKE_SEED);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AbilityDef def = def();
        if (def == null || def.cloud() == null) {
            return;
        }
        AbilityDef.CloudParams cloud = def.cloud();
        if (cloud.strikeInterval() > 0) {
            tickStrikes(serverLevel, def, cloud);
        }
        if (!this.isRemoved()) {
            tickModules(serverLevel, def, cloud);
        }
    }

    private void tickStrikes(ServerLevel serverLevel, AbilityDef def, AbilityDef.CloudParams cloud) {
        if (this.nextStrikeAge < 0) {
            this.nextStrikeAge = cloud.strikeInterval();
        }
        int age = this.tickCount;

        if (this.pendingStrikeAge < 0 && age >= this.nextStrikeAge - cloud.telegraphTicks()) {
            this.pendingPoint = chooseStrikePoint(serverLevel, def);
            this.pendingStrikeAge = this.nextStrikeAge;
            this.entityData.set(STRIKE_X, (float) this.pendingPoint.x);
            this.entityData.set(STRIKE_Y, (float) this.pendingPoint.y);
            this.entityData.set(STRIKE_Z, (float) this.pendingPoint.z);
            this.entityData.set(STRIKE_AGE, this.pendingStrikeAge);
            this.entityData.set(STRIKE_SEED, serverLevel.getRandom().nextInt());
        }

        if (this.pendingStrikeAge >= 0 && age >= this.pendingStrikeAge) {
            Player owner = ownerPlayer();
            Vec3 center = this.pendingPoint.add(0, 1.0, 0);
            AbilityDamage.applyAreaHit(serverLevel, def, this, owner, center,
                    cloud.strikeRadius(), getPower(), owner);
            playHitSound(serverLevel, def, this.pendingPoint);
            // The strike burst rides the seeded protocol with the
            // strike's own synced seed, so the particle burst and
            // the renderer's bolt agree on the same strike.
            AbilityFxService.send(this, def, AbilityDef.FxEvent.CARRIER_HIT,
                    this.pendingPoint.add(0, 0.5, 0), Vec3.ZERO,
                    strikeSeed(), getPower(), 1);
            AbilityFeedbackService.carrier(this, def, AbilityDef.FxEvent.CARRIER_HIT,
                    this.pendingPoint);
            this.entityData.set(STRIKE_AGE, -1);
            this.pendingStrikeAge = -1;
            this.pendingPoint = null;
            this.nextStrikeAge = age + cloud.strikeInterval();
        }
    }

    private Vec3 chooseStrikePoint(ServerLevel level, AbilityDef def) {
        double radius = radiusAt(def, this.tickCount);
        Player owner = ownerPlayer();
        Vec3 center = this.position();
        AABB box = new AABB(center.x - radius, center.y - 2, center.z - radius,
                center.x + radius, center.y + def.cloud().height() + 4, center.z + radius);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner);
        if (!candidates.isEmpty()) {
            LivingEntity victim = candidates.get(level.getRandom().nextInt(candidates.size()));
            return new Vec3(victim.getX(), victim.getY(), victim.getZ());
        }
        double ang = level.getRandom().nextDouble() * Math.PI * 2.0;
        double rr = radius * Math.sqrt(level.getRandom().nextDouble());
        double px = center.x + Math.cos(ang) * rr;
        double pz = center.z + Math.sin(ang) * rr;
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(px), (int) Math.floor(pz));
        return new Vec3(px, gy, pz);
    }

    private void playHitSound(ServerLevel level, AbilityDef def, Vec3 at) {
        String soundId = def.sounds() != null ? def.sounds().hit() : null;
        if (soundId == null || soundId.isEmpty()) {
            return;
        }
        net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.tryParse(soundId);
        if (id == null) {
            return;
        }
        SoundEvent event = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(id);
        if (event != null) {
            level.playSound(null, at.x, at.y, at.z, event, SoundSource.WEATHER, 1.2f, 1.0f);
        }
    }

    // ------------------------------------------------------------------
    // Phase 6 cloud modules. Everything below runs server side only,
    // after the strike logic above, and only while the cloud is alive.
    // ------------------------------------------------------------------

    /** Living non-owner entities inside the domain's current radius. */
    private List<LivingEntity> insiders(ServerLevel level, AbilityDef def, double radius, Player owner) {
        Vec3 center = this.position();
        AABB box = new AABB(center.x - radius, center.y - 2, center.z - radius,
                center.x + radius, center.y + def.cloud().height() + 4, center.z + radius);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner)) {
            double dx = e.getX() - center.x;
            double dz = e.getZ() - center.z;
            if (Math.sqrt(dx * dx + dz * dz) <= radius + e.getBbWidth() * 0.5) {
                out.add(e);
            }
        }
        return out;
    }

    private void tickModules(ServerLevel serverLevel, AbilityDef def, AbilityDef.CloudParams cloud) {
        Player owner = ownerPlayer();
        int age = this.tickCount;

        // Follow: the domain rides its anchor.
        if ("caster".equals(cloud.follow()) && owner != null) {
            int gy = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING,
                    (int) Math.floor(owner.getX()), (int) Math.floor(owner.getZ()));
            this.setPos(owner.getX(), gy, owner.getZ());
        } else if ("entity".equals(cloud.follow()) && this.targetUuid != null) {
            Entity target = serverLevel.getEntity(this.targetUuid);
            if (target != null && target.isAlive()) {
                this.setPos(target.getX(), target.getY(), target.getZ());
            }
        }

        double radius = radiusAt(def, age);
        Vec3 center = this.position();

        // First tick: spawn burst and the terrain spawn event.
        if (age == 1 && !this.spawnBurstDone) {
            this.spawnBurstDone = true;
            if (cloud.onSpawnBurst() != null) {
                AbilityDamage.applyAreaScaled(serverLevel, def, this, owner, center,
                        cloud.radiusStart(), getPower(), owner, cloud.onSpawnBurst().damageScale());
                if (cloud.onSpawnBurst().knockback() > 0) {
                    for (LivingEntity e : insiders(serverLevel, def, cloud.radiusStart(), owner)) {
                        Vec3 away = e.position().subtract(center);
                        away = new Vec3(away.x, 0, away.z);
                        if (away.lengthSqr() > 1.0e-4) {
                            e.setDeltaMovement(e.getDeltaMovement().add(
                                    away.normalize().scale(cloud.onSpawnBurst().knockback())));
                        }
                    }
                }
                AbilityFxService.send(this, def, AbilityDef.FxEvent.CARRIER_HIT,
                        center, Vec3.ZERO, getSeed() ^ age, getPower(), 1);
            }
            if (def.terrain() != null && "spawn".equals(def.terrain().event())) {
                AbilityTerrain.eventAt(serverLevel, this.blockPosition(), def.terrain());
            }
        }

        // Chains: refresh the latched set every 10 ticks.
        if (cloud.chains() != null && age % 10 == 1) {
            this.latched.clear();
            List<LivingEntity> near = insiders(serverLevel, def, radius, owner);
            near.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(center)));
            for (int i = 0; i < Math.min(cloud.chains().maxLinks(), near.size()); i++) {
                this.latched.add(near.get(i).getUUID());
            }
        }
        boolean chainsActive = cloud.chains() != null;

        List<LivingEntity> inside = insiders(serverLevel, def, radius, owner);

        // Pull (signed) and vortex.
        if (cloud.pull() != 0 || cloud.vortex() != null) {
            for (LivingEntity e : inside) {
                if (chainsActive && !this.latched.contains(e.getUUID())) {
                    continue;
                }
                Vec3 toCenter = center.subtract(e.position());
                Vec3 horiz = new Vec3(toCenter.x, 0, toCenter.z);
                double dist = horiz.length();
                Vec3 vel = e.getDeltaMovement();
                if (dist > 1.0e-3) {
                    Vec3 dirIn = horiz.scale(1.0 / dist);
                    if (cloud.pull() != 0) {
                        vel = vel.add(dirIn.scale(cloud.pull() * 0.045));
                    }
                    if (cloud.vortex() != null && cloud.vortex().swirl() != 0) {
                        Vec3 perp = new Vec3(-dirIn.z, 0, dirIn.x);
                        vel = vel.add(perp.scale(cloud.vortex().swirl() * 0.05));
                    }
                }
                if (cloud.vortex() != null && cloud.vortex().lift() > 0) {
                    double targetY = cloud.vortex().lift() * 0.55;
                    if (vel.y < targetY) {
                        vel = new Vec3(vel.x, Math.min(targetY, vel.y + 0.06), vel.z);
                    }
                }
                e.setDeltaMovement(vel);
            }
        }

        // Freeze window: insiders are velocity-zeroed and stasis-marked.
        if (cloud.freezeTicks() > 0 && age <= cloud.freezeTicks()) {
            for (LivingEntity e : inside) {
                e.setDeltaMovement(Vec3.ZERO);
                // Pause mob AI (goals, pathing, attack timers) for the window.
                // Mobs already NoAI by someone else are left alone.
                if (e instanceof net.minecraft.world.entity.Mob mob && !mob.isNoAi()) {
                    mob.setNoAi(true);
                    this.aiPaused.add(mob.getUUID());
                }
                if (age % 5 == 0 && DbaEffects.TEMPORAL_STASIS_HOLDER != null) {
                    e.addEffect(new MobEffectInstance(DbaEffects.TEMPORAL_STASIS_HOLDER, 10, 0));
                }
            }
        }

        if (!this.aiPaused.isEmpty() && age > cloud.freezeTicks()) {
            resumePausedAi(serverLevel);
        }

        // Projectile stasis.
        if (cloud.stasis() != null) {
            AABB box = new AABB(center.x - radius, center.y - 2, center.z - radius,
                    center.x + radius, center.y + cloud.height(), center.z + radius);
            for (Projectile projectile : serverLevel.getEntitiesOfClass(Projectile.class, box,
                    pr -> !pr.isRemoved() && pr.getOwner() != owner)) {
                if ("freeze".equals(cloud.stasis().mode())) {
                    this.frozenProjectiles.putIfAbsent(projectile.getId(), projectile.getDeltaMovement());
                    projectile.setDeltaMovement(Vec3.ZERO);
                } else {
                    projectile.setDeltaMovement(
                            projectile.getDeltaMovement().scale(cloud.stasis().factor()));
                }
            }
        }

        // Inside effect on its interval.
        if (cloud.insideEffect() != null && age % cloud.insideEffect().intervalTicks() == 0) {
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(cloud.insideEffect().effectId());
            if (effect != null) {
                for (LivingEntity e : inside) {
                    e.addEffect(new MobEffectInstance(
                            BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                            cloud.insideEffect().durationTicks(), 0));
                }
            }
        }

        // Inside damage on its interval (latched only when chains are present).
        if (cloud.insideDamageInterval() > 0 && age % cloud.insideDamageInterval() == 0) {
            for (LivingEntity e : inside) {
                if (chainsActive && !this.latched.contains(e.getUUID())) {
                    continue;
                }
                double dist = Math.sqrt(Math.pow(e.getX() - center.x, 2) + Math.pow(e.getZ() - center.z, 2));
                AbilityDamage.applyHit(serverLevel, def, this, owner, e,
                        getPower(), radius <= 0 ? 0 : dist / radius, false);
            }
        }

        // One-shot rewind, after the freeze window.
        if (cloud.rewindTicks() > 0 && !this.rewindDone && age >= Math.max(cloud.freezeTicks(), 1)) {
            for (LivingEntity e : inside) {
                RewindService.rewind(e, cloud.rewindTicks());
            }
            this.rewindDone = true;
            playHitSound(serverLevel, def, center);
        }

        // Terrain field: convert the floor over time.
        if (def.terrain() != null && "field".equals(def.terrain().event())
                && def.terrain().placeId() != null) {
            int budgetPerTick = (int) Math.ceil(
                    (double) def.terrain().cap() / Math.max(1, def.phases().active())) + 1;
            int remaining = def.terrain().cap() - this.fieldConverted;
            if (remaining > 0) {
                this.fieldConverted += AbilityTerrain.fieldStep(serverLevel, center, radius,
                        def.terrain(), Math.min(budgetPerTick, remaining));
            }
        }
    }

    /** Natural expiry: the finale burst, finale terrain, and field crust. */
    @Override
    protected void onExpire() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AbilityDef def = def();
        if (def == null || def.cloud() == null) {
            return;
        }
        AbilityDef.CloudParams cloud = def.cloud();
        Player owner = ownerPlayer();
        Vec3 center = this.position();
        double radius = radiusAt(def, this.tickCount);

        AbilityDef.FinaleSpec finale = cloud.finale();
        if (finale != null && !this.finaleDone) {
            this.finaleDone = true;
            double finaleRadius = radius * finale.radiusScale();
            AbilityDamage.applyAreaScaled(serverLevel, def, this, owner, center,
                    finaleRadius, getPower(), owner, finale.damageScale());
            if (finale.knockback() != 0 || finale.slamDown() > 0) {
                for (LivingEntity e : insiders(serverLevel, def, finaleRadius, owner)) {
                    Vec3 away = e.position().subtract(center);
                    away = new Vec3(away.x, 0, away.z);
                    Vec3 vel = e.getDeltaMovement();
                    if (away.lengthSqr() > 1.0e-4 && finale.knockback() != 0) {
                        vel = vel.add(away.normalize().scale(finale.knockback()));
                    }
                    if (finale.slamDown() > 0) {
                        vel = new Vec3(vel.x, vel.y - finale.slamDown(), vel.z);
                    }
                    e.setDeltaMovement(vel);
                }
            }
            for (int i = 0; i < finale.strikes(); i++) {
                Vec3 point = chooseStrikePoint(serverLevel, def);
                AbilityDamage.applyAreaHit(serverLevel, def, this, owner,
                        point.add(0, 1.0, 0), cloud.strikeRadius(), getPower(), owner);
                if (i < 4) {
                    AbilityFxService.send(this, def, AbilityDef.FxEvent.CARRIER_HIT,
                            point.add(0, 0.5, 0), Vec3.ZERO,
                            getSeed() ^ this.tickCount ^ i, getPower(), 1);
                }
            }
        }

        // Models Pass: a "finale" trigger models set dresses the
        // finale moment (the Eclipse sky blades rain here, timed
        // with the finale damage above; presentation only).
        if (def.models() != null && "finale".equals(def.models().trigger())) {
            double areaRadius = def.cloud().finale() != null
                    ? radius * def.cloud().finale().radiusScale() : radius;
            com.dragonblockarcanedba.ability.ModelDisplaySpawner.spawnForFinale(
                    serverLevel, def, center, areaRadius, getOwnerUuid(),
                    getSeed() ^ this.tickCount);
        }

        if (def.terrain() != null && "finale".equals(def.terrain().event())) {
            AbilityTerrain.eventAt(serverLevel, this.blockPosition(), def.terrain());
        }
        if (def.terrain() != null && "field".equals(def.terrain().event())) {
            AbilityTerrain.crustRing(serverLevel, center, radius, def.terrain());
        }
    }

    /** Restores frozen projectile velocities on any removal path. */
    @Override
    public void remove(RemovalReason reason) {
        if (this.level() instanceof ServerLevel sl && !this.aiPaused.isEmpty()) {
            resumePausedAi(sl);
        }
        if (this.level() instanceof ServerLevel serverLevel && !this.frozenProjectiles.isEmpty()) {
            for (Map.Entry<Integer, Vec3> entry : this.frozenProjectiles.entrySet()) {
                Entity projectile = serverLevel.getEntity(entry.getKey());
                if (projectile != null && !projectile.isRemoved()) {
                    projectile.setDeltaMovement(entry.getValue());
                }
            }
            this.frozenProjectiles.clear();
        }
        super.remove(reason);
    }

    private void resumePausedAi(ServerLevel serverLevel) {
        for (UUID id : this.aiPaused) {
            if (serverLevel.getEntity(id) instanceof net.minecraft.world.entity.Mob mob) {
                mob.setNoAi(false);
            }
        }
        this.aiPaused.clear();
    }
}
