package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.ZGravitySlamEntity;
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
 * Entity Renderer for Z-Sword Gravity Slam in Minecraft 26.2.
 * Renders massive radiating 3D tectonic gravity canyon trenches, colossal erupted Katchin monolith slabs,
 * expanding 3D gravitational shockwave dome, and vertical erupting singularity kinetic geysers.
 */
public class ZGravitySlamRenderer extends EntityRenderer<ZGravitySlamEntity, ZGravitySlamRenderer.ZGravitySlamRenderState> {

    public ZGravitySlamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ZGravitySlamRenderState extends EntityRenderState {
        public float radius = 10.0f;
        public float powerRatio = 1.0f;
        public float age = 0;
        public int lifetime = 50;
        public long seed = 0;
    }

    @Override
    public boolean shouldRender(ZGravitySlamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public ZGravitySlamRenderState createRenderState() {
        return new ZGravitySlamRenderState();
    }

    @Override
    public void extractRenderState(ZGravitySlamEntity entity, ZGravitySlamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.powerRatio = entity.getPowerRatio();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(ZGravitySlamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        float power = state.powerRatio;
        float age = state.age;
        float progress = Math.min(1.0f, age / (float) state.lifetime);
        float fade = Math.max(0.0f, 1.0f - progress);
        RenderType renderType = KiRenderHelper.kiRenderType();

        float rCol = 1.0f;
        float gCol = 0.82f;
        float bCol = 0.20f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Expanding 3D Gravitational Shockwave Discs & Kinetic Repulsion Dome
            float shockR = Math.min(radius * 1.35f, age * (0.85f + power * 0.45f));
            if (fade > 0.05f) {
                // Outer Golden Shockwave Crest
                drawGroundRing(matrix, buffer, 0, 0.04f, 0, shockR, shockR * 0.85f, 36, rCol, gCol, bCol, fade * 0.90f);
                // Inner Violet Gravitational Core Bed
                drawGroundRing(matrix, buffer, 0, 0.05f, 0, shockR * 0.65f, 0.0f, 28, 0.55f, 0.15f, 0.95f, fade * 0.75f);

                // Expanding 3D Hemispherical Kinetic Repulsion Dome Wall
                float domeH = (3.0f + power * 3.5f) * (1.0f - progress * 0.55f);
                drawShockwaveDome(matrix, buffer, 0, 0.05f, 0, shockR, domeH, 32, rCol, gCol, bCol, fade * 0.65f);
            }

            // 2. Radiating 3D Tectonic Gravity Canyon Fractures / Branching Rupture Trenches
            int trenchCount = 14 + (int) (power * 6); // 14 to 20 trenches
            for (int t = 0; t < trenchCount; t++) {
                double tAngle = (t / (double) trenchCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.25);
                float trenchLen = radius * (0.75f + rng.nextFloat() * 0.40f);

                float curX = 0, curZ = 0;
                int steps = 6;
                for (int s = 0; s < steps; s++) {
                    float nxtX = curX + (float) Math.cos(tAngle) * (trenchLen / steps) + (rng.nextFloat() - 0.5f) * 0.45f;
                    float nxtZ = curZ + (float) Math.sin(tAngle) * (trenchLen / steps) + (rng.nextFloat() - 0.5f) * 0.45f;

                    float width = (0.35f + power * 0.20f) * (1.0f - (s / (float) steps) * 0.45f) * Math.min(1.0f, age * 0.40f);
                    // Violet Gravity Base Trench
                    drawTrenchSegment(matrix, buffer, curX, curZ, nxtX, nxtZ, width, 0.35f, 0.08f, 0.65f, fade * 0.95f);
                    // Glowing Incandescent Golden Core
                    drawTrenchSegment(matrix, buffer, curX, curZ, nxtX, nxtZ, width * 0.40f, 1.0f, 0.95f, 0.45f, fade);

                    curX = nxtX; curZ = nxtZ;
                }
            }

            // 3. Colossal Erupted 3D Katchin Monolith Slabs & Shattered Earth Crags
            int rockCount = 18 + (int) (power * 10); // 18 to 28 monoliths
            for (int r = 0; r < rockCount; r++) {
                double rockAngle = (r / (double) rockCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.35);
                float rockDist = radius * (0.22f + rng.nextFloat() * 0.70f);

                float rx = (float) Math.cos(rockAngle) * rockDist;
                float rz = (float) Math.sin(rockAngle) * rockDist;

                float maxH = (1.8f + power * 2.2f) + rng.nextFloat() * (2.0f + power * 1.5f);
                float slabHeight = maxH * Math.min(1.0f, age * 0.50f);
                float slabWidth = 0.50f + rng.nextFloat() * 0.45f;

                // Outward leaning angle
                float tiltFactor = 0.30f + rng.nextFloat() * 0.30f;
                float tiltX = (float) Math.cos(rockAngle) * (slabHeight * tiltFactor);
                float tiltZ = (float) Math.sin(rockAngle) * (slabHeight * tiltFactor);

                float tx = -(float) Math.sin(rockAngle) * slabWidth * 0.5f;
                float tz = (float) Math.cos(rockAngle) * slabWidth * 0.5f;

                // 3D Monolith (Dark Katchin top with radiant golden-violet underbelly)
                drawRockMonolith(matrix, buffer,
                    rx - tx, 0.05f, rz - tz,
                    rx + tx, 0.05f, rz + tz,
                    rx + tiltX, slabHeight, rz + tiltZ,
                    0.65f, 0.20f, 0.95f, 0.95f * fade,
                    0.20f, 0.15f, 0.28f, 1.0f * fade
                );
            }

            // 4. Vertical Erupting Gravitational Singularity Pillars / Energy Geysers
            int geyserCount = 8 + (int) (power * 4); // 8 to 12 geysers
            for (int g = 0; g < geyserCount; g++) {
                double gAng = (g / (double) geyserCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.2);
                float gDist = radius * (0.40f + rng.nextFloat() * 0.45f);
                float gx = (float) Math.cos(gAng) * gDist;
                float gz = (float) Math.sin(gAng) * gDist;

                float gHeight = (6.0f + power * 4.0f) + rng.nextFloat() * 3.0f;
                float gWidth = 0.60f + power * 0.25f;

                drawVerticalGeyser(matrix, buffer, gx, gz, gHeight, gWidth, fade);
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
            consumer.addVertex(matrix, x2 * 0.35f, cy + height, z2 * 0.35f).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1 * 0.35f, cy + height, z1 * 0.35f).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
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
        // Core Golden Column
        consumer.addVertex(matrix, x - width, 0.05f, z).setColor(1.0f, 0.90f, 0.30f, fade * 0.95f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x + width, 0.05f, z).setColor(1.0f, 0.90f, 0.30f, fade * 0.95f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x + width * 0.3f, height, z).setColor(0.60f, 0.15f, 0.95f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x - width * 0.3f, height, z).setColor(0.60f, 0.15f, 0.95f, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Perpendicular Violet Column
        consumer.addVertex(matrix, x, 0.05f, z - width).setColor(1.0f, 0.90f, 0.30f, fade * 0.95f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x, 0.05f, z + width).setColor(1.0f, 0.90f, 0.30f, fade * 0.95f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x, height, z + width * 0.3f).setColor(0.60f, 0.15f, 0.95f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x, height, z - width * 0.3f).setColor(0.60f, 0.15f, 0.95f, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
