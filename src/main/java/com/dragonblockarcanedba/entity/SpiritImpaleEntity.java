package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.LayeredParticles;
import com.dragonblockarcanedba.util.PermanentDestruction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Spirit Impale Entity: Physical 3D divine celestial swords, rotating judgement runic arrays,
 * ascending divine pillar, and crystalline shockwave spikes skewering and suspending the target.
 *
 * Spectacle pass: the hold now runs 40 ticks so the array can build, peak,
 * and die properly. The pillar sheds the shared three layer vortex particles
 * every tick, the spawn slams a ground burst with real debris and a
 * permanent crystal mark under the target, and the end is a closing burst with
 * mote rain instead of a silent stop.
 */
public class SpiritImpaleEntity extends Projectile {
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(SpiritImpaleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(SpiritImpaleEntity.class, EntityDataSerializers.INT);

    private static final int LIFETIME = 40;
    private static final int CYAN = 0x00E5FF;

    private int maxLifetime = LIFETIME;
    private int lifetime = LIFETIME;
    private boolean opened = false;

    public SpiritImpaleEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SpiritImpaleEntity(Level level, LivingEntity owner, LivingEntity target, float scale) {
        super(DbaEntities.SPIRIT_IMPALE, level);
        this.setOwner(owner);
        this.noPhysics = true;
        this.setScale(scale);
        if (target != null) {
            this.setTargetId(target.getId());
            this.setPos(target.getX(), target.getY(), target.getZ());
        } else {
            this.setTargetId(-1);
            if (owner != null) {
                this.setPos(owner.getX(), owner.getY(), owner.getZ());
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SCALE, 1.0f);
        builder.define(TARGET_ID, -1);
    }

    public float getImpaleScale() {
        return this.entityData.get(SCALE);
    }

    public void setScale(float scale) {
        this.entityData.set(SCALE, scale);
    }

    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    public void setTargetId(int id) {
        this.entityData.set(TARGET_ID, id);
    }

    public int getMaxLifetime() {
        return this.maxLifetime;
    }

    @Override
    public void tick() {
        super.tick();

        int targetId = getTargetId();
        if (targetId != -1) {
            Entity target = this.level().getEntity(targetId);
            if (target instanceof LivingEntity living && living.isAlive()) {
                this.setPos(living.getX(), living.getY(), living.getZ());
            }
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            float scale = getImpaleScale();
            Vec3 center = this.position();

            // Opening beat (points 2 and 3): the array slamming in kicks up
            // ground dust, throws real debris off the ground it stands on,
            // and crystallizes a small scar that restores itself.
            if (!this.opened) {
                this.opened = true;
                LayeredParticles.emitGroundBurst(serverLevel, center, 2.2f * scale, CYAN);
                Entity owner = this.getOwner();
                DebrisSpawner.spawnField(serverLevel, owner instanceof LivingEntity lo ? lo : null,
                    center, 12, 2.4 * scale, 3.0, 22);
                scarGround(serverLevel, center, scale);
            }

            // Standing layered vortex (point 1): core motes up the pillar,
            // cyan dust band, wispy mote rim, all on the shared envelope.
            LayeredParticles.emitVortex(serverLevel, center.add(0, 0.1, 0),
                2.4f * scale, 6.0f * scale, this.tickCount, this.maxLifetime,
                CYAN, com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE);

            // Dissipation (point 5): the last stretch rains motes back down.
            if (this.lifetime <= 10) {
                LayeredParticles.emitRain(serverLevel, center, 2.2f * scale, 5.0f,
                    this.maxLifetime - this.lifetime, this.maxLifetime,
                    com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE);
            }

            this.lifetime--;
            if (this.lifetime <= 0) {
                // Closing burst: the array releases in light, never just stops.
                serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                    center.x, center.y + 1.0, center.z, 10, 1.4 * scale, 0.6, 1.4 * scale, 0.12);
                serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.KI_MOTE,
                    center.x, center.y + 1.2, center.z, 8, 1.0 * scale, 1.0, 1.0 * scale, 0.06);
                this.discard();
            }
        }
    }

    /**
     * Crystallize the surface cells under the array (center plus cardinals,
     * each at its own column height so slopes never float glass). The
     * crystal is permanent; PermanentDestruction refuses protected blocks.
     */
    private void scarGround(ServerLevel level, Vec3 center, float scale) {
        var glass = Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.LIGHT_BLUE).defaultBlockState();
        int[][] offsets = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        var rand = level.getRandom();
        for (int[] off : offsets) {
            if (rand.nextFloat() > 0.65f) continue;
            BlockPos column = new BlockPos(
                (int) Math.floor(center.x) + off[0], 0, (int) Math.floor(center.z) + off[1]);
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
            // The scar belongs to the ground the target stands on. If the
            // surface is far below, the target is airborne and the array
            // leaves the distant ground alone.
            if (surface.getY() < (int) Math.floor(center.y) - 3) continue;
            PermanentDestruction.convert(level, surface, glass);
        }
    }
}
