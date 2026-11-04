package com.dragonblockarcanedba.util;

import com.dragonblockarcanedba.network.AtmospherePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Server side atmosphere (SPECTACLE-STANDARD point 4): weapon storms drive
 * REAL server weather for their life, with the prior weather saved and
 * restored when the last storm ends, plus a gloom broadcast so nearby
 * clients darken through AtmosphereClient. Weather kind follows the weapon:
 * THUNDER for electric and tempest themes, RAIN for wind and water themes.
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

    private static final class SavedWeather {
        int clearTime, rainTime, thunderTime;
        boolean raining, thundering;
    }

    private static final Map<ServerLevel, List<Storm>> ACTIVE = new HashMap<>();
    private static final Map<ServerLevel, SavedWeather> SAVED = new HashMap<>();

    /** Start or extend an atmosphere zone. Overlapping storms merge their weather; the strongest kind wins. */
    public static void begin(ServerLevel level, Vec3 center, float radius, float gloom, Kind kind, int ticks) {
        List<Storm> storms = ACTIVE.computeIfAbsent(level, k -> new ArrayList<>());
        long expiry = level.getGameTime() + Math.max(20, ticks);
        for (Storm s : storms) {
            if (s.center.distanceToSqr(center) < 4.0 && s.kind == kind) {
                s.expiry = Math.max(s.expiry, expiry);
                applyWeather(level);
                return;
            }
        }
        Storm storm = new Storm(center, radius, gloom, kind, expiry);
        storms.add(storm);
        applyWeather(level);
        broadcast(level, storm, Math.max(20, ticks));
    }

    private static void applyWeather(ServerLevel level) {
        List<Storm> storms = ACTIVE.get(level);
        if (storms == null || storms.isEmpty()) return;
        WeatherData data = level.getWeatherData();
        if (!SAVED.containsKey(level)) {
            SavedWeather saved = new SavedWeather();
            saved.clearTime = data.getClearWeatherTime();
            saved.rainTime = data.getRainTime();
            saved.thunderTime = data.getThunderTime();
            saved.raining = data.isRaining();
            saved.thundering = data.isThundering();
            SAVED.put(level, saved);
        }
        boolean thunder = storms.stream().anyMatch(s -> s.kind == Kind.THUNDER);
        data.setClearWeatherTime(0);
        data.setRaining(true);
        data.setRainTime(20 * 60);
        data.setThundering(thunder);
        data.setThunderTime(20 * 60);
    }

    private static void restoreWeather(ServerLevel level) {
        SavedWeather saved = SAVED.remove(level);
        if (saved == null) return;
        WeatherData data = level.getWeatherData();
        data.setClearWeatherTime(saved.clearTime);
        data.setRainTime(saved.rainTime);
        data.setThunderTime(saved.thunderTime);
        data.setRaining(saved.raining);
        data.setThundering(saved.thundering);
    }

    private static void broadcast(ServerLevel level, Storm storm, int ticks) {
        AtmospherePayload payload = new AtmospherePayload(
            storm.center.x, storm.center.y, storm.center.z, storm.radius, storm.gloom, ticks);
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
                restoreWeather(level);
                it.remove();
                continue;
            }
            // Keep weather timers fed while any storm lives, and refresh the
            // client gloom before it expires on their side.
            WeatherData data = level.getWeatherData();
            if (data.getRainTime() < 20 * 10) data.setRainTime(20 * 60);
            if (data.isThundering() && data.getThunderTime() < 20 * 10) data.setThunderTime(20 * 60);
            for (Storm s : storms) {
                if (--s.broadcastIn <= 0) {
                    s.broadcastIn = 30;
                    broadcast(level, s, (int) Math.min(Integer.MAX_VALUE, s.expiry - now));
                }
            }
        }
    }
}
