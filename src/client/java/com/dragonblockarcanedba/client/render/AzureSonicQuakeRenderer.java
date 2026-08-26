package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.AzureSonicQuakeEntity;
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
 * Entity Renderer for Sonic Quake Impact Shockwave in Minecraft 26.2.
 * Renders physical 3D supersonic ground shatter, vertical aerodynamic pressure needles, and radiant sonic wave crests.
 */
public class AzureSonicQuakeRenderer extends EntityRenderer<AzureSonicQuakeEntity, AzureSonicQuakeRenderer.QuakeRenderState> {

    public AzureSonicQuakeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class QuakeRenderState extends EntityRenderState {
        public float maxRadius = 8.5f;
        public float age = 0;
    }

    @Override
    public boolean shouldRender(AzureSonicQuakeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public QuakeRenderState createRenderState() {
        return new QuakeRenderState();
    }

    @Override
    public void extractRenderState(AzureSonicQuakeEntity entity, QuakeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.maxRadius = entity.getMaxRadius();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(QuakeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = 18.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float expansionEase = (float) Math.sin(progress * (Math.PI / 2.0));
        float currentRadius = Math.max(0.5f, expansionEase * state.maxRadius);
        float alpha = (1.0f - (progress * progress)) * 0.90f;

        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. High-Pressure 3D Supersonic Shockwave Ring (Raised Beveled Crest)
            int segments = 32;
            float ringWidth = Math.min(1.4f, currentRadius * 0.35f);
            float rOuter = currentRadius;
            float rInner = Math.max(0.1f, currentRadius - ringWidth);
            float crestHeight = 0.65f * (1.0f - progress * 0.8f);

            drawBeveledShockRing(matrix, buffer, 0, 0.05f, 0, rOuter, rInner, crestHeight, segments,
                0.0f, 0.90f, 1.0f, alpha,
                0.85f, 1.0f, 1.0f, alpha * 0.95f
            );

            // 2. Secondary Inner Sonic Compression Ring
            float rSubOuter = currentRadius * 0.55f;
            float rSubInner = Math.max(0.05f, rSubOuter - (ringWidth * 0.6f));
            drawFlatRing(matrix, buffer, 0, 0.08f, 0, rSubOuter, rSubInner, 24, 0.0f, 1.0f, 0.8f, alpha * 0.65f);

            // 3. Physical Vertical Aerodynamic Shards / Pressure Needles
            int shardCount = 18;
            float shardRadius = currentRadius * 0.85f;
            float shardHeight = 1.6f * (1.0f - progress);

            for (int i = 0; i < shardCount; i++) {
                double angle = (i / (double) shardCount) * Math.PI * 2.0 + (i % 2 == 0 ? 0.05 : -0.05);
                float sx = (float) Math.cos(angle) * shardRadius;
                float sz = (float) Math.sin(angle) * shardRadius;

                // Outward radial lean
                float leanX = (float) Math.cos(angle) * 0.4f * (1.0f - progress);
                float leanZ = (float) Math.sin(angle) * 0.4f * (1.0f - progress);

                float shardWidth = 0.28f * (1.0f - progress * 0.5f);
                float perpX = (float) -Math.sin(angle) * shardWidth;
                float perpZ = (float) Math.cos(angle) * shardWidth;

                // Draw 3D Shard Spike
                drawTriangle(matrix, buffer,
                    sx - perpX, 0.05f, sz - perpZ,
                    sx + perpX, 0.05f, sz + perpZ,
                    sx + leanX, 0.05f + shardHeight, sz + leanZ,
                    0.4f, 0.95f, 1.0f, alpha * 0.85f
                );
            }

            // 4. Ground Burst Epicenter Star / Shatter Core
            float coreRadius = Math.max(0.2f, (1.0f - progress) * 2.0f);
            drawFlatRing(matrix, buffer, 0, 0.12f, 0, coreRadius, 0.0f, 12, 1.0f, 1.0f, 1.0f, alpha * 0.90f);
        });

        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawBeveledShockRing(Matrix4f matrix, VertexConsumer consumer,
                                             float cx, float cy, float cz,
                                             float rOuter, float rInner, float height,
                                             int segments,
                                             float rBase, float gBase, float bBase, float aBase,
                                             float rTop, float gTop, float bTop, float aTop) {
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

            // Outer rising bevel
            consumer.addVertex(matrix, x1Out, cy, z1Out).setColor(rBase, gBase, bBase, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy, z2Out).setColor(rBase, gBase, bBase, 0.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy + height, z2Out).setColor(rTop, gTop, bTop, aTop).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1Out, cy + height, z1Out).setColor(rTop, gTop, bTop, aTop).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Inward slope
            consumer.addVertex(matrix, x1Out, cy + height, z1Out).setColor(rTop, gTop, bTop, aTop).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy + height, z2Out).setColor(rTop, gTop, bTop, aTop).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2In, cy, z2In).setColor(rBase, gBase, bBase, aBase * 0.5f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1In, cy, z1In).setColor(rBase, gBase, bBase, aBase * 0.5f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawFlatRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
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

            consumer.addVertex(matrix, x1In, cy, z1In).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2In, cy, z2In).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy, z2Out).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1Out, cy, z1Out).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawTriangle(Matrix4f matrix, VertexConsumer consumer,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float x3, float y3, float z3,
                                     float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Reverse
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
