package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.HollowRushTrailEntity;
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
 * Entity Renderer for Hollow Rush Dash Corridor in Minecraft 26.2.
 * Renders a physical 3D volumetric void Mach corridor, twin counter-spiraling streamlines, origin/destination spatial puncture rings, and a hyper-velocity void core spike.
 */
public class HollowRushTrailRenderer extends EntityRenderer<HollowRushTrailEntity, HollowRushTrailRenderer.TrailRenderState> {

    public HollowRushTrailRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TrailRenderState extends EntityRenderState {
        public float length = 5.0f;
        public boolean isThirdDash = false;
        public float age = 0;
        public float yRot = 0;
        public float xRot = 0;
    }

    @Override
    public boolean shouldRender(HollowRushTrailEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public TrailRenderState createRenderState() {
        return new TrailRenderState();
    }

    @Override
    public void extractRenderState(HollowRushTrailEntity entity, TrailRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.length = entity.getTrailLength();
        state.isThirdDash = entity.isThirdDash();
        state.age = entity.tickCount + partialTicks;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
    }

    @Override
    public void submit(TrailRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = 14.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float alpha = (1.0f - progress) * (state.isThirdDash ? 0.95f : 0.80f);
        float len = Math.max(0.5f, state.length);
        float widthScale = state.isThirdDash ? 1.4f : 1.0f;

        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Volumetric 3D Void Mach Corridor (Faceted 8-sided spatial tunnel)
            int tunnelSegments = 8;
            float corridorRadius = 0.55f * widthScale;
            float coreRadius = 0.22f * widthScale;

            for (int i = 0; i < tunnelSegments; i++) {
                double a1 = (i / (double) tunnelSegments) * Math.PI * 2.0;
                double a2 = ((i + 1) / (double) tunnelSegments) * Math.PI * 2.0;

                float x1 = (float) Math.cos(a1) * corridorRadius;
                float y1 = (float) Math.sin(a1) * corridorRadius;
                float x2 = (float) Math.cos(a2) * corridorRadius;
                float y2 = (float) Math.sin(a2) * corridorRadius;

                // Outer Void Mantle
                drawQuad(matrix, buffer,
                    x1, y1, 0,
                    x2, y2, 0,
                    x2, y2, len,
                    x1, y1, len,
                    0.25f, 0.02f, 0.55f, alpha * 0.70f
                );

                // Inner Cyan Luminescence
                float ix1 = (float) Math.cos(a1) * coreRadius;
                float iy1 = (float) Math.sin(a1) * coreRadius;
                float ix2 = (float) Math.cos(a2) * coreRadius;
                float iy2 = (float) Math.sin(a2) * coreRadius;

                drawQuad(matrix, buffer,
                    ix1, iy1, 0,
                    ix2, iy2, 0,
                    ix2, iy2, len,
                    ix1, iy1, len,
                    0.15f, 0.90f, 1.0f, alpha * 0.85f
                );
            }

            // 2. Twin Counter-Spiraling Void Streamlines
            int ribbonSteps = 14;
            for (int strand = 0; strand < 2; strand++) {
                float strandOffset = strand * (float) Math.PI;
                for (int s = 0; s < ribbonSteps; s++) {
                    float t1 = s / (float) ribbonSteps;
                    float t2 = (s + 1) / (float) ribbonSteps;

                    float z1 = t1 * len;
                    float z2 = t2 * len;

                    float r1 = (corridorRadius * 1.15f) * (0.8f + 0.2f * (float) Math.sin(t1 * Math.PI));
                    float r2 = (corridorRadius * 1.15f) * (0.8f + 0.2f * (float) Math.sin(t2 * Math.PI));

                    double ang1 = strandOffset + (t1 * Math.PI * 3.0) + (state.age * 0.3);
                    double ang2 = strandOffset + (t2 * Math.PI * 3.0) + (state.age * 0.3);

                    float rx1 = (float) Math.cos(ang1) * r1;
                    float ry1 = (float) Math.sin(ang1) * r1;
                    float rx2 = (float) Math.cos(ang2) * r2;
                    float ry2 = (float) Math.sin(ang2) * r2;

                    float rw = 0.16f * widthScale;

                    drawQuad(matrix, buffer,
                        rx1 - rw, ry1, z1,
                        rx1 + rw, ry1, z1,
                        rx2 + rw, ry2, z2,
                        rx2 - rw, ry2, z2,
                        0.75f, 0.10f, 1.0f, alpha * (1.0f - progress * 0.5f)
                    );
                }
            }

            // 3. Origin and Destination Dimensional Puncture Rings
            float ringRadius = (0.75f + progress * 0.6f) * widthScale;
            drawRingXY(matrix, buffer, 0, 0, 0, ringRadius, ringRadius * 0.70f, 16, 0.40f, 0.05f, 0.85f, alpha * 0.85f);
            drawRingXY(matrix, buffer, 0, 0, len, ringRadius * 1.2f, ringRadius * 0.80f, 16, 0.15f, 0.95f, 1.0f, alpha * 0.95f);

            // 4. Hyper-Velocity Void Core Spike
            float spikeR = 0.08f * widthScale;
            drawQuad(matrix, buffer, -spikeR, -spikeR, -0.2f, spikeR, -spikeR, -0.2f, spikeR, -spikeR, len + 0.4f, -spikeR, -spikeR, len + 0.4f, 1.0f, 1.0f, 1.0f, alpha * 0.98f);
            drawQuad(matrix, buffer, -spikeR, spikeR, -0.2f, spikeR, spikeR, -0.2f, spikeR, spikeR, len + 0.4f, -spikeR, spikeR, len + 0.4f, 1.0f, 1.0f, 1.0f, alpha * 0.98f);
            drawQuad(matrix, buffer, -spikeR, -spikeR, -0.2f, -spikeR, spikeR, -0.2f, -spikeR, spikeR, len + 0.4f, -spikeR, -spikeR, len + 0.4f, 1.0f, 1.0f, 1.0f, alpha * 0.98f);
            drawQuad(matrix, buffer, spikeR, -spikeR, -0.2f, spikeR, spikeR, -0.2f, spikeR, spikeR, len + 0.4f, spikeR, -spikeR, len + 0.4f, 1.0f, 1.0f, 1.0f, alpha * 0.98f);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawRingXY(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

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

        // Reverse
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
