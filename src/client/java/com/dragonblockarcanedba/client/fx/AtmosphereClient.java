package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.network.AtmospherePayload;
import com.dragonblockarcanedba.particle.DbaMaterialOptions;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.sound.DbaSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client side of AtmosphereServer. Bespoke pass (BESPOKE-STANDARD rule 5):
 * the client renders weapon storms itself, because the server no longer
 * touches world weather. The local pieces per storm volume are:
 * gloom, lerped toward the strongest source the player stands in and read
 * by DbaPostFx for its grade; RAIN streaks falling inside the volume around
 * the player, deepened by vapor precipitation, ground mist, wind driven
 * dust, darkness material, WISP_CURL wind wisps, and ASH_FLAKE airborne
 * grit; and thunder, DbaSounds thunder scheduled at
 * random points in THUNDER volumes. No hard cuts anywhere. This system
 * adds no geometry of its own; the thunder geometry belongs to the
 * shared Bolt system.
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
            emitMaterialLayers(mc, s, playerPos, reach);
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

    /**
     * Material depth over the rain streaks: a falling vapor veil in the same
     * clipped cylinder, ground mist and wind driven dust on the terrain
     * under the player, and darkness material drifting through the gloom
     * volume. The complex sprite layer rides that same gloom volume
     * source: WISP_CURL wind wisps and ASH_FLAKE airborne grit, carried
     * on the same wind vector as the dust layer. The post grade keeps
     * its original strength and lerp; these particles give the gloom a
     * body instead of leaving it as a flat screen tint.
     */
    private static void emitMaterialLayers(Minecraft mc, Source s, Vec3 playerPos, double reach) {
        int seed = Double.hashCode(s.center.x) * 31 + Double.hashCode(s.center.z);

        MaterialEmitters.Source precipitation = random -> {
            for (int attempt = 0; attempt < 6; attempt++) {
                double x = playerPos.x + (random.nextDouble() * 2.0 - 1.0) * reach;
                double z = playerPos.z + (random.nextDouble() * 2.0 - 1.0) * reach;
                double dx = x - s.center.x, dz = z - s.center.z;
                if (dx * dx + dz * dz <= s.radius * s.radius) {
                    double y = playerPos.y + 13.0 + random.nextDouble() * 5.0;
                    return new MaterialEmitters.Sample(new Vec3(x, y, z), Vec3.ZERO);
                }
            }
            return new MaterialEmitters.Sample(
                    new Vec3(playerPos.x, playerPos.y + 15.0, playerPos.z), Vec3.ZERO);
        };
        MaterialEmitters.burst(mc.level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.VAPOR, 0xA9C6DE, 0.72f,
                        seed, new Vec3(0.0, -1.05, 0.0), 0.10f),
                precipitation, 3);

        if (clientTick % 4 != 0) return;

        MaterialEmitters.Source groundArea = random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = Math.sqrt(random.nextDouble()) * Math.min(10.0, s.radius);
            double x = playerPos.x + Math.cos(ang) * rr;
            double z = playerPos.z + Math.sin(ang) * rr;
            double dx = x - s.center.x, dz = z - s.center.z;
            if (dx * dx + dz * dz > s.radius * s.radius) {
                x = playerPos.x;
                z = playerPos.z;
            }
            int groundY = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (int) Math.floor(x), (int) Math.floor(z));
            return new MaterialEmitters.Sample(new Vec3(x, groundY + 0.22, z), Vec3.ZERO);
        };
        MaterialEmitters.burst(mc.level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.MIST, 0x93A9BC, 1.55f,
                        seed + 41, new Vec3(0.015, 0.005, 0.0), 0.025f),
                groundArea, 3);
        MaterialEmitters.burst(mc.level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.DUST, 0x8E9BA6, 0.85f,
                        seed + 67, new Vec3(0.20, 0.012, 0.03), 0.045f),
                groundArea, 2);

        MaterialEmitters.Source gloomVolume = random -> {
            for (int attempt = 0; attempt < 6; attempt++) {
                double half = Math.min(7.0, s.radius);
                double x = playerPos.x + (random.nextDouble() * 2.0 - 1.0) * half;
                double y = playerPos.y + 1.5 + (random.nextDouble() * 2.0 - 1.0) * 1.7;
                double z = playerPos.z + (random.nextDouble() * 2.0 - 1.0) * half;
                double dx = x - s.center.x, dz = z - s.center.z;
                if (dx * dx + dz * dz <= s.radius * s.radius) {
                    return new MaterialEmitters.Sample(new Vec3(x, y, z), Vec3.ZERO);
                }
            }
            return new MaterialEmitters.Sample(
                    new Vec3(playerPos.x, playerPos.y + 1.5, playerPos.z), Vec3.ZERO);
        };
        MaterialEmitters.burst(mc.level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.DARKNESS, 0x0A0D13, 1.15f,
                        seed + 97, Vec3.ZERO, 0.018f),
                gloomVolume, s.gloom >= 0.7f ? 2 : 1);
        // Complex sprite layer, on this same gated cadence and volume as
        // the darkness layer above: 2 per burst every 4 ticks is 10 per
        // second per type, inside the 40 per second continuous cap and
        // in keeping with the existing material counts here (mist 3,
        // dust 2, darkness 1 to 2 per gated burst). Both ride the dust
        // layer wind vector, so the wisps and grit move with the storm
        // air the material layers already established.
        MaterialEmitters.burst(mc.level,
                MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL,
                        new Vec3(0.20, 0.012, 0.03), 0.045f),
                gloomVolume, 2);
        MaterialEmitters.burst(mc.level,
                MaterialEmitters.Preset.fixed(DbaParticles.ASH_FLAKE,
                        new Vec3(0.20, 0.005, 0.03), 0.045f),
                gloomVolume, 2);
    }

    public static boolean gloomActive() {
        return gloomCurrent > 0.05f;
    }

    public static float gloom() {
        return gloomCurrent;
    }
}
