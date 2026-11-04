package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.SpiritCleaveEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * A straight divine sword of light. The spirit sword's swing, not a shared crescent.
 */
public class SpiritCleaveRenderer extends EntityRenderer<SpiritCleaveEntity, SpiritCleaveRenderer.CleaveState> {
    public SpiritCleaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CleaveState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public float age;
        public float life = SpiritCleaveEntity.FLIGHT_TICKS + SpiritCleaveEntity.FADE_TICKS;
    }

    public boolean shouldRender(SpiritCleaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(SpiritCleaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public CleaveState createRenderState() {
        return new CleaveState();
    }

    @Override
    public void extractRenderState(SpiritCleaveEntity entity, CleaveState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTick;
        state.life = entity.getLifeTicks();
    }

    @Override
    public void submit(CleaveState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float open = ease(Math.min(1.0f, state.age / 3.0f));
        // One envelope for the whole life (spectacle standard point 5): the
        // same FxEnvelope the server emits particles with, driven by the
        // synced life, so the blade builds, peaks, and dissipates with its
        // trail instead of fading on a private 8 tick clock.
        float fade = FxEnvelope.envelope(state.age, state.life, 0.08f, 0.30f);
        float length = 2.25f * (0.30f + 0.70f * open);
        float tipFlash = ease(Math.max(0.0f, 1.0f - Math.max(0.0f, state.age - 4.0f) / 4.0f));
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        RenderType type = KiRenderHelper.kiRenderType();
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            float a = 0.95f * fade;
            drawSword(buffer, matrix, pose, 0.22f, length, 0.05f, 0.16f, 1.0f, 0.95f, 0.72f, a);
            drawSword(buffer, matrix, pose, 0.015f, length + 0.16f, 0.012f, 0.10f, 1.0f, 1.0f, 1.0f, a);
            // Layered trail with depth (point 1): a wide faint gold wake
            // behind a cyan mid wake behind the bright core wake, so the
            // blade drags three fading layers instead of one flat ribbon.
            drawSwordWake(buffer, matrix, pose, 0.34f, 1.50f, 1.0f, 0.80f, 0.30f, a * 0.30f);
            drawSwordWake(buffer, matrix, pose, 0.24f, 1.00f, 0.0f, 0.90f, 1.0f, a * 0.50f);
            drawSwordWake(buffer, matrix, pose, 0.16f, 0.55f, 1.0f, 0.92f, 0.55f, a);
            drawGuardHorns(buffer, matrix, pose, a);
            if (tipFlash > 0.02f) {
                drawTipBurst(buffer, matrix, pose, length, tipFlash, a);
            }
        });
        // Glow halo pass: a fat, faint copy of the blade so the cut reads
        // as light at range. Alpha steps down with camera distance.
        if (fade > 0.02f) {
            float haloAlpha = 0.16f * fade * (0.35f + 0.65f * distScale);
            collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
                Matrix4f matrix = pose.pose();
                drawSword(buffer, matrix, pose, 0.36f, length, 0.12f, 0.16f, 0.55f, 0.95f, 1.0f, haloAlpha);
            });
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }

    private static void v(com.mojang.blaze3d.vertex.VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                          float x, float y, float z, float r, float g, float b, float a) {
        KiRenderHelper.vertex(buffer, matrix, pose, x, y, z, r, g, b, a, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
    }

    private static void strip(com.mojang.blaze3d.vertex.VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                              float x1, float y1, float z1, float x2, float y2, float z2,
                              float x3, float y3, float z3, float x4, float y4, float z4,
                              float r, float g, float b, float a1, float a2, float a3, float a4) {
        v(buffer, matrix, pose, x1, y1, z1, r, g, b, a1);
        v(buffer, matrix, pose, x2, y2, z2, r, g, b, a2);
        v(buffer, matrix, pose, x3, y3, z3, r, g, b, a3);
        v(buffer, matrix, pose, x4, y4, z4, r, g, b, a4);
    }

    /** Tapered sword with a ridge and a slight bow, so the blade is not one flat triangle. */
    private static void drawSword(com.mojang.blaze3d.vertex.VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                                  float halfWidth, float length, float thick, float bow, float r, float g, float b, float a) {
        int segs = 9;
        for (int i = 0; i < segs; i++) {
            float t1 = i / (float) segs;
            float t2 = (i + 1) / (float) segs;
            float w1 = halfWidth * (1.0f - t1 * t1);
            float w2 = halfWidth * (1.0f - t2 * t2);
            float y1 = bow * (float) Math.sin(t1 * Math.PI);
            float y2 = bow * (float) Math.sin(t2 * Math.PI);
            float z1 = 0.05f + t1 * length;
            float z2 = 0.05f + t2 * length;
            float h1 = thick * (1.0f - t1);
            float h2 = thick * (1.0f - t2);
            float e1 = a * (0.55f + 0.45f * (1.0f - t1));
            float e2 = a * (0.55f + 0.45f * (1.0f - t2));
            strip(buffer, matrix, pose, -w1, y1, z1, 0.0f, y1 + h1, z1, 0.0f, y2 + h2, z2, -w2, y2, z2, r, g, b, e1, e1, e2, e2);
            strip(buffer, matrix, pose, 0.0f, y1 + h1, z1, w1, y1, z1, w2, y2, z2, 0.0f, y2 + h2, z2, r, g, b, e1, e1, e2, e2);
            strip(buffer, matrix, pose, -w1, y1, z1, -w2, y2, z2, 0.0f, y2 - h2, z2, 0.0f, y1 - h1, z1, r, g, b, e1, e2, e2, e1);
            strip(buffer, matrix, pose, w1, y1, z1, 0.0f, y1 - h1, z1, 0.0f, y2 - h2, z2, w2, y2, z2, r, g, b, e1, e1, e2, e2);
            float lip1 = Math.max(0.004f, w1 * 0.16f);
            float lip2 = Math.max(0.004f, w2 * 0.16f);
            strip(buffer, matrix, pose, -w1, y1, z1, -w1 + lip1, y1, z1, -w2 + lip2, y2, z2, -w2, y2, z2, 1.0f, 0.92f, 0.55f, e1, e1, e2 * 0.15f, e2 * 0.15f);
            strip(buffer, matrix, pose, w1 - lip1, y1, z1, w1, y1, z1, w2, y2, z2, w2 - lip2, y2, z2, 1.0f, 0.92f, 0.55f, e1, e1, e2 * 0.15f, e2 * 0.15f);
        }
    }

    private static void drawSwordWake(com.mojang.blaze3d.vertex.VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                                      float halfWidth, float back, float r, float g, float b, float a) {
        int segs = 6;
        for (int i = 0; i < segs; i++) {
            float t1 = i / (float) segs;
            float t2 = (i + 1) / (float) segs;
            float z1 = -back * t1;
            float z2 = -back * t2;
            float y1 = -0.12f * (float) Math.sin(t1 * Math.PI);
            float y2 = -0.12f * (float) Math.sin(t2 * Math.PI);
            float w1 = halfWidth * (1.0f - t1 * 0.75f);
            float w2 = halfWidth * (1.0f - t2 * 0.75f);
            float a1 = a * 0.7f * (1.0f - t1);
            float a2 = a * 0.7f * (1.0f - t2);
            strip(buffer, matrix, pose, -w1, y1, z1, w1, y1, z1, w2, y2, z2, -w2, y2, z2, r, g, b, a1, a1, a2, a2);
            strip(buffer, matrix, pose, -w1 * 0.28f, y1 + 0.02f, z1, w1 * 0.28f, y1 + 0.02f, z1, w2 * 0.28f, y2 + 0.02f, z2, -w2 * 0.28f, y2 + 0.02f, z2, 1.0f, 1.0f, 1.0f, a1, a1, a2, a2);
        }
    }

    private static void drawGuardHorns(com.mojang.blaze3d.vertex.VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose, float a) {
        drawHorn(buffer, matrix, pose, -1.0f, a);
        drawHorn(buffer, matrix, pose, 1.0f, a);
    }

    private static void drawHorn(com.mojang.blaze3d.vertex.VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose, float dir, float a) {
        int segs = 4;
        for (int i = 0; i < segs; i++) {
            float t1 = i / (float) segs;
            float t2 = (i + 1) / (float) segs;
            float x1 = dir * (0.16f + t1 * 0.52f);
            float x2 = dir * (0.16f + t2 * 0.52f);
            float y1 = 0.05f * (float) Math.sin(t1 * Math.PI);
            float y2 = 0.05f * (float) Math.sin(t2 * Math.PI);
            float z1 = -0.02f - t1 * 0.28f;
            float z2 = -0.02f - t2 * 0.28f;
            float w1 = 0.05f * (1.0f - t1);
            float w2 = 0.05f * (1.0f - t2);
            float a1 = a * (1.0f - t1 * 0.35f);
            float a2 = a * (1.0f - t2 * 0.35f);
            strip(buffer, matrix, pose, x1, y1 - w1, z1, x1, y1 + w1, z1, x2, y2 + w2, z2, x2, y2 - w2, z2, 1.0f, 0.86f, 0.42f, a1, a1, a2, a2);
            strip(buffer, matrix, pose, x1, y1, z1 - w1 * 0.4f, x1, y1, z1 + w1 * 0.4f, x2, y2, z2 + w2 * 0.25f, x2, y2, z2 - w2 * 0.25f, 1.0f, 1.0f, 0.9f, a1 * 0.8f, a1 * 0.8f, a2 * 0.3f, a2 * 0.3f);
        }
    }

    /** Petals around the point that shrink to nothing as the flash ends. */
    private static void drawTipBurst(com.mojang.blaze3d.vertex.VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                                     float length, float tipFlash, float a) {
        int petals = 6;
        float reach = 0.28f * tipFlash;
        float base = 0.035f * tipFlash;
        float hot = a * tipFlash;
        for (int p = 0; p < petals; p++) {
            float ang = p * ((float) Math.PI * 2.0f / petals);
            float ox = (float) Math.cos(ang);
            float oy = (float) Math.sin(ang);
            float px = -oy * base;
            float py = ox * base;
            float mx = ox * reach * 0.62f;
            float my = oy * reach * 0.45f;
            float mz = length + reach * 0.40f;
            float tz = length + reach;
            strip(buffer, matrix, pose,
                px, py, length,
                -px, -py, length,
                mx * 0.35f, my * 0.35f, mz,
                -mx * 0.35f, -my * 0.35f, mz,
                1.0f, 0.95f, 0.7f, hot, hot, hot * 0.45f, hot * 0.45f);
            strip(buffer, matrix, pose,
                mx * 0.35f, my * 0.35f, mz,
                -mx * 0.35f, -my * 0.35f, mz,
                ox * reach * 0.08f, oy * reach * 0.08f, tz,
                ox * reach * 0.08f, oy * reach * 0.08f, tz,
                1.0f, 1.0f, 1.0f, hot * 0.45f, hot * 0.45f, 0.0f, 0.0f);
        }
    }
}
