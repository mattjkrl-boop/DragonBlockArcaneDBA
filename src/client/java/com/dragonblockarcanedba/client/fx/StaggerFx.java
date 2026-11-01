package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.network.BossStaggerPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client store for boss staggers. The server broadcasts a BossStaggerPayload
 * when a boss-class entity takes a heavy DBA hit; the renderer reads strength
 * and progress from here while the stagger plays out. Mirrors TumbleFx.
 */
public final class StaggerFx {
    private StaggerFx() {}

    private static final class Entry {
        float strength;
        int duration;
        int ticksLeft;
    }

    private static final Map<Integer, Entry> BY_ID = new HashMap<>();

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(BossStaggerPayload.TYPE, (payload, context) ->
            context.client().execute(() -> {
                Entry e = new Entry();
                e.strength = payload.strength();
                e.duration = Math.max(1, payload.durationTicks());
                e.ticksLeft = e.duration;
                BY_ID.put(payload.entityId(), e);
            }));
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null) {
            BY_ID.clear();
            return;
        }
        Iterator<Entry> it = BY_ID.values().iterator();
        while (it.hasNext()) {
            if (--it.next().ticksLeft <= 0) it.remove();
        }
    }

    public static float strengthFor(int entityId) {
        Entry e = BY_ID.get(entityId);
        return e == null ? 0f : e.strength;
    }

    /** Elapsed fraction of the stagger, 0 at the hit and 1 when it ends. */
    public static float progressFor(int entityId) {
        Entry e = BY_ID.get(entityId);
        if (e == null) return 1f;
        return 1f - (e.ticksLeft / (float) e.duration);
    }
}
