package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.AzureTornadoEntity;
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

public class AzureTornadoRenderer extends EntityRenderer<AzureTornadoEntity, AzureTornadoRenderer.TornadoRenderState> {

    public AzureTornadoRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TornadoRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public boolean isGiant = false;
        public float age = 0;
    }

    @Override
    public TornadoRenderState createRenderState() {
        return new TornadoRenderState();
    }

    @Override
    public void extractRenderState(AzureTornadoEntity entity, TornadoRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getScale();
        state.isGiant = entity.isGiant();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(TornadoRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float scale = state.scale;
        boolean giant = state.isGiant;
        float baseRadius = scale * (giant ? 6.0f : 2.5f);
        float totalHeight = scale * (giant ? 14.0f : 5.5f);

        poseStack.pushPose();
        float rot = state.age * 20.0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            int levels = 6;
            int segments = 16;

            for (int lvl = 0; lvl < levels; lvl++) {
                float y1 = (lvl / (float) levels) * totalHeight;
                float y2 = ((lvl + 1) / (float) levels) * totalHeight;
                float r1 = baseRadius * (0.3f + 0.7f * (lvl / (float) levels));
                float r2 = baseRadius * (0.3f + 0.7f * ((lvl + 1) / (float) levels));

                float alpha = giant ? 0.65f : 0.45f;
                float red = 0.0f;
                float green = 0.8f + (lvl * 0.03f);
                float blue = 1.0f;

                for (int i = 0; i < segments; i++) {
                    double a1 = (i / (double) segments) * Math.PI * 2;
                    double a2 = ((i + 1) / (double) segments) * Math.PI * 2;

                    float x1 = (float) (Math.cos(a1) * r1);
                    float z1 = (float) (Math.sin(a1) * r1);
                    float x2 = (float) (Math.cos(a2) * r1);
                    float z2 = (float) (Math.sin(a2) * r1);

                    float x3 = (float) (Math.cos(a2) * r2);
                    float z3 = (float) (Math.sin(a2) * r2);
                    float x4 = (float) (Math.cos(a1) * r2);
                    float z4 = (float) (Math.sin(a1) * r2);

                    drawQuad(matrix, buffer, x1, y1, z1, x2, y1, z2, x3, y2, z3, x4, y2, z4, red, green, blue, alpha);
                }
            }
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
