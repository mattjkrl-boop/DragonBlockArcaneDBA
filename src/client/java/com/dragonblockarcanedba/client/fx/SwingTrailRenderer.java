package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaParticles;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Draws only the trails listed in WeaponTrailProfiles. That table is the katana's
 * white slash. The old generic swing (one arc on every weapon, on top of the
 * weapon's own effect) is not drawn. Purely client side.
 */
public final class SwingTrailRenderer {
    private SwingTrailRenderer() {}

    private static final int SUBSTEPS = 4;

    private record Sample(Vec3 hilt, Vec3 tip, int born) {}

    private static final class Trail {
        final ArrayDeque<Sample> samples = new ArrayDeque<>();
        WeaponTrailProfiles.Profile profile;
        int lastFlashClock = -100;
    }

    private static final Map<UUID, Trail> TRAILS = new HashMap<>();
    private static int clock;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(SwingTrailRenderer::tick);
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(context -> {
            if (TRAILS.isEmpty()) return;
            Vec3 cam = context.levelState().cameraRenderState.pos;
            context.submitNodeCollector().submitCustomGeometry(context.poseStack(), KiRenderHelper.kiRenderType(), (pose, buffer) -> {
                Matrix4f mat = pose.pose();
                for (Trail trail : TRAILS.values()) {
                    WeaponTrailProfiles.Profile p = trail.profile;
                    if (p == null) continue;
                    float r = KiRenderHelper.red(p.color()), g = KiRenderHelper.green(p.color()), b = KiRenderHelper.blue(p.color());
                    float cr = r + (1f - r) * 0.7f, cg = g + (1f - g) * 0.7f, cb = b + (1f - b) * 0.7f;
                    Sample prev = null;
                    for (Sample s : trail.samples) {
                        if (prev != null) {
                            float a0 = alpha(p, prev) * 0.55f, a1 = alpha(p, s) * 0.55f;
                            ribbon(buffer, mat, pose, prev.hilt, prev.tip, s.hilt, s.tip, cam, r, g, b, a0, a1);
                            // hot core along the leading half of the blade
                            Vec3 c0 = prev.hilt.lerp(prev.tip, 0.5), c1 = s.hilt.lerp(s.tip, 0.5);
                            ribbon(buffer, mat, pose, c0, prev.tip, c1, s.tip, cam, cr, cg, cb, a0 * 1.5f, a1 * 1.5f);
                        }
                        prev = s;
                    }
                }
            });
        });
    }

    private static float alpha(WeaponTrailProfiles.Profile p, Sample s) {
        float t = 1f - (clock - s.born) / (float) p.fadeTicks();
        t = Math.max(0f, Math.min(1f, t));
        float eased = t * t * (3f - 2f * t);
        return eased * p.alpha();
    }

    private static void ribbon(com.mojang.blaze3d.vertex.VertexConsumer buffer, Matrix4f mat, com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                               Vec3 h0v, Vec3 t0v, Vec3 h1v, Vec3 t1v, Vec3 cam, float r, float g, float b, float a0, float a1) {
        float[] h0 = rel(h0v, cam), t0 = rel(t0v, cam), h1 = rel(h1v, cam), t1 = rel(t1v, cam);
        KiRenderHelper.vertex(buffer, mat, pose, h0[0], h0[1], h0[2], r, g, b, 0f, 0, 0, 0, 1, 0);
        KiRenderHelper.vertex(buffer, mat, pose, t0[0], t0[1], t0[2], r, g, b, a0, 1, 0, 0, 1, 0);
        KiRenderHelper.vertex(buffer, mat, pose, t1[0], t1[1], t1[2], r, g, b, a1, 1, 1, 0, 1, 0);
        KiRenderHelper.vertex(buffer, mat, pose, h1[0], h1[1], h1[2], r, g, b, 0f, 0, 1, 0, 1, 0);
        // back face so the ribbon is visible from both sides
        KiRenderHelper.vertex(buffer, mat, pose, h1[0], h1[1], h1[2], r, g, b, 0f, 0, 1, 0, -1, 0);
        KiRenderHelper.vertex(buffer, mat, pose, t1[0], t1[1], t1[2], r, g, b, a1, 1, 1, 0, -1, 0);
        KiRenderHelper.vertex(buffer, mat, pose, t0[0], t0[1], t0[2], r, g, b, a0, 1, 0, 0, -1, 0);
        KiRenderHelper.vertex(buffer, mat, pose, h0[0], h0[1], h0[2], r, g, b, 0f, 0, 0, 0, -1, 0);
    }

    private static float[] rel(Vec3 v, Vec3 cam) {
        return new float[] {(float) (v.x - cam.x), (float) (v.y - cam.y), (float) (v.z - cam.z)};
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
            Trail trail = TRAILS.computeIfAbsent(player.getUUID(), id -> new Trail());
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
            if (trail.samples.isEmpty() && clock - trail.lastFlashClock > 20) it.remove();
        }
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

    /** Blaster: a spark burst at the muzzle plus a short tracer streak, no ribbon sweep. */
    private static void flash(Minecraft mc, Player player, WeaponTrailProfiles.Profile p, Trail trail) {
        Vec3[] b = basis(player);
        Vec3 muzzle = b[3].add(b[0].scale(1.15)).add(b[1].scale(0.22)).add(0, -0.08, 0);
        Vec3 end = b[3].add(b[0].scale(p.reach()));
        trail.samples.addLast(new Sample(muzzle, end, clock));
        if (trail.samples.size() > 8) trail.samples.pollFirst();
        if (DbaEffectsConfig.effectDensity() > 0f) {
            for (int i = 0; i < 5; i++) {
                mc.level.addParticle(DbaParticles.SPARK, muzzle.x, muzzle.y, muzzle.z,
                    b[0].x * 0.25 + (player.getRandom().nextDouble() - 0.5) * 0.12,
                    b[0].y * 0.25 + (player.getRandom().nextDouble() - 0.5) * 0.12,
                    b[0].z * 0.25 + (player.getRandom().nextDouble() - 0.5) * 0.12);
            }
        }
    }

    private static void tipParticles(Minecraft mc, Player player, WeaponTrailProfiles.Profile p) {
        Vec3[] b = basis(player);
        Vec3 tipPos = b[3].add(b[0].scale(p.reach() * 0.8));
        ParticleOptions type = switch (p.tip()) {
            case SPARK -> DbaParticles.SPARK;
            case EMBER -> DbaParticles.EMBER;
            case VOID_SMOKE -> DbaParticles.VOID_SMOKE;
            case WIND -> DbaParticles.WIND;
            case NONE -> null;
        };
        if (type == null) return;
        mc.level.addParticle(type, tipPos.x, tipPos.y, tipPos.z,
            (player.getRandom().nextDouble() - 0.5) * 0.05, 0.03, (player.getRandom().nextDouble() - 0.5) * 0.05);
    }
}
