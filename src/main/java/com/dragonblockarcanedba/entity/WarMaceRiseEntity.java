package com.dragonblockarcanedba.entity;

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

/** The rising blow under a war mace. One hit, then the crescent climbs and ends. */
public class WarMaceRiseEntity extends Projectile {
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(WarMaceRiseEntity.class, EntityDataSerializers.FLOAT);
    private final Set<Integer> hitIds = new HashSet<>();

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
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel server) || this.tickCount != 1) {
            return;
        }
        Entity owner = this.getOwner();
        float damage = this.entityData.get(DAMAGE);
        AABB box = this.getBoundingBox().inflate(3.2, 1.6, 3.2);
        List<LivingEntity> hits = server.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != owner && !this.hitIds.contains(e.getId()));
        for (LivingEntity living : hits) {
            if (living.distanceToSqr(this) > 3.4 * 3.4) {
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
        }
    }
}
