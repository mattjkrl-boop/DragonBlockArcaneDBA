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

    @Override
    public boolean shouldRender(KatanaChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
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
        float alpha = 0.65f + (charge * 0.35f);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Planar 3D Ground Iaido Focus Rings
            int segments = 24;
            float rOuter = 1.3f + (charge * 0.4f);
            // Outer Ring: Electric Cyan
            drawRotatingRing(matrix, buffer, 0, 0.03f, 0, rOuter, rOuter * 0.88f, segments, age * 25.0f,
                0.15f, 0.85f, 1.0f, alpha * 0.75f * pulse);
            // Inner Core Ring: Radiant Silver-White
            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, rOuter * 0.65f, rOuter * 0.52f, segments, -age * 35.0f,
                0.90f, 0.95f, 1.0f, alpha * 0.90f * pulse);

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
