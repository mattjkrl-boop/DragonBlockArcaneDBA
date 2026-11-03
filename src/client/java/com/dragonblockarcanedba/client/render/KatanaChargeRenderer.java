package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.KatanaChargeEntity;
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
 * Entity Renderer for Katana Flashdraw Charge-up in Minecraft 26.2.
 * Renders physical 3D iaido drawing stance ground focus rings, sheathed hilt spark matrix,
 * and converging crystalline silver motes.
 */
public class KatanaChargeRenderer extends EntityRenderer<KatanaChargeEntity, KatanaChargeRenderer.KatanaChargeRenderState> {

    public KatanaChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class KatanaChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(KatanaChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(KatanaChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public KatanaChargeRenderState createRenderState() {
        return new KatanaChargeRenderState();
    }

    @Override
    public void extractRenderState(KatanaChargeEntity entity, KatanaChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(KatanaChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float charge = state.chargeRatio;
        float age = state.age;
        float pulse = 0.85f + 0.15f * (float) Math.sin(age * 0.4f);
        // Spawn fade-in over the first 4 ticks so the charge construct does not
        // pop in at full brightness.
        float alpha = (0.65f + (charge * 0.35f)) * Math.min(1.0f, age / 4.0f);

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Planar 3D Ground Iaido Focus Rings
            int segments = 36;
            float rOuter = 1.3f + (charge * 0.4f);
            // Outer Ring: Electric Cyan
            drawRotatingRing(matrix, buffer, 0, 0.03f, 0, rOuter, rOuter * 0.88f, segments, age * 25.0f,
                0.15f, 0.85f, 1.0f, alpha * 0.75f * pulse);
            // Inner Core Ring: Radiant Silver-White
            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, rOuter * 0.65f, rOuter * 0.52f, segments, -age * 35.0f,
                0.90f, 0.95f, 1.0f, alpha * 0.90f * pulse);
            float coreR = rOuter * (0.22f + charge * 0.08f);
            drawRotatingRing(matrix, buffer, 0, 0.08f, 0, coreR, coreR * 0.45f, segments, age * 48.0f,
                1.0f, 1.0f, 1.0f, alpha * pulse);
            int arcSeg = 16;
            float arcSpan = 0.7f + charge * 0.45f;
            float arcChord = 0.28f + charge * 0.22f;
            for (int i = 0; i < arcSeg; i++) {
                float t1 = (i / (float) arcSeg) * 2.0f - 1.0f;
                float t2 = ((i + 1) / (float) arcSeg) * 2.0f - 1.0f;
                float x1 = t1 * arcSpan;
                float z1 = 0.45f + (1.0f - t1 * t1) * arcChord;
                float x2 = t2 * arcSpan;
                float z2 = 0.45f + (1.0f - t2 * t2) * arcChord;
                float taper1 = 1.0f - Math.abs(t1) * 0.8f;
                float taper2 = 1.0f - Math.abs(t2) * 0.8f;
                drawQuad(matrix, buffer, x1, 0.06f, z1, x2, 0.06f, z2, x2, 0.06f, z2 - 0.18f * taper2, x1, 0.06f, z1 - 0.18f * taper1, 0.86f, 0.95f, 1.0f, alpha * 0.8f * taper1);
                drawQuad(matrix, buffer, x1, 0.075f, z1, x2, 0.075f, z2, x2, 0.075f, z2 - 0.028f * taper2, x1, 0.075f, z1 - 0.028f * taper1, 1.0f, 1.0f, 1.0f, alpha * taper1);
            }

            // 2. Sheathed Hilt Spark Matrix (Left hip drawing position: x = -0.3, y = 0.75, z = -0.05)
            float hiltX = -0.28f;
            float hiltY = 0.75f;
            float hiltZ = -0.05f;
            float sparkSize = (0.12f + charge * 0.10f) * pulse;
            drawDiamondSpark(matrix, buffer, hiltX, hiltY, hiltZ, sparkSize, 1.0f, 1.0f, 1.0f, alpha);
            drawDiamondSpark(matrix, buffer, hiltX, hiltY, hiltZ, sparkSize * 0.6f, 0.2f, 0.9f, 1.0f, alpha * 0.8f);

            // 3. Converging Inward Crystalline Silver Motes
            int moteCount = 10 + (int) (charge * 8);
            for (int m = 0; m < moteCount; m++) {
                float moteProg = (age * 0.06f + (m / (float) moteCount)) % 1.0f;
                float inwardProg = 1.0f - moteProg; // Moves from outside in towards hilt
                double ang = (m * 2.39996) + (age * 0.08f); // Golden ratio angle spread
                float dist = 0.2f + inwardProg * (1.2f + charge * 0.5f);

                float mx = hiltX + (float) Math.cos(ang) * dist;
                float my = hiltY + ((rng.nextFloat() - 0.5f) * 0.6f * inwardProg);
                float mz = hiltZ + (float) Math.sin(ang) * dist;
                float mSize = 0.04f * (0.4f + moteProg * 0.6f);
                float mAlpha = alpha * moteProg;

                drawDiamondSpark(matrix, buffer, mx, my, mz, mSize, 0.8f, 0.95f, 1.0f, mAlpha);
            }
        });

        // Additive halo pass: a wider, faint copy of the outer cyan ring and
        // the hilt spark so the charge blooms and anchors to the ground
        // instead of reading matte.
        float haloRadius = (1.3f + (charge * 0.4f)) * 1.15f;
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            drawRotatingRing(matrix, buffer, 0, 0.03f, 0, haloRadius, haloRadius * 0.82f, 36, age * 25.0f,
                0.15f, 0.85f, 1.0f, 0.16f * alpha);
            drawDiamondSpark(matrix, buffer, -0.28f, 0.75f, -0.05f, (0.12f + charge * 0.10f) * 1.7f, 1.0f, 1.0f, 1.0f, 0.20f * alpha);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawRotatingRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2.0) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2.0) + rotRad;

            float x1Out = cx + (float) Math.cos(a1) * rOuter;
            float z1Out = cz + (float) Math.sin(a1) * rOuter;
            float x2Out = cx + (float) Math.cos(a2) * rOuter;
            float z2Out = cz + (float) Math.sin(a2) * rOuter;

            float x1In = cx + (float) Math.cos(a1) * rInner;
            float z1In = cz + (float) Math.sin(a1) * rInner;
            float x2In = cx + (float) Math.cos(a2) * rInner;
            float z2In = cz + (float) Math.sin(a2) * rInner;

            drawQuad(matrix, consumer, x1In, cy, z1In, x2In, cy, z2In, x2Out, cy, z2Out, x1Out, cy, z1Out, r, g, b, a);
        }
    }

    private static void drawDiamondSpark(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float r, float g, float b, float a) {
        float h = size * 1.4f;
        float w = size * 0.5f;

        consumer.addVertex(matrix, cx - w, cy, cz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx + w, cy, cz).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        consumer.addVertex(matrix, cx, cy, cz - w).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy, cz + w).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        int arms = 4;
        int steps = 3;
        for (int arm = 0; arm < arms; arm++) {
            float ang = arm * ((float) Math.PI * 0.5f) + 0.35f;
            float dx = (float) Math.cos(ang);
            float dz = (float) Math.sin(ang);
            float px = -dz;
            float pz = dx;
            for (int i = 0; i < steps; i++) {
                float t1 = i / (float) steps;
                float t2 = (i + 1) / (float) steps;
                float bow1 = (float) Math.sin(t1 * Math.PI) * size * 0.45f;
                float bow2 = (float) Math.sin(t2 * Math.PI) * size * 0.45f;
                float d1 = size * (0.35f + 1.15f * t1);
                float d2 = size * (0.35f + 1.15f * t2);
                float w1 = size * 0.28f * (1.0f - t1);
                float w2 = size * 0.28f * (1.0f - t2);
                float x1 = cx + dx * d1 + px * bow1;
                float z1 = cz + dz * d1 + pz * bow1;
                float x2 = cx + dx * d2 + px * bow2;
                float z2 = cz + dz * d2 + pz * bow2;
                float a1 = a * (1.0f - t1 * 0.25f);
                float a2 = a * (1.0f - t2);
                consumer.addVertex(matrix, x1 - px * w1, cy, z1 - pz * w1).setColor(r, g, b, a1 * 0.4f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1 + px * w1, cy, z1 + pz * w1).setColor(r, g, b, a1 * 0.4f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 + px * w2, cy, z2 + pz * w2).setColor(r, g, b, a2 * 0.08f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 - px * w2, cy, z2 - pz * w2).setColor(r, g, b, a2 * 0.08f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1 - px * w1 * 0.3f, cy, z1 - pz * w1 * 0.3f).setColor(1.0f, 1.0f, 1.0f, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1 + px * w1 * 0.3f, cy, z1 + pz * w1 * 0.3f).setColor(1.0f, 1.0f, 1.0f, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 + px * w2 * 0.3f, cy, z2 + pz * w2 * 0.3f).setColor(1.0f, 1.0f, 1.0f, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 - px * w2 * 0.3f, cy, z2 - pz * w2 * 0.3f).setColor(1.0f, 1.0f, 1.0f, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            }
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
