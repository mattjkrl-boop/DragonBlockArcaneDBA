package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.DimensionalWarpRiftEntity;
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

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DimensionalWarpRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DimensionalWarpRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
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
        float life = (float) state.maxLifetime;
        float progress = Math.min(1.0f, state.age / life);
        if (progress >= 1.0f) return;

        // Shared lifecycle envelope (point 5): build over the first 20 percent,
        // full presence through the burst, dissipate over the last 35 percent.
        // Replaces the old sine hump, which spent half the rift's short life
        // half faded. The tear's own open and shut shaping below still applies.
        float env = FxEnvelope.envelope(state.age, life, 0.20f, 0.35f);
        if (env <= 0.001f) return;
        float fade = env;
        float distScale = FxEnvelope.distScale(Math.sqrt(Math.max(0.0, state.distanceToCameraSq)));

        float entrance = ease(Math.min(1.0f, state.age / 6.0f));
        float expansion = state.isDestination ? (0.6f + progress * 0.7f) : (1.0f - progress * 0.4f);
        float radius = state.radius * entrance * expansion;
        float pulse = 0.88f + 0.12f * (float) Math.sin(state.age * 0.5f);
        RenderType renderType = KiRenderHelper.kiRenderType();

        final float fRadius = radius;
        final float fAge = state.age;
        final float fFade = fade;
        final float fDist = distScale;
        final float fProgress = progress;
        final boolean fDest = state.isDestination;
        final long fSeed = state.seed;

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            float shut = ease(Math.max(0.0f, (fProgress - 0.55f) / 0.45f));
            float openTear = 1.0f - 0.88f * shut;
            float tearH = (2.8f + pulse * 0.35f) * entrance * openTear * (fDest ? (0.55f + 0.55f * ease(fProgress)) : (1.0f - 0.45f * ease(fProgress)));
            float tearW = 0.42f * entrance * openTear * (fDest ? (0.35f + 0.75f * ease(fProgress)) : (1.0f - 0.7f * ease(fProgress)));
            float tearY = 1.4f;

            // Ground anchor glow and the layered vortex column around the tear
            // (point 1): the shared three band system, so the rift reads in the
            // same visual language as every other spectacle effect.
            LayeredRender.drawAnchorGlow(pose, buffer, fRadius * 1.2f, 0.05f,
                0.62f, 0.10f, 0.92f, 0.55f * fFade, fDist);
            LayeredRender.drawVortexBands(pose, buffer, fRadius * 0.55f, 3.4f, fAge,
                0.62f, 0.10f, 0.92f, fFade, fDist);

            drawRotatingRing(matrix, buffer, 0, 0.04f, 0, fRadius * 0.72f, fRadius * 0.58f, 32, fAge * 10.0f,
                0.55f, 0.02f, 0.90f, fFade * 0.75f);

            // Eight radiating fissure trenches cracking out from the tear.
            for (int i = 0; i < 8; i++) {
                double ang = Math.toRadians(i * 45.0 + hash(fSeed, i) * 22.0);
                float inner = fRadius * 0.25f;
                float outer = fRadius * (0.95f + 0.30f * hash(fSeed, i + 40));
                drawTrenchSegment(matrix, buffer,
                    (float) Math.cos(ang) * inner, (float) Math.sin(ang) * inner,
                    (float) Math.cos(ang) * outer, (float) Math.sin(ang) * outer,
                    0.16f, 0.55f, 0.05f, 0.90f, fFade * 0.50f);
            }

            // Dual gyroscopic orbital rings around the tear.
            drawTiltedOrbitalRing(matrix, buffer, fRadius * 0.62f, tearY, 38.0f, fAge * 24.0f,
                1.0f, 0.15f, 0.85f, fFade * 0.65f);
            drawTiltedOrbitalRing(matrix, buffer, fRadius * 0.62f, tearY, -38.0f, -fAge * 19.0f,
                0.60f, 0.05f, 0.95f, fFade * 0.55f);

            drawVerticalTearSlit(matrix, buffer, tearW, tearH, tearY, 0.0f, fFade * openTear);
            drawTearWake(matrix, buffer, tearW, tearH, tearY, fFade * (1.0f - shut), shut);

            // Central spinning singularity, the same octahedron language as
            // the slash's core.
            drawSingularityOctahedron(matrix, buffer, 0, tearY, 0, 0.34f * pulse, fAge * 35.0f,
                0.02f, 0.0f, 0.05f, 0.99f * fFade,
                1.0f, 0.15f, 0.85f, 0.95f * fFade,
                1.0f, 1.0f, 1.0f, fFade);

            // Eight crystalline void shards: they spiral inward at the origin
            // as the world is pulled apart, and erupt outward at the
            // destination as the arrival blows the tear open.
            for (int i = 0; i < 8; i++) {
                double ang = fSeed * 0.001 + i * (Math.PI * 2.0 / 8.0) + fAge * 0.06;
                float orbitFrac = fDest
                    ? 0.30f + 0.65f * ease(fProgress)
                    : 0.85f - 0.50f * ease(fProgress);
                float orbitR = fRadius * orbitFrac;
                float cx = (float) Math.cos(ang) * orbitR;
                float cz = (float) Math.sin(ang) * orbitR;
                float cy = 0.6f + 1.7f * hash(fSeed, i + 80) + 0.18f * (float) Math.sin(fAge * 0.35 + i);
                float tx = (float) -Math.sin(ang), tz = (float) Math.cos(ang);
                float hw = 0.14f;
                draw3DShardPyramid(matrix, buffer,
                    cx + tx * hw, cy, cz + tz * hw,
                    cx - tx * hw, cy, cz - tz * hw,
                    cx * 0.55f, cy + 0.55f, cz * 0.55f,
                    0.55f, 0.05f, 0.90f, fFade * 0.75f,
                    1.0f, 0.15f, 0.85f, fFade * 0.95f);
            }

            // Six shattered void slabs levered up around the rim, tipped with
            // the tear's magenta where the space broke.
            for (int i = 0; i < 6; i++) {
                double ang = Math.toRadians(i * 60.0 + hash(fSeed, i + 120) * 26.0);
                float rimR = fRadius * 0.85f;
                float cx = (float) Math.cos(ang) * rimR;
                float cz = (float) Math.sin(ang) * rimR;
                float tx = (float) -Math.sin(ang), tz = (float) Math.cos(ang);
                float hw = 0.30f;
                float tipR = fRadius * 0.60f;
                drawRockSlab(matrix, buffer,
                    cx + tx * hw, 0.06f, cz + tz * hw,
                    cx - tx * hw, 0.06f, cz - tz * hw,
                    (float) Math.cos(ang) * tipR, 1.0f + 0.45f * hash(fSeed, i + 160), (float) Math.sin(ang) * tipR,
                    0.08f, 0.01f, 0.14f, fFade * 0.90f,
                    0.85f, 0.10f, 0.60f, fFade * 0.90f);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /** Deterministic per-rift, per-index variation in 0..1, so every tear differs slightly but never flickers. */
    private static float hash(long seed, int i) {
        long h = seed ^ (i * 0x9E3779B97F4A7C15L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
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
        // The black core now follows the same fade as every other layer; it
        // used to sit at a fixed 0.98 alpha and outlive the sine envelope.
        drawTearQuad(matrix, consumer, x1 * 0.5f, cy, z1 * 0.5f, x2 * 0.5f, cy, z2 * 0.5f, 0, cy + halfH * 0.7f, 0, 0, cy - halfH * 0.7f, 0,
            0.02f, 0.0f, 0.05f, 0.98f * fade);

        // Center White-Hot Reality Seam
        drawTearQuad(matrix, consumer, x1 * 0.15f, cy, z1 * 0.15f, x2 * 0.15f, cy, z2 * 0.15f, 0, cy + halfH * 0.9f, 0, 0, cy - halfH * 0.9f, 0,
            1.0f, 1.0f, 1.0f, fade);
    }

    /** A curved ribbon behind the tear. It pinches and disappears as the rift shuts. */
    private static void drawTearWake(Matrix4f matrix, VertexConsumer consumer, float halfW, float halfH, float cy, float fade, float shut) {
        float trail = 1.15f * (1.0f - shut);
        if (trail < 0.04f || fade < 0.02f) {
            return;
        }
        int segs = 5;
        for (int i = 0; i < segs; i++) {
            float t1 = i / (float) segs;
            float t2 = (i + 1) / (float) segs;
            float z1 = -trail * t1;
            float z2 = -trail * t2;
            float bow1 = halfW * 0.85f * (float) Math.sin(t1 * Math.PI);
            float bow2 = halfW * 0.85f * (float) Math.sin(t2 * Math.PI);
            float h1 = halfH * 0.42f * (1.0f - t1);
            float h2 = halfH * 0.42f * (1.0f - t2);
            float a1 = fade * 0.75f * (1.0f - t1);
            float a2 = fade * 0.75f * (1.0f - t2);
            wakeStrip(matrix, consumer, -halfW * 0.4f + bow1, cy + h1, z1, halfW * 0.4f + bow1, cy + h1, z1, halfW * 0.2f + bow2, cy + h2, z2, -halfW * 0.2f + bow2, cy + h2, z2, 0.75f, 0.08f, 0.95f, a1, a2);
            wakeStrip(matrix, consumer, -halfW * 0.12f + bow1, cy + h1 * 0.35f, z1, halfW * 0.12f + bow1, cy + h1 * 0.35f, z1, halfW * 0.05f + bow2, cy, z2, -halfW * 0.05f + bow2, cy, z2, 1.0f, 1.0f, 1.0f, a1, a2 * 0.4f);
            wakeStrip(matrix, consumer, bow1, cy + h1 * 0.5f, z1, bow1, cy - h1 * 0.5f, z1, bow2 * 0.4f, cy - h2 * 0.15f, z2 - trail * 0.05f, bow2 * 0.4f, cy + h2 * 0.15f, z2 - trail * 0.05f, 0.55f, 0.04f, 0.85f, a1 * 0.4f, 0.0f);
        }
    }

    private static void wakeStrip(Matrix4f matrix, VertexConsumer consumer,
                                  float x1, float y1, float z1, float x2, float y2, float z2,
                                  float x3, float y3, float z3, float x4, float y4, float z4,
                                  float r, float g, float b, float a1, float a2) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
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
        int segments = 32;
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
