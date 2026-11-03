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

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ZStanceAuraEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ZStanceAuraEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
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
            int groundSegments = 48;
            drawRotatingRing(matrix, buffer, 0, 0.04f, 0, baseRadius * 1.05f, baseRadius * 0.92f, groundSegments, age * 6.0f,
                0.40f, 0.10f, 0.80f, baseAlpha * 0.85f);
            drawRotatingRing(matrix, buffer, 0, 0.03f, 0, baseRadius * 0.92f, baseRadius * 0.80f, groundSegments, age * -8.0f,
                0.20f, 0.05f, 0.45f, baseAlpha * 0.70f);
            // Dense Inner Singularity Disc (Pitch Black & Intense Violet rim)
            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, 0.75f, 0.0f, 20, age * 24.0f,
                0.10f, 0.0f, 0.25f, baseAlpha * 0.95f);
            drawRotatingRing(matrix, buffer, 0, 0.09f, 0, 0.42f, 0.26f, 24, -age * 30.0f,
                0.85f, 0.75f, 1.0f, baseAlpha);

            // 2. Inward-Contracting 3D Dimensional Event Horizon Rings (Simulating violent gravity suction)
            int contractingRings = 4;
            for (int k = 0; k < contractingRings; k++) {
                float ringPhase = (age * 0.35f + k * (baseRadius / contractingRings)) % baseRadius;
                float currentR = baseRadius - ringPhase;
                if (currentR > 0.3f && currentR < baseRadius) {
                    float fade = (currentR / baseRadius) * baseAlpha * 0.85f;
                    float ringY = 0.06f + (1.0f - currentR / baseRadius) * 0.8f;
                    drawRotatingRing(matrix, buffer, 0, ringY, 0, currentR, Math.max(0.1f, currentR - 0.25f), 36, age * 12.0f + k * 45.0f,
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

                int segs = 28;
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

        // Additive halo pass: the singularity eye and the outer ground ring
        // get a faint bloom, and a soft violet disc anchors the field to the
        // ground under the caster.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, 0.95f, 0.0f, 28, age * 24.0f,
                0.55f, 0.20f, 0.95f, baseAlpha * 0.22f);
            drawRotatingRing(matrix, buffer, 0, 0.04f, 0, baseRadius * 1.16f, baseRadius * 1.02f, 48, age * 6.0f,
                0.40f, 0.10f, 0.80f, baseAlpha * 0.12f);
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
        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        float mz = (z1 + z2) * 0.5f;
        float hx = (x2 - x1) * 0.5f;
        float hy = (y2 - y1) * 0.5f;
        float hz = (z2 - z1) * 0.5f;
        float dx = tipX - mx;
        float dy = tipY - my;
        float dz = tipZ - mz;
        float bx = hy * dz - hz * dy;
        float by = hz * dx - hx * dz;
        float bz = hx * dy - hy * dx;
        float bl = (float) Math.sqrt(bx * bx + by * by + bz * bz);
        if (bl > 0.001f) {
            bx /= bl;
            by /= bl;
            bz /= bl;
        }
        float bowAmp = (float) Math.sqrt(hx * hx + hy * hy + hz * hz) * 0.7f;
        float cr = Math.max(rBase, rTip);
        float cg = Math.max(gBase, gTip);
        float cb = Math.max(bBase, bTip);
        int steps = 6;
        for (int i = 0; i < steps; i++) {
            float t1 = i / (float) steps;
            float t2 = (i + 1) / (float) steps;
            float bow1 = (float) Math.sin(t1 * Math.PI) * bowAmp;
            float bow2 = (float) Math.sin(t2 * Math.PI) * bowAmp;
            float w1 = 1.0f - t1 * 0.78f;
            float w2 = 1.0f - t2 * 0.78f;
            float ax = mx + dx * t1 + bx * bow1;
            float ay = my + dy * t1 + by * bow1;
            float az = mz + dz * t1 + bz * bow1;
            float cx = mx + dx * t2 + bx * bow2;
            float cy = my + dy * t2 + by * bow2;
            float cz = mz + dz * t2 + bz * bow2;
            float ar = rBase + (rTip - rBase) * t1;
            float ag = gBase + (gTip - gBase) * t1;
            float ab = bBase + (bTip - bBase) * t1;
            float aa = aBase + (aTip - aBase) * t1;
            float br = rBase + (rTip - rBase) * t2;
            float bg = gBase + (gTip - gBase) * t2;
            float bb = bBase + (bTip - bBase) * t2;
            float ba = aBase + (aTip - aBase) * t2;
            consumer.addVertex(matrix, ax - hx * w1, ay - hy * w1, az - hz * w1).setColor(ar, ag, ab, aa).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ax + hx * w1, ay + hy * w1, az + hz * w1).setColor(ar, ag, ab, aa).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx + hx * w2, cy + hy * w2, cz + hz * w2).setColor(br, bg, bb, ba).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx - hx * w2, cy - hy * w2, cz - hz * w2).setColor(br, bg, bb, ba).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            float k1 = w1 * 0.22f;
            float k2 = w2 * 0.22f;
            consumer.addVertex(matrix, ax - hx * k1, ay - hy * k1, az - hz * k1).setColor(cr, cg, cb, aa).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ax + hx * k1, ay + hy * k1, az + hz * k1).setColor(cr, cg, cb, aa).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx + hx * k2, cy + hy * k2, cz + hz * k2).setColor(cr, cg, cb, ba).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx - hx * k2, cy - hy * k2, cz - hz * k2).setColor(cr, cg, cb, ba).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase * 0.6f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase * 0.6f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, mx - dx * 0.16f + hx * 0.2f, my - dy * 0.16f, mz - dz * 0.16f + hz * 0.2f).setColor(rBase, gBase, bBase, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, mx - dx * 0.16f - hx * 0.2f, my - dy * 0.16f, mz - dz * 0.16f - hz * 0.2f).setColor(rBase, gBase, bBase, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawRuneDiamond(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float r, float g, float b, float a) {
        int arms = 4;
        int steps = 3;
        for (int arm = 0; arm < arms; arm++) {
            float ang = (arm / (float) arms) * (float) Math.PI * 2.0f;
            float dx = (float) Math.cos(ang);
            float dz = (float) Math.sin(ang);
            float px = -dz;
            float pz = dx;
            for (int i = 0; i < steps; i++) {
                float t1 = i / (float) steps;
                float t2 = (i + 1) / (float) steps;
                float bow1 = (float) Math.sin(t1 * Math.PI) * size * 0.22f;
                float bow2 = (float) Math.sin(t2 * Math.PI) * size * 0.22f;
                float d1 = size * (0.2f + 0.9f * t1);
                float d2 = size * (0.2f + 0.9f * t2);
                float w1 = size * 0.18f * (1.0f - t1);
                float w2 = size * 0.18f * (1.0f - t2);
                float x1 = cx + dx * d1 + px * bow1;
                float z1 = cz + dz * d1 + pz * bow1;
                float x2 = cx + dx * d2 + px * bow2;
                float z2 = cz + dz * d2 + pz * bow2;
                float a1 = a * (1.0f - t1 * 0.2f);
                float a2 = a * (1.0f - t2);
                consumer.addVertex(matrix, x1 - px * w1, cy, z1 - pz * w1).setColor(r, g, b, a1 * 0.45f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1 + px * w1, cy, z1 + pz * w1).setColor(r, g, b, a1 * 0.45f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 + px * w2, cy, z2 + pz * w2).setColor(r, g, b, a2 * 0.08f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 - px * w2, cy, z2 - pz * w2).setColor(r, g, b, a2 * 0.08f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1 - px * w1 * 0.28f, cy + 0.02f, z1 - pz * w1 * 0.28f).setColor(1.0f, 0.95f, 0.7f, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1 + px * w1 * 0.28f, cy + 0.02f, z1 + pz * w1 * 0.28f).setColor(1.0f, 0.95f, 0.7f, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 + px * w2 * 0.28f, cy + 0.02f, z2 + pz * w2 * 0.28f).setColor(1.0f, 0.95f, 0.7f, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 - px * w2 * 0.28f, cy + 0.02f, z2 - pz * w2 * 0.28f).setColor(1.0f, 0.95f, 0.7f, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            }
        }
    }

    private static void drawRibbonSegment(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float width, float r, float g, float b, float a) {
        float edge = width * 0.5f;
        float core = width * 0.16f;
        consumer.addVertex(matrix, x1, y1 - edge * 1.35f, z1).setColor(r, g, b, a * 0.22f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1 + edge * 1.35f, z1).setColor(r, g, b, a * 0.22f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2 + edge * 0.85f, z2).setColor(r, g, b, a * 0.28f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2 - edge * 0.85f, z2).setColor(r, g, b, a * 0.28f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1 - edge, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1 + edge, z1).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2 + edge * 0.9f, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2 - edge * 0.9f, z2).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1 - core, z1).setColor(1.0f, 0.9f, 0.45f, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1 + core, z1).setColor(1.0f, 0.9f, 0.45f, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2 + core * 0.7f, z2).setColor(1.0f, 0.9f, 0.45f, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2 - core * 0.7f, z2).setColor(1.0f, 0.9f, 0.45f, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
