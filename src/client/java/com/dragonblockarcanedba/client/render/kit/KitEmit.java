package com.dragonblockarcanedba.client.render.kit;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Shared vertex emission for the primitive kit, in the same
 * discipline as CarrierGeometry and ComplexGeometry: every vertex
 * goes through KiRenderHelper.vertex, so the light hand-off behind
 * submitSolidLit (KiRenderHelper.withLight) applies to kit pieces
 * exactly as it does to the older builders, and a physical kit
 * piece submitted on the lit path is lit by the world instead of
 * glowing. Colors travel as float rgb triples extracted from ARGB
 * ints; alpha is always a separate per-corner value.
 */
final class KitEmit {
    private KitEmit() {}

    static float[] rgb(int argb) {
        return new float[]{KiRenderHelper.red(argb), KiRenderHelper.green(argb), KiRenderHelper.blue(argb)};
    }

    static float[] mix(float[] a, float[] b, float t) {
        float c = Math.max(0f, Math.min(1f, t));
        return new float[]{
                a[0] + (b[0] - a[0]) * c,
                a[1] + (b[1] - a[1]) * c,
                a[2] + (b[2] - a[2]) * c};
    }

    static float[] shade(float[] rgb, float brightness) {
        return new float[]{
                Math.min(1f, rgb[0] * brightness),
                Math.min(1f, rgb[1] * brightness),
                Math.min(1f, rgb[2] * brightness)};
    }

    static void v(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose, Vec3 p,
                  float[] rgb, float a, float u, float v, Vec3 n) {
        KiRenderHelper.vertex(buffer, mat, pose, (float) p.x, (float) p.y, (float) p.z,
                rgb[0], rgb[1], rgb[2], a, u, v, (float) n.x, (float) n.y, (float) n.z);
    }

    /**
     * Double-sided quad with per-corner color and alpha; the
     * second winding carries the negated normal. Returns 8.
     */
    static int quad(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose,
                    Vec3 p0, float[] c0, float a0,
                    Vec3 p1, float[] c1, float a1,
                    Vec3 p2, float[] c2, float a2,
                    Vec3 p3, float[] c3, float a3, Vec3 n) {
        v(buffer, mat, pose, p0, c0, a0, 0f, 0f, n);
        v(buffer, mat, pose, p1, c1, a1, 1f, 0f, n);
        v(buffer, mat, pose, p2, c2, a2, 1f, 1f, n);
        v(buffer, mat, pose, p3, c3, a3, 0f, 1f, n);
        Vec3 rn = n.scale(-1.0);
        v(buffer, mat, pose, p3, c3, a3, 0f, 1f, rn);
        v(buffer, mat, pose, p2, c2, a2, 1f, 1f, rn);
        v(buffer, mat, pose, p1, c1, a1, 1f, 0f, rn);
        v(buffer, mat, pose, p0, c0, a0, 0f, 0f, rn);
        return 8;
    }

    /**
     * Single-sided quad (outward winding only), for shells whose
     * far side is never the read. Returns 4.
     */
    static int quadSingle(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose,
                          Vec3 p0, float[] c0, float a0,
                          Vec3 p1, float[] c1, float a1,
                          Vec3 p2, float[] c2, float a2,
                          Vec3 p3, float[] c3, float a3, Vec3 n) {
        v(buffer, mat, pose, p0, c0, a0, 0f, 0f, n);
        v(buffer, mat, pose, p1, c1, a1, 1f, 0f, n);
        v(buffer, mat, pose, p2, c2, a2, 1f, 1f, n);
        v(buffer, mat, pose, p3, c3, a3, 0f, 1f, n);
        return 4;
    }
}
