package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import com.dragonblockarcanedba.entity.ITrackedSwarmEntity;
import com.dragonblockarcanedba.util.SwarmHelper;


public class TridentShardEntity extends Projectile implements ITrackedSwarmEntity {
    private int shardIndex = 0;
    private boolean recalling = false;
    private LivingEntity target = null;
    private int lifeTime = 0;
    private float health = 15000.0f; // 150 * 100 damage

    public TridentShardEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public TridentShardEntity(Level level, LivingEntity owner, int index) {
        super(DbaEntities.TRIDENT_SHARD, level);
        this.setOwner(owner);
        this.shardIndex = index;
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No specific synched data needed right now
    }

    public void recall() {
        this.recalling = true;
    }

    public boolean isRecalling() {
        return this.recalling;
    }

    @Override
    public int getSwarmIndex() {
        return this.shardIndex;
    }

    @Override
    public float getSwarmHealth() {
        return this.health;
    }

    @Override
    public void setSwarmHealth(float health) {
        this.health = health;
    }

    private void updateHealthState() {
        if (this.level() instanceof ServerLevel && this.getOwner() instanceof Player player) {
            SwarmHelper.updateSwarmHealth(player, com.dragonblockarcanedba.item.DevilTridentItem.class, this.shardIndex, this.health);
        }
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    public void intercept(Projectile proj) {
        proj.discard();
        this.health -= 50.0f; // Arbitrary damage from projectile
        this.updateHealthState();
        if (this.health <= 0) {
            this.discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!this.isRemoved()) { // Changed from isInvulnerableTo because the shard should be damageable
            this.health -= amount;
            this.updateHealthState();
            if (this.health <= 0) {
                this.discard();
            }
            return true;
        }
        return false;
    }

    public int getOwnerId() {
        Entity owner = this.getOwner();
        return owner != null ? owner.getId() : -1;
    }

    @Override
    public void tick() {
        super.tick();
        this.lifeTime++;

        if (this.level().isClientSide()) return;

        Entity owner = this.getOwner();
        if (owner == null) {
            this.discard(); // Despawn immediately if chunk loaded but owner not online/around
            return;
        }
        if (!owner.isAlive() || this.distanceToSqr(owner) > 64 * 64) {
            this.discard(); // Despawn if owner dead or far away
            return;
        }

        if (recalling) {
            Vec3 dir = owner.position().add(0, owner.getBbHeight() / 2, 0).subtract(this.position());
            if (dir.lengthSqr() < 1.0) {
                this.discard();
            } else {
                this.setPos(this.getX() + dir.normalize().x, this.getY() + dir.normalize().y, this.getZ() + dir.normalize().z);
            }
            return;
        }

        // Note: AI behavior is managed by DevilTridentItem.manageShardSwarm, which sets `target`

        if (target != null && target.isAlive()) {
            Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
            
            // Move towards target smoothly but fast
            Vec3 currentPos = this.position();
            Vec3 dir = targetPos.subtract(currentPos);
            if (dir.lengthSqr() > 0.1) {
                Vec3 move = dir.normalize().scale(1.5); // Fast attack speed
                this.setPos(currentPos.x + move.x, currentPos.y + move.y, currentPos.z + move.z);
            }

            // Damage if close
            if (this.distanceToSqr(target) < 4.0) {
                target.hurtServer((ServerLevel) this.level(), this.level().damageSources().magic(), 100.0f);
                target.addEffect(new MobEffectInstance(DbaEffects.DEVILS_HANDS_HOLDER, 60, 0, false, false));
                
                // Recoil
                this.health -= 100.0f;
                this.updateHealthState();
                if (this.health <= 0) {
                    this.discard();
                }
            }
        } else {
            // Orbit owner with zero delay
            double orbitRadius = 2.0;
            double speed = this.lifeTime * 0.2;
            double angle = speed + (shardIndex * (Math.PI * 2 / 10.0));
            
            // Creates an angled, vertical orbit
            double offsetX = Math.cos(angle) * orbitRadius;
            double offsetY = Math.sin(angle) * orbitRadius; // Vertical movement
            double offsetZ = Math.sin(angle) * orbitRadius; // Angled depth

            this.setPos(owner.getX() + offsetX, owner.getY() + owner.getBbHeight() / 2 + offsetY, owner.getZ() + offsetZ);
        }
    }
}
