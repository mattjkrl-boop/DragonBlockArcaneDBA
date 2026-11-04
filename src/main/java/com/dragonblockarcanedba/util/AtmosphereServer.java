package com.dragonblockarcanedba.util;

import com.dragonblockarcanedba.network.AtmospherePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Server side atmosphere. Bespoke pass (BESPOKE-STANDARD rule 5): a weapon
 * storm is a tracked volume, nothing more. The server keeps the storm list,
 * merges overlaps, expires storms, and rebroadcasts volumes to nearby
 * clients; AtmosphereClient renders the rain, thunder, and gloom locally.
 * The world's WeatherData is NEVER read or written by a weapon again, so
 * there is no weather to save or restore: expiry just ends the broadcast.
 * Kind follows the weapon: THUNDER for electric and tempest themes, RAIN
 * for wind and water themes.
 */
public final class AtmosphereServer {
    private AtmosphereServer() {}

    public enum Kind { RAIN, THUNDER }

    private static final class Storm {
        final Vec3 center;
        final float radius;
        final float gloom;
        final Kind kind;
        long expiry;
        int broadcastIn;
        Storm(Vec3 center, float radius, float gloom, Kind kind, long expiry) {
            this.center = center; this.radius = radius; this.gloom = gloom;
            this.kind = kind; this.expiry = expiry;
        }
    }

    private static final Map<ServerLevel, List<Storm>> ACTIVE = new HashMap<>();

    /** Start or extend an atmosphere zone. Overlapping storms of the same kind merge. */
    public static void begin(ServerLevel level, Vec3 center, float radius, float gloom, Kind kind, int ticks) {
        List<Storm> storms = ACTIVE.computeIfAbsent(level, k -> new ArrayList<>());
        long expiry = level.getGameTime() + Math.max(20, ticks);
        for (Storm s : storms) {
            if (s.center.distanceToSqr(center) < 4.0 && s.kind == kind) {
                s.expiry = Math.max(s.expiry, expiry);
                return;
            }
        }
        Storm storm = new Storm(center, radius, gloom, kind, expiry);
        storms.add(storm);
        broadcast(level, storm, Math.max(20, ticks));
    }

    private static void broadcast(ServerLevel level, Storm storm, int ticks) {
        AtmospherePayload payload = new AtmospherePayload(
            storm.center.x, storm.center.y, storm.center.z, storm.radius, storm.gloom, ticks,
            storm.kind == Kind.THUNDER ? AtmospherePayload.KIND_THUNDER : AtmospherePayload.KIND_RAIN);
        for (ServerPlayer player : PlayerLookup.around(level, storm.center, storm.radius + 48.0)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(AtmosphereServer::tickServer);
    }

    private static void tickServer(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        Iterator<Map.Entry<ServerLevel, List<Storm>>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ServerLevel, List<Storm>> entry = it.next();
            ServerLevel level = entry.getKey();
            long now = level.getGameTime();
            List<Storm> storms = entry.getValue();
            storms.removeIf(s -> s.expiry <= now);
            if (storms.isEmpty()) {
                it.remove();
                continue;
            }
            // Refresh the client storm volumes before they expire client side.
            for (Storm s : storms) {
                if (--s.broadcastIn <= 0) {
                    s.broadcastIn = 30;
                    broadcast(level, s, (int) Math.min(Integer.MAX_VALUE, s.expiry - now));
                }
            }
        }
    }
}
