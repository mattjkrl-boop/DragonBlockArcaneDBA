package com.dragonblockarcanedba.entity;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Azure Sonic Quake Entity: Physical 3D supersonic ground shockwave and kinetic shatter geometry.
 *
 * Spectacle pass: the quake now owns the bounciness it hands out. The item
 * used to stamp a permanent 0.85 BOUNCINESS modifier on every victim and no
 * code anywhere removed it, so one quake changed a mob forever. Victims are
 * registered here, the modifier comes off BOUNCE_TICKS later, and the pending
 * list is saved so an unload mid bounce cannot strand the modifier either.
 * On its first tick the quake also does its ground work: base dust, a field
 * of real debris, and a temporary torn ground scar under the impact.
 */
public class AzureSonicQuakeEntity extends Projectile {
    private static final EntityDataAccessor<Float> MAX_RADIUS = SynchedEntityData.defineId(AzureSonicQuakeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> SEED = SynchedEntityData.defineId(AzureSonicQuakeEntity.class, EntityDataSerializers.LONG);

    /** How long the bounciness stamped by triggerSonicQuake lasts on a victim. */
    public static final int BOUNCE_TICKS = 60;

    private int maxLifetime = 18;
    private boolean groundWorkDone = false;
    private final List<UUID> bounceTargets = new ArrayList<>();
    private int bounceTicksLeft = 0;
    private int removalAttempts = 0;

    public AzureSonicQuakeEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public AzureSonicQuakeEntity(Level level, LivingEntity owner, Vec3 pos, float maxRadius) {
        super(DbaEntities.AZURE_SONIC_QUAKE, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.noPhysics = true;
        this.entityData.set(MAX_RADIUS, maxRadius);
        // Renderer shape seed: fixed at spawn, synced and saved.
        this.entityData.set(SEED, this.getUUID().getMostSignificantBits() ^ this.getUUID().getLeastSignificantBits());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MAX_RADIUS, 8.5f);
        builder.define(SEED, 0L);
    }

    /** Register a victim whose bounce modifier this quake must remove later. */
    public void addBounceTarget(UUID uuid) {
        if (!this.bounceTargets.contains(uuid)) {
            this.bounceTargets.add(uuid);
        }
        this.bounceTicksLeft = Math.max(this.bounceTicksLeft, BOUNCE_TICKS);
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("RenderSeed", this.getRenderSeed());
        output.putBoolean("GroundWorkDone", this.groundWorkDone);
        output.putInt("BounceTicksLeft", this.bounceTicksLeft);
        output.putInt("BounceCount", this.bounceTargets.size());
        for (int i = 0; i < this.bounceTargets.size(); i++) {
            UUID u = this.bounceTargets.get(i);
            output.putLong("Bounce" + i + "Most", u.getMostSignificantBits());
            output.putLong("Bounce" + i + "Least", u.getLeastSignificantBits());
        }
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(SEED, input.getLongOr("RenderSeed", 0L));
        this.groundWorkDone = input.getBooleanOr("GroundWorkDone", false);
        this.bounceTicksLeft = input.getIntOr("BounceTicksLeft", 0);
        int count = input.getIntOr("BounceCount", 0);
        this.bounceTargets.clear();
        for (int i = 0; i < count; i++) {
            this.bounceTargets.add(new UUID(
                input.getLongOr("Bounce" + i + "Most", 0L),
                input.getLongOr("Bounce" + i + "Least", 0L)));
        }
    }

    public float getMaxRadius() {
        return this.entityData.get(MAX_RADIUS);
    }

    public long getRenderSeed() {
        return this.entityData.get(SEED);
    }

    public int getMaxLifetime() {
        return this.maxLifetime;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!this.groundWorkDone) {
            this.groundWorkDone = true;
            doGroundWork(serverLevel);
        }

        // Bounce bookkeeping: count down, then strip the modifier off every
        // registered victim. Victims in unloaded chunks are retried for a
        // while; a victim that no longer exists took its modifier with it.
        if (!this.bounceTargets.isEmpty()) {
            if (this.bounceTicksLeft > 0) {
                this.bounceTicksLeft--;
            } else if (this.tickCount % 20 == 0 || this.removalAttempts == 0) {
                this.removalAttempts++;
                List<UUID> remaining = new ArrayList<>();
                for (UUID uuid : this.bounceTargets) {
                    Entity e = serverLevel.getEntity(uuid);
                    if (e instanceof LivingEntity living) {
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.removeModifier(
                            living,
                            com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                            com.dragonblockarcanedba.DragonBlockArcaneDBA.id("sonic_quake_bounce"));
                    } else if (e == null && this.removalAttempts < 10) {
                        remaining.add(uuid);
                    }
                }
                this.bounceTargets.clear();
                this.bounceTargets.addAll(remaining);
            }
        }

        // The visual life is 18 ticks, but the entity stays until every bounce
        // it handed out has been taken back.
        if (this.tickCount >= this.maxLifetime && this.bounceTargets.isEmpty()) {
            this.discard();
        }
    }

    /** Dust burst, real debris, and a torn ground scar at the impact point. */
    private void doGroundWork(ServerLevel level) {
        Vec3 pos = this.position();
        float radius = getMaxRadius();
        LivingEntity livingOwner = this.getOwner() instanceof LivingEntity ? (LivingEntity) this.getOwner() : null;

        com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(level, pos,
            radius * 0.5f, AzureStormEntity.AZURE_ARGB);

        float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
        int count = Math.max(6, Math.round(26f * density));
        com.dragonblockarcanedba.util.DebrisSpawner.spawnField(level, livingOwner, pos,
            count, radius * 0.8, 3.5, 26);

        // Shatter read: grass in the inner disk is torn to dirt, permanently.
        int r = Math.max(1, (int) Math.ceil(radius * 0.6));
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int cx = (int) Math.floor(pos.x), cz = (int) Math.floor(pos.z);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                if (level.getRandom().nextFloat() > 0.35f) continue;
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, cx + dx, cz + dz) - 1;
                m.set(cx + dx, y, cz + dz);
                BlockState state = level.getBlockState(m);
                if (state.is(Blocks.GRASS_BLOCK)) {
                    com.dragonblockarcanedba.util.PermanentDestruction.convert(level, m.immutable(), Blocks.DIRT.defaultBlockState());
                }
            }
        }

        // Ring fissures: two cracked rings at radius 3 and 5, cut as
        // tangential fissure segments so the quake's rings are real cuts,
        // one block deep at WEAK tier, sharing one budget.
        int budget = 80;
        budget -= ringFissure(level, cx, cz, (int) Math.floor(pos.y), 3, budget);
        ringFissure(level, cx, cz, (int) Math.floor(pos.y), 5, budget);
    }

    /**
     * Cut one ring fissure: eight tangential segments around the circle,
     * each starting at the surface of its own column. Returns how many
     * blocks were destroyed, bounded by the remaining budget.
     */
    private int ringFissure(ServerLevel level, int cx, int cz, int yHint, int ringRadius, int budget) {
        if (budget <= 0) return 0;
        int destroyed = 0;
        int segments = 8;
        double segLen = 2.0 * Math.PI * ringRadius / segments + 0.5;
        for (int s = 0; s < segments && destroyed < budget; s++) {
            double a = s / (double) segments * Math.PI * 2.0;
            int sx = cx + (int) Math.round(Math.cos(a) * ringRadius);
            int sz = cz + (int) Math.round(Math.sin(a) * ringRadius);
            BlockPos air = com.dragonblockarcanedba.util.PermanentDestruction.findSurface(level, sx, sz, yHint + 1);
            if (air == null) continue;
            Vec3 tangent = new Vec3(-Math.sin(a), 0, Math.cos(a));
            destroyed += com.dragonblockarcanedba.util.PermanentDestruction.fissure(level, air, tangent,
                segLen, 1, 1, false,
                com.dragonblockarcanedba.util.PermanentDestruction.TIER_WEAK, budget - destroyed);
        }
        return destroyed;
    }
}
