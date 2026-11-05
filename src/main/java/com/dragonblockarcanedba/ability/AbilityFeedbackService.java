package com.dragonblockarcanedba.ability;

import com.dragonblockarcanedba.network.ScreenShakePayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Server side of the Phase 5 feedback service: the one place
 * ability screen shakes are sent. A send happens only when the
 * definition authors a shake for the event, exactly the discipline
 * AbilityFxService follows for recipes, and it happens on the same
 * tick the event already happens on: this class moves no damage,
 * cost, cooldown, timing, or carrier behavior.
 *
 * Each event sends ONE ScreenShakePayload per affected player: the
 * players inside the shake's radius of the event origin. The
 * payload carries the origin and radius; every client computes
 * its own distance-squared falloff from its own position and
 * arbitrates against its other active shakes (strongest wins), so
 * the server never needs per-player strengths. Ability hitstop is
 * deliberately not part of this service: see the Phase 5 report
 * for the designed-but-deferred field and the reasons.
 */
public final class AbilityFeedbackService {
    private AbilityFeedbackService() {}

    /** The fire event: the carrier spawns at the caster, so the caster is the origin. */
    public static void fire(ServerPlayer caster, AbilityDef def) {
        if (caster == null || def == null) {
            return;
        }
        AbilityDef.ShakeSpec shake = def.feedback().onFire();
        if (shake == null) {
            return;
        }
        send((ServerLevel) caster.level(), caster.position(), shake);
    }

    /** A carrier moment (hit or end) at the carrier's position or the event's own origin. */
    public static void carrier(Entity carrier, AbilityDef def, AbilityDef.FxEvent event, Vec3 origin) {
        if (carrier == null || def == null || !(carrier.level() instanceof ServerLevel level)) {
            return;
        }
        AbilityDef.ShakeSpec shake = def.feedback().forEvent(event);
        if (shake == null) {
            return;
        }
        send(level, origin, shake);
    }

    private static void send(ServerLevel level, Vec3 origin, AbilityDef.ShakeSpec shake) {
        ScreenShakePayload payload = new ScreenShakePayload(
                shake.strength(), shake.durationTicks(), 0,
                origin.x, origin.y, origin.z, shake.radius());
        for (ServerPlayer viewer : PlayerLookup.around(level, origin, shake.radius())) {
            ServerPlayNetworking.send(viewer, payload);
        }
    }
}
