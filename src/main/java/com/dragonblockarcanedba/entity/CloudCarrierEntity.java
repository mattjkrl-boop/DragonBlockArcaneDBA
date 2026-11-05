package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.ability.AbilityDamage;
import com.dragonblockarcanedba.ability.AbilityDef;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Cloud carrier: a stationary domain that grows from
 * radiusStart to radiusEnd across its life and, on a fixed interval,
 * calls a telegraphed lightning strike inside itself. The pending
 * strike is the one piece of gameplay information every observer
 * must see identically, so it is synced (position, the entity age at
 * which it fires, a shape seed): five extra fields, one strike at a
 * time, scheduled strictly sequentially for exactly that reason.
 * The renderer and the strike picker read the same radius formula,
 * so what you see is what can hit you.
 */
public class CloudCarrierEntity extends CarrierEntity {
    private static final EntityDataAccessor<Float> STRIKE_X =
            SynchedEntityData.defineId(CloudCarrierEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STRIKE_Y =
            SynchedEntityData.defineId(CloudCarrierEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STRIKE_Z =
            SynchedEntityData.defineId(CloudCarrierEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> STRIKE_AGE =
            SynchedEntityData.defineId(CloudCarrierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STRIKE_SEED =
            SynchedEntityData.defineId(CloudCarrierEntity.class, EntityDataSerializers.INT);

    private int nextStrikeAge = -1;
    private int pendingStrikeAge = -1;
    private Vec3 pendingPoint;

    public CloudCarrierEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STRIKE_X, 0f);
        builder.define(STRIKE_Y, 0f);
        builder.define(STRIKE_Z, 0f);
        builder.define(STRIKE_AGE, -1);
        builder.define(STRIKE_SEED, 0);
    }

    /** Current domain radius at the given fractional age, shared by server logic and renderers. */
    public static double radiusAt(AbilityDef def, float ageTicks) {
        AbilityDef.CloudParams cloud = def.cloud();
        float frac = def.phases().active() <= 0 ? 1f
                : Math.max(0f, Math.min(1f, ageTicks / def.phases().active()));
        return cloud.radiusStart() + (cloud.radiusEnd() - cloud.radiusStart()) * frac;
    }

    public boolean hasPendingStrike() {
        return this.entityData.get(STRIKE_AGE) >= 0;
    }

    public Vec3 strikePos() {
        return new Vec3(this.entityData.get(STRIKE_X), this.entityData.get(STRIKE_Y), this.entityData.get(STRIKE_Z));
    }

    public int strikeAge() {
        return this.entityData.get(STRIKE_AGE);
    }

    public int strikeSeed() {
        return this.entityData.get(STRIKE_SEED);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AbilityDef def = def();
        if (def == null || def.cloud() == null) {
            return;
        }
        AbilityDef.CloudParams cloud = def.cloud();
        if (cloud.strikeInterval() <= 0) {
            return;
        }
        if (this.nextStrikeAge < 0) {
            this.nextStrikeAge = cloud.strikeInterval();
        }
        int age = this.tickCount;

        if (this.pendingStrikeAge < 0 && age >= this.nextStrikeAge - cloud.telegraphTicks()) {
            this.pendingPoint = chooseStrikePoint(serverLevel, def);
            this.pendingStrikeAge = this.nextStrikeAge;
            this.entityData.set(STRIKE_X, (float) this.pendingPoint.x);
            this.entityData.set(STRIKE_Y, (float) this.pendingPoint.y);
            this.entityData.set(STRIKE_Z, (float) this.pendingPoint.z);
            this.entityData.set(STRIKE_AGE, this.pendingStrikeAge);
            this.entityData.set(STRIKE_SEED, serverLevel.getRandom().nextInt());
        }

        if (this.pendingStrikeAge >= 0 && age >= this.pendingStrikeAge) {
            Player owner = ownerPlayer();
            Vec3 center = this.pendingPoint.add(0, 1.0, 0);
            AbilityDamage.applyArea(serverLevel, def, this, owner, center,
                    cloud.strikeRadius(), getPower(), owner);
            playHitSound(serverLevel, def, this.pendingPoint);
            this.entityData.set(STRIKE_AGE, -1);
            this.pendingStrikeAge = -1;
            this.pendingPoint = null;
            this.nextStrikeAge = age + cloud.strikeInterval();
        }
    }

    private Vec3 chooseStrikePoint(ServerLevel level, AbilityDef def) {
        double radius = radiusAt(def, this.tickCount);
        Player owner = ownerPlayer();
        Vec3 center = this.position();
        AABB box = new AABB(center.x - radius, center.y - 2, center.z - radius,
                center.x + radius, center.y + def.cloud().height() + 4, center.z + radius);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner);
        if (!candidates.isEmpty()) {
            LivingEntity victim = candidates.get(level.getRandom().nextInt(candidates.size()));
            return new Vec3(victim.getX(), victim.getY(), victim.getZ());
        }
        double ang = level.getRandom().nextDouble() * Math.PI * 2.0;
        double rr = radius * Math.sqrt(level.getRandom().nextDouble());
        double px = center.x + Math.cos(ang) * rr;
        double pz = center.z + Math.sin(ang) * rr;
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(px), (int) Math.floor(pz));
        return new Vec3(px, gy, pz);
    }

    private void playHitSound(ServerLevel level, AbilityDef def, Vec3 at) {
        String soundId = def.sounds() != null ? def.sounds().hit() : null;
        if (soundId == null || soundId.isEmpty()) {
            return;
        }
        net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.tryParse(soundId);
        if (id == null) {
            return;
        }
        SoundEvent event = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(id);
        if (event != null) {
            level.playSound(null, at.x, at.y, at.z, event, SoundSource.WEATHER, 1.2f, 1.0f);
        }
    }
}
