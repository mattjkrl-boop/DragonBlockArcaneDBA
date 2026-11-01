package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.network.DeathTumblePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client store for ragdoll-lite death tumbles. The server broadcasts a
 * DeathTumblePayload when an entity dies to a recent DBA hit; the renderer
 * reads strengths from here while the corpse plays its death animation.
 * Entries age out after 30 ticks, longer than any death animation.
 */
public final class TumbleFx {
    private TumbleFx() {}

    private static final class Entry {
        float strength;
        int ticksLeft = 30;
    }

    private static final Map<Integer, Entry> BY_ID = new HashMap<>();

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(DeathTumblePayload.TYPE, (payload, context) ->
            context.client().execute(() -> {
                Entry e = new Entry();
                e.strength = payload.strength();
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
}
