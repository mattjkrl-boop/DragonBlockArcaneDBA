package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Sky Cracks: a real tear in the sky above the Cursed Tempest. The renderer
 * draws a branching crack network from the synced seed (see
 * SkyCracksRenderer), the entity follows its caster for its 100 tick life so
 * the tear stays over the moving storm, and every 20 ticks it sheds a visual
 * BoltEntity strike down into the storm below. Damage stays with the
 * tempest's own strike systems in CurseBladeItem; the cracks are the storm's
 * sky made visible, with a job to do.
 */
public class SkyCracksEntity extends Projectile {
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(SkyCracksEntity.class, EntityDataSerializers.INT);

    public static final int LIFE_TICKS = 100; // 5 seconds

    public SkyCracksEntity(EntityType<?> entityType, Level level) {
        super((EntityType<? extends Projectile>) entityType, level);
        this.noPhysics = true;
        if (!level.isClientSide()) {
            this.entityData.set(SEED, level.getRandom().nextInt());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SEED, 0);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        ServerLevel serverLevel = (ServerLevel) this.level();

        // Stay over the storm: the tempest moves with its caster.
        Entity owner = this.getOwner();
        if (owner != null && owner.isAlive()) {
            this.setPos(owner.getX(), Math.min(280.0, owner.getY() + 55.0), owner.getZ());
        }

        // The cracks spit bolts down into the tempest. Visual only: the item
        // owns all lightning damage, so these never double dip.
        if (this.tickCount % 20 == 10) {
            double rx = this.getX() + (serverLevel.getRandom().nextDouble() - 0.5) * 44.0;
            double rz = this.getZ() + (serverLevel.getRandom().nextDouble() - 0.5) * 44.0;
            double ry = serverLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) rx, (int) rz);
            boolean violet = ((getSeed() + this.tickCount) & 1) == 0;
            BoltEntity bolt = new BoltEntity(serverLevel, new Vec3(rx, ry, rz),
                serverLevel.getRandom().nextInt(), violet ? 0xFF9B30FF : 0xFFF20D33, 34.0f, 8);
            serverLevel.addFreshEntity(bolt);
        }

        if (this.tickCount >= LIFE_TICKS) {
            this.discard();
        }
    }
}
