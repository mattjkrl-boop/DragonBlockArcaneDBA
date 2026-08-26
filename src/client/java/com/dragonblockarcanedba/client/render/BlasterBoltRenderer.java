package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.BlasterBoltEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * Entity Renderer for Blaster Bolt in MC 26.2.
 * Renders an aerodynamic high-energy physical 3D laser bolt:
 * - Superdense white-hot cylindrical inner core rod with aerodynamic conical head
 * - Multi-faceted fluted plasma compression sheath with heat-based color shifting
 * - 4 physical aerodynamic stabilizer energy vanes radiating along the fuselage
 * - Orbital plasma compression nodes and leading edge cross-aperture flare
 * - Extended 3D ion tracer tail wake with alpha gradient decay
 */
public class BlasterBoltRenderer extends EntityRenderer<BlasterBoltEntity, BlasterBoltRenderer.BlasterBoltRenderState> {
    public BlasterBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BlasterBoltRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public boolean isOvercharged = false;
        public float heatRatio = 0.0f;
        public float age = 0.0f;
    }

    @Override
    public BlasterBoltRenderState createRenderState() {
        return new BlasterBoltRenderState();
    }

    @Override
    public void extractRenderState(BlasterBoltEntity entity, BlasterBoltRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.isOvercharged = entity.isOvercharged();
        state.heatRatio = entity.getHeatRatio();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(BlasterBoltRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        float age = state.age;
        float pulse = 1.0f + 0.08f * (float) Math.sin(age * 2.5f);

        boolean overcharged = state.isOvercharged;
        float heat = state.heatRatio;

        float radius = overcharged ? 0.36f * pulse : (0.18f + heat * 0.12f) * pulse;
        float headLength = overcharged ? 1.4f : (0.85f + heat * 0.35f);
        float tailLength = overcharged ? 3.4f : (2.0f + heat * 1.0f);

        // Color palette based on heat / overcharge
        float r, g, b;
        if (overcharged) {
            r = 1.0f;
            g = 0.12f;
            b = 0.32f;
        } else if (heat > 0.6f) {
            r = 1.0f;
            g = 0.45f;
            b = 0.05f;
        } else {
            r = 1.0f;
            g = 0.88f - heat * 0.30f;
            b = 0.10f;
        }

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            int sides = 8;

            // 1. Outer Fluted Plasma Sheath (Faceted cylinder with nose cone and ion tail)
            drawPlasmaBolt(matrix, buffer, radius, headLength, tailLength, sides, age * 20.0f, r, g, b, 0.88f);

            // 2. Dense White-Hot Plasma Core (Inner high-intensity rod)
            drawPlasmaBolt(matrix, buffer, radius * 0.45f, headLength * 0.9f, tailLength * 0.65f, sides, -age * 30.0f, 1.0f, 1.0f, 0.95f, 1.0f);

            // 3. 4 Aerodynamic Stabilizing Energy Vanes / Fins (swept backward)
            float finSpan = radius * 2.3f;
            float finBaseZ = -headLength * 0.2f;
            float finBackZ = -tailLength * 0.55f;

            for (int f = 0; f < 4; f++) {
                double angle = (f / 4.0) * Math.PI * 2.0 + Math.toRadians(age * 15.0f);
                float fx = (float) Math.cos(angle) * finSpan;
                float fy = (float) Math.sin(angle) * finSpan;

                drawQuad(matrix, buffer,
                    0, 0, headLength * 0.4f,
                    fx, fy, finBaseZ,
                    fx * 0.6f, fy * 0.6f, finBackZ,
                    0, 0, finBackZ * 0.7f,
                    r, g, b, 0.75f
                );
            }

            // 4. Plasma Compression Ringlets (Orbital energy nodes)
            drawPlaneRing(matrix, buffer, 0, 0, 0, radius * 1.35f, radius * 1.05f, 12, age * 40.0f, r, g, b, 0.85f);
            drawPlaneRing(matrix, buffer, 0, 0, -headLength * 0.5f, radius * 1.20f, radius * 0.95f, 12, -age * 45.0f, 1.0f, 1.0f, 0.9f, 0.90f);

            // 5. Leading Edge Cross-Flare (at bolt apex)
            float flareSize = radius * 1.6f;
            drawQuad(matrix, buffer,
                -flareSize, 0, headLength,
                flareSize, 0, headLength,
                0, -flareSize, headLength,
                0, flareSize, headLength,
                1.0f, 1.0f, 1.0f, 0.95f
            );
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawPlasmaBolt(Matrix4f matrix, VertexConsumer consumer,
                                       float radius, float headLen, float tailLen, int sides,
                                       float rotDeg, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2.0 + rotRad;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2.0 + rotRad;

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            // Forward aerodynamic nose cone
            drawTriangle(matrix, consumer,
                0, 0, headLen,
                x2, y2, 0,
                x1, y1, 0,
                r, g, b, a
            );

            // Cylindrical body mid-section
            drawQuad(matrix, consumer,
                x1, y1, 0,
                x2, y2, 0,
                x2, y2, -tailLen * 0.35f,
                x1, y1, -tailLen * 0.35f,
                r, g, b, a
            );

            // Backward ion tracer tail (Tapered to vertex point with alpha fade)
            consumer.addVertex(matrix, x1, y1, -tailLen * 0.35f).setColor(r, g, b, a).setUv(0, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, x2, y2, -tailLen * 0.35f).setColor(r, g, b, a).setUv(1, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, 0, 0, -tailLen).setColor(r, g, b, 0.0f).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, 0, 0, -tailLen).setColor(r, g, b, 0.0f).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);

            // Reverse for tail
            consumer.addVertex(matrix, 0, 0, -tailLen).setColor(r, g, b, 0.0f).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, 0, 0, -tailLen).setColor(r, g, b, 0.0f).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, y2, -tailLen * 0.35f).setColor(r, g, b, a).setUv(1, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, y1, -tailLen * 0.35f).setColor(r, g, b, a).setUv(0, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawPlaneRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0 + rotRad;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0 + rotRad;

            float x1Out = cx + (float) Math.cos(a1) * rOuter;
            float y1Out = cy + (float) Math.sin(a1) * rOuter;
            float x2Out = cx + (float) Math.cos(a2) * rOuter;
            float y2Out = cy + (float) Math.sin(a2) * rOuter;

            float x1In = cx + (float) Math.cos(a1) * rInner;
            float y1In = cy + (float) Math.sin(a1) * rInner;
            float x2In = cx + (float) Math.cos(a2) * rInner;
            float y2In = cy + (float) Math.sin(a2) * rInner;

            drawQuad(matrix, consumer, x1In, y1In, cz, x2In, y2In, cz, x2Out, y2Out, cz, x1Out, y1Out, cz, r, g, b, a);
        }
    }

    private static void drawTriangle(Matrix4f matrix, VertexConsumer consumer,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float x3, float y3, float z3,
                                     float r, float g, float b, float a) {
        drawQuad(matrix, consumer, x1, y1, z1, x2, y2, z2, x3, y3, z3, x1, y1, z1, r, g, b, a);
    }

    private static void drawQuad(Matrix4f matrix, VertexConsumer consumer,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Reverse side
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
