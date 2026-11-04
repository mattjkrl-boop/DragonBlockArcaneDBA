package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.SpikeFlailSpinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Orbiting ball, then the outward crack when the channel lets go. */
public class SpikeFlailSpinRenderer extends EntityRenderer<SpikeFlailSpinEntity, SpikeFlailSpinRenderer.SpinState> {
    public SpikeFlailSpinRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SpinState extends EntityRenderState {
        public float angle;
        public float radius;
        public boolean releasing;
        public int releaseAge;
    }

    public boolean shouldRender(SpikeFlailSpinEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(SpikeFlailSpinEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SpinState createRenderState() {
        return new SpinState();
    }

    @Override
    public void extractRenderState(SpikeFlailSpinEntity entity, SpinState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.angle = entity.angle();
        state.radius = entity.orbitRadius();
        state.releasing = entity.releasing();
        state.releaseAge = entity.releaseAge();
    }

    @Override
    public void submit(SpinState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float fade = state.releasing
            ? ease(Math.max(0.0f, 1.0f - state.releaseAge / 8.0f))
            : 1.0f;
        double rad = Math.toRadians(state.angle);
        float bx = (float) (Math.cos(rad) * state.radius);
        float bz = (float) (Math.sin(rad) * state.radius);
        float by = 1.15f;
        RenderType type = KiRenderHelper.kiRenderType();
        float ballX = bx;
        float ballY = by;
        float ballZ = bz;
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            SpikeFlailSwingRenderer.drawChain(buffer, matrix, pose, 0.25f, 1.0f, 0.15f, ballX, ballY, ballZ, fade);
            if (state.releasing) {
                drawCrack(buffer, matrix, pose, state.radius, fade);
            }
        });
        poseStack.pushPose();
        poseStack.translate(ballX, ballY, ballZ);
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) ->
            SpikeFlailSwingRenderer.drawBall(pose, buffer, 0.4f, fade));
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Mth.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
    }

    private static void drawCrack(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose, float radius, float fade) {
        int around = 20;
        int bands = 4;
        float inner = radius * 0.7f;
        float bow = 0.7f * fade;
        for (int i = 0; i < around; i++) {
            float a0 = (float) (i * Math.PI * 2.0 / around);
            float a1 = (float) ((i + 1) * Math.PI * 2.0 / around);
            for (int b = 0; b < bands; b++) {
                float t0 = b / (float) bands;
                float t1 = (b + 1) / (float) bands;
                float r0 = inner + (radius - inner) * t0;
                float r1 = inner + (radius - inner) * t1;
                float h0 = bow * (float) Math.sin(t0 * Math.PI);
                float h1 = bow * (float) Math.sin(t1 * Math.PI);
                float alpha = fade * (0.25f + 0.7f * (float) Math.sin(t1 * Math.PI));
                float hot = b > bands - 2 ? 1.0f : 0.75f;
                KiRenderHelper.vertex(buffer, matrix, pose, (float) Math.cos(a0) * r0, 0.12f + h0, (float) Math.sin(a0) * r0, hot, hot, 0.9f, alpha, 0, 0, 0, 1, 0);
                KiRenderHelper.vertex(buffer, matrix, pose, (float) Math.cos(a1) * r0, 0.12f + h0, (float) Math.sin(a1) * r0, hot, hot, 0.9f, alpha, 1, 0, 0, 1, 0);
                KiRenderHelper.vertex(buffer, matrix, pose, (float) Math.cos(a1) * r1, 0.12f + h1, (float) Math.sin(a1) * r1, 1.0f, 0.95f, 0.8f, alpha, 1, 1, 0, 1, 0);
                KiRenderHelper.vertex(buffer, matrix, pose, (float) Math.cos(a0) * r1, 0.12f + h1, (float) Math.sin(a0) * r1, 1.0f, 0.95f, 0.8f, alpha, 0, 1, 0, 1, 0);
            }
        }
    }
}
