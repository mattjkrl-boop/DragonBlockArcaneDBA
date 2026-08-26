package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.SaberDimensionalLineSlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Entity Renderer for Saber Dimensional Line Slash in Minecraft 26.2.
 * Renders a massive, continuous physical 3D dimensional reality cut beam along the 3D Best-Fit Line Snap Finisher vector:
 * - Multi-faceted diamond laser beam spanning the full length of the snapped targets
 * - Inner blinding white-hot cutting core
 * - Cyan plasma mantle & outer spatial rift distortion sheath
 * - Transversal reality-crack ribs & concentric shock rings.
 */
public class SaberDimensionalLineSlashRenderer extends EntityRenderer<SaberDimensionalLineSlashEntity, SaberDimensionalLineSlashRenderer.LineSlashRenderState> {

    public SaberDimensionalLineSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class LineSlashRenderState extends EntityRenderState {
        public Vec3 direction = new Vec3(1, 0, 0);
        public float lineLength = 10.0f;
        public float scale = 1.0f;
        public float age = 0;
        public int maxLifetime = 20;
    }

    @Override
    public boolean shouldRender(SaberDimensionalLineSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public LineSlashRenderState createRenderState() {
        return new LineSlashRenderState();
    }

    @Override
    public void extractRenderState(SaberDimensionalLineSlashEntity entity, LineSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.direction = entity.getLineDirection();
        state.lineLength = entity.getLineLength();
        state.scale = entity.getSlashScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
    }

    @Override
    public void submit(LineSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        if (progress >= 1.0f) return;

        float fade = (1.0f - progress * progress);
        float scale = state.scale * (1.0f + progress * 0.25f);
        RenderType renderType = KiRenderHelper.kiRenderType();

        Vec3 dir = state.direction.normalize();
        double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float yaw = (float) (Math.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) -(Math.atan2(dir.y, horiz) * (180.0 / Math.PI));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        float halfLen = (state.lineLength * 0.5f) + 1.0f;
        float coreRadius = 0.15f * scale;
        float mantleRadius = 0.55f * scale;
        float outerRadius = 1.15f * scale;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Outer Deep Cyan Spatial Distortion Sheath (Cylindrical volumetric beam)
            drawVolumetricBeam(matrix, buffer, -halfLen, halfLen, outerRadius, 12,
                0.0f, 0.65f, 1.0f, 0.45f * fade);

            // 2. Radiant Cyan Plasma Mantle (Diamond cross-section)
            drawVolumetricBeam(matrix, buffer, -halfLen, halfLen, mantleRadius, 8,
                0.15f, 0.92f, 1.0f, 0.85f * fade);

            // 3. Blinding White-Hot Razor Cutting Core
            drawVolumetricBeam(matrix, buffer, -halfLen, halfLen, coreRadius, 6,
                1.0f, 1.0f, 1.0f, 1.0f * fade);

            // 4. Transversal Reality Crack Ribs along the beam
            int ribCount = Math.max(4, (int) (state.lineLength * 1.5f));
            for (int r = 0; r <= ribCount; r++) {
                float z = -halfLen + (state.lineLength + 2.0f) * (r / (float) ribCount);
                float ribSpan = outerRadius * (1.2f + 0.3f * (float) Math.sin(r * 1.3f + state.age * 0.4f));
                drawTransversalRib(matrix, buffer, z, ribSpan, 0.0f, 0.85f, 1.0f, fade * 0.75f);
            }

            // 5. Concentric Shock Rings at Endpoints and Center
            drawShockRing(matrix, buffer, -halfLen, outerRadius * 1.5f, 0.0f, 0.9f, 1.0f, fade * 0.8f);
            drawShockRing(matrix, buffer, 0.0f, outerRadius * 1.8f, 1.0f, 1.0f, 1.0f, fade * 0.9f);
            drawShockRing(matrix, buffer, halfLen, outerRadius * 1.5f, 0.0f, 0.9f, 1.0f, fade * 0.8f);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawVolumetricBeam(Matrix4f matrix, VertexConsumer consumer, float zStart, float zEnd, float radius, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            consumer.addVertex(matrix, x1, y1, zStart).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2, zStart).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2, zEnd).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, y1, zEnd).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawTransversalRib(Matrix4f matrix, VertexConsumer consumer, float z, float span, float r, float g, float b, float a) {
        // Cross Diamond Rib
        float th = 0.08f;
        consumer.addVertex(matrix, -span, 0, z).setColor(r, g, b, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, 0, span, z).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, span, 0, z).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, 0, -span, z).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }

    private static void drawShockRing(Matrix4f matrix, VertexConsumer consumer, float z, float radius, float r, float g, float b, float a) {
        int segments = 16;
        float rInner = radius * 0.75f;
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            float ix1 = (float) Math.cos(a1) * rInner;
            float iy1 = (float) Math.sin(a1) * rInner;
            float ix2 = (float) Math.cos(a2) * rInner;
            float iy2 = (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, ix1, iy1, z).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, ix2, iy2, z).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, y2, z).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, y1, z).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }
}
