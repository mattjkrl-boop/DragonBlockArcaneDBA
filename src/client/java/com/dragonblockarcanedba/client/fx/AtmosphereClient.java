package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.network.AtmospherePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client side of AtmosphereServer (SPECTACLE-STANDARD point 4). Holds gloom
 * sources refreshed by AtmospherePayload; the gloom value lerps toward the
 * strongest source the local player is standing in, and back out when the
 * source expires or the player leaves. DbaPostFx reads gloomActive() to run
 * its eclipse grade, so the darkening is a post grade layered over the real
 * server weather the server side sets. No hard cuts anywhere.
 */
public final class AtmosphereClient {
    private AtmosphereClient() {}

    private static final class Source {
        Vec3 center;
        float radius;
        float gloom;
        long expiryClientTick;
    }

    private static final List<Source> SOURCES = new ArrayList<>();
    private static float gloomCurrent = 0f;
    private static long clientTick = 0;

    public static void accept(AtmospherePayload payload) {
        long expiry = clientTick + Math.max(20, payload.ticks());
        for (Source s : SOURCES) {
            if (s.center.distanceToSqr(new Vec3(payload.x(), payload.y(), payload.z())) < 4.0) {
                s.expiryClientTick = Math.max(s.expiryClientTick, expiry);
                s.gloom = Math.max(s.gloom, payload.gloom());
                s.radius = Math.max(s.radius, payload.radius());
                return;
            }
        }
        Source s = new Source();
        s.center = new Vec3(payload.x(), payload.y(), payload.z());
        s.radius = payload.radius();
        s.gloom = payload.gloom();
        s.expiryClientTick = expiry;
        SOURCES.add(s);
    }

    public static void tick(Minecraft mc) {
        clientTick++;
        float target = 0f;
        if (mc.player != null) {
            Vec3 p = mc.player.position();
            Iterator<Source> it = SOURCES.iterator();
            while (it.hasNext()) {
                Source s = it.next();
                if (s.expiryClientTick <= clientTick) { it.remove(); continue; }
                double d = p.distanceTo(s.center);
                if (d < s.radius) {
                    // Full gloom inside 70 percent of the radius, feathered at the rim.
                    float feather = (float) Math.min(1.0, (s.radius - d) / (s.radius * 0.3));
                    target = Math.max(target, s.gloom * feather);
                }
            }
        } else {
            SOURCES.clear();
        }
        // Lerp at a fixed rate: full in over about 2 seconds, out over about 3.
        float rate = target > gloomCurrent ? 0.012f : 0.008f;
        if (gloomCurrent < target) gloomCurrent = Math.min(target, gloomCurrent + rate);
        else gloomCurrent = Math.max(target, gloomCurrent - rate);
    }

    public static boolean gloomActive() {
        return gloomCurrent > 0.05f;
    }

    public static float gloom() {
        return gloomCurrent;
    }
}
