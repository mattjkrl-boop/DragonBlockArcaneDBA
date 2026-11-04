package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.WarMaceRiseEntity;
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

/** An upward crescent. It climbs, thins, and goes out. */
public class WarMaceRiseRenderer extends EntityRenderer<WarMaceRiseEntity, WarMaceRiseRenderer.RiseState> {
    public WarMaceRiseRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class RiseState extends EntityRenderState {
        public float age;
        public float yRot;
    }

    public boolean shouldRender(WarMaceRiseEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(WarMaceRiseEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public RiseState createRenderState() {
        return new RiseState();
    }

    @Override
    public void extractRenderState(WarMaceRiseEntity entity, RiseState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.yRot = entity.getYRot();
    }

    @Override
    public void submit(RiseState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float open = ease(Math.min(1.0f, state.age / 3.0f));
        float fade = ease(Math.max(0.0f, 1.0f - state.age / 8.0f));
        float lift = state.age * 0.22f;
        RenderType type = KiRenderHelper.kiRenderType();
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.translate(0.0f, lift, 0.35f);
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            drawCrescent(buffer, matrix, pose, 1.15f * (0.45f + 0.55f * open), 0.85f * open, fade);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }

    private static void drawCrescent(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                                     float radius, float thick, float fade) {
        int segs = 10;
        for (int i = 0; i < segs; i++) {
            float t0 = i / (float) segs;
            float t1 = (i + 1) / (float) segs;
            float a0 = (float) ((-0.85 + 1.7 * t0) * Math.PI);
            float a1 = (float) ((-0.85 + 1.7 * t1) * Math.PI);
            float bow0 = thick * (float) Math.sin(t0 * Math.PI);
            float bow1 = thick * (float) Math.sin(t1 * Math.PI);
            float x0 = (float) Math.cos(a0) * radius;
            float y0 = (float) Math.sin(a0) * radius * 0.85f + bow0;
            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius * 0.85f + bow1;
            float lip = 0.16f + 0.1f * (float) Math.sin(t0 * Math.PI);
            float alpha = fade * (0.4f + 0.6f * (float) Math.sin(t1 * Math.PI));
            KiRenderHelper.vertex(buffer, matrix, pose, x0, y0, -lip, 0.85f, 0.62f, 0.28f, alpha, 0, 0, 0, 0, 1);
            KiRenderHelper.vertex(buffer, matrix, pose, x0, y0, lip, 0.85f, 0.62f, 0.28f, alpha, 1, 0, 0, 0, 1);
            KiRenderHelper.vertex(buffer, matrix, pose, x1, y1, lip, 1.0f, 0.9f, 0.62f, alpha, 1, 1, 0, 0, 1);
            KiRenderHelper.vertex(buffer, matrix, pose, x1, y1, -lip, 1.0f, 0.9f, 0.62f, alpha, 0, 1, 0, 0, 1);
            KiRenderHelper.vertex(buffer, matrix, pose, x0, y0 + 0.08f, 0.0f, 1.0f, 0.95f, 0.8f, alpha, 0, 0, 0, 1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x0, y0 - 0.05f, 0.0f, 0.55f, 0.32f, 0.12f, alpha * 0.8f, 1, 0, 0, 1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x1, y1 - 0.05f, 0.0f, 0.55f, 0.32f, 0.12f, alpha * 0.8f, 1, 1, 0, 1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x1, y1 + 0.08f, 0.0f, 1.0f, 0.95f, 0.8f, alpha, 0, 1, 0, 1, 0);
        }
    }
}
