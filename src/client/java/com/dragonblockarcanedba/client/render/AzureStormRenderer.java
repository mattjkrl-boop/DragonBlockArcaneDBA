package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.AzureStormEntity;
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

import java.util.Random;

/**
 * Entity Renderer for Azure Storm Domain in Minecraft 26.2.
 * Renders a massive, cinematic 3D tempest weather domain:
 * - Double-layered rotating volumetric cyclone cloud dome with turbulence harmonics
 * - Physical 3D downburst columns and geometric rain shafts
 * - Quad-tier counter-rotating ground whirlpools and surging wave rings
 * - Volumetric branching atmospheric lightning discharges across the dome
 * - Central cyclonic tempest eye and radiant updraft column
 */
public class AzureStormRenderer extends EntityRenderer<AzureStormEntity, AzureStormRenderer.AzureStormRenderState> {

    public AzureStormRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AzureStormRenderState extends EntityRenderState {
        public float radius = 12.0f;
        public float age = 0;
        public long seed = 0;
    }

    @Override
    public boolean shouldRender(AzureStormEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public AzureStormRenderState createRenderState() {
        return new AzureStormRenderState();
    }

    @Override
    public void extractRenderState(AzureStormEntity entity, AzureStormRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(AzureStormRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        float age = state.age;
        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Quad-Tier Ground Whirlpool & Wave Surges (4 counter-rotating wind bands)
            int groundSegments = 32;
            for (int band = 0; band < 4; band++) {
                float rOuter = radius * (0.30f + band * 0.23f);
                float rInner = rOuter * 0.84f;
                float rotSpeed = (band % 2 == 0 ? 1.0f : -1.35f) * (14.0f + band * 7.0f);
                float bandAlpha = 0.58f - (band * 0.09f);

                float red = band == 0 ? 0.4f : 0.0f;
                float green = 0.75f + (band * 0.08f);
                float blue = 1.0f;

                drawRotatingRing(matrix, buffer, 0, 0.08f + (band * 0.06f), 0, rOuter, rInner, groundSegments, age * rotSpeed, red, green, blue, bandAlpha);
            }

            // 2. Volumetric Double-Layered Cyclone Cloud Dome
            float domeHeight = Math.min(22.0f, radius * 1.35f);

            // Layer A: Outer Massive Rotating Cyclone Dome
            int domeLatitudes = 10;
            int domeLongitudes = 24;
            for (int lat = 0; lat < domeLatitudes; lat++) {
                float latProgress1 = lat / (float) domeLatitudes;
                float latProgress2 = (lat + 1) / (float) domeLatitudes;

                float y1 = 2.0f + (float) Math.sin(latProgress1 * (Math.PI / 2.0)) * domeHeight;
                float y2 = 2.0f + (float) Math.sin(latProgress2 * (Math.PI / 2.0)) * domeHeight;

                float r1 = radius * (float) Math.cos(latProgress1 * (Math.PI / 2.0));
                float r2 = radius * (float) Math.cos(latProgress2 * (Math.PI / 2.0));

                float domeAlpha = 0.38f + (latProgress1 * 0.30f);
                float red = 0.02f;
                float green = 0.22f + (latProgress1 * 0.55f);
                float blue = 0.55f + (latProgress1 * 0.45f);

                for (int lon = 0; lon < domeLongitudes; lon++) {
                    double angleOffset = (age * (5.0f + lat * 2.0f)) * (Math.PI / 180.0);
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

                    drawQuad(matrix, buffer, x1, y1, z1, x2, y1, z2, x3, y2, z3, x4, y2, z4, red, green, blue, domeAlpha);
                }
            }

            // Layer B: Inner Turbulent Stratus Cyclone Ceiling (Counter-Rotating)
            int innerLats = 6;
            int innerLons = 18;
            float innerDomeHeight = domeHeight * 0.70f;
            float innerRadius = radius * 0.85f;

            for (int lat = 0; lat < innerLats; lat++) {
                float p1 = lat / (float) innerLats;
                float p2 = (lat + 1) / (float) innerLats;

                float iy1 = 3.0f + (float) Math.sin(p1 * (Math.PI / 2.0)) * innerDomeHeight;
                float iy2 = 3.0f + (float) Math.sin(p2 * (Math.PI / 2.0)) * innerDomeHeight;

                float ir1 = innerRadius * (float) Math.cos(p1 * (Math.PI / 2.0));
                float ir2 = innerRadius * (float) Math.cos(p2 * (Math.PI / 2.0));

                float alpha = 0.28f + (p1 * 0.20f);

                for (int lon = 0; lon < innerLons; lon++) {
                    double angleOffset = (-age * (8.0f + lat * 3.0f)) * (Math.PI / 180.0);
                    double a1 = ((lon / (double) innerLons) * Math.PI * 2.0) + angleOffset;
                    double a2 = (((lon + 1) / (double) innerLons) * Math.PI * 2.0) + angleOffset;

                    float x1 = (float) Math.cos(a1) * ir1;
                    float z1 = (float) Math.sin(a1) * ir1;
                    float x2 = (float) Math.cos(a2) * ir1;
                    float z2 = (float) Math.sin(a2) * ir1;

                    float x3 = (float) Math.cos(a2) * ir2;
                    float z3 = (float) Math.sin(a2) * ir2;
                    float x4 = (float) Math.cos(a1) * ir2;
                    float z4 = (float) Math.sin(a1) * ir2;

                    drawQuad(matrix, buffer, x1, iy1, z1, x2, iy1, z2, x3, iy2, z3, x4, iy2, z4, 0.0f, 0.85f, 1.0f, alpha);
                }
            }

            // 3. Physical Geometric Rain Shafts & Downburst Streamlines
            Random rainRng = new Random(state.seed + 777);
            int shaftCount = 14;
            for (int i = 0; i < shaftCount; i++) {
                double shaftAngle = rainRng.nextDouble() * Math.PI * 2.0 + (age * 0.05);
                float shaftR = radius * (0.25f + rainRng.nextFloat() * 0.65f);

                float sx = (float) Math.cos(shaftAngle) * shaftR;
                float sz = (float) Math.sin(shaftAngle) * shaftR;

                float topY = 4.0f + rainRng.nextFloat() * (domeHeight * 0.7f);
                float botY = 0.1f;

                // Animate downward falling streaks
                float animOffset = (age * 0.8f + i * 1.5f) % 4.0f;
                float streakTop = Math.max(botY, topY - animOffset * 3.0f);
                float streakBot = Math.max(botY, streakTop - 3.5f);

                renderBeam(matrix, buffer, sx, streakTop, sz, sx + 0.2f, streakBot, sz + 0.2f, 0.08f, 0.5f, 0.9f, 1.0f, 0.60f);
            }

            // 4. Central Tempest Eye Updraft Column (Intense central cyclone core)
            int eyeLevels = 6;
            float eyeRadius = radius * 0.18f;
            for (int lvl = 0; lvl < eyeLevels; lvl++) {
                float ey1 = (lvl / (float) eyeLevels) * domeHeight;
                float ey2 = ((lvl + 1) / (float) eyeLevels) * domeHeight;
                float er1 = eyeRadius * (0.6f + 0.4f * (lvl / (float) eyeLevels));
                float er2 = eyeRadius * (0.6f + 0.4f * ((lvl + 1) / (float) eyeLevels));

                float rotEye = age * (30.0f + lvl * 5.0f) * (float) (Math.PI / 180.0);
                for (int i = 0; i < 12; i++) {
                    double a1 = ((i / 12.0) * Math.PI * 2.0) + rotEye;
                    double a2 = (((i + 1) / 12.0) * Math.PI * 2.0) + rotEye;

                    float x1 = (float) Math.cos(a1) * er1;
                    float z1 = (float) Math.sin(a1) * er1;
                    float x2 = (float) Math.cos(a2) * er1;
                    float z2 = (float) Math.sin(a2) * er1;

                    float x3 = (float) Math.cos(a2) * er2;
                    float z3 = (float) Math.sin(a2) * er2;
                    float x4 = (float) Math.cos(a1) * er2;
                    float z4 = (float) Math.sin(a1) * er2;

                    drawQuad(matrix, buffer, x1, ey1, z1, x2, ey1, z2, x3, ey2, z3, x4, ey2, z4, 0.8f, 1.0f, 1.0f, 0.45f);
                }
            }

            // 5. Volumetric Atmospheric Lightning Discharges within Storm Boundary
            Random rng = new Random(state.seed + ((long) (age / 3.0f) * 1000));
            int arcCount = 4;
            for (int a = 0; a < arcCount; a++) {
                double startAngle = rng.nextDouble() * Math.PI * 2.0;
                float arcR = radius * (0.25f + rng.nextFloat() * 0.65f);
                float sx = (float) Math.cos(startAngle) * arcR;
                float sz = (float) Math.sin(startAngle) * arcR;
                float sy = 7.0f + rng.nextFloat() * 9.0f;

                float curX = sx, curY = sy, curZ = sz;
                int arcSteps = 6;
                for (int s = 0; s < arcSteps; s++) {
                    float nxtX = curX + (rng.nextFloat() - 0.5f) * 3.5f;
                    float nxtY = curY - (1.1f + rng.nextFloat() * 1.8f);
                    float nxtZ = curZ + (rng.nextFloat() - 0.5f) * 3.5f;

                    renderBeam(matrix, buffer, curX, curY, curZ, nxtX, nxtY, nxtZ, 0.20f, 0.0f, 0.95f, 1.0f, 0.90f);
                    renderBeam(matrix, buffer, curX, curY, curZ, nxtX, nxtY, nxtZ, 0.07f, 0.95f, 1.0f, 1.0f, 1.0f);

                    curX = nxtX; curY = nxtY; curZ = nxtZ;
                }
            }
        });
    }

    private static void drawRotatingRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        double rotRad = rotDeg * (Math.PI / 180.0);
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2.0) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2.0) + rotRad;

            float x1 = cx + (float) Math.cos(a1) * rOuter;
            float z1 = cz + (float) Math.sin(a1) * rOuter;
            float x2 = cx + (float) Math.cos(a2) * rOuter;
            float z2 = cz + (float) Math.sin(a2) * rOuter;

            float ix1 = cx + (float) Math.cos(a1) * rInner;
            float iz1 = cz + (float) Math.sin(a1) * rInner;
            float ix2 = cx + (float) Math.cos(a2) * rInner;
            float iz2 = cz + (float) Math.sin(a2) * rInner;

            drawQuad(matrix, consumer, ix1, cy, iz1, ix2, cy, iz2, x2, cy, z2, x1, cy, z1, r, g, b, a);
        }
    }

    private static void renderBeam(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float radius, float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * radius;
        float nz = dx / len * radius;
        float ny = radius;

        drawQuad(matrix, consumer, x1 - nx, y1, z1 - nz, x1 + nx, y1, z1 + nz, x2 + nx, y2, z2 + nz, x2 - nx, y2, z2 - nz, r, g, b, a);
        drawQuad(matrix, consumer, x1, y1 - ny, z1, x1, y1 + ny, z1, x2, y2 + ny, z2, x2, y2 - ny, z2, r, g, b, a);
    }

    private static void drawQuad(Matrix4f matrix, VertexConsumer consumer,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Reverse for backface
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
