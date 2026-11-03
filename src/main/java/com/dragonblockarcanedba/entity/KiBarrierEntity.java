package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.ki.KiBarrier;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Visual and lifetime shell for a raised Ki Barrier. It sticks to the caster
 * and asks {@link KiBarrier} to spend ki. It does not collide, so hits still
 * land on the player and get priced once in the damage mixin.
 */
public class KiBarrierEntity extends Projectile {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(KiBarrierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> USED_PERCENT = SynchedEntityData.defineId(KiBarrierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(KiBarrierEntity.class, EntityDataSerializers.INT);

    /** Ki this cast is allowed to spend, snapshotted when the barrier is raised. */
    private double budgetTotal;
    private double budgetRemaining;
    private long absorbSoundAt;

    public KiBarrierEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public KiBarrierEntity(Level level, ServerPlayer owner, int color, int usedPercent) {
        super(KiBarrier.TYPE, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setOwner(owner);
        this.entityData.set(OWNER_ID, owner.getId());
        this.entityData.set(COLOR, color);
        this.entityData.set(USED_PERCENT, usedPercent);
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xFF55DDFF);
        builder.define(USED_PERCENT, 50);
        builder.define(OWNER_ID, -1);
    }

    public int getColor() {
        return this.entityData.get(COLOR);
    }

    public int getUsedPercent() {
        return this.entityData.get(USED_PERCENT);
    }

    public int getOwnerId() {
        return this.entityData.get(OWNER_ID);
    }

    public void setBudget(double budget) {
        this.budgetTotal = budget;
        this.budgetRemaining = budget;
    }

    public double getBudgetTotal() {
        return this.budgetTotal;
    }

    public double getBudgetRemaining() {
        return this.budgetRemaining;
    }

    public void spendBudget(double amount) {
        this.budgetRemaining -= amount;
    }

    /**
     * @return true when an absorb sound is allowed, so a flurry does not stack the cue
     */
    public boolean claimAbsorbSound(long gameTime) {
        if (gameTime < this.absorbSoundAt) {
            return false;
        }
        this.absorbSoundAt = gameTime + 6L;
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = this.getOwner();
        if (owner == null && getOwnerId() != -1) {
            owner = this.level().getEntity(getOwnerId());
        }
        if (owner != null) {
            this.setPos(owner.getX(), owner.getY(), owner.getZ());
            this.setDeltaMovement(Vec3.ZERO);
        }
        if (this.level().isClientSide() || this.isRemoved()) {
            return;
        }
        if (owner instanceof ServerPlayer player && player.isAlive() && !player.isRemoved()) {
            KiBarrier.serverTick(this, player);
        } else {
            KiBarrier.forget(this);
        }
    }
}
