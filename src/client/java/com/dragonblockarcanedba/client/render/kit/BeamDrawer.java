package com.dragonblockarcanedba.client.render.kit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * An entity-less beam between two points: the research's beam
 * pattern (Iron's and Mahou draw beams with no entity at all; a
 * renderer raycasts or reads synced cast state per frame and
 * draws). The beam is a segmented wiggle chain in the Mahou
 * style: the path bows off its axis with two seeded sine fields
 * animated by age, pinned to zero at both ends, and is drawn as
 * two shells of crossed ribbon pairs, a near-white core and a
 * wider theme halo, so it reads as a volume from every angle
 * without a cylinder. Width tapers toward both ends. The flow
 * read comes from the animated wiggle fields themselves; a
 * textured-canvas UV scroll is a consumer-side upgrade for when
 * a beam attack lands. Duration and fade belong to the caller:
 * pass the envelope-scaled alpha per frame.
 *
 * Energy piece: draw through ComplexGeometry.submitEnergy.
 * There is no consumer yet: Phase 6 authors the beam attacks
 * (the Spirit Cannon pattern) on this, and the Phase 4 harness
 * proves counts now.
 * Budget: 32 vertices per path segment (2 shells, 2 crossed
 * planes, double-sided), segment count 6 to 16 by distance
 * scale, so the ceiling is 512 vertices.
 */
public final class BeamDrawer {
    private BeamDrawer() {}

    public static int draw(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                           Vec3 from, Vec3 to, float age, float width,
                           int coreArgb, int midArgb, float alpha, float distScale) {
        double len = from.distanceTo(to);
        if (len < 1e-4 || width <= 0 || alpha <= 0.001f) {
            return 0;
        }
        Matrix4f mat = pose.pose();
        Vec3 dir = to.subtract(from).scale(1.0 / len);
        Vec3 ref = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 perpA = dir.cross(ref).normalize();
        Vec3 perpB = dir.cross(perpA).normalize();
        int segments = Math.max(6, Math.min(16, Math.round(14 * distScale)));
        float phaseA = (seed & 0xFF) * 0.37f;
        float phaseB = ((seed >> 8) & 0xFF) * 0.29f;

        Vec3[] pts = new Vec3[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            double bow = Math.sin(t * Math.PI) * len * 0.012;
            double wobA = Math.sin(t * Math.PI * 2.0 + age * 1.9 + phaseA) * bow;
            double wobB = Math.cos(t * Math.PI * 1.5 - age * 1.6 + phaseB) * bow;
            pts[i] = from.lerp(to, t).add(perpA.scale(wobA)).add(perpB.scale(wobB));
        }

        float[] core = KitEmit.mix(KitEmit.rgb(midArgb), KitEmit.rgb(coreArgb), 0.75f);
        float[] halo = KitEmit.rgb(midArgb);
        int verts = 0;
        // Halo shell first (wider, dimmer), then the hot core.
        verts += shell(mat, pose, buffer, pts, perpA, perpB, width * 0.5f,
                halo, alpha * 0.38f);
        verts += shell(mat, pose, buffer, pts, perpA, perpB, width * 0.15f,
                core, alpha);
        return verts;
    }

    private static int shell(Matrix4f mat, PoseStack.Pose pose, VertexConsumer buffer,
                             Vec3[] pts, Vec3 perpA, Vec3 perpB, float halfWidth,
                             float[] rgb, float alpha) {
        int verts = 0;
        for (int plane = 0; plane < 2; plane++) {
            Vec3 off = plane == 0 ? perpA : perpB;
            Vec3 n = plane == 0 ? perpB : perpA;
            for (int i = 0; i < pts.length - 1; i++) {
                double t0 = (double) i / (pts.length - 1);
                double t1 = (double) (i + 1) / (pts.length - 1);
                double w0 = halfWidth * endScale(t0);
                double w1 = halfWidth * endScale(t1);
                Vec3 p0 = pts[i].add(off.scale(-w0));
                Vec3 p1 = pts[i].add(off.scale(w0));
                Vec3 p2 = pts[i + 1].add(off.scale(w1));
                Vec3 p3 = pts[i + 1].add(off.scale(-w1));
                verts += KitEmit.quad(buffer, mat, pose,
                        p0, rgb, alpha, p1, rgb, alpha, p2, rgb, alpha, p3, rgb, alpha, n);
            }
        }
        return verts;
    }

    /** Beam width profile: pinched at both ends, full at the middle. */
    private static double endScale(double t) {
        return 0.35 + 0.65 * (1.0 - Math.abs(2.0 * t - 1.0));
    }
}
