package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Hollow Afterimage Entity — Ghostly translucent player clone left behind when passing through enemies.
 */
public class HollowAfterimageEntity extends Projectile {
    private static final EntityDataAccessor<Float> SYNC_YAW = SynchedEntityData.defineId(HollowAfterimageEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SYNC_PITCH = SynchedEntityData.defineId(HollowAfterimageEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> OWNER_UUID = SynchedEntityData.defineId(HollowAfterimageEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> SKIN_COLOR = SynchedEntityData.defineId(HollowAfterimageEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HAIR_COLOR = SynchedEntityData.defineId(HollowAfterimageEntity.class, EntityDataSerializers.INT);

    public HollowAfterimageEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public HollowAfterimageEntity(Level level, Player player) {
        super(DbaEntities.HOLLOW_AFTERIMAGE, level);
        this.setOwner(player);
        this.setPos(player.getX(), player.getY(), player.getZ());
        this.setYRot(player.getYRot());
        this.setXRot(player.getXRot());
        this.noPhysics = true;

        this.entityData.set(SYNC_YAW, player.getYRot());
        this.entityData.set(SYNC_PITCH, player.getXRot());
        this.entityData.set(OWNER_UUID, player.getUUID().toString());

        if (player instanceof PlayerStatsAccessor accessor) {
            this.entityData.set(SKIN_COLOR, parseHexColor(accessor.dba$getSkinColor(), 0xFF8CC8FF));
            this.entityData.set(HAIR_COLOR, parseHexColor(accessor.dba$getHairColor(), 0xFF1EB4FF));
        }
    }

    private static int parseHexColor(String hex, int defaultColor) {
        if (hex == null || hex.isEmpty()) return defaultColor;
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            return 0xFF000000 | Integer.parseInt(hex, 16);
        } catch (Exception e) {
            return defaultColor;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SYNC_YAW, 0.0f);
        builder.define(SYNC_PITCH, 0.0f);
        builder.define(OWNER_UUID, "");
        builder.define(SKIN_COLOR, 0xFF8CC8FF);
        builder.define(HAIR_COLOR, 0xFF1EB4FF);
    }

    public float getSyncYaw() {
        return this.entityData.get(SYNC_YAW);
    }

    public float getSyncPitch() {
        return this.entityData.get(SYNC_PITCH);
    }

    public String getOwnerUuid() {
        return this.entityData.get(OWNER_UUID);
    }

    public int getSkinColor() {
        return this.entityData.get(SKIN_COLOR);
    }

    public int getHairColor() {
        return this.entityData.get(HAIR_COLOR);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Apply Dark Faded to all nearby enemies within 4 blocks
            if (this.tickCount % 5 == 0) {
                AABB auraBox = this.getBoundingBox().inflate(4.0);
                List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(LivingEntity.class, auraBox, e -> e.isAlive() && e != this.getOwner());
                for (LivingEntity enemy : enemies) {
                    enemy.addEffect(new MobEffectInstance(DbaEffects.DARK_FADED_HOLDER, 60, 0, false, true));
                }
            }

            // Dark aura particles
            if (this.tickCount % 3 == 0) {
                double px = this.getX() + (serverLevel.getRandom().nextDouble() - 0.5) * 0.8;
                double py = this.getY() + serverLevel.getRandom().nextDouble() * 1.8;
                double pz = this.getZ() + (serverLevel.getRandom().nextDouble() - 0.5) * 0.8;

                serverLevel.sendParticles(
                    new DustParticleOptions(0x110022, 1.3F),
                    px, py, pz,
                    1, 0.0, 0.03, 0.0, 0.01
                );
                serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    px, py, pz,
                    1, 0.0, 0.02, 0.0, 0.01
                );
            }

            // Discard after 5 seconds (100 ticks)
            if (this.tickCount >= 100) {
                this.discard();
            }
        }
    }
}
