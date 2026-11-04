package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.DbaDamage;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Brave Slash Entity - Fired by the Brave Sword on combo crescents and heroic strikes.
 * Radiant golden-cyan energy crescent piercing lines of enemies.
 * Life is 24 ticks: at the item's 1.8 per tick launch speed the cut travels
 * about 43 blocks, in range of the combo logic that creates it, and it dies
 * on the first solid block it reaches instead of cutting through walls.
 */
public class BraveSlashEntity extends Projectile {
    public static final int MAX_LIFE = 24;

    private float damage = 600.0f;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public BraveSlashEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BraveSlashEntity(Level level, LivingEntity owner, float damage) {
        super(DbaEntities.BRAVE_SLASH, level);
        this.setOwner(owner);
        if (owner != null) {
            this.setPos(owner.getX(), owner.getY() + 1.0, owner.getZ());
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
        }
        this.damage = damage;
        this.noPhysics = true;
    }

    public int getMaxLifetime() {
        return MAX_LIFE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 movement = this.getDeltaMovement();
        double nextX = this.getX() + movement.x;
        double nextY = this.getY() + movement.y;
        double nextZ = this.getZ() + movement.z;

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // The cut is a physical wave: a solid block stops it with a burst
            // instead of letting it hit targets hiding behind cover.
            BlockPos nextPos = BlockPos.containing(nextX, nextY, nextZ);
            BlockState nextState = serverLevel.getBlockState(nextPos);
            if (!nextState.isAir() && !nextState.getCollisionShape(serverLevel, nextPos).isEmpty()) {
                serverLevel.sendParticles(DbaParticles.SPARK, nextX, nextY, nextZ, 12, 0.35, 0.35, 0.35, 0.12);
                serverLevel.sendParticles(LayeredParticles.dust(0xFFD700, 1.3f), nextX, nextY, nextZ, 8, 0.3, 0.3, 0.3, 0.06);
                this.discard();
                return;
            }
        }
        this.setPos(nextX, nextY, nextZ);

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Entity owner = this.getOwner();
            // Bespoke damage: the cut itself is the direct entity, the owner
            // the attacker, typed as valor instead of the vanilla spread.
            DamageSource damageSource = DbaDamage.valor(this, owner);

            double width = 3.0;
            double height = 1.2;
            AABB hitbox = this.getBoundingBox().inflate(width, height, width);

            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, hitbox,
                e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
            );

            for (LivingEntity target : targets) {
                hitEntityIds.add(target.getId());

                target.hurtServer(serverLevel, damageSource, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (this.damage)));

                if (owner instanceof LivingEntity livingOwner) {
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 25, 0, false, false, false
                    ), livingOwner);
                }

                // Knockback
                Vec3 knockback = movement.normalize().scale(1.4);
                target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.25, knockback.z));
                com.dragonblockarcanedba.util.DbaCompat.markHurt(target);

                // Golden critical sparks
                serverLevel.sendParticles(DbaParticles.CRIT_SPARK, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.3, 0.5, 0.3, 0.15);
            }

            // Layered wake (standard point 1): a bright mote core on the flight
            // line, gold dust as the mid band, cyan wisps shedding off the edges,
            // all breathing with the same envelope the renderer fades by.
            float env = FxEnvelope.envelope(this.tickCount, MAX_LIFE, 0.2f, 0.35f);
            float density = DbaEffectsConfig.effectDensity();
            if (env > 0f && density > 0f) {
                int core = Math.max(1, Math.round(2f * density * env));
                serverLevel.sendParticles(DbaParticles.KI_MOTE, this.getX(), this.getY(), this.getZ(),
                    core, 0.12, 0.12, 0.12, 0.01);
                int mid = Math.max(1, Math.round(2f * density * env));
                serverLevel.sendParticles(LayeredParticles.dust(0xFFD700, 1.25f), this.getX(), this.getY(), this.getZ(),
                    mid, 0.45, 0.3, 0.45, 0.03);
                int outer = Math.max(1, Math.round(1.5f * density * env));
                serverLevel.sendParticles(LayeredParticles.dust(0x00F2FF, 1.5f), this.getX(), this.getY(), this.getZ(),
                    outer, 0.8, 0.45, 0.8, 0.02);
            }

            if (this.tickCount > MAX_LIFE) {
                this.discard();
            }
        }
    }
}
