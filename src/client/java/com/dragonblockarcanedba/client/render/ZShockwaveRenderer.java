package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.ZShockwaveEntity;
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
 * Entity Renderer for Z-Sword Massive Kinetic Shockwave in Minecraft 26.2.
 * Renders a massive, wide-span curved horizontal kinetic wave with radiant golden-white pressure crests.
 */
public class ZShockwaveRenderer extends EntityRenderer<ZShockwaveEntity, ZShockwaveRenderer.ZShockwaveRenderState> {
    public ZShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ZShockwaveRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public boolean isSubWave = false;
        public float yRot = 0;
        public float xRot = 0;
    }

    @Override
    public ZShockwaveRenderState createRenderState() {
        return new ZShockwaveRenderState();
    }

    @Override
    public void extractRenderState(ZShockwaveEntity entity, ZShockwaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.isSubWave = entity.isSubWave();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
    }

    @Override
    public void submit(ZShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        float charge = state.chargeRatio;
        boolean sub = state.isSubWave;
        float span = sub ? (2.5f + charge * 2.5f) : (3.5f + charge * 4.5f);
        float chord = sub ? 1.0f : (1.2f + charge * 0.8f);
        int segments = 20;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Radiant Golden-Orange Kinetic Outer Aura
            drawCrescentArc(matrix, buffer, span + 0.4f, chord + 0.4f, 0.35f, segments, 1.0f, 0.70f, 0.1f, 0.85f);

            // 2. High-Pressure Kinetic White-Gold Shock Crest
            drawCrescentArc(matrix, buffer, span, chord, 0.16f, segments, 1.0f, 1.0f, 0.85f, 0.95f);

            // 3. Ultra-Dense Kinetic Compression Edge
            drawCrescentArc(matrix, buffer, span * 0.75f, chord * 0.75f, 0.08f, segments, 1.0f, 1.0f, 1.0f, 1.0f);
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

            float z1Trail = z1 - 0.6f * (1.0f - Math.abs(t1));
            float z2Trail = z2 - 0.6f * (1.0f - Math.abs(t2));

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
