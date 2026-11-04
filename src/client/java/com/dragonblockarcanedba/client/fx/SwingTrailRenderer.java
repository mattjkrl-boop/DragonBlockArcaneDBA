package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaMaterialOptions;
import com.dragonblockarcanedba.particle.DbaParticles;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Emits material along the swing paths listed in WeaponTrailProfiles.
 *
 * The samples are emission data only. Nothing in this class draws a ribbon
 * or any other geometry: the old trail surface is now plasma and vapor
 * particles spawned along the recorded hilt and tip path by the shared
 * MaterialEmitters accumulator, in the profile color, with a near-white
 * plasma core along the leading half of the blade. The profile table has
 * not changed, and neither have its colors, reach, sweep, fade ticks, or
 * tip behavior. Purely client side.
 */
public final class SwingTrailRenderer {
    private SwingTrailRenderer() {}

    private static final int SUBSTEPS = 4;
    private static final float TICK_SECONDS = 1.0f / 20.0f;

    private record Sample(Vec3 hilt, Vec3 tip, int born) {}

    private static final class Trail {
        final ArrayDeque<Sample> samples = new ArrayDeque<>();
        WeaponTrailProfiles.Profile profile;
        int lastFlashClock = -100;
        int seedBase;
        WeaponTrailProfiles.Profile emitterProfile;
        ClientLevel emitterLevel;
        MaterialEmitters.Emitter plasma;
        MaterialEmitters.Emitter core;
        MaterialEmitters.Emitter vapor;
    }

    private static final Map<UUID, Trail> TRAILS = new HashMap<>();
    private static int clock;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(SwingTrailRenderer::tick);
    }

    private static float alpha(WeaponTrailProfiles.Profile p, Sample s) {
        float t = 1f - (clock - s.born) / (float) p.fadeTicks();
        t = Math.max(0f, Math.min(1f, t));
        float eased = t * t * (3f - 2f * t);
        return eased * p.alpha();
    }

    private static void tick(Minecraft mc) {
        clock++;
        if (mc.level == null || mc.isPaused()) {
            if (mc.level == null) TRAILS.clear();
            return;
        }
        float density = DbaEffectsConfig.effectDensity();
        for (Player player : mc.level.players()) {
            WeaponTrailProfiles.Profile profile = WeaponTrailProfiles.forItem(player.getMainHandItem().getItem());
            if (profile == null) continue;
            float anim = com.dragonblockarcanedba.util.DbaCompat.attackAnim(player, 1.0f);
            float prevAnim = com.dragonblockarcanedba.util.DbaCompat.attackAnim(player, 0.0f);
            if (anim <= 0f && prevAnim <= 0f) continue;
            Trail trail = TRAILS.computeIfAbsent(player.getUUID(), id -> {
                Trail created = new Trail();
                created.seedBase = id.hashCode();
                return created;
            });
            trail.profile = profile;
            if (profile.kind() == WeaponTrailProfiles.Kind.FLASH) {
                if (clock - trail.lastFlashClock > 5) {
                    trail.lastFlashClock = clock;
                    flash(mc, player, profile, trail);
                }
                continue;
            }
            for (int i = 1; i <= SUBSTEPS; i++) {
                float t = prevAnim + (anim - prevAnim) * (i / (float) SUBSTEPS);
                addSample(trail, player, profile, t);
            }
            if (density > 0f && profile.tip() != WeaponTrailProfiles.Tip.NONE && anim > 0.3f && anim < 0.8f) {
                tipParticles(mc, player, profile);
            }
        }
        Iterator<Map.Entry<UUID, Trail>> it = TRAILS.entrySet().iterator();
        while (it.hasNext()) {
            Trail trail = it.next().getValue();
            int maxAge = trail.profile == null ? 6 : trail.profile.fadeTicks();
            while (!trail.samples.isEmpty() && clock - trail.samples.peekFirst().born > maxAge) trail.samples.pollFirst();
            if (trail.samples.isEmpty() && clock - trail.lastFlashClock > 20) {
                it.remove();
            } else if (!trail.samples.isEmpty() && trail.profile != null) {
                emitTrail(mc.level, trail);
            }
        }
    }

    /**
     * Feeds the trail emitters for one client tick. The emission envelope
     * is the old ribbon alpha: the strongest surviving sample alpha, taken
     * over the profile fade window and distance-scaled like every other
     * converted effect.
     */
    private static void emitTrail(ClientLevel level, Trail trail) {
        WeaponTrailProfiles.Profile p = trail.profile;
        if (p == null) return;
        if (trail.samples.size() < 2 && p.kind() != WeaponTrailProfiles.Kind.FLASH) return;
        ensureEmitters(level, trail);
        float envelope = 0f;
        for (Sample s : trail.samples) envelope = Math.max(envelope, alpha(p, s));
        Sample newest = trail.samples.peekLast();
        if (newest != null) envelope *= MaterialEmitters.distanceScale(newest.tip());
        if (envelope <= 0.01f) return;
        trail.plasma.tick(TICK_SECONDS, envelope);
        trail.core.tick(TICK_SECONDS, envelope);
        trail.vapor.tick(TICK_SECONDS, envelope * 0.7f);
    }

    private static void ensureEmitters(ClientLevel level, Trail trail) {
        WeaponTrailProfiles.Profile p = trail.profile;
        if (trail.plasma != null && trail.emitterProfile == p && trail.emitterLevel == level) return;
        float profileScale = (float) Math.max(0.75, Math.min(1.5, p.reach() / 2.0));
        MaterialEmitters.Source blade = trailPathSource(trail, 0.0, 1.0, 0.22);
        MaterialEmitters.Source leading = trailPathSource(trail, 0.5, 1.0, 0.22);
        trail.plasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA, p.color(),
                        profileScale * 0.95f, trail.seedBase ^ 0x51A5, Vec3.ZERO, 0.018f),
                blade, 280f, 340);
        trail.core = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA, coreColor(p),
                        profileScale * 0.62f, trail.seedBase ^ 0xC02E, Vec3.ZERO, 0.012f),
                leading, 150f, 220);
        trail.vapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.VAPOR, p.color(),
                        profileScale * 1.3f, trail.seedBase ^ 0x9A70, Vec3.ZERO, 0.03f),
                blade, 85f, 130);
        trail.emitterProfile = p;
        trail.emitterLevel = level;
    }

    /** The old hot core color: the profile color pushed 70 percent toward white. */
    private static int coreColor(WeaponTrailProfiles.Profile p) {
        int color = p.color();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r += Math.round((255 - r) * 0.7f);
        g += Math.round((255 - g) * 0.7f);
        b += Math.round((255 - b) * 0.7f);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * A source over the recorded swing path. Each sample picks a segment
     * between two recorded swing samples and a point across the blade
     * between hilt and tip, and reports the segment motion as a small
     * source velocity so the material keeps moving the way the swing did.
     * A lone sample, which is how a FLASH profile records its shot, is a
     * single hilt to tip line.
     */
    private static MaterialEmitters.Source trailPathSource(Trail trail, double minBlade, double maxBlade,
                                                           double velocityScale) {
        return random -> samplePath(trail, random, minBlade, maxBlade, velocityScale);
    }

    private static MaterialEmitters.Sample samplePath(Trail trail, RandomSource random,
                                                      double minBlade, double maxBlade, double velocityScale) {
        List<Sample> samples = new ArrayList<>(trail.samples);
        if (samples.isEmpty()) return new MaterialEmitters.Sample(Vec3.ZERO, Vec3.ZERO);
        double blade = minBlade + random.nextDouble() * (maxBlade - minBlade);
        if (samples.size() == 1) {
            Sample only = samples.get(0);
            return new MaterialEmitters.Sample(only.hilt().lerp(only.tip(), blade), Vec3.ZERO);
        }
        int segment = random.nextInt(samples.size() - 1);
        Sample a = samples.get(segment);
        Sample b = samples.get(segment + 1);
        double time = random.nextDouble();
        Vec3 aPoint = a.hilt().lerp(a.tip(), blade);
        Vec3 bPoint = b.hilt().lerp(b.tip(), blade);
        return new MaterialEmitters.Sample(aPoint.lerp(bPoint, time),
                bPoint.subtract(aPoint).scale(velocityScale));
    }

    private static Vec3[] basis(Player player) {
        float yaw = (float) Math.toRadians(player.getYRot());
        float pitch = (float) Math.toRadians(player.getXRot());
        Vec3 forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 up = forward.cross(right).normalize().scale(-1);
        Vec3 origin = player.getEyePosition().add(0, -0.35, 0);
        return new Vec3[] {forward, right, up, origin};
    }

    /** Swing sample by profile kind: ARC and FAN sweep, THRUST stabs along the look direction. */
    private static void addSample(Trail trail, Player player, WeaponTrailProfiles.Profile p, float anim) {
        Vec3[] b = basis(player);
        Vec3 forward = b[0], right = b[1], up = b[2], origin = b[3];
        Vec3 hilt, tip;
        if (p.kind() == WeaponTrailProfiles.Kind.THRUST) {
            double ext = Math.sin(Math.PI * Math.min(1f, anim * 1.15f)) * p.reach();
            hilt = origin.add(forward.scale(0.55 + ext * 0.3));
            tip = origin.add(forward.scale(0.55 + ext)).add(right.scale(0.12));
        } else {
            double angle = Math.toRadians(-p.sweepDeg() / 2.0 + p.sweepDeg() * anim);
            Vec3 dir = forward.scale(Math.cos(angle) * 0.7).add(right.scale(-Math.sin(angle))).add(up.scale(Math.sin(angle) * 0.35)).normalize();
            hilt = origin.add(dir.scale(0.6));
            tip = origin.add(dir.scale(p.reach()));
        }
        trail.samples.addLast(new Sample(hilt, tip, clock));
        if (trail.samples.size() > 64) trail.samples.pollFirst();
    }

    /** Blaster: a spark burst at the muzzle, with the shot line recorded as emission data for the trail emitters. */
    private static void flash(Minecraft mc, Player player, WeaponTrailProfiles.Profile p, Trail trail) {
        Vec3[] b = basis(player);
        Vec3 muzzle = b[3].add(b[0].scale(1.15)).add(b[1].scale(0.22)).add(0, -0.08, 0);
        Vec3 end = b[3].add(b[0].scale(p.reach()));
        trail.samples.addLast(new Sample(muzzle, end, clock));
        if (trail.samples.size() > 8) trail.samples.pollFirst();
        MaterialEmitters.burst(mc.level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, b[0].scale(0.25), 0.06f),
                MaterialEmitters.point(muzzle), 5);
    }

    private static void tipParticles(Minecraft mc, Player player, WeaponTrailProfiles.Profile p) {
        Vec3[] b = basis(player);
        Vec3 tipPos = b[3].add(b[0].scale(p.reach() * 0.8));
        Vec3 drift = new Vec3(0, 0.03, 0);
        MaterialEmitters.Preset preset = switch (p.tip()) {
            case SPARK -> MaterialEmitters.Preset.fixed(DbaParticles.SPARK, drift, 0.025f);
            case EMBER -> MaterialEmitters.Preset.ember(drift, 0.025f);
            case VOID_SMOKE -> MaterialEmitters.Preset.fixed(DbaParticles.VOID_SMOKE, drift, 0.025f);
            case WIND -> MaterialEmitters.Preset.fixed(DbaParticles.WIND, drift, 0.025f);
            case NONE -> null;
        };
        if (preset == null) return;
        MaterialEmitters.burst(mc.level, preset, MaterialEmitters.point(tipPos), 1);
    }
}
