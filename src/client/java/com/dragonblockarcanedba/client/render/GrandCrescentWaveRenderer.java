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
 * Renders a massive, physical volumetric 3D golden-white crescent energy greatsword wave:
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
    }

    @Override
    public boolean shouldRender(GrandCrescentWaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
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
    }

    @Override
    public void submit(GrandCrescentWaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float maxLife = 50.0f;
        float progress = Math.min(1.0f, state.ageInTicks / maxLife);
        float fade = (1.0f - progress * progress);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        float span = 4.8f;
        float chord = 1.6f;
        int segments = 22;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Volumetric 3D Grand Crescent Wave Blade (Thick multi-layered bevel)
            // Layer A: Outer Radiant Golden Canopy
            drawVolumetricCrescent(matrix, buffer, span + 0.45f, chord + 0.45f, 0.28f, 0.70f, segments,
                1.0f, 0.82f, 0.15f, 0.85f * fade);

            // Layer B: Dense Pure White-Gold Core Blade
            drawVolumetricCrescent(matrix, buffer, span, chord, 0.16f, 0.45f, segments,
                1.0f, 0.98f, 0.80f, 0.98f * fade);

            // Layer C: Blinding White Razor Cutting Edge
            drawVolumetricCrescent(matrix, buffer, span * 0.75f, chord * 0.75f, 0.08f, 0.20f, segments,
                1.0f, 1.0f, 1.0f, 1.0f * fade);

            // 2. Dual Upper & Lower Vortex Wing Ribbons
            float wingSpan = span * 1.08f;
            float wingChord = chord * 1.15f;
            drawWingRibbon(matrix, buffer, wingSpan, wingChord, 0.22f, segments, 1.0f, 0.90f, 0.30f, 0.75f * fade, state.ageInTicks * 0.2f);
            drawWingRibbon(matrix, buffer, wingSpan, wingChord, -0.22f, segments, 1.0f, 0.90f, 0.30f, 0.75f * fade, -state.ageInTicks * 0.2f);

            // 3. Trailing Secondary & Tertiary 3D Shockwave Arcs
            drawTrailingArc(matrix, buffer, span * 0.90f, chord * 0.90f, -0.55f, segments, 1.0f, 0.75f, 0.10f, 0.60f * fade);
            drawTrailingArc(matrix, buffer, span * 0.70f, chord * 0.70f, -1.05f, segments, 1.0f, 0.65f, 0.05f, 0.40f * fade);

            // 4. Inscribed Sacred Valor Glyphs across center arc
            drawValorGlyphs(matrix, buffer, span, chord, fade);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawVolumetricCrescent(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float thickness, float trailLength, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;

            float x1 = t1 * span;
            float z1 = (1.0f - t1 * t1) * chord;
            float x2 = t2 * span;
            float z2 = (1.0f - t2 * t2) * chord;

            float th1 = thickness * (1.0f - Math.abs(t1) * 0.70f);
            float th2 = thickness * (1.0f - Math.abs(t2) * 0.70f);

            float z1Trail = z1 - trailLength * (1.0f - Math.abs(t1) * 0.60f);
            float z2Trail = z2 - trailLength * (1.0f - Math.abs(t2) * 0.60f);

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

    private static void drawValorGlyphs(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float fade) {
        int glyphCount = 7;
        for (int g = 0; g < glyphCount; g++) {
            float t = (g / (float) (glyphCount - 1)) * 1.6f - 0.8f;
            float gx = t * span;
            float gz = (1.0f - t * t) * chord - 0.15f;

            float gSize = 0.12f;
            consumer.addVertex(matrix, gx - gSize, 0.08f, gz).setColor(1.0f, 1.0f, 0.90f, fade).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, gx, 0.08f, gz + gSize).setColor(1.0f, 1.0f, 0.90f, fade).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, gx + gSize, 0.08f, gz).setColor(1.0f, 1.0f, 0.90f, fade).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, gx, 0.08f, gz - gSize).setColor(1.0f, 1.0f, 0.90f, fade).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }
}
