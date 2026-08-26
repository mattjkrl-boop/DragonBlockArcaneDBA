package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Evil Spear Projectile Entity — Fired by Evil Spear (Evil Impale).
 * High-velocity spectral crimson spear that pierces entities, applies Marked by Evil, and pulls targets inward.
 */
public class EvilSpearProjectileEntity extends Projectile {
    private float damage = 450.0f;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public EvilSpearProjectileEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public EvilSpearProjectileEntity(Level level, LivingEntity owner, float damage) {
        super(DbaEntities.EVIL_SPEAR_PROJECTILE, level);
        this.setOwner(owner);
        if (owner != null) {
            this.setPos(owner.getX(), owner.getEyeY() - 0.2, owner.getZ());
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
        }
        this.damage = damage;
        this.noPhysics = true;
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
        this.setPos(nextX, nextY, nextZ);

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Entity owner = this.getOwner();
            DamageSource damageSource = owner instanceof net.minecraft.world.entity.player.Player playerOwner
                ? serverLevel.damageSources().playerAttack(playerOwner)
                : (owner instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobProjectile(this, livingOwner) : serverLevel.damageSources().generic());

            AABB hitbox = this.getBoundingBox().inflate(1.2, 1.2, 1.2);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, hitbox,
                e -> e.isAlive() && e != owner && !hitEntityIds.contains(e.getId())
            );

            for (LivingEntity target : targets) {
                hitEntityIds.add(target.getId());

                // Calculate damage with Marked by Evil bonus if already marked (+30%)
                float finalDamage = this.damage;
                if (target.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.MARKED_BY_EVIL_HOLDER)) {
                    finalDamage *= 1.3f;
                }

                // Deal damage
                target.hurtServer(serverLevel, damageSource, finalDamage);

                // Apply Marked by Evil for 8 seconds (160 ticks)
                target.addEffect(new MobEffectInstance(
                    com.dragonblockarcanedba.effect.DbaEffects.MARKED_BY_EVIL_HOLDER, 160, 0, false, true
                ), owner);

                // MC 26.2 Physics: Spear impalement pin friction & drag
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    target,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("evil_spear_hit_friction"),
                    3.0,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    target,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("evil_spear_hit_drag"),
                    2.0,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );

                // Apply cinematic tracking holder
                if (owner instanceof LivingEntity livingOwner) {
                    target.addEffect(new MobEffectInstance(
                        com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 25, 0, false, false, false
                    ), livingOwner);
                }

                // Tweak B: Inward vacuum pull toward the spear's impact path
                Vec3 toSpear = this.position().subtract(target.position()).normalize().scale(0.8);
                target.setDeltaMovement(target.getDeltaMovement().add(toSpear.x, 0.2, toSpear.z));
                target.hurtMarked = true;

                // Impact crimson shockwave pushing nearby enemies
                AABB shockBox = target.getBoundingBox().inflate(3.5);
                List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, shockBox, e -> e != target && e != owner && e.isAlive()
                );
                for (LivingEntity n : nearby) {
                    Vec3 push = n.position().subtract(target.position()).normalize().scale(1.2);
                    n.setDeltaMovement(n.getDeltaMovement().add(push.x, 0.2, push.z));
                    n.hurtMarked = true;
                }

                // Spawn physical 3D impact shatter & spikes
                HellHuntImpactEntity impact = new HellHuntImpactEntity(
                    serverLevel, owner instanceof LivingEntity l ? l : null, target.position().add(0, 0.05, 0), 2.6f, false
                );
                serverLevel.addFreshEntity(impact);

                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.8f, 0.6f);
            }

            if (this.tickCount > 40) {
                this.discard();
            }
        }
    }
}
