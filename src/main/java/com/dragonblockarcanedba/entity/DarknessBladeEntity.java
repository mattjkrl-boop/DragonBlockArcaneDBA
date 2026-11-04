package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.util.GroundScar;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Darkness Blade Entity: Giant falling dark energy blade during Abyssal Eclipse execution.
 * Falls rapidly from the sky and detonates upon impacting the ground/enemies.
 */
public class DarknessBladeEntity extends Projectile {
    // Matches the only call site: DaburaSwordItem passes 350 for eclipse blades.
    private float damage = 350.0f;

    public DarknessBladeEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DarknessBladeEntity(Level level, LivingEntity owner, Vec3 spawnPos, float damage) {
        super(DbaEntities.DARKNESS_BLADE, level);
        this.setOwner(owner);
        this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        this.damage = damage;
        this.noPhysics = true;
        this.setDeltaMovement(0, -1.8, 0); // Fast downward plunge
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 movement = this.getDeltaMovement();
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Falling trail: void smoke and sparks stream off the blade
            // every 2 ticks as it plunges.
            if (this.tickCount % 2 == 0) {
                serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.VOID_SMOKE,
                    this.getX(), this.getY() + 2.0, this.getZ(), 3, 0.4, 1.2, 0.4, 0.02);
                serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                    this.getX(), this.getY() + 0.5, this.getZ(), 1, 0.3, 0.5, 0.3, 0.05);
            }

            // Impact check: a solid block at the blade or directly under it,
            // or the 40 tick failsafe. onGround() is not used: this entity has
            // noPhysics set, so it never collides and onGround never turns true.
            BlockPos pos = this.blockPosition();
            BlockPos groundPos = !serverLevel.getBlockState(pos).isAir() ? pos
                : !serverLevel.getBlockState(pos.below()).isAir() ? pos.below()
                : null;
            if (groundPos != null || this.tickCount > 40) {
                // Impact detonation! Bespoke pass: the splash uses the
                // mod's darkness type with this blade as the direct entity.
                LivingEntity owner = (LivingEntity) this.getOwner();
                DamageSource damageSource = com.dragonblockarcanedba.util.DbaDamage.darkness(this, owner);

                AABB splashBox = this.getBoundingBox().inflate(4.0, 2.0, 4.0);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, splashBox, e -> e.isAlive() && e != owner);

                for (LivingEntity t : targets) {
                    t.hurtServer(serverLevel, damageSource, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (this.damage)));
                }

                // Physical 3D Void Shatter Shockwave & Ground Fissures
                DarknessShatterEntity shatter = new DarknessShatterEntity(
                    serverLevel, owner, this.position().add(0, 0.05, 0), 2.8f
                );
                serverLevel.addFreshEntity(shatter);

                // World interaction: the impact scars the exact block it landed
                // on with temporary obsidian, and kicks up a dust burst.
                if (groundPos != null) {
                    GroundScar.scarDisk(serverLevel, groundPos, 2,
                        Blocks.OBSIDIAN.defaultBlockState(), 120, 0.6f, serverLevel.getRandom());
                }
                LayeredParticles.emitGroundBurst(serverLevel, this.position(), 2.8f, 0xA605F2);

                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), com.dragonblockarcanedba.sound.DbaSounds.BOOM_BIG, SoundSource.PLAYERS, 1.5f, 0.6f);

                this.discard();
            }
        }
    }
}
