package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.ZStanceAuraEntity;
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
 * Entity Renderer for Z-Sword Katchin Weight & Gravity Stance in Minecraft 26.2.
 * Renders a physical 3D gravity suction field, 8 towering vibrating Katchin weight monoliths,
 * inward-contracting 3D event horizon rings, and dense gravitational singularity core.
 */
public class ZStanceAuraRenderer extends EntityRenderer<ZStanceAuraEntity, ZStanceAuraRenderer.ZStanceRenderState> {

    public ZStanceAuraRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ZStanceRenderState extends EntityRenderState {
        public int heldTicks = 0;
        public float powerRatio = 0.0f;
        public float age = 0;
        public long seed = 0;
    }

    @Override
    public boolean shouldRender(ZStanceAuraEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public ZStanceRenderState createRenderState() {
        return new ZStanceRenderState();
    }

    @Override
    public void extractRenderState(ZStanceAuraEntity entity, ZStanceRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.heldTicks = entity.getHeldTicks();
        state.powerRatio = entity.getPowerRatio();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(ZStanceRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float power = state.powerRatio;
        float age = state.age;

        float baseRadius = 2.0f + (power * 3.5f);
        float baseAlpha = 0.70f + (power * 0.28f);
        float pulse = 0.94f + 0.06f * (float) Math.sin(age * 0.40f);
        float tremble = (float) Math.sin(age * 3.2f) * (0.015f + power * 0.05f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Gravitational Compression Ground Disc & Singularity Eye
            int groundSegments = 36;
            drawRotatingRing(matrix, buffer, 0, 0.04f, 0, baseRadius * 1.05f, baseRadius * 0.92f, groundSegments, age * 6.0f,
                0.40f, 0.10f, 0.80f, baseAlpha * 0.85f);
            drawRotatingRing(matrix, buffer, 0, 0.03f, 0, baseRadius * 0.92f, baseRadius * 0.80f, groundSegments, age * -8.0f,
                0.20f, 0.05f, 0.45f, baseAlpha * 0.70f);
            // Dense Inner Singularity Disc (Pitch Black & Intense Violet rim)
            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, 0.75f, 0.0f, 20, age * 24.0f,
                0.10f, 0.0f, 0.25f, baseAlpha * 0.95f);

            // 2. Inward-Contracting 3D Dimensional Event Horizon Rings (Simulating violent gravity suction)
            int contractingRings = 4;
            for (int k = 0; k < contractingRings; k++) {
                float ringPhase = (age * 0.35f + k * (baseRadius / contractingRings)) % baseRadius;
                float currentR = baseRadius - ringPhase;
                if (currentR > 0.3f && currentR < baseRadius) {
                    float fade = (currentR / baseRadius) * baseAlpha * 0.85f;
                    float ringY = 0.06f + (1.0f - currentR / baseRadius) * 0.8f;
                    drawRotatingRing(matrix, buffer, 0, ringY, 0, currentR, Math.max(0.1f, currentR - 0.25f), 28, age * 12.0f + k * 45.0f,
                        0.55f, 0.15f, 0.95f, fade);
                }
            }

            // 3. Physical 3D Katchin Weight Monoliths / Divine Gravity Anchors
            int monolithCount = 6 + (int) (power * 2); // 6 to 8 monoliths
            for (int m = 0; m < monolithCount; m++) {
                double mAng = (m / (double) monolithCount) * Math.PI * 2.0 + Math.toRadians(age * 3.0f);
                float mDist = 1.6f + (power * 1.8f);

                float mx = (float) Math.cos(mAng) * mDist + (rng.nextFloat() - 0.5f) * tremble;
                float mz = (float) Math.sin(mAng) * mDist + (rng.nextFloat() - 0.5f) * tremble;

                float mHeight = (1.4f + power * 1.6f) * pulse;
                float mWidth = 0.35f + power * 0.15f;

                // Tangent vector for width
                float tx = -(float) Math.sin(mAng) * mWidth * 0.5f;
                float tz = (float) Math.cos(mAng) * mWidth * 0.5f;

                // Outward lean for heavy anchor feel
                float lean = 0.20f + power * 0.15f;
                float lx = (float) Math.cos(mAng) * lean;
                float lz = (float) Math.sin(mAng) * lean;

                // 3D Katchin Monolith Geometry (Dense metallic black body with violet runic core)
                drawMonolith(matrix, buffer,
                    mx - tx, 0.05f, mz - tz,
                    mx + tx, 0.05f, mz + tz,
                    mx + lx, mHeight, mz + lz,
                    0.15f, 0.08f, 0.30f, baseAlpha,
                    0.55f, 0.20f, 0.95f, baseAlpha * 0.90f
                );

                // Levitating Golden Gravity Runes above each anchor
                float runeY = mHeight + 0.25f + 0.1f * (float) Math.sin(age * 0.5f + m);
                drawRuneDiamond(matrix, buffer, mx + lx, runeY, mz + lz, 0.22f, 1.0f, 0.85f, 0.20f, baseAlpha * 0.95f);
            }

            // 4. Inward Dimensional Gravity Funnel (Inverted Suction Mesh)
            int funnelSteps = 16;
            for (int s = 0; s < funnelSteps; s++) {
                float p1 = s / (float) funnelSteps;
                float p2 = (s + 1) / (float) funnelSteps;

                float y1 = 0.1f + p1 * (3.0f + power * 2.0f);
                float y2 = 0.1f + p2 * (3.0f + power * 2.0f);

                float r1 = (0.3f + p1 * baseRadius) * pulse;
                float r2 = (0.3f + p2 * baseRadius) * pulse;

                int segs = 20;
                for (int i = 0; i < segs; i++) {
                    double a1 = (i / (double) segs) * Math.PI * 2.0 + (age * 0.12f + p1 * 1.5f);
                    double a2 = ((i + 1) / (double) segs) * Math.PI * 2.0 + (age * 0.12f + p1 * 1.5f);

                    float x1 = (float) Math.cos(a1) * r1;
                    float z1 = (float) Math.sin(a1) * r1;
                    float x2 = (float) Math.cos(a2) * r1;
                    float z2 = (float) Math.sin(a2) * r1;

                    float x3 = (float) Math.cos(a2) * r2;
                    float z3 = (float) Math.sin(a2) * r2;
                    float x4 = (float) Math.cos(a1) * r2;
                    float z4 = (float) Math.sin(a1) * r2;

                    float alphaStep = baseAlpha * 0.35f * (1.0f - p1 * 0.6f);
                    buffer.addVertex(matrix, x1, y1, z1).setColor(0.35f, 0.05f, 0.70f, alphaStep).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                    buffer.addVertex(matrix, x2, y1, z2).setColor(0.35f, 0.05f, 0.70f, alphaStep).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                    buffer.addVertex(matrix, x3, y2, z3).setColor(0.15f, 0.02f, 0.40f, alphaStep * 0.5f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                    buffer.addVertex(matrix, x4, y2, z4).setColor(0.15f, 0.02f, 0.40f, alphaStep * 0.5f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                }
            }

            // 5. Downward Spiraling Gravity Stream Ribbons
            int streamCount = 3;
            for (int st = 0; st < streamCount; st++) {
                float stOffset = st * (float) (Math.PI * 2.0 / streamCount);
                int rSteps = 14;
                for (int s = 0; s < rSteps; s++) {
                    float prog1 = s / (float) rSteps;
                    float prog2 = (s + 1) / (float) rSteps;

                    float sy1 = (1.0f - prog1) * (2.8f + power * 1.5f);
                    float sy2 = (1.0f - prog2) * (2.8f + power * 1.5f);

                    float sRad1 = (0.2f + prog1 * baseRadius * 0.8f);
                    float sRad2 = (0.2f + prog2 * baseRadius * 0.8f);

                    double sa1 = age * 0.25f + prog1 * Math.PI * 3.0 + stOffset;
                    double sa2 = age * 0.25f + prog2 * Math.PI * 3.0 + stOffset;

                    float sx1 = (float) Math.cos(sa1) * sRad1;
                    float sz1 = (float) Math.sin(sa1) * sRad1;
                    float sx2 = (float) Math.cos(sa2) * sRad2;
                    float sz2 = (float) Math.sin(sa2) * sRad2;

                    drawRibbonSegment(matrix, buffer, sx1, sy1, sz1, sx2, sy2, sz2, 0.14f,
                        1.0f, 0.80f, 0.20f, baseAlpha * 0.85f * (1.0f - prog1 * 0.4f));
                }
            }
        });

        super.submit(state, poseStack, collector, cameraState);
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

    private static void drawMonolith(Matrix4f matrix, VertexConsumer consumer,
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

    private static void drawRuneDiamond(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float r, float g, float b, float a) {
        float half = size * 0.5f;
        consumer.addVertex(matrix, cx, cy + half, cz).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx + half, cy, cz).setColor(r, g, b, a).setUv(1, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy - half, cz).setColor(r, g, b, a).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx - half, cy, cz).setColor(r, g, b, a).setUv(0, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawRibbonSegment(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float width, float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1 - width * 0.5f, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1 + width * 0.5f, z1).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2 + width * 0.5f, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2 - width * 0.5f, z2).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
