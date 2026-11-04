package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.WarMaceSmashEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * A bowed shell in the ground, now one layer of three: an anchor glow disc
 * grounds it, three counter rotating LayeredRender bands carry the dust ring
 * at the shell's edge, and the original bronze shell stays as the bright core
 * on top. Everything runs the shared FxEnvelope, so the shell builds in over
 * the first ticks instead of appearing at full alpha, peaks, and settles.
 */
public class WarMaceSmashRenderer extends EntityRenderer<WarMaceSmashEntity, WarMaceSmashRenderer.SmashState> {
    public WarMaceSmashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SmashState extends EntityRenderState {
        public float age;
        public boolean heavy;
        public float radius;
        public double ex;
        public double ey;
        public double ez;
    }

    public boolean shouldRender(WarMaceSmashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(WarMaceSmashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SmashState createRenderState() {
        return new SmashState();
    }

    @Override
    public void extractRenderState(WarMaceSmashEntity entity, SmashState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.heavy = entity.isHeavy();
        state.radius = entity.radiusAt(state.age);
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
    }

    @Override
    public void submit(SmashState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float life = state.heavy ? 14.0f : 10.0f;
        float env = FxEnvelope.envelope(state.age, life, 0.15f, 0.35f);
        float bow = (state.heavy ? 1.35f : 0.72f) * (0.35f + 0.65f * env);
        float distScale = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);
        float bandsHeight = state.heavy ? 1.7f : 1.05f;
        RenderType type = KiRenderHelper.kiRenderType();
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            LayeredRender.drawAnchorGlow(pose, buffer, state.radius * 1.02f, 0.05f,
                1.0f, 0.78f, 0.38f, 0.55f * env, distScale);
            LayeredRender.drawVortexBands(pose, buffer, state.radius * 0.82f, bandsHeight, state.age,
                0.85f, 0.62f, 0.28f, env, distScale);
            drawShell(buffer, matrix, pose, state.radius, bow, env, state.heavy, distScale);
        });
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawShell(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                                  float radius, float bow, float fade, boolean heavy, float distScale) {
        int around = FxEnvelope.segments(22, distScale, 12);
        int bands = 5;
        float inner = radius * 0.62f;
        for (int i = 0; i < around; i++) {
            float a0 = (float) (i * Math.PI * 2.0 / around);
            float a1 = (float) ((i + 1) * Math.PI * 2.0 / around);
            float ripple = 1.0f + 0.08f * (float) Math.sin(a0 * 3.0);
            for (int b = 0; b < bands; b++) {
                float t0 = b / (float) bands;
                float t1 = (b + 1) / (float) bands;
                float r0 = (inner + (radius - inner) * t0) * ripple;
                float r1 = (inner + (radius - inner) * t1) * ripple;
                float h0 = bow * (float) Math.sin(t0 * Math.PI);
                float h1 = bow * (float) Math.sin(t1 * Math.PI);
                float edge = b == bands - 1 ? 1.0f : 0.55f + 0.45f * t1;
                float cr = heavy ? 0.72f + 0.28f * edge : 0.78f + 0.16f * edge;
                float cg = heavy ? 0.42f + 0.28f * edge : 0.58f + 0.18f * edge;
                float cb = heavy ? 0.16f : 0.28f + 0.2f * edge;
                float alpha = fade * (0.35f + 0.6f * (float) Math.sin(t1 * Math.PI));
                quad(buffer, matrix, pose, a0, r0, h0, a1, r1, h1, cr, cg, cb, alpha);
                if (b == bands / 2) {
                    quad(buffer, matrix, pose, a0, r0, h0 + 0.05f, a1, r1, h1 + 0.05f, 1.0f, 0.92f, 0.7f, alpha * 0.85f);
                }
            }
        }
    }

    private static void quad(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                             float a0, float r0, float h0, float a1, float r1, float h1,
                             float r, float g, float b, float a) {
        float x0 = (float) Math.cos(a0) * r0;
        float z0 = (float) Math.sin(a0) * r0;
        float x1 = (float) Math.cos(a1) * r0;
        float z1 = (float) Math.sin(a1) * r0;
        float x2 = (float) Math.cos(a1) * r1;
        float z2 = (float) Math.sin(a1) * r1;
        float x3 = (float) Math.cos(a0) * r1;
        float z3 = (float) Math.sin(a0) * r1;
        KiRenderHelper.vertex(buffer, matrix, pose, x0, h0, z0, r, g, b, a, 0, 0, 0, 1, 0);
        KiRenderHelper.vertex(buffer, matrix, pose, x1, h0, z1, r, g, b, a, 1, 0, 0, 1, 0);
        KiRenderHelper.vertex(buffer, matrix, pose, x2, h1, z2, r, g, b, a, 1, 1, 0, 1, 0);
        KiRenderHelper.vertex(buffer, matrix, pose, x3, h1, z3, r, g, b, a, 0, 1, 0, 1, 0);
    }
}
