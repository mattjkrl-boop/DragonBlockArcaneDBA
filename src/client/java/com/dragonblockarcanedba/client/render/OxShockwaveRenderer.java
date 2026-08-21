package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.OxShockwaveEntity;
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
 * Entity Renderer for Ox King's Groundbreaker Shockwave in Minecraft 26.2.
 * Renders an expanding continuous volcanic ground shockwave ring with magma crests and earthquake fractures.
 */
public class OxShockwaveRenderer extends EntityRenderer<OxShockwaveEntity, OxShockwaveRenderer.OxShockwaveRenderState> {
    public OxShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class OxShockwaveRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public boolean isSubWave = false;
        public float currentRadius = 1.0f;
    }

    @Override
    public OxShockwaveRenderState createRenderState() {
        return new OxShockwaveRenderState();
    }

    @Override
    public void extractRenderState(OxShockwaveEntity entity, OxShockwaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.isSubWave = entity.isSubWave();
        state.currentRadius = entity.getCurrentRadius();
    }

    @Override
    public void submit(OxShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float radius = state.currentRadius;
        int segments = 28;
        float height = 0.35f + (state.chargeRatio * 0.45f);
        float ringWidth = 0.85f + (state.chargeRatio * 0.6f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Fiery Magma Outer Ring
            drawContinuousShockwaveRing(matrix, buffer, 0, 0.05f, 0, radius, radius - ringWidth, height, segments,
                1.0f, 0.30f, 0.0f, 0.85f, 1.0f, 0.1f, 0.0f, 0.0f);

            // 2. Blazing White-Gold Inner Ridge
            drawContinuousShockwaveRing(matrix, buffer, 0, 0.08f, 0, radius - ringWidth * 0.25f, radius - ringWidth * 0.75f, height * 1.3f, segments,
                1.0f, 0.90f, 0.2f, 0.95f, 1.0f, 0.4f, 0.0f, 0.8f);
        });

        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawContinuousShockwaveRing(Matrix4f matrix, VertexConsumer consumer,
                                                   float cx, float cy, float cz,
                                                   float rOuter, float rInner, float height,
                                                   int segments,
                                                   float rTop, float gTop, float bTop, float aTop,
                                                   float rBot, float gBot, float bBot, float aBot) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1Out = cx + (float) Math.cos(a1) * rOuter;
            float z1Out = cz + (float) Math.sin(a1) * rOuter;
            float x2Out = cx + (float) Math.cos(a2) * rOuter;
            float z2Out = cz + (float) Math.sin(a2) * rOuter;

            float x1In = cx + (float) Math.cos(a1) * rInner;
            float z1In = cz + (float) Math.sin(a1) * rInner;
            float x2In = cx + (float) Math.cos(a2) * rInner;
            float z2In = cz + (float) Math.sin(a2) * rInner;

            // Outer rising wall
            consumer.addVertex(matrix, x1Out, cy, z1Out).setColor(rBot, gBot, bBot, aBot).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy, z2Out).setColor(rBot, gBot, bBot, aBot).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy + height, z2Out).setColor(rTop, gTop, bTop, aTop).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1Out, cy + height, z1Out).setColor(rTop, gTop, bTop, aTop).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Top ridge connecting outer to inner
            consumer.addVertex(matrix, x1Out, cy + height, z1Out).setColor(rTop, gTop, bTop, aTop).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy + height, z2Out).setColor(rTop, gTop, bTop, aTop).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2In, cy, z2In).setColor(rBot, gBot, bBot, aBot).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1In, cy, z1In).setColor(rBot, gBot, bBot, aBot).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }
}
