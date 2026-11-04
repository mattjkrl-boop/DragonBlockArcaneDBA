package com.dragonblockarcanedba.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * One piece of storm debris (SPECTACLE-STANDARD point 2): a real block that
 * orbits upward on a helical path around a center, gets flung off the helix,
 * falls under gravity, lands, and crumbles away. Spawned in fields by
 * util/DebrisSpawner. The motion is server authoritative; the renderer only
 * needs the synced block, scale, and seed (tumble is derived from age + seed).
 *
 * Phases: 0 orbit rise, 1 released and falling, 2 landed and fading.
 */
public class DebrisEntity extends Projectile {
    private static final EntityDataAccessor<BlockState> BLOCK = SynchedEntityData.defineId(DebrisEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(DebrisEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(DebrisEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(DebrisEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FADE_AT = SynchedEntityData.defineId(DebrisEntity.class, EntityDataSerializers.INT);

    public static final int PHASE_ORBIT = 0;
    public static final int PHASE_FALLING = 1;
    public static final int PHASE_LANDED = 2;
    public static final int FADE_TICKS = 12;
    public static final int HARD_LIFE = 400;

    // Server side orbit state (persisted).
    private double centerX, centerY, centerZ;
    private double orbitRadius;
    private double angle;
    private double angularSpeed;
    private double riseSpeed;
    private int orbitTicks = 60;

    public DebrisEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public DebrisEntity(Level level, Vec3 center, BlockState block, float scale, int seed,
                        double orbitRadius, double startAngle, double angularSpeed,
                        double riseSpeed, int orbitTicks) {
        super(DbaEntities.DEBRIS, level);
        this.centerX = center.x; this.centerY = center.y; this.centerZ = center.z;
        this.orbitRadius = orbitRadius;
        this.angle = startAngle;
        this.angularSpeed = angularSpeed;
        this.riseSpeed = riseSpeed;
        this.orbitTicks = orbitTicks;
        this.noPhysics = true;
        this.entityData.set(BLOCK, block);
        this.entityData.set(SCALE, scale);
        this.entityData.set(SEED, seed);
        this.setPos(center.x + Math.cos(angle) * orbitRadius, center.y, center.z + Math.sin(angle) * orbitRadius);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BLOCK, Blocks.DIRT.defaultBlockState());
        builder.define(SCALE, 0.75f);
        builder.define(SEED, 0);
        builder.define(PHASE, PHASE_ORBIT);
        builder.define(FADE_AT, -1);
    }

    public BlockState getBlock() { return this.entityData.get(BLOCK); }
    public float getScale() { return this.entityData.get(SCALE); }
    public int getSeed() { return this.entityData.get(SEED); }
    public int getPhase() { return this.entityData.get(PHASE); }
    public int getFadeAt() { return this.entityData.get(FADE_AT); }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        if (this.tickCount > HARD_LIFE) { this.discard(); return; }

        int phase = getPhase();
        if (phase == PHASE_ORBIT) {
            this.angle += this.angularSpeed;
            double y = this.getY() + this.riseSpeed;
            this.setPos(this.centerX + Math.cos(this.angle) * this.orbitRadius, y,
                        this.centerZ + Math.sin(this.angle) * this.orbitRadius);
            if (this.tickCount >= this.orbitTicks) {
                // Release: keep the tangential velocity, add a little outward and up,
                // so pieces leave the helix the way the storm throws them.
                double tx = -Math.sin(this.angle), tz = Math.cos(this.angle);
                double dirSign = this.angularSpeed >= 0 ? 1.0 : -1.0;
                this.setDeltaMovement(
                    tx * 0.35 * dirSign + Math.cos(this.angle) * 0.18,
                    0.22,
                    tz * 0.35 * dirSign + Math.sin(this.angle) * 0.18);
                this.entityData.set(PHASE, PHASE_FALLING);
            }
        } else if (phase == PHASE_FALLING) {
            Vec3 vel = this.getDeltaMovement();
            vel = new Vec3(vel.x * 0.99, Math.max(vel.y - 0.04, -1.6), vel.z * 0.99);
            this.setDeltaMovement(vel);
            Vec3 next = this.position().add(vel);
            this.setPos(next.x, next.y, next.z);
            // Landed when the block below is solid or motion stalls at ground level.
            var below = net.minecraft.core.BlockPos.containing(next.x, next.y - 0.06, next.z);
            if (!this.level().getBlockState(below).getCollisionShape(this.level(), below).isEmpty() && vel.y <= 0) {
                this.entityData.set(PHASE, PHASE_LANDED);
                this.entityData.set(FADE_AT, this.tickCount);
                if (this.level() instanceof ServerLevel server) {
                    server.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.DUST_PUFF,
                        next.x, next.y + 0.2, next.z, 3, 0.2, 0.08, 0.2, 0.02);
                    // Monolith-class pieces (the largest size class) dent
                    // the ground they fall on: a small permanent crater,
                    // WEAK tier, so only soft ground gives. Ordinary chunks
                    // land harmlessly, as they always have.
                    if (this.getScale() >= 1.0f) {
                        com.dragonblockarcanedba.util.PermanentDestruction.crater(server, below.immutable(),
                            2.0, 0.5, false,
                            com.dragonblockarcanedba.util.PermanentDestruction.TIER_WEAK, 60,
                            server.getRandom());
                    }
                }
            }
        } else {
            int fadeAt = getFadeAt();
            if (fadeAt >= 0 && this.tickCount >= fadeAt + FADE_TICKS) this.discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Block", BuiltInRegistries.BLOCK.getKey(getBlock().getBlock()).toString());
        output.putFloat("Scale", getScale());
        output.putInt("Seed", getSeed());
        output.putInt("Phase", getPhase());
        output.putInt("FadeAt", getFadeAt());
        output.putDouble("CenterX", this.centerX);
        output.putDouble("CenterY", this.centerY);
        output.putDouble("CenterZ", this.centerZ);
        output.putDouble("OrbitRadius", this.orbitRadius);
        output.putDouble("Angle", this.angle);
        output.putDouble("AngularSpeed", this.angularSpeed);
        output.putDouble("RiseSpeed", this.riseSpeed);
        output.putInt("OrbitTicks", this.orbitTicks);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        String id = input.getStringOr("Block", "minecraft:dirt");
        var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
        if (block != null) this.entityData.set(BLOCK, block.defaultBlockState());
        this.entityData.set(SCALE, input.getFloatOr("Scale", 0.75f));
        this.entityData.set(SEED, input.getIntOr("Seed", 0));
        this.entityData.set(PHASE, input.getIntOr("Phase", PHASE_FALLING));
        this.entityData.set(FADE_AT, input.getIntOr("FadeAt", -1));
        this.centerX = input.getDoubleOr("CenterX", this.getX());
        this.centerY = input.getDoubleOr("CenterY", this.getY());
        this.centerZ = input.getDoubleOr("CenterZ", this.getZ());
        this.orbitRadius = input.getDoubleOr("OrbitRadius", 2.0);
        this.angle = input.getDoubleOr("Angle", 0.0);
        this.angularSpeed = input.getDoubleOr("AngularSpeed", 0.1);
        this.riseSpeed = input.getDoubleOr("RiseSpeed", 0.1);
        this.orbitTicks = input.getIntOr("OrbitTicks", 60);
    }
}
