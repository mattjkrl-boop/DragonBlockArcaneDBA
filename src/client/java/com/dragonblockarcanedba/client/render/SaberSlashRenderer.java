package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.SaberSlashEntity;
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
 * Entity Renderer for Saber Slash in Minecraft 26.2.
 * Renders a physical volumetric 3D crescent blade with electric-cyan aura, silver-cyan plasma mantle,
 * razor white-hot cutting edge, aerodynamic wake vanes, and dynamic cross-cleave accents.
 */
public class SaberSlashRenderer extends EntityRenderer<SaberSlashEntity, SaberSlashRenderer.SaberSlashRenderState> {

    public SaberSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SaberSlashRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float tiltAngle = 0.0f;
        public boolean isStrong = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public int maxLifetime = 10;
        public double ex = 0;
        public double ey = 0;
        public double ez = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SaberSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SaberSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SaberSlashRenderState createRenderState() {
        return new SaberSlashRenderState();
    }

    @Override
    public void extractRenderState(SaberSlashEntity entity, SaberSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getSlashScale();
        state.tiltAngle = entity.getTiltAngle();
        state.isStrong = entity.isStrong();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
    }

    @Override
    public void submit(SaberSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        if (progress >= 1.0f) return;

        float open = smooth(Math.min(1.0f, progress / 0.34f));
        float fade = 1.0f - smooth(progress);
        float impact = strike(progress);
        float scale = state.scale * (state.isStrong ? 1.28f : 1.0f);
        float span = 2.55f * scale * (0.70f + 0.30f * open);
        float chord = 0.92f * scale * (0.70f + 0.30f * open);
        float trail = (0.22f + 0.70f * smooth(Math.min(1.0f, progress / 0.7f))) * scale;
        boolean strong = state.isStrong;
        // Segment count steps down with camera distance (standard point 1):
        // near views keep the full 28 segment crescent, distant ones cost less.
        float ds = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);
        int segments = FxEnvelope.segments(28, ds, 12);
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(state.tiltAngle)));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            // Wispy outer layer at the standard outer band alpha, so the cut
            // reads core, mid, edge instead of a single flat mesh crescent.
            drawVolumetricCrescent(matrix, buffer, span * 1.16f, chord * 1.10f, 0.060f, trail * 1.30f, segments, 0.0f, 0.75f, 1.0f, LayeredRender.ALPHA_OUTER * fade);
            drawVolumetricCrescent(matrix, buffer, span, chord, 0.048f, trail, segments, 0.0f, 0.75f, 1.0f, 0.86f * fade);
            drawVolumetricCrescent(matrix, buffer, span * 0.80f, chord * 0.80f, 0.016f, trail * 0.22f, segments, 1.0f, 1.0f, 1.0f, fade);
            drawTipSpurs(matrix, buffer, span, chord, 0.11f * scale, fade);
            if (strong) {
                drawVolumetricCrescent(matrix, buffer, span * 0.76f, -chord * 0.76f, 0.032f, trail * 0.35f, segments, 0.0f, 0.90f, 1.0f, 0.90f * fade);
            }
            if (impact > 0.02f) {
                drawDiamondSpark(matrix, buffer, 0.0f, 0.0f, chord * 0.42f, 0.18f * impact, 1.0f, 1.0f, 1.0f, impact);
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
        if (progress <= 0.22f || progress >= 0.55f) return 0.0f;
        return (float) Math.sin(((progress - 0.22f) / 0.33f) * Math.PI);
    }

    private static void drawVolumetricCrescent(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float thickness, float trailLength, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;

            float x1 = t1 * span;
            float z1 = (1.0f - t1 * t1) * chord;
            float x2 = t2 * span;
            float z2 = (1.0f - t2 * t2) * chord;

            float th1 = thickness * (1.0f - Math.abs(t1) * 0.75f);
            float th2 = thickness * (1.0f - Math.abs(t2) * 0.75f);

            float z1Trail = z1 - trailLength * (1.0f - Math.abs(t1));
            float z2Trail = z2 - trailLength * (1.0f - Math.abs(t2));

            // Top Surface
            consumer.addVertex(matrix, x1, th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2 * 0.3f, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, th1 * 0.3f, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom Surface
            consumer.addVertex(matrix, x1, -th1 * 0.3f, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2 * 0.3f, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, -th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

            // Front Leading Bevel Edge
            consumer.addVertex(matrix, x1, -th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, th2, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, th1, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawTipSpurs(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float spurSize, float fade) {
        drawCurvedSpur(matrix, consumer, -span, -1.0f, spurSize, chord, fade);
        drawCurvedSpur(matrix, consumer, span, 1.0f, spurSize, chord, fade);
    }

    private static void drawCurvedSpur(Matrix4f matrix, VertexConsumer consumer, float tipX, float sign, float spur, float chord, float fade) {
        int steps = 5;
        for (int i = 0; i < steps; i++) {
            float t1 = i / (float) steps;
            float t2 = (i + 1) / (float) steps;
            float z1 = -(spur * 2.1f + chord * 0.08f) * t1;
            float z2 = -(spur * 2.1f + chord * 0.08f) * t2;
            float bow1 = (float) Math.sin(t1 * Math.PI) * spur * 0.55f;
            float bow2 = (float) Math.sin(t2 * Math.PI) * spur * 0.55f;
            float x1 = tipX + sign * (spur * 1.5f * t1 + bow1);
            float x2 = tipX + sign * (spur * 1.5f * t2 + bow2);
            float w1 = spur * 0.22f * (1.0f - t1);
            float w2 = spur * 0.22f * (1.0f - t2);
            float a1 = fade * (1.0f - t1);
            float a2 = fade * (1.0f - t2) * 0.15f;
            consumer.addVertex(matrix, x1, -w1, z1).setColor(0.0f, 0.85f, 1.0f, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, w1, z1).setColor(0.0f, 0.85f, 1.0f, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, w2, z2).setColor(0.8f, 0.95f, 1.0f, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, -w2, z2).setColor(0.8f, 0.95f, 1.0f, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, -w1 * 0.28f, z1).setColor(1.0f, 1.0f, 1.0f, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, w1 * 0.28f, z1).setColor(1.0f, 1.0f, 1.0f, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, w2 * 0.28f, z2).setColor(1.0f, 1.0f, 1.0f, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, -w2 * 0.28f, z2).setColor(1.0f, 1.0f, 1.0f, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawDiamondSpark(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float r, float g, float b, float a) {
        float h = size * 1.4f;
        float w = size * 0.5f;

        consumer.addVertex(matrix, cx - w, cy, cz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx + w, cy, cz).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        consumer.addVertex(matrix, cx, cy, cz - w).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy, cz + w).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        int arms = 4;
        int steps = 3;
        for (int arm = 0; arm < arms; arm++) {
            float ang = arm * ((float) Math.PI * 0.5f) + 0.35f;
            float dx = (float) Math.cos(ang);
            float dz = (float) Math.sin(ang);
            float px = -dz;
            float pz = dx;
            for (int i = 0; i < steps; i++) {
                float t1 = i / (float) steps;
                float t2 = (i + 1) / (float) steps;
                float bow1 = (float) Math.sin(t1 * Math.PI) * size * 0.45f;
                float bow2 = (float) Math.sin(t2 * Math.PI) * size * 0.45f;
                float d1 = size * (0.35f + 1.15f * t1);
                float d2 = size * (0.35f + 1.15f * t2);
                float w1 = size * 0.28f * (1.0f - t1);
                float w2 = size * 0.28f * (1.0f - t2);
                float x1 = cx + dx * d1 + px * bow1;
                float z1 = cz + dz * d1 + pz * bow1;
                float x2 = cx + dx * d2 + px * bow2;
                float z2 = cz + dz * d2 + pz * bow2;
                float a1 = a * (1.0f - t1 * 0.25f);
                float a2 = a * (1.0f - t2);
                consumer.addVertex(matrix, x1 - px * w1, cy, z1 - pz * w1).setColor(r, g, b, a1 * 0.4f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1 + px * w1, cy, z1 + pz * w1).setColor(r, g, b, a1 * 0.4f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 + px * w2, cy, z2 + pz * w2).setColor(r, g, b, a2 * 0.08f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 - px * w2, cy, z2 - pz * w2).setColor(r, g, b, a2 * 0.08f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1 - px * w1 * 0.3f, cy, z1 - pz * w1 * 0.3f).setColor(1.0f, 1.0f, 1.0f, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1 + px * w1 * 0.3f, cy, z1 + pz * w1 * 0.3f).setColor(1.0f, 1.0f, 1.0f, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 + px * w2 * 0.3f, cy, z2 + pz * w2 * 0.3f).setColor(1.0f, 1.0f, 1.0f, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 - px * w2 * 0.3f, cy, z2 - pz * w2 * 0.3f).setColor(1.0f, 1.0f, 1.0f, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            }
        }
    }
}
