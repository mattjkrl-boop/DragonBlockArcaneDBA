package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.SaberDimensionalLineSlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Entity Renderer for Saber Dimensional Line Slash in Minecraft 26.2.
 * Renders a massive, continuous physical 3D dimensional reality cut beam along the 3D Best-Fit Line Snap Finisher vector:
 * - Multi-faceted diamond laser beam spanning the full length of the snapped targets
 * - Inner blinding white-hot cutting core
 * - Cyan plasma mantle & outer spatial rift distortion sheath
 * - Transversal reality-crack ribs & concentric shock rings.
 */
public class SaberDimensionalLineSlashRenderer extends EntityRenderer<SaberDimensionalLineSlashEntity, SaberDimensionalLineSlashRenderer.LineSlashRenderState> {

    public SaberDimensionalLineSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class LineSlashRenderState extends EntityRenderState {
        public Vec3 direction = new Vec3(1, 0, 0);
        public float lineLength = 10.0f;
        public float scale = 1.0f;
        public float age = 0;
        public int maxLifetime = 20;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SaberDimensionalLineSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SaberDimensionalLineSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public LineSlashRenderState createRenderState() {
        return new LineSlashRenderState();
    }

    @Override
    public void extractRenderState(SaberDimensionalLineSlashEntity entity, LineSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.direction = entity.getLineDirection();
        state.lineLength = entity.getLineLength();
        state.scale = entity.getSlashScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
    }

    @Override
    public void submit(LineSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        if (progress >= 1.0f) return;

        float cut = smooth(Math.min(1.0f, progress / 0.40f));
        float fade = 1.0f - smooth(progress);
        float impact = strike(progress);
        float scale = state.scale;
        RenderType renderType = KiRenderHelper.kiRenderType();

        Vec3 dir = state.direction.normalize();
        double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float yaw = (float) (Math.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) -(Math.atan2(dir.y, horiz) * (180.0 / Math.PI));

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-yaw)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(pitch)));

        float halfLen = ((state.lineLength * 0.5f) + 1.0f) * cut;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            drawVolumetricBeam(matrix, buffer, -halfLen, halfLen, 0.16f * scale, 8,
                0.0f, 0.75f, 1.0f, 0.72f * fade);
            drawVolumetricBeam(matrix, buffer, -halfLen, halfLen, 0.045f * scale, 6,
                1.0f, 1.0f, 1.0f, fade);
            int bands = 3;
            for (int b = 0; b < bands; b++) {
                float bandProg = (state.age * 0.08f + (b / (float) bands)) % 1.0f;
                float bandZ = -halfLen + bandProg * (halfLen * 2.0f);
                drawShockRing(matrix, buffer, bandZ, 0.38f * scale, 0.55f, 0.95f, 1.0f, fade * 0.5f * (1.0f - bandProg * 0.35f));
            }
            if (impact > 0.02f) {
                drawShockRing(matrix, buffer, 0.0f, 0.55f * scale * impact, 1.0f, 1.0f, 1.0f, impact);
            }
            int helix = 18;
            float spin = state.age * 0.45f;
            float edgeR = 0.22f * scale;
            for (int h = 0; h < helix; h++) {
                float t1 = h / (float) helix;
                float t2 = (h + 1) / (float) helix;
                float z1 = -halfLen + (halfLen * 2.0f) * t1;
                float z2 = -halfLen + (halfLen * 2.0f) * t2;
                float taper1 = (float) Math.sin(t1 * Math.PI);
                float taper2 = (float) Math.sin(t2 * Math.PI);
                float ang1 = spin + t1 * (float) Math.PI * 4.0f;
                float ang2 = spin + t2 * (float) Math.PI * 4.0f;
                float x1 = (float) Math.cos(ang1) * edgeR * taper1;
                float y1 = (float) Math.sin(ang1) * edgeR * taper1;
                float x2 = (float) Math.cos(ang2) * edgeR * taper2;
                float y2 = (float) Math.sin(ang2) * edgeR * taper2;
                float px = -(y2 - y1);
                float py = x2 - x1;
                float pLen = (float) Math.sqrt(px * px + py * py);
                if (pLen < 0.0001f) continue;
                px = px / pLen * 0.02f * scale;
                py = py / pLen * 0.02f * scale;
                float ha = fade * taper1;
                buffer.addVertex(matrix, x1 - px, y1 - py, z1).setColor(0.75f, 0.95f, 1.0f, ha).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
                buffer.addVertex(matrix, x1 + px, y1 + py, z1).setColor(0.75f, 0.95f, 1.0f, ha).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
                buffer.addVertex(matrix, x2 + px, y2 + py, z2).setColor(1.0f, 1.0f, 1.0f, fade * taper2 * 0.2f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
                buffer.addVertex(matrix, x2 - px, y2 - py, z2).setColor(1.0f, 1.0f, 1.0f, fade * taper2 * 0.2f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
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
        if (progress <= 0.32f || progress >= 0.62f) return 0.0f;
        return (float) Math.sin(((progress - 0.32f) / 0.30f) * Math.PI);
    }

    private static void drawVolumetricBeam(Matrix4f matrix, VertexConsumer consumer, float zStart, float zEnd, float radius, int segments, float r, float g, float b, float a) {
        int steps = 14;
        float span = zEnd - zStart;
        for (int s = 0; s < steps; s++) {
            float t1 = s / (float) steps;
            float t2 = (s + 1) / (float) steps;
            float z1 = zStart + span * t1;
            float z2 = zStart + span * t2;
            float taper1 = (float) Math.sin(t1 * Math.PI);
            float taper2 = (float) Math.sin(t2 * Math.PI);
            float r1 = radius * (0.18f + 0.82f * taper1);
            float r2 = radius * (0.18f + 0.82f * taper2);
            float a1 = a * (0.2f + 0.8f * taper1);
            float a2 = a * (0.2f + 0.8f * taper2);
            for (int i = 0; i < segments; i++) {
                double ang1 = (i / (double) segments) * Math.PI * 2.0;
                double ang2 = ((i + 1) / (double) segments) * Math.PI * 2.0;
                float c1 = (float) Math.cos(ang1);
                float s1 = (float) Math.sin(ang1);
                float c2 = (float) Math.cos(ang2);
                float s2 = (float) Math.sin(ang2);
                consumer.addVertex(matrix, c1 * r1, s1 * r1, z1).setColor(r, g, b, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(c1, s1, 0);
                consumer.addVertex(matrix, c2 * r1, s2 * r1, z1).setColor(r, g, b, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(c2, s2, 0);
                consumer.addVertex(matrix, c2 * r2, s2 * r2, z2).setColor(r, g, b, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(c2, s2, 0);
                consumer.addVertex(matrix, c1 * r2, s1 * r2, z2).setColor(r, g, b, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(c1, s1, 0);
            }
        }
    }

    private static void drawTransversalRib(Matrix4f matrix, VertexConsumer consumer, float z, float span, float r, float g, float b, float a) {
        // Cross Diamond Rib
        float th = 0.08f;
        consumer.addVertex(matrix, -span, 0, z).setColor(r, g, b, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, 0, span, z).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, span, 0, z).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, 0, -span, z).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }

    private static void drawShockRing(Matrix4f matrix, VertexConsumer consumer, float z, float radius, float r, float g, float b, float a) {
        int segments = 24;
        float rInner = radius * 0.75f;
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            float ix1 = (float) Math.cos(a1) * rInner;
            float iy1 = (float) Math.sin(a1) * rInner;
            float ix2 = (float) Math.cos(a2) * rInner;
            float iy2 = (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, ix1, iy1, z).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, ix2, iy2, z).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, y2, z).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, y1, z).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }
}
