package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.ability.AbilityDamage;
import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.ability.AbilityFeedbackService;
import com.dragonblockarcanedba.ability.AbilityFxService;
import com.dragonblockarcanedba.ability.AbilityService;
import com.dragonblockarcanedba.ability.AbilityTerrain;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The Wave carrier: a ground-anchored band that travels in a
 * straight line from the caster, damaging each living entity once
 * as it reaches them, with linear falloff from its center out to
 * its radius. All numbers come from the ability definition; this
 * class holds no per-weapon behavior. The band's look (an arc of
 * arcDegrees around the travel direction) is the renderer's reading
 * of the same parameters.
 */
public class WaveCarrierEntity extends CarrierEntity {
    private final Set<UUID> hit = new HashSet<>();
    private final Set<Integer> deflected = new HashSet<>();
    private boolean selfLaunched;
    private Vec3 origin;
    private boolean carrierHitSummonFired;
    private int trailConverted;

    public WaveCarrierEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        AbilityDef def = def();
        if (def == null || def.wave() == null) {
            return;
        }
        AbilityDef.WaveParams wave = def.wave();

        // Travel: straight ahead at the synced yaw, snapped to the ground.
        double yawRad = Math.toRadians(this.getYRot());
        double dx = -Math.sin(yawRad);
        double dz = Math.cos(yawRad);
        if (this.origin == null) {
            this.origin = this.position();
        }
        double nx = this.getX() + dx * wave.speed();
        double nz = this.getZ() + dz * wave.speed();
        int groundY = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING,
                (int) Math.floor(nx), (int) Math.floor(nz));
        this.setPos(nx, groundY, nz);

        // Damage: everyone inside the band volume, once each.
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 center = this.position();
            AABB box = new AABB(
                    center.x - wave.radius(), center.y - 0.5, center.z - wave.radius(),
                    center.x + wave.radius(), center.y + wave.height() + 1.0, center.z + wave.radius());
            Player owner = ownerPlayer();
            int hitsThisTick = 0;
            for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && e != owner)) {
                if (this.hit.contains(target.getUUID())) {
                    continue;
                }
                double distX = target.getX() - center.x;
                double distZ = target.getZ() - center.z;
                double dist = Math.sqrt(distX * distX + distZ * distZ);
                if (wave.reach() > 0 && !insideCone(target, wave, dx, dz)) {
                    continue;
                }
                if (dist <= wave.radius() + target.getBbWidth() * 0.5) {
                    this.hit.add(target.getUUID());
                    AbilityDamage.applyHit(serverLevel, def, this, owner, target,
                            getPower(), dist / wave.radius(), false);
                    if (wave.knockback() != null) {
                        Vec3 away = new Vec3(distX, 0, distZ);
                        Vec3 vel = target.getDeltaMovement();
                        if (away.lengthSqr() > 1.0e-4 && wave.knockback().out() > 0) {
                            vel = vel.add(away.normalize().scale(wave.knockback().out()));
                        }
                        if (wave.knockback().up() > 0) {
                            vel = new Vec3(vel.x, vel.y + wave.knockback().up(), vel.z);
                        }
                        target.setDeltaMovement(vel);
                    }
                    hitsThisTick++;
                }
            }
            // One packet per tick with hits, aggregated by repeat
            // (capped so a huge sweep stays one small packet).
            if (hitsThisTick > 0) {
                AbilityFxService.send(this, def, AbilityDef.FxEvent.CARRIER_HIT,
                        center, new Vec3(dx, 0, dz),
                        getSeed() ^ this.tickCount, getPower(), Math.min(hitsThisTick, 8));
                AbilityFeedbackService.carrier(this, def, AbilityDef.FxEvent.CARRIER_HIT, center);
                // Phase 6: self-launch on the first connecting tick.
                if (wave.selfLaunch() > 0 && !this.selfLaunched && owner != null) {
                    this.selfLaunched = true;
                    Vec3 vel = owner.getDeltaMovement();
                    Vec3 launched = new Vec3(vel.x, Math.max(vel.y, wave.selfLaunch()), vel.z);
                    // A bare server setDeltaMovement on a player never reaches the
                    // client; MovementPhysics syncs it.
                    if (owner instanceof ServerPlayer sp) {
                        com.dragonblockarcanedba.movement.MovementPhysics.setVelocity(sp, launched);
                    } else {
                        owner.setDeltaMovement(launched);
                    }
                }
                // Phase 6: carrierHit summons, once per wave unless data says otherwise.
                if (owner instanceof ServerPlayer serverOwner) {
                    for (AbilityDef.SummonSpec summon : def.summons()) {
                        if (!"carrierHit".equals(summon.when())) {
                            continue;
                        }
                        if (summon.once() && this.carrierHitSummonFired) {
                            continue;
                        }
                        this.carrierHitSummonFired = true;
                        AbilityService.executeSummon(serverOwner, def, summon,
                                getPower(), center, null);
                    }
                }
            }
            tickWaveModules(serverLevel, def, wave, box);
        }
    }

    /** Phase 6 wave modules that run even on ticks without a hit: deflect and terrain trail. */
    /** The reach cone: within reach of the spawn point and half the arc of the aim. */
    private boolean insideCone(net.minecraft.world.entity.Entity target, AbilityDef.WaveParams wave, double dx, double dz) {
        double ox = target.getX() - this.origin.x;
        double oz = target.getZ() - this.origin.z;
        double d = Math.sqrt(ox * ox + oz * oz);
        if (d > wave.reach() + target.getBbWidth() * 0.5) {
            return false;
        }
        if (d < 1.5 || wave.arcDegrees() >= 360) {
            return true;
        }
        double cos = (ox * dx + oz * dz) / d;
        return cos >= Math.cos(Math.toRadians(wave.arcDegrees() * 0.5));
    }

    private void tickWaveModules(ServerLevel serverLevel, AbilityDef def,
                                 AbilityDef.WaveParams wave, AABB box) {
        AbilityDef.WaveDeflectSpec deflect = wave.deflect();
        if (deflect != null) {
            Player owner = ownerPlayer();
            // With a reach cone the whole cone bats projectiles back for the carrier's life
            // (Wind Spin design: 25 block reach, 35 degrees); otherwise only the band does.
            boolean cone = wave.reach() > 0 && this.origin != null;
            double yawRad = Math.toRadians(this.getYRot());
            double cdx = -Math.sin(yawRad);
            double cdz = Math.cos(yawRad);
            AABB scan = cone
                    ? new AABB(this.origin, this.origin).inflate(wave.reach(), 0, wave.reach())
                            .expandTowards(0, wave.height() + 1.0, 0).move(0, -0.5, 0)
                    : box;
            for (Projectile projectile : serverLevel.getEntitiesOfClass(Projectile.class, scan,
                    pr -> !pr.isRemoved() && pr.getOwner() != owner
                            && (!cone || insideCone(pr, wave, cdx, cdz)))) {
                if (this.deflected.contains(projectile.getId())) {
                    continue;
                }
                this.deflected.add(projectile.getId());
                projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-deflect.scale()));
                if (deflect.claim() && owner != null) {
                    projectile.setOwner(owner);
                }
            }
        }
        if (def.terrain() != null && "trail".equals(def.terrain().event())
                && def.terrain().placeId() != null) {
            int remaining = def.terrain().cap() - this.trailConverted;
            if (remaining > 0) {
                this.trailConverted += AbilityTerrain.trailStep(serverLevel, this, def.terrain(), remaining);
            }
        }
    }

    /**
     * carrierEnd summons: spawned where the wave dies, facing the
     * wave's travel yaw (Heaven Splitter's lingering torn wall).
     */
    @Override
    protected void onExpire() {
        AbilityDef def = def();
        if (def == null || !(ownerPlayer() instanceof ServerPlayer serverOwner)) {
            return;
        }
        for (AbilityDef.SummonSpec summon : def.summons()) {
            if (!"carrierEnd".equals(summon.when())) {
                continue;
            }
            net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.tryParse(summon.ability());
            AbilityDef summoned = id == null ? null : com.dragonblockarcanedba.ability.AbilityRegistry.get(id);
            if (summoned != null) {
                AbilityService.spawnCarrier(serverOwner, summoned, getPower(), this.position(), this.getYRot());
            }
        }
    }
}
