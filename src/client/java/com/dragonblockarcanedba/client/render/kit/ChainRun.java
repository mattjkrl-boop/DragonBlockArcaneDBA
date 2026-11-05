package com.dragonblockarcanedba.client.render.kit;

import com.dragonblockarcanedba.util.SeedHash;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * A run of individual chain links along a catenary between two
 * anchors: the Curse Chain read from the Cursed Blade picture,
 * where the research is blunt that a flat ribbon will never read
 * as chain. Each link is a flat elongated ring (eight annulus
 * sectors) laid in its own plane along the curve's tangent, and
 * consecutive links roll 90 degrees against each other, the way
 * real chain alternates. The curve sags by the sag parameter (a
 * fraction of the span, peaking at mid-run), so a chain between
 * two points drapes under tension instead of ruling a straight
 * line. Link count derives from the span and link size and is
 * capped, so a long chain thins its links' spacing claim rather
 * than its budget.
 *
 * Energy chains (curses, specters) draw through
 * ComplexGeometry.submitEnergy with the color mixed toward the
 * core; a physical chain would go through submitSolidLit. There
 * is no consumer yet: Phase 6 authors the chain attacks, and the
 * Phase 4 harness proves counts and determinism now.
 * Budget: 64 vertices per link (8 sectors, double-sided), link
 * count capped at 48, so the ceiling is 3072 vertices per run.
 */
public final class ChainRun {
    private ChainRun() {}

    private static final int MAX_LINKS = 48;
    private static final int SECTORS = 8;

    public static int draw(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                           Vec3 from, Vec3 to, double sag, double linkSize,
                           int midArgb, int coreArgb, float alpha) {
        double span = from.distanceTo(to);
        if (span < 1e-4 || linkSize <= 0) {
            return 0;
        }
        Matrix4f mat = pose.pose();
        float[] base = KitEmit.mix(KitEmit.rgb(midArgb), KitEmit.rgb(coreArgb), 0.30f);
        int links = Math.max(2, Math.min(MAX_LINKS,
                (int) Math.ceil(span / (linkSize * 0.8))));
        int verts = 0;

        for (int i = 0; i < links; i++) {
            double t = (i + 0.5) / links;
            Vec3 center = pointAt(from, to, sag, span, t);
            Vec3 tangent = tangentAt(from, to, sag, span, t);
            Vec3 side = tangent.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 1e-6) {
                side = new Vec3(1, 0, 0);
            } else {
                side = side.normalize();
            }
            Vec3 up = side.cross(tangent).normalize();
            double roll = (i % 2) * (Math.PI * 0.5) + (SeedHash.hash1(seed, i * 3 + 1) - 0.5) * 0.3;
            Vec3 bAxis = up.scale(Math.cos(roll)).add(side.scale(Math.sin(roll)));
            Vec3 planeN = tangent.cross(bAxis).normalize();

            double halfA = linkSize * 0.60;
            double halfB = linkSize * 0.30;
            double w = linkSize * 0.09;
            for (int k = 0; k < SECTORS; k++) {
                double th0 = Math.PI * 2.0 * k / SECTORS;
                double th1 = Math.PI * 2.0 * (k + 1) / SECTORS;
                Vec3 o0 = ringPoint(center, tangent, bAxis, th0, halfA + w, halfB + w);
                Vec3 o1 = ringPoint(center, tangent, bAxis, th1, halfA + w, halfB + w);
                Vec3 i1 = ringPoint(center, tangent, bAxis, th1, halfA - w, halfB - w);
                Vec3 i0 = ringPoint(center, tangent, bAxis, th0, halfA - w, halfB - w);
                float shade = (k % 2 == 0 ? 0.82f : 1.05f)
                        * (0.90f + 0.20f * SeedHash.hash1(seed, i * 13 + k));
                float[] col = KitEmit.shade(base, shade);
                verts += KitEmit.quad(buffer, mat, pose,
                        o0, col, alpha, o1, col, alpha, i1, col, alpha, i0, col, alpha, planeN);
            }
        }
        return verts;
    }

    private static Vec3 ringPoint(Vec3 center, Vec3 aAxis, Vec3 bAxis, double theta,
                                  double halfA, double halfB) {
        return center.add(aAxis.scale(Math.cos(theta) * halfA)).add(bAxis.scale(Math.sin(theta) * halfB));
    }

    private static Vec3 pointAt(Vec3 from, Vec3 to, double sag, double span, double t) {
        Vec3 p = from.lerp(to, t);
        double droop = sag * span * 0.22 * (4.0 * t * (1.0 - t));
        return new Vec3(p.x, p.y - droop, p.z);
    }

    private static Vec3 tangentAt(Vec3 from, Vec3 to, double sag, double span, double t) {
        double e = 0.01;
        Vec3 a = pointAt(from, to, sag, span, Math.max(0.0, t - e));
        Vec3 b = pointAt(from, to, sag, span, Math.min(1.0, t + e));
        Vec3 d = b.subtract(a);
        return d.lengthSqr() < 1e-9 ? new Vec3(0, 0, 1) : d.normalize();
    }
}
