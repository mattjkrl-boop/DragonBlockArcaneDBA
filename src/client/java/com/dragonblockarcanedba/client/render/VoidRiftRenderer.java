package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.VoidRiftEntity;
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

public class VoidRiftRenderer extends EntityRenderer<VoidRiftEntity, VoidRiftRenderer.RiftRenderState> {

    public VoidRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class RiftRenderState extends EntityRenderState {
        public float radius = 2.5f;
        public boolean isImploding = false;
        public float age = 0;
    }

    @Override
    public RiftRenderState createRenderState() {
        return new RiftRenderState();
    }

    @Override
    public void extractRenderState(VoidRiftEntity entity, RiftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.isImploding = entity.isImploding();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(RiftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float radius = state.radius;
        float rot = state.age * (state.isImploding ? 25.0f : 8.0f);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Draw spinning concentric void rings
            int segments = 24;
            for (int ring = 1; ring <= 3; ring++) {
                float r = radius * (ring / 3.0f);
                float rInner = r * 0.75f;
                float alpha = state.isImploding ? 0.95f : 0.75f;
                float red = ring == 1 ? 0.1f : (ring == 2 ? 0.3f : 0.6f);
                float green = 0.0f;
                float blue = ring == 1 ? 0.2f : (ring == 2 ? 0.6f : 0.9f);

                for (int i = 0; i < segments; i++) {
                    double a1 = (i / (double) segments) * Math.PI * 2;
                    double a2 = ((i + 1) / (double) segments) * Math.PI * 2;

                    float x1 = (float) (Math.cos(a1) * r);
                    float z1 = (float) (Math.sin(a1) * r);
                    float x2 = (float) (Math.cos(a2) * r);
                    float z2 = (float) (Math.sin(a2) * r);

                    float ix1 = (float) (Math.cos(a1) * rInner);
                    float iz1 = (float) (Math.sin(a1) * rInner);
                    float ix2 = (float) (Math.cos(a2) * rInner);
                    float iz2 = (float) (Math.sin(a2) * rInner);

                    drawQuad(matrix, buffer, ix1, 0, iz1, ix2, 0, iz2, x2, 0, z2, x1, 0, z1, red, green, blue, alpha);
                }
            }

            // Vertical spatial eye tear
            float vHeight = radius * 1.5f;
            float vWidth = radius * 0.3f;
            drawQuad(matrix, buffer, -vWidth, -vHeight, 0, vWidth, -vHeight, 0, vWidth, vHeight, 0, -vWidth, vHeight, 0, 0.8f, 0.1f, 1.0f, 0.85f);
            drawQuad(matrix, buffer, 0, -vHeight, -vWidth, 0, -vHeight, vWidth, 0, vHeight, vWidth, 0, vHeight, -vWidth, 0.5f, 0.0f, 0.8f, 0.85f);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
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
    }
}
