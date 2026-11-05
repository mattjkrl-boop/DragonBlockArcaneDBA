package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Swarm host (Phase 6): planted at the deploy point, it owns the
 * shard entities and the shared health budget they spend. It drains
 * the owner's Ki per minute; when the drain cannot be paid the swarm
 * starves. Target assignment (marked target first, then nearest
 * enemies, then projectile intercepts) runs here so shards stay dumb
 * steering. The service watches endReason to pick the cooldown.
 */
public class SwarmHostEntity extends CarrierEntity {

    public enum EndReason { NONE, RECALLED, STARVED, EXPIRED }

    /** Why this host ended, read by AbilityService's swarm watcher. Set before any self-discard. */
    public EndReason endReason = EndReason.NONE;

    private final List<SwarmShardEntity> shards = new ArrayList<>();
    private float budgetLeft;
    private UUID markedTarget;
    private boolean discardingShards;

    public SwarmHostEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void init(AbilityDef def, int seed, float power, UUID ownerUuid) {
        super.init(def, seed, power, ownerUuid);
        if (def.swarm() != null) {
            this.budgetLeft = (float) def.swarm().budget();
        }
    }

    public float budgetLeft() {
        return this.budgetLeft;
    }

    public void setMarked(UUID target) {
        this.markedTarget = target;
    }

    public UUID markedTarget() {
        return this.markedTarget;
    }

    /** Spawns the shard ring; called by AbilityService right after the host is added. */
    public void spawnShards(ServerLevel level) {
        AbilityDef def = def();
        if (def == null || def.swarm() == null) {
            return;
        }
        int count = def.swarm().count();
        for (int i = 0; i < count; i++) {
            SwarmShardEntity shard = new SwarmShardEntity(DbaEntities.ABILITY_SWARM_SHARD, level);
            double ang = (double) i / count * Math.PI * 2.0;
            shard.setPos(this.getX() + Math.cos(ang) * 2.3,
                    this.getY() + 1.6,
                    this.getZ() + Math.sin(ang) * 2.3);
            shard.init(this, def, getSeed() + i);
            level.addFreshEntity(shard);
            this.shards.add(shard);
        }
    }

    /**
     * Charges damage or spend against the shared budget. A shard whose
     * own accumulated total reaches its hull share (budget over count)
     * is spent and discards; an empty budget ends the whole swarm.
     */
    public void damageBudget(float amount, SwarmShardEntity from) {
        this.budgetLeft -= amount;
        AbilityDef def = def();
        if (def != null && def.swarm() != null && from != null
                && from.accumulated() >= def.swarm().budget() / Math.max(1, def.swarm().count())) {
            from.discard();
        }
        if (this.budgetLeft <= 0 && !this.isRemoved()) {
            discardAll();
        }
    }

    /** Ends the swarm now: every shard discards, then the host. */
    public void discardAll() {
        this.discardingShards = true;
        for (SwarmShardEntity shard : new ArrayList<>(this.shards)) {
            if (!shard.isRemoved()) {
                shard.discard();
            }
        }
        this.discardingShards = false;
        this.discard();
    }

    @Override
    protected void onExpire() {
        this.endReason = EndReason.EXPIRED;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.discardingShards) {
            this.discardingShards = true;
            for (SwarmShardEntity shard : new ArrayList<>(this.shards)) {
                if (!shard.isRemoved()) {
                    shard.discard();
                }
            }
            this.discardingShards = false;
        }
        super.remove(reason);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)
                || this.isRemoved()) {
            return;
        }
        AbilityDef def = def();
        if (def == null || def.swarm() == null) {
            return;
        }
        AbilityDef.SwarmParams swarm = def.swarm();
        this.shards.removeIf(shard -> shard.isRemoved());

        // Terrain spawn event runs on the first tick, like the cloud's.
        if (this.tickCount == 1 && def.terrain() != null && "spawn".equals(def.terrain().event())) {
            com.dragonblockarcanedba.ability.AbilityTerrain.eventAt(serverLevel,
                    this.blockPosition(), def.terrain());
        }

        Player owner = ownerPlayer();

        // Ki drain, charged once per second.
        if (this.tickCount % 20 == 0 && swarm.drainPercentPerMinute() > 0) {
            if (owner == null) {
                this.endReason = EndReason.STARVED;
                discardAll();
                return;
            }
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) owner;
            double price = PlayerStats.getMaxKi(owner) * swarm.drainPercentPerMinute() / 100.0 / 60.0;
            if (accessor.dba$getCurrentKi() < price) {
                this.endReason = EndReason.STARVED;
                discardAll();
                return;
            }
            accessor.dba$addKi(-price);
            accessor.dba$syncStats();
        }

        // Assignment every 10 ticks.
        if (this.tickCount % 10 != 0) {
            return;
        }
        Vec3 center = this.position();
        List<LivingEntity> enemies = new ArrayList<>(serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(center, center).inflate(swarm.assignRadius()),
                e -> e.isAlive() && e != owner));
        enemies.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(center)));
        LivingEntity marked = null;
        if (this.markedTarget != null) {
            for (LivingEntity enemy : enemies) {
                if (enemy.getUUID().equals(this.markedTarget)) {
                    marked = enemy;
                    break;
                }
            }
        }
        if (marked != null) {
            enemies.remove(marked);
            enemies.add(0, marked);
        }
        List<Projectile> projectiles = serverLevel.getEntitiesOfClass(Projectile.class,
                new AABB(center, center).inflate(swarm.interceptRadius()),
                pr -> !pr.isRemoved() && (owner == null || pr.getOwner() != owner));
        Set<Integer> targetedProjectiles = new HashSet<>();
        for (SwarmShardEntity shard : this.shards) {
            if (shard.busy()) {
                continue;
            }
            LivingEntity assignment = null;
            for (LivingEntity enemy : enemies) {
                int assigned = 0;
                for (SwarmShardEntity other : this.shards) {
                    // Count is approximated by state only; marked and
                    // nearest enemies simply attract idle shards first.
                    if (other != shard && other.state() == SwarmShardEntity.State.SEEK) {
                        assigned++;
                    }
                }
                if (assigned < 3 || enemy == marked) {
                    assignment = enemy;
                    break;
                }
            }
            if (assignment != null) {
                shard.assignSeek(assignment);
                continue;
            }
            for (Projectile projectile : projectiles) {
                if (projectile.isRemoved() || targetedProjectiles.contains(projectile.getId())) {
                    continue;
                }
                // One shard per projectile: claim it in this pass.
                targetedProjectiles.add(projectile.getId());
                shard.assignIntercept(projectile);
                break;
            }
        }
    }
}
