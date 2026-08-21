package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.item.GrandSwordItem;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
 * - Ally Buffs: Strength II & Resistance II.
 * - Caster Buffs: Strength III, Resistance III, and Haste II / Attack Speed boost (Tweak B).
 * - Enemy Debuff: Slowness II.
 * - Projectile Stasis: Enemy projectiles entering the field hang suspended in mid-air for up to 4s.
 *   Detonates on contact; resumes trajectory if the field moves away.
 */
public class ValorFieldEntity extends Projectile {
    public static final float FIELD_RADIUS = 9.0f;

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
    }

    public LivingEntity getCaster() {
        return this.getOwner() instanceof LivingEntity living ? living : null;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            LivingEntity caster = getCaster();

            // Check caster validity
            if (caster == null || !caster.isAlive() || caster.isRemoved()) {
                releaseAllSuspended(serverLevel);
                this.discard();
                return;
            }

            // Must be holding Grand Sword and using it
            if (caster instanceof Player player) {
                boolean isHolding = player.getMainHandItem().getItem() instanceof GrandSwordItem ||
                                    player.getOffhandItem().getItem() instanceof GrandSwordItem;
                if (!isHolding || (this.tickCount > 5 && !player.isUsingItem())) {
                    releaseAllSuspended(serverLevel);
                    this.discard();
                    return;
                }

                // Continuous Ki/Stamina drain (~3.5% per second = 0.175% per tick)
                PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
                double maxKi = PlayerStats.getMaxKi(player);
                double drainPerTick = (maxKi * 0.035) / 20.0;
                double currentKi = accessor.dba$getCurrentKi();

                if (currentKi >= drainPerTick) {
                    accessor.dba$addKi(-drainPerTick);
                    if (this.tickCount % 5 == 0) {
                        accessor.dba$syncStats();
                    }
                } else {
                    // Out of Ki: cancel field
                    player.stopUsingItem();
                    releaseAllSuspended(serverLevel);
                    this.discard();
                    return;
                }
            }

            // Tweak C: Follow player position smoothly
            this.setPos(caster.getX(), caster.getY(), caster.getZ());
            Vec3 center = this.position();

            // 1. Buffs and Debuffs (Applied every 10 ticks)
            if (this.tickCount % 10 == 0) {
                // Caster receives Tier 3 Valor (Level III / Amplifier 2)
                caster.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.VALOR_HOLDER, 40, 2, false, false, true));

                AABB fieldBox = new AABB(
                    center.x - FIELD_RADIUS, center.y - FIELD_RADIUS, center.z - FIELD_RADIUS,
                    center.x + FIELD_RADIUS, center.y + FIELD_RADIUS, center.z + FIELD_RADIUS
                );

                List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, fieldBox, e -> e.isAlive() && e != caster);

                for (LivingEntity entity : entities) {
                    double dist = Math.sqrt(entity.distanceToSqr(center));
                    if (dist <= FIELD_RADIUS) {
                        if (isAlly(caster, entity)) {
                            // Allies receive Tier 2 Valor (Level II / Amplifier 1)
                            entity.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.VALOR_HOLDER, 40, 1, false, false, true));
                        } else {
                            // Enemy Debuff: Valor Stun
                            entity.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.VALOR_STUN_HOLDER, 40, 1, false, false, true));

                            // Continuous golden energy field pressure damage & cinematic tracking
                            if (caster instanceof Player playerCaster) {
                                PlayerStatsAccessor acc = (PlayerStatsAccessor) playerCaster;
                                float fieldPressureDmg = 55.0f + (float) (acc.dba$getStrength() * 0.45f);
                                entity.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(playerCaster), fieldPressureDmg);
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

            // 2. Translucent golden dome particles
            if (this.tickCount % 2 == 0) {
                for (int i = 0; i < 16; i++) {
                    double u = serverLevel.getRandom().nextDouble();
                    double v = serverLevel.getRandom().nextDouble();
                    double theta = u * 2.0 * Math.PI;
                    double phi = Math.acos(2.0 * v - 1.0);
                    double sinPhi = Math.sin(phi);

                    double px = center.x + FIELD_RADIUS * sinPhi * Math.cos(theta);
                    double py = center.y + 1.0 + FIELD_RADIUS * Math.cos(phi);
                    double pz = center.z + FIELD_RADIUS * sinPhi * Math.sin(theta);

                    serverLevel.sendParticles(
                        new DustParticleOptions(0xFFD700, 1.6f),
                        px, py, pz,
                        1, 0, 0, 0, 0
                    );
                }
            }

            // 3. Projectile Stasis & Suspension
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
                    continue;
                }

                double dist = Math.sqrt(p.distanceToSqr(center));
                if (dist > FIELD_RADIUS) {
                    // Caster walked away / left circle: Resume projectile flight along original trajectory!
                    p.setNoGravity(data.origNoGravity);
                    p.setDeltaMovement(data.origVelocity);
                    p.hurtMarked = true;
                    it.remove();
                    continue;
                }

                // Still in field: freeze solidly in mid-air
                data.suspendedTicks++;
                p.setDeltaMovement(0, 0, 0);

                // Pulsing golden stasis particles around suspended projectile
                if (data.suspendedTicks % 2 == 0) {
                    serverLevel.sendParticles(
                        new DustParticleOptions(0xFFD700, 1.5f),
                        p.getX(), p.getY() + 0.1, p.getZ(),
                        2, 0.15, 0.15, 0.15, 0.02
                    );
                }

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

                    victim.hurtServer(serverLevel, dmgSource, fullSpeedDmg);

                    if (p instanceof KiBlastEntity kiBlast) {
                        serverLevel.sendParticles(new DustParticleOptions(kiBlast.getColor(), 2.0f), p.getX(), p.getY() + 0.2, p.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
                        serverLevel.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9f, 1.5f);
                    } else {
                        serverLevel.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 0.2, p.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
                        serverLevel.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                    p.discard();
                    it.remove();
                    continue;
                }

                // 10-Second Stasis Timer (200 ticks): Breaks, falls down, and disappears after 10s
                if (data.suspendedTicks >= 200) {
                    serverLevel.sendParticles(ParticleTypes.POOF, p.getX(), p.getY(), p.getZ(), 6, 0.1, 0.1, 0.1, 0.05);
                    serverLevel.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                    p.discard();
                    it.remove();
                }
            }
        }
    }

    private void releaseAllSuspended(ServerLevel serverLevel) {
        for (Map.Entry<UUID, SuspendedProjData> entry : suspendedMap.entrySet()) {
            Entity entity = serverLevel.getEntity(entry.getKey());
            if (entity instanceof Projectile p && p.isAlive()) {
                p.setNoGravity(entry.getValue().origNoGravity);
                p.setDeltaMovement(entry.getValue().origVelocity);
                p.hurtMarked = true;
            }
        }
        suspendedMap.clear();
    }

    private static boolean isAlly(LivingEntity caster, LivingEntity other) {
        if (caster instanceof Player && other instanceof Player) {
            return true; // Players are treated as allies
        }
        return caster.isAlliedTo(other);
    }
}
