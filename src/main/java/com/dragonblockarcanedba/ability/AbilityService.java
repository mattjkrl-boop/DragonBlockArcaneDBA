package com.dragonblockarcanedba.ability;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.entity.BeamCarrierEntity;
import com.dragonblockarcanedba.entity.CloudCarrierEntity;
import com.dragonblockarcanedba.entity.DbaEntities;
import com.dragonblockarcanedba.entity.ProjectileCarrierEntity;
import com.dragonblockarcanedba.entity.SwarmHostEntity;
import com.dragonblockarcanedba.entity.WaveCarrierEntity;
import com.dragonblockarcanedba.network.AbilitySyncPayload;
import com.dragonblockarcanedba.network.C2SWeaponClickPayload;
import com.dragonblockarcanedba.network.CastSyncPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side execution for every ability: the only place casts are
 * started, charged, fired, cooled down, and charged for. State is
 * two maps (one active cast per player; per-ability cooldown end
 * times plus a shared recovery lockout), keyed by player UUID
 * against level game time. Ability identity always comes from the
 * item actually in the player's main hand through AbilityRegistry;
 * the click payload only says which button changed.
 *
 * Phase 6 adds a per-player Runtime beside the cast maps: timed
 * caster movement, channels (sustain and autoFire), combo counting,
 * multi-charge regen, the swarm deploy state, and delayed volley
 * spawns. Every new structure is inert for defs without the new
 * blocks, so the proof attacks run exactly the Phase 1 to 5 paths.
 */
public final class AbilityService {
    private AbilityService() {}

    private record Cast(Identifier abilityId, long startTick, boolean chargeRelease) {}

    private static final Map<UUID, Cast> CASTS = new HashMap<>();
    private static final Map<UUID, Map<Identifier, Long>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> LOCKOUT = new HashMap<>();

    // ---------------- Phase 6 runtime state ----------------

    private static final class MoveState {
        AbilityDef def;
        float power;
        String mode;
        long startTick;
        int ticksLeft;
        int hopTimer;
        int hopsDone;
        final Set<UUID> visited = new java.util.LinkedHashSet<>();
        double dashDirX;
        double dashDirZ;
        boolean startedAirborne;
        int seed;
    }

    private static final class ChannelState {
        AbilityDef def;
        boolean autoFire;
        Entity carrier;
        long carrierSpawnTick;
        double heat;
        int ticksSinceShot;
        int startupLeft;
    }

    private static final class ComboState {
        int count;
        long lastHitTick;
        boolean armed;
    }

    private static final class ChargeState {
        int count;
        int max;
        int regenTicks;
        long nextRegenTick;
    }

    private static final class SwarmState {
        SwarmHostEntity host;
        Identifier defId;
        long deployTick;
    }

    private record PendingSpawn(AbilityDef def, float power, float yawOffset) {}

    private static final class Runtime {
        MoveState move;
        ChannelState channel;
        final Map<Identifier, ComboState> combos = new HashMap<>();
        final Map<Identifier, ChargeState> charges = new HashMap<>();
        SwarmState swarm;
        final Map<Long, PendingSpawn> pendingByTick = new HashMap<>();
        long auraLastDamageTick = -1000000L;
        final Set<Integer> auraDeflected = new HashSet<>();
        /** Discrete Ki drains in flight: {perTick, ticksLeft}. */
        final List<double[]> kiDrains = new ArrayList<>();
    }

    private static final Map<UUID, Runtime> RUNTIMES = new HashMap<>();

    private static Runtime runtime(ServerPlayer player) {
        return RUNTIMES.computeIfAbsent(player.getUUID(), k -> new Runtime());
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayer(player);
                tickRuntime(player);
            }
        });
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> sendSync(player));
    }

    public static void sendSync(ServerPlayer player) {
        java.util.List<AbilitySyncPayload.Entry> entries = new java.util.ArrayList<>();
        AbilityLoader.rawJson().forEach((id, json) ->
                entries.add(new AbilitySyncPayload.Entry(id.toString(), json)));
        ServerPlayNetworking.send(player, new AbilitySyncPayload(entries));
    }

    /** Entry point for C2SWeaponClickPayload, called on the server thread. */
    public static void onClick(ServerPlayer player, int button, int action, int heldTicks) {
        AbilityDef.Slot slot = button == C2SWeaponClickPayload.BUTTON_LEFT
                ? AbilityDef.Slot.LEFT : AbilityDef.Slot.RIGHT;
        if (action == C2SWeaponClickPayload.ACTION_PRESS) {
            onPress(player, slot);
        } else if (action == C2SWeaponClickPayload.ACTION_RELEASE) {
            onRelease(player, slot, heldTicks);
        }
    }

    private static void onPress(ServerPlayer player, AbilityDef.Slot slot) {
        long now = player.level().getGameTime();
        UUID uuid = player.getUUID();
        Runtime rt = runtime(player);
        AbilityDef heldBase = heldAbility(player, slot);

        // (a) Swarm marking: with a live swarm, left click on the
        // swarm's weapon marks the looked-at living entity instead
        // of casting.
        if (rt.swarm != null && rt.swarm.host != null && !rt.swarm.host.isRemoved()
                && slot == AbilityDef.Slot.LEFT) {
            AbilityDef swarmDef = AbilityRegistry.get(rt.swarm.defId);
            ItemStack stack = player.getMainHandItem();
            Identifier itemId = stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (swarmDef != null && swarmDef.weapon().equals(itemId)) {
                LivingEntity target = raycastEntity(player, 40.0, 3.0);
                if (target != null) {
                    rt.swarm.host.setMarked(target.getUUID());
                    playSoundId(player, swarmDef.sounds() != null ? swarmDef.sounds().hit() : null, 1.0f, 1.0f);
                }
                return;
            }
        }
        if (heldBase == null) {
            return;
        }

        // (b) Swarm toggle recall: pressing the swarm def's slot
        // again recalls it (after the lockout window).
        if (heldBase.carrier() == AbilityDef.Carrier.SWARM && rt.swarm != null
                && rt.swarm.defId.equals(heldBase.id()) && heldBase.swarm() != null) {
            if (now - rt.swarm.deployTick < heldBase.swarm().lockoutTicks()) {
                return;
            }
            rt.swarm.host.endReason = SwarmHostEntity.EndReason.RECALLED;
            rt.swarm.host.discardAll();
            rt.swarm = null;
            setCooldown(player, heldBase.id(), heldBase.swarm().recallCooldownTicks());
            LOCKOUT.put(uuid, now + heldBase.swarm().lockoutTicks());
            return;
        }

        // (c) Variant resolution: sneak first, then airborne.
        AbilityDef def = resolveVariant(player, heldBase);

        // Starting anything cancels a charge in progress, silently.
        Cast replaced = CASTS.remove(uuid);
        if (replaced != null) {
            broadcastCast(player, AbilityRegistry.get(replaced.abilityId()),
                    CastSyncPayload.ACTION_CANCEL, now);
        }

        if (now < LOCKOUT.getOrDefault(uuid, 0L)) {
            return;
        }

        // (d) Charges replace the cooldown check for multi-charge defs.
        boolean chargeDef = def.charges() > 1;
        if (chargeDef) {
            ChargeState cs = rt.charges.computeIfAbsent(def.id(), k -> {
                ChargeState fresh = new ChargeState();
                fresh.count = def.charges();
                fresh.max = def.charges();
                fresh.regenTicks = regenTicksOf(def);
                return fresh;
            });
            if (cs.count <= 0) {
                return;
            }
        } else {
            Map<Identifier, Long> cds = COOLDOWNS.get(uuid);
            if (cds != null && now < cds.getOrDefault(def.id(), 0L)) {
                return;
            }
        }

        // (e) Channels: press starts the channel and returns.
        if (def.sustain() != null || def.autoFire() != null) {
            if (rt.channel != null) {
                return;
            }
            if (def.sustain() != null && def.kiCost() > 0) {
                PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
                double price = def.kiPrice(PlayerStats.getMaxKi(player));
                if (accessor.dba$getCurrentKi() < price) {
                    playSound(player, "dragonblockarcanedba:fizzle", 0.7f, 1.0f);
                    return;
                }
            }
            ChannelState channel = new ChannelState();
            channel.def = def;
            channel.autoFire = def.autoFire() != null;
            channel.startupLeft = def.phases().startup();
            rt.channel = channel;
            broadcastCast(player, def, CastSyncPayload.ACTION_START, now);
            AbilityFxService.send(player, def, AbilityDef.FxEvent.CAST_START,
                    player.position().add(0, 1.0, 0), player.getLookAngle(),
                    player.level().getRandom().nextInt(), 1.0f, 1);
            playSoundId(player, def.sounds() != null ? def.sounds().charge() : null, 0.8f, 1.0f);
            return;
        }

        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        double price = def.kiPrice(PlayerStats.getMaxKi(player));
        if (accessor.dba$getCurrentKi() < price) {
            playSound(player, "dragonblockarcanedba:fizzle", 0.7f, 1.0f);
            return;
        }

        if (def.phases().startup() <= 0) {
            AbilityFxService.send(player, def, AbilityDef.FxEvent.CAST_START,
                    player.position().add(0, 1.0, 0), player.getLookAngle(),
                    player.level().getRandom().nextInt(), 1.0f, 1);
            fire(player, def, 1.0f);
            return;
        }
        CASTS.put(uuid, new Cast(def.id(), now, def.chargeRelease()));
        broadcastCast(player, def, CastSyncPayload.ACTION_START, now);
        ModelDisplaySpawner.spawnForCast(player, def, player.level().getRandom().nextInt());
        AbilityFxService.send(player, def, AbilityDef.FxEvent.CAST_START,
                player.position().add(0, 1.0, 0), player.getLookAngle(),
                player.level().getRandom().nextInt(), 1.0f, 1);
        playSoundId(player, def.sounds() != null ? def.sounds().charge() : null, 0.8f, 1.0f);
    }

    private static int regenTicksOf(AbilityDef def) {
        return def.chargeRegenTicks() > 0 ? def.chargeRegenTicks() : def.cooldownTicks();
    }

    /** Sneak variant wins, then the air variant, else the base def. */
    private static AbilityDef resolveVariant(ServerPlayer player, AbilityDef base) {
        if (base == null) {
            return null;
        }
        if (player.isShiftKeyDown() && base.sneakVariant() != null) {
            AbilityDef variant = AbilityRegistry.get(Identifier.tryParse(base.sneakVariant()));
            if (variant != null) {
                return variant;
            }
        } else if (!player.onGround() && base.airVariant() != null) {
            AbilityDef variant = AbilityRegistry.get(Identifier.tryParse(base.airVariant()));
            if (variant != null) {
                return variant;
            }
        }
        return base;
    }

    private static void onRelease(ServerPlayer player, AbilityDef.Slot slot, int heldTicks) {
        UUID uuid = player.getUUID();
        Runtime rt = RUNTIMES.get(uuid);

        // Releasing the channel's slot ends the channel.
        if (rt != null && rt.channel != null) {
            if (rt.channel.def.slot() == slot) {
                endChannel(player, rt);
            }
            return;
        }

        Cast cast = CASTS.get(uuid);
        if (cast == null || !cast.chargeRelease()) {
            return;
        }
        AbilityDef base = heldAbility(player, slot);
        AbilityDef def = resolveVariant(player, base);
        CASTS.remove(uuid);
        if (def == null || !def.id().equals(cast.abilityId())) {
            broadcastCast(player, AbilityRegistry.get(cast.abilityId()),
                    CastSyncPayload.ACTION_CANCEL, player.level().getGameTime());
            return; // weapon changed mid-charge: the charge is gone
        }
        long held = player.level().getGameTime() - cast.startTick();
        float frac = def.phases().startup() <= 0 ? 1.0f
                : Math.max(0f, Math.min(1f, (float) held / def.phases().startup()));
        float power = def.minPower() + (1.0f - def.minPower()) * frac;

        // Critical window: releasing inside it scales power and summons.
        boolean critical = def.criticalWindow() != null
                && held >= def.criticalWindow().fromTicks() && held <= def.criticalWindow().toTicks();
        if (critical) {
            power *= (float) def.criticalWindow().powerScale();
        }
        fire(player, def, power);
        if (critical && def.criticalWindow().summonAbility() != null) {
            executeSummon(player, def,
                    new AbilityDef.SummonSpec(def.criticalWindow().summonAbility(), "caster", "critical", false),
                    power, player.position(), null);
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Cast cast = CASTS.get(uuid);
        if (cast == null) {
            return;
        }
        AbilityDef def = AbilityRegistry.get(cast.abilityId());
        if (def == null) {
            CASTS.remove(uuid);
            // The definition is gone (a reload removed it), so whether
            // it had an animation block is unknowable here; the cancel
            // goes out ungated and clients that track no cast for this
            // player simply ignore it.
            broadcastCastRaw(player, cast.abilityId(),
                    CastSyncPayload.ACTION_CANCEL, player.level().getGameTime());
            return;
        }
        // The charge dies if the weapon leaves the main hand.
        AbilityDef held = heldAbility(player, def.slot());
        if (held == null || !held.id().equals(def.id())) {
            CASTS.remove(uuid);
            broadcastCast(player, def, CastSyncPayload.ACTION_CANCEL, player.level().getGameTime());
            return;
        }
        long now = player.level().getGameTime();
        long elapsed = now - cast.startTick();

        // Charge dressing is client-local since Phase 3: every
        // client expands the definition's charge_tick recipes from
        // the cast store (client/ability/ClientAbilityFx), so the
        // server sends no charge particles at all.

        // Phase 6: rooted charges pin the caster; auras run while held.
        if (def.rooted()) {
            Vec3 vel = player.getDeltaMovement();
            player.setDeltaMovement(0, vel.y, 0);
            player.resetFallDistance();
        }
        if (def.aura() != null) {
            tickAura(player, def);
        }

        if (!cast.chargeRelease() && elapsed >= def.phases().startup()) {
            CASTS.remove(uuid);
            fire(player, def, 1.0f);
        }
    }

    /** The aura module, ticked while a def with an aura block is being held. */
    private static void tickAura(ServerPlayer player, AbilityDef def) {
        AbilityDef.AuraSpec aura = def.aura();
        Runtime rt = runtime(player);
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        Player owner = player;
        Vec3 center = player.position();
        AABB box = new AABB(center, center).inflate(aura.radius()).expandTowards(0, 3, 0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner);

        if (aura.pull() != 0) {
            for (LivingEntity e : targets) {
                Vec3 toCenter = center.subtract(e.position());
                Vec3 horiz = new Vec3(toCenter.x, 0, toCenter.z);
                if (horiz.lengthSqr() > 1.0e-4) {
                    e.setDeltaMovement(e.getDeltaMovement()
                            .add(horiz.normalize().scale(aura.pull() * 0.045)));
                }
            }
        }
        if (aura.deflect()) {
            for (net.minecraft.world.entity.projectile.Projectile projectile
                    : level.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, box,
                    pr -> !pr.isRemoved() && pr.getOwner() != owner)) {
                if (rt.auraDeflected.contains(projectile.getId())) {
                    continue;
                }
                rt.auraDeflected.add(projectile.getId());
                projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-aura.deflectScale()));
                projectile.setOwner(owner);
            }
        }
        if (aura.damageInterval() > 0 && now - rt.auraLastDamageTick >= aura.damageInterval()) {
            rt.auraLastDamageTick = now;
            for (LivingEntity e : targets) {
                double dist = e.position().distanceTo(center);
                AbilityDamage.applyHitScaled(level, def, player, player, e,
                        1.0f, aura.radius() <= 0 ? 0 : dist / aura.radius(), false, aura.damageScale());
            }
        }
        if (aura.selfEffectId() != null && now % 20 == 0) {
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(aura.selfEffectId());
            if (effect != null) {
                player.addEffect(new MobEffectInstance(
                        BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), aura.selfEffectDuration(), 0));
            }
        }
    }

    private static void tickRuntime(ServerPlayer player) {
        Runtime rt = RUNTIMES.get(player.getUUID());
        if (rt == null) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();

        if (!rt.kiDrains.isEmpty()) {
            PlayerStatsAccessor ki = (PlayerStatsAccessor) player;
            double take = 0;
            for (java.util.Iterator<double[]> it = rt.kiDrains.iterator(); it.hasNext(); ) {
                double[] d = it.next();
                take += d[0];
                if (--d[1] <= 0) {
                    it.remove();
                }
            }
            ki.dba$addKi(-Math.min(take, ki.dba$getCurrentKi()));
            ki.dba$pauseKiRecovery(20);
            ki.dba$syncStats();
        }

        // Channels die on weapon swap or when their def vanishes.
        if (rt.channel != null) {
            AbilityDef held = heldAbility(player, rt.channel.def.slot());
            if (held == null || !held.id().equals(rt.channel.def.id())) {
                endChannel(player, rt);
            } else {
                tickChannel(player, rt);
            }
        }

        if (rt.move != null) {
            tickMove(player, rt);
        }

        // Delayed volley spawns.
        if (!rt.pendingByTick.isEmpty()) {
            Iterator<Map.Entry<Long, PendingSpawn>> it = rt.pendingByTick.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, PendingSpawn> entry = it.next();
                if (now >= entry.getKey()) {
                    PendingSpawn pending = entry.getValue();
                    spawnProjectile(player, pending.def(), pending.power(),
                            player.getYRot() + pending.yawOffset(), player.getXRot());
                    it.remove();
                }
            }
        }

        // Swarm watcher: a host that vanished without a recall picks
        // its cooldown from its end reason.
        if (rt.swarm != null && (rt.swarm.host == null || rt.swarm.host.isRemoved())) {
            SwarmHostEntity.EndReason reason = rt.swarm.host != null
                    ? rt.swarm.host.endReason : SwarmHostEntity.EndReason.NONE;
            AbilityDef swarmDef = AbilityRegistry.get(rt.swarm.defId);
            if (swarmDef != null && swarmDef.swarm() != null) {
                switch (reason) {
                    case RECALLED -> {
                    }
                    case STARVED -> setCooldown(player, swarmDef.id(), swarmDef.swarm().starveCooldownTicks());
                    case EXPIRED, NONE -> setCooldown(player, swarmDef.id(), swarmDef.cooldownTicks());
                }
            }
            rt.swarm = null;
        }

        // Charge regen.
        for (ChargeState cs : rt.charges.values()) {
            while (cs.nextRegenTick > 0 && now >= cs.nextRegenTick && cs.count < cs.max) {
                cs.count++;
                if (cs.count < cs.max && cs.regenTicks > 0) {
                    cs.nextRegenTick += cs.regenTicks;
                } else {
                    cs.nextRegenTick = 0;
                }
            }
            if (cs.count >= cs.max) {
                cs.nextRegenTick = 0;
            }
        }
    }

    private static void tickChannel(ServerPlayer player, Runtime rt) {
        ChannelState channel = rt.channel;
        AbilityDef def = channel.def;
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        double maxKi = PlayerStats.getMaxKi(player);

        if (channel.startupLeft > 0) {
            channel.startupLeft--;
            if (channel.startupLeft > 0) {
                return;
            }
        }

        if (!channel.autoFire) {
            // Sustain: one beam carrier, Ki drained per tick.
            if (channel.carrier == null) {
                channel.carrier = spawnCarrier(player, def, 1.0f, null, player.getYRot());
                channel.carrierSpawnTick = now;
                if (channel.carrier == null) {
                    endChannel(player, rt);
                    return;
                }
            }
            double perTick = maxKi * def.sustain().kiPercentPerSecond() / 2000.0;
            if (accessor.dba$getCurrentKi() < perTick || channel.carrier.isRemoved()
                    || now - channel.carrierSpawnTick >= def.phases().active()) {
                endChannel(player, rt);
                return;
            }
            accessor.dba$addKi(-perTick);
            accessor.dba$pauseKiRecovery(20);
            accessor.dba$syncStats();
            return;
        }

        // AutoFire: one projectile per interval, heat to overcharge.
        channel.ticksSinceShot++;
        if (channel.ticksSinceShot < def.autoFire().intervalTicks()) {
            return;
        }
        channel.ticksSinceShot = 0;
        double price = def.kiPrice(maxKi);
        if (accessor.dba$getCurrentKi() < price) {
            endChannel(player, rt);
            return;
        }
        accessor.dba$addKi(-price);
        accessor.dba$pauseKiRecovery(20);
        accessor.dba$syncStats();
        channel.heat += def.autoFire().heatPerShot();
        boolean over = def.autoFire().overchargeHeat() > 0 && channel.heat >= def.autoFire().overchargeHeat();
        float shotPower = over ? (float) def.autoFire().overchargePower() : 1.0f;
        Entity shot = spawnCarrier(player, def, shotPower, null, player.getYRot());
        if (over) {
            if (shot instanceof ProjectileCarrierEntity projectile) {
                projectile.setAoeScale((float) def.autoFire().overchargeAoeScale());
            }
            channel.heat = 0;
        }
        AbilityFxService.send(player, def, AbilityDef.FxEvent.FIRE,
                player.position().add(0, 1.0, 0), player.getLookAngle(),
                level.getRandom().nextInt(), shotPower, 1);
        playSoundId(player, def.sounds() != null ? def.sounds().fire() : null, 1.0f, 1.0f);
    }

    /** Ends a channel: carrier discarded, lockout and cooldown from now, pose cancelled. */
    private static void endChannel(ServerPlayer player, Runtime rt) {
        ChannelState channel = rt.channel;
        if (channel == null) {
            return;
        }
        rt.channel = null;
        if (channel.carrier != null && !channel.carrier.isRemoved()) {
            channel.carrier.discard();
        }
        long now = player.level().getGameTime();
        LOCKOUT.put(player.getUUID(), now + channel.def.phases().recovery());
        setCooldown(player, channel.def.id(), channel.def.cooldownTicks());
        broadcastCast(player, channel.def, CastSyncPayload.ACTION_CANCEL, now);
    }

    /** Writes a cooldown end time for one ability. LOCKOUT is untouched. */
    public static void setCooldown(ServerPlayer player, Identifier defId, int ticks) {
        COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(defId, player.level().getGameTime() + ticks);
    }

    /** Combo hit report from the damage path (Phase 6 combo module). */
    public static void noteComboHit(ServerPlayer player, AbilityDef def) {
        if (def == null || def.combo() == null) {
            return;
        }
        Runtime rt = runtime(player);
        long now = player.level().getGameTime();
        ComboState combo = rt.combos.computeIfAbsent(def.id(), k -> new ComboState());
        if (now - combo.lastHitTick > def.combo().windowTicks()) {
            combo.count = 0;
        }
        combo.count++;
        combo.lastHitTick = now;
        if (combo.count >= def.combo().hits()) {
            combo.armed = true;
            combo.count = 0;
        }
    }

    private static void fire(ServerPlayer player, AbilityDef def, float power) {
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        double price = def.kiPrice(PlayerStats.getMaxKi(player));
        if (accessor.dba$getCurrentKi() < price) {
            playSound(player, "dragonblockarcanedba:fizzle", 0.7f, 1.0f);
            return;
        }
        if (def.kiDrainTicks() > 0) {
            // Discrete drain: the full price must be on hand at fire, then
            // it leaves in equal parts over the window (tickRuntime).
            runtime(player).kiDrains.add(new double[] {price / def.kiDrainTicks(), def.kiDrainTicks()});
        } else {
            accessor.dba$addKi(-price);
        }
        accessor.dba$pauseKiRecovery(20);
        accessor.dba$syncStats();

        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        UUID uuid = player.getUUID();
        Runtime rt = runtime(player);
        LOCKOUT.put(uuid, now + def.phases().recovery());
        if (def.charges() > 1) {
            ChargeState cs = rt.charges.get(def.id());
            if (cs != null && cs.count > 0) {
                cs.count--;
                if (cs.nextRegenTick <= 0 && cs.regenTicks > 0) {
                    cs.nextRegenTick = now + cs.regenTicks;
                }
            }
        } else {
            COOLDOWNS.computeIfAbsent(uuid, k -> new HashMap<>())
                    .put(def.id(), now + def.phases().recovery() + def.cooldownTicks());
        }

        int seed = level.getRandom().nextInt();

        // Move module: instant modes run now, timed modes register a
        // MoveState and the carrier waits for the move to finish.
        Vec3 departure = null;
        boolean timedMove = false;
        if (def.move() != null) {
            switch (def.move().mode()) {
                case "step", "warp" -> {
                    departure = player.position();
                    doStepWarp(player, def);
                    AbilityFxService.send(player, def, AbilityDef.FxEvent.FIRE,
                            departure.add(0, 1.0, 0), player.getLookAngle(),
                            seed ^ 0x57E9, power, 1);
                }
                case "launch" -> {
                    // Vertical launch plus an optional push along the horizontal aim.
                    // Synced through MovementPhysics: a bare setDeltaMovement on a
                    // server player never reaches the client.
                    Vec3 vel = player.getDeltaMovement();
                    double fwd = def.move().forward();
                    Vec3 look = player.getLookAngle();
                    Vec3 flat = new Vec3(look.x, 0, look.z);
                    Vec3 push = fwd > 0 && flat.lengthSqr() > 1.0E-6 ? flat.normalize().scale(fwd) : Vec3.ZERO;
                    com.dragonblockarcanedba.movement.MovementPhysics.setVelocity(player,
                            new Vec3(vel.x + push.x, def.move().distance() * 0.09, vel.z + push.z));
                }
                case "dash", "chain", "slam", "rush" -> {
                    startMove(player, rt, def, power, seed);
                    timedMove = true;
                }
                default -> {
                }
            }
        }

        // Melee module: an instant cone hit at the fire tick.
        if (def.melee() != null) {
            resolveMelee(player, def, power);
        }

        // Carrier: skipped for timed moves (finishMove spawns it) and
        // combo-gated until the combo is armed.
        if (!timedMove && def.carrier() != AbilityDef.Carrier.NONE) {
            boolean gated = def.combo() != null && def.combo().carrierOnComplete();
            boolean spawn = true;
            if (gated) {
                ComboState combo = rt.combos.get(def.id());
                if (combo != null && combo.armed) {
                    combo.armed = false;
                    combo.count = 0;
                } else {
                    spawn = false;
                }
            }
            if (spawn) {
                if (def.carrier() == AbilityDef.Carrier.PROJECTILE
                        && def.volley() != null && def.volley().count() > 1) {
                    spawnVolley(player, rt, def, power, now);
                } else {
                    Entity spawned = spawnCarrier(player, def, power, null, player.getYRot());
                    if (spawned instanceof SwarmHostEntity host) {
                        SwarmState state = new SwarmState();
                        state.host = host;
                        state.defId = def.id();
                        state.deployTick = now;
                        rt.swarm = state;
                    }
                }
                // Step and warp echo a cloud carrier at the departure point.
                // The fire models dressed the destination; the departure
                // gets the def's step sets (origin rifts) instead.
                if (departure != null && def.carrier() == AbilityDef.Carrier.CLOUD) {
                    spawnCarrierInternal(player, def, power, departure, player.getYRot());
                }
                if (departure != null && !def.models().isEmpty()) {
                    ModelDisplaySpawner.spawnForStep(player, def, departure, player.getYRot(), seed ^ 0x5EED);
                }
            }
        }

        // Summons at the fire moment.
        for (AbilityDef.SummonSpec summon : def.summons()) {
            if ("fire".equals(summon.when())) {
                executeSummon(player, def, summon, power, null, null);
            } else if ("fullPower".equals(summon.when()) && power >= 0.99f) {
                executeSummon(player, def, summon, power, null, null);
            }
        }

        playSoundId(player, def.sounds() != null ? def.sounds().fire() : null, 1.0f, 1.0f);
        broadcastCast(player, def, CastSyncPayload.ACTION_FIRE, now);
        AbilityFxService.send(player, def, AbilityDef.FxEvent.FIRE,
                player.position().add(0, 1.0, 0), player.getLookAngle(), seed, power, 1);
        AbilityFeedbackService.fire(player, def);
    }

    /**
     * Spawns one def's carrier for a player. pos overrides the spawn
     * position when non-null (wave and swarm ground-snap to it, cloud
     * uses it as its anchor, beam and projectile ignore it and ride
     * the caster). Returns the spawned entity, or null for carrier
     * NONE. This is the one carrier spawn path: fire(), moves,
     * channels, volleys, and summons all go through it.
     */
    public static Entity spawnCarrier(ServerPlayer player, AbilityDef def, float power, Vec3 pos, float yaw) {
        Entity carrier = spawnCarrierInternal(player, def, power, pos, yaw);
        // Models Pass: a def's "models" block dresses its carrier
        // spawn with the generic modeled presentation set. This is
        // the one carrier spawn path, so casts, summons, moves,
        // and channels all dress identically.
        if (!def.models().isEmpty()) {
            ModelDisplaySpawner.spawnForFire(player, def, carrier, power,
                    ((ServerLevel) player.level()).getRandom().nextInt());
        }
        return carrier;
    }

    private static Entity spawnCarrierInternal(ServerPlayer player, AbilityDef def, float power, Vec3 pos, float yaw) {
        ServerLevel level = (ServerLevel) player.level();
        int seed = level.getRandom().nextInt();
        UUID uuid = player.getUUID();
        switch (def.carrier()) {
            case WAVE -> {
                WaveCarrierEntity wave = new WaveCarrierEntity(DbaEntities.ABILITY_WAVE, level);
                Vec3 at = pos != null ? pos : player.position();
                int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                        (int) Math.floor(at.x), (int) Math.floor(at.z));
                wave.setPos(at.x, gy, at.z);
                wave.setYRot(yaw);
                wave.init(def, seed, power, uuid);
                level.addFreshEntity(wave);
                return wave;
            }
            case CLOUD -> {
                CloudCarrierEntity cloud = new CloudCarrierEntity(DbaEntities.ABILITY_CLOUD, level);
                cloud.setYRot(yaw);
                Vec3 anchor;
                UUID targetUuid = null;
                if (pos != null) {
                    anchor = pos;
                } else if (def.cloud() != null && "entity".equals(def.cloud().anchor())) {
                    LivingEntity target = raycastEntity(player, 40.0, 3.0);
                    if (target != null) {
                        targetUuid = target.getUUID();
                        anchor = target.position();
                    } else {
                        anchor = anchorFor(player, def);
                    }
                } else {
                    anchor = anchorFor(player, def);
                }
                cloud.setPos(anchor.x, anchor.y, anchor.z);
                if (targetUuid != null) {
                    cloud.setTargetUuid(targetUuid);
                }
                cloud.init(def, seed, power, uuid);
                level.addFreshEntity(cloud);
                return cloud;
            }
            case PROJECTILE -> {
                return spawnProjectile(player, def, power, yaw, player.getXRot());
            }
            case BEAM -> {
                BeamCarrierEntity beam = new BeamCarrierEntity(DbaEntities.ABILITY_BEAM, level);
                beam.setPos(player.getEyePosition());
                beam.setYRot(yaw);
                beam.setXRot(player.getXRot());
                beam.init(def, seed, power, uuid);
                level.addFreshEntity(beam);
                return beam;
            }
            case SWARM -> {
                SwarmHostEntity host = new SwarmHostEntity(DbaEntities.ABILITY_SWARM_HOST, level);
                Vec3 at = pos != null ? pos : player.position();
                int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                        (int) Math.floor(at.x), (int) Math.floor(at.z));
                host.setPos(at.x, gy, at.z);
                host.setYRot(yaw);
                host.init(def, seed, power, uuid);
                level.addFreshEntity(host);
                host.spawnShards(level);
                if (def.swarm() != null && def.swarm().slamRadius() > 0) {
                    AbilityDamage.applyAreaHit(level, def, host, player, host.position(),
                            def.swarm().slamRadius(), power, player);
                }
                return host;
            }
            case NONE -> {
                return null;
            }
        }
        return null;
    }

    private static ProjectileCarrierEntity spawnProjectile(ServerPlayer player, AbilityDef def,
                                                           float power, float yaw, float pitch) {
        ServerLevel level = (ServerLevel) player.level();
        ProjectileCarrierEntity bolt =
                new ProjectileCarrierEntity(DbaEntities.ABILITY_PROJECTILE, level);
        bolt.setOwner(player);
        bolt.setPos(player.getEyePosition());
        bolt.init(def, level.getRandom().nextInt(), power);
        bolt.shootFromRotation(player, pitch, yaw, 0.0f,
                (float) def.projectile().speed(), 0.0f);
        level.addFreshEntity(bolt);
        return bolt;
    }

    private static void spawnVolley(ServerPlayer player, Runtime rt, AbilityDef def, float power, long now) {
        AbilityDef.VolleySpec volley = def.volley();
        int count = volley.count();
        for (int i = 0; i < count; i++) {
            float offset = count == 1 ? 0f
                    : (float) (-volley.spreadDegrees() / 2.0
                    + volley.spreadDegrees() * ((double) i / (count - 1)));
            if (i == 0 || volley.delayTicks() == 0) {
                spawnProjectile(player, def, power, player.getYRot() + offset, player.getXRot());
            } else {
                rt.pendingByTick.put(now + (long) volley.delayTicks() * i,
                        new PendingSpawn(def, power, offset));
            }
        }
    }

    /**
     * Executes one summons entry: spawns the summoned def's carrier
     * at the caster, at the look anchor, or at a hit position.
     * Summons never cascade: spawnCarrier executes no summons, and
     * this method is called only from primary casts and the wave's
     * carrierHit hook.
     */
    public static void executeSummon(ServerPlayer caster, AbilityDef sourceDef,
                                     AbilityDef.SummonSpec spec, float power,
                                     Vec3 hitPos, LivingEntity hitEntity) {
        if (spec == null) {
            return;
        }
        Identifier id = Identifier.tryParse(spec.ability());
        if (id == null) {
            return;
        }
        AbilityDef summoned = AbilityRegistry.get(id);
        if (summoned == null) {
            return;
        }
        switch (spec.at()) {
            case "hit" -> {
                if (hitPos != null) {
                    spawnCarrier(caster, summoned, power, hitPos, caster.getYRot());
                }
            }
            case "look" -> {
                if (summoned.carrier() == AbilityDef.Carrier.CLOUD) {
                    spawnCarrier(caster, summoned, power, anchorFor(caster, summoned), caster.getYRot());
                } else {
                    spawnCarrier(caster, summoned, power, caster.position(), caster.getYRot());
                }
            }
            default -> spawnCarrier(caster, summoned, power, caster.position(), caster.getYRot());
        }
    }

    /** The melee module's cone resolution at the fire tick. */
    private static void resolveMelee(ServerPlayer player, AbilityDef def, float power) {
        ServerLevel level = (ServerLevel) player.level();
        AbilityDef.MeleeSpec melee = def.melee();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB box = new AABB(eye, eye).inflate(melee.range());
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != player);
        candidates.sort(java.util.Comparator.comparingDouble(e -> e.position().distanceToSqr(eye)));
        boolean any = false;
        LivingEntity firstHit = null;
        Vec3 firstHitPos = null;
        for (LivingEntity target : candidates) {
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(eye);
            double dist = to.length();
            if (dist > melee.range() + target.getBbWidth() * 0.5) {
                continue;
            }
            if (melee.arcDegrees() < 360 && dist > 1.0e-4) {
                double cos = Math.max(-1, Math.min(1, look.dot(to.scale(1.0 / dist))));
                if (Math.toDegrees(Math.acos(cos)) > melee.arcDegrees() / 2.0) {
                    continue;
                }
            }
            AbilityDamage.applyHit(level, def, player, player, target,
                    power, melee.range() <= 0 ? 0 : dist / melee.range(), true);
            if (!any) {
                firstHit = target;
                firstHitPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
            }
            any = true;
            if (melee.arcDegrees() < 360) {
                break; // a narrow arc hits only the single closest valid target
            }
        }
        if (any) {
            int seed = level.getRandom().nextInt();
            AbilityFxService.send(player, def, AbilityDef.FxEvent.CARRIER_HIT,
                    firstHitPos, look, seed, power, 1);
            AbilityFeedbackService.carrier(player, def, AbilityDef.FxEvent.CARRIER_HIT, firstHitPos);
            for (AbilityDef.SummonSpec summon : def.summons()) {
                if ("meleeHit".equals(summon.when())) {
                    executeSummon(player, def, summon, power, firstHitPos, firstHit);
                }
            }
        }
    }

    /** Instant move modes: step (short clip teleport) and warp (long look teleport). */
    private static void doStepWarp(ServerPlayer player, AbilityDef def) {
        ServerLevel level = (ServerLevel) player.level();
        AbilityDef.MoveSpec move = def.move();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 dest;
        if ("step".equals(move.mode())) {
            Vec3 end = eye.add(look.scale(move.distance()));
            BlockHitResult hit = level.clip(new ClipContext(eye, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3 stop = hit.getLocation().add(look.scale(-0.5));
                dest = new Vec3(stop.x, stop.y - player.getEyeHeight(), stop.z);
            } else {
                dest = new Vec3(player.getX() + look.x * move.distance(),
                        player.getY() + look.y * move.distance(),
                        player.getZ() + look.z * move.distance());
            }
        } else {
            double maxDist = move.maxDistance();
            Vec3 end = eye.add(look.scale(maxDist));
            BlockHitResult hit = level.clip(new ClipContext(eye, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                var pos = hit.getBlockPos();
                int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
                dest = new Vec3(pos.getX() + 0.5, gy, pos.getZ() + 0.5);
            } else {
                double dist = Math.min(maxDist, 128);
                dest = new Vec3(player.getX() + look.x * dist,
                        player.getY() + look.y * dist,
                        player.getZ() + look.z * dist);
            }
        }
        player.teleportTo(level, dest.x, dest.y, dest.z,
                java.util.Collections.emptySet(), player.getYRot(), player.getXRot(), false);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
    }

    private static void startMove(ServerPlayer player, Runtime rt, AbilityDef def, float power, int seed) {
        MoveState move = new MoveState();
        move.def = def;
        move.power = power;
        move.mode = def.move().mode();
        move.startTick = player.level().getGameTime();
        move.seed = seed;
        move.startedAirborne = !player.onGround();
        switch (move.mode) {
            case "dash" -> {
                Vec3 look = player.getLookAngle();
                Vec3 horiz = new Vec3(look.x, 0, look.z);
                if (horiz.lengthSqr() < 1.0e-4) {
                    double yawRad = Math.toRadians(player.getYRot());
                    horiz = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
                }
                horiz = horiz.normalize();
                move.dashDirX = horiz.x;
                move.dashDirZ = horiz.z;
                double dashDistance = def.move().distance();
                if (def.chargeRelease() && def.minPower() > 0) {
                    // A charged dash travels farther the longer it was
                    // charged: distance scales from the minPower fraction
                    // at a tap to full distance at full power (Heroic
                    // Dash: 8 to 24 blocks from distance 24, minPower 0.34).
                    dashDistance *= def.minPower() + (1.0 - def.minPower()) * power;
                }
                move.ticksLeft = Math.max(1, (int) Math.ceil(dashDistance / 2.2));
            }
            case "slam" -> {
                move.ticksLeft = 80;
                player.setDeltaMovement(player.getLookAngle().scale(1.6));
            }
            case "rush" -> move.ticksLeft = Math.max(1, def.phases().active());
            default -> move.ticksLeft = 1;
        }
        rt.move = move;
    }

    private static void tickMove(ServerPlayer player, Runtime rt) {
        MoveState move = rt.move;
        AbilityDef def = move.def;
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        switch (move.mode) {
            case "dash" -> {
                Vec3 vel = player.getDeltaMovement();
                player.setDeltaMovement(move.dashDirX * 2.2, vel.y, move.dashDirZ * 2.2);
                if (def.move().damageOnPass()) {
                    damageOnPass(player, def, move, false);
                }
                move.ticksLeft--;
                if (move.ticksLeft <= 0) {
                    finishMove(player, rt, true);
                }
            }
            case "chain" -> {
                move.hopTimer++;
                if (move.hopTimer < def.move().stepTicks()) {
                    return;
                }
                move.hopTimer = 0;
                LivingEntity best = null;
                double bestDist = Double.MAX_VALUE;
                AABB box = new AABB(player.position(), player.position()).inflate(def.move().range());
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                        en -> en.isAlive() && en != player && !move.visited.contains(en.getUUID()))) {
                    double dist = e.position().distanceTo(player.position());
                    if (dist <= def.move().range() && dist < bestDist) {
                        bestDist = dist;
                        best = e;
                    }
                }
                if (best == null) {
                    finishMove(player, rt, true);
                    return;
                }
                Vec3 prev = player.position();
                // step sets (Flashdraw afterimages) stay where the hop leaves
                ModelDisplaySpawner.spawnForStep(player, def, prev, player.getYRot(),
                        move.seed ^ (move.hopsDone * 977));
                Vec3 targetPos = best.position();
                Vec3 away = new Vec3(prev.x - targetPos.x, 0, prev.z - targetPos.z);
                if (away.lengthSqr() < 1.0e-4) {
                    Vec3 look = player.getLookAngle();
                    away = new Vec3(-look.x, 0, -look.z);
                }
                away = away.normalize();
                Vec3 dest = new Vec3(targetPos.x + away.x * 1.3, targetPos.y,
                        targetPos.z + away.z * 1.3);
                double dx = targetPos.x - dest.x;
                double dz = targetPos.z - dest.z;
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                player.teleportTo(level, dest.x, dest.y, dest.z,
                        java.util.Collections.emptySet(), yaw, player.getXRot(), false);
                player.setDeltaMovement(Vec3.ZERO);
                player.resetFallDistance();
                if (def.move().damageOnPass()) {
                    AbilityDamage.applyHit(level, def, player, player, best, move.power, 0, true);
                }
                AbilityFxService.send(player, def, AbilityDef.FxEvent.FIRE,
                        targetPos.add(0, 1.0, 0), new Vec3(dx, 0, dz),
                        move.seed ^ move.hopsDone, move.power, 1);
                move.visited.add(best.getUUID());
                move.hopsDone++;
                if (move.hopsDone >= def.move().targets()) {
                    finishMove(player, rt, true);
                }
            }
            case "slam" -> {
                if (player.onGround() || now - move.startTick >= 80) {
                    finishMove(player, rt, true);
                }
            }
            case "rush" -> {
                Vec3 look = player.getLookAngle();
                Vec3 vel = look.scale(1.5);
                if (look.y < 0 && !player.onGround()) {
                    vel = new Vec3(vel.x, Math.max(vel.y, -0.1), vel.z);
                }
                player.setDeltaMovement(vel);
                player.resetFallDistance();
                move.ticksLeft--;
                if (move.startedAirborne && player.onGround()) {
                    finishMove(player, rt, true);
                } else if (move.ticksLeft <= 0) {
                    finishMove(player, rt, false);
                }
            }
            default -> finishMove(player, rt, true);
        }
    }

    /** Dash damageOnPass: each newly passed entity takes one carrier-kind hit. */
    private static void damageOnPass(ServerPlayer player, AbilityDef def, MoveState move, boolean meleeKind) {
        ServerLevel level = (ServerLevel) player.level();
        AABB box = new AABB(player.position(), player.position()).inflate(1.7).expandTowards(0, 2, 0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                en -> en.isAlive() && en != player && !move.visited.contains(en.getUUID()))) {
            move.visited.add(e.getUUID());
            AbilityDamage.applyHit(level, def, player, player, e, move.power, 0, meleeKind);
            AbilityFxService.send(player, def, AbilityDef.FxEvent.CARRIER_HIT,
                    player.position().add(0, 1.0, 0), player.getLookAngle(),
                    move.seed ^ e.getId(), move.power, 1);
        }
    }

    /** Chain lineUp: snap every chained target onto one line along the caster's aim, ready for the finisher. */
    private static void lineUpChained(ServerPlayer player, MoveState move) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0e-4) {
            return;
        }
        flat = flat.normalize();
        int slot = 0;
        for (java.util.UUID id : move.visited) {
            if (!(level.getEntity(id) instanceof LivingEntity e) || !e.isAlive()) {
                continue;
            }
            double d = 2.5 + slot * 1.6;
            Vec3 to = new Vec3(player.getX() + flat.x * d, player.getY(), player.getZ() + flat.z * d);
            AABB moved = e.getBoundingBox().move(to.subtract(e.position()));
            if (!level.noCollision(e, moved)) {
                to = new Vec3(to.x, e.getY(), to.z);
                if (!level.noCollision(e, e.getBoundingBox().move(to.subtract(e.position())))) {
                    continue;
                }
            }
            e.teleportTo(to.x, to.y, to.z);
            e.setDeltaMovement(Vec3.ZERO);
            com.dragonblockarcanedba.util.DbaCompat.markHurt(e);
            slot++;
        }
    }

    /** A timed move ends: its pending carrier fires here (unless a rush timed out unlanded). */
    private static void finishMove(ServerPlayer player, Runtime rt, boolean fireCarrier) {
        MoveState move = rt.move;
        if (move == null) {
            return;
        }
        rt.move = null;
        AbilityDef def = move.def;
        if (fireCarrier && "chain".equals(def.move().mode()) && def.move().lineUp()) {
            lineUpChained(player, move);
        }
        if (!fireCarrier || def.carrier() == AbilityDef.Carrier.NONE) {
            return;
        }
        if (def.combo() != null && def.combo().carrierOnComplete()) {
            ComboState combo = rt.combos.get(def.id());
            if (combo == null || !combo.armed) {
                return;
            }
            combo.armed = false;
            combo.count = 0;
        }
        Entity spawned = spawnCarrier(player, def, move.power, player.position(), player.getYRot());
        if (spawned instanceof SwarmHostEntity host) {
            SwarmState state = new SwarmState();
            state.host = host;
            state.defId = def.id();
            state.deployTick = player.level().getGameTime();
            rt.swarm = state;
        }
    }

    /** Corridor raycast for living entities (anchor entity, swarm marking). */
    private static LivingEntity raycastEntity(ServerPlayer player, double maxDist, double corridor) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB box = new AABB(eye, eye.add(look.scale(maxDist))).inflate(corridor);
        LivingEntity best = null;
        double bestAlong = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                en -> en.isAlive() && en != player)) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
            double along = to.dot(look);
            if (along < 0 || along > maxDist) {
                continue;
            }
            double perp = to.subtract(look.scale(along)).length();
            if (perp > corridor + e.getBbWidth() * 0.5) {
                continue;
            }
            if (along < bestAlong) {
                bestAlong = along;
                best = e;
            }
        }
        return best;
    }

    /**
     * Tells the caster and everyone tracking them about a cast
     * lifecycle transition, for the client animation layer. Only
     * abilities whose data carries an animation block produce
     * traffic; the packet is the whole signal (see CastSyncPayload
     * for why start, fire, and cancel are each necessary).
     */
    private static void broadcastCast(ServerPlayer player, AbilityDef def, int action, long tick) {
        if (def == null || def.animation() == null) {
            return;
        }
        broadcastCastRaw(player, def.id(), action, tick);
    }

    private static void broadcastCastRaw(ServerPlayer player, Identifier abilityId, int action, long tick) {
        CastSyncPayload payload = new CastSyncPayload(
                player.getId(), abilityId.toString(), action, tick);
        for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(viewer, payload);
        }
        ServerPlayNetworking.send(player, payload);
    }

    private static Vec3 anchorFor(ServerPlayer player, AbilityDef def) {
        ServerLevel level = (ServerLevel) player.level();
        if (def.cloud() != null && "target".equals(def.cloud().anchor())) {
            HitResult hit = player.pick(40.0, 1.0f, false);
            if (hit instanceof BlockHitResult blockHit) {
                var pos = blockHit.getBlockPos();
                int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
                return new Vec3(pos.getX() + 0.5, gy, pos.getZ() + 0.5);
            }
        }
        if (def.cloud() != null && "entity".equals(def.cloud().anchor())) {
            LivingEntity target = raycastEntity(player, 40.0, 3.0);
            if (target != null) {
                return target.position();
            }
        }
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                (int) Math.floor(player.getX()), (int) Math.floor(player.getZ()));
        return new Vec3(player.getX(), gy, player.getZ());
    }

    private static AbilityDef heldAbility(ServerPlayer player, AbilityDef.Slot slot) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return null;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return AbilityRegistry.forItem(itemId, slot);
    }

    private static void playSoundId(ServerPlayer player, String soundId, float volume, float pitch) {
        if (soundId == null || soundId.isEmpty()) {
            return;
        }
        playSound(player, soundId, volume, pitch);
    }

    private static void playSound(ServerPlayer player, String soundId, float volume, float pitch) {
        Identifier id = Identifier.tryParse(soundId);
        if (id == null) {
            return;
        }
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(id);
        if (event != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    event, SoundSource.PLAYERS, volume, pitch);
        }
    }

    /** Drops cooldown entries that have expired, called rarely from datapack reload paths. */
    public static void prune() {
        Iterator<Map.Entry<UUID, Map<Identifier, Long>>> it = COOLDOWNS.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isEmpty()) {
                it.remove();
            }
        }
    }

    /** True while the player holds an uncommitted cast (charge or startup) of this def. */
    public static boolean isCasting(UUID playerId, Identifier defId) {
        Cast cast = CASTS.get(playerId);
        return cast != null && cast.abilityId().equals(defId);
    }
}
