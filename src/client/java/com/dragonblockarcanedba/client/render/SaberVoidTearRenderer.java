package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.SaberVoidTearEntity;
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
 * Entity Renderer for Saber Void Tear in Minecraft 26.2.
 * Renders an expanding physical 3D void warp tear, dual counter-rotating sonic boom shock rings,
 * forward conical slipstream, and central spatial rift slit.
 */
public class SaberVoidTearRenderer extends EntityRenderer<SaberVoidTearEntity, SaberVoidTearRenderer.VoidTearRenderState> {

    public SaberVoidTearRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class VoidTearRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public boolean isDestination = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public int maxLifetime = 12;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SaberVoidTearEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SaberVoidTearEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public VoidTearRenderState createRenderState() {
        return new VoidTearRenderState();
    }

    @Override
    public void extractRenderState(SaberVoidTearEntity entity, VoidTearRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getTearScale();
        state.isDestination = entity.isDestination();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
    }

    @Override
    public void submit(VoidTearRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        if (progress >= 1.0f) return;

        float fade = 1.0f - smooth(progress);
        float open = smooth(Math.min(1.0f, progress / 0.45f));
        float impact = strike(progress);
        float scale = state.scale * (0.55f + 0.7f * open);
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            float slitH = 1.35f * (0.4f + 0.6f * open) * state.scale;
            float slitW = 0.12f * (1.0f - progress * 0.55f) * state.scale;
            drawRiftSlit(matrix, buffer, slitW, slitH, fade);
            int spikeCount = 6;
            for (int i = 0; i < spikeCount; i++) {
                double angle = (i / (double) spikeCount) * Math.PI * 2.0 + (state.age * 0.12);
                float spikeLen = 1.15f * scale;
                float tipX = (float) Math.cos(angle) * spikeLen;
                float tipY = (float) Math.sin(angle) * spikeLen;
                drawSpike(matrix, buffer, tipX, tipY, 0.10f * (0.45f + 0.55f * open), fade * 0.85f);
            }
            float coneFade = fade * (1.0f - smooth(Math.max(0.0f, (progress - 0.4f) / 0.6f)));
            if (coneFade > 0.02f) {
                drawSonicCone(matrix, buffer, scale, open, coneFade);
            }
            if (impact > 0.02f) {
                float rOuter = 1.15f * scale * (0.45f + impact);
                drawShockRing(matrix, buffer, rOuter, rOuter * 0.78f, 24, 0.0f,
                    0.0f, 0.85f, 1.0f, impact * 0.9f);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    private static float strike(float progress) {
        if (progress >= 0.48f) return 0.0f;
        return (float) Math.sin((progress / 0.48f) * Math.PI);
    }

    private static void drawShockRing(Matrix4f matrix, VertexConsumer consumer, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        float rotRad = (float) Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2) + rotRad;

            float x1 = (float) Math.cos(a1) * rOuter;
            float y1 = (float) Math.sin(a1) * rOuter;
            float x2 = (float) Math.cos(a2) * rOuter;
            float y2 = (float) Math.sin(a2) * rOuter;

            float ix1 = (float) Math.cos(a1) * rInner;
            float iy1 = (float) Math.sin(a1) * rInner;
            float ix2 = (float) Math.cos(a2) * rInner;
            float iy2 = (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, ix1, iy1, 0).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, ix2, iy2, 0).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawSonicCone(Matrix4f matrix, VertexConsumer consumer, float scale, float progress, float fade) {
        int segments = 16;
        float coneLen = 2.2f * progress * scale;
        float coneBaseRadius = 0.2f * scale;
        float coneTipRadius = 1.4f * scale;

        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float bx1 = (float) Math.cos(a1) * coneBaseRadius;
            float by1 = (float) Math.sin(a1) * coneBaseRadius;
            float bx2 = (float) Math.cos(a2) * coneBaseRadius;
            float by2 = (float) Math.sin(a2) * coneBaseRadius;

            float tx1 = (float) Math.cos(a1) * coneTipRadius;
            float ty1 = (float) Math.sin(a1) * coneTipRadius;
            float tx2 = (float) Math.cos(a2) * coneTipRadius;
            float ty2 = (float) Math.sin(a2) * coneTipRadius;

            consumer.addVertex(matrix, bx1, by1, 0).setColor(1.0f, 1.0f, 1.0f, fade * 0.9f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, bx2, by2, 0).setColor(1.0f, 1.0f, 1.0f, fade * 0.9f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, tx2, ty2, coneLen).setColor(0.0f, 0.85f, 1.0f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, tx1, ty1, coneLen).setColor(0.0f, 0.85f, 1.0f, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawRiftSlit(Matrix4f matrix, VertexConsumer consumer, float w, float h, float fade) {
        // Outer dark violet void halo. This used to pass alpha 0.0 on all four
        // vertices, so the halo was dead geometry and the bright slit had no
        // dark rim to sit in. It now fades with the rift.
        consumer.addVertex(matrix, -w * 2.5f, 0, 0).setColor(0.1f, 0.0f, 0.25f, fade * 0.55f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, 0, h * 1.5f, 0).setColor(0.2f, 0.0f, 0.4f, fade * 0.55f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, w * 2.5f, 0, 0).setColor(0.1f, 0.0f, 0.25f, fade * 0.55f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, 0, -h * 1.5f, 0).setColor(0.2f, 0.0f, 0.4f, fade * 0.55f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);

        // Inner glowing cyan-white rift core
        consumer.addVertex(matrix, -w, 0, 0.02f).setColor(0.0f, 0.95f, 1.0f, fade * 0.95f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, 0, h, 0.02f).setColor(1.0f, 1.0f, 1.0f, fade).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, w, 0, 0.02f).setColor(0.0f, 0.95f, 1.0f, fade * 0.95f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, 0, -h, 0.02f).setColor(1.0f, 1.0f, 1.0f, fade).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }

    private static void drawSpike(Matrix4f matrix, VertexConsumer consumer, float tipX, float tipY, float baseW, float fade) {
        float len = (float) Math.sqrt(tipX * tipX + tipY * tipY);
        if (len < 0.001f) return;
        float dx = tipX / len;
        float dy = tipY / len;
        float px = -dy;
        float py = dx;
        int steps = 6;
        for (int i = 0; i < steps; i++) {
            float t1 = i / (float) steps;
            float t2 = (i + 1) / (float) steps;
            float bow1 = (float) Math.sin(t1 * Math.PI) * baseW * 1.6f;
            float bow2 = (float) Math.sin(t2 * Math.PI) * baseW * 1.6f;
            float w1 = baseW * (1.0f - t1);
            float w2 = baseW * (1.0f - t2);
            float x1 = dx * len * t1 + px * bow1;
            float y1 = dy * len * t1 + py * bow1;
            float x2 = dx * len * t2 + px * bow2;
            float y2 = dy * len * t2 + py * bow2;
            float a1 = fade * (1.0f - t1 * 0.85f);
            float a2 = fade * (1.0f - t2 * 0.85f);
            consumer.addVertex(matrix, x1 - px * w1, y1 - py * w1, 0).setColor(0.0f, 0.85f, 1.0f, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1 + px * w1, y1 + py * w1, 0).setColor(0.0f, 0.85f, 1.0f, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2 + px * w2, y2 + py * w2, 0).setColor(0.0f, 0.85f, 1.0f, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2 - px * w2, y2 - py * w2, 0).setColor(0.0f, 0.85f, 1.0f, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            float c1 = w1 * 0.28f;
            float c2 = w2 * 0.28f;
            consumer.addVertex(matrix, x1 - px * c1, y1 - py * c1, 0.02f).setColor(1.0f, 1.0f, 1.0f, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1 + px * c1, y1 + py * c1, 0.02f).setColor(1.0f, 1.0f, 1.0f, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2 + px * c2, y2 + py * c2, 0.02f).setColor(1.0f, 1.0f, 1.0f, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2 - px * c2, y2 - py * c2, 0.02f).setColor(1.0f, 1.0f, 1.0f, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
        consumer.addVertex(matrix, -px * baseW, -py * baseW, 0).setColor(0.0f, 0.85f, 1.0f, fade * 0.7f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, px * baseW, py * baseW, 0).setColor(0.0f, 0.85f, 1.0f, fade * 0.7f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, -dx * baseW * 1.4f + px * baseW * 0.15f, -dy * baseW * 1.4f + py * baseW * 0.15f, 0).setColor(0.0f, 0.85f, 1.0f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, -dx * baseW * 1.4f - px * baseW * 0.15f, -dy * baseW * 1.4f - py * baseW * 0.15f, 0).setColor(0.0f, 0.85f, 1.0f, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }
}
