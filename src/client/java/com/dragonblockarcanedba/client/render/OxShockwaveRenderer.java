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

import java.util.Random;

/**
 * Entity Renderer for Ox King's Groundbreaker Shockwave in Minecraft 26.2.
 * Renders towering 3D jagged earth crags, erupted basalt monoliths, tiered volcanic magma strata,
 * and expanding multi-layered 3D shockwave compression walls.
 */
public class OxShockwaveRenderer extends EntityRenderer<OxShockwaveEntity, OxShockwaveRenderer.OxShockwaveRenderState> {
    public OxShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class OxShockwaveRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public boolean isSubWave = false;
        public float currentRadius = 1.0f;
        public float age = 0;
        public long seed = 0;
    }

    @Override
    public boolean shouldRender(OxShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
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
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(OxShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float radius = state.currentRadius;
        float charge = state.chargeRatio;
        boolean subWave = state.isSubWave;
        float age = state.age;

        float waveScale = subWave ? 0.75f : 1.0f;
        float cragMaxHeight = (1.2f + charge * 2.8f) * waveScale;
        float alpha = Math.max(0.2f, Math.min(1.0f, 1.2f - (age * 0.02f)));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Towering 3D Erupted Earth Crags & Basalt Monoliths around the expanding perimeter
            int cragCount = subWave ? 24 : (28 + (int) (charge * 8)); // 28 to 36 crags
            for (int c = 0; c < cragCount; c++) {
                double baseAngle = (c / (double) cragCount) * Math.PI * 2.0;
                double angleOffset = (rng.nextDouble() - 0.5) * 0.15;
                double angle = baseAngle + angleOffset;

                float rOffset = (rng.nextFloat() - 0.5f) * 0.8f;
                float cragDist = Math.max(0.5f, radius + rOffset);

                float cx = (float) Math.cos(angle) * cragDist;
                float cz = (float) Math.sin(angle) * cragDist;

                // Height variations
                float hVar = 0.75f + rng.nextFloat() * 0.5f;
                float cragHeight = cragMaxHeight * hVar;
                float cragWidth = (0.45f + rng.nextFloat() * 0.35f) * waveScale;

                // Outward radial tilt (crags lean violently forward/outward as earth fractures)
                float tiltFactor = 0.35f + rng.nextFloat() * 0.25f;
                float tipX = cx + (float) Math.cos(angle) * (cragHeight * tiltFactor);
                float tipZ = cz + (float) Math.sin(angle) * (cragHeight * tiltFactor);
                float tipY = cragHeight;

                // Tangent vector for width
                float tx = -(float) Math.sin(angle) * cragWidth * 0.5f;
                float tz = (float) Math.cos(angle) * cragWidth * 0.5f;

                // Secondary depth offset for thick 3D rock face
                float nx = (float) Math.cos(angle) * 0.25f;
                float nz = (float) Math.sin(angle) * 0.25f;

                // Draw faceted 3D Crag (Molten volcanic base transitioning into dark basalt top)
                drawFacetedCrag(matrix, buffer,
                    cx - tx, 0.05f, cz - tz,
                    cx + tx, 0.05f, cz + tz,
                    cx - nx, 0.05f, cz - nz,
                    tipX, tipY, tipZ,
                    1.0f, subWave ? 0.6f : 0.25f, 0.02f, alpha * 0.95f, // Molten base
                    0.28f, 0.22f, 0.18f, alpha                        // Basalt peak
                );
            }

            // 2. Multi-Layered 3D Magma Shockwave Crests & Ground Displacement Ridge
            int segments = 32;
            float ringWidth = (0.9f + charge * 0.7f) * waveScale;

            // Outer Ground Displacement Wall (Molten orange-red)
            drawContinuousShockwaveRing(matrix, buffer, 0, 0.05f, 0, radius, Math.max(0.1f, radius - ringWidth), cragMaxHeight * 0.45f, segments,
                1.0f, subWave ? 0.65f : 0.30f, 0.02f, alpha * 0.85f,
                0.85f, 0.15f, 0.0f, 0.0f);

            // Inner Blazing White-Gold Pressure Ridge
            drawContinuousShockwaveRing(matrix, buffer, 0, 0.08f, 0, radius - ringWidth * 0.2f, Math.max(0.1f, radius - ringWidth * 0.75f), cragMaxHeight * 0.75f, segments,
                1.0f, 0.90f, 0.25f, alpha * 0.95f,
                1.0f, 0.45f, 0.05f, alpha * 0.80f);

            // 3. Erupted Stone Spikes & Debris Shards along leading edge
            int shardCount = 14;
            for (int s = 0; s < shardCount; s++) {
                double sAngle = (s / (double) shardCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.3);
                float sDist = radius * (0.85f + rng.nextFloat() * 0.25f);
                float sx = (float) Math.cos(sAngle) * sDist;
                float sz = (float) Math.sin(sAngle) * sDist;

                float shardH = (0.6f + rng.nextFloat() * 0.8f) * waveScale;
                float shardW = 0.18f + rng.nextFloat() * 0.15f;

                float tiltX = (float) Math.cos(sAngle) * 0.2f;
                float tiltZ = (float) Math.sin(sAngle) * 0.2f;

                drawFacetedCrag(matrix, buffer,
                    sx - shardW, 0.05f, sz,
                    sx + shardW, 0.05f, sz,
                    sx, 0.05f, sz - shardW,
                    sx + tiltX, shardH, sz + tiltZ,
                    1.0f, 0.50f, 0.10f, alpha * 0.90f,
                    0.20f, 0.16f, 0.14f, alpha
                );
            }
        });

        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawFacetedCrag(Matrix4f matrix, VertexConsumer consumer,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        float x3, float y3, float z3,
                                        float tipX, float tipY, float tipZ,
                                        float rBase, float gBase, float bBase, float aBase,
                                        float rTip, float gTip, float bTip, float aTip) {
        // Face 1: (v1, v2, tip)
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Face 2: (v2, v3, tip)
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Face 3: (v3, v1, tip)
        consumer.addVertex(matrix, x3, y3, z3).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
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
