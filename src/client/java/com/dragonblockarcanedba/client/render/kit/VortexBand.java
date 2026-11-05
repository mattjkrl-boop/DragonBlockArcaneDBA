package com.dragonblockarcanedba.client.render.kit;

import com.dragonblockarcanedba.util.SeedHash;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * A raised ring of torn energy slabs: the Saturn ring form from
 * the Z Shockwave picture. Each slab is a chunky arc segment with
 * a seeded span (62 to 92 percent of its slot, so the ring has
 * real gaps), a jittered radius and height, a tangential tilt on
 * its top edge, and a facet shade step; about a quarter of the
 * slabs tip their color toward the core, so the ring glints as it
 * turns. Rotation is a caller parameter (radians, driven by time
 * and a speed the caller owns), which keeps the builder a pure
 * function of its inputs: same seed, same ring, every client.
 * Partial arcs are a slab count and rotation choice away; the
 * full ring is the shipped use.
 *
 * Energy piece: draw through ComplexGeometry.submitEnergy. A
 * physical variant on the lit path is possible but has no
 * consumer yet.
 * Budget: 24 vertices per slab (outer, inner, and top faces,
 * double-sided; the underside is skipped, it is never the read).
 * Callers cap slabs at 48, so the ceiling is 1152 vertices; the
 * charge ring uses 26 slabs for 624.
 */
public final class VortexBand {
    private VortexBand() {}

    /** Slabs whose span hash falls under this fraction are gaps in the ring. */
    private static final float GAP_FRACTION = 0.16f;

    public static int draw(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                           double centerY, double radius, double bandWidth, double slabHeight,
                           int slabs, double rotation,
                           int rimArgb, int midArgb, int coreArgb,
                           float alpha) {
        if (slabs <= 0 || radius <= 0 || bandWidth <= 0 || slabHeight <= 0) {
            return 0;
        }
        Matrix4f mat = pose.pose();
        // rimArgb is accepted so every kit builder speaks the
        // same core/mid/rim language; slab bodies live in mid and
        // core, and the rim shows in the caller's ground ring.
        float[] mid = KitEmit.rgb(midArgb);
        float[] core = KitEmit.rgb(coreArgb);
        double span = Math.PI * 2.0 / slabs;
        int verts = 0;

        for (int s = 0; s < slabs; s++) {
            if (SeedHash.hash1(seed, s * 7 + 1) < GAP_FRACTION) {
                continue;
            }
            double spanFrac = 0.62 + 0.30 * SeedHash.hash1(seed, s * 7 + 2);
            double rJit = (SeedHash.hash1(seed, s * 7 + 3) - 0.5) * radius * 0.05;
            double rIn = radius - bandWidth * 0.5 + rJit;
            double rOut = radius + bandWidth * 0.5 + rJit;
            double yc = centerY + (SeedHash.hash1(seed, s * 7 + 4) - 0.5) * slabHeight * 0.9;
            double tilt = (SeedHash.hash1(seed, s * 7 + 5) - 0.5) * 0.35 / Math.max(1.0, radius);
            float shade = 0.72f + 0.43f * SeedHash.hash1(seed, s * 7 + 6);
            float[] base = SeedHash.hash1(seed, s * 7 + 7) < 0.28f
                    ? KitEmit.mix(mid, core, 0.55f) : mid;
            float[] col = KitEmit.shade(base, shade);
            float[] colTop = KitEmit.shade(base, shade * 1.12f);
            float a = alpha * (0.70f + 0.30f * SeedHash.hash1(seed, s * 7 + 8));

            double aMid = rotation + span * (s + 0.5);
            double a0 = aMid - span * spanFrac * 0.5;
            double a1 = aMid + span * spanFrac * 0.5;
            double yB = yc - slabHeight * 0.5;
            double yT = yc + slabHeight * 0.5;
            // Bottom corners, then top corners with the tilt
            // applied as an angular shift, so slabs lean as they
            // rise instead of standing machined.
            Vec3 ob0 = polar(rOut, a0, yB), ob1 = polar(rOut, a1, yB);
            Vec3 ot0 = polar(rOut, a0 + tilt, yT), ot1 = polar(rOut, a1 + tilt, yT);
            Vec3 ib0 = polar(rIn, a0, yB), ib1 = polar(rIn, a1, yB);
            Vec3 it0 = polar(rIn, a0 + tilt, yT), it1 = polar(rIn, a1 + tilt, yT);
            Vec3 nOut = polar(1.0, aMid, 0);
            Vec3 nIn = nOut.scale(-1.0);
            Vec3 nUp = new Vec3(0, 1, 0);
            // Outer face, inner face, top face.
            verts += KitEmit.quad(buffer, mat, pose,
                    ob0, col, a, ob1, col, a, ot1, colTop, a, ot0, colTop, a, nOut);
            verts += KitEmit.quad(buffer, mat, pose,
                    ib1, col, a, ib0, col, a, it0, colTop, a, it1, colTop, a, nIn);
            verts += KitEmit.quad(buffer, mat, pose,
                    ot0, colTop, a, ot1, colTop, a, it1, colTop, a, it0, colTop, a, nUp);
        }
        return verts;
    }

    private static Vec3 polar(double radius, double angle, double y) {
        return new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
    }
}
