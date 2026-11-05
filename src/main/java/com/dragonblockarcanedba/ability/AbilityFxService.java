package com.dragonblockarcanedba.ability;

import com.dragonblockarcanedba.network.AbilityFxPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Server side of the seeded effect protocol (Rebuild Phase 3): the
 * one place AbilityFxPayloads are built and sent. A send happens
 * only when the definition actually authors recipes for the event,
 * so abilities without fx data produce no traffic at all, the same
 * discipline CastSyncPayload follows. Sends go to every player
 * tracking the anchor entity, plus the anchor itself when it is a
 * player (PlayerLookup.tracking excludes the tracked player).
 *
 * This class moves no damage, cost, cooldown, or timing: it is
 * called from the exact ticks the events already happened on in
 * Phases 1 and 2, and it only describes them to clients.
 */
public final class AbilityFxService {
    private AbilityFxService() {}

    /**
     * Sends one event packet anchored on an entity (a caster for
     * cast events, a carrier for carrier events). repeat aggregates
     * same-tick repetitions (wave hits); clients expand the burst
     * once per repeat with seed + r.
     */
    public static void send(Entity anchor, AbilityDef def, AbilityDef.FxEvent event,
                            Vec3 origin, Vec3 dir, int seed, float power, int repeat) {
        if (anchor == null || def == null || !def.hasFx(event)) {
            return;
        }
        AbilityFxPayload payload = new AbilityFxPayload(def.id().toString(), event.ordinal(),
                origin.x, origin.y, origin.z,
                (float) dir.x, (float) dir.y, (float) dir.z,
                seed, power, Math.max(1, repeat));
        for (ServerPlayer viewer : PlayerLookup.tracking(anchor)) {
            ServerPlayNetworking.send(viewer, payload);
        }
        if (anchor instanceof ServerPlayer self) {
            ServerPlayNetworking.send(self, payload);
        }
    }
}
