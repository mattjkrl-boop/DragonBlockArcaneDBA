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

        float fade = (1.0f - progress * progress);
        float expansion = 0.6f + progress * 1.4f;
        float scale = state.scale * expansion;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Dual Counter-Rotating Concentric Sonic Shock Rings
            int ringSegments = 28;
            float rOuter = 1.8f * scale;
            float rInner = rOuter * 0.72f;
            drawShockRing(matrix, buffer, rOuter, rInner, ringSegments, state.age * 25.0f,
                0.0f, 0.85f, 1.0f, fade * 0.85f);
            drawShockRing(matrix, buffer, rOuter * 0.75f, rInner * 0.65f, ringSegments, -state.age * 40.0f,
                0.85f, 0.98f, 1.0f, fade * 0.95f);

            // 2. 3D Conical Sonic Boom Slipstream (expanding forward along look vector)
            drawSonicCone(matrix, buffer, scale, progress, fade);

            // 3. Central Vertical Spatial Tear Rift Slit
            float slitH = 1.4f * (1.0f - progress * 0.4f) * state.scale;
            float slitW = 0.15f * (1.0f - progress * 0.8f) * state.scale;
            drawRiftSlit(matrix, buffer, slitW, slitH, fade);

            // 4. 6-Point Radial Void Rupture Spikes
            int spikeCount = 6;
            for (int i = 0; i < spikeCount; i++) {
                double angle = (i / (double) spikeCount) * Math.PI * 2.0 + (state.age * 0.12);
                float spikeLen = 1.6f * scale;
                float tipX = (float) Math.cos(angle) * spikeLen;
                float tipY = (float) Math.sin(angle) * spikeLen;
                drawSpike(matrix, buffer, tipX, tipY, 0.16f * (1.0f - progress), fade);
            }
        });

        // Additive halo pass: a wider faint copy of the outer shock ring so
        // the tear blooms around its dark rim.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            float rOuter = 1.8f * scale * 1.12f;
            drawShockRing(matrix, buffer, rOuter, rOuter * 0.70f, 28, state.age * 25.0f,
                0.0f, 0.85f, 1.0f, fade * 0.16f);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
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
        float normX = -tipY * (baseW / (float) Math.sqrt(tipX * tipX + tipY * tipY));
        float normY = tipX * (baseW / (float) Math.sqrt(tipX * tipX + tipY * tipY));

        consumer.addVertex(matrix, -normX, -normY, 0).setColor(0.0f, 0.85f, 1.0f, fade * 0.8f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, normX, normY, 0).setColor(0.0f, 0.85f, 1.0f, fade * 0.8f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, tipX, tipY, 0).setColor(1.0f, 1.0f, 1.0f, 0.0f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, tipX, tipY, 0).setColor(1.0f, 1.0f, 1.0f, 0.0f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }
}
