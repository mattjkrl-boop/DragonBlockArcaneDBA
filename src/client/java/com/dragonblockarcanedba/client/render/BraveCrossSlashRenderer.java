package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.BraveCrossSlashEntity;
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
 * Entity Renderer for Brave Finisher Cross Slash in Minecraft 26.2.
 * Renders physical 3D dual-intersecting volumetric golden-cyan crescent blades ("X" cruciform),
 * central geometric starburst core flare, and piercing diagonal valor vanes.
 */
public class BraveCrossSlashRenderer extends EntityRenderer<BraveCrossSlashEntity, BraveCrossSlashRenderer.CrossSlashRenderState> {

    public BraveCrossSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CrossSlashRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public int maxLifetime = 40;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BraveCrossSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BraveCrossSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public CrossSlashRenderState createRenderState() {
        return new CrossSlashRenderState();
    }

    @Override
    public void extractRenderState(BraveCrossSlashEntity entity, CrossSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getSlashScale();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
    }

    @Override
    public void submit(CrossSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float life = (float) state.maxLifetime;
        float progress = Math.min(1.0f, state.age / life);
        if (progress >= 1.0f) return;

        // Shared lifecycle over the entity's real 40 tick life: blades draw
        // out across the build fifth, hold through the damage pulses, then
        // dissipate across the last 30 percent.
        float drawOut = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.2f));
        float fade = FxEnvelope.envelope(state.age, life, 0.2f, 0.3f);
        float impact = strike(progress);
        float scale = state.scale;
        int segments = FxEnvelope.segments(28,
            LayeredRender.distScale(cameraState, state.x, state.y, state.z), 12);

        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            float span = 4.6f * scale * drawOut;
            drawCrossBlade(matrix, buffer, span, fade, 45.0f, segments);
            drawCrossBlade(matrix, buffer, span, fade, -45.0f, segments);
            if (impact > 0.02f) {
                drawGeometricStarburst(matrix, buffer, 0, 0, 0.1f, 1.15f * scale * impact, 8, 0.0f,
                    1.0f, 0.88f, 0.20f, impact,
                    0.15f, 0.95f, 1.0f, impact * 0.85f
                );
            }
        });

        // Outer layer (standard point 1): a wider, faint copy of both blades
        // so the cross has a wispy rim around its dense core, the mod's
        // layered translucency technique for glow.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            float span = 4.6f * scale * drawOut * 1.16f;
            drawCrossBlade(matrix, buffer, span, fade * LayeredRender.ALPHA_OUTER, 45.0f, segments);
            drawCrossBlade(matrix, buffer, span, fade * LayeredRender.ALPHA_OUTER, -45.0f, segments);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float strike(float progress) {
        if (progress <= 0.28f || progress >= 0.62f) return 0.0f;
        return (float) Math.sin(((progress - 0.28f) / 0.34f) * Math.PI);
    }

    private static void drawCrossBlade(Matrix4f matrix, VertexConsumer buffer, float span, float fade, float rollDeg, int segments) {
        float rad = (float) Math.toRadians(rollDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        float reach = Math.abs(span);
        float chord = reach * 0.30f;
        drawRolledCrescent(matrix, buffer, reach, chord, 0.16f, 0.42f, segments, cos, sin, 1.0f, 0.84f, 0.0f, 0.90f * fade);
        drawRolledCrescent(matrix, buffer, reach * 0.84f, chord * 0.84f, 0.055f, 0.16f, segments, cos, sin, 0.0f, 0.95f, 1.0f, 0.95f * fade);
        drawRolledCrescent(matrix, buffer, reach * 0.72f, chord * 0.72f, 0.02f, 0.06f, segments, cos, sin, 1.0f, 1.0f, 1.0f, fade);
        drawRolledSpur(matrix, buffer, -reach, 0.20f, cos, sin, fade);
        drawRolledSpur(matrix, buffer, reach, 0.20f, cos, sin, fade);
    }

    /** Crescent in the blade plane, then rolled so the pair crosses. */
    private static void drawRolledCrescent(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float thickness, float trailLength, int segments, float cos, float sin, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;
            float x1 = t1 * span;
            float x2 = t2 * span;
            float z1 = (1.0f - t1 * t1) * chord;
            float z2 = (1.0f - t2 * t2) * chord;
            float th1 = thickness * (1.0f - Math.abs(t1) * 0.78f);
            float th2 = thickness * (1.0f - Math.abs(t2) * 0.78f);
            float wake1 = 1.0f - Math.abs(t1) * 0.65f;
            float wake2 = 1.0f - Math.abs(t2) * 0.65f;

            rolledVertex(matrix, consumer, x1, th1, z1, cos, sin, r, g, b, a);
            rolledVertex(matrix, consumer, x2, th2, z2, cos, sin, r, g, b, a);
            rolledVertex(matrix, consumer, x2, th2 * 0.25f, z2 - trailLength * wake2, cos, sin, r, g, b, 0.0f);
            rolledVertex(matrix, consumer, x1, th1 * 0.25f, z1 - trailLength * wake1, cos, sin, r, g, b, 0.0f);

            rolledVertex(matrix, consumer, x1, -th1 * 0.25f, z1 - trailLength * wake1, cos, sin, r, g, b, 0.0f);
            rolledVertex(matrix, consumer, x2, -th2 * 0.25f, z2 - trailLength * wake2, cos, sin, r, g, b, 0.0f);
            rolledVertex(matrix, consumer, x2, -th2, z2, cos, sin, r, g, b, a);
            rolledVertex(matrix, consumer, x1, -th1, z1, cos, sin, r, g, b, a);
        }
    }

    private static void drawRolledSpur(Matrix4f matrix, VertexConsumer consumer, float tipX, float spur, float cos, float sin, float fade) {
        float sign = tipX < 0.0f ? -1.0f : 1.0f;
        int steps = 5;
        for (int i = 0; i < steps; i++) {
            float t1 = i / (float) steps;
            float t2 = (i + 1) / (float) steps;
            float z1 = -spur * 1.9f * t1;
            float z2 = -spur * 1.9f * t2;
            float bow1 = (float) Math.sin(t1 * Math.PI) * spur * 0.45f;
            float bow2 = (float) Math.sin(t2 * Math.PI) * spur * 0.45f;
            float x1 = tipX + sign * (spur * 1.35f * t1) - sign * bow1;
            float x2 = tipX + sign * (spur * 1.35f * t2) - sign * bow2;
            float w1 = spur * 0.18f * (1.0f - t1);
            float w2 = spur * 0.18f * (1.0f - t2);
            float a1 = fade * (1.0f - t1);
            float a2 = fade * (1.0f - t2) * 0.12f;
            rolledVertex(matrix, consumer, x1, -w1, z1, cos, sin, 0.0f, 0.95f, 1.0f, a1);
            rolledVertex(matrix, consumer, x1, w1, z1, cos, sin, 0.0f, 0.95f, 1.0f, a1);
            rolledVertex(matrix, consumer, x2, w2, z2, cos, sin, 1.0f, 0.84f, 0.0f, a2);
            rolledVertex(matrix, consumer, x2, -w2, z2, cos, sin, 1.0f, 0.84f, 0.0f, a2);
            rolledVertex(matrix, consumer, x1, -w1 * 0.28f, z1, cos, sin, 1.0f, 1.0f, 1.0f, a1);
            rolledVertex(matrix, consumer, x1, w1 * 0.28f, z1, cos, sin, 1.0f, 1.0f, 1.0f, a1);
            rolledVertex(matrix, consumer, x2, w2 * 0.28f, z2, cos, sin, 1.0f, 1.0f, 1.0f, a2);
            rolledVertex(matrix, consumer, x2, -w2 * 0.28f, z2, cos, sin, 1.0f, 1.0f, 1.0f, a2);
        }
    }

    private static void rolledVertex(Matrix4f matrix, VertexConsumer consumer, float x, float y, float z, float cos, float sin, float r, float g, float b, float a) {
        float xr = x * cos - y * sin;
        float yr = x * sin + y * cos;
        consumer.addVertex(matrix, xr, yr, z).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }

    private static void drawGeometricStarburst(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float radius, int points, float rotDeg, float rCenter, float gCenter, float bCenter, float aCenter, float rEdge, float gEdge, float bEdge, float aEdge) {
        double rotRad = Math.toRadians(rotDeg);
        int totalSegments = points * 2;
        for (int i = 0; i < totalSegments; i++) {
            double a1 = ((i / (double) totalSegments) * Math.PI * 2.0) + rotRad;
            double a2 = (((i + 1) / (double) totalSegments) * Math.PI * 2.0) + rotRad;

            float r1 = (i % 2 == 0) ? radius : (radius * 0.35f);
            float r2 = ((i + 1) % 2 == 0) ? radius : (radius * 0.35f);

            float x1 = cx + (float) Math.cos(a1) * r1;
            float y1 = cy + (float) Math.sin(a1) * r1;
            float x2 = cx + (float) Math.cos(a2) * r2;
            float y2 = cy + (float) Math.sin(a2) * r2;

            // Front
            consumer.addVertex(matrix, cx, cy, cz).setColor(rCenter, gCenter, bCenter, aCenter).setUv(0.5f, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, y1, cz).setColor(rEdge, gEdge, bEdge, aEdge).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, y2, cz).setColor(rEdge, gEdge, bEdge, aEdge).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, cx, cy, cz).setColor(rCenter, gCenter, bCenter, aCenter).setUv(0.5f, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);

            // Back
            consumer.addVertex(matrix, cx, cy, cz).setColor(rCenter, gCenter, bCenter, aCenter).setUv(0.5f, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, x2, y2, cz).setColor(rEdge, gEdge, bEdge, aEdge).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, x1, y1, cz).setColor(rEdge, gEdge, bEdge, aEdge).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, cx, cy, cz).setColor(rCenter, gCenter, bCenter, aCenter).setUv(0.5f, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
        }
    }
}
