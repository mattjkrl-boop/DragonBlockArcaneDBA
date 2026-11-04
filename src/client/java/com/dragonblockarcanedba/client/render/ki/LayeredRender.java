package com.dragonblockarcanedba.client.render.ki;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * Shared layered geometry for spectacle effects (SPECTACLE-STANDARD point 1).
 * Every big effect is drawn in three layers with the standard band alphas:
 * core 0.85, mid 0.45, outer 0.18, all multiplied by the lifecycle envelope
 * and by the distance scale, so all weapons share one visual language and
 * distant effects step down instead of costing full geometry.
 *
 * All methods draw in entity local space (origin at the effect center, ground
 * at y = 0) inside an existing submitCustomGeometry call.
 */
public final class LayeredRender {
    private LayeredRender() {}

    public static final float ALPHA_CORE = 0.85f;
    public static final float ALPHA_MID = 0.45f;
    public static final float ALPHA_OUTER = 0.18f;

    /** Distance scale from the camera state, for segment counts and alpha. */
    public static float distScale(CameraRenderState camera, double ex, double ey, double ez) {
        if (camera == null || camera.pos == null) return 1f;
        double dx = camera.pos.x - ex, dy = camera.pos.y - ey, dz = camera.pos.z - ez;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return com.dragonblockarcanedba.util.FxEnvelope.distScale(d);
    }

    /**
     * A standing vortex or column in three counter rotating bands:
     * a tight bright core, a theme mid band, and a wide wispy outer band whose
     * alpha dies with height. radiusAt is the mid band radius; the core is
     * 0.38 of it and the outer 1.45 of it.
     */
    public static void drawVortexBands(PoseStack.Pose pose, VertexConsumer buffer,
                                       float radiusAt, float height, float age,
                                       float r, float g, float b, float envelope,
                                       float distScale) {
        if (envelope <= 0.01f) return;
        Matrix4f matrix = pose.pose();
        band(matrix, buffer, pose, radiusAt * 0.38f, height, age * 2.6f,
            Math.min(1f, r + 0.45f), Math.min(1f, g + 0.45f), Math.min(1f, b + 0.45f),
            ALPHA_CORE * envelope, distScale, 0.9f);
        band(matrix, buffer, pose, radiusAt, height, -age * 1.7f,
            r, g, b, ALPHA_MID * envelope, distScale, 0.75f);
        band(matrix, buffer, pose, radiusAt * 1.45f, height * 1.1f, age * 0.9f,
            r, g, b, ALPHA_OUTER * envelope, distScale, 0.55f);
    }

    /** One helical band: stacked rings whose centers rotate and shrink with height. */
    private static void band(Matrix4f matrix, VertexConsumer buffer, PoseStack.Pose pose,
                             float radius, float height, float rotDeg, float r, float g, float b,
                             float alpha, float distScale, float topFade) {
        int segments = com.dragonblockarcanedba.util.FxEnvelope.segments(28, distScale, 10);
        int rings = 6;
        float rot = (float) Math.toRadians(rotDeg);
        for (int ring = 0; ring < rings; ring++) {
            float t0 = ring / (float) rings;
            float t1 = (ring + 1) / (float) rings;
            float y0 = t0 * height;
            float y1 = t1 * height;
            float rad0 = radius * (1f - t0 * 0.35f);
            float rad1 = radius * (1f - t1 * 0.35f);
            float a0 = alpha * (1f - t0 * topFade);
            float a1 = alpha * (1f - t1 * topFade);
            float twist0 = rot + t0 * 1.9f;
            float twist1 = rot + t1 * 1.9f;
            for (int i = 0; i < segments; i++) {
                double s0 = (i / (double) segments) * Math.PI * 2.0;
                double s1 = ((i + 1) / (double) segments) * Math.PI * 2.0;
                float x0 = (float) Math.cos(s0 + twist0) * rad0, z0 = (float) Math.sin(s0 + twist0) * rad0;
                float x1 = (float) Math.cos(s1 + twist0) * rad0, z1 = (float) Math.sin(s1 + twist0) * rad0;
                float x2 = (float) Math.cos(s1 + twist1) * rad1, z2 = (float) Math.sin(s1 + twist1) * rad1;
                float x3 = (float) Math.cos(s0 + twist1) * rad1, z3 = (float) Math.sin(s0 + twist1) * rad1;
                KiRenderHelper.vertex(buffer, matrix, pose, x0, y0, z0, r, g, b, a0, 0, 0, 0, 1, 0);
                KiRenderHelper.vertex(buffer, matrix, pose, x1, y0, z1, r, g, b, a0, 1, 0, 0, 1, 0);
                KiRenderHelper.vertex(buffer, matrix, pose, x2, y1, z2, r, g, b, a1, 1, 1, 0, 1, 0);
                KiRenderHelper.vertex(buffer, matrix, pose, x3, y1, z3, r, g, b, a1, 0, 1, 0, 1, 0);
                KiRenderHelper.vertex(buffer, matrix, pose, x3, y1, z3, r, g, b, a1, 0, 1, 0, -1, 0);
                KiRenderHelper.vertex(buffer, matrix, pose, x2, y1, z2, r, g, b, a1, 1, 1, 0, -1, 0);
                KiRenderHelper.vertex(buffer, matrix, pose, x1, y0, z1, r, g, b, a0, 1, 0, 0, -1, 0);
                KiRenderHelper.vertex(buffer, matrix, pose, x0, y0, z0, r, g, b, a0, 0, 0, 0, -1, 0);
            }
        }
    }

    /** Ground anchor glow: a soft additive style disc that grounds an effect to the world. */
    public static void drawAnchorGlow(PoseStack.Pose pose, VertexConsumer buffer,
                                      float radius, float y, float r, float g, float b,
                                      float alpha, float distScale) {
        if (alpha <= 0.01f) return;
        Matrix4f matrix = pose.pose();
        int segments = com.dragonblockarcanedba.util.FxEnvelope.segments(24, distScale, 10);
        for (int i = 0; i < segments; i++) {
            double a0 = (i / (double) segments) * Math.PI * 2.0;
            double a1 = ((i + 1) / (double) segments) * Math.PI * 2.0;
            float x0 = (float) Math.cos(a0) * radius, z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius, z1 = (float) Math.sin(a1) * radius;
            KiRenderHelper.vertex(buffer, matrix, pose, 0, y, 0, r, g, b, alpha, 0.5f, 0.5f, 0, 1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x0, y, z0, r, g, b, alpha * 0.1f, 0, 0, 0, 1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x1, y, z1, r, g, b, alpha * 0.1f, 1, 0, 0, 1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x1, y, z1, r, g, b, alpha * 0.1f, 1, 0, 0, -1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x0, y, z0, r, g, b, alpha * 0.1f, 0, 0, 0, -1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, 0, y, 0, r, g, b, alpha, 0.5f, 0.5f, 0, -1, 0);
        }
    }
}
