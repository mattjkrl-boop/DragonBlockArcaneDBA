package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.BraveCrossSlashEntity;
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
 * Entity Renderer for Brave Finisher Cross Slash in Minecraft 26.2.
 * Renders physical 3D dual-intersecting volumetric golden-cyan crescent blades ("X" cruciform),
 * central geometric starburst core flare, and piercing diagonal valor vanes.
 */
public class BraveCrossSlashRenderer extends EntityRenderer<BraveCrossSlashEntity, BraveCrossSlashRenderer.CrossSlashRenderState> {

    public BraveCrossSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CrossSlashRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public int maxLifetime = 18;
    }

    @Override
    public boolean shouldRender(BraveCrossSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public CrossSlashRenderState createRenderState() {
        return new CrossSlashRenderState();
    }

    @Override
    public void extractRenderState(BraveCrossSlashEntity entity, CrossSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getSlashScale();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
    }

    @Override
    public void submit(CrossSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        if (progress >= 1.0f) return;

        float fade = 1.0f - (progress * progress);
        float scale = state.scale * (1.0f + progress * 0.35f);

        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            float span = 4.6f * scale;
            float chord = 1.4f * scale;
            int segments = 20;

            // Render Blade 1: Tilted +45 degrees
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(45.0f));
            drawCrossBlade(pose.pose(), buffer, span, chord, segments, fade);
            poseStack.popPose();

            // Render Blade 2: Tilted -45 degrees
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0f));
            drawCrossBlade(pose.pose(), buffer, span, chord, segments, fade);
            poseStack.popPose();

            // Central Geometric Starburst Core Flare
            float flareRadius = (1.4f + progress * 0.8f) * scale;
            drawGeometricStarburst(matrix, buffer, 0, 0, 0.1f, flareRadius, 8, state.age * 25.0f,
                1.0f, 0.88f, 0.20f, fade * 0.95f,
                0.15f, 0.95f, 1.0f, fade * 0.90f
            );

            // Piercing Diagonal Light Vanes (Shooting outward along the diagonals)
            float vaneLen = (3.5f + progress * 2.5f) * scale;
            float vaneWidth = 0.18f * (1.0f - progress * 0.5f) * scale;
            for (int i = 0; i < 4; i++) {
                double angle = Math.toRadians(45.0 + i * 90.0);
                float vx = (float) Math.cos(angle) * vaneLen;
                float vy = (float) Math.sin(angle) * vaneLen;
                drawTaperedBeam(matrix, buffer, 0, 0, 0.05f, vx, vy, 0.05f, vaneWidth, 1.0f, 0.95f, 0.4f, fade * 0.90f);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawCrossBlade(Matrix4f matrix, VertexConsumer buffer, float span, float chord, int segments, float fade) {
        // 1. Radiant Gold Outer Glow Crescent
        drawVolumetricCrescent(matrix, buffer, span + 0.35f, chord + 0.35f, 0.25f, 0.55f, segments,
            1.0f, 0.84f, 0.0f, 0.90f * fade);

        // 2. Heroic Cyan Plasma Inner Core
        drawVolumetricCrescent(matrix, buffer, span, chord, 0.14f, 0.38f, segments,
            0.0f, 0.95f, 1.0f, 0.95f * fade);

        // 3. Pure White Hyper-Velocity Cutting Edge
        drawVolumetricCrescent(matrix, buffer, span * 0.72f, chord * 0.72f, 0.06f, 0.12f, segments,
            1.0f, 1.0f, 1.0f, 1.0f * fade);

        // 4. Transversal Tip Spurs
        drawTipSpurs(matrix, buffer, span, chord, 0.22f, fade);
    }

    private static void drawVolumetricCrescent(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float thickness, float trailLength, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;

            float x1 = t1 * span;
            float z1 = (1.0f - t1 * t1) * chord;
            float x2 = t2 * span;
            float z2 = (1.0f - t2 * t2) * chord;

            float th1 = thickness * (1.0f - Math.abs(t1) * 0.75f);
            float th2 = thickness * (1.0f - Math.abs(t2) * 0.75f);

            float z1Trail = z1 - trailLength * (1.0f - Math.abs(t1));
            float z2Trail = z2 - trailLength * (1.0f - Math.abs(t2));

            // Top Surface
            consumer.addVertex(matrix, x1, th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2 * 0.3f, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, th1 * 0.3f, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom Surface
            consumer.addVertex(matrix, x1, -th1 * 0.3f, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2 * 0.3f, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, -th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

            // Front Leading Bevel Face
            consumer.addVertex(matrix, x1, -th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, th2, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, th1, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawTipSpurs(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float spurSize, float fade) {
        // Left Tip Spur
        consumer.addVertex(matrix, -span, 0, 0).setColor(0.0f, 0.95f, 1.0f, fade).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -span - spurSize * 1.5f, 0, -spurSize * 2.0f).setColor(1.0f, 0.84f, 0.0f, 0.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -span + spurSize * 0.5f, 0, -spurSize).setColor(0.0f, 0.95f, 1.0f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -span, 0, 0).setColor(0.0f, 0.95f, 1.0f, fade).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Right Tip Spur
        consumer.addVertex(matrix, span, 0, 0).setColor(0.0f, 0.95f, 1.0f, fade).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, span + spurSize * 1.5f, 0, -spurSize * 2.0f).setColor(1.0f, 0.84f, 0.0f, 0.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, span - spurSize * 0.5f, 0, -spurSize).setColor(0.0f, 0.95f, 1.0f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, span, 0, 0).setColor(0.0f, 0.95f, 1.0f, fade).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawGeometricStarburst(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float radius, int points, float rotDeg, float rCenter, float gCenter, float bCenter, float aCenter, float rEdge, float gEdge, float bEdge, float aEdge) {
        double rotRad = Math.toRadians(rotDeg);
        int totalSegments = points * 2;
        for (int i = 0; i < totalSegments; i++) {
            double a1 = ((i / (double) totalSegments) * Math.PI * 2.0) + rotRad;
            double a2 = (((i + 1) / (double) totalSegments) * Math.PI * 2.0) + rotRad;

            float r1 = (i % 2 == 0) ? radius : (radius * 0.35f);
            float r2 = ((i + 1) % 2 == 0) ? radius : (radius * 0.35f);

            float x1 = cx + (float) Math.cos(a1) * r1;
            float y1 = cy + (float) Math.sin(a1) * r1;
            float x2 = cx + (float) Math.cos(a2) * r2;
            float y2 = cy + (float) Math.sin(a2) * r2;

            // Front
            consumer.addVertex(matrix, cx, cy, cz).setColor(rCenter, gCenter, bCenter, aCenter).setUv(0.5f, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, y1, cz).setColor(rEdge, gEdge, bEdge, aEdge).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, y2, cz).setColor(rEdge, gEdge, bEdge, aEdge).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, cx, cy, cz).setColor(rCenter, gCenter, bCenter, aCenter).setUv(0.5f, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);

            // Back
            consumer.addVertex(matrix, cx, cy, cz).setColor(rCenter, gCenter, bCenter, aCenter).setUv(0.5f, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, x2, y2, cz).setColor(rEdge, gEdge, bEdge, aEdge).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, x1, y1, cz).setColor(rEdge, gEdge, bEdge, aEdge).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, cx, cy, cz).setColor(rCenter, gCenter, bCenter, aCenter).setUv(0.5f, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
        }
    }

    private static void drawTaperedBeam(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float width, float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) return;

        float nx = (-dy / len) * width;
        float ny = (dx / len) * width;

        consumer.addVertex(matrix, x1 - nx, y1 - ny, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x1 + nx, y1 + ny, z1).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, 0.0f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, 0.0f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }
}
