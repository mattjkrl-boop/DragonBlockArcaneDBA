package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.SorrowSlashEntity;
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
 * Entity Renderer for Sorrow Slash in Minecraft 26.2.
 * Renders a massive 3D scythe crescent slash arc with abyssal void silhouette, weeping magenta blade crest, and razor cyan cutting edge.
 */
public class SorrowSlashRenderer extends EntityRenderer<SorrowSlashEntity, SorrowSlashRenderer.SorrowSlashRenderState> {

    public SorrowSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SorrowSlashRenderState extends EntityRenderState {
        public boolean tiltRight = false;
        public boolean isImpactBurst = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
    }

    @Override
    public boolean shouldRender(SorrowSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public SorrowSlashRenderState createRenderState() {
        return new SorrowSlashRenderState();
    }

    @Override
    public void extractRenderState(SorrowSlashEntity entity, SorrowSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tiltRight = entity.getTilt();
        state.isImpactBurst = entity.isImpactBurst();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(SorrowSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float maxLife = state.isImpactBurst ? 16.0f : 30.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        float fade = (1.0f - progress * progress);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        // Diagonal / Scythe reap tilt
        float tiltAngle = state.tiltRight ? 42.0f : -42.0f;
        poseStack.mulPose(Axis.ZP.rotationDegrees(tiltAngle));

        // Slight forward sweep animation
        float scale = state.isImpactBurst ? (1.0f + progress * 0.4f) : 1.0f;
        poseStack.scale(scale, scale, scale);

        float span = 4.2f;
        float chord = 1.35f;
        int segments = 20;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Abyssal Dark Void Outer Silhouette
            drawCrescentArc(matrix, buffer, span + 0.45f, chord + 0.4f, 0.26f, segments,
                0.08f, 0.0f, 0.15f, 0.90f * fade);

            // 2. Weeping Sorrow Radiant Magenta / Violet Blade Crest
            drawCrescentArc(matrix, buffer, span, chord, 0.14f, segments,
                0.80f, 0.05f, 0.85f, 0.95f * fade);

            // 3. Piercing Razor-Sharp Cyan / White Dimensional Edge
            drawCrescentArc(matrix, buffer, span * 0.72f, chord * 0.72f, 0.06f, segments,
                0.90f, 0.98f, 1.0f, 1.0f * fade);

            // 4. Secondary Cross-Cleave Accent if Impact Burst
            if (state.isImpactBurst) {
                drawCrescentArc(matrix, buffer, span * 0.55f, -chord * 0.55f, 0.08f, segments,
                    0.65f, 0.0f, 0.90f, 0.75f * fade);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawCrescentArc(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float thickness, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;

            float x1 = t1 * span;
            float z1 = (1.0f - t1 * t1) * chord;
            float x2 = t2 * span;
            float z2 = (1.0f - t2 * t2) * chord;

            float th1 = thickness * (1.0f - Math.abs(t1) * 0.75f);
            float th2 = thickness * (1.0f - Math.abs(t2) * 0.75f);

            float z1Trail = z1 - 0.5f * (1.0f - Math.abs(t1));
            float z2Trail = z2 - 0.5f * (1.0f - Math.abs(t2));

            // Top Face
            consumer.addVertex(matrix, x1, th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, th1, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom Face
            consumer.addVertex(matrix, x1, -th1, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, -th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }
}
