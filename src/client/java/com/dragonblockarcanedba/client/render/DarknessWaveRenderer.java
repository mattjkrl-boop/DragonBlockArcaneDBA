package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DarknessWaveEntity;
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
 * Entity Renderer for Darkness Wave in Minecraft 26.2.
 * Renders an expansive, glowing abyssal black/purple crescent darkness slash.
 */
public class DarknessWaveRenderer extends EntityRenderer<DarknessWaveEntity, DarknessWaveRenderer.DarknessWaveRenderState> {
    public DarknessWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DarknessWaveRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public boolean isSecondary = false;
    }

    @Override
    public DarknessWaveRenderState createRenderState() {
        return new DarknessWaveRenderState();
    }

    @Override
    public void extractRenderState(DarknessWaveEntity entity, DarknessWaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.isSecondary = entity.isSecondary();
    }

    @Override
    public void submit(DarknessWaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        float span = state.isSecondary ? 2.8f : 4.0f;
        float chord = state.isSecondary ? 0.9f : 1.3f;
        int segments = 18;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Abyssal Black Outer Glow
            drawCrescentArc(matrix, buffer, span + 0.4f, chord + 0.4f, 0.28f, segments, 0.08f, 0.0f, 0.15f, 0.95f);

            // 2. Dark Purple Void Core
            drawCrescentArc(matrix, buffer, span, chord, 0.12f, segments, 0.55f, 0.05f, 0.85f, 0.92f);

            // 3. Demonic Crimson Flare Edge
            drawCrescentArc(matrix, buffer, span * 0.7f, chord * 0.7f, 0.05f, segments, 0.95f, 0.1f, 0.3f, 0.95f);
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

            // Top
            consumer.addVertex(matrix, x1, th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, th1, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom
            consumer.addVertex(matrix, x1, -th1, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, -th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }
}
