package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.WarMaceRiseEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
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
 * An upward crescent with a trail. The main crescent climbs as before; two
 * lagging ghost crescents below it and one wider, fainter wisp crescent
 * around it give the swing layered depth, and an expanding anchor glow ring
 * at the base marks the launch for the first few ticks. All layers run the
 * shared FxEnvelope, so the swing builds in and settles instead of popping.
 */
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
        float env = FxEnvelope.envelope(state.age, 8.0f, 0.12f, 0.30f);
        float lift = state.age * 0.22f;
        float radius = 1.15f * (0.45f + 0.55f * open);
        float thick = 0.85f * open;
        RenderType type = KiRenderHelper.kiRenderType();

        // Impact feedback at the base: an expanding glow ring for the first
        // ticks, drawn without the climb so it stays on the launch point.
        if (state.age < 3.5f) {
            float ringAlpha = (1.0f - state.age / 3.5f) * 0.6f;
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
            collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
                LayeredRender.drawAnchorGlow(pose, buffer, 0.8f + state.age * 0.55f, 0.05f,
                    1.0f, 0.8f, 0.45f, ringAlpha, 1.0f);
            });
            poseStack.popPose();
        }

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.translate(0.0f, lift, 0.35f);
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            // Trail ghosts first, then the wide wisp, then the main crescent.
            drawCrescent(buffer, matrix, pose, radius * 0.92f, thick * 0.8f, env * 0.20f, -0.56f);
            drawCrescent(buffer, matrix, pose, radius * 0.96f, thick * 0.9f, env * 0.40f, -0.28f);
            drawCrescent(buffer, matrix, pose, radius * 1.3f, thick * 1.1f, env * 0.30f, 0.06f);
            drawCrescent(buffer, matrix, pose, radius, thick, env, 0.0f);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }

    private static void drawCrescent(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                                     float radius, float thick, float fade, float yOff) {
        int segs = 10;
        for (int i = 0; i < segs; i++) {
            float t0 = i / (float) segs;
            float t1 = (i + 1) / (float) segs;
            float a0 = (float) ((-0.85 + 1.7 * t0) * Math.PI);
            float a1 = (float) ((-0.85 + 1.7 * t1) * Math.PI);
            float bow0 = thick * (float) Math.sin(t0 * Math.PI);
            float bow1 = thick * (float) Math.sin(t1 * Math.PI);
            float x0 = (float) Math.cos(a0) * radius;
            float y0 = (float) Math.sin(a0) * radius * 0.85f + bow0 + yOff;
            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius * 0.85f + bow1 + yOff;
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
