package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.AbyssalDomainEntity;
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
 * Entity Renderer for Abyssal Eclipse Void Domain in Minecraft 26.2.
 * Completely replaces vanilla smoke/dust particle spam with a monumental 3D supernatural environment:
 * - Double-layered volumetric 3D void cloud dome
 * - Quad-tier counter-rotating corrupted ground whirlpools
 * - 16 Orbiting 3D jagged obsidian void spires
 * - Central vertical cyclonic vortex updraft column
 * - Atmospheric 3D void lightning discharges across the dome interior
 */
public class AbyssalDomainRenderer extends EntityRenderer<AbyssalDomainEntity, AbyssalDomainRenderer.AbyssalDomainRenderState> {

    public AbyssalDomainRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AbyssalDomainRenderState extends EntityRenderState {
        public float radius = 10.0f;
        public float age = 0;
        public long seed = 0;
    }

    @Override
    public boolean shouldRender(AbyssalDomainEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public AbyssalDomainRenderState createRenderState() {
        return new AbyssalDomainRenderState();
    }

    @Override
    public void extractRenderState(AbyssalDomainEntity entity, AbyssalDomainRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(AbyssalDomainRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        float age = state.age;
        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Quad-Tier Counter-Rotating Ground Whirlpools (4 dark corrupted energy bands)
            int groundSegments = 32;
            for (int band = 0; band < 4; band++) {
                float rOuter = radius * (0.30f + band * 0.23f);
                float rInner = rOuter * 0.83f;
                float rotSpeed = (band % 2 == 0 ? 1.0f : -1.3f) * (12.0f + band * 6.0f);
                float bandAlpha = 0.65f - (band * 0.08f);

                float red = band % 2 == 0 ? 0.95f : 0.45f;
                float green = 0.02f;
                float blue = band % 2 == 0 ? 0.20f : 0.95f;

                drawRotatingRing(matrix, buffer, 0, 0.08f + (band * 0.05f), 0, rOuter, rInner, groundSegments, age * rotSpeed, red, green, blue, bandAlpha);
            }

            // 2. Volumetric Double-Layered Void Cyclone Dome
            float domeHeight = Math.min(24.0f, radius * 1.30f);

            // Layer A: Outer Massive Rotating Void Cloud Dome
            int domeLatitudes = 10;
            int domeLongitudes = 24;
            for (int lat = 0; lat < domeLatitudes; lat++) {
                float latProgress1 = lat / (float) domeLatitudes;
                float latProgress2 = (lat + 1) / (float) domeLatitudes;

                float y1 = 1.0f + (float) Math.sin(latProgress1 * (Math.PI / 2.0)) * domeHeight;
                float y2 = 1.0f + (float) Math.sin(latProgress2 * (Math.PI / 2.0)) * domeHeight;

                float r1 = radius * (float) Math.cos(latProgress1 * (Math.PI / 2.0));
                float r2 = radius * (float) Math.cos(latProgress2 * (Math.PI / 2.0));

                float domeAlpha = 0.35f + (latProgress1 * 0.35f);
                float red = 0.15f + (latProgress1 * 0.65f);
                float green = 0.01f;
                float blue = 0.35f + (latProgress1 * 0.55f);

                for (int lon = 0; lon < domeLongitudes; lon++) {
                    double angleOffset = (age * (4.0f + lat * 1.5f)) * (Math.PI / 180.0);
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

            // Layer B: Inner Turbulent Corrupted Stratus Ceiling (Counter-Rotating)
            int innerLats = 6;
            int innerLons = 18;
            float innerDomeHeight = domeHeight * 0.75f;
            float innerRadius = radius * 0.85f;

            for (int lat = 0; lat < innerLats; lat++) {
                float p1 = lat / (float) innerLats;
                float p2 = (lat + 1) / (float) innerLats;

                float iy1 = 2.0f + (float) Math.sin(p1 * (Math.PI / 2.0)) * innerDomeHeight;
                float iy2 = 2.0f + (float) Math.sin(p2 * (Math.PI / 2.0)) * innerDomeHeight;

                float ir1 = innerRadius * (float) Math.cos(p1 * (Math.PI / 2.0));
                float ir2 = innerRadius * (float) Math.cos(p2 * (Math.PI / 2.0));

                float alpha = 0.28f + (p1 * 0.22f);

                for (int lon = 0; lon < innerLons; lon++) {
                    double angleOffset = (-age * (6.0f + lat * 2.5f)) * (Math.PI / 180.0);
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

                    drawQuad(matrix, buffer, x1, iy1, z1, x2, iy1, z2, x3, iy2, z3, x4, iy2, z4, 0.95f, 0.05f, 0.35f, alpha);
                }
            }

            // 3. 16 Orbiting 3D Jagged Obsidian Void Spires (Floating & Tilting around domain perimeter)
            int spireCount = 16;
            float spireOrbitR = radius * 0.92f;
            float spireRot = age * 15.0f;

            for (int i = 0; i < spireCount; i++) {
                double sAngle = (i / (double) spireCount) * Math.PI * 2.0 + Math.toRadians(spireRot);
                float sx = (float) Math.cos(sAngle) * spireOrbitR;
                float sz = (float) Math.sin(sAngle) * spireOrbitR;
                float sy = 1.0f + (float) Math.sin(age * 0.15f + i * 0.8f) * 0.8f;

                float spireHeight = 3.2f + (i % 3 == 0 ? 1.5f : 0.0f);
                float spireWidth = 0.35f;

                // Inward lean vector toward player
                float leanX = -(float) Math.cos(sAngle) * 0.6f;
                float leanZ = -(float) Math.sin(sAngle) * 0.6f;

                // Tangent vector
                float tx = -(float) Math.sin(sAngle) * spireWidth;
                float tz = (float) Math.cos(sAngle) * spireWidth;

                draw3DShardPyramid(matrix, buffer,
                    sx - tx, sy, sz - tz,
                    sx + tx, sy, sz + tz,
                    sx + leanX, sy + spireHeight, sz + leanZ,
                    0.15f, 0.05f, 0.22f, 0.95f,
                    0.95f, 0.05f, 0.40f, 1.0f
                );
            }

            // 4. Central Abyssal Vortex Funnel Column (Towering vertical cyclonic vortex)
            int eyeLevels = 8;
            float eyeRadius = Math.max(1.8f, radius * 0.16f);
            for (int lvl = 0; lvl < eyeLevels; lvl++) {
                float ey1 = (lvl / (float) eyeLevels) * domeHeight;
                float ey2 = ((lvl + 1) / (float) eyeLevels) * domeHeight;
                float er1 = eyeRadius * (0.6f + 0.5f * (lvl / (float) eyeLevels));
                float er2 = eyeRadius * (0.6f + 0.5f * ((lvl + 1) / (float) eyeLevels));

                float rotEye = age * (24.0f + lvl * 4.0f) * (float) (Math.PI / 180.0);
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

                    drawQuad(matrix, buffer, x1, ey1, z1, x2, ey1, z2, x3, ey2, z3, x4, ey2, z4, 0.85f, 0.05f, 0.95f, 0.45f);
                }
            }

            // 5. Atmospheric 3D Void Lightning Discharges
            Random rng = new Random(state.seed + ((long) (age / 3.0f) * 1000));
            int arcCount = 4;
            for (int a = 0; a < arcCount; a++) {
                double startAngle = rng.nextDouble() * Math.PI * 2.0;
                float arcR = radius * (0.30f + rng.nextFloat() * 0.60f);
                float sx = (float) Math.cos(startAngle) * arcR;
                float sz = (float) Math.sin(startAngle) * arcR;
                float sy = 6.0f + rng.nextFloat() * 10.0f;

                float curX = sx, curY = sy, curZ = sz;
                int arcSteps = 5;
                for (int s = 0; s < arcSteps; s++) {
                    float nxtX = curX + (rng.nextFloat() - 0.5f) * 3.2f;
                    float nxtY = curY - (1.2f + rng.nextFloat() * 1.6f);
                    float nxtZ = curZ + (rng.nextFloat() - 0.5f) * 3.2f;

                    renderBeam(matrix, buffer, curX, curY, curZ, nxtX, nxtY, nxtZ, 0.18f, 0.95f, 0.05f, 0.25f, 0.90f);
                    renderBeam(matrix, buffer, curX, curY, curZ, nxtX, nxtY, nxtZ, 0.06f, 1.0f, 0.85f, 0.95f, 1.0f);

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

    private static void draw3DShardPyramid(Matrix4f matrix, VertexConsumer consumer,
                                          float x1, float y1, float z1,
                                          float x2, float y2, float z2,
                                          float tipX, float tipY, float tipZ,
                                          float rBase, float gBase, float bBase, float aBase,
                                          float rTip, float gTip, float bTip, float aTip) {
        // Front Face
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Back Face
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
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
