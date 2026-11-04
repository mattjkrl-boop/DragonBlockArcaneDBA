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

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ZShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ZShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
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
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        float charge = state.chargeRatio;
        boolean sub = state.isSubWave;
        float age = state.age;

        // Lifetime fade: the wave lives up to 80 ticks and used to vanish at full
        // opacity on discard. Fade in over the first 6 ticks, hold, then ease out
        // over the final 25 ticks.
        float fadeIn = smooth(Math.min(1.0f, age / 6.0f));
        float fadeOut = age <= 55.0f ? 1.0f : 1.0f - smooth((age - 55.0f) / 25.0f);
        float fade = fadeIn * Math.max(0.0f, fadeOut);
        float impact = strike(Math.min(1.0f, age / 18.0f));

        float fpScale = (state.isFirstPersonOwner && state.age < 6.0f) ? (0.55f + (state.age / 6.0f) * 0.45f) : 1.0f;
        // Mild growth across the fade in window: the crescent used to appear at
        // full 22 block span on its first tick. It now opens from 80 percent
        // while it leaves the caster; the damage band uses the full span, and
        // the core covers the small early gap at point blank range.
        float grow = 0.8f + 0.2f * fadeIn;
        float span = (sub ? (3.2f + charge * 3.2f) : (4.8f + charge * 6.2f)) * fpScale * grow;
        float chord = (sub ? (1.0f + charge * 0.6f) : (1.6f + charge * 1.4f)) * fpScale;
        float height = (sub ? (0.9f + charge * 0.8f) : (1.5f + charge * 1.8f)) * fpScale;
        float thickness = (sub ? (0.22f + charge * 0.15f) : (0.38f + charge * 0.32f)) * fpScale;
        float dist = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        int segments = com.dragonblockarcanedba.util.FxEnvelope.segments(36, dist, 16);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            float plate = thickness * 0.16f;
            drawVolumetricCrescent(matrix, buffer, span, chord, height, plate,
                0.85f, segments, 1.0f, 0.88f, 0.20f, 0.95f * fade, fade * 0.32f);
            drawVolumetricCrescent(matrix, buffer, span * 0.26f, chord * 0.94f, height * 0.78f, plate * 0.30f,
                0.20f, segments, 1.0f, 1.0f, 0.92f, fade, 0.0f);
            drawWakeRibbon(matrix, buffer, span * 0.92f, chord, height * 0.48f, 0.40f, segments,
                1.0f, 0.80f, 0.15f, fade * 0.40f);
            if (impact > 0.02f) {
                drawApexSolarCore(matrix, buffer, 0, 0, chord, (0.28f + charge * 0.16f) * impact,
                    1.0f, 1.0f, 0.95f, impact);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /**
     * Renders a fully closed 3D volumetric crescent geometry with top, bottom, front bevels, and tapered trail.
     */
    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    private static float strike(float progress) {
        if (progress >= 0.7f) return 0.0f;
        return (float) Math.sin((progress / 0.7f) * Math.PI);
    }

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
        int arms = 4;
        int steps = 3;
        for (int arm = 0; arm < arms; arm++) {
            float ang = arm * ((float) Math.PI * 0.5f);
            float dx = (float) Math.cos(ang);
            float dy = (float) Math.sin(ang);
            float px = -dy;
            float py = dx;
            for (int i = 0; i < steps; i++) {
                float t1 = i / (float) steps;
                float t2 = (i + 1) / (float) steps;
                float bow1 = (float) Math.sin(t1 * Math.PI) * radius * 0.35f;
                float bow2 = (float) Math.sin(t2 * Math.PI) * radius * 0.35f;
                float d1 = radius * (0.45f + 0.9f * t1);
                float d2 = radius * (0.45f + 0.9f * t2);
                float w1 = radius * 0.16f * (1.0f - t1);
                float w2 = radius * 0.16f * (1.0f - t2);
                float x1 = cx + dx * d1 + px * bow1;
                float y1 = cy + dy * d1 + py * bow1;
                float x2 = cx + dx * d2 + px * bow2;
                float y2 = cy + dy * d2 + py * bow2;
                consumer.addVertex(matrix, x1 - px * w1, y1 - py * w1, cz).setColor(r, g, b, a * (1.0f - t1)).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
                consumer.addVertex(matrix, x1 + px * w1, y1 + py * w1, cz).setColor(r, g, b, a * (1.0f - t1)).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
                consumer.addVertex(matrix, x2 + px * w2, y2 + py * w2, cz).setColor(1.0f, 0.95f, 0.7f, a * (1.0f - t2) * 0.15f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
                consumer.addVertex(matrix, x2 - px * w2, y2 - py * w2, cz).setColor(1.0f, 0.95f, 0.7f, a * (1.0f - t2) * 0.15f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            }
        }
    }

}
