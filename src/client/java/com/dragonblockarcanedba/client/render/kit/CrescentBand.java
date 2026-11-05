package com.dragonblockarcanedba.client.render.kit;

import com.dragonblockarcanedba.util.SeedHash;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * A vertical arc band of energy: the wave silhouette from the
 * approved pictures, generalized from the Phase 1 inline band in
 * WaveCarrierRenderer. The band stands on yBase, centered on the
 * forward direction, and carries the pictures' three-strip color
 * profile up its height: rim at the ground, mid through the body,
 * a hot core strip peaking at 68 percent of the height, then a
 * hard core edge dissolving to a transparent rim-colored top. The
 * strip table below reproduces the Phase 1 band's stops and alpha
 * multipliers exactly, so at turbulence 0 the emitted vertices are
 * the Phase 1 band's, position for position and color for color.
 * Turbulence displaces each column's radius and top edge with
 * seeded value noise, so the wall reads torn and wind-ripped
 * instead of a clean gradient sheet; the displacement is a pure
 * function of the seed, so every client tears the wall
 * identically.
 *
 * Energy piece: draw through ComplexGeometry.submitEnergy.
 * Budget: 24 vertices per segment (3 strips, double-sided), so
 * 720 at the wave's full-detail 30 segments and 1152 at the 48
 * segment ceiling. Segment counts come from the caller through
 * the FxEnvelope distance discipline, as before.
 */
public final class CrescentBand {
    private CrescentBand() {}

    // The three strips as (bottom height fraction, top height
    // fraction), (bottom color pick, top color pick) into
    // {rim, mid, core}, and (bottom alpha, top alpha) multipliers.
    // Strip 3 starts at the core's hard top edge (0.55) and fades
    // to a transparent rim top.
    private static final double[][] STRIP_Y = {{0.0, 0.35}, {0.35, 0.68}, {0.68, 1.0}};
    private static final int[][] STRIP_COLOR = {{0, 1}, {1, 2}, {2, 0}};
    private static final float[][] STRIP_ALPHA = {{0.75f, 0.90f}, {0.90f, 1.00f}, {0.55f, 0.0f}};

    public static int draw(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                           double fwdX, double fwdZ, double radius, double yBase, double height,
                           double arcRad, int segments,
                           int rimArgb, int midArgb, int coreArgb,
                           float alpha, float turbulence) {
        if (segments <= 0 || height <= 0 || radius <= 0) {
            return 0;
        }
        Matrix4f mat = pose.pose();
        float[][] stopRgb = {KitEmit.rgb(rimArgb), KitEmit.rgb(midArgb), KitEmit.rgb(coreArgb)};
        double perpX = -fwdZ;
        double perpZ = fwdX;
        float turb = Math.max(0f, Math.min(1f, turbulence));
        int verts = 0;

        // Per-column shape: radius breathing and a torn top edge.
        double[] colR = new double[segments + 1];
        double[] colTop = new double[segments + 1];
        for (int i = 0; i <= segments; i++) {
            colR[i] = radius + (SeedHash.noise1(seed, i * 0.37f) - 0.5) * 2.0 * turb * radius * 0.05;
            colTop[i] = 1.0 + (SeedHash.noise1(seed ^ 0x51F7A9L, i * 0.41f) - 0.5) * 2.0 * turb * 0.16;
        }

        for (int strip = 0; strip < 3; strip++) {
            double yFrac0 = STRIP_Y[strip][0];
            double yFrac1 = STRIP_Y[strip][1];
            float[] cBot = stopRgb[STRIP_COLOR[strip][0]];
            float[] cTop = stopRgb[STRIP_COLOR[strip][1]];
            float aBot = alpha * STRIP_ALPHA[strip][0];
            float aTop = alpha * STRIP_ALPHA[strip][1];
            for (int i = 0; i < segments; i++) {
                double a0 = -arcRad * 0.5 + arcRad * i / segments;
                double a1 = -arcRad * 0.5 + arcRad * (i + 1) / segments;
                Vec3 p0 = point(fwdX, fwdZ, perpX, perpZ, a0, colR[i], yBase, height, yFrac0, colTop[i]);
                Vec3 p1 = point(fwdX, fwdZ, perpX, perpZ, a1, colR[i + 1], yBase, height, yFrac0, colTop[i + 1]);
                Vec3 p2 = point(fwdX, fwdZ, perpX, perpZ, a1, colR[i + 1], yBase, height, yFrac1, colTop[i + 1]);
                Vec3 p3 = point(fwdX, fwdZ, perpX, perpZ, a0, colR[i], yBase, height, yFrac1, colTop[i]);
                double na = (a0 + a1) * 0.5;
                Vec3 n = new Vec3(fwdX * Math.cos(na) + perpX * Math.sin(na), 0,
                        fwdZ * Math.cos(na) + perpZ * Math.sin(na));
                verts += KitEmit.quad(buffer, mat, pose,
                        p0, cBot, aBot, p1, cBot, aBot, p2, cTop, aTop, p3, cTop, aTop, n);
            }
        }
        return verts;
    }

    private static Vec3 point(double fwdX, double fwdZ, double perpX, double perpZ,
                              double angle, double radius, double yBase, double height,
                              double yFrac, double colTop) {
        double c = Math.cos(angle), s = Math.sin(angle);
        // The torn top edge scales the column's whole height, so
        // the lower strips lean with the tear instead of shearing.
        double y = yBase + height * yFrac * (1.0 + (colTop - 1.0) * yFrac);
        return new Vec3((fwdX * c + perpX * s) * radius, y, (fwdZ * c + perpZ * s) * radius);
    }
}
