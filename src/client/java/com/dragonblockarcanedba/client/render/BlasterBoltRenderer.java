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
 * Renders an aerodynamic high-energy plasma projectile with cylindrical glow sheath and tapered tracer tail.
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
    }

    @Override
    public void submit(BlasterBoltRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        float radius = state.isOvercharged ? 0.35f : (0.16f + state.heatRatio * 0.12f);
        float headLength = state.isOvercharged ? 1.4f : 0.8f;
        float tailLength = headLength * 1.8f;

        // Plasma palette
        float r = state.isOvercharged ? 1.0f : 1.0f;
        float g = state.isOvercharged ? 0.15f : Math.max(0.2f, 0.85f - state.heatRatio * 0.55f);
        float b = state.isOvercharged ? 0.35f : 0.05f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            int sides = 8;

            // 1. Outer Plasma Sheath (Cylindrical with tapered nose and tail)
            drawPlasmaCylinder(matrix, buffer, radius, headLength, tailLength, sides, r, g, b, 0.85f);

            // 2. White-Hot Intense Plasma Core
            drawPlasmaCylinder(matrix, buffer, radius * 0.45f, headLength * 0.85f, tailLength * 0.6f, sides, 1.0f, 1.0f, 0.95f, 1.0f);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawPlasmaCylinder(Matrix4f matrix, VertexConsumer consumer,
                                          float radius, float headLen, float tailLen, int sides,
                                          float r, float g, float b, float a) {
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2;

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            // Forward cone (Nose)
            consumer.addVertex(matrix, 0, 0, headLen).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, a).setUv(1, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, a).setUv(0, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, 0, 0, headLen).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);

            // Backward tracer tail (Tapered to point with alpha fade)
            consumer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, a).setUv(0, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, a).setUv(1, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, 0, 0, -tailLen).setColor(r, g, b, 0.0f).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, 0, 0, -tailLen).setColor(r, g, b, 0.0f).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
        }
    }
}
