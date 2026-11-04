package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.fx.DbaImpactFx;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
 * The rising blow under a war mace. The hit volume climbs with the rendered
 * crescent (0.22 blocks per tick, the renderer's own lift) across ticks 1 to
 * 6 of its 8 tick life, so a target is caught when the crescent actually
 * reaches it instead of everything resolving on tick 1. Each target is hit
 * once, for the full rise damage. A layered dust column rises with it, and
 * the first connect answers with a spark burst and a medium impact shake.
 */
public class WarMaceRiseEntity extends Projectile {
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(WarMaceRiseEntity.class, EntityDataSerializers.FLOAT);

    /** Bronze theme color for dust, matching the renderer crescent (0.85, 0.62, 0.28). */
    private static final int THEME_ARGB = 0xD99E47;
    /** Last tick the hit volume still climbs and can connect. */
    private static final int DAMAGE_LAST_TICK = 6;
    /** Crescent climb per tick; must match WarMaceRiseRenderer's lift. */
    private static final double LIFT_PER_TICK = 0.22;

    private final Set<Integer> hitIds = new HashSet<>();
    /** Impact feedback fires once, on the first connect. */
    private boolean connected;

    public WarMaceRiseEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public WarMaceRiseEntity(Level level, LivingEntity owner, float damage) {
        super(DbaEntities.WAR_MACE_RISE, level);
        this.setOwner(owner);
        this.noPhysics = true;
        if (owner != null) {
            Vec3 look = owner.getLookAngle();
            this.setPos(owner.getX() + look.x * 0.8, owner.getY() + 0.2, owner.getZ() + look.z * 0.8);
            this.setYRot(owner.getYRot());
        }
        this.entityData.set(DAMAGE, damage);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DAMAGE, 480.0f);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > 8) {
            this.discard();
            return;
        }
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel server)) {
            return;
        }

        // Layered column for the whole climb: launch dust at the base on
        // tick 1, then the three layer vortex rising with the crescent.
        if (this.tickCount == 1) {
            LayeredParticles.emitGroundBurst(server, this.position(), 1.7f, THEME_ARGB);
        }
        LayeredParticles.emitVortex(server, this.position(), 1.25f, 2.4f,
            this.tickCount, 8f, THEME_ARGB, DbaParticles.KI_MOTE);

        if (this.tickCount > DAMAGE_LAST_TICK) {
            return;
        }
        Entity owner = this.getOwner();
        float damage = this.entityData.get(DAMAGE);
        double lift = (this.tickCount - 1) * LIFT_PER_TICK;
        Vec3 center = this.position().add(0.0, lift, 0.0);
        AABB box = this.getBoundingBox().inflate(3.2, 1.6, 3.2).move(0.0, lift, 0.0);
        List<LivingEntity> hits = server.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != owner && !this.hitIds.contains(e.getId()));
        for (LivingEntity living : hits) {
            if (living.distanceToSqr(center) > 3.4 * 3.4) {
                continue;
            }
            this.hitIds.add(living.getId());
            var source = owner instanceof net.minecraft.world.entity.player.Player player
                ? server.damageSources().playerAttack(player)
                : server.damageSources().magic();
            living.hurtServer(server, source, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(damage));
            Vec3 away = living.position().subtract(this.position());
            away = new Vec3(away.x, 0.0, away.z);
            if (away.lengthSqr() < 0.04) {
                away = new Vec3(0.0, 0.0, 1.0);
            }
            away = away.normalize().scale(0.45);
            living.setDeltaMovement(living.getDeltaMovement().add(away.x, 0.95, away.z));
            com.dragonblockarcanedba.util.DbaCompat.markHurt(living);
            if (!this.connected) {
                this.connected = true;
                server.sendParticles(DbaParticles.SPARK,
                    living.getX(), living.getY() + 1.0, living.getZ(),
                    10, 0.3, 0.3, 0.3, 0.25);
                DbaImpactFx.medium(server, living.getX(), living.getY(), living.getZ());
            }
        }
    }
}
