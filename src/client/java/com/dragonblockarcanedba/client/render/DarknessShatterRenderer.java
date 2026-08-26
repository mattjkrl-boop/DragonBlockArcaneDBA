package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DarknessShatterEntity;
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
 * Entity Renderer for Darkness Void Shatter & Shockwave in Minecraft 26.2.
 * Renders physical 3D tectonic fissure trenches, erupting jagged obsidian slabs,
 * expanding multi-tiered void shockwave rings, and a central vertical void eruption column.
 */
public class DarknessShatterRenderer extends EntityRenderer<DarknessShatterEntity, DarknessShatterRenderer.ShatterRenderState> {

    public DarknessShatterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShatterRenderState extends EntityRenderState {
        public float radius = 4.0f;
        public float age = 0;
        public int lifetime = 40;
        public long seed = 0;
    }

    @Override
    public boolean shouldRender(DarknessShatterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public ShatterRenderState createRenderState() {
        return new ShatterRenderState();
    }

    @Override
    public void extractRenderState(DarknessShatterEntity entity, ShatterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(ShatterRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        float age = state.age;
        float fade = Math.max(0.0f, 1.0f - (age / (float) (state.lifetime + age)));
        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Expanding Multi-Tiered Void Ground Shockwave Disks
            float shockR = Math.min(radius * 1.35f, age * 0.55f);
            if (fade > 0.05f) {
                // Outer Deep Violet Plasma Ring
                drawGroundRing(matrix, buffer, 0, 0.04f, 0, shockR, shockR * 0.82f, 28, 0.55f, 0.05f, 0.90f, fade * 0.88f);
                // Middle Glowing Demonic Crimson Ring
                drawGroundRing(matrix, buffer, 0, 0.05f, 0, shockR * 0.80f, shockR * 0.62f, 24, 0.95f, 0.08f, 0.35f, fade * 0.92f);
                // Inner Obsidian Void Accretion Disc
                drawGroundRing(matrix, buffer, 0, 0.06f, 0, shockR * 0.58f, 0.0f, 18, 0.12f, 0.0f, 0.22f, fade * 0.95f);
            }

            // 2. 8 Radiating 3D Tectonic Fissure Trenches
            int trenchCount = 8;
            for (int t = 0; t < trenchCount; t++) {
                double tAngle = (t / (double) trenchCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.3);
                float trenchLen = radius * (0.70f + rng.nextFloat() * 0.45f);

                float curX = 0, curZ = 0;
                int steps = 5;
                for (int s = 0; s < steps; s++) {
                    float nxtX = curX + (float) Math.cos(tAngle) * (trenchLen / steps) + (rng.nextFloat() - 0.5f) * 0.45f;
                    float nxtZ = curZ + (float) Math.sin(tAngle) * (trenchLen / steps) + (rng.nextFloat() - 0.5f) * 0.45f;

                    float width = 0.30f * (1.0f - (s / (float) steps)) * Math.min(1.0f, age * 0.35f);
                    // Magma Void Trench Base
                    drawTrenchSegment(matrix, buffer, curX, curZ, nxtX, nxtZ, width, 0.65f, 0.05f, 0.95f, fade * 0.95f);
                    // Inner Demonic Core
                    drawTrenchSegment(matrix, buffer, curX, curZ, nxtX, nxtZ, width * 0.4f, 1.0f, 0.20f, 0.50f, fade);

                    curX = nxtX; curZ = nxtZ;
                }
            }

            // 3. 12 Erupting 3D Shattered Obsidian / Void Debris Slabs
            int rockCount = 12;
            for (int r = 0; r < rockCount; r++) {
                double rockAngle = (r / (double) rockCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.4);
                float rockDist = radius * (0.35f + rng.nextFloat() * 0.55f);

                float rx = (float) Math.cos(rockAngle) * rockDist;
                float rz = (float) Math.sin(rockAngle) * rockDist;

                float slabHeight = (0.6f + rng.nextFloat() * 0.9f) * Math.min(1.0f, age * 0.45f);
                float slabWidth = 0.38f + rng.nextFloat() * 0.32f;

                // Slab leans outward from center
                float tiltX = (float) Math.cos(rockAngle) * 0.35f;
                float tiltZ = (float) Math.sin(rockAngle) * 0.35f;

                float tx = -(float) Math.sin(rockAngle) * slabWidth * 0.5f;
                float tz = (float) Math.cos(rockAngle) * slabWidth * 0.5f;

                drawRockSlab(matrix, buffer,
                    rx - tx, 0.05f, rz - tz,
                    rx + tx, 0.05f, rz + tz,
                    rx + tiltX, slabHeight, rz + tiltZ,
                    0.08f, 0.0f, 0.16f, 0.95f * fade,
                    0.75f, 0.05f, 0.95f, 1.0f * fade
                );
            }

            // 4. Central Towering Vertical Void Eruption Column
            if (fade > 0.1f) {
                float colHeight = (radius * 1.8f) * Math.min(1.0f, age * 0.3f);
                float colRadius = (radius * 0.25f) * (1.0f - (age / (float) state.lifetime) * 0.5f);
                int colSegments = 12;

                for (int c = 0; c < colSegments; c++) {
                    double a1 = ((c / (double) colSegments) * Math.PI * 2.0) + (age * 0.15);
                    double a2 = (((c + 1) / (double) colSegments) * Math.PI * 2.0) + (age * 0.15);

                    float cx1 = (float) Math.cos(a1) * colRadius;
                    float cz1 = (float) Math.sin(a1) * colRadius;
                    float cx2 = (float) Math.cos(a2) * colRadius;
                    float cz2 = (float) Math.sin(a2) * colRadius;

                    float cx3 = cx2 * 0.7f;
                    float cz3 = cz2 * 0.7f;
                    float cx4 = cx1 * 0.7f;
                    float cz4 = cz1 * 0.7f;

                    drawQuad(matrix, buffer, cx1, 0.05f, cz1, cx2, 0.05f, cz2, cx3, colHeight, cz3, cx4, colHeight, cz4,
                        0.75f, 0.05f, 0.95f, fade * 0.75f);
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

    private static void drawRockSlab(Matrix4f matrix, VertexConsumer consumer,
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

    private static void drawGroundRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2;

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

        // Reverse
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
