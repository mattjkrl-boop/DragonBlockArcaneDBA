package com.dragonblockarcanedba.movement;

import com.dragonblockarcanedba.item.geo.DbaGeoWeapon;
import com.dragonblockarcanedba.network.BossStaggerPayload;
import com.dragonblockarcanedba.network.DeathTumblePayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 6 mob physics, server side. One damage hook gives every DBA hit the same
 * physical language, scaled by the target's weight class:
 *
 * - LIGHT mobs pop into the air and juggle (fall speed clamped while the window lives).
 * - HEAVY mobs stagger: a short, low launch.
 * - BOSS class (high knockback resistance or very high health) barely moves.
 *
 * Tracked non-player mobs also get one ground bounce and one wall splat per launch,
 * each with capped impact damage. Players receive the launch only; their landing
 * feel belongs to the Phase 5 movement layer. Deaths caused by a recent DBA hit
 * broadcast a DeathTumblePayload so clients can ragdoll-lite the corpse.
 */
public final class MobPhysics {
    private MobPhysics() {}

    private enum Weight {
        LIGHT(1.0, 0.62), HEAVY(0.45, 0.40), BOSS(0.12, 0.25);
        final double scale;
        final double liftFactor;
        Weight(double scale, double liftFactor) { this.scale = scale; this.liftFactor = liftFactor; }
    }

    private static final class Tracked {
        LivingEntity entity;
        long expiry;
        boolean bounced;
        boolean splatted;
        boolean wasGrounded = true;
        double lastVy;
    }

    private record LastHit(float strength, long tick, boolean dba, UUID attackerId) {}

    private static final Map<UUID, Tracked> TRACKED = new HashMap<>();
    private static final Map<UUID, LastHit> LAST_HIT = new HashMap<>();
    private static final Map<UUID, Long> STAGGER_READY_AT = new HashMap<>();

    private static final double MIN_LAUNCH_DAMAGE = 6.0;
    private static final double STAGGER_MIN_DAMAGE = 18.0;
    private static final long STAGGER_COOLDOWN_TICKS = 30;

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (blocked || damageTaken <= 0) return;
            boolean dba = isDbaSource(source.getEntity()) || isDbaSource(source.getDirectEntity());
            long now = entity.level().getGameTime();
            UUID attackerId = source.getEntity() instanceof Player p ? p.getUUID() : null;
            LAST_HIT.put(entity.getUUID(), new LastHit((float) damageTaken, now, dba, attackerId));
            if (!dba || damageTaken < MIN_LAUNCH_DAMAGE) return;
            Entity attacker = source.getEntity();
            if (attacker == null || attacker == entity) return;
            launch(entity, attacker, damageTaken);
            maybeStagger(entity, damageTaken);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            LastHit hit = LAST_HIT.remove(entity.getUUID());
            TRACKED.remove(entity.getUUID());
            STAGGER_READY_AT.remove(entity.getUUID());
            if (hit == null || !hit.dba()) return;
            if (entity.level().getGameTime() - hit.tick() > 60) return;
            float strength = Math.min(1.5f, 0.4f + hit.strength() / 22f);
            DeathTumblePayload payload = new DeathTumblePayload(entity.getId(), strength);
            for (ServerPlayer viewer : PlayerLookup.tracking(entity)) {
                ServerPlayNetworking.send(viewer, payload);
            }
            // Finisher impact frame for the killer: a hitstop hold plus a shockwave-grade
            // shake (the client routes intensity >= 1.2 to the shockwave post effect too).
            if (hit.attackerId() != null && entity.level().getServer() != null) {
                ServerPlayer killer = entity.level().getServer().getPlayerList().getPlayer(hit.attackerId());
                if (killer != null && killer != entity) {
                    float intensity = 1.25f + strength * 0.35f;
                    ServerPlayNetworking.send(killer, new com.dragonblockarcanedba.network.ScreenShakePayload(intensity, 6, 3));
                }
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = server.getTickCount();
            Iterator<Tracked> it = TRACKED.values().iterator();
            while (it.hasNext()) {
                Tracked t = it.next();
                if (!t.entity.isAlive() || now > t.expiry) {
                    it.remove();
                    continue;
                }
                tickTracked(t);
            }
        });
    }

    /**
     * Boss stagger: bosses barely launch by design, so a heavy DBA hit (18 plus)
     * instead rocks them. Server side it is a brief slowness; the visible rock and
     * dip are client side via BossStaggerPayload. Thirty tick cooldown per boss so
     * a fast combo cannot lock a boss in the animation.
     */
    private static void maybeStagger(LivingEntity entity, float damageTaken) {
        if (entity instanceof Player || damageTaken < STAGGER_MIN_DAMAGE) return;
        if (weightOf(entity) != Weight.BOSS) return;
        long now = entity.level().getGameTime();
        Long readyAt = STAGGER_READY_AT.get(entity.getUUID());
        if (readyAt != null && now < readyAt) return;
        STAGGER_READY_AT.put(entity.getUUID(), now + STAGGER_COOLDOWN_TICKS);
        float strength = Math.min(1.2f, 0.35f + damageTaken / 90f);
        entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 1));
        BossStaggerPayload payload = new BossStaggerPayload(entity.getId(), strength, 16);
        for (ServerPlayer viewer : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(viewer, payload);
        }
    }

    private static boolean isDbaSource(Entity e) {
        if (e == null) return false;
        if (e instanceof Player player) {
            return player.getMainHandItem().getItem() instanceof DbaGeoWeapon;
        }
        return e.getClass().getPackageName().startsWith("com.dragonblockarcanedba.entity");
    }

    private static Weight weightOf(LivingEntity target) {
        double kb = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        float hp = target.getMaxHealth();
        if (kb >= 0.9 || hp >= 400f) return Weight.BOSS;
        if (kb >= 0.45 || hp >= 90f) return Weight.HEAVY;
        return Weight.LIGHT;
    }

    private static void launch(LivingEntity target, Entity attacker, float damage) {
        Weight w = weightOf(target);
        double power = Math.min(1.45, 0.30 + damage * 0.028) * w.scale;
        if (power <= 0.03) return;
        Vec3 dir = new Vec3(target.getX() - attacker.getX(), 0, target.getZ() - attacker.getZ());
        if (dir.lengthSqr() < 1.0e-4) dir = new Vec3(0, 0, 1);
        dir = dir.normalize();
        Vec3 vel = target.getDeltaMovement();
        double vy = Math.max(vel.y, power * w.liftFactor);
        target.setDeltaMovement(vel.x * 0.3 + dir.x * power, vy, vel.z * 0.3 + dir.z * power);
        com.dragonblockarcanedba.util.DbaCompat.markHurt(target);
        if (!(target instanceof Player) && target.level() instanceof ServerLevel) {
            Tracked t = new Tracked();
            t.entity = target;
            t.expiry = target.level().getGameTime() + 36;
            t.wasGrounded = target.onGround();
            t.lastVy = vel.y;
            TRACKED.put(target.getUUID(), t);
        }
    }

    private static void tickTracked(Tracked t) {
        LivingEntity e = t.entity;
        Vec3 vel = e.getDeltaMovement();
        boolean grounded = e.onGround();
        // Juggle: while the window lives, falling is capped so the mob hangs for the next hit.
        if (!grounded && vel.y < -0.22) {
            e.setDeltaMovement(vel.x, -0.22, vel.z);
            vel = e.getDeltaMovement();
        }
        // Ground bounce, once per launch.
        if (!t.wasGrounded && grounded && !t.bounced && t.lastVy <= -0.85) {
            t.bounced = true;
            double bounce = -t.lastVy * 0.42;
            e.setDeltaMovement(vel.x, bounce, vel.z);
            com.dragonblockarcanedba.util.DbaCompat.markHurt(e);
            float dmg = (float) Math.min(6.0, -t.lastVy * 2.2);
            e.hurt(e.level().damageSources().generic(), dmg);
            if (e.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.15, e.getZ(), 12, 0.4, 0.1, 0.4, 0.05);
                level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.PLAYER_SMALL_FALL, SoundSource.HOSTILE, 0.6f, 1.0f);
            }
        }
        // Wall splat, once per launch.
        if (!t.splatted && e.horizontalCollision) {
            double hs = Math.hypot(vel.x, vel.z);
            if (hs > 0.5) {
                t.splatted = true;
                e.hurt(e.level().damageSources().generic(), (float) Math.min(8.0, hs * 7.0));
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 24, 2));
                if (e.level() instanceof ServerLevel level) {
                    level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.7f, 0.8f);
                }
            }
        }
        t.wasGrounded = grounded;
        t.lastVy = e.getDeltaMovement().y;
    }
}
