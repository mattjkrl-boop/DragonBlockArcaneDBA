package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.ability.AbilityDamage;
import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.ability.AbilityFxService;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * One swarm shard (Phase 6): a free entity on the projectile family
 * pattern (own synced ability id, seed, power) that orbits its host,
 * seeks assigned targets, intercepts enemy projectiles, and returns.
 * Movement is manual steering; there is no physics. Damage a shard
 * takes or spends is charged against the host's shared budget.
 */
public class SwarmShardEntity extends Entity {
    private static final EntityDataAccessor<String> ABILITY_ID =
            SynchedEntityData.defineId(SwarmShardEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> SEED =
            SynchedEntityData.defineId(SwarmShardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER =
            SynchedEntityData.defineId(SwarmShardEntity.class, EntityDataSerializers.FLOAT);

    public enum State { ORBIT, SEEK, INTERCEPT, RETURN }

    private SwarmHostEntity host;
    private AbilityDef cachedDef;
    private State state = State.ORBIT;
    private LivingEntity targetEntity;
    private Projectile targetProjectile;
    private float accumulated;
    private double orbitAngle;

    public SwarmShardEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ABILITY_ID, "");
        builder.define(SEED, 0);
        builder.define(POWER, 1.0f);
    }

    /** Server-side setup, called by the host right after spawning. */
    public void init(SwarmHostEntity host, AbilityDef def, int seed) {
        this.host = host;
        this.cachedDef = def;
        this.entityData.set(ABILITY_ID, def.id().toString());
        this.entityData.set(SEED, seed);
        this.entityData.set(POWER, host.getPower());
        this.orbitAngle = seed * 0.7;
    }

    public String getAbilityIdString() {
        return this.entityData.get(ABILITY_ID);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
    }

    public float getPower() {
        return this.entityData.get(POWER);
    }

    public AbilityDef def() {
        if (this.cachedDef != null) {
            return this.cachedDef;
        }
        String idStr = getAbilityIdString();
        if (idStr == null || idStr.isEmpty()) {
            return null;
        }
        this.cachedDef = com.dragonblockarcanedba.ability.AbilityRegistry.get(
                net.minecraft.resources.Identifier.tryParse(idStr));
        return this.cachedDef;
    }

    public State state() {
        return this.state;
    }

    public float accumulated() {
        return this.accumulated;
    }

    public boolean busy() {
        return this.state != State.ORBIT;
    }

    /** Host assignment: chase a living target. */
    public void assignSeek(LivingEntity target) {
        this.targetEntity = target;
        this.targetProjectile = null;
        this.state = State.SEEK;
    }

    /** Host assignment: destroy an enemy projectile. */
    public void assignIntercept(Projectile projectile) {
        this.targetProjectile = projectile;
        this.targetEntity = null;
        this.state = State.INTERCEPT;
    }

    private Vec3 orbitPoint() {
        Vec3 hostPos = this.host.position();
        return new Vec3(
                hostPos.x + Math.cos(this.orbitAngle) * 2.3,
                hostPos.y + 1.6 + Math.sin(this.tickCount * 0.1 + getSeed()) * 0.2,
                hostPos.z + Math.sin(this.orbitAngle) * 2.3);
    }

    private void steer(Vec3 goal, double speed, double lerp) {
        Vec3 to = goal.subtract(this.position());
        double dist = to.length();
        if (dist < 1.0e-4) {
            return;
        }
        Vec3 desired = to.scale(speed / dist);
        this.setDeltaMovement(this.getDeltaMovement().lerp(desired, lerp));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.host == null || this.host.isRemoved()) {
            this.discard();
            return;
        }
        AbilityDef def = def();
        switch (this.state) {
            case ORBIT -> {
                this.orbitAngle += 0.13;
                Vec3 goal = orbitPoint();
                steer(goal, 1.1, 0.3);
                if (this.position().distanceTo(goal) < 0.5) {
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.6));
                }
            }
            case SEEK -> {
                if (this.targetEntity == null || !this.targetEntity.isAlive()) {
                    this.state = State.RETURN;
                    break;
                }
                Vec3 goal = this.targetEntity.position()
                        .add(0, this.targetEntity.getBbHeight() * 0.6, 0);
                steer(goal, 1.7, 0.35);
                if (this.position().distanceTo(goal) < 1.5 && def != null && def.swarm() != null) {
                    Player owner = this.host.ownerPlayer();
                    float dmg = AbilityDamage.amountRaw(def.swarm().shardBase(),
                            def.swarm().shardStat(), def.swarm().shardPerStat(), owner, 1.0f);
                    if (dmg > 0) {
                        this.targetEntity.hurtServer(serverLevel,
                                AbilityDamage.source(def, this, owner), dmg);
                    }
                    AbilityFxService.send(this, def, AbilityDef.FxEvent.CARRIER_HIT,
                            this.position(), this.getDeltaMovement(), getSeed(), 1.0f, 1);
                    this.state = State.RETURN;
                }
            }
            case INTERCEPT -> {
                if (this.targetProjectile == null || this.targetProjectile.isRemoved()) {
                    this.state = State.RETURN;
                    break;
                }
                Vec3 goal = this.targetProjectile.position();
                steer(goal, 1.9, 0.35);
                if (this.position().distanceTo(goal) < 1.3) {
                    this.targetProjectile.discard();
                    this.host.damageBudget(25.0f, this);
                    this.state = State.RETURN;
                }
            }
            case RETURN -> {
                Vec3 goal = orbitPoint();
                steer(goal, 1.7, 0.3);
                if (this.position().distanceTo(goal) < 0.6) {
                    this.state = State.ORBIT;
                }
            }
        }
        this.setPos(this.position().add(this.getDeltaMovement()));
    }

    /** Shards are hurtable: damage taken is charged to the host budget. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        this.accumulated += amount;
        if (this.host != null && !this.host.isRemoved()) {
            this.host.damageBudget(amount, this);
        }
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putString("Ability", getAbilityIdString());
        output.putInt("Seed", getSeed());
        output.putFloat("Power", getPower());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.getString("Ability").ifPresent(v -> this.entityData.set(ABILITY_ID, v));
        this.entityData.set(SEED, input.getIntOr("Seed", 0));
        this.entityData.set(POWER, input.getFloatOr("Power", 1.0f));
        this.cachedDef = null;
    }
}
