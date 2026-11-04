package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.network.BossStaggerPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

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
                emitImpact(context.client(), payload);
            }));
    }

    private static void emitImpact(Minecraft client, BossStaggerPayload payload) {
        if (client.level == null) return;
        Entity entity = client.level.getEntity(payload.entityId());
        if (entity == null) return;
        float strength = Math.max(0f, payload.strength());
        Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.55, 0);
        double radius = Math.max(0.35, entity.getBbWidth() * 0.55);
        int seed = payload.entityId() * 31 + 41;
        MaterialEmitters.burst(client.level,
                MaterialEmitters.Preset.dust(0xC2B49A, 0.9f, seed, new Vec3(0, 0.04, 0), 0.16f),
                MaterialEmitters.sphereVolume(center, radius),
                10 + Math.round(8f * strength));
        MaterialEmitters.burst(client.level,
                MaterialEmitters.Preset.plasma(0xFFCA6A, 0.9f, seed + 1, Vec3.ZERO, 0.22f),
                MaterialEmitters.sphereVolume(center, radius),
                6 + Math.round(6f * strength));
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
