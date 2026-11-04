package com.dragonblockarcanedba.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.LayeredParticles;
import com.dragonblockarcanedba.util.PermanentDestruction;

import java.util.List;

/**
 * Hell Hunt Impact Entity: Physical 3D demonic ground-shatter fissure trenches, erupting cursed blood/obsidian spikes,
 * expanding shockwave seals, and surging execution pillars triggered during Hell Hunt chain executions.
 *
 * Spectacle pass: the drawn fissures are now backed by real world interaction.
 * On its first tick the impact marks the actual ground (permanent obsidian,
 * plus a real crater on the final form), throws a themed debris field (DebrisSpawner), and
 * fires a layered ground burst. Through its life it emits the shared three
 * layer vortex particles under the FxEnvelope and rains embers while it dies,
 * so the effect settles instead of discarding at visible alpha.
 */
public class HellHuntImpactEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(HellHuntImpactEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_FINAL = SynchedEntityData.defineId(HellHuntImpactEntity.class, EntityDataSerializers.BOOLEAN);

    /** Total lifetimes, shared with the renderer so both sides agree on the envelope. */
    public static final int LIFE_NORMAL = 35;
    public static final int LIFE_FINAL = 50;
    /** Demonic crimson theme color for the layered particle calls. */
    public static final int THEME_ARGB = 0xD21F3C;

    private int lifetime = LIFE_NORMAL; // server side countdown
    private int totalLifetime = LIFE_NORMAL;
    private boolean impactDone = false;

    public HellHuntImpactEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public HellHuntImpactEntity(Level level, LivingEntity owner, Vec3 pos, float radius, boolean isFinal) {
        super(DbaEntities.HELL_HUNT_IMPACT, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.setRadius(radius);
        this.setFinal(isFinal);
        this.totalLifetime = isFinal ? LIFE_FINAL : LIFE_NORMAL;
        this.lifetime = this.totalLifetime;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 3.0f);
        builder.define(IS_FINAL, false);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(RADIUS, radius);
    }

    public boolean isFinal() {
        return this.entityData.get(IS_FINAL);
    }

    public void setFinal(boolean isFinal) {
        this.entityData.set(IS_FINAL, isFinal);
    }

    public int getLifetime() {
        return this.totalLifetime;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if (!this.impactDone) {
                this.impactDone = true;
                onImpact(serverLevel);
            }

            // Layered spectacle (standard points 1 and 5): core, mid, and
            // outer particle layers under the shared envelope for the whole
            // life, then an ember drizzle while the effect dissipates.
            Vec3 center = new Vec3(this.getX(), this.getY() + 0.1, this.getZ());
            LayeredParticles.emitVortex(serverLevel, center, getRadius() * 0.85f,
                isFinal() ? 3.4f : 2.4f, this.tickCount, this.totalLifetime,
                THEME_ARGB, DbaParticles.EMBER);
            if (this.tickCount > this.totalLifetime * 0.6f) {
                LayeredParticles.emitRain(serverLevel, center, getRadius(), 2.0f,
                    this.tickCount, this.totalLifetime, DbaParticles.EMBER);
            }

            this.lifetime--;
            if (this.lifetime <= 0) {
                this.discard();
            }
        }
    }

    /**
     * First tick world interaction (standard points 2 and 3): a layered dust
     * burst at the base, a debris field of hell rock torn out of the impact,
     * and a real permanent mark where the renderer draws its fissures.
     */
    private void onImpact(ServerLevel level) {
        Vec3 center = new Vec3(this.getX(), this.getY() + 0.1, this.getZ());
        LayeredParticles.emitGroundBurst(level, center, getRadius(), THEME_ARGB);

        float density = DbaEffectsConfig.effectDensity();
        if (density > 0f) {
            List<BlockState> palette = List.of(
                Blocks.OBSIDIAN.defaultBlockState(),
                Blocks.BLACKSTONE.defaultBlockState(),
                Blocks.CRIMSON_NYLIUM.defaultBlockState(),
                Blocks.NETHERRACK.defaultBlockState());
            int count = Math.max(1, Math.round((isFinal() ? 24 : 10) * density));
            LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
            DebrisSpawner.spawnField(level, owner, center, count,
                getRadius() * 0.85, 2.4, 26, palette);
        }

        scarGround(level);
    }

    /**
     * Mark the surface under the impact with permanent obsidian, following
     * each column to its real surface the way LavaSwordGround does, so
     * slopes mark correctly and nothing floats. On the final form the
     * impact also punches a shallow crater into the ground at its heart.
     * PermanentDestruction refuses protected blocks.
     */
    private void scarGround(ServerLevel level) {
        int r = isFinal() ? 2 : 1;
        float chance = isFinal() ? 0.65f : 0.8f;
        BlockState scar = Blocks.OBSIDIAN.defaultBlockState();
        BlockPos base = BlockPos.containing(this.getX(), this.getY(), this.getZ());
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                if (level.getRandom().nextFloat() > chance) continue;
                // Find this column's surface: the first solid block at or
                // below the impact height with air above it.
                for (int dy = 1; dy >= -3; dy--) {
                    m.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    BlockState state = level.getBlockState(m);
                    if (state.isAir() || !state.getFluidState().isEmpty()) continue;
                    if (!state.getCollisionShape(level, m).isEmpty()
                        && level.getBlockState(m.above()).getCollisionShape(level, m.above()).isEmpty()) {
                        PermanentDestruction.convert(level, m.immutable(), scar);
                        break;
                    }
                }
            }
        }
        if (isFinal()) {
            BlockPos air = PermanentDestruction.findSurface(level, base.getX(), base.getZ(), base.getY());
            if (air != null) {
                PermanentDestruction.crater(level, air.below(), 2.0, 0.5, false,
                    PermanentDestruction.TIER_WEAK, 60, level.getRandom());
            }
        }
    }
}
