package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.ZChargeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Entity Renderer for Z-Sword Shockwave Charging in Minecraft 26.2.
 * Renders a physical 3D geometric golden vortex funnel, counter-rotating celestial energy rings,
 * levitating vibrating 3D sacred golden crystal prisms, and radiating divine ground mandala.
 */
public class ZChargeRenderer extends EntityRenderer<ZChargeEntity, ZChargeRenderer.ZChargeRenderState> {

    public ZChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ZChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
        public long seed = 0;
    }

    @Override
    public boolean shouldRender(ZChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public ZChargeRenderState createRenderState() {
        return new ZChargeRenderState();
    }

    @Override
    public void extractRenderState(ZChargeEntity entity, ZChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(ZChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float charge = state.chargeRatio;
        float age = state.age;

        float baseRadius = 1.2f + (charge * 2.2f);
        float baseAlpha = 0.70f + (charge * 0.30f);
        float tremble = (float) Math.sin(age * 3.5f) * (0.02f + charge * 0.08f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Radiating Divine Ground Seal Mandala & Sacred Pressure Spokes
            int groundSegments = 32;
            drawRotatingRing(matrix, buffer, 0, 0.04f, 0, baseRadius * 1.05f, baseRadius * 0.94f, groundSegments, age * 8.0f,
                1.0f, 0.85f, 0.15f, baseAlpha * 0.85f);
            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, baseRadius * 0.68f, baseRadius * 0.56f, groundSegments, age * -14.0f,
                1.0f, 0.95f, 0.40f, baseAlpha * 0.95f);

            // 8 Sacred Radial Pressure Lines
            for (int i = 0; i < 8; i++) {
                double spokeAng = (i / 8.0) * Math.PI * 2.0 + Math.toRadians(age * 8.0f);
                float sx1 = (float) Math.cos(spokeAng) * (baseRadius * 0.35f);
                float sz1 = (float) Math.sin(spokeAng) * (baseRadius * 0.35f);
                float sx2 = (float) Math.cos(spokeAng) * (baseRadius * 1.05f);
                float sz2 = (float) Math.sin(spokeAng) * (baseRadius * 1.05f);
                drawTrenchSegment(matrix, buffer, sx1, sz1, sx2, sz2, 0.12f, 1.0f, 0.80f, 0.10f, baseAlpha * 0.80f);
            }

            // 2. Physical 3D Geometric Golden Vortex Funnel (drawing into sword hilt)
            int funnelArms = 4;
            int funnelSteps = 16;
            float funnelHeight = 2.4f + charge * 1.4f;
            float coreY = 1.0f; // Sword position

            for (int a = 0; a < funnelArms; a++) {
                float armOffset = (a / (float) funnelArms) * (float) Math.PI * 2.0f;
                for (int s = 0; s < funnelSteps; s++) {
                    float p1 = s / (float) funnelSteps;
                    float p2 = (s + 1) / (float) funnelSteps;

                    // Spiraling downward into center
                    float y1 = coreY + (1.0f - p1) * funnelHeight;
                    float y2 = coreY + (1.0f - p2) * funnelHeight;

                    float rad1 = (0.2f + (1.0f - p1) * baseRadius) * (0.8f + 0.2f * (float) Math.sin(age * 0.3f + a));
                    float rad2 = (0.2f + (1.0f - p2) * baseRadius) * (0.8f + 0.2f * (float) Math.sin(age * 0.3f + a));

                    double ang1 = age * 0.22f + p1 * Math.PI * 4.0 + armOffset;
                    double ang2 = age * 0.22f + p2 * Math.PI * 4.0 + armOffset;

                    float x1 = (float) Math.cos(ang1) * rad1;
                    float z1 = (float) Math.sin(ang1) * rad1;
                    float x2 = (float) Math.cos(ang2) * rad2;
                    float z2 = (float) Math.sin(ang2) * rad2;

                    float width = 0.18f * (1.0f - p1 * 0.4f) * (0.6f + charge * 0.6f);
                    drawRibbonSegment(matrix, buffer, x1, y1, z1, x2, y2, z2, width,
                        1.0f, 0.85f, 0.20f, baseAlpha * (0.5f + p1 * 0.5f));
                    // High-luminance inner core ribbon
                    drawRibbonSegment(matrix, buffer, x1, y1, z1, x2, y2, z2, width * 0.4f,
                        1.0f, 1.0f, 0.80f, baseAlpha * 0.90f);
                }
            }

            // 3. Multi-Tier Counter-Rotating Concentric 3D Celestial Energy Rings
            int ringSegments = 24;
            // Ring 1 (Tilted outer golden ring)
            drawInclinedRing(matrix, buffer, 0, coreY + 0.3f, 0, baseRadius * 0.90f, 0.08f, ringSegments, age * 16.0f, 25.0f,
                1.0f, 0.75f, 0.05f, baseAlpha * 0.85f);
            // Ring 2 (Tilted opposing amber ring)
            drawInclinedRing(matrix, buffer, 0, coreY - 0.2f, 0, baseRadius * 0.65f, 0.07f, ringSegments, age * -22.0f, -30.0f,
                1.0f, 0.90f, 0.30f, baseAlpha * 0.90f);
            // Ring 3 (Tight horizontal white-gold compression ring)
            drawRotatingRing(matrix, buffer, 0, coreY, 0, baseRadius * 0.40f, baseRadius * 0.32f, ringSegments, age * 30.0f,
                1.0f, 1.0f, 0.90f, baseAlpha);

            // 4. Levitating & Vibrating 3D Sacred Golden Crystal Prisms (Octahedrons)
            int prismCount = 8 + (int) (charge * 6); // 8 to 14 prisms
            for (int r = 0; r < prismCount; r++) {
                double prismAng = (r / (double) prismCount) * Math.PI * 2.0 + (age * 0.08f);
                float prismDist = 0.5f + rng.nextFloat() * (baseRadius * 0.75f);

                float px = (float) Math.cos(prismAng) * prismDist + (rng.nextFloat() - 0.5f) * tremble;
                float pz = (float) Math.sin(prismAng) * prismDist + (rng.nextFloat() - 0.5f) * tremble;

                float floatPhase = age * 0.3f + r * 1.6f;
                float py = coreY + (float) Math.sin(floatPhase) * (0.35f + charge * 0.45f);

                float pSize = 0.15f + rng.nextFloat() * 0.15f + charge * 0.10f;
                float pHeight = 0.25f + rng.nextFloat() * 0.20f + charge * 0.15f;

                // 3D Octahedron Crystal Geometry (upper and lower pyramid facets)
                drawCrystalOctahedron(matrix, buffer, px, py, pz, pSize, pHeight,
                    1.0f, 0.90f, 0.30f, baseAlpha * 0.95f,
                    1.0f, 1.0f, 0.90f, baseAlpha
                );
            }

            // 5. Overdrive Solar Ray Flares (charge >= 0.4f)
            if (charge >= 0.4f) {
                int flareCount = 6 + (int) (charge * 6);
                for (int i = 0; i < flareCount; i++) {
                    double fAng = (i / (double) flareCount) * Math.PI * 2.0 + (age * 0.15f);
                    float fLen = (1.2f + charge * 1.8f) * (0.8f + 0.2f * (float) Math.sin(age * 0.6f + i));
                    float fx = (float) Math.cos(fAng) * fLen;
                    float fz = (float) Math.sin(fAng) * fLen;
                    float fy = coreY + (float) Math.sin(i * 2.1f) * 0.5f;

                    drawTrenchSegment(matrix, buffer, 0, 0, fx, fz, 0.08f + charge * 0.06f,
                        1.0f, 1.0f, 0.85f, baseAlpha * 0.90f);
                }
            }
        });

        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawTrenchSegment(Matrix4f matrix, VertexConsumer consumer, float x1, float z1, float x2, float z2, float width, float r, float g, float b, float a) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * width;
        float nz = dx / len * width;

        consumer.addVertex(matrix, x1 - nx, 0.04f, z1 - nz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx, 0.04f, z1 + nz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx, 0.04f, z2 + nz).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx, 0.04f, z2 - nz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawRotatingRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = rotRad + (i / (double) segments) * Math.PI * 2.0;
            double a2 = rotRad + ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1Out = cx + (float) Math.cos(a1) * rOuter;
            float z1Out = cz + (float) Math.sin(a1) * rOuter;
            float x2Out = cx + (float) Math.cos(a2) * rOuter;
            float z2Out = cz + (float) Math.sin(a2) * rOuter;

            float x1In = cx + (float) Math.cos(a1) * rInner;
            float z1In = cz + (float) Math.sin(a1) * rInner;
            float x2In = cx + (float) Math.cos(a2) * rInner;
            float z2In = cz + (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, x1Out, cy, z1Out).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy, z2Out).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2In, cy, z2In).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1In, cy, z1In).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawInclinedRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float radius, float thickness, int segments, float rotDeg, float tiltDeg, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        double tiltRad = Math.toRadians(tiltDeg);
        float cosTilt = (float) Math.cos(tiltRad);
        float sinTilt = (float) Math.sin(tiltRad);

        for (int i = 0; i < segments; i++) {
            double a1 = rotRad + (i / (double) segments) * Math.PI * 2.0;
            double a2 = rotRad + ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = (float) Math.cos(a1) * radius;
            float z1 = (float) Math.sin(a1) * radius;
            float y1 = z1 * sinTilt;
            z1 = z1 * cosTilt;

            float x2 = (float) Math.cos(a2) * radius;
            float z2 = (float) Math.sin(a2) * radius;
            float y2 = z2 * sinTilt;
            z2 = z2 * cosTilt;

            consumer.addVertex(matrix, cx + x1, cy + y1 - thickness, cz + z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx + x2, cy + y2 - thickness, cz + z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx + x2, cy + y2 + thickness, cz + z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx + x1, cy + y1 + thickness, cz + z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawRibbonSegment(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float width, float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1 - width * 0.5f, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1 + width * 0.5f, z1).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2 + width * 0.5f, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2 - width * 0.5f, z2).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawCrystalOctahedron(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float height,
                                              float rBase, float gBase, float bBase, float aBase,
                                              float rTip, float gTip, float bTip, float aTip) {
        float half = size * 0.5f;
        float halfH = height * 0.5f;

        // Top Pyramid (4 faces)
        drawTriangle(matrix, consumer, cx - half, cy, cz - half, cx + half, cy, cz - half, cx, cy + halfH, cz, rBase, gBase, bBase, aBase, rTip, gTip, bTip, aTip);
        drawTriangle(matrix, consumer, cx + half, cy, cz - half, cx + half, cy, cz + half, cx, cy + halfH, cz, rBase, gBase, bBase, aBase, rTip, gTip, bTip, aTip);
        drawTriangle(matrix, consumer, cx + half, cy, cz + half, cx - half, cy, cz + half, cx, cy + halfH, cz, rBase, gBase, bBase, aBase, rTip, gTip, bTip, aTip);
        drawTriangle(matrix, consumer, cx - half, cy, cz + half, cx - half, cy, cz - half, cx, cy + halfH, cz, rBase, gBase, bBase, aBase, rTip, gTip, bTip, aTip);

        // Bottom Pyramid (4 faces)
        drawTriangle(matrix, consumer, cx + half, cy, cz - half, cx - half, cy, cz - half, cx, cy - halfH, cz, rBase, gBase, bBase, aBase, rTip, gTip, bTip, aTip);
        drawTriangle(matrix, consumer, cx + half, cy, cz + half, cx + half, cy, cz - half, cx, cy - halfH, cz, rBase, gBase, bBase, aBase, rTip, gTip, bTip, aTip);
        drawTriangle(matrix, consumer, cx - half, cy, cz + half, cx + half, cy, cz + half, cx, cy - halfH, cz, rBase, gBase, bBase, aBase, rTip, gTip, bTip, aTip);
        drawTriangle(matrix, consumer, cx - half, cy, cz - half, cx - half, cy, cz + half, cx, cy - halfH, cz, rBase, gBase, bBase, aBase, rTip, gTip, bTip, aTip);
    }

    private static void drawTriangle(Matrix4f matrix, VertexConsumer consumer,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float tipX, float tipY, float tipZ,
                                     float rBase, float gBase, float bBase, float aBase,
                                     float rTip, float gTip, float bTip, float aTip) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
