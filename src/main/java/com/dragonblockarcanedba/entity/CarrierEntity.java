package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.ability.AbilityFeedbackService;
import com.dragonblockarcanedba.ability.AbilityFxService;
import com.dragonblockarcanedba.ability.AbilityRegistry;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

/**
 * Base for the generic ability carriers (Wave, Cloud). The synced
 * contract is deliberately minimal and identical for every carrier:
 * the ability id, a seed, and the cast power. Everything else a
 * renderer or a behavior needs comes from the AbilityDef the id
 * resolves to, on both sides (server registry from the datapack,
 * client registry from the sync payload), so carriers never grow
 * per-ability synced fields. Carriers are transient: they refuse to
 * save, and if the game persists one anyway it carries its id in
 * NBT and re-resolves its definition on load.
 */
public abstract class CarrierEntity extends Entity {
    protected static final EntityDataAccessor<String> ABILITY_ID =
            SynchedEntityData.defineId(CarrierEntity.class, EntityDataSerializers.STRING);
    protected static final EntityDataAccessor<Integer> SEED =
            SynchedEntityData.defineId(CarrierEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Float> POWER =
            SynchedEntityData.defineId(CarrierEntity.class, EntityDataSerializers.FLOAT);

    private UUID ownerUuid;
    private AbilityDef cachedDef;

    protected CarrierEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ABILITY_ID, "");
        builder.define(SEED, 0);
        builder.define(POWER, 1.0f);
    }

    /** Server-side setup before the entity is added to the level. */
    public void init(AbilityDef def, int seed, float power, UUID ownerUuid) {
        this.entityData.set(ABILITY_ID, def.id().toString());
        this.entityData.set(SEED, seed);
        this.entityData.set(POWER, power);
        this.ownerUuid = ownerUuid;
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

    /** The definition this carrier is running, or null if it is gone (reload removed it). */
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

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    /** The owning player, server side. Owners are players by design (AbilityService spawns). */
    public Player ownerPlayer() {
        if (this.ownerUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        for (Player player : serverLevel.players()) {
            if (player.getUUID().equals(this.ownerUuid)) {
                return player;
            }
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            AbilityDef def = def();
            if (def == null || this.tickCount > def.phases().active()) {
                // Natural expiry (a vanished definition has no
                // recipes to send): announce the end on the same
                // tick the carrier dies, presentation only.
                if (def != null && this.tickCount > def.phases().active()) {
                    double yawRad = Math.toRadians(this.getYRot());
                    AbilityFxService.send(this, def, AbilityDef.FxEvent.CARRIER_END,
                            this.position(),
                            new net.minecraft.world.phys.Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad)),
                            getSeed(), getPower(), 1);
                    AbilityFeedbackService.carrier(this, def, AbilityDef.FxEvent.CARRIER_END,
                            this.position());
                }
                this.discard();
            }
        }
    }

    /** Carriers are pure effects: nothing can hurt them. */
    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putString("Ability", getAbilityIdString());
        output.putInt("Seed", getSeed());
        output.putFloat("Power", getPower());
        if (this.ownerUuid != null) {
            output.putString("Owner", this.ownerUuid.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.getString("Ability").ifPresent(v -> this.entityData.set(ABILITY_ID, v));
        this.entityData.set(SEED, input.getIntOr("Seed", 0));
        this.entityData.set(POWER, input.getFloatOr("Power", 1.0f));
        input.getString("Owner").ifPresent(v -> {
            try {
                this.ownerUuid = UUID.fromString(v);
            } catch (IllegalArgumentException bad) {
                this.ownerUuid = null;
            }
        });
        this.cachedDef = null;
    }
}
