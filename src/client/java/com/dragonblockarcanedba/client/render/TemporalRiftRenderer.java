package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.TemporalRiftEntity;
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
 * Entity Renderer for Temporal Rift in Minecraft 26.2.
 * Renders a monumental, high-performance translucent 3D celestial dome and rotating 3D ground astrolabe:
 * - Volumetric 3D Translucent Celestial Dome Shell (12-block radius) with dynamic breathing alpha
 * - Physical 3D Rotating Clock Decal & Multi-Tier Astrolabe with 12 hour steles and reversing 3D clock hands
 * - Dual tilted gyroscopic orbital precession rings
 * - 12 vertical meridian time streams connecting ground hour steles to the zenith apex
 * - Zenith stasis apex octahedron jewel
 */
public class TemporalRiftRenderer extends EntityRenderer<TemporalRiftEntity, TemporalRiftRenderer.RiftRenderState> {

    public TemporalRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class RiftRenderState extends EntityRenderState {
        public float radius = 12.0f;
        public float ageInTicks = 0;
        public int maxLifetime = 60;
    }

    @Override
    public boolean shouldRender(TemporalRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public RiftRenderState createRenderState() {
        return new RiftRenderState();
    }

    @Override
    public void extractRenderState(TemporalRiftEntity entity, RiftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
    }

    @Override
    public void submit(RiftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float age = state.ageInTicks;
        float maxLife = (float) state.maxLifetime;
        float progress = Math.min(1.0f, age / maxLife);
        if (progress >= 1.0f) return;

        // Smooth deployment expansion in first 6 ticks, smooth collapse in last 10 ticks
        float entrance = Math.min(1.0f, age / 6.0f);
        float exit = Math.min(1.0f, (maxLife - age) / 10.0f);
        float scaleMultiplier = (float) Math.sin(entrance * (Math.PI / 2.0)) * Math.max(0.0f, exit);
        if (scaleMultiplier <= 0.001f) return;

        float radius = state.radius * scaleMultiplier;
        float pulse = 0.88f + 0.12f * (float) Math.sin(age * 0.20f);
        float fade = Math.min(1.0f, exit);
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Volumetric 3D Translucent Celestial Dome Shell (Multi-Latitude Mesh)
            int domeLatitudes = 11;
            int domeLongitudes = 28;
            float domeHeight = radius * 1.05f;

            for (int lat = 0; lat < domeLatitudes; lat++) {
                float latProgress1 = lat / (float) domeLatitudes;
                float latProgress2 = (lat + 1) / (float) domeLatitudes;

                float y1 = 0.1f + (float) Math.sin(latProgress1 * (Math.PI / 2.0)) * domeHeight;
                float y2 = 0.1f + (float) Math.sin(latProgress2 * (Math.PI / 2.0)) * domeHeight;

                float r1 = radius * (float) Math.cos(latProgress1 * (Math.PI / 2.0));
                float r2 = radius * (float) Math.cos(latProgress2 * (Math.PI / 2.0));

                float domeAlpha = (0.13f + latProgress1 * 0.15f) * pulse * fade;

                // Color shift from celestial cyan at base to diamond silver at apex
                float cr = 0.55f + latProgress1 * 0.40f;
                float cg = 0.85f + latProgress1 * 0.13f;
                float cb = 1.0f;

                for (int lon = 0; lon < domeLongitudes; lon++) {
                    double angleOffset = (age * (1.0f + lat * 0.25f)) * (Math.PI / 180.0);
                    double a1 = ((lon / (double) domeLongitudes) * Math.PI * 2.0) + angleOffset;
                    double a2 = (((lon + 1) / (double) domeLongitudes) * Math.PI * 2.0) + angleOffset;

                    float x1 = (float) Math.cos(a1) * r1;
                    float z1 = (float) Math.sin(a1) * r1;
                    float x2 = (float) Math.cos(a2) * r1;
                    float z2 = (float) Math.sin(a2) * r1;

                    float x3 = (float) Math.cos(a2) * r2;
                    float z3 = (float) Math.sin(a2) * r2;
                    float x4 = (float) Math.cos(a1) * r2;
                    float z4 = (float) Math.sin(a1) * r2;

                    drawQuad(matrix, buffer, x1, y1, z1, x2, y1, z2, x3, y2, z3, x4, y2, z4,
                        cr, cg, cb, domeAlpha);
                }
            }

            // 2. Physical 3D Rotating Clock Decal & Ground Chronometer Mandala
            int groundSegments = 36;

            // Outer Chrono Rim
            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, radius, radius * 0.91f, groundSegments, age * 6.0f,
                0.70f, 0.92f, 1.0f, 0.80f * pulse * fade);

            // Minute Track Ring
            drawRotatingRing(matrix, buffer, 0, 0.07f, 0, radius * 0.88f, radius * 0.82f, groundSegments, -age * 8.0f,
                0.90f, 0.96f, 1.0f, 0.70f * pulse * fade);

            // 12 Physical 3D Hour Marker Steles (Placed every 30 degrees)
            for (int h = 0; h < 12; h++) {
                double hourAngle = (h / 12.0) * Math.PI * 2.0 + Math.toRadians(age * 6.0f);
                float hx = (float) Math.cos(hourAngle);
                float hz = (float) Math.sin(hourAngle);

                float rStart = radius * 0.82f;
                float rEnd = radius * 0.98f;

                // Major markers (12, 3, 6, 9) are larger and brighter
                boolean isCardinal = (h % 3 == 0);
                float markerWidth = isCardinal ? 0.28f : 0.16f;

                drawTrenchSegment(matrix, buffer,
                    hx * rStart, hz * rStart,
                    hx * rEnd, hz * rEnd,
                    markerWidth,
                    isCardinal ? 1.0f : 0.75f, isCardinal ? 1.0f : 0.92f, 1.0f, 0.90f * pulse * fade);
            }

            // Outer Rotating Runic Astrolabe Ring
            drawRotatingRing(matrix, buffer, 0, 0.09f, 0, radius * 0.62f, radius * 0.50f, groundSegments, age * 12.0f,
                0.60f, 0.88f, 1.0f, 0.75f * pulse * fade);

            // Inner Counter-Rotating Glyph Ring
            drawRotatingRing(matrix, buffer, 0, 0.11f, 0, radius * 0.40f, radius * 0.28f, groundSegments, -age * 16.0f,
                0.85f, 0.75f, 1.0f, 0.80f * pulse * fade);

            // Central 12-Point Starburst Chronometer Seal
            drawRotatingRing(matrix, buffer, 0, 0.13f, 0, radius * 0.22f, 0.0f, 12, age * 20.0f,
                0.98f, 0.98f, 1.0f, 0.85f * pulse * fade);

            // 3. Physical 3D Clock Hands (Reversing in Real-Time)
            // Hour Hand: sweeping backwards at -age * 15 deg
            float hourHandAngle = (float) Math.toRadians(-age * 15.0f);
            drawClockHand(matrix, buffer, hourHandAngle, radius * 0.42f, 0.38f, 0.14f,
                0.95f, 0.98f, 1.0f, 0.95f * pulse * fade);

            // Minute Hand: sweeping backwards at -age * 45 deg
            float minuteHandAngle = (float) Math.toRadians(-age * 45.0f);
            drawClockHand(matrix, buffer, minuteHandAngle, radius * 0.72f, 0.24f, 0.15f,
                0.60f, 0.90f, 1.0f, 0.95f * pulse * fade);

            // Center Jewel Cap
            drawOctahedralJewel(matrix, buffer, 0, 0.20f, 0, 0.55f * scaleMultiplier, age * 25.0f,
                1.0f, 1.0f, 1.0f, 0.95f * fade);

            // 4. Dual Tilted Orbital Precession Chrono-Rings
            // Orbital Ring A: Tilted +42 degrees
            drawTiltedOrbitalRing(matrix, buffer, radius * 0.98f, 42.0f, age * 16.0f,
                0.55f, 0.90f, 1.0f, 0.75f * pulse * fade);

            // Orbital Ring B: Tilted -42 degrees
            drawTiltedOrbitalRing(matrix, buffer, radius * 0.98f, -42.0f, -age * 16.0f,
                0.85f, 0.70f, 1.0f, 0.75f * pulse * fade);

            // 5. 12 Vertical Meridian Time Streams Rising to Apex
            for (int stream = 0; stream < 12; stream++) {
                double streamAngle = (stream / 12.0) * Math.PI * 2.0 + Math.toRadians(age * 6.0f);
                drawMeridianStream(matrix, buffer, streamAngle, radius, domeHeight, 12,
                    0.70f, 0.92f, 1.0f, 0.80f * pulse * fade);
            }

            // 6. Zenith Stasis Apex Jewel
            drawOctahedralJewel(matrix, buffer, 0, domeHeight + 0.1f, 0, 0.85f * scaleMultiplier, age * 30.0f,
                0.95f, 0.98f, 1.0f, 0.95f * fade);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawQuad(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Reverse side for interior dome visibility
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }

    private static void drawRotatingRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        float rotRad = (float) Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2) + rotRad;

            float x1 = cx + (float) Math.cos(a1) * rOuter;
            float z1 = cz + (float) Math.sin(a1) * rOuter;
            float x2 = cx + (float) Math.cos(a2) * rOuter;
            float z2 = cz + (float) Math.sin(a2) * rOuter;

            float ix1 = cx + (float) Math.cos(a1) * rInner;
            float iz1 = cz + (float) Math.sin(a1) * rInner;
            float ix2 = cx + (float) Math.cos(a2) * rInner;
            float iz2 = cz + (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, ix1, cy, iz1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ix2, cy, iz2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, cy, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, cy, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawClockHand(Matrix4f matrix, VertexConsumer consumer, float angleRad, float length, float baseWidth, float yLevel, float r, float g, float b, float a) {
        float cosA = (float) Math.cos(angleRad);
        float sinA = (float) Math.sin(angleRad);

        float tipX = cosA * length;
        float tipZ = sinA * length;

        float normX = -sinA * baseWidth * 0.5f;
        float normZ = cosA * baseWidth * 0.5f;

        // Tail opposite side
        float tailX = -cosA * (baseWidth * 1.2f);
        float tailZ = -sinA * (baseWidth * 1.2f);

        consumer.addVertex(matrix, tailX - normX, yLevel, tailZ - normZ).setColor(r, g, b, a * 0.8f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tailX + normX, yLevel, tailZ + normZ).setColor(r, g, b, a * 0.8f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, yLevel, tipZ).setColor(1.0f, 1.0f, 1.0f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, yLevel, tipZ).setColor(1.0f, 1.0f, 1.0f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawTiltedOrbitalRing(Matrix4f matrix, VertexConsumer consumer, float radius, float tiltDeg, float rotDeg, float r, float g, float b, float a) {
        int segments = 28;
        float width = 0.16f;
        double tiltRad = Math.toRadians(tiltDeg);
        double rotRad = Math.toRadians(rotDeg);

        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2) + rotRad;

            float x1_flat = (float) Math.cos(a1) * radius;
            float z1_flat = (float) Math.sin(a1) * radius;
            float x2_flat = (float) Math.cos(a2) * radius;
            float z2_flat = (float) Math.sin(a2) * radius;

            float x1 = x1_flat;
            float y1 = 1.0f + (float) (-z1_flat * Math.sin(tiltRad));
            float z1 = (float) (z1_flat * Math.cos(tiltRad));

            float x2 = x2_flat;
            float y2 = 1.0f + (float) (-z2_flat * Math.sin(tiltRad));
            float z2 = (float) (z2_flat * Math.cos(tiltRad));

            consumer.addVertex(matrix, x1, y1 + width, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2 + width, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2 - width, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, y1 - width, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawMeridianStream(Matrix4f matrix, VertexConsumer consumer, double angle, float radius, float height, int steps, float r, float g, float b, float a) {
        float ribWidth = 0.14f;
        float sinA = (float) Math.sin(angle);
        float cosA = (float) Math.cos(angle);

        float normX = -sinA * ribWidth * 0.5f;
        float normZ = cosA * ribWidth * 0.5f;

        for (int i = 0; i < steps; i++) {
            float p1 = i / (float) steps;
            float p2 = (i + 1) / (float) steps;

            float y1 = 0.1f + (float) Math.sin(p1 * (Math.PI / 2.0)) * height;
            float y2 = 0.1f + (float) Math.sin(p2 * (Math.PI / 2.0)) * height;

            float r1 = radius * (float) Math.cos(p1 * (Math.PI / 2.0));
            float r2 = radius * (float) Math.cos(p2 * (Math.PI / 2.0));

            float x1 = cosA * r1;
            float z1 = sinA * r1;
            float x2 = cosA * r2;
            float z2 = sinA * r2;

            consumer.addVertex(matrix, x1 - normX, y1, z1 - normZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1 + normX, y1, z1 + normZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2 + normX, y2, z2 + normZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2 - normX, y2, z2 - normZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawOctahedralJewel(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float rotDeg, float r, float g, float b, float a) {
        float rotRad = (float) Math.toRadians(rotDeg);
        float h = size * 1.2f;
        float w = size * 0.7f;

        for (int i = 0; i < 4; i++) {
            double a1 = (i / 4.0) * Math.PI * 2.0 + rotRad;
            double a2 = ((i + 1) / 4.0) * Math.PI * 2.0 + rotRad;

            float x1 = cx + (float) Math.cos(a1) * w;
            float z1 = cz + (float) Math.sin(a1) * w;
            float x2 = cx + (float) Math.cos(a2) * w;
            float z2 = cz + (float) Math.sin(a2) * w;

            // Top pyramid
            consumer.addVertex(matrix, x1, cy, z1).setColor(0.6f, 0.9f, 1.0f, a * 0.8f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, cy, z2).setColor(0.6f, 0.9f, 1.0f, a * 0.8f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom pyramid
            consumer.addVertex(matrix, x2, cy, z2).setColor(0.6f, 0.9f, 1.0f, a * 0.8f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, cy, z1).setColor(0.6f, 0.9f, 1.0f, a * 0.8f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }

    private static void drawTrenchSegment(Matrix4f matrix, VertexConsumer consumer, float x1, float z1, float x2, float z2, float width, float r, float g, float b, float a) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * width * 0.5f;
        float nz = dx / len * width * 0.5f;

        consumer.addVertex(matrix, x1 - nx, 0.08f, z1 - nz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx, 0.08f, z1 + nz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx, 0.08f, z2 + nz).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx, 0.08f, z2 - nz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
