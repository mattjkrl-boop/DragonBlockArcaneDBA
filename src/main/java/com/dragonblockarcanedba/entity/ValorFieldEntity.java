package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.item.GrandSwordItem;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.dragonblockarcanedba.util.WeaponDrainHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Valor Field Entity — Mobile protective golden dome summoned by Grand Sword (Right Click).
 *
 * Features:
 * - 9.0-block radius protective energy field that follows the player (Tweak C).
 * - Buffs, refreshed every 10 ticks for 40 ticks: the caster gets Valor III
 *   plus Strength III, Resistance III and Haste II; allies get Valor II plus
 *   Strength II and Resistance II. (The Valor effect itself grants attack
 *   damage, attack speed, armor and armor toughness per level; the vanilla
 *   effects are applied alongside it so the promised buffs are real.)
 * - Enemy Debuff: Valor Stun II plus Slowness II, and pressure damage while
 *   the caster is a player.
 * - Projectile Stasis: Enemy projectiles entering the field hang suspended in
 *   mid-air for up to 10s (200 ticks). Detonates on contact; resumes
 *   trajectory if the field moves away.
 * - Ki Drain: 90% of max Ki per minute (1.5% per second, smooth), drained by
 *   this entity while the field is up.
 * - Lifecycle: the dome rises over 10 ticks on spawn and fades out over
 *   FADE_OUT_TICKS on every removal path (release, Ki exhaustion, caster
 *   death, disconnect). It never discards at full brightness.
 */
public class ValorFieldEntity extends Projectile {
    public static final float FIELD_RADIUS = 9.0f;
    public static final int FADE_OUT_TICKS = 12;

    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> FADE_START =
        SynchedEntityData.defineId(ValorFieldEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<String> SUSPENDED_IDS =
        SynchedEntityData.defineId(ValorFieldEntity.class, net.minecraft.network.syncher.EntityDataSerializers.STRING);

    private boolean suspendedDirty = true;
    private static final int VALOR_GOLD_ARGB = 0xFFD833;

    private static class SuspendedProjData {
        Vec3 origVelocity;
        boolean origNoGravity;
        int suspendedTicks;

        SuspendedProjData(Vec3 origVelocity, boolean origNoGravity) {
            this.origVelocity = origVelocity;
            this.origNoGravity = origNoGravity;
            this.suspendedTicks = 0;
        }
    }

    private final Map<UUID, SuspendedProjData> suspendedMap = new HashMap<>();

    public ValorFieldEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public ValorFieldEntity(Level level, LivingEntity caster) {
        super(DbaEntities.VALOR_FIELD, level);
        this.setOwner(caster);
        if (caster != null) {
            this.setPos(caster.getX(), caster.getY(), caster.getZ());
        }
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FADE_START, -1);
        builder.define(SUSPENDED_IDS, "");
    }

    public LivingEntity getCaster() {
        return this.getOwner() instanceof LivingEntity living ? living : null;
    }

    /** Tick at which the fade out began, or -1 while the field is fully up. Synced for the renderer. */
    public int getFadeStart() {
        return this.entityData.get(FADE_START);
    }

    public boolean isFadingOut() {
        return this.getFadeStart() >= 0;
    }

    /**
     * Network ids of the projectiles this field actually suspended, synced so
     * the renderer draws stasis cages only around those projectiles. (The
     * renderer used to cage anything slow inside the radius.)
     */
    public java.util.Set<Integer> getSuspendedIds() {
        String raw = this.entityData.get(SUSPENDED_IDS);
        if (raw == null || raw.isEmpty()) return java.util.Set.of();
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (String part : raw.split(",")) {
            try {
                ids.add(Integer.parseInt(part));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    /**
     * Begin the fade out: suspended projectiles resume immediately, the dome
     * renders its 12 tick dissipation, then the entity discards. Every
     * removal path goes through here; nothing discards the field directly.
     */
    public void beginFadeOut() {
        if (this.isFadingOut()) return;
        this.entityData.set(FADE_START, this.tickCount);
        if (this.level() instanceof ServerLevel serverLevel) {
            releaseAllSuspended(serverLevel);
        }
    }

    private void syncSuspendedIds(ServerLevel serverLevel) {
        StringBuilder sb = new StringBuilder();
        for (UUID uuid : suspendedMap.keySet()) {
            Entity e = serverLevel.getEntity(uuid);
            if (e != null) {
                if (sb.length() > 0) sb.append(',');
                sb.append(e.getId());
            }
        }
        this.entityData.set(SUSPENDED_IDS, sb.toString());
        this.suspendedDirty = false;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            LivingEntity caster = getCaster();

            // Fade out in progress: no buffs, no stasis, no drain. The dome
            // follows a living caster, rains theme sparks as it dissipates,
            // and discards only after FADE_OUT_TICKS.
            if (this.isFadingOut()) {
                if (caster != null && caster.isAlive()) {
                    this.setPos(caster.getX(), caster.getY(), caster.getZ());
                }
                if (this.suspendedDirty) {
                    syncSuspendedIds(serverLevel);
                }
                com.dragonblockarcanedba.util.LayeredParticles.emitRain(serverLevel, this.position(),
                    FIELD_RADIUS * 0.8f, 4.0f, 19f, 20f,
                    com.dragonblockarcanedba.particle.DbaParticles.SPARK);
                if (this.tickCount - this.getFadeStart() >= FADE_OUT_TICKS) {
                    this.discard();
                }
                return;
            }

            // Check caster validity: every failure fades the field out
            // instead of discarding the dome in one tick.
            if (caster == null || !caster.isAlive() || caster.isRemoved()) {
                beginFadeOut();
                return;
            }

            // Must be holding Grand Sword and using it
            if (caster instanceof Player player) {
                boolean isHolding = player.getMainHandItem().getItem() instanceof GrandSwordItem ||
                                    player.getOffhandItem().getItem() instanceof GrandSwordItem;
                if (!isHolding || (this.tickCount > 5 && !player.isUsingItem())) {
                    beginFadeOut();
                    return;
                }

                // Continuous Ki drain: 90% per minute (smooth)
                if (!WeaponDrainHelper.drainKiPerTick(player, 90.0)) {
                    // Out of Ki: cancel the use; releaseUsing and this call
                    // both route the field into its fade out.
                    player.stopUsingItem();
                    beginFadeOut();
                    return;
                }
                if (this.tickCount % 5 == 0) {
                    ((PlayerStatsAccessor) player).dba$syncStats();
                }
            }

            // Tweak C: Follow player position smoothly
            this.setPos(caster.getX(), caster.getY(), caster.getZ());
            Vec3 center = this.position();

            // Spawn moment: the field used to appear at full power in total silence.
            if (this.tickCount == 1) {
                serverLevel.playSound(null, center.x, center.y + 1.0, center.z,
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.1f);
                float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
                if (density > 0f) {
                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                        center.x, center.y + 1.0, center.z, Math.round(36 * density), 3.5, 1.2, 3.5, 0.25);
                }
                com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel, center,
                    FIELD_RADIUS * 0.7f, VALOR_GOLD_ARGB);
            }

            // Theme particles: the shared layered vortex (bright core motes,
            // gold mid band, wispy outer sparks) fills the dome volume. The
            // old code scattered about 4 SPARK every 4 ticks across the whole
            // 9 block radius, leaving the interior near empty. Age is capped
            // so the envelope holds at peak for the field's open ended life.
            if (this.tickCount % 2 == 0) {
                com.dragonblockarcanedba.util.LayeredParticles.emitVortex(serverLevel, center,
                    FIELD_RADIUS * 0.85f, 3.2f, Math.min(this.tickCount, 20), 28,
                    VALOR_GOLD_ARGB, com.dragonblockarcanedba.particle.DbaParticles.SPARK);
            }

            // 1. Buffs and Debuffs (Applied every 10 ticks)
            if (this.tickCount % 10 == 0) {
                // Caster receives Tier 3 Valor (Level III / Amplifier 2), plus
                // the vanilla buffs the field promises: Strength III,
                // Resistance III and Haste II. The header used to promise
                // these while the code applied none of them.
                caster.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.VALOR_HOLDER, 40, 2, false, false, true));
                caster.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.STRENGTH, 40, 2, false, false, true));
                caster.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.RESISTANCE, 40, 2, false, false, true));
                caster.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.HASTE, 40, 1, false, false, true));

                AABB fieldBox = new AABB(
                    center.x - FIELD_RADIUS, center.y - FIELD_RADIUS, center.z - FIELD_RADIUS,
                    center.x + FIELD_RADIUS, center.y + FIELD_RADIUS, center.z + FIELD_RADIUS
                );

                List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, fieldBox, e -> e.isAlive() && e != caster);

                for (LivingEntity entity : entities) {
                    double dist = Math.sqrt(entity.distanceToSqr(center));
                    if (dist <= FIELD_RADIUS) {
                        if (isAlly(caster, entity)) {
                            // Allies receive Tier 2 Valor (Level II / Amplifier 1),
                            // plus the promised Strength II and Resistance II.
                            entity.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.VALOR_HOLDER, 40, 1, false, false, true));
                            entity.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.STRENGTH, 40, 1, false, false, true));
                            entity.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.RESISTANCE, 40, 1, false, false, true));
                        } else {
                            // Enemy Debuff: Valor Stun II plus the promised Slowness II.
                            entity.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.VALOR_STUN_HOLDER, 40, 1, false, false, true));
                            entity.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 40, 1, false, false, true));

                            // Continuous golden energy field pressure damage & cinematic tracking
                            if (caster instanceof Player playerCaster) {
                                PlayerStatsAccessor acc = (PlayerStatsAccessor) playerCaster;
                                float fieldPressureDmg = 55.0f + (float) (acc.dba$getStrength() * 0.45f);
                                entity.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(playerCaster), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (fieldPressureDmg)));
                                // Pressure hits had no feedback at all: enemies just lost health.
                                serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), 3, 0.3, 0.3, 0.3, 0.05);
                                entity.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 40, 0, false, false, false), playerCaster);
                            }
                        }
                    }
                }

                // Ambient golden field hum sound
                if (this.tickCount % 30 == 0) {
                    serverLevel.playSound(null, center.x, center.y + 1.0, center.z,
                        SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.7f, 1.2f);
                }
            }

            // 2. Projectile Stasis & Suspension
            AABB projBox = new AABB(
                center.x - FIELD_RADIUS, center.y - FIELD_RADIUS, center.z - FIELD_RADIUS,
                center.x + FIELD_RADIUS, center.y + FIELD_RADIUS, center.z + FIELD_RADIUS
            );

            List<Projectile> projectiles = serverLevel.getEntitiesOfClass(
                Projectile.class, projBox,
                p -> p.isAlive() && p.getOwner() != caster && !(p instanceof OxShockwaveEntity) && !(p instanceof GrandCrescentWaveEntity)
            );

            // Suspend new projectiles entering the field
            for (Projectile p : projectiles) {
                double dist = Math.sqrt(p.distanceToSqr(center));
                if (dist <= FIELD_RADIUS) {
                    UUID pUuid = p.getUUID();
                    if (!suspendedMap.containsKey(pUuid)) {
                        Vec3 curVel = p.getDeltaMovement();
                        suspendedMap.put(pUuid, new SuspendedProjData(curVel, p.isNoGravity()));
                        this.suspendedDirty = true;
                        p.setNoGravity(true);
                        p.setDeltaMovement(0, 0, 0);

                        serverLevel.playSound(null, p.getX(), p.getY(), p.getZ(),
                            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.8f);
                    }
                }
            }

            // Process currently suspended projectiles
            Iterator<Map.Entry<UUID, SuspendedProjData>> it = suspendedMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, SuspendedProjData> entry = it.next();
                UUID pUuid = entry.getKey();
                SuspendedProjData data = entry.getValue();

                Entity entity = serverLevel.getEntity(pUuid);
                if (!(entity instanceof Projectile p) || !p.isAlive()) {
                    it.remove();
                    this.suspendedDirty = true;
                    continue;
                }

                double dist = Math.sqrt(p.distanceToSqr(center));
                if (dist > FIELD_RADIUS) {
                    // Caster walked away / left circle: Resume projectile flight along original trajectory!
                    p.setNoGravity(data.origNoGravity);
                    p.setDeltaMovement(data.origVelocity);
                    com.dragonblockarcanedba.util.DbaCompat.markHurt(p);
                    it.remove();
                    this.suspendedDirty = true;
                    continue;
                }

                // Still in field: freeze solidly in mid-air
                data.suspendedTicks++;
                p.setDeltaMovement(0, 0, 0);

                // Check collision with ANY living entity (deals full-speed normal projectile damage if someone walks into it)
                AABB pBox = p.getBoundingBox().inflate(0.4);
                List<LivingEntity> colliders = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, pBox,
                    LivingEntity::isAlive
                );

                if (!colliders.isEmpty()) {
                    LivingEntity victim = colliders.get(0);
                    float speed = (float) Math.max(1.0, data.origVelocity.length());
                    float fullSpeedDmg = 16.0f * speed;

                    if (p instanceof KiBlastEntity kiBlast) {
                        fullSpeedDmg = kiBlast.getDamage();
                    }

                    DamageSource dmgSource = p.getOwner() instanceof LivingEntity livingOwner
                        ? (p instanceof KiBlastEntity ? serverLevel.damageSources().indirectMagic(p, livingOwner) : serverLevel.damageSources().mobProjectile(p, livingOwner))
                        : serverLevel.damageSources().generic();

                    victim.hurtServer(serverLevel, dmgSource, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (fullSpeedDmg)));

                    if (p instanceof KiBlastEntity kiBlast) {
                        serverLevel.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9f, 1.5f);
                    } else {
                        serverLevel.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                    p.discard();
                    it.remove();
                    this.suspendedDirty = true;
                    continue;
                }

                // 10-Second Stasis Timer (200 ticks): Breaks, falls down, and disappears after 10s
                if (data.suspendedTicks >= 200) {
                    serverLevel.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                    p.discard();
                    it.remove();
                    this.suspendedDirty = true;
                }
            }

            if (this.suspendedDirty) {
                syncSuspendedIds(serverLevel);
            }
        }
    }

    private void releaseAllSuspended(ServerLevel serverLevel) {
        for (Map.Entry<UUID, SuspendedProjData> entry : suspendedMap.entrySet()) {
            Entity entity = serverLevel.getEntity(entry.getKey());
            if (entity instanceof Projectile p && p.isAlive()) {
                p.setNoGravity(entry.getValue().origNoGravity);
                p.setDeltaMovement(entry.getValue().origVelocity);
                com.dragonblockarcanedba.util.DbaCompat.markHurt(p);
            }
        }
        suspendedMap.clear();
        this.suspendedDirty = true;
    }

    private static boolean isAlly(LivingEntity caster, LivingEntity other) {
        // Real alliance only (same team). This used to return true for ANY
        // player versus player pair, so an opposing player standing in the
        // field was buffed instead of debuffed and pressure damaged.
        return caster.isAlliedTo(other);
    }
}
