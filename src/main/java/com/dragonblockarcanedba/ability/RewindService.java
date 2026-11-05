package com.dragonblockarcanedba.ability;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 6 rewind service (module M11): a light global position
 * history. Every 4 ticks, every living entity within 48 blocks of a
 * player is sampled (game time, position, rotation), keeping the
 * newest 30 samples per entity. rewind() snaps an entity back to the
 * newest sample at least the requested ticks old. Position only:
 * health, effects, and inventory are never rewound.
 */
public final class RewindService {
    private RewindService() {}

    private record Sample(long gameTime, double x, double y, double z, float yRot, float xRot) {}

    private static final class History {
        final ArrayDeque<Sample> samples = new ArrayDeque<>();
        long lastRefresh;
    }

    private static final Map<UUID, History> HISTORIES = new HashMap<>();
    private static boolean registered;
    private static long tickCounter;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter % 4 != 0) {
                return;
            }
            for (ServerLevel level : server.getAllLevels()) {
                long now = level.getGameTime();
                for (ServerPlayer player : level.players()) {
                    AABB box = player.getBoundingBox().inflate(48);
                    for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                            e -> e.isAlive())) {
                        History history = HISTORIES.computeIfAbsent(entity.getUUID(), k -> new History());
                        history.lastRefresh = tickCounter;
                        history.samples.addLast(new Sample(now, entity.getX(), entity.getY(), entity.getZ(),
                                entity.getYRot(), entity.getXRot()));
                        while (history.samples.size() > 30) {
                            history.samples.removeFirst();
                        }
                    }
                }
            }
            // Lazy pruning: entries not refreshed for 240 ticks are
            // dropped; dead entities simply stop being refreshed.
            long pruneNow = tickCounter;
            HISTORIES.entrySet().removeIf(en -> pruneNow - en.getValue().lastRefresh > 240);
        });
    }

    /**
     * Snaps the entity back by the given ticks of recorded history.
     * Returns false when no usable sample exists. A successful rewind
     * clears the entity's samples, so a second rift cannot chain
     * rewind the same stretch.
     */
    public static boolean rewind(LivingEntity entity, int ticks) {
        History history = HISTORIES.get(entity.getUUID());
        if (history == null || history.samples.isEmpty()) {
            return false;
        }
        long now = entity.level().getGameTime();
        long target = now - ticks;
        Sample chosen = null;
        Iterator<Sample> it = history.samples.descendingIterator();
        while (it.hasNext()) {
            Sample sample = it.next();
            if (sample.gameTime() <= target) {
                chosen = sample;
                break;
            }
        }
        if (chosen == null) {
            Sample oldest = history.samples.peekFirst();
            if (oldest == null || now - oldest.gameTime() < ticks / 2) {
                return false;
            }
            chosen = oldest;
        }
        entity.snapTo(chosen.x(), chosen.y(), chosen.z(), chosen.yRot(), chosen.xRot());
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0;
        history.samples.clear();
        return true;
    }
}
