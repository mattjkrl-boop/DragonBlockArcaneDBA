package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.ability.AbilityDamage;
import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.ability.AbilityFeedbackService;
import com.dragonblockarcanedba.ability.AbilityFxService;
import com.dragonblockarcanedba.ability.AbilityRegistry;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Projectile carrier, in the KiBlastEntity pattern: a
 * ThrowableProjectile parameterized entirely by its ability
 * definition (speed at launch, gravity scale, direct damage, an
 * optional area burst on any hit, and a casing choice the renderer
 * reads). It carries the same synced contract as the other
 * carriers (ability id, seed, power) under its own accessors,
 * because it cannot share the CarrierEntity base class.
 */
public class ProjectileCarrierEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<String> ABILITY_ID =
            SynchedEntityData.defineId(ProjectileCarrierEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> SEED =
            SynchedEntityData.defineId(ProjectileCarrierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER =
            SynchedEntityData.defineId(ProjectileCarrierEntity.class, EntityDataSerializers.FLOAT);

    private AbilityDef cachedDef;

    /** Phase 6: multiplies aoeRadius at burst time (autoFire overcharge). Server-set only. */
    private float aoeScale = 1.0f;

    public void setAoeScale(float aoeScale) {
        this.aoeScale = aoeScale;
    }

    public float getAoeScale() {
        return this.aoeScale;
    }

    public ProjectileCarrierEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ABILITY_ID, "");
        builder.define(SEED, 0);
        builder.define(POWER, 1.0f);
    }

    /** Server-side setup before the entity is added to the level. */
    public void init(AbilityDef def, int seed, float power) {
        this.entityData.set(ABILITY_ID, def.id().toString());
        this.entityData.set(SEED, seed);
        this.entityData.set(POWER, power);
        this.cachedDef = def;
    }

    public String getAbilityIdString() {
        return this.entityData.get(ABILITY_ID);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
    }

    public float getPower() {
        return this.entityData.get(POWER);
    }

    public AbilityDef def() {
        if (this.cachedDef != null) {
            return this.cachedDef;
        }
        String idStr = getAbilityIdString();
        if (idStr == null || idStr.isEmpty()) {
            return null;
        }
        this.cachedDef = AbilityRegistry.get(Identifier.tryParse(idStr));
        return this.cachedDef;
    }

    @Override
    protected double getDefaultGravity() {
        AbilityDef def = def();
        if (def != null && def.projectile() != null) {
            return 0.03 * def.projectile().gravityScale();
        }
        return 0.03;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            AbilityDef def = def();
            if (def == null || this.tickCount > def.phases().active()) {
                if (def != null && this.tickCount > def.phases().active()) {
                    AbilityFxService.send(this, def, AbilityDef.FxEvent.CARRIER_END,
                            this.position(), this.getDeltaMovement(),
                            getSeed(), getPower(), 1);
                    AbilityFeedbackService.carrier(this, def, AbilityDef.FxEvent.CARRIER_END,
                            this.position());
                }
                this.discard();
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            AbilityDef def = def();
            if (def != null && def.projectile() != null) {
                LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
                if (result.getEntity() instanceof LivingEntity target && target != owner) {
                    AbilityDamage.applyHit(serverLevel, def, this, owner, target, getPower(), 0.0, false);
                }
                burst(serverLevel, def, owner, this.position());
                terrainImpact(serverLevel, def);
                AbilityFxService.send(this, def, AbilityDef.FxEvent.CARRIER_HIT,
                        this.position(), this.getDeltaMovement(),
                        getSeed() ^ 0x4917, getPower(), 1);
                AbilityFeedbackService.carrier(this, def, AbilityDef.FxEvent.CARRIER_HIT,
                        this.position());
            }
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            AbilityDef def = def();
            if (def != null && def.projectile() != null) {
                LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
                burst(serverLevel, def, owner, this.position());
                terrainImpact(serverLevel, def);
                AbilityFxService.send(this, def, AbilityDef.FxEvent.CARRIER_HIT,
                        this.position(), this.getDeltaMovement(),
                        getSeed() ^ 0x4917, getPower(), 1);
                AbilityFeedbackService.carrier(this, def, AbilityDef.FxEvent.CARRIER_HIT,
                        this.position());
            }
            this.discard();
        }
    }

    private void burst(ServerLevel level, AbilityDef def, LivingEntity owner, Vec3 at) {
        double aoe = def.projectile().aoeRadius() * this.aoeScale;
        if (aoe > 0) {
            AbilityDamage.applyAreaHit(level, def, this, owner, at, aoe, getPower(), owner);
        }
    }

    /** Terrain impact event (Phase 6), fired on any projectile hit before the discard. */
    private void terrainImpact(ServerLevel level, AbilityDef def) {
        if (def.terrain() != null && "impact".equals(def.terrain().event())) {
            com.dragonblockarcanedba.ability.AbilityTerrain.eventAt(level,
                    this.blockPosition(), def.terrain());
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Ability", getAbilityIdString());
        output.putInt("Seed", getSeed());
        output.putFloat("Power", getPower());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getString("Ability").ifPresent(v -> this.entityData.set(ABILITY_ID, v));
        this.entityData.set(SEED, input.getIntOr("Seed", 0));
        this.entityData.set(POWER, input.getFloatOr("Power", 1.0f));
        this.cachedDef = null;
    }
}
