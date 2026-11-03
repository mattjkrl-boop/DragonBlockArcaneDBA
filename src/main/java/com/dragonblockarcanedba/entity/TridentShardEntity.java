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
import com.dragonblockarcanedba.util.SwarmHelper;

public class TridentShardEntity extends Projectile implements ITrackedSwarmEntity {
    private int shardIndex = 0;
    private boolean recalling = false;
    /** Manual right-click recall. The wipe cooldown is not applied again when these arrive. */
    private boolean manualRecall = false;
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
        // Projectile base handles position and rotation syncing
    }

    public void recall() {
        this.recalling = true;
        this.manualRecall = true;
    }

    /**
     * Health is gone. Fly back into the trident and disappear only on arrival.
     * Does not start the swarm cooldown; that waits until every shard is home.
     */
    public void returnHome() {
        if (this.manualRecall || this.isRemoved()) {
            return;
        }
        this.health = 0.0f;
        this.updateHealthState();
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
        this.health -= 50.0f;
        this.updateHealthState();
        if (this.health <= 0) {
            this.returnHome();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!this.isRemoved()) {
            this.health -= amount;
            this.updateHealthState();
            if (this.health <= 0) {
                this.returnHome();
            }
            return true;
        }
        return false;
    }

    public int getOwnerId() {
        Entity owner = this.getOwner();
        return owner != null ? owner.getId() : -1;
    }

    /** The trident in the owner's hand, main hand or offhand. */
    private Vec3 handPoint(Entity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-4) {
            right = new Vec3(1, 0, 0);
        } else {
            right = right.normalize();
        }
        double side = 0.46;
        if (owner instanceof Player player
            && !(player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.DevilTridentItem)
            && player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.DevilTridentItem) {
            side = -0.46;
        }
        return owner.getEyePosition().add(right.scale(side)).add(0.0, -0.48, 0.0).add(look.scale(0.32));
    }

    /** Removed on arrival. The wipe cooldown starts only when no shard is still out. */
    private void finishReturn() {
        boolean manual = this.manualRecall;
        Entity owner = this.getOwner();
        final int ownerId = owner == null ? -1 : owner.getId();
        Level level = this.level();
        this.discard();
        if (manual || !(owner instanceof Player player) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean anyLeft = false;
        for (TridentShardEntity other : serverLevel.getEntitiesOfClass(
            TridentShardEntity.class,
            player.getBoundingBox().inflate(128.0),
            e -> e.getOwnerId() == ownerId && e != this && !e.isRemoved())) {
            anyLeft = true;
            break;
        }
        if (!anyLeft) {
            com.dragonblockarcanedba.item.DevilTridentItem.onSwarmArrivedHome(player);
        }
    }

    private void flyHome(Entity owner) {
        Vec3 dir = this.handPoint(owner).subtract(this.position());
        double dist = dir.length();
        if (dist < 0.42) {
            this.finishReturn();
            return;
        }
        double speed = Math.min(1.8, Math.max(0.35, dist * 0.48));
        Vec3 step = dir.scale(speed / dist);
        Vec3 bow = dir.cross(new Vec3(0, 1, 0));
        if (bow.lengthSqr() > 1.0E-4 && dist > 1.0) {
            double curl = Math.sin(this.shardIndex * 1.3) * 0.16 * Math.min(1.0, (dist - 1.0) / 5.0);
            step = step.add(bow.normalize().scale(curl));
        }
        this.updateHeading(step);
        this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
    }

    private void updateHeading(Vec3 motion) {
        if (motion.lengthSqr() > 0.0001) {
            double horiz = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            float yRot = (float) (Math.atan2(motion.z, motion.x) * (180.0 / Math.PI)) - 90.0f;
            float xRot = (float) -(Math.atan2(motion.y, horiz) * (180.0 / Math.PI));
            this.setYRot(yRot);
            this.setXRot(xRot);
        }
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

        // Theme particles (redesign T5): each shard sheds a slow ember so the
        // orbiting swarm reads as burning blades, not silent knives.
        if (this.tickCount % 3 == 0 && this.level() instanceof ServerLevel serverLevel) {
            float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
            if (density > 0f) {
                serverLevel.sendParticles(
                    com.dragonblockarcanedba.particle.DbaParticles.EMBER,
                    this.getX(), this.getY(), this.getZ(),
                    Math.max(1, Math.round(1f * density)),
                    0.08, 0.08, 0.08, 0.01);
            }
        }
        if (recalling) {
            if (!owner.isAlive()) {
                this.finishReturn();
                return;
            }
            this.flyHome(owner);
            return;
        }
        if (!owner.isAlive() || this.distanceToSqr(owner) > 64 * 64) {
            this.discard();
            return;
        }

        // AI behavior is managed by DevilTridentItem.manageShardSwarm, which sets `target`
        if (target != null && target.isAlive()) {
            Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
            
            // Move towards target smoothly but fast
            Vec3 currentPos = this.position();
            Vec3 dir = targetPos.subtract(currentPos);
            if (dir.lengthSqr() > 0.1) {
                Vec3 move = dir.normalize().scale(2.5); // Very fast attack speed
                updateHeading(move);
                this.setPos(currentPos.x + move.x, currentPos.y + move.y, currentPos.z + move.z);
            }

            // Damage if close
            if (this.distanceToSqr(target) < 4.0) {
                net.minecraft.world.damagesource.DamageSource shardSource = this.getOwner() instanceof LivingEntity livingOwner
                    ? this.level().damageSources().mobProjectile(this, livingOwner)
                    : this.level().damageSources().magic();
                // Shard damage scales with the owner's Strength (100 + Strength x 0.5).
                float dealt = this.getOwner() instanceof Player ownerPlayer
                    ? com.dragonblockarcanedba.item.DevilTridentItem.shardDamage(ownerPlayer)
                    : 100.0f;
                target.hurtServer((ServerLevel) this.level(), shardSource, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(dealt));
                target.addEffect(new MobEffectInstance(DbaEffects.DEVILS_HANDS_HOLDER, 60, 0, false, false), this.getOwner());
                this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    net.minecraft.sounds.SoundEvents.TRIDENT_HIT, net.minecraft.sounds.SoundSource.HOSTILE, 0.7f, 1.1f);

                // Recoil / degradation: the swarm health budget is spent in damage dealt.
                this.health -= dealt;
                this.updateHealthState();
                if (this.health <= 0) {
                    this.returnHome();
                }
            }
        } else {
            // Orbit owner with zero delay
            double orbitRadius = 2.2;
            double speed = this.lifeTime * 0.4;
            double angle = speed + (shardIndex * (Math.PI * 2 / 10.0));
            
            double offsetX = Math.cos(angle) * orbitRadius;
            double offsetY = Math.sin(angle * 1.5) * 0.75 + 0.3; // Gentle vertical wave
            double offsetZ = Math.sin(angle) * orbitRadius;

            Vec3 nextPos = new Vec3(owner.getX() + offsetX, owner.getY() + owner.getBbHeight() * 0.5 + offsetY, owner.getZ() + offsetZ);
            Vec3 motion = nextPos.subtract(this.position());
            updateHeading(motion);

            this.setPos(nextPos.x, nextPos.y, nextPos.z);
        }
    }
}
