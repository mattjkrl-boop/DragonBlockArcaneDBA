package com.dragonblockarcanedba.client.render.kit;

import com.dragonblockarcanedba.util.SeedHash;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * A seeded cluster of rock spikes erupting from the ground: the
 * ground.spikes half of the Phase 6 geometry.ground block (the Ox
 * crags, the Void Rift ring, the Evil Spear body burst read as a
 * tight ground ring). Each spike is a tapered four-sided prism
 * rising along a slightly bent spine: a wide base ring, a narrower
 * mid ring offset by a seeded lean (up to 12 percent of the spike
 * height), and a point. Per-face shade steps ride a base-to-tip
 * color gradient, so the cluster reads as fractured physical rock
 * in the weapon palette, never as smooth cones.
 *
 * This is physical matter: draw through
 * ComplexGeometry.submitSolidLit with the host's light sample,
 * never the emissive path.
 *
 * Layouts (positions in the caller's local frame, ground at y 0):
 * - 0 ring: spikes on a circle of the given radius. Slots are
 *   seeded: some slots are skipped and some spikes pair close to
 *   their neighbour, so the ring clusters and gaps instead of
 *   marching evenly. The placed count is still exactly the count
 *   asked for (late slots are forced once the slack runs out).
 * - 1 scatter: seeded points in the disc of the given radius,
 *   each pulled toward one of two to three seeded cluster
 *   centers, so spikes bunch instead of tiling the disc.
 * - 2 fan: spikes along an arc wedge of 100 degrees centered on
 *   local +Z (the wave band's facing convention: forward is +Z
 *   at zero yaw), at the given radius with seeded radial jitter.
 *
 * Budget: 44 vertices per spike (four double-sided lower quads at
 * 8, four tip triangles at 3). The count is hard capped at 24
 * inside the builder, so the ceiling is 1056 vertices per call.
 * Deterministic: the same seed emits the same vertex stream on
 * every client (SeedHash only, no other randomness).
 */
public final class SpikeCluster {
    private SpikeCluster() {}

    /** Hard cap on spikes per call, whatever the data asks for. */
    public static final int MAX_COUNT = 24;
    private static final double FAN_HALF_RAD = Math.toRadians(50.0);

    public static int draw(PoseStack.Pose pose, VertexConsumer buffer, long seed, int count,
                           double radius, double height, int layout,
                           int baseArgb, int tipArgb, float alpha) {
        if (count <= 0 || height <= 0 || alpha <= 0.001f) {
            return 0;
        }
        int n = Math.min(count, MAX_COUNT);
        Matrix4f mat = pose.pose();
        float[] base = KitEmit.rgb(baseArgb);
        float[] tip = KitEmit.rgb(tipArgb);
        int verts = 0;

        if (layout == 1) {
            // Scatter: two to three cluster centers in the disc.
            int centers = 2 + (SeedHash.hash1(seed, 900) < 0.5f ? 0 : 1);
            double[] cx = new double[centers];
            double[] cz = new double[centers];
            for (int c = 0; c < centers; c++) {
                double a = SeedHash.hash1(seed, 910 + c * 2) * Math.PI * 2.0;
                double r = radius * 0.55 * Math.sqrt(SeedHash.hash1(seed, 911 + c * 2));
                cx[c] = Math.cos(a) * r;
                cz[c] = Math.sin(a) * r;
            }
            for (int i = 0; i < n; i++) {
                double a = SeedHash.hash1(seed, i * 7 + 1) * Math.PI * 2.0;
                double r = radius * Math.sqrt(SeedHash.hash1(seed, i * 7 + 2));
                double x = Math.cos(a) * r;
                double z = Math.sin(a) * r;
                int c = Math.min(centers - 1,
                        (int) (SeedHash.hash1(seed, i * 7 + 3) * centers));
                double pull = 0.45 + 0.25 * SeedHash.hash1(seed, i * 7 + 4);
                x += (cx[c] - x) * pull;
                z += (cz[c] - z) * pull;
                verts += spike(mat, pose, buffer, seed, i, x, z, height, base, tip, alpha);
            }
        } else if (layout == 2) {
            // Fan: an arc wedge centered on local +Z.
            for (int i = 0; i < n; i++) {
                double t = n == 1 ? 0.5 : (double) i / (n - 1);
                double ang = -FAN_HALF_RAD + 2.0 * FAN_HALF_RAD * t
                        + (SeedHash.hash1(seed, i * 7 + 1) - 0.5) * 0.22;
                double r = radius * (0.82 + 0.30 * SeedHash.hash1(seed, i * 7 + 2));
                // Local +Z is angle zero: x = r sin(ang), z = r cos(ang).
                double x = Math.sin(ang) * r;
                double z = Math.cos(ang) * r;
                verts += spike(mat, pose, buffer, seed, i, x, z, height, base, tip, alpha);
            }
        } else {
            // Ring: seeded slots with skips and close pairs, but the
            // placed count is exact: once the remaining slots equal
            // the remaining spikes, every slot is forced.
            int slots = n + Math.max(1, n / 4);
            int placed = 0;
            double prevAngle = 0;
            for (int s = 0; s < slots && placed < n; s++) {
                int remainingSlots = slots - s;
                int remainingSpikes = n - placed;
                boolean forced = remainingSlots <= remainingSpikes;
                float skipHash = SeedHash.hash1(seed, 500 + s * 3);
                if (!forced && s > 0 && skipHash < 0.22f) {
                    continue;
                }
                double angle;
                if (placed > 0 && SeedHash.hash1(seed, 500 + s * 3 + 1) < 0.30f) {
                    // Pair close to the previous spike.
                    angle = prevAngle + 0.10 + 0.10 * SeedHash.hash1(seed, 500 + s * 3 + 2);
                } else {
                    angle = Math.PI * 2.0 * s / slots
                            + (SeedHash.hash1(seed, 500 + s * 3 + 2) - 0.5) * 0.30;
                }
                prevAngle = angle;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                verts += spike(mat, pose, buffer, seed, placed, x, z, height, base, tip, alpha);
                placed++;
            }
        }
        return verts;
    }

    /** One bent, tapered four-sided spike at (x, z). Emits 44 vertices. */
    private static int spike(Matrix4f mat, PoseStack.Pose pose, VertexConsumer buffer,
                             long seed, int i, double x, double z, double height,
                             float[] base, float[] tip, float alpha) {
        double scale = 0.7 + 0.6 * SeedHash.hash1(seed, i * 11 + 1);
        double h = height * scale;
        double hw = h * 0.16;
        double roll = SeedHash.hash1(seed, i * 11 + 2) * Math.PI * 0.5;
        // Bent spine: the mid joint leans up to 12 percent of the
        // height in a seeded direction; the tip leans further along
        // the same line, so the spike reads grown, not machined.
        double leanAng = SeedHash.hash1(seed, i * 11 + 3) * Math.PI * 2.0;
        double leanAmt = SeedHash.hash1(seed, i * 11 + 4) * 0.12 * h;
        double leanX = Math.cos(leanAng) * leanAmt;
        double leanZ = Math.sin(leanAng) * leanAmt;
        double midY = h * 0.55;
        double midHw = hw * 0.62;

        Vec3[] baseRing = new Vec3[4];
        Vec3[] midRing = new Vec3[4];
        for (int k = 0; k < 4; k++) {
            double a = roll + k * Math.PI * 0.5;
            double ca = Math.cos(a), sa = Math.sin(a);
            double jit = 0.85 + 0.30 * SeedHash.hash1(seed, i * 11 + 5 + k);
            baseRing[k] = new Vec3(x + ca * hw * jit, 0, z + sa * hw * jit);
            midRing[k] = new Vec3(x + leanX + ca * midHw * jit, midY, z + leanZ + sa * midHw * jit);
        }
        Vec3 tipP = new Vec3(x + leanX * 1.7, h, z + leanZ * 1.7);

        int verts = 0;
        for (int k = 0; k < 4; k++) {
            int k1 = (k + 1) % 4;
            double a = roll + (k + 0.5) * Math.PI * 0.5;
            Vec3 n = new Vec3(Math.cos(a), 0.25, Math.sin(a)).normalize();
            float shade = 0.78f + 0.38f * SeedHash.hash1(seed, i * 17 + k);
            float[] cBase = KitEmit.shade(base, shade);
            float[] cMid = KitEmit.shade(KitEmit.mix(base, tip, 0.55f), shade * 1.05f);
            float[] cTip = KitEmit.shade(tip, shade * 1.12f);
            // Lower frustum face, double-sided.
            verts += KitEmit.quad(buffer, mat, pose,
                    baseRing[k], cBase, alpha, baseRing[k1], cBase, alpha,
                    midRing[k1], cMid, alpha, midRing[k], cMid, alpha, n);
            // Tip triangle, outward winding.
            Vec3 tn = tipP.subtract(midRing[k]).cross(midRing[k1].subtract(midRing[k]));
            tn = tn.lengthSqr() < 1e-9 ? n : tn.normalize();
            KitEmit.v(buffer, mat, pose, midRing[k], cMid, alpha, 0f, 0f, tn);
            KitEmit.v(buffer, mat, pose, tipP, cTip, alpha, 0.5f, 1f, tn);
            KitEmit.v(buffer, mat, pose, midRing[k1], cMid, alpha, 1f, 0f, tn);
            verts += 3;
        }
        return verts;
    }
}
