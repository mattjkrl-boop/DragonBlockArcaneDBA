package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.TemporalRiftEntity;
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
        public double worldX = 0;
        public double worldY = 0;
        public double worldZ = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(TemporalRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(TemporalRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
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
        state.worldX = entity.getX();
        state.worldY = entity.getY();
        state.worldZ = entity.getZ();
    }

    @Override
    public void submit(RiftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float age = state.ageInTicks;
        float maxLife = (float) state.maxLifetime;

        // Shared lifecycle envelope (point 5): build over the first 15
        // percent, peak, dissipate over the last 35 percent. The same
        // envelope drives the entity's particles, drift, and damage, so the
        // dome's alpha, the motes, and the second burst half all agree.
        // Nothing pops in at full alpha and nothing discards at visible alpha.
        float env = FxEnvelope.envelope(age, maxLife, 0.15f, 0.35f);
        if (env <= 0.001f) return;

        // The radius unfolds during the build and holds through dissipation;
        // the old version also shrank the radius to zero over the last 10
        // ticks on top of the fade, which read as the dome collapsing early.
        float buildT = FxEnvelope.smoothstep(Math.min(1.0f, age / (maxLife * 0.15f)));
        float radiusScale = 0.35f + 0.65f * buildT;
        float radius = state.radius * radiusScale;
        float pulse = 0.88f + 0.12f * (float) Math.sin(age * 0.20f);
        float fade = env;

        // Point 1: geometry and alpha step down with camera distance.
        float ds = LayeredRender.distScale(cameraState, state.worldX, state.worldY, state.worldZ);
        float alphaScale = (float) Math.sqrt(ds);
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Volumetric 3D Translucent Celestial Dome Shell (Multi-Latitude Mesh)
            int domeLatitudes = FxEnvelope.segments(14, ds, 6);
            int domeLongitudes = FxEnvelope.segments(40, ds, 16);
            float domeHeight = radius * 1.05f;

            for (int lat = 0; lat < domeLatitudes; lat++) {
                float latProgress1 = lat / (float) domeLatitudes;
                float latProgress2 = (lat + 1) / (float) domeLatitudes;

                float y1 = 0.1f + (float) Math.sin(latProgress1 * (Math.PI / 2.0)) * domeHeight;
                float y2 = 0.1f + (float) Math.sin(latProgress2 * (Math.PI / 2.0)) * domeHeight;

                float r1 = radius * (float) Math.cos(latProgress1 * (Math.PI / 2.0));
                float r2 = radius * (float) Math.cos(latProgress2 * (Math.PI / 2.0));

                float domeAlpha = (0.13f + latProgress1 * 0.15f) * pulse * fade * alphaScale;

                // Color shift from celestial cyan at base to diamond silver at apex.
                // 3-stop ramp (T2): the base band now starts in a deeper cyan so
                // the dome has real depth against the silver apex.
                float cr = 0.38f + latProgress1 * 0.57f;
                float cg = 0.76f + latProgress1 * 0.22f;
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

            // Brighter inner shell of the same dome, so the clock has a hot core under the glass.
            int coreLats = FxEnvelope.segments(8, ds, 4);
            int coreLons = FxEnvelope.segments(28, ds, 12);
            float coreRadius = radius * 0.72f;
            float coreHeight = coreRadius * 1.05f;
            for (int lat = 0; lat < coreLats; lat++) {
                float latProgress1 = lat / (float) coreLats;
                float latProgress2 = (lat + 1) / (float) coreLats;
                float y1 = 0.12f + (float) Math.sin(latProgress1 * (Math.PI / 2.0)) * coreHeight;
                float y2 = 0.12f + (float) Math.sin(latProgress2 * (Math.PI / 2.0)) * coreHeight;
                float r1 = coreRadius * (float) Math.cos(latProgress1 * (Math.PI / 2.0));
                float r2 = coreRadius * (float) Math.cos(latProgress2 * (Math.PI / 2.0));
                float coreAlpha = (0.05f + latProgress1 * 0.16f) * pulse * fade * alphaScale;
                for (int lon = 0; lon < coreLons; lon++) {
                    double angleOffset = (-age * 0.6f) * (Math.PI / 180.0);
                    double a1 = ((lon / (double) coreLons) * Math.PI * 2.0) + angleOffset;
                    double a2 = (((lon + 1) / (double) coreLons) * Math.PI * 2.0) + angleOffset;
                    float x1 = (float) Math.cos(a1) * r1;
                    float z1 = (float) Math.sin(a1) * r1;
                    float x2 = (float) Math.cos(a2) * r1;
                    float z2 = (float) Math.sin(a2) * r1;
                    float x3 = (float) Math.cos(a2) * r2;
                    float z3 = (float) Math.sin(a2) * r2;
                    float x4 = (float) Math.cos(a1) * r2;
                    float z4 = (float) Math.sin(a1) * r2;
                    drawQuad(matrix, buffer, x1, y1, z1, x2, y1, z2, x3, y2, z3, x4, y2, z4,
                        0.85f, 0.96f, 1.0f, coreAlpha);
                }
            }

            // Layered interior (point 1): three counter-rotating time
            // current bands inside the dome shells, plus the shared ground
            // anchor glow, so the dome has moving depth instead of two
            // static glass shells. Both run on the same envelope.
            LayeredRender.drawVortexBands(pose, buffer, radius * 0.52f, domeHeight * 0.72f, age,
                0.55f, 0.85f, 1.0f, env * 0.85f, ds);
            LayeredRender.drawAnchorGlow(pose, buffer, radius * 0.98f, 0.05f,
                0.45f, 0.78f, 1.0f, 0.50f * env * pulse, ds);

            // 2. Physical 3D Rotating Clock Decal & Ground Chronometer Mandala
            int groundSegments = FxEnvelope.segments(48, ds, 20);

            // Outer Chrono Rim
            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, radius, radius * 0.91f, groundSegments, age * 6.0f,
                0.70f, 0.92f, 1.0f, 0.80f * pulse * fade);

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

                float lift = isCardinal ? 0.16f : 0.09f;
                drawLash(matrix, buffer,
                    hx * rStart, 0.06f, hz * rStart,
                    hx * (rStart + rEnd) * 0.5f, lift, hz * (rStart + rEnd) * 0.5f,
                    hx * rEnd, 0.05f, hz * rEnd,
                    markerWidth * 0.45f, 5,
                    isCardinal ? 0.75f : 0.45f, isCardinal ? 0.92f : 0.78f, 1.0f,
                    1.0f, 1.0f, 1.0f,
                    0.92f * pulse * fade);
            }

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
            drawOctahedralJewel(matrix, buffer, 0, 0.20f, 0, 0.55f * radiusScale, age * 25.0f,
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
                drawMeridianStream(matrix, buffer, streamAngle, radius, domeHeight, 16,
                    0.70f, 0.92f, 1.0f, 0.80f * pulse * fade);
            }

            // 6. Zenith Stasis Apex Jewel
            drawOctahedralJewel(matrix, buffer, 0, domeHeight + 0.1f, 0, 0.85f * radiusScale, age * 30.0f,
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

    private static void drawLash(Matrix4f matrix, VertexConsumer consumer,
                                 float x0, float y0, float z0,
                                 float xc, float yc, float zc,
                                 float x1, float y1, float z1,
                                 float width, int steps,
                                 float er, float eg, float eb,
                                 float cr, float cg, float cb,
                                 float alpha) {
        int n = Math.max(4, steps);
        float px = x0;
        float py = y0;
        float pz = z0;
        for (int i = 1; i <= n; i++) {
            float t = i / (float) n;
            float u = 1.0f - t;
            float x = u * u * x0 + 2.0f * u * t * xc + t * t * x1;
            float y = u * u * y0 + 2.0f * u * t * yc + t * t * y1;
            float z = u * u * z0 + 2.0f * u * t * zc + t * t * z1;
            float w = width * (1.0f - t * 0.9f);
            float a = alpha * (1.0f - t * t);
            lashSpan(matrix, consumer, px, py, pz, x, y, z, w, er, eg, eb, a);
            lashSpan(matrix, consumer, px, py, pz, x, y, z, w * 0.32f, cr, cg, cb, Math.min(1.0f, a * 1.2f));
            px = x;
            py = y;
            pz = z;
        }
    }

    private static void lashSpan(Matrix4f matrix, VertexConsumer consumer,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float width,
                                 float r, float g, float b, float a) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float ax = 0.0f;
        float ay = 1.0f;
        float az = 0.0f;
        if (dx * dx + dz * dz < dy * dy) {
            ax = 1.0f;
            ay = 0.0f;
            az = 0.0f;
        }
        float px = dy * az - dz * ay;
        float py = dz * ax - dx * az;
        float pz = dx * ay - dy * ax;
        float plen = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (plen < 0.0001f) {
            return;
        }
        float s = width / plen;
        px *= s;
        py *= s;
        pz *= s;
        consumer.addVertex(matrix, x0 - px, y0 - py, z0 - pz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x0 + px, y0 + py, z0 + pz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 + px, y1 + py, z1 + pz).setColor(r, g, b, a * 0.65f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 - px, y1 - py, z1 - pz).setColor(r, g, b, a * 0.65f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 - px, y1 - py, z1 - pz).setColor(r, g, b, a * 0.65f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x1 + px, y1 + py, z1 + pz).setColor(r, g, b, a * 0.65f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x0 + px, y0 + py, z0 + pz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x0 - px, y0 - py, z0 - pz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
    }

    private static void drawClockHand(Matrix4f matrix, VertexConsumer consumer, float angleRad, float length, float baseWidth, float yLevel, float r, float g, float b, float a) {
        float cosA = (float) Math.cos(angleRad);
        float sinA = (float) Math.sin(angleRad);
        float tail = baseWidth * 1.15f;
        drawLash(matrix, consumer,
            -cosA * tail, yLevel, -sinA * tail,
            cosA * length * 0.42f - sinA * baseWidth * 1.4f, yLevel + 0.08f, sinA * length * 0.42f + cosA * baseWidth * 1.4f,
            cosA * length, yLevel + 0.02f, sinA * length,
            baseWidth * 0.55f, 6,
            r * 0.65f, g * 0.75f, b,
            1.0f, 1.0f, 1.0f,
            a
        );
    }

    private static void drawTiltedOrbitalRing(Matrix4f matrix, VertexConsumer consumer, float radius, float tiltDeg, float rotDeg, float r, float g, float b, float a) {
        int segments = 40;
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
        float sinA = (float) Math.sin(angle);
        float cosA = (float) Math.cos(angle);
        float sideX = -sinA;
        float sideZ = cosA;

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

            float w1 = 0.07f * (1.0f - p1 * 0.75f);
            float w2 = 0.07f * (1.0f - p2 * 0.75f);
            float a1 = a * (1.0f - p1 * 0.45f);
            float a2 = a * (1.0f - p2 * 0.45f);

            consumer.addVertex(matrix, x1 - sideX * w1, y1, z1 - sideZ * w1).setColor(r, g, b, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(sideX, 0, sideZ);
            consumer.addVertex(matrix, x1 + sideX * w1, y1, z1 + sideZ * w1).setColor(r, g, b, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(sideX, 0, sideZ);
            consumer.addVertex(matrix, x2 + sideX * w2, y2, z2 + sideZ * w2).setColor(r, g, b, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(sideX, 0, sideZ);
            consumer.addVertex(matrix, x2 - sideX * w2, y2, z2 - sideZ * w2).setColor(r, g, b, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(sideX, 0, sideZ);

            float c1 = w1 * 0.32f;
            float c2 = w2 * 0.32f;
            consumer.addVertex(matrix, x1 - sideX * c1, y1 + 0.02f, z1 - sideZ * c1).setColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, a1 * 1.15f)).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(sideX, 0, sideZ);
            consumer.addVertex(matrix, x1 + sideX * c1, y1 + 0.02f, z1 + sideZ * c1).setColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, a1 * 1.15f)).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(sideX, 0, sideZ);
            consumer.addVertex(matrix, x2 + sideX * c2, y2 + 0.02f, z2 + sideZ * c2).setColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, a2 * 1.15f)).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(sideX, 0, sideZ);
            consumer.addVertex(matrix, x2 - sideX * c2, y2 + 0.02f, z2 - sideZ * c2).setColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, a2 * 1.15f)).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(sideX, 0, sideZ);
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
}
