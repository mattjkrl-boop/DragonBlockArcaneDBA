package com.dragonblockarcanedba.movement;

import com.dragonblockarcanedba.network.C2SMovementPayload;
import com.dragonblockarcanedba.network.ScreenShakePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 5 shared movement layer, server authoritative. Gives every player the same
 * three verbs and any ability a common base to build on, replacing per-ability
 * movement one migration at a time:
 *
 * - Dash: momentum-preserving horizontal burst along the look direction. Works on
 *   the ground freely; in the air it consumes the one air dash until ground contact.
 * - Ki step: one mid-air refresh jump, consumed until ground contact.
 * - Fast fall: a downward slam; landing after it produces the strongest impact.
 *
 * Landing impact is classified from the vertical speed tracked each server tick:
 * past the threshold it kicks up dust, plays the big-fall sound, and sends the player
 * a screen shake scaled by impact. Speeds are fixed constants, which are the caps:
 * the server never applies more than these values, whatever the client asks for.
 */
public final class MovementPhysics {
    private MovementPhysics() {}

    private static final double DASH_SPEED = 1.35;
    private static final double KI_STEP_LIFT = 0.85;
    private static final double FAST_FALL_SPEED = -2.2;
    private static final double IMPACT_MIN_SPEED = 0.95;

    private static final class State {
        boolean airDashUsed;
        boolean kiStepUsed;
        boolean fastFalling;
        boolean wasOnGround = true;
        double lastVy;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(C2SMovementPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handle(player, payload.action()));
        });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(
            (handler, server) -> forget(handler.getPlayer().getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                for (ServerPlayer player : level.players()) {
                    tickPlayer(player);
                }
            }
        });
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    private static void handle(ServerPlayer player, int action) {
        if (!player.isAlive() || player.isSpectator() || player.getAbilities().flying) return;
        State s = state(player);
        Vec3 vel = player.getDeltaMovement();
        switch (action) {
            case C2SMovementPayload.DASH -> {
                boolean grounded = player.onGround();
                if (!grounded && s.airDashUsed) return;
                Vec3 look = player.getLookAngle();
                Vec3 flat = new Vec3(look.x, 0, look.z);
                if (flat.lengthSqr() < 1.0e-4) return;
                flat = flat.normalize().scale(DASH_SPEED);
                double vy = grounded ? Math.max(vel.y, 0.12) : vel.y * 0.35;
                apply(player, new Vec3(flat.x, vy, flat.z));
                if (!grounded) s.airDashUsed = true;
            }
            case C2SMovementPayload.KI_STEP -> {
                if (player.onGround() || s.kiStepUsed) return;
                apply(player, new Vec3(vel.x * 1.12, KI_STEP_LIFT, vel.z * 1.12));
                s.kiStepUsed = true;
            }
            case C2SMovementPayload.FAST_FALL -> {
                if (player.onGround() || s.fastFalling) return;
                apply(player, new Vec3(vel.x * 0.4, FAST_FALL_SPEED, vel.z * 0.4));
                s.fastFalling = true;
            }
            default -> { /* unknown action: ignore */ }
        }
    }

    private static void apply(ServerPlayer player, Vec3 velocity) {
        player.setDeltaMovement(velocity);
        // Server players only push velocity to their client when marked hurt.
        player.hurtMarked = true;
    }

    private static void tickPlayer(ServerPlayer player) {
        State s = state(player);
        boolean grounded = player.onGround();
        if (grounded) {
            if (!s.wasOnGround) onLand(player, s);
            s.airDashUsed = false;
            s.kiStepUsed = false;
            s.fastFalling = false;
        }
        s.wasOnGround = grounded;
        s.lastVy = player.getDeltaMovement().y;
    }

    private static void onLand(ServerPlayer player, State s) {
        double impact = Math.abs(s.lastVy);
        if (impact < IMPACT_MIN_SPEED) return;
        ServerLevel level = (ServerLevel) player.level();
        double x = player.getX(), y = player.getY(), z = player.getZ();
        int dust = s.fastFalling ? 26 : 14;
        level.sendParticles(ParticleTypes.POOF, x, y + 0.15, z, dust, 0.55, 0.12, 0.55, 0.06);
        level.playSound(null, x, y, z, SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 0.7f, 0.9f);
        float intensity = (float) Math.min(1.6, 0.45 + (impact - IMPACT_MIN_SPEED) * 0.55) * (s.fastFalling ? 1.35f : 1.0f);
        ServerPlayNetworking.send(player, new ScreenShakePayload(intensity, 7, 0));
    }

    /** Drops tracked state when a player leaves, so the map cannot grow forever. */
    public static void forget(UUID playerId) {
        STATES.remove(playerId);
    }
}
