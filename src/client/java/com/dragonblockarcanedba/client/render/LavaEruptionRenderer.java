package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.LavaEruptionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** A lava wave that bows outward along the swing and burns down. */
public class LavaEruptionRenderer extends EntityRenderer<LavaEruptionEntity, LavaEruptionRenderer.EruptState> {
    public LavaEruptionRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class EruptState extends EntityRenderState {
        public float age;
        public float yRot;
        public float reach;
    }

    public boolean shouldRender(LavaEruptionEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(LavaEruptionEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public EruptState createRenderState() {
        return new EruptState();
    }

    @Override
    public void extractRenderState(LavaEruptionEntity entity, EruptState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.yRot = entity.getYRot();
        state.reach = entity.reachAt(state.age);
    }

    @Override
    public void submit(EruptState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float fade = ease(Math.max(0.0f, 1.0f - state.age / 12.0f));
        float bow = (0.45f + 0.9f * ease(Math.min(1.0f, state.age / 4.0f))) * fade;
        RenderType type = KiRenderHelper.kiRenderType();
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            drawWave(buffer, matrix, pose, state.reach, bow, fade);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Mth.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
    }

    private static void drawWave(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                                 float reach, float bow, float fade) {
        int across = 14;
        int bands = 5;
        float arc = 1.15f;
        for (int i = 0; i < across; i++) {
            float u0 = i / (float) across;
            float u1 = (i + 1) / (float) across;
            float a0 = (u0 - 0.5f) * arc * 2.0f;
            float a1 = (u1 - 0.5f) * arc * 2.0f;
            float side = (float) Math.sin(u0 * Math.PI);
            for (int b = 0; b < bands; b++) {
                float t0 = b / (float) bands;
                float t1 = (b + 1) / (float) bands;
                float r0 = reach * (0.25f + 0.75f * t0);
                float r1 = reach * (0.25f + 0.75f * t1);
                float h0 = bow * (float) Math.sin(t0 * Math.PI) * (0.45f + 0.55f * side);
                float h1 = bow * (float) Math.sin(t1 * Math.PI) * (0.45f + 0.55f * side);
                float x0 = (float) Math.sin(a0) * r0;
                float z0 = (float) Math.cos(a0) * r0;
                float x1 = (float) Math.sin(a1) * r0;
                float z1 = (float) Math.cos(a1) * r0;
                float x2 = (float) Math.sin(a1) * r1;
                float z2 = (float) Math.cos(a1) * r1;
                float x3 = (float) Math.sin(a0) * r1;
                float z3 = (float) Math.cos(a0) * r1;
                float alpha = fade * (0.3f + 0.65f * (float) Math.sin(t1 * Math.PI)) * (0.4f + 0.6f * side);
                float edge = t1 > 0.75f ? 1.0f : 0.0f;
                float red = 0.42f + 0.58f * edge;
                float green = 0.06f + 0.62f * edge * (float) Math.sin(t1 * Math.PI);
                float blue = 0.02f;
                KiRenderHelper.vertex(buffer, matrix, pose, x0, h0, z0, red, green, blue, alpha, 0, 0, 0, 1, 0);
                KiRenderHelper.vertex(buffer, matrix, pose, x1, h0, z1, red, green, blue, alpha, 1, 0, 0, 1, 0);
                KiRenderHelper.vertex(buffer, matrix, pose, x2, h1, z2, 1.0f, 0.72f, 0.16f, alpha, 1, 1, 0, 1, 0);
                KiRenderHelper.vertex(buffer, matrix, pose, x3, h1, z3, 1.0f, 0.72f, 0.16f, alpha, 0, 1, 0, 1, 0);
            }
        }
    }
}
