package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.KingsSlamEntity;
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
 * Entity Renderer for Ox King's Slam (Normal and Flawless) in Minecraft 26.2.
 * Renders massive radiating 3D tectonic canyon trenches, colossal erupted basalt monoliths,
 * expanding 3D volcanic shockwave domes, and vertical erupting magma geysers.
 */
public class KingsSlamRenderer extends EntityRenderer<KingsSlamEntity, KingsSlamRenderer.KingsSlamRenderState> {

    public KingsSlamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class KingsSlamRenderState extends EntityRenderState {
        public float radius = 10.0f;
        public boolean isFlawless = false;
        public float age = 0;
        public int lifetime = 40;
        public long seed = 0;
    }

    @Override
    public boolean shouldRender(KingsSlamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public KingsSlamRenderState createRenderState() {
        return new KingsSlamRenderState();
    }

    @Override
    public void extractRenderState(KingsSlamEntity entity, KingsSlamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.isFlawless = entity.isFlawless();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(KingsSlamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        boolean isFlawless = state.isFlawless;
        float age = state.age;
        float progress = Math.min(1.0f, age / (float) state.lifetime);
        float fade = Math.max(0.0f, 1.0f - progress);
        RenderType renderType = KiRenderHelper.kiRenderType();

        float rCol = isFlawless ? 1.0f : 1.0f;
        float gCol = isFlawless ? 0.85f : 0.35f;
        float bCol = isFlawless ? 0.20f : 0.02f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Expanding 3D Volcanic Ground Shockwave Discs & Caldera Blast Core
            float shockR = Math.min(radius * 1.35f, age * (isFlawless ? 0.95f : 0.70f));
            if (fade > 0.05f) {
                // Outer Shockwave Crest
                drawGroundRing(matrix, buffer, 0, 0.04f, 0, shockR, shockR * 0.84f, 32, rCol, gCol, bCol, fade * 0.85f);
                // Inner Incandescent Caldera Bed
                drawGroundRing(matrix, buffer, 0, 0.05f, 0, shockR * 0.65f, 0.0f, 24, 1.0f, isFlawless ? 0.95f : 0.60f, 0.10f, fade * 0.70f);

                // Expanding 3D Hemispherical Volcanic Dome Wall
                float domeH = (2.5f + (isFlawless ? 3.0f : 1.5f)) * (1.0f - progress * 0.6f);
                drawShockwaveDome(matrix, buffer, 0, 0.05f, 0, shockR, domeH, 28, rCol, gCol, bCol, fade * 0.65f);
            }

            // 2. Radiating 3D Tectonic Canyon Fissure Trenches
            int trenchCount = isFlawless ? 18 : 12;
            for (int t = 0; t < trenchCount; t++) {
                double tAngle = (t / (double) trenchCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.25);
                float trenchLen = radius * (0.70f + rng.nextFloat() * 0.45f);

                float curX = 0, curZ = 0;
                int steps = 6;
                for (int s = 0; s < steps; s++) {
                    float nxtX = curX + (float) Math.cos(tAngle) * (trenchLen / steps) + (rng.nextFloat() - 0.5f) * 0.45f;
                    float nxtZ = curZ + (float) Math.sin(tAngle) * (trenchLen / steps) + (rng.nextFloat() - 0.5f) * 0.45f;

                    float width = (isFlawless ? 0.45f : 0.32f) * (1.0f - (s / (float) steps) * 0.4f) * Math.min(1.0f, age * 0.35f);
                    // Magma Base Trench
                    drawTrenchSegment(matrix, buffer, curX, curZ, nxtX, nxtZ, width, rCol, gCol * 0.7f, bCol * 0.5f, fade * 0.95f);
                    // Glowing Molten Core
                    drawTrenchSegment(matrix, buffer, curX, curZ, nxtX, nxtZ, width * 0.38f, 1.0f, isFlawless ? 1.0f : 0.85f, 0.40f, fade);

                    curX = nxtX; curZ = nxtZ;
                }
            }

            // 3. Colossal Erupted Basalt Monoliths & Jagged Earth Crags
            int rockCount = isFlawless ? 28 : 16;
            for (int r = 0; r < rockCount; r++) {
                double rockAngle = (r / (double) rockCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.35);
                float rockDist = radius * (0.25f + rng.nextFloat() * 0.65f);

                float rx = (float) Math.cos(rockAngle) * rockDist;
                float rz = (float) Math.sin(rockAngle) * rockDist;

                float maxH = (isFlawless ? 2.5f : 1.5f) + rng.nextFloat() * (isFlawless ? 3.0f : 1.8f);
                float slabHeight = maxH * Math.min(1.0f, age * 0.45f);
                float slabWidth = 0.45f + rng.nextFloat() * 0.40f;

                // Outward leaning angle
                float tiltFactor = 0.35f + rng.nextFloat() * 0.25f;
                float tiltX = (float) Math.cos(rockAngle) * (slabHeight * tiltFactor);
                float tiltZ = (float) Math.sin(rockAngle) * (slabHeight * tiltFactor);

                float tx = -(float) Math.sin(rockAngle) * slabWidth * 0.5f;
                float tz = (float) Math.cos(rockAngle) * slabWidth * 0.5f;

                drawRockMonolith(matrix, buffer,
                    rx - tx, 0.05f, rz - tz,
                    rx + tx, 0.05f, rz + tz,
                    rx + tiltX, slabHeight, rz + tiltZ,
                    1.0f, isFlawless ? 0.70f : 0.30f, 0.05f, 0.95f * fade,
                    0.25f, 0.20f, 0.16f, 1.0f * fade
                );
            }

            // 4. Flawless King's Slam: Vertical Erupting Subterranean Magma Geysers / Fire Pillars
            if (isFlawless) {
                int geyserCount = 8;
                for (int g = 0; g < geyserCount; g++) {
                    double gAng = (g / (double) geyserCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.2);
                    float gDist = radius * (0.45f + rng.nextFloat() * 0.40f);
                    float gx = (float) Math.cos(gAng) * gDist;
                    float gz = (float) Math.sin(gAng) * gDist;

                    float gHeight = 6.0f + rng.nextFloat() * 2.5f;
                    float gWidth = 0.55f;

                    drawVerticalGeyser(matrix, buffer, gx, gz, gHeight, gWidth, fade);
                }
            }
        });
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

    private static void drawGroundRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

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

    private static void drawShockwaveDome(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float radius, float height, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = cx + (float) Math.cos(a1) * radius;
            float z1 = cz + (float) Math.sin(a1) * radius;
            float x2 = cx + (float) Math.cos(a2) * radius;
            float z2 = cz + (float) Math.sin(a2) * radius;

            // Outer dome wall
            consumer.addVertex(matrix, x1, cy, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, cy, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2 * 0.4f, cy + height, z2 * 0.4f).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1 * 0.4f, cy + height, z1 * 0.4f).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawRockMonolith(Matrix4f matrix, VertexConsumer consumer,
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
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawVerticalGeyser(Matrix4f matrix, VertexConsumer consumer, float x, float z, float height, float width, float fade) {
        // Core Column
        consumer.addVertex(matrix, x - width, 0.05f, z).setColor(1.0f, 0.90f, 0.20f, fade * 0.95f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x + width, 0.05f, z).setColor(1.0f, 0.90f, 0.20f, fade * 0.95f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x + width * 0.3f, height, z).setColor(1.0f, 0.40f, 0.02f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x - width * 0.3f, height, z).setColor(1.0f, 0.40f, 0.02f, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Perpendicular Column
        consumer.addVertex(matrix, x, 0.05f, z - width).setColor(1.0f, 0.90f, 0.20f, fade * 0.95f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x, 0.05f, z + width).setColor(1.0f, 0.90f, 0.20f, fade * 0.95f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x, height, z + width * 0.3f).setColor(1.0f, 0.40f, 0.02f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x, height, z - width * 0.3f).setColor(1.0f, 0.40f, 0.02f, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
