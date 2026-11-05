package com.dragonblockarcanedba.client.render.ability;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

/**
 * Small shared quad emitters for the generic carrier renderers: an
 * arc band (the Wave's body), a flat ground ring (domain rims,
 * telegraph markers), and a vertical tube (telegraph pillars). All
 * emit both windings so they read from either side, and all take
 * plain RGBA floats so callers drive them straight from definition
 * colors and envelopes. This is deliberately the simple Phase 1
 * geometry; the primitive kit phase enriches behind these same
 * renderer classes.
 */
final class CarrierGeometry {
    private CarrierGeometry() {}

    static float r(int argb) { return KiRenderHelper.red(argb); }
    static float g(int argb) { return KiRenderHelper.green(argb); }
    static float b(int argb) { return KiRenderHelper.blue(argb); }

    private static void v(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose,
                          double x, double y, double z,
                          float r, float g, float b, float a,
                          float nx, float ny, float nz) {
        KiRenderHelper.vertex(buffer, mat, pose, (float) x, (float) y, (float) z,
                r, g, b, a, 0f, 0f, nx, ny, nz);
    }

    private static void quad(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose,
                             double[] p0, double[] p1, double[] p2, double[] p3,
                             float[] c0, float[] c1, float[] c2, float[] c3,
                             float nx, float ny, float nz) {
        v(buffer, mat, pose, p0[0], p0[1], p0[2], c0[0], c0[1], c0[2], c0[3], nx, ny, nz);
        v(buffer, mat, pose, p1[0], p1[1], p1[2], c1[0], c1[1], c1[2], c1[3], nx, ny, nz);
        v(buffer, mat, pose, p2[0], p2[1], p2[2], c2[0], c2[1], c2[2], c2[3], nx, ny, nz);
        v(buffer, mat, pose, p3[0], p3[1], p3[2], c3[0], c3[1], c3[2], c3[3], nx, ny, nz);
        v(buffer, mat, pose, p3[0], p3[1], p3[2], c3[0], c3[1], c3[2], c3[3], -nx, -ny, -nz);
        v(buffer, mat, pose, p2[0], p2[1], p2[2], c2[0], c2[1], c2[2], c2[3], -nx, -ny, -nz);
        v(buffer, mat, pose, p1[0], p1[1], p1[2], c1[0], c1[1], c1[2], c1[3], -nx, -ny, -nz);
        v(buffer, mat, pose, p0[0], p0[1], p0[2], c0[0], c0[1], c0[2], c0[3], -nx, -ny, -nz);
    }

    /**
     * A vertical arc band centered on the forward direction, standing
     * on yBottom and rising to yTop, at the given radius. Bottom and
     * top colors are full RGBA sets so callers build gradients.
     */
    static void arcBand(PoseStack.Pose pose, VertexConsumer buffer,
                        double fwdX, double fwdZ, double radius,
                        double yBottom, double yTop, double arcRad, int segments,
                        float[] bottom, float[] top) {
        Matrix4f mat = pose.pose();
        double perpX = -fwdZ;
        double perpZ = fwdX;
        for (int i = 0; i < segments; i++) {
            double a0 = -arcRad * 0.5 + arcRad * i / segments;
            double a1 = -arcRad * 0.5 + arcRad * (i + 1) / segments;
            double c0 = Math.cos(a0), s0 = Math.sin(a0);
            double c1 = Math.cos(a1), s1 = Math.sin(a1);
            double[] p0 = {(fwdX * c0 + perpX * s0) * radius, yBottom, (fwdZ * c0 + perpZ * s0) * radius};
            double[] p1 = {(fwdX * c1 + perpX * s1) * radius, yBottom, (fwdZ * c1 + perpZ * s1) * radius};
            double[] p2 = {(fwdX * c1 + perpX * s1) * radius, yTop, (fwdZ * c1 + perpZ * s1) * radius};
            double[] p3 = {(fwdX * c0 + perpX * s0) * radius, yTop, (fwdZ * c0 + perpZ * s0) * radius};
            float nx = (float) ((fwdX * (c0 + c1) + perpX * (s0 + s1)) * 0.5);
            float nz = (float) ((fwdZ * (c0 + c1) + perpZ * (s0 + s1)) * 0.5);
            quad(buffer, mat, pose, p0, p1, p2, p3, bottom, bottom, top, top, nx, 0f, nz);
        }
    }

    /** A flat ring in the XZ plane at height y, centered on (cx, cz). */
    static void flatRing(PoseStack.Pose pose, VertexConsumer buffer,
                         double cx, double cz, double y,
                         double rIn, double rOut, int segments,
                         float r, float g, float b, float a) {
        Matrix4f mat = pose.pose();
        float[] c = {r, g, b, a};
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0 * i / segments;
            double a1 = Math.PI * 2.0 * (i + 1) / segments;
            double[] p0 = {cx + Math.cos(a0) * rIn, y, cz + Math.sin(a0) * rIn};
            double[] p1 = {cx + Math.cos(a1) * rIn, y, cz + Math.sin(a1) * rIn};
            double[] p2 = {cx + Math.cos(a1) * rOut, y, cz + Math.sin(a1) * rOut};
            double[] p3 = {cx + Math.cos(a0) * rOut, y, cz + Math.sin(a0) * rOut};
            quad(buffer, mat, pose, p0, p1, p2, p3, c, c, c, c, 0f, 1f, 0f);
        }
    }

    /** A vertical open tube around (cx, cz) from y0 to y1. */
    static void tube(PoseStack.Pose pose, VertexConsumer buffer,
                     double cx, double cz, double radius, double y0, double y1, int sides,
                     float r, float g, float b, float aBottom, float aTop) {
        Matrix4f mat = pose.pose();
        float[] cb = {r, g, b, aBottom};
        float[] ct = {r, g, b, aTop};
        for (int i = 0; i < sides; i++) {
            double a0 = Math.PI * 2.0 * i / sides;
            double a1 = Math.PI * 2.0 * (i + 1) / sides;
            double[] p0 = {cx + Math.cos(a0) * radius, y0, cz + Math.sin(a0) * radius};
            double[] p1 = {cx + Math.cos(a1) * radius, y0, cz + Math.sin(a1) * radius};
            double[] p2 = {cx + Math.cos(a1) * radius, y1, cz + Math.sin(a1) * radius};
            double[] p3 = {cx + Math.cos(a0) * radius, y1, cz + Math.sin(a0) * radius};
            float nx = (float) Math.cos((a0 + a1) * 0.5);
            float nz = (float) Math.sin((a0 + a1) * 0.5);
            quad(buffer, mat, pose, p0, p1, p2, p3, cb, cb, ct, ct, nx, 0f, nz);
        }
    }
}
