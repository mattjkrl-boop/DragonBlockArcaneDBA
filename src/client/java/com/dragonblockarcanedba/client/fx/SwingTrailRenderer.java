package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaMaterialOptions;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
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
 * Draws and emits along the swing paths listed in WeaponTrailProfiles.
 *
 * Complex pass: the trail is a layered composition built from the same
 * recorded samples. The pieces layer is a chain of ComplexGeometry
 * tendrils whose quadratic bezier spines run through the sampled path
 * points: one chain along the blade tip arc and one along the mid blade,
 * submitted through submitEnergy with the tendril canvas, so each strand
 * coils in 3D, twists along its length, and tears at the builder's
 * seeded pinch windows and at every chain joint near the arc ends. The
 * complex particle layer is SPARK_STREAK sprites shed along the tip
 * fifth of the blade for blade profiles (the steel themes). The material
 * layer is unchanged filler: plasma and vapor particles spawned along
 * the recorded hilt and tip path by the shared MaterialEmitters
 * accumulator, in the profile color, with a near-white plasma core along
 * the leading half of the blade. The pieces are drawn from the level
 * render collect-submits event in camera relative space, anchored on the
 * oldest surviving sample so the geometry and the emission sit on the
 * exact same path. The profile table has not changed, and neither have
 * its colors, reach, sweep, fade ticks, or tip behavior. Purely client
 * side.
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
        MaterialEmitters.Emitter streaks;
    }

    private static final Map<UUID, Trail> TRAILS = new HashMap<>();
    private static int clock;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(SwingTrailRenderer::tick);
        LevelRenderEvents.COLLECT_SUBMITS.register(SwingTrailRenderer::collectSubmits);
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
        if (trail.streaks != null) trail.streaks.tick(TICK_SECONDS, envelope);
    }

    /**
     * Pieces layer, drawn once per frame from the level render submit
     * pass. The pose stack there is camera relative, so each trail is
     * anchored on its oldest surviving tip sample: the stack is pushed,
     * translated to that anchor minus the camera, and the tendril chains
     * are submitted in anchor local space, then the stack is popped, the
     * same pattern vanilla uses for block entity submits.
     */
    private static void collectSubmits(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || TRAILS.isEmpty()) return;
        if (context.levelState() == null || context.levelState().cameraRenderState == null) return;
        Vec3 cam = context.levelState().cameraRenderState.pos;
        if (cam == null) return;
        PoseStack poseStack = context.poseStack();
        for (Trail trail : TRAILS.values()) {
            drawTrailPieces(context, poseStack, cam, trail);
        }
    }

    private static void drawTrailPieces(LevelRenderContext context, PoseStack poseStack, Vec3 cam, Trail trail) {
        WeaponTrailProfiles.Profile p = trail.profile;
        if (p == null || trail.samples.isEmpty()) return;
        List<Sample> samples = new ArrayList<>(trail.samples);
        float envelope = 0f;
        for (Sample s : samples) envelope = Math.max(envelope, alpha(p, s));
        Sample newest = samples.get(samples.size() - 1);
        float ds = MaterialEmitters.distanceScale(newest.tip());
        envelope *= ds;
        if (envelope <= 0.01f) return;
        float profileScale = (float) Math.max(0.75, Math.min(1.5, p.reach() / 2.0));
        final float env = envelope;
        final int rootColor = p.color();
        final int tipColor = coreColor(p);
        final long seedBase = trail.seedBase;

        poseStack.pushPose();
        try {
            if (p.kind() == WeaponTrailProfiles.Kind.FLASH || samples.size() == 1) {
                // A blaster shot records one hilt to tip line: one tendril
                // along the shot, its curl supplied by the builder.
                Sample shot = newest;
                Vec3 anchor = shot.hilt();
                poseStack.translate(anchor.x - cam.x, anchor.y - cam.y, anchor.z - cam.z);
                Vec3 from = Vec3.ZERO;
                Vec3 to = shot.tip().subtract(anchor);
                Vec3 control = from.lerp(to, 0.5);
                float width = 0.13f * profileScale;
                ComplexGeometry.submitEnergy(context.submitNodeCollector(), poseStack, ComplexTextures.TENDRIL,
                        (pose, buffer) -> ComplexGeometry.tendril(pose, buffer, seedBase ^ 0xF1A5L,
                                from, control, to, width, rootColor, tipColor, 0.95f, env, ds));
                return;
            }
            List<Vec3> tipPts = bladePoints(samples, 1.0);
            List<Vec3> midPts = bladePoints(samples, 0.55);
            Vec3 anchor = tipPts.get(0);
            poseStack.translate(anchor.x - cam.x, anchor.y - cam.y, anchor.z - cam.z);
            List<Vec3> tipLocal = localTo(tipPts, anchor);
            List<Vec3> midLocal = localTo(midPts, anchor);
            float tipWidth = 0.20f * profileScale;
            float midWidth = 0.15f * profileScale;
            ComplexGeometry.submitEnergy(context.submitNodeCollector(), poseStack, ComplexTextures.TENDRIL,
                    (pose, buffer) -> {
                        tendrilChain(pose, buffer, seedBase ^ 0x7E1DL, tipLocal, tipWidth, rootColor, tipColor, env, ds);
                        tendrilChain(pose, buffer, seedBase ^ 0x7E2DL, midLocal, midWidth, rootColor, tipColor, env, ds);
                    });
        } finally {
            poseStack.popPose();
        }
    }

    /** Points along the recorded path at one blade fraction, decimated to at most nine controls. */
    private static List<Vec3> bladePoints(List<Sample> samples, double blade) {
        List<Vec3> pts = new ArrayList<>();
        int n = samples.size();
        int want = Math.min(n, 9);
        for (int i = 0; i < want; i++) {
            int idx = want == 1 ? 0 : Math.round(i * (n - 1) / (float) (want - 1));
            Sample s = samples.get(idx);
            pts.add(s.hilt().lerp(s.tip(), blade));
        }
        return pts;
    }

    private static List<Vec3> localTo(List<Vec3> pts, Vec3 anchor) {
        List<Vec3> out = new ArrayList<>(pts.size());
        for (Vec3 pt : pts) out.add(pt.subtract(anchor));
        return out;
    }

    /**
     * One tendril per pair of path intervals, each taking three
     * consecutive path points as its bezier from, control, and to, so the
     * strand follows the sampled swing exactly and narrows at every joint.
     */
    private static void tendrilChain(com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                                     com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                     long seed, List<Vec3> pts, float width,
                                     int rootColor, int tipColor, float env, float ds) {
        if (pts.size() < 2) return;
        if (pts.size() == 2) {
            Vec3 mid = pts.get(0).lerp(pts.get(1), 0.5);
            ComplexGeometry.tendril(pose, buffer, seed, pts.get(0), mid, pts.get(1),
                    width, rootColor, tipColor, 0.95f, env, ds);
            return;
        }
        int last = 0;
        for (int i = 0; i + 2 < pts.size(); i += 2) {
            ComplexGeometry.tendril(pose, buffer, seed + i * 7919L, pts.get(i), pts.get(i + 1), pts.get(i + 2),
                    width, rootColor, tipColor, 0.95f, env, ds);
            last = i + 2;
        }
        if (last < pts.size() - 1) {
            int n = pts.size();
            ComplexGeometry.tendril(pose, buffer, seed + 0x3D09L, pts.get(n - 3), pts.get(n - 2), pts.get(n - 1),
                    width, rootColor, tipColor, 0.95f, env, ds);
        }
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
        // Complex particle layer: spark streaks shed along the tip fifth
        // of the blade. Blade profiles only: a FLASH profile is a blaster
        // shot, not steel, and keeps its existing spark burst instead.
        trail.streaks = p.kind() == WeaponTrailProfiles.Kind.FLASH ? null
                : new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.05f),
                        trailPathSource(trail, 0.85, 1.0, 0.30), 30f, 48);
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
