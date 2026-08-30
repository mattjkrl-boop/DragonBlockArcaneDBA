package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.GrandClashSparkEntity;
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
 * Entity Renderer for Grand Clash Spark in Minecraft 26.2.
 * Renders an explosive physical 3D golden-white energy deflection clash:
 * - Dual counter-rotating golden deflection shock rings
 * - 8-point physical 3D diamond energy clash spikes/prisms
 * - 12 high-velocity flying 3D diamond spark shards
 * - Blinding radiant white-gold core flash
 */
public class GrandClashSparkRenderer extends EntityRenderer<GrandClashSparkEntity, GrandClashSparkRenderer.ClashRenderState> {

    public GrandClashSparkRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ClashRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float age = 0;
        public int maxLifetime = 10;
        public long seed = 0;
    }

    @Override
    public boolean shouldRender(GrandClashSparkEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public ClashRenderState createRenderState() {
        return new ClashRenderState();
    }

    @Override
    public void extractRenderState(GrandClashSparkEntity entity, ClashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getClashScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(ClashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        if (progress >= 1.0f) return;

        float fade = 1.0f - (progress * progress);
        float scale = state.scale * (0.85f + progress * 0.95f);
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Dual Counter-Rotating Golden Clash Shock Rings
            int ringSegments = 20;
            float rOuter = 1.6f * scale;
            float rInner = rOuter * 0.75f;
            drawClashRing(matrix, buffer, rOuter, rInner, ringSegments, state.age * 30.0f, 1.0f, 0.85f, 0.15f, fade * 0.85f);
            drawClashRing(matrix, buffer, rOuter * 0.75f, rInner * 0.65f, ringSegments, -state.age * 45.0f, 1.0f, 0.98f, 0.70f, fade * 0.95f);

            // 2. 8-Point Physical 3D Diamond Energy Clash Spikes
            int spikeCount = 8;
            for (int i = 0; i < spikeCount; i++) {
                double angle = (i / (double) spikeCount) * Math.PI * 2.0 + (state.age * 0.15);
                float spikeLen = (1.4f + (i % 2 == 0 ? 0.8f : 0.3f)) * scale;
                float spikeWidth = 0.22f * (1.0f - progress * 0.7f) * scale;

                float tipX = (float) Math.cos(angle) * spikeLen;
                float tipY = (float) Math.sin(angle) * spikeLen;

                float normX = -(float) Math.sin(angle) * spikeWidth * 0.5f;
                float normY = (float) Math.cos(angle) * spikeWidth * 0.5f;

                drawSpikeQuad(matrix, buffer,
                    -normX, -normY, -0.05f,
                    normX, normY, -0.05f,
                    tipX, tipY, 0.0f,
                    1.0f, 0.95f, 0.40f, fade,
                    1.0f, 0.70f, 0.10f, fade * 0.6f
                );
                drawSpikeQuad(matrix, buffer,
                    -normX, -normY, 0.05f,
                    normX, normY, 0.05f,
                    tipX, tipY, 0.0f,
                    1.0f, 0.95f, 0.40f, fade,
                    1.0f, 0.70f, 0.10f, fade * 0.6f
                );
            }

            // 3. 12 High-Velocity Flying 3D Diamond Spark Shards
            int sparkCount = 12;
            for (int s = 0; s < sparkCount; s++) {
                double sparkAngle = (s / (double) sparkCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.4);
                float speed = 1.2f + rng.nextFloat() * 1.5f;
                float sparkDist = progress * speed * 2.8f * scale;

                float sx = (float) Math.cos(sparkAngle) * sparkDist;
                float sy = (float) Math.sin(sparkAngle) * sparkDist + (rng.nextFloat() - 0.5f) * 0.5f;
                float sz = (rng.nextFloat() - 0.5f) * sparkDist * 0.6f;

                float sparkSize = 0.14f * (1.0f - progress * 0.8f);
                drawDiamondSpark(matrix, buffer, sx, sy, sz, sparkSize, 1.0f, 0.90f, 0.30f, fade);
            }

            // 4. Blinding Radiant White-Gold Core Flash (Flashing diamond polyhedron)
            float coreSize = (0.75f * (1.0f - progress * 0.6f)) * scale;
            drawDiamondSpark(matrix, buffer, 0, 0, 0, coreSize, 1.0f, 1.0f, 0.95f, fade);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawClashRing(Matrix4f matrix, VertexConsumer consumer, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        float rotRad = (float) Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2) + rotRad;

            float x1 = (float) Math.cos(a1) * rOuter;
            float y1 = (float) Math.sin(a1) * rOuter;
            float x2 = (float) Math.cos(a2) * rOuter;
            float y2 = (float) Math.sin(a2) * rOuter;

            float ix1 = (float) Math.cos(a1) * rInner;
            float iy1 = (float) Math.sin(a1) * rInner;
            float ix2 = (float) Math.cos(a2) * rInner;
            float iy2 = (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, ix1, iy1, 0).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, ix2, iy2, 0).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawSpikeQuad(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float tipX, float tipY, float tipZ, float rBase, float gBase, float bBase, float aBase, float rTip, float gTip, float bTip, float aTip) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }

    private static void drawDiamondSpark(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float r, float g, float b, float a) {
        float h = size * 1.5f;
        float w = size * 0.6f;

        // Diamond quad 1
        consumer.addVertex(matrix, cx - w, cy, cz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx + w, cy, cz).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Diamond quad 2 (cross plane)
        consumer.addVertex(matrix, cx, cy, cz - w).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy, cz + w).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
    }
}
