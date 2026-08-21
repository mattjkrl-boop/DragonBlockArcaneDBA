package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.GrandCrescentWaveEntity;
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
 * Entity Renderer for Grand Crescent Wave in Minecraft 26.2.
 * Renders a massive, radiant golden-white crescent energy slash.
 */
public class GrandCrescentWaveRenderer extends EntityRenderer<GrandCrescentWaveEntity, GrandCrescentWaveRenderer.GrandCrescentWaveRenderState> {
    public GrandCrescentWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class GrandCrescentWaveRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
    }

    @Override
    public GrandCrescentWaveRenderState createRenderState() {
        return new GrandCrescentWaveRenderState();
    }

    @Override
    public void extractRenderState(GrandCrescentWaveEntity entity, GrandCrescentWaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(GrandCrescentWaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        float span = 4.2f;
        float chord = 1.4f;
        int segments = 18;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Radiant Golden Outer Flare
            drawCrescentArc(matrix, buffer, span + 0.45f, chord + 0.45f, 0.30f, segments, 1.0f, 0.85f, 0.1f, 0.85f);

            // 2. Pure White Radiant Sharp Core
            drawCrescentArc(matrix, buffer, span, chord, 0.14f, segments, 1.0f, 1.0f, 0.95f, 0.95f);

            // 3. Blinding Blade Edge
            drawCrescentArc(matrix, buffer, span * 0.75f, chord * 0.75f, 0.06f, segments, 1.0f, 1.0f, 1.0f, 1.0f);
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

            float z1Trail = z1 - 0.55f * (1.0f - Math.abs(t1));
            float z2Trail = z2 - 0.55f * (1.0f - Math.abs(t2));

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
