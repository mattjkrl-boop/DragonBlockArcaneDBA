package com.dragonblockarcanedba.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Azure Lightning Entity: Electric Cyan Dragon Lightning.
 *
 * Spectacle pass: this entity is the telegraph and the damage, no longer an
 * instant invisible hit. For its telegraph ticks the renderer draws a ground
 * warning at exactly the strike radius; when the telegraph completes, a
 * shared BoltEntity (branched, seeded, azure or rare gold) strikes the point
 * and the damage lands in that same tick, inside the warned circle. Damage,
 * radius, telegraph length, rare flag, and the struck flag are all synced
 * and saved: a restored strike used to revert to a default 400 damage and
 * strike a second time.
 */
public class AzureLightningEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> IS_RARE = SynchedEntityData.defineId(AzureLightningEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(AzureLightningEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(AzureLightningEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TELEGRAPH_TICKS = SynchedEntityData.defineId(AzureLightningEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> SEED = SynchedEntityData.defineId(AzureLightningEntity.class, EntityDataSerializers.LONG);

    /** Ticks the strike glow lingers after the bolt lands. */
    public static final int AFTERGLOW_TICKS = 8;

    private boolean struck = false;
    /** True only when the struck state came back from a save, not from this session. */
    private boolean loadedStruck = false;

    public AzureLightningEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public AzureLightningEntity(Level level, LivingEntity owner, double x, double y, double z, float damage) {
        super(DbaEntities.AZURE_LIGHTNING, level);
        this.setOwner(owner);
        this.setPos(x, y, z);
        this.noPhysics = true;
        this.entityData.set(DAMAGE, damage);
        // Renderer shape seed: fixed at spawn, synced and saved.
        this.entityData.set(SEED, this.getUUID().getMostSignificantBits() ^ this.getUUID().getLeastSignificantBits());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IS_RARE, false);
        builder.define(DAMAGE, 400.0f);
        builder.define(RADIUS, 3.5f);
        builder.define(TELEGRAPH_TICKS, 12);
        builder.define(SEED, 0L);
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Damage", getDamage());
        output.putFloat("Radius", getRadius());
        output.putInt("TelegraphTicks", getTelegraphTicks());
        output.putBoolean("Rare", isRare());
        output.putBoolean("HasStruck", this.struck);
        output.putLong("RenderSeed", this.getRenderSeed());
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DAMAGE, input.getFloatOr("Damage", 400.0f));
        this.entityData.set(RADIUS, input.getFloatOr("Radius", 3.5f));
        this.entityData.set(TELEGRAPH_TICKS, input.getIntOr("TelegraphTicks", 12));
        this.entityData.set(IS_RARE, input.getBooleanOr("Rare", false));
        this.struck = input.getBooleanOr("HasStruck", false);
        this.entityData.set(SEED, input.getLongOr("RenderSeed", 0L));
        this.loadedStruck = this.struck;
    }

    public boolean isRare() {
        return this.entityData.get(IS_RARE);
    }

    public void setRare(boolean rare) {
        this.entityData.set(IS_RARE, rare);
        if (rare) {
            // Rare strikes warn a wider circle and hit harder; the telegraph
            // grows with the damage zone so the warning stays honest.
            this.entityData.set(RADIUS, 4.5f);
        }
    }

    public float getDamage() {
        return this.entityData.get(DAMAGE);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public int getTelegraphTicks() {
        return this.entityData.get(TELEGRAPH_TICKS);
    }

    public void setTelegraphTicks(int ticks) {
        this.entityData.set(TELEGRAPH_TICKS, Math.max(2, ticks));
    }

    public boolean hasStruck() {
        return this.struck;
    }

    public long getRenderSeed() {
        return this.entityData.get(SEED);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // A strike restored after it already landed has nothing left to do:
        // its bolt was shown before the unload, so it leaves quietly instead
        // of striking twice. A strike that lands this session stays for its
        // afterglow instead.
        if (this.loadedStruck) {
            this.discard();
            return;
        }

        if (this.tickCount >= getTelegraphTicks()) {
            strike(serverLevel);
            this.struck = true;
        }

        if (this.tickCount >= getTelegraphTicks() + AFTERGLOW_TICKS) {
            this.discard();
        }
    }

    private void strike(ServerLevel serverLevel) {
        LivingEntity livingOwner = this.getOwner() instanceof LivingEntity ? (LivingEntity) this.getOwner() : null;
        float radius = getRadius();
        Vec3 point = this.position();

        // The visible bolt: shared BoltEntity, branched from a fresh seed,
        // azure for a normal strike and pale gold for a rare one.
        int color = isRare() ? 0xFFFFE9A8 : 0xFF9FD8FF;
        BoltEntity bolt = new BoltEntity(serverLevel, point, serverLevel.getRandom().nextInt(),
            color, 44.0f, 9);
        if (livingOwner != null) {
            bolt.setOwner(livingOwner);
        }
        serverLevel.addFreshEntity(bolt);

        // Damage lands now, at the visual's peak tick, inside exactly the
        // circle the telegraph warned (radius across, 2 below to 6 above).
        AABB aoe = new AABB(this.getX() - radius, this.getY() - 2, this.getZ() - radius,
                            this.getX() + radius, this.getY() + 6, this.getZ() + radius);
        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, aoe,
            e -> e.isAlive() && e != this.getOwner());
        float dmg = getDamage() * (isRare() ? 1.25f : 1.0f);
        for (LivingEntity victim : victims) {
            double dx = victim.getX() - this.getX();
            double dz = victim.getZ() - this.getZ();
            if (dx * dx + dz * dz > radius * radius) continue;
            net.minecraft.world.damagesource.DamageSource boltSource = livingOwner != null
                ? com.dragonblockarcanedba.util.DbaDamage.tempest(this, livingOwner)
                : com.dragonblockarcanedba.util.DbaDamage.tempest(this, null);
            victim.hurtServer(serverLevel, boltSource,
                com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(dmg));
            victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.4, 0));
            com.dragonblockarcanedba.util.DbaCompat.markHurt(victim);
        }

        // Ground feedback: sparks always fire now (they used to be skipped
        // whenever the strike point sat inside a non air block), plus a dust
        // burst and a few chunks of the ground itself thrown up by the hit.
        serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
            this.getX(), this.getY() + 0.1, this.getZ(),
            20, 1.5, 0.2, 1.5, 0.1);
        com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel, point,
            radius * 0.6f, AzureStormEntity.AZURE_ARGB);
        com.dragonblockarcanedba.util.DebrisSpawner.spawnField(serverLevel, livingOwner, point,
            8, radius * 0.9, 3.0, 14);

        // Thunder lands with the bolt, not with the telegraph.
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.THUNDER_CRACK, SoundSource.WEATHER, 2.5f, 1.0f);
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.LIGHTNING_ZAP, SoundSource.WEATHER, 2.5f, 1.0f);
    }
}
