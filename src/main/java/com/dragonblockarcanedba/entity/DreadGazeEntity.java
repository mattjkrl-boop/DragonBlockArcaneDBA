package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * Dread Gaze: the invisible anchor Evil Dread hangs its eyes on. Spawned
 * and replaced by EvilDreadEffect, it follows its victim at their feet
 * and carries only the victim id; the renderer seeds the eye ring from
 * the victim's UUID, so the ring is stable even if this anchor is
 * replaced mid-effect. It has no collision, deals no damage, and
 * discards itself within ten ticks of the victim dying, leaving, or
 * losing the effect, so it can never outlive the dread that made it.
 */
public class DreadGazeEntity extends Projectile {
    private static final EntityDataAccessor<Integer> VICTIM_ID = SynchedEntityData.defineId(DreadGazeEntity.class, EntityDataSerializers.INT);

    private int missingTicks = 0;

    public DreadGazeEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DreadGazeEntity(Level level, LivingEntity victim) {
        super(DbaEntities.DREAD_GAZE, level);
        this.setPos(victim.getX(), victim.getY(), victim.getZ());
        this.entityData.set(VICTIM_ID, victim.getId());
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(VICTIM_ID, -1);
    }

    public int getVictimId() {
        return this.entityData.get(VICTIM_ID);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            Entity target = getVictimId() >= 0 ? serverLevel.getEntity(getVictimId()) : null;
            if (target instanceof LivingEntity victim && victim.isAlive() && !victim.isRemoved()
                && victim.hasEffect(DbaEffects.EVIL_DREAD_HOLDER)) {
                this.missingTicks = 0;
                this.setPos(victim.getX(), victim.getY(), victim.getZ());
            } else if (++this.missingTicks > 10) {
                // The dread ended, the victim died or left this level, or
                // the anchor loaded without its victim: ten ticks of
                // grace for packet ordering, then it removes itself.
                this.discard();
            }
        }
    }
}
