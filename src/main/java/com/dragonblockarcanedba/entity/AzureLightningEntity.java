package com.dragonblockarcanedba.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Azure Lightning Entity — Electric Cyan Dragon Lightning.
 */
public class AzureLightningEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> IS_RARE = SynchedEntityData.defineId(AzureLightningEntity.class, EntityDataSerializers.BOOLEAN);

    private int life = 12;
    private float damage = 400.0f;

    public AzureLightningEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public AzureLightningEntity(Level level, LivingEntity owner, double x, double y, double z, float damage) {
        super(DbaEntities.AZURE_LIGHTNING, level);
        this.setOwner(owner);
        this.setPos(x, y, z);
        this.damage = damage;
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IS_RARE, false);
    }

    public boolean isRare() {
        return this.entityData.get(IS_RARE);
    }

    public void setRare(boolean rare) {
        this.entityData.set(IS_RARE, rare);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if (this.life == 12) {
                // Strike damage on frame 1
                AABB aoe = new AABB(this.getX() - 3.5, this.getY() - 2, this.getZ() - 3.5,
                                    this.getX() + 3.5, this.getY() + 4, this.getZ() + 3.5);
                List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, aoe, e -> e.isAlive() && e != this.getOwner());
                for (LivingEntity victim : victims) {
                    net.minecraft.world.damagesource.DamageSource boltSource = this.getOwner() instanceof LivingEntity owner
                        ? serverLevel.damageSources().indirectMagic(this, owner)
                        : serverLevel.damageSources().lightningBolt();
                    victim.hurtServer(serverLevel, boltSource, this.damage);
                    victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.4, 0));
                    victim.hurtMarked = true;
                }

                // Extinguish / water / electricity spark effect
                BlockPos groundPos = BlockPos.containing(this.position());
                if (serverLevel.getBlockState(groundPos).isAir()) {
                    serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        this.getX(), this.getY() + 0.1, this.getZ(),
                        20, 1.5, 0.2, 1.5, 0.1
                    );
                }
            }

            this.life--;
            if (this.life <= 0) {
                this.discard();
            }
        }
    }
}
