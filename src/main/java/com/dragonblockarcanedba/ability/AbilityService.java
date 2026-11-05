package com.dragonblockarcanedba.ability;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.entity.CloudCarrierEntity;
import com.dragonblockarcanedba.entity.DbaEntities;
import com.dragonblockarcanedba.entity.ProjectileCarrierEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side execution for every ability: the only place casts are
 * started, charged, fired, cooled down, and charged for. State is
 * two maps (one active cast per player; per-ability cooldown end
 * times plus a shared recovery lockout), keyed by player UUID
 * against level game time. Ability identity always comes from the
 * item actually in the player's main hand through AbilityRegistry;
 * the click payload only says which button changed.
 */
public final class AbilityService {
    private AbilityService() {}

    private record Cast(Identifier abilityId, long startTick, boolean chargeRelease) {}

    private static final Map<UUID, Cast> CASTS = new HashMap<>();
    private static final Map<UUID, Map<Identifier, Long>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> LOCKOUT = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayer(player);
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
        AbilityDef def = heldAbility(player, slot);
        if (def == null) {
            return;
        }
        long now = player.level().getGameTime();
        UUID uuid = player.getUUID();

        // Starting anything cancels a charge in progress, silently.
        Cast replaced = CASTS.remove(uuid);
        if (replaced != null) {
            broadcastCast(player, AbilityRegistry.get(replaced.abilityId()),
                    CastSyncPayload.ACTION_CANCEL, now);
        }

        if (now < LOCKOUT.getOrDefault(uuid, 0L)) {
            return;
        }
        Map<Identifier, Long> cds = COOLDOWNS.get(uuid);
        if (cds != null && now < cds.getOrDefault(def.id(), 0L)) {
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
        AbilityFxService.send(player, def, AbilityDef.FxEvent.CAST_START,
                player.position().add(0, 1.0, 0), player.getLookAngle(),
                player.level().getRandom().nextInt(), 1.0f, 1);
        playSoundId(player, def.sounds() != null ? def.sounds().charge() : null, 0.8f, 1.0f);
    }

    private static void onRelease(ServerPlayer player, AbilityDef.Slot slot, int heldTicks) {
        UUID uuid = player.getUUID();
        Cast cast = CASTS.get(uuid);
        if (cast == null || !cast.chargeRelease()) {
            return;
        }
        AbilityDef def = heldAbility(player, slot);
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
        fire(player, def, power);
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

        if (!cast.chargeRelease() && elapsed >= def.phases().startup()) {
            CASTS.remove(uuid);
            fire(player, def, 1.0f);
        }
    }

    private static void fire(ServerPlayer player, AbilityDef def, float power) {
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        double price = def.kiPrice(PlayerStats.getMaxKi(player));
        if (accessor.dba$getCurrentKi() < price) {
            playSound(player, "dragonblockarcanedba:fizzle", 0.7f, 1.0f);
            return;
        }
        accessor.dba$addKi(-price);
        accessor.dba$pauseKiRecovery(20);
        accessor.dba$syncStats();

        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        UUID uuid = player.getUUID();
        LOCKOUT.put(uuid, now + def.phases().recovery());
        COOLDOWNS.computeIfAbsent(uuid, k -> new HashMap<>())
                .put(def.id(), now + def.phases().recovery() + def.cooldownTicks());

        int seed = level.getRandom().nextInt();
        switch (def.carrier()) {
            case WAVE -> {
                WaveCarrierEntity wave = new WaveCarrierEntity(DbaEntities.ABILITY_WAVE, level);
                int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                        (int) Math.floor(player.getX()), (int) Math.floor(player.getZ()));
                wave.setPos(player.getX(), gy, player.getZ());
                wave.setYRot(player.getYRot());
                wave.init(def, seed, power, uuid);
                level.addFreshEntity(wave);
            }
            case CLOUD -> {
                CloudCarrierEntity cloud = new CloudCarrierEntity(DbaEntities.ABILITY_CLOUD, level);
                Vec3 anchor = anchorFor(player, def);
                cloud.setPos(anchor.x, anchor.y, anchor.z);
                cloud.init(def, seed, power, uuid);
                level.addFreshEntity(cloud);
            }
            case PROJECTILE -> {
                ProjectileCarrierEntity bolt =
                        new ProjectileCarrierEntity(DbaEntities.ABILITY_PROJECTILE, level);
                bolt.setOwner(player);
                bolt.setPos(player.getEyePosition());
                bolt.init(def, seed, power);
                bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f,
                        (float) def.projectile().speed(), 0.0f);
                level.addFreshEntity(bolt);
            }
        }
        playSoundId(player, def.sounds() != null ? def.sounds().fire() : null, 1.0f, 1.0f);
        broadcastCast(player, def, CastSyncPayload.ACTION_FIRE, now);
        AbilityFxService.send(player, def, AbilityDef.FxEvent.FIRE,
                player.position().add(0, 1.0, 0), player.getLookAngle(), seed, power, 1);
        AbilityFeedbackService.fire(player, def);
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
}
