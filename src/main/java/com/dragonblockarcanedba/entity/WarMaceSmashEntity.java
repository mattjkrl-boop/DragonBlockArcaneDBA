package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.item.WarMaceItem;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Overhead smash. A falling mace writes a wider, taller shell. Hits travel with the lip.
 */
public class WarMaceSmashEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> HEAVY = SynchedEntityData.defineId(WarMaceSmashEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(WarMaceSmashEntity.class, EntityDataSerializers.FLOAT);

    private final Set<Integer> hitIds = new HashSet<>();
    /** One connected smash launches the owner once, no matter how many mobs it caught. */
    private boolean relaunched;

    public WarMaceSmashEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public WarMaceSmashEntity(Level level, LivingEntity owner, float damage, boolean heavy) {
        super(DbaEntities.WAR_MACE_SMASH, level);
        this.setOwner(owner);
        this.noPhysics = true;
        if (owner != null) {
            this.setPos(owner.getX(), owner.getY() + 0.05, owner.getZ());
            this.setYRot(owner.getYRot());
        }
        this.entityData.set(HEAVY, heavy);
        this.entityData.set(DAMAGE, damage);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HEAVY, false);
        builder.define(DAMAGE, 900.0f);
    }

    public boolean isHeavy() {
        return this.entityData.get(HEAVY);
    }

    public int life() {
        return this.isHeavy() ? 14 : 10;
    }

    public float radiusAt(float age) {
        float max = this.isHeavy() ? 7.6f : 4.5f;
        float linear = Math.max(0.0f, Math.min(1.0f, age / this.life()));
        float eased = 1.0f - (1.0f - linear) * (1.0f - linear) * (1.0f - linear);
        return 0.7f + eased * (max - 0.7f);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > this.life()) {
            this.discard();
            return;
        }
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        Entity owner = this.getOwner();
        float radius = this.radiusAt(this.tickCount);
        float previous = this.radiusAt(this.tickCount - 1);
        AABB box = new AABB(
            this.getX() - radius, this.getY() - 0.4, this.getZ() - radius,
            this.getX() + radius, this.getY() + 2.4, this.getZ() + radius);
        List<LivingEntity> hits = server.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != owner && !this.hitIds.contains(e.getId()));
        float damage = this.entityData.get(DAMAGE);
        for (LivingEntity living : hits) {
            double dx = living.getX() - this.getX();
            double dz = living.getZ() - this.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            boolean inLip = dist <= radius + 0.6 && dist >= previous - 0.8;
            if (this.tickCount > 2 && !inLip) {
                continue;
            }
            this.hitIds.add(living.getId());
            var source = owner instanceof net.minecraft.world.entity.player.Player player
                ? server.damageSources().playerAttack(player)
                : server.damageSources().magic();
            living.hurtServer(server, source, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(damage));
            // A small nudge only. The player is the one who goes up, and the mob has to
            // still be under the next falling smash.
            double nx = 0.0;
            double nz = 0.0;
            if (dist >= 0.15) {
                nx = dx / dist;
                nz = dz / dist;
            }
            living.setDeltaMovement(living.getDeltaMovement().add(nx * 0.25, 0.08, nz * 0.25));
            com.dragonblockarcanedba.util.DbaCompat.markHurt(living);
            if (!this.relaunched && owner instanceof ServerPlayer smasher) {
                this.relaunched = true;
                WarMaceItem.relaunchFromSmash(smasher);
            }
        }
    }
}
