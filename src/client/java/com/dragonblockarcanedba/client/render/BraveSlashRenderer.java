package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.BraveSlashEntity;
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
 * Entity Renderer for Brave Slash in MC 26.2.
 * Renders a razor-sharp, glowing golden/cyan aerodynamic crescent energy wave.
 */
public class BraveSlashRenderer extends EntityRenderer<BraveSlashEntity, BraveSlashRenderer.BraveSlashRenderState> {
    public BraveSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BraveSlashRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
    }

    @Override
    public BraveSlashRenderState createRenderState() {
        return new BraveSlashRenderState();
    }

    @Override
    public void extractRenderState(BraveSlashEntity entity, BraveSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(BraveSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        float span = 3.2f;
        float chord = 1.1f;
        int segments = 16;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Radiant Gold Outer Glow Crescent
            drawCrescentArc(matrix, buffer, span + 0.35f, chord + 0.35f, 0.25f, segments, 1.0f, 0.84f, 0.0f, 0.85f);

            // 2. Brilliant Heroic Cyan Inner Core
            drawCrescentArc(matrix, buffer, span, chord, 0.12f, segments, 0.0f, 0.95f, 1.0f, 0.95f);

            // 3. Blinding White Cutting Edge
            drawCrescentArc(matrix, buffer, span * 0.7f, chord * 0.7f, 0.05f, segments, 1.0f, 1.0f, 1.0f, 1.0f);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawCrescentArc(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float thickness, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f; // -1 to 1
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;

            float x1 = t1 * span;
            float z1 = (1.0f - t1 * t1) * chord;
            float x2 = t2 * span;
            float z2 = (1.0f - t2 * t2) * chord;

            // Taper thickness towards tips
            float th1 = thickness * (1.0f - Math.abs(t1) * 0.7f);
            float th2 = thickness * (1.0f - Math.abs(t2) * 0.7f);

            // Trailing edge
            float z1Trail = z1 - 0.4f * (1.0f - Math.abs(t1));
            float z2Trail = z2 - 0.4f * (1.0f - Math.abs(t2));

            // Top quad
            consumer.addVertex(matrix, x1, th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, th1, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom quad
            consumer.addVertex(matrix, x1, -th1, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, -th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }
}
