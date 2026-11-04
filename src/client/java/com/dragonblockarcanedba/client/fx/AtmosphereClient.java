package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.network.AtmospherePayload;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.sound.DbaSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client side of AtmosphereServer. Bespoke pass (BESPOKE-STANDARD rule 5):
 * the client renders weapon storms itself, because the server no longer
 * touches world weather. Three local pieces per storm volume:
 * gloom, lerped toward the strongest source the player stands in and read
 * by DbaPostFx for its grade; rain, the mod's RAIN particles falling inside
 * the volume around the player; and thunder, DbaSounds thunder scheduled
 * at random points in THUNDER volumes. No hard cuts anywhere.
 */
public final class AtmosphereClient {
    private AtmosphereClient() {}

    private static final class Source {
        Vec3 center;
        float radius;
        float gloom;
        boolean thunder;
        long expiryClientTick;
        long nextThunderTick;
    }

    private static final List<Source> SOURCES = new ArrayList<>();
    private static float gloomCurrent = 0f;
    private static long clientTick = 0;

    public static void accept(AtmospherePayload payload) {
        long expiry = clientTick + Math.max(20, payload.ticks());
        Vec3 center = new Vec3(payload.x(), payload.y(), payload.z());
        for (Source s : SOURCES) {
            if (s.center.distanceToSqr(center) < 4.0) {
                s.expiryClientTick = Math.max(s.expiryClientTick, expiry);
                s.gloom = Math.max(s.gloom, payload.gloom());
                s.radius = Math.max(s.radius, payload.radius());
                s.thunder = s.thunder || payload.kind() == AtmospherePayload.KIND_THUNDER;
                return;
            }
        }
        Source s = new Source();
        s.center = center;
        s.radius = payload.radius();
        s.gloom = payload.gloom();
        s.thunder = payload.kind() == AtmospherePayload.KIND_THUNDER;
        s.expiryClientTick = expiry;
        s.nextThunderTick = clientTick + 40;
        SOURCES.add(s);
    }

    public static void tick(Minecraft mc) {
        clientTick++;
        float target = 0f;
        if (mc.player != null && mc.level != null) {
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
                    renderStorm(mc, s, p);
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

    /** Rain and thunder for one storm the player is standing in. */
    private static void renderStorm(Minecraft mc, Source s, Vec3 playerPos) {
        RandomSource rand = mc.level.getRandom();
        float density = DbaEffectsConfig.effectDensity();
        if (density > 0f) {
            // Rain streaks in a cylinder around the player, clipped to the storm volume.
            int n = Math.max(1, Math.round(7f * density));
            double reach = Math.min(22.0, s.radius);
            for (int i = 0; i < n; i++) {
                double x = playerPos.x + (rand.nextDouble() * 2.0 - 1.0) * reach;
                double z = playerPos.z + (rand.nextDouble() * 2.0 - 1.0) * reach;
                double dx = x - s.center.x, dz = z - s.center.z;
                if (dx * dx + dz * dz > s.radius * s.radius) continue;
                double y = playerPos.y + 13.0 + rand.nextDouble() * 5.0;
                mc.level.addParticle(DbaParticles.RAIN, x, y, z, 0.0, -2.4, 0.0);
            }
        }
        if (s.thunder && clientTick >= s.nextThunderTick) {
            s.nextThunderTick = clientTick + 90 + rand.nextInt(170);
            double ang = rand.nextDouble() * Math.PI * 2.0;
            double rr = rand.nextDouble() * s.radius * 0.8;
            double x = s.center.x + Math.cos(ang) * rr;
            double z = s.center.z + Math.sin(ang) * rr;
            double y = s.center.y + 4.0 + rand.nextDouble() * 5.0;
            mc.level.playLocalSound(x, y, z,
                rand.nextBoolean() ? DbaSounds.THUNDER_CRACK : DbaSounds.THUNDER_DARK,
                SoundSource.WEATHER, 1.1f, 0.92f + rand.nextFloat() * 0.16f, false);
        }
    }

    public static boolean gloomActive() {
        return gloomCurrent > 0.05f;
    }

    public static float gloom() {
        return gloomCurrent;
    }
}
