package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DimensionalWarpRiftEntity;
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
 * Entity Renderer for Dimensional Warp Rift in Minecraft 26.2.
 * Renders a physical 3D spacetime tear and void portal:
 * - Ground-plane 3D rotating spacetime astrolabe decal & concentric shock rings
 * - 8 radiating 3D tectonic spatial fissure trenches
 * - 10 floating/erupting shattered obsidian void debris slabs
 * - Towering volumetric vertical spacetime tear event horizon slit with white-hot reality seam
 * - Dual tilted gyroscopic orbital accretion rings (+38° and -38°)
 * - 8 orbiting 3D crystalline void shards (spiraling inward at origin, erupting outward at destination)
 * - Central spinning singularity octahedron core
 */
public class DimensionalWarpRiftRenderer extends EntityRenderer<DimensionalWarpRiftEntity, DimensionalWarpRiftRenderer.WarpRiftRenderState> {

    public DimensionalWarpRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class WarpRiftRenderState extends EntityRenderState {
        public float radius = 3.5f;
        public boolean isDestination = false;
        public float age = 0;
        public int maxLifetime = 24;
        public long seed = 0;
    }

    @Override
    public boolean shouldRender(DimensionalWarpRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public WarpRiftRenderState createRenderState() {
        return new WarpRiftRenderState();
    }

    @Override
    public void extractRenderState(DimensionalWarpRiftEntity entity, WarpRiftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRiftRadius();
        state.isDestination = entity.isDestination();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(WarpRiftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        if (progress >= 1.0f) return;

        float entrance = Math.min(1.0f, state.age / 4.0f);
        float exit = Math.max(0.0f, 1.0f - progress);
        float fade = (float) Math.sin(progress * Math.PI);
        if (fade <= 0.001f) return;

        float expansion = state.isDestination ? (0.6f + progress * 0.7f) : (1.0f - progress * 0.4f);
        float radius = state.radius * entrance * expansion;
        float pulse = 0.88f + 0.12f * (float) Math.sin(state.age * 0.5f);
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Ground-Plane 3D Rotating Spacetime Astrolabe & Concentric Shock Rings
            int groundSegments = 32;

            // Outer Crimson Rift Ring
            drawRotatingRing(matrix, buffer, 0, 0.04f, 0, radius * 1.05f, radius * 0.92f, groundSegments, state.age * 8.0f,
                0.85f, 0.05f, 0.20f, fade * 0.85f * pulse);

            // Middle Abyssal Violet Glyph Ring
            drawRotatingRing(matrix, buffer, 0, 0.06f, 0, radius * 0.82f, radius * 0.68f, groundSegments, -state.age * 12.0f,
                0.55f, 0.02f, 0.90f, fade * 0.90f * pulse);

            // Inner Neon Magenta Accretion Ring
            drawRotatingRing(matrix, buffer, 0, 0.08f, 0, radius * 0.50f, radius * 0.32f, groundSegments, state.age * 20.0f,
                1.0f, 0.15f, 0.85f, fade * 0.95f * pulse);

            // Central Pure White Singularity Eye Disk
            drawRotatingRing(matrix, buffer, 0, 0.10f, 0, radius * 0.22f, 0.0f, 16, -state.age * 28.0f,
                1.0f, 0.95f, 1.0f, fade * 1.0f);

            // 2. 8 Radiating 3D Tectonic Spatial Fissure Trenches
            int trenchCount = 8;
            for (int t = 0; t < trenchCount; t++) {
                double tAngle = (t / (double) trenchCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.25);
                float trenchLen = radius * (0.85f + rng.nextFloat() * 0.35f);

                float curX = 0, curZ = 0;
                int steps = 4;
                for (int s = 0; s < steps; s++) {
                    float nxtX = curX + (float) Math.cos(tAngle) * (trenchLen / steps) + (rng.nextFloat() - 0.5f) * 0.3f;
                    float nxtZ = curZ + (float) Math.sin(tAngle) * (trenchLen / steps) + (rng.nextFloat() - 0.5f) * 0.3f;

                    float width = 0.22f * (1.0f - (s / (float) steps)) * entrance;
                    // Deep violet base
                    drawTrenchSegment(matrix, buffer, curX, curZ, nxtX, nxtZ, width,
                        0.60f, 0.05f, 0.90f, fade * 0.90f);
                    // Magma blood-red core
                    drawTrenchSegment(matrix, buffer, curX, curZ, nxtX, nxtZ, width * 0.45f,
                        1.0f, 0.10f, 0.25f, fade * 0.95f);

                    curX = nxtX;
                    curZ = nxtZ;
                }
            }

            // 3. 10 Floating/Erupting Shattered Obsidian Void Debris Slabs
            int slabCount = 10;
            for (int s = 0; s < slabCount; s++) {
                double sAngle = (s / (double) slabCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.3);
                float sDist = radius * (0.45f + rng.nextFloat() * 0.45f);

                float sx = (float) Math.cos(sAngle) * sDist;
                float sz = (float) Math.sin(sAngle) * sDist;

                float slabHeight = (0.4f + rng.nextFloat() * 0.6f) * entrance * (1.0f - (state.isDestination ? 0.0f : progress * 0.5f));
                float slabWidth = 0.25f + rng.nextFloat() * 0.2f;

                float tiltX = (float) Math.cos(sAngle) * (state.isDestination ? -0.2f : 0.25f);
                float tiltZ = (float) Math.sin(sAngle) * (state.isDestination ? -0.2f : 0.25f);

                float perpX = -(float) Math.sin(sAngle) * slabWidth * 0.5f;
                float perpZ = (float) Math.cos(sAngle) * slabWidth * 0.5f;

                drawRockSlab(matrix, buffer,
                    sx - perpX, 0.05f, sz - perpZ,
                    sx + perpX, 0.05f, sz + perpZ,
                    sx + tiltX, slabHeight, sz + tiltZ,
                    0.12f, 0.02f, 0.20f, fade * 0.90f,
                    0.95f, 0.10f, 0.45f, fade * 0.95f
                );
            }

            // 4. Towering Volumetric Vertical Spacetime Tear (Event Horizon Portal Slit)
            float tearH = (2.6f + pulse * 0.4f) * entrance * (state.isDestination ? 1.1f : (1.0f - progress * 0.3f));
            float tearW = 0.45f * entrance * (state.isDestination ? (1.0f + progress * 0.3f) : (1.0f - progress * 0.5f));
            float tearY = 1.4f;

            // Multi-angle vertical diamond tear planes
            drawVerticalTearSlit(matrix, buffer, tearW, tearH, tearY, 0.0f, fade);
            drawVerticalTearSlit(matrix, buffer, tearW * 0.75f, tearH * 0.85f, tearY, 45.0f, fade * 0.85f);
            drawVerticalTearSlit(matrix, buffer, tearW, tearH, tearY, 90.0f, fade);
            drawVerticalTearSlit(matrix, buffer, tearW * 0.75f, tearH * 0.85f, tearY, 135.0f, fade * 0.85f);

            // 5. Dual Tilted Gyroscopic Orbital Accretion Rings (+38° and -38°)
            float ringR = radius * 0.75f;
            drawTiltedOrbitalRing(matrix, buffer, ringR, tearY, 38.0f, state.age * 18.0f,
                0.85f, 0.05f, 0.45f, fade * 0.85f * pulse);
            drawTiltedOrbitalRing(matrix, buffer, ringR * 0.82f, tearY, -38.0f, -state.age * 22.0f,
                0.45f, 0.02f, 0.95f, fade * 0.85f * pulse);

            // 6. 8 Orbiting 3D Crystalline Void Shards (Pyramids)
            int shardCount = 8;
            for (int i = 0; i < shardCount; i++) {
                double shardAngle = (i / (double) shardCount) * Math.PI * 2.0 + Math.toRadians(state.age * 25.0f);
                float shardOrbit = state.isDestination
                    ? (radius * (0.3f + progress * 0.6f)) // Expanding outward on destination
                    : (radius * (0.85f - progress * 0.55f)); // Spiraling inward on origin

                float sx = (float) Math.cos(shardAngle) * shardOrbit;
                float sz = (float) Math.sin(shardAngle) * shardOrbit;
                float sy = tearY + (float) Math.sin(state.age * 0.3f + i * 0.8f) * 0.5f;

                float shardSize = 0.28f * entrance;
                draw3DShardPyramid(matrix, buffer,
                    sx - shardSize * 0.5f, sy, sz - shardSize * 0.5f,
                    sx + shardSize * 0.5f, sy, sz + shardSize * 0.5f,
                    sx, sy + shardSize * 1.4f * (i % 2 == 0 ? 1.0f : -1.0f), sz,
                    0.20f, 0.02f, 0.35f, fade * 0.85f,
                    1.0f, 0.15f, 0.85f, fade * 0.98f
                );
            }

            // 7. Central Spinning Singularity Octahedron Core
            float coreSize = 0.45f * entrance * pulse * (state.isDestination ? (1.0f + progress * 0.4f) : (1.0f - progress * 0.4f));
            drawSingularityOctahedron(matrix, buffer, 0, tearY, 0, coreSize, state.age * 35.0f,
                0.02f, 0.0f, 0.05f, 0.99f,
                0.90f, 0.10f, 0.95f, fade * 0.95f,
                1.0f, 1.0f, 1.0f, fade
            );
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawRotatingRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        float rotRad = (float) Math.toRadians(rotDeg);
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

            consumer.addVertex(matrix, ix1, cy, iz1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ix2, cy, iz2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, cy, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, cy, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawTrenchSegment(Matrix4f matrix, VertexConsumer consumer, float x1, float z1, float x2, float z2, float width, float r, float g, float b, float a) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * width * 0.5f;
        float nz = dx / len * width * 0.5f;

        consumer.addVertex(matrix, x1 - nx, 0.05f, z1 - nz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx, 0.05f, z1 + nz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx, 0.05f, z2 + nz).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx, 0.05f, z2 - nz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
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

    private static void drawVerticalTearSlit(Matrix4f matrix, VertexConsumer consumer, float halfW, float halfH, float cy, float angleDeg, float fade) {
        double rad = Math.toRadians(angleDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float x1 = -halfW * cos;
        float z1 = -halfW * sin;
        float x2 = halfW * cos;
        float z2 = halfW * sin;

        // Outer Dark Void Corona
        drawTearQuad(matrix, consumer, x1 * 1.6f, cy, z1 * 1.6f, x2 * 1.6f, cy, z2 * 1.6f, 0, cy + halfH * 1.3f, 0, 0, cy - halfH * 1.3f, 0,
            0.60f, 0.05f, 0.90f, fade * 0.70f);

        // Mid Magenta Slit
        drawTearQuad(matrix, consumer, x1, cy, z1, x2, cy, z2, 0, cy + halfH, 0, 0, cy - halfH, 0,
            1.0f, 0.15f, 0.85f, fade * 0.90f);

        // Core Pitch-Black Event Horizon Slit
        drawTearQuad(matrix, consumer, x1 * 0.5f, cy, z1 * 0.5f, x2 * 0.5f, cy, z2 * 0.5f, 0, cy + halfH * 0.7f, 0, 0, cy - halfH * 0.7f, 0,
            0.02f, 0.0f, 0.05f, 0.98f);

        // Center White-Hot Reality Seam
        drawTearQuad(matrix, consumer, x1 * 0.15f, cy, z1 * 0.15f, x2 * 0.15f, cy, z2 * 0.15f, 0, cy + halfH * 0.9f, 0, 0, cy - halfH * 0.9f, 0,
            1.0f, 1.0f, 1.0f, fade);
    }

    private static void drawTearQuad(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float topX, float topY, float topZ, float botX, float botY, float botZ, float r, float g, float b, float a) {
        // Top triangle
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, topX, topY, topZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, topX, topY, topZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Bottom triangle
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, botX, botY, botZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, botX, botY, botZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

        // Reverse side
        consumer.addVertex(matrix, topX, topY, topZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, topX, topY, topZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

        consumer.addVertex(matrix, botX, botY, botZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, botX, botY, botZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawTiltedOrbitalRing(Matrix4f matrix, VertexConsumer consumer, float radius, float cy, float tiltDeg, float rotDeg, float r, float g, float b, float a) {
        int segments = 24;
        float width = 0.12f;
        double tiltRad = Math.toRadians(tiltDeg);
        double rotRad = Math.toRadians(rotDeg);

        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2.0) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2.0) + rotRad;

            float x1_flat = (float) Math.cos(a1) * radius;
            float z1_flat = (float) Math.sin(a1) * radius;
            float x2_flat = (float) Math.cos(a2) * radius;
            float z2_flat = (float) Math.sin(a2) * radius;

            float x1 = x1_flat;
            float y1 = cy + (float) (-z1_flat * Math.sin(tiltRad));
            float z1 = (float) (z1_flat * Math.cos(tiltRad));

            float x2 = x2_flat;
            float y2 = cy + (float) (-z2_flat * Math.sin(tiltRad));
            float z2 = (float) (z2_flat * Math.cos(tiltRad));

            consumer.addVertex(matrix, x1, y1 + width, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2 + width, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2 - width, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, y1 - width, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
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

    private static void drawSingularityOctahedron(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float rotDeg, float rVoid, float gVoid, float bVoid, float aVoid, float rGlow, float gGlow, float bGlow, float aGlow, float rCore, float gCore, float bCore, float aCore) {
        float rotRad = (float) Math.toRadians(rotDeg);
        float h = size * 1.3f;
        float w = size * 0.75f;

        for (int i = 0; i < 4; i++) {
            double a1 = (i / 4.0) * Math.PI * 2.0 + rotRad;
            double a2 = ((i + 1) / 4.0) * Math.PI * 2.0 + rotRad;

            float x1 = cx + (float) Math.cos(a1) * w;
            float z1 = cz + (float) Math.sin(a1) * w;
            float x2 = cx + (float) Math.cos(a2) * w;
            float z2 = cz + (float) Math.sin(a2) * w;

            // Outer Glow Shell Top
            consumer.addVertex(matrix, x1, cy, z1).setColor(rGlow, gGlow, bGlow, aGlow).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, cy, z2).setColor(rGlow, gGlow, bGlow, aGlow).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, cy + h, cz).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, cy + h, cz).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Outer Glow Shell Bottom
            consumer.addVertex(matrix, x2, cy, z2).setColor(rGlow, gGlow, bGlow, aGlow).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, cy, z1).setColor(rGlow, gGlow, bGlow, aGlow).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, cx, cy - h, cz).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, cx, cy - h, cz).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }
}
