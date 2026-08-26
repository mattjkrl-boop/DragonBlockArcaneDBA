package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DevilSlamShockwaveEntity;
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
 * Entity Renderer for Devil Slam Shockwave in Minecraft 26.2.
 * Renders physical 3D demonic ground-shatter fissure trenches, erupting jagged brimstone/obsidian spikes,
 * expanding shockwave demonic seals, and surging vertical hellfire eruption pillars.
 */
public class DevilSlamShockwaveRenderer extends EntityRenderer<DevilSlamShockwaveEntity, DevilSlamShockwaveRenderer.DevilSlamRenderState> {

    public DevilSlamShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DevilSlamRenderState extends EntityRenderState {
        public float radius = 5.0f;
        public float age = 0;
        public int lifetime = 35;
        public long seed = 0;
    }

    @Override
    public boolean shouldRender(DevilSlamShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public DevilSlamRenderState createRenderState() {
        return new DevilSlamRenderState();
    }

    @Override
    public void extractRenderState(DevilSlamShockwaveEntity entity, DevilSlamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(DevilSlamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        float age = state.age;
        float fade = Math.max(0.0f, 1.0f - (age / (float) (state.lifetime + age)));
        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Expanding Demonic Ground Impact Shockwave Disks & Runic Seals
            float shockR = Math.min(radius * 1.35f, age * 0.55f);
            if (fade > 0.05f) {
                // Outer Shock Ring: Crimson Hellfire
                drawGroundRing(matrix, buffer, 0, 0.04f, 0, shockR, shockR * 0.84f, 28, 0.85f, 0.0f, 0.12f, fade * 0.85f);
                // Inner Blood Ring: Concentrated Demonic Surge
                drawGroundRing(matrix, buffer, 0, 0.05f, 0, shockR * 0.65f, 0.0f, 20, 1.0f, 0.05f, 0.20f, fade * 0.75f);
                // Perimeter Runic Teeth / Glyph Marks
                int teeth = 12;
                for (int i = 0; i < teeth; i++) {
                    double tAng = (i / (double) teeth) * Math.PI * 2.0 + (age * 0.02);
                    float tx1 = (float) Math.cos(tAng) * (shockR * 0.80f);
                    float tz1 = (float) Math.sin(tAng) * (shockR * 0.80f);
                    float tx2 = (float) Math.cos(tAng) * (shockR * 1.05f);
                    float tz2 = (float) Math.sin(tAng) * (shockR * 1.05f);
                    drawTrenchSegment(matrix, buffer, tx1, tz1, tx2, tz2, 0.12f, 1.0f, 0.2f, 0.3f, fade * 0.9f);
                }
            }

            // 2. 8 to 10 Radiating 3D Demonic Tectonic Fissure Trenches
            int trenchCount = 9;
            for (int t = 0; t < trenchCount; t++) {
                double tAngle = (t / (double) trenchCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.25);
                float trenchLen = radius * (0.75f + rng.nextFloat() * 0.50f);

                float curX = 0, curZ = 0;
                int steps = 6;
                for (int s = 0; s < steps; s++) {
                    float nxtX = curX + (float) Math.cos(tAngle) * (trenchLen / steps) + (rng.nextFloat() - 0.5f) * 0.45f;
                    float nxtZ = curZ + (float) Math.sin(tAngle) * (trenchLen / steps) + (rng.nextFloat() - 0.5f) * 0.45f;

                    float width = 0.35f * (1.0f - (s / (float) steps)) * Math.min(1.0f, age * 0.35f);
                    // Magma Blood Trench Base
                    drawTrenchSegment(matrix, buffer, curX, curZ, nxtX, nxtZ, width, 0.85f, 0.0f, 0.12f, fade * 0.95f);
                    // Inner Burning Core
                    drawTrenchSegment(matrix, buffer, curX, curZ, nxtX, nxtZ, width * 0.38f, 1.0f, 0.75f, 0.85f, fade);

                    curX = nxtX; curZ = nxtZ;
                }
            }

            // 3. 14 Erupting 3D Jagged Brimstone / Demonic Obsidian Spikes
            int spikeCount = 14;
            for (int r = 0; r < spikeCount; r++) {
                double spikeAngle = (r / (double) spikeCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.35);
                float spikeDist = radius * (0.30f + rng.nextFloat() * 0.65f);

                float rx = (float) Math.cos(spikeAngle) * spikeDist;
                float rz = (float) Math.sin(spikeAngle) * spikeDist;

                float spikeHeight = (0.7f + rng.nextFloat() * 1.1f) * Math.min(1.0f, age * 0.45f);
                float spikeWidth = 0.32f + rng.nextFloat() * 0.28f;

                // Spike leans outward from center
                float tiltX = (float) Math.cos(spikeAngle) * 0.35f;
                float tiltZ = (float) Math.sin(spikeAngle) * 0.35f;

                float tx = -(float) Math.sin(spikeAngle) * spikeWidth * 0.5f;
                float tz = (float) Math.cos(spikeAngle) * spikeWidth * 0.5f;

                drawSpike(matrix, buffer,
                    rx - tx, 0.05f, rz - tz,
                    rx + tx, 0.05f, rz + tz,
                    rx + tiltX, spikeHeight, rz + tiltZ,
                    0.18f, 0.0f, 0.05f, 0.95f * fade,
                    1.0f, 0.08f, 0.22f, 1.0f * fade
                );
            }

            // 4. Surging Vertical Demonic Hellfire Eruption Column
            float pillarLife = 14.0f;
            if (age < pillarLife) {
                float pillarProgress = age / pillarLife;
                float pillarAlpha = (1.0f - pillarProgress) * 0.85f;
                float pillarHeight = 6.5f * Math.min(1.0f, age * 0.4f);
                float pillarRadius = 0.75f * (1.0f + pillarProgress * 0.4f);

                // Outer Translucent Crimson Energy Shroud
                drawCylinderPillar(matrix, buffer, 0, 0, pillarRadius, pillarHeight, 8, 0.85f, 0.0f, 0.12f, pillarAlpha * 0.65f);
                // Inner Dense Core Beam
                drawCylinderPillar(matrix, buffer, 0, 0, pillarRadius * 0.45f, pillarHeight * 1.05f, 6, 1.0f, 0.80f, 0.85f, pillarAlpha * 0.95f);
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

    private static void drawSpike(Matrix4f matrix, VertexConsumer consumer,
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

    private static void drawCylinderPillar(Matrix4f matrix, VertexConsumer consumer, float cx, float cz, float radius, float height, int sides, float r, float g, float b, float a) {
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2;

            float x1 = cx + (float) Math.cos(a1) * radius;
            float z1 = cz + (float) Math.sin(a1) * radius;
            float x2 = cx + (float) Math.cos(a2) * radius;
            float z2 = cz + (float) Math.sin(a2) * radius;

            consumer.addVertex(matrix, x1, 0.05f, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, 0.05f, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, height, z2).setColor(r, g, b, a * 0.2f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, height, z1).setColor(r, g, b, a * 0.2f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Reverse
            consumer.addVertex(matrix, x1, height, z1).setColor(r, g, b, a * 0.2f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, height, z2).setColor(r, g, b, a * 0.2f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, 0.05f, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, 0.05f, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }
}
