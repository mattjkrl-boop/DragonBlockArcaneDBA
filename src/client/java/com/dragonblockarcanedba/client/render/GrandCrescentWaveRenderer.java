package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.GrandCrescentWaveEntity;
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
 * Entity Renderer for Grand Crescent Wave in Minecraft 26.2.
 * Renders a tapered golden crescent blade: core, edge, wings, and a wake on the arc.
 * - Multi-layered volumetric 3D double-beveled crescent wave blade
 * - Radiant golden outer canopy, pure white-gold energy core, blinding razor cutting edge
 * - Upper and lower helical vortex wing ribbons
 * - Trailing secondary & tertiary 3D shockwave wave ripples
 * - Inscribed geometric valor runes along the arc
 */
public class GrandCrescentWaveRenderer extends EntityRenderer<GrandCrescentWaveEntity, GrandCrescentWaveRenderer.GrandCrescentWaveRenderState> {

    public GrandCrescentWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class GrandCrescentWaveRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
        public int hitTick = -1;
        public boolean isFirstPersonOwner = false;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(GrandCrescentWaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(GrandCrescentWaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public GrandCrescentWaveRenderState createRenderState() {
        return new GrandCrescentWaveRenderState();
    }

    @Override
    public void extractRenderState(GrandCrescentWaveEntity entity, GrandCrescentWaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.hitTick = entity.getHitTick();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(GrandCrescentWaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        // Lifecycle runs on the shared envelope: the crescent opens over its
        // first ticks, peaks, and dissipates across the last 35 percent of
        // its 80 tick life. A block strike starts a separate 12 tick fade
        // from the hit tick, so the wave never vanishes at full brightness.
        float maxLife = GrandCrescentWaveEntity.MAX_LIFE_TICKS;
        float progress = Math.min(1.0f, state.ageInTicks / maxLife);
        float fade = com.dragonblockarcanedba.util.FxEnvelope.envelope(
            state.ageInTicks, maxLife, 0.04f, 0.35f);
        if (state.hitTick >= 0) {
            float hitFade = 1.0f - smooth(Math.min(1.0f,
                (state.ageInTicks - state.hitTick) / (float) GrandCrescentWaveEntity.DISSIPATE_TICKS));
            fade = Math.min(fade, hitFade);
        }
        float open = smooth(Math.min(1.0f, state.ageInTicks / 12.0f));
        float impact = strike(progress);

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        float fpScale = (state.isFirstPersonOwner && state.ageInTicks < 6.0f)
            ? (0.55f + (state.ageInTicks / 6.0f) * 0.45f)
            : 1.0f;
        float span = 4.8f * fpScale * (0.80f + 0.20f * open);
        float chord = 1.55f * fpScale * (0.80f + 0.20f * open);
        // Distance scaling (standard point 1): distant waves step their
        // segment count down instead of costing full geometry.
        float distScale = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(
            cameraState, state.x, state.y, state.z);
        int segments = com.dragonblockarcanedba.util.FxEnvelope.segments(32, distScale, 14);
        float age = state.ageInTicks;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            drawVolumetricCrescent(matrix, buffer, span, chord, 0.15f, 0.62f, segments, 1.0f, 0.82f, 0.15f, 0.88f * fade);
            drawVolumetricCrescent(matrix, buffer, span * 0.78f, chord * 0.78f, 0.045f, 0.22f, segments, 1.0f, 0.98f, 0.80f, fade);
            drawVolumetricCrescent(matrix, buffer, span * 0.68f, chord * 0.68f, 0.016f, 0.08f, segments, 1.0f, 1.0f, 1.0f, fade);
            drawWingRibbon(matrix, buffer, span * 1.04f, chord, 0.18f, segments, 1.0f, 0.90f, 0.30f, 0.70f * fade, age * 0.2f);
            drawWingRibbon(matrix, buffer, span * 1.04f, chord, -0.18f, segments, 1.0f, 0.90f, 0.30f, 0.70f * fade, -age * 0.2f);
            drawTrailingArc(matrix, buffer, span * 0.88f, chord * 0.88f, -0.48f, segments, 1.0f, 0.75f, 0.10f, 0.45f * fade);
            drawValorGlyphs(matrix, buffer, span, chord, fade, age);
            if (impact > 0.02f) {
                float s = 0.34f * impact;
                float belly = chord * 0.5f;
                buffer.addVertex(matrix, -s, 0.0f, belly).setColor(1.0f, 0.98f, 0.8f, impact).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
                buffer.addVertex(matrix, 0.0f, s, belly).setColor(1.0f, 1.0f, 1.0f, impact).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
                buffer.addVertex(matrix, s, 0.0f, belly).setColor(1.0f, 0.98f, 0.8f, impact).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
                buffer.addVertex(matrix, 0.0f, -s, belly).setColor(1.0f, 1.0f, 1.0f, impact).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    private static float strike(float progress) {
        if (progress >= 0.16f) return 0.0f;
        return (float) Math.sin((progress / 0.16f) * Math.PI);
    }

    private static void drawVolumetricCrescent(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float thickness, float trailLength, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;
            float x1 = t1 * span;
            float z1 = (1.0f - t1 * t1) * chord;
            float x2 = t2 * span;
            float z2 = (1.0f - t2 * t2) * chord;
            float th1 = thickness * (1.0f - Math.abs(t1) * 0.78f);
            float th2 = thickness * (1.0f - Math.abs(t2) * 0.78f);
            float z1Trail = z1 - trailLength * (1.0f - Math.abs(t1) * 0.55f);
            float z2Trail = z2 - trailLength * (1.0f - Math.abs(t2) * 0.55f);

            // Top Bevel Face
            consumer.addVertex(matrix, x1, th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2 * 0.3f, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, th1 * 0.3f, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom Bevel Face
            consumer.addVertex(matrix, x1, -th1 * 0.3f, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2 * 0.3f, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, -th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

            // Front Razor Edge Face
            consumer.addVertex(matrix, x1, -th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, th2, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, th1, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawWingRibbon(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float yOffset, int segments, float r, float g, float b, float a, float rotOffset) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;

            float x1 = t1 * span;
            float z1 = (1.0f - t1 * t1) * chord;
            float x2 = t2 * span;
            float z2 = (1.0f - t2 * t2) * chord;

            float y1 = yOffset + (float) Math.sin(t1 * Math.PI + rotOffset) * 0.12f;
            float y2 = yOffset + (float) Math.sin(t2 * Math.PI + rotOffset) * 0.12f;

            float ribbonWidth = 0.10f * (1.0f - Math.abs(t1) * 0.5f);

            consumer.addVertex(matrix, x1, y1 + ribbonWidth, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2 + ribbonWidth, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2 - ribbonWidth, z2 - 0.25f).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, y1 - ribbonWidth, z1 - 0.25f).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawTrailingArc(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float zOffset, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;
            float x1 = t1 * span;
            float z1 = (1.0f - t1 * t1) * chord + zOffset;
            float x2 = t2 * span;
            float z2 = (1.0f - t2 * t2) * chord + zOffset;

            consumer.addVertex(matrix, x1, 0.05f, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, 0.05f, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, 0.05f, z2 - 0.35f).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, 0.05f, z1 - 0.35f).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawValorGlyphs(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float fade, float age) {
        int glyphCount = 7;
        for (int g = 0; g < glyphCount; g++) {
            float t = (g / (float) (glyphCount - 1)) * 1.6f - 0.8f;
            float gx = t * span;
            float gz = (1.0f - t * t) * chord * 0.72f;

            // Energy flow: a brightness pulse travels glyph to glyph along
            // the arc instead of every glyph sitting at one flat alpha.
            float glyphAlpha = fade * (0.70f + 0.30f * (float) Math.sin(age * 0.6f - g * 0.9f));

            float gSize = 0.12f;
            consumer.addVertex(matrix, gx - gSize, 0.08f, gz).setColor(1.0f, 1.0f, 0.90f, glyphAlpha).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, gx, 0.08f, gz + gSize).setColor(1.0f, 1.0f, 0.90f, glyphAlpha).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, gx + gSize, 0.08f, gz).setColor(1.0f, 1.0f, 0.90f, glyphAlpha).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, gx, 0.08f, gz - gSize).setColor(1.0f, 1.0f, 0.90f, glyphAlpha).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }
}
