package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.util.AtmosphereServer;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.dragonblockarcanedba.util.GroundScar;
import com.dragonblockarcanedba.util.LayeredParticles;
import com.dragonblockarcanedba.util.VortexPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Bansho Cyclone Entity: the fan's great cyclone, a standing storm with a full
 * lifecycle (SPECTACLE-STANDARD point 5). Cast by BanshoFanItem on a landed
 * melee hit, gated there by a per-player storm cooldown.
 *
 * Life is 100 ticks: a 20 percent build-up while the funnel grows and the
 * debris field lifts, a peak where layered particles, vortex forces, and
 * shredding all run at full strength, then a 35 percent dissipation where the
 * storm ejects everything it still holds, the debris rains back down, and the
 * funnel dissolves instead of vanishing. Damage lands only inside the active
 * window: slashDamage x 0.11 every 4 ticks, 16 ticks over a full stay, for
 * 176 percent of the cyclone figure in total.
 */
public class BanshoCycloneEntity extends Projectile {
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(BanshoCycloneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(BanshoCycloneEntity.class, EntityDataSerializers.INT);

    public static final int LIFE_TICKS = 100;
    public static final float BUILD_FRAC = 0.20f;
    public static final float DECAY_FRAC = 0.35f;
    private static final int EJECT_LEAD_TICKS = 18;
    private static final int DEBRIS_COUNT = 64;
    private static final int THEME_ARGB = 0x26FFA6;

    private float slashDamage = 200.0f;
    private boolean debrisSpawned = false;
    private boolean ejected = false;

    public BanshoCycloneEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BanshoCycloneEntity(Level level, LivingEntity owner, Vec3 pos, float damage) {
        super(DbaEntities.BANSHO_CYCLONE, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.slashDamage = damage;
        this.noPhysics = true;
        this.setScale(1.0f);
        this.entityData.set(LIFETIME, LIFE_TICKS);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SCALE, 1.0f);
        builder.define(LIFETIME, LIFE_TICKS);
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public void setScale(float scale) {
        this.entityData.set(SCALE, scale);
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    public float effectRadius() {
        return getScale() * 4.0f;
    }

    public float effectHeight() {
        return getScale() * 6.5f;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        float scale = getScale();
        double radius = scale * 4.0;
        double height = scale * 6.5;
        Vec3 center = this.position();
        int age = this.tickCount;
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
        float envelope = FxEnvelope.envelope(age, LIFE_TICKS, BUILD_FRAC, DECAY_FRAC);

        // Cast beat: dust bursts off the ground the storm stands on, and the
        // sky answers. The fan's storm drives real rain for its whole life.
        if (age == 1) {
            LayeredParticles.emitGroundBurst(serverLevel, center, (float) radius * 0.85f, THEME_ARGB);
            AtmosphereServer.begin(serverLevel, center, 24f, 0.45f, AtmosphereServer.Kind.RAIN, LIFE_TICKS);
        }

        // Debris field lifts as the funnel reaches full strength (point 2):
        // real ground blocks on helical orbits that rain down in the decay.
        if (!this.debrisSpawned && age >= Math.round(LIFE_TICKS * BUILD_FRAC)) {
            this.debrisSpawned = true;
            float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
            int count = Math.round(DEBRIS_COUNT * density);
            if (count > 0) {
                DebrisSpawner.spawnField(serverLevel, owner, center, count,
                    radius * 0.95, height * 0.85, 50);
            }
        }

        // Layered particle bands every tick (point 1), plus a falling wisp
        // drizzle while the storm dissipates (point 5).
        LayeredParticles.emitVortex(serverLevel, center, (float) radius, (float) height,
            age, LIFE_TICKS, THEME_ARGB, com.dragonblockarcanedba.particle.DbaParticles.WIND);
        if (age > LIFE_TICKS * (1f - DECAY_FRAC)) {
            LayeredParticles.emitRain(serverLevel, center, (float) radius, (float) height,
                age, LIFE_TICKS, com.dragonblockarcanedba.particle.DbaParticles.WIND);
        }

        boolean active = FxEnvelope.inActiveWindow(age, LIFE_TICKS, BUILD_FRAC, DECAY_FRAC);

        // Vortex forces (point 3) until the ejection beat: pull, swirl, and
        // core lift, weight scaled inside VortexPhysics, envelope ramped.
        if (!this.ejected) {
            VortexPhysics.applyVortex(serverLevel, center, radius, height, owner,
                0.30, 1.6, 0.55, envelope);
        }

        // Shredding inside the active window only, spread across it.
        if (active && age % 4 == 0) {
            AABB aoe = new AABB(center.x - radius, center.y - 0.5, center.z - radius,
                                center.x + radius, center.y + height, center.z + radius);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, aoe,
                e -> e.isAlive() && e != this.getOwner()
            );
            net.minecraft.world.damagesource.DamageSource source = this.getOwner() instanceof net.minecraft.world.entity.player.Player playerOwner
                ? serverLevel.damageSources().playerAttack(playerOwner)
                : (owner != null ? serverLevel.damageSources().mobAttack(owner) : serverLevel.damageSources().magic());
            for (LivingEntity target : targets) {
                target.hurtServer(serverLevel, source, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (this.slashDamage * 0.11f)));
                // Bleeding I (amplifier 0) for 5 seconds (100 ticks), refreshed per shred tick.
                target.addEffect(new MobEffectInstance(DbaEffects.BLEEDING_HOLDER, 100, 0, false, true), this.getOwner());
            }
        }

        // The storm scours the ground it grinds on (point 3, light touch):
        // grass strips to dirt, dirt roughens to coarse dirt, then heals.
        if (active && age % 10 == 0) {
            scourGround(serverLevel, center);
        }

        // Ejection beat (point 5): near the end the storm throws everything it
        // still holds outward and up instead of dropping it at discard.
        if (!this.ejected && age >= LIFE_TICKS - EJECT_LEAD_TICKS) {
            this.ejected = true;
            AABB aoe = new AABB(center.x - radius, center.y - 0.5, center.z - radius,
                                center.x + radius, center.y + height, center.z + radius);
            List<LivingEntity> held = serverLevel.getEntitiesOfClass(
                LivingEntity.class, aoe,
                e -> e.isAlive() && e != this.getOwner()
            );
            for (LivingEntity target : held) {
                VortexPhysics.fling(target, center, 1.15, 0.9);
            }
            serverLevel.playSound(null, center.x, center.y, center.z,
                SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.2f, 1.0f);
        }

        // Howling tempest sound.
        if (age == 1 || age % 12 == 0) {
            serverLevel.playSound(null, center.x, center.y, center.z,
                SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.4f, 1.1f);
            serverLevel.playSound(null, center.x, center.y, center.z,
                SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0f, 1.3f);
        }

        if (age >= LIFE_TICKS) {
            this.discard();
        }
    }

    /**
     * Wind scouring: only soft ground changes (grass to dirt, dirt and podzol
     * to coarse dirt), in a radius 3 disk under the funnel, restored by
     * GroundScar about 12 seconds after the last scour pass.
     */
    private void scourGround(ServerLevel level, Vec3 center) {
        int cx = (int) Math.floor(center.x);
        int cz = (int) Math.floor(center.z);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz > 9) continue;
                if (level.getRandom().nextFloat() > 0.6f) continue;
                int x = cx + dx;
                int z = cz + dz;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                m.set(x, y, z);
                BlockState state = level.getBlockState(m);
                BlockState scoured = null;
                if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM)) {
                    scoured = Blocks.DIRT.defaultBlockState();
                } else if (state.is(Blocks.DIRT) || state.is(Blocks.PODZOL)) {
                    scoured = Blocks.COARSE_DIRT.defaultBlockState();
                }
                if (scoured != null) {
                    GroundScar.scar(level, m.immutable(), scoured, 240);
                }
            }
        }
    }
}
