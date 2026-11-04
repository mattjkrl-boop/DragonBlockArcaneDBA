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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Delayed launch, second phase of the Z Sword Gravity Slam. Roots expire and
 * every target is flung up and outward from the slam centre.
 *
 * Spectacle pass: the slam radius, power and delay are synced data now, so the
 * telegraph ring covers the REAL slam radius (it used to be hardcoded at 5.0
 * shrinking to 1.2 while the slam itself reached 18). The caster is stored as
 * owner. The whole state is saved and loaded, targets rebuilt by UUID after a
 * reload, so a chunk unload can no longer silently cancel the launch. The
 * bounce and air drag modifiers are timed (200 ticks) instead of permanent,
 * and the launch moment has debris, particles and sound instead of silence.
 */
public class DelayedLaunchEntity extends Projectile {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(DelayedLaunchEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(DelayedLaunchEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DELAY = SynchedEntityData.defineId(DelayedLaunchEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(DelayedLaunchEntity.class, EntityDataSerializers.INT);

    private final List<LivingEntity> targets = new ArrayList<>();
    private final List<UUID> pendingTargetIds = new ArrayList<>();
    private Vec3 centerPos = Vec3.ZERO;
    private float powerRatio = 1.0f;
    private int delayTicks = 40;
    private int elapsed = 0;
    private boolean resolved = true;

    public DelayedLaunchEntity(EntityType<?> entityType, Level level) {
        super((EntityType<? extends Projectile>) entityType, level);
        this.noPhysics = true;
    }

    public DelayedLaunchEntity(Level level, Vec3 center, float powerRatio, List<LivingEntity> targets) {
        this(level, null, center, powerRatio, targets, (float) (6.0 + powerRatio * 12.0), 40);
    }

    public DelayedLaunchEntity(Level level, LivingEntity owner, Vec3 center, float powerRatio,
                               List<LivingEntity> targets, float radius, int delayTicks) {
        super(DbaEntities.DELAYED_LAUNCH, level);
        this.setPos(center.x, center.y, center.z);
        if (owner != null) this.setOwner(owner);
        this.centerPos = center;
        this.powerRatio = powerRatio;
        this.delayTicks = delayTicks;
        this.targets.addAll(targets);
        this.noPhysics = true;
        this.entityData.set(RADIUS, radius);
        this.entityData.set(POWER, powerRatio);
        this.entityData.set(DELAY, delayTicks);
        this.entityData.set(SEED, level.getRandom().nextInt());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 10.0f);
        builder.define(POWER, 1.0f);
        builder.define(DELAY, 40);
        builder.define(SEED, 0);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public float getPowerRatio() {
        return this.entityData.get(POWER);
    }

    public int getDelayTicks() {
        return this.entityData.get(DELAY);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Radius", getRadius());
        output.putFloat("PowerRatio", this.powerRatio);
        output.putInt("DelayTicks", this.delayTicks);
        output.putInt("Elapsed", this.elapsed);
        output.putInt("Seed", getSeed());
        output.putDouble("CenterX", this.centerPos.x);
        output.putDouble("CenterY", this.centerPos.y);
        output.putDouble("CenterZ", this.centerPos.z);
        Entity owner = this.getOwner();
        output.putString("Owner", owner != null ? owner.getUUID().toString() : "");
        List<UUID> ids = new ArrayList<>();
        for (LivingEntity target : this.targets) ids.add(target.getUUID());
        ids.addAll(this.pendingTargetIds);
        output.putInt("TargetCount", ids.size());
        for (int i = 0; i < ids.size(); i++) {
            output.putString("Target" + i, ids.get(i).toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        this.powerRatio = input.getFloatOr("PowerRatio", 1.0f);
        this.delayTicks = input.getIntOr("DelayTicks", 40);
        this.elapsed = input.getIntOr("Elapsed", 0);
        this.entityData.set(SEED, input.getIntOr("Seed", 0));
        this.centerPos = new Vec3(
            input.getDoubleOr("CenterX", this.getX()),
            input.getDoubleOr("CenterY", this.getY()),
            input.getDoubleOr("CenterZ", this.getZ()));
        this.entityData.set(RADIUS, input.getFloatOr("Radius", (float) (6.0 + this.powerRatio * 12.0)));
        this.entityData.set(POWER, this.powerRatio);
        this.entityData.set(DELAY, this.delayTicks);
        int count = input.getIntOr("TargetCount", 0);
        this.pendingTargetIds.clear();
        for (int i = 0; i < count; i++) {
            String s = input.getStringOr("Target" + i, "");
            if (!s.isEmpty()) this.pendingTargetIds.add(UUID.fromString(s));
        }
        String ownerId = input.getStringOr("Owner", "");
        if (!ownerId.isEmpty()) {
            this.pendingOwnerId = UUID.fromString(ownerId);
        }
        this.resolved = false;
    }

    private UUID pendingOwnerId = null;

    /** After a reload the entity references are gone; rebuild them by UUID. */
    private void resolveReferences(ServerLevel level) {
        this.resolved = true;
        if (this.pendingOwnerId != null && this.getOwner() == null) {
            Entity e = level.getEntity(this.pendingOwnerId);
            if (e != null) this.setOwner(e);
        }
        this.pendingOwnerId = null;
        for (UUID id : this.pendingTargetIds) {
            Entity e = level.getEntity(id);
            if (e instanceof LivingEntity living && living.isAlive() && !this.targets.contains(living)) {
                this.targets.add(living);
            }
        }
        this.pendingTargetIds.clear();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) return;
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        if (!this.resolved) resolveReferences(serverLevel);
        this.elapsed++;

        // Countdown particles: violet void smoke hangs over the contracting
        // telegraph ring while the launch counts down. The ring radius now
        // tracks the real slam radius instead of a hardcoded 5.0.
        if (this.elapsed % 3 == 0) {
            float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
            if (density > 0f) {
                float progress = Math.min(1.0f, this.elapsed / (float) this.delayTicks);
                double ringRadius = getRadius() * (1.0 - progress * 0.65);
                serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.VOID_SMOKE,
                    this.getX(), this.getY() + 0.3, this.getZ(),
                    Math.max(1, Math.round(5f * density)),
                    ringRadius * 0.7, 0.3, ringRadius * 0.7, 0.01);
            }
        }

        if (this.elapsed >= this.delayTicks) {
            launch(serverLevel);
            this.discard();
        }
    }

    private void launch(ServerLevel serverLevel) {
        Entity owner = this.getOwner();
        for (LivingEntity target : targets) {
            if (target != null && target.isAlive()) {
                // Same launch numbers as before (horizontal 1.2 plus 1.8 per
                // power, up 1.0 plus 0.8 per power), now run through the shared
                // fling so boss class targets barely move like everywhere else.
                com.dragonblockarcanedba.util.VortexPhysics.fling(target, this.centerPos,
                    1.2 + powerRatio * 1.8, 1.0 + powerRatio * 0.8);

                // MC 26.2 Physics: gravitational launch bounce and reduced air
                // drag, now TIMED at 200 ticks. They used to be applied with no
                // removal anywhere, so the change was permanent.
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyTimedModifier(
                    target,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("delayed_launch_bounce"),
                    0.90,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE,
                    200
                );
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyTimedModifier(
                    target,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("delayed_launch_drag"),
                    -0.50,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    200
                );
            }
        }

        // The launch moment used to be silent with no burst at all. Debris,
        // a gold ground burst, impact shake and sound mark the payoff now.
        com.dragonblockarcanedba.util.DebrisSpawner.spawnField(serverLevel,
            owner instanceof LivingEntity livingOwner ? livingOwner : null,
            this.centerPos, 16 + (int) (powerRatio * 16), getRadius() * 0.7, 3.5, 18);
        com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel, this.centerPos, getRadius() * 0.5f, 0x7A1FD6);
        com.dragonblockarcanedba.fx.DbaImpactFx.medium(serverLevel, this.centerPos.x, this.centerPos.y, this.centerPos.z);
        serverLevel.playSound(null, this.centerPos.x, this.centerPos.y, this.centerPos.z,
            com.dragonblockarcanedba.sound.DbaSounds.RELEASE_BLAST, net.minecraft.sounds.SoundSource.HOSTILE, 1.2f, 0.8f);
    }
}
