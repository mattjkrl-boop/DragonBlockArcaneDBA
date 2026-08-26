package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.ZShockwaveEntity;
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
 * Entity Renderer for Z-Sword Massive Kinetic Shockwave in Minecraft 26.2.
 * Renders a colossal, towering 3D volumetric golden crescent blade model with multi-tier incandescent
 * pressure crests, transverse geometric kinetic ribs, trailing swept-back wake ribbons, and solar apex core.
 */
public class ZShockwaveRenderer extends EntityRenderer<ZShockwaveEntity, ZShockwaveRenderer.ZShockwaveRenderState> {
    public ZShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ZShockwaveRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public boolean isSubWave = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public long seed = 0;
        public boolean isFirstPersonOwner = false;
    }

    @Override
    public boolean shouldRender(ZShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public ZShockwaveRenderState createRenderState() {
        return new ZShockwaveRenderState();
    }

    @Override
    public void extractRenderState(ZShockwaveEntity entity, ZShockwaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.isSubWave = entity.isSubWave();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(ZShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        float charge = state.chargeRatio;
        boolean sub = state.isSubWave;
        float age = state.age;

        float fpScale = (state.isFirstPersonOwner && state.age < 6.0f) ? (0.55f + (state.age / 6.0f) * 0.45f) : 1.0f;
        float span = (sub ? (3.2f + charge * 3.2f) : (4.8f + charge * 6.2f)) * fpScale;
        float chord = (sub ? (1.0f + charge * 0.6f) : (1.6f + charge * 1.4f)) * fpScale;
        float height = (sub ? (0.9f + charge * 0.8f) : (1.5f + charge * 1.8f)) * fpScale;
        float thickness = (sub ? (0.22f + charge * 0.15f) : (0.38f + charge * 0.32f)) * fpScale;
        int segments = 28;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Radiant Outer Golden-Amber Volumetric Sheath (Expansive Aura)
            drawVolumetricCrescent(matrix, buffer, span + 0.6f, chord + 0.5f, height + 0.4f, thickness + 0.20f,
                1.4f, segments, 1.0f, 0.70f, 0.08f, 0.75f, 0.0f);

            // 2. Main Towering 3D Divine Golden Blade Body (Solid Physical Presence)
            drawVolumetricCrescent(matrix, buffer, span, chord, height, thickness,
                1.0f, segments, 1.0f, 0.88f, 0.20f, 0.95f, 0.85f);

            // 3. High-Pressure Incandescent White-Gold Leading Cutting Spine
            drawVolumetricCrescent(matrix, buffer, span * 0.92f, chord * 0.96f, height * 0.65f, thickness * 0.45f,
                0.5f, segments, 1.0f, 1.0f, 0.92f, 1.0f, 1.0f);

            // 4. Transverse Geometric Kinetic Shock Ribs (Hypersonic shockwave struts)
            int ribCount = sub ? 8 : (12 + (int) (charge * 6));
            for (int r = 1; r < ribCount; r++) {
                float t = (r / (float) ribCount) * 2.0f - 1.0f;
                if (Math.abs(t) < 0.1f) continue; // Skip center nose

                float rx = t * span;
                float rz = (1.0f - t * t) * chord;
                float rH = height * (1.0f - Math.abs(t) * 0.4f);
                float rTrail = (1.2f + charge * 0.8f) * (1.0f - Math.abs(t) * 0.6f);

                drawShockRib(matrix, buffer, rx, rz, rH, rTrail, 0.14f,
                    1.0f, 0.95f, 0.50f, 0.90f);
            }

            // 5. Swept-Back 3D Kinetic Wake Flow Ribbons (Layered atmospheric displacement)
            int ribbonLayers = sub ? 2 : 4;
            for (int l = 0; l < ribbonLayers; l++) {
                float layerOffset = (l + 1) * 0.55f;
                float lAlpha = (1.0f - (l / (float) ribbonLayers)) * 0.60f;
                drawWakeRibbon(matrix, buffer, span * 0.95f, chord, height * 0.8f, layerOffset, segments,
                    1.0f, 0.80f, 0.15f, lAlpha);
            }

            // 6. Central Solar Apex Core & Leading Kinetic Compression Rings
            float coreZ = chord + 0.15f;
            drawApexSolarCore(matrix, buffer, 0, 0, coreZ, 0.45f + charge * 0.35f,
                1.0f, 1.0f, 0.95f, 1.0f);

            // Rotating front compression rings
            drawFrontCompressionRing(matrix, buffer, 0, 0, coreZ + 0.25f, 0.65f + charge * 0.45f, 0.08f, 20, age * 24.0f,
                1.0f, 0.90f, 0.30f, 0.90f);
            drawFrontCompressionRing(matrix, buffer, 0, 0, coreZ + 0.50f, 0.45f + charge * 0.35f, 0.06f, 16, age * -32.0f,
                1.0f, 1.0f, 0.85f, 0.95f);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /**
     * Renders a fully closed 3D volumetric crescent geometry with top, bottom, front bevels, and tapered trail.
     */
    private static void drawVolumetricCrescent(Matrix4f matrix, VertexConsumer consumer,
                                               float span, float chord, float height, float thickness, float trailLength,
                                               int segments, float r, float g, float b, float aLead, float aTrail) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;

            float x1 = t1 * span;
            float z1 = (1.0f - t1 * t1) * chord;
            float x2 = t2 * span;
            float z2 = (1.0f - t2 * t2) * chord;

            // Height and thickness falloff towards crescent tips
            float factor1 = 1.0f - (float) Math.pow(Math.abs(t1), 1.8);
            float factor2 = 1.0f - (float) Math.pow(Math.abs(t2), 1.8);

            float h1 = height * factor1;
            float h2 = height * factor2;
            float th1 = thickness * factor1;
            float th2 = thickness * factor2;

            float z1Trail = z1 - trailLength * factor1;
            float z2Trail = z2 - trailLength * factor2;

            // 1. Upper Beveled Face (Crest)
            consumer.addVertex(matrix, x1, 0, z1).setColor(r, g, b, aLead).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, 0, z2).setColor(r, g, b, aLead).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, h2, z2 - th2).setColor(r, g, b, aLead * 0.9f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, h1, z1 - th1).setColor(r, g, b, aLead * 0.9f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // 2. Lower Beveled Face (Keel)
            consumer.addVertex(matrix, x1, -h1, z1 - th1).setColor(r, g, b, aLead * 0.9f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -h2, z2 - th2).setColor(r, g, b, aLead * 0.9f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, 0, z2).setColor(r, g, b, aLead).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, 0, z1).setColor(r, g, b, aLead).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

            // 3. Upper Trailing Slope (Back toward trail)
            consumer.addVertex(matrix, x1, h1, z1 - th1).setColor(r, g, b, aLead * 0.8f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, h2, z2 - th2).setColor(r, g, b, aLead * 0.8f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, 0, z2Trail).setColor(r, g, b, aTrail).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, 0, z1Trail).setColor(r, g, b, aTrail).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // 4. Lower Trailing Slope
            consumer.addVertex(matrix, x1, 0, z1Trail).setColor(r, g, b, aTrail).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, 0, z2Trail).setColor(r, g, b, aTrail).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -h2, z2 - th2).setColor(r, g, b, aLead * 0.8f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, -h1, z1 - th1).setColor(r, g, b, aLead * 0.8f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }

    private static void drawShockRib(Matrix4f matrix, VertexConsumer consumer, float rx, float rz, float height, float trail, float width,
                                     float r, float g, float b, float a) {
        float halfW = width * 0.5f;

        // Vertical Rib Blade
        consumer.addVertex(matrix, rx - halfW, -height, rz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, rx + halfW, -height, rz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, rx + halfW, height, rz - trail * 0.4f).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, rx - halfW, height, rz - trail * 0.4f).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Backward Taper
        consumer.addVertex(matrix, rx - halfW, height, rz - trail * 0.4f).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, rx + halfW, height, rz - trail * 0.4f).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, rx + halfW, 0, rz - trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, rx - halfW, 0, rz - trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawWakeRibbon(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float height, float offsetZ, int segments,
                                       float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;

            float x1 = t1 * span;
            float z1 = (1.0f - t1 * t1) * chord - offsetZ;
            float x2 = t2 * span;
            float z2 = (1.0f - t2 * t2) * chord - offsetZ;

            float h1 = height * (1.0f - Math.abs(t1) * 0.8f);
            float h2 = height * (1.0f - Math.abs(t2) * 0.8f);

            consumer.addVertex(matrix, x1, -h1 * 0.5f, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, -h2 * 0.5f, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, h2 * 0.5f, z2).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, h1 * 0.5f, z1).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawApexSolarCore(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float radius,
                                          float r, float g, float b, float a) {
        float rHalf = radius * 0.6f;
        // Diamond 3D core
        consumer.addVertex(matrix, cx - rHalf, cy, cz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx + rHalf, cy, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy + radius, cz).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy + radius, cz).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        consumer.addVertex(matrix, cx + rHalf, cy, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, cx - rHalf, cy, cz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, cx, cy - radius, cz).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, cx, cy - radius, cz).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }

    private static void drawFrontCompressionRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float radius, float thickness, int segments, float rotDeg,
                                                 float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = rotRad + (i / (double) segments) * Math.PI * 2.0;
            double a2 = rotRad + ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1Out = cx + (float) Math.cos(a1) * radius;
            float y1Out = cy + (float) Math.sin(a1) * radius;
            float x2Out = cx + (float) Math.cos(a2) * radius;
            float y2Out = cy + (float) Math.sin(a2) * radius;

            float x1In = cx + (float) Math.cos(a1) * (radius - thickness);
            float y1In = cy + (float) Math.sin(a1) * (radius - thickness);
            float x2In = cx + (float) Math.cos(a2) * (radius - thickness);
            float y2In = cy + (float) Math.sin(a2) * (radius - thickness);

            consumer.addVertex(matrix, x1Out, y1Out, cz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2Out, y2Out, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2In, y2In, cz).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1In, y1In, cz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }
}
