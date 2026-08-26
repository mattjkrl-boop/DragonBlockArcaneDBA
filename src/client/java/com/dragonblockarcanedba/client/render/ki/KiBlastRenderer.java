package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.entity.KiBlastEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Entity Renderer for Ki Blast / Energy Laser Bolts in Minecraft 26.2.
 * Renders physical 3D aerodynamic energy laser geometry:
 * - Directionally aligned with entity flight pitch and yaw.
 * - Multi-faceted diamond-beveled white-hot piercing energy core.
 * - Radiant outer plasma sheath with aerodynamic nose cone and tapered trailing energy wake.
 * - Orbiting helical energy vortex ribbons and resonance rings.
 */
public class KiBlastRenderer extends EntityRenderer<KiBlastEntity, KiBlastRenderer.BlastRenderState> {

    public KiBlastRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BlastRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float ageInTicks = 0;
        public float yRot = 0;
        public float xRot = 0;
    }

    @Override
    public BlastRenderState createRenderState() {
        return new BlastRenderState();
    }

    @Override
    public void extractRenderState(KiBlastEntity entity, BlastRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
    }

    @Override
    public void submit(BlastRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float r = KiRenderHelper.red(state.color);
        float g = KiRenderHelper.green(state.color);
        float b = KiRenderHelper.blue(state.color);
        RenderType renderType = KiRenderHelper.kiRenderType();

        float pulse = 1.0f + 0.12f * Mth.sin(state.ageInTicks * 0.9f);

        poseStack.pushPose();
        // Align with projectile trajectory
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        // Spin along flight axis
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.ageInTicks * 30.0f));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            float radius = 0.20f * pulse;
            float headLen = 0.75f;
            float tailLen = 1.6f;

            // 1. Outer Aerodynamic Plasma Sheath (8-sided tapered cylinder with nose cone and wake tail)
            drawLaserBoltBody(matrix, buffer, radius, headLen, tailLen, 8, r, g, b, 0.85f);

            // 2. White-Hot Intense Piercing Energy Core Spindle
            drawLaserBoltBody(matrix, buffer, radius * 0.45f, headLen * 0.95f, tailLen * 0.65f, 6, 1.0f, 1.0f, 0.95f, 1.0f);

            // 3. Orbiting Helical Energy Wake Ribbons
            drawHelicalVortex(matrix, buffer, radius * 1.35f, tailLen, state.ageInTicks, r, g, b);

            // 4. Perpendicular Energy Resonance Ring
            drawResonanceRing(matrix, buffer, 0, 0, 0, radius * 1.5f, radius * 1.1f, 12, r, g, b, 0.75f);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawLaserBoltBody(Matrix4f matrix, VertexConsumer consumer,
                                         float radius, float headLen, float tailLen, int sides,
                                         float r, float g, float b, float a) {
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2;

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            // Forward Cone (Piercing Nose)
            consumer.addVertex(matrix, 0, 0, headLen).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, a).setUv(1, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, a).setUv(0, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, 0, 0, headLen).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);

            // Backward Tracer Tail (Tapered to trailing point with alpha fade)
            consumer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, a).setUv(0, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, a).setUv(1, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, 0, 0, -tailLen).setColor(r, g, b, 0.0f).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, 0, 0, -tailLen).setColor(r, g, b, 0.0f).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
        }
    }

    private static void drawHelicalVortex(Matrix4f matrix, VertexConsumer consumer, float helixRadius, float length, float age, float r, float g, float b) {
        int steps = 10;
        for (int strand = 0; strand < 2; strand++) {
            double strandOffset = strand * Math.PI;
            for (int i = 0; i < steps; i++) {
                float p1 = i / (float) steps;
                float p2 = (i + 1) / (float) steps;

                float z1 = 0.2f - (p1 * (length + 0.2f));
                float z2 = 0.2f - (p2 * (length + 0.2f));

                float curR1 = helixRadius * (1.0f - p1 * 0.4f);
                float curR2 = helixRadius * (1.0f - p2 * 0.4f);

                double a1 = strandOffset + age * 0.5 + (p1 * Math.PI * 3.0);
                double a2 = strandOffset + age * 0.5 + (p2 * Math.PI * 3.0);

                float x1 = (float) Math.cos(a1) * curR1;
                float y1 = (float) Math.sin(a1) * curR1;
                float x2 = (float) Math.cos(a2) * curR2;
                float y2 = (float) Math.sin(a2) * curR2;

                float alpha = (1.0f - p1) * 0.70f;

                drawRibbonSegment(matrix, consumer, x1, y1, z1, x2, y2, z2, 0.04f, r, g, b, alpha);
            }
        }
    }

    private static void drawRibbonSegment(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float width, float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) return;

        float nx = -dy / len * width * 0.5f;
        float ny = dx / len * width * 0.5f;

        consumer.addVertex(matrix, x1 - nx, y1 - ny, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x1 + nx, y1 + ny, z1).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2 + nx, y2 + ny, z2).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2 - nx, y2 - ny, z2).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }

    private static void drawResonanceRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2;

            float x1 = cx + (float) Math.cos(a1) * rOuter;
            float y1 = cy + (float) Math.sin(a1) * rOuter;
            float x2 = cx + (float) Math.cos(a2) * rOuter;
            float y2 = cy + (float) Math.sin(a2) * rOuter;

            float ix1 = cx + (float) Math.cos(a1) * rInner;
            float iy1 = cy + (float) Math.sin(a1) * rInner;
            float ix2 = cx + (float) Math.cos(a2) * rInner;
            float iy2 = cy + (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, ix1, iy1, cz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, ix2, iy2, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, y2, cz).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, y1, cz).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }
}
