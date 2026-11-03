package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.SaberDodgeSparkEntity;
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
 * Entity Renderer for Saber Dodge Spark in Minecraft 26.2.
 * Renders an explosive physical 3D golden deflection shatter & clash spark effect:
 * - Dual counter-rotating golden clash shock rings
 * - 8-point physical 3D diamond energy clash spikes
 * - 12 high-velocity flying 3D diamond spark shards
 * - Blinding radiant white-gold core flash.
 */
public class SaberDodgeSparkRenderer extends EntityRenderer<SaberDodgeSparkEntity, SaberDodgeSparkRenderer.DodgeSparkRenderState> {

    public SaberDodgeSparkRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DodgeSparkRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float age = 0;
        public int maxLifetime = 12;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SaberDodgeSparkEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SaberDodgeSparkEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DodgeSparkRenderState createRenderState() {
        return new DodgeSparkRenderState();
    }

    @Override
    public void extractRenderState(SaberDodgeSparkEntity entity, DodgeSparkRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getSparkScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(DodgeSparkRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        if (progress >= 1.0f) return;

        float fade = (1.0f - progress * progress);
        float scale = state.scale * (0.9f + progress * 1.1f);
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        SubmitNodeCollector.CustomGeometryRenderer geometry = (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Dual Counter-Rotating Golden Clash Shock Rings
            int ringSegments = 20;
            float rOuter = 1.8f * scale;
            float rInner = rOuter * 0.72f;
            drawClashRing(matrix, buffer, rOuter, rInner, ringSegments, state.age * 35.0f, 1.0f, 0.85f, 0.15f, fade * 0.90f);
            drawClashRing(matrix, buffer, rOuter * 0.75f, rInner * 0.65f, ringSegments, -state.age * 50.0f, 1.0f, 0.98f, 0.70f, fade * 0.98f);
            drawClashRing(matrix, buffer, rOuter * 0.28f, rOuter * 0.10f, 16, state.age * 22.0f, 1.0f, 1.0f, 1.0f, fade);

            // 2. 8-Point Physical 3D Diamond Energy Clash Spikes
            int spikeCount = 8;
            for (int i = 0; i < spikeCount; i++) {
                double angle = (i / (double) spikeCount) * Math.PI * 2.0 + (state.age * 0.15);
                float spikeLen = (1.5f + (i % 2 == 0 ? 0.9f : 0.4f)) * scale;
                float spikeWidth = 0.24f * (1.0f - progress * 0.7f) * scale;

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
                float speed = 1.3f + rng.nextFloat() * 1.6f;
                float sparkDist = progress * speed * 3.0f * scale;

                float sx = (float) Math.cos(sparkAngle) * sparkDist;
                float sy = (float) Math.sin(sparkAngle) * sparkDist + (rng.nextFloat() - 0.5f) * 0.6f;
                float sz = (rng.nextFloat() - 0.5f) * sparkDist * 0.7f;

                float sparkSize = 0.15f * (1.0f - progress * 0.8f);
                drawDiamondSpark(matrix, buffer, sx, sy, sz, sparkSize, 1.0f, 0.92f, 0.35f, fade);
            }

            // 4. Blinding Radiant White-Gold Core Flash
            float coreSize = (0.8f * (1.0f - progress * 0.6f)) * scale;
            drawDiamondSpark(matrix, buffer, 0, 0, 0, coreSize, 1.0f, 1.0f, 0.95f, fade);
        };

        // The burst is built in one plane and nearly vanishes edge-on, so the
        // same geometry is submitted a second time rotated 90 degrees, giving
        // the clash volume from any viewing angle.
        collector.submitCustomGeometry(poseStack, renderType, geometry);
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotationY((float) Math.PI / 2.0f));
        collector.submitCustomGeometry(poseStack, renderType, geometry);
        poseStack.popPose();

        // Additive halo pass (light touch for a 12 tick burst): an oversized
        // faint copy of the core flash and one clash ring so the impact blooms.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            float coreSize = (0.8f * (1.0f - progress * 0.6f)) * scale * 1.6f;
            drawDiamondSpark(matrix, buffer, 0, 0, 0, coreSize, 1.0f, 1.0f, 0.95f, fade * 0.22f);
            drawClashRing(matrix, buffer, 1.8f * scale * 1.15f, 1.8f * scale * 0.80f, 28, state.age * 35.0f, 1.0f, 0.85f, 0.15f, fade * 0.14f);
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
        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        float mz = (z1 + z2) * 0.5f;
        float bx = (x2 - x1) * 0.5f;
        float by = (y2 - y1) * 0.5f;
        float dx = tipX - mx;
        float dy = tipY - my;
        float dz = tipZ - mz;
        float px = -dy;
        float py = dx;
        float pLen = (float) Math.sqrt(px * px + py * py);
        if (pLen > 0.001f) {
            px /= pLen;
            py /= pLen;
        }
        int steps = 5;
        for (int i = 0; i < steps; i++) {
            float t1 = i / (float) steps;
            float t2 = (i + 1) / (float) steps;
            float bow1 = (float) Math.sin(t1 * Math.PI) * 0.12f;
            float bow2 = (float) Math.sin(t2 * Math.PI) * 0.12f;
            float w1 = 1.0f - t1;
            float w2 = 1.0f - t2;
            float ax = mx + dx * t1 + px * bow1;
            float ay = my + dy * t1 + py * bow1;
            float az = mz + dz * t1;
            float bx2 = mx + dx * t2 + px * bow2;
            float by2 = my + dy * t2 + py * bow2;
            float bz = mz + dz * t2;
            float ar = rBase + (rTip - rBase) * t1;
            float ag = gBase + (gTip - gBase) * t1;
            float ab = bBase + (bTip - bBase) * t1;
            float aa = aBase + (aTip - aBase) * t1;
            float br = rBase + (rTip - rBase) * t2;
            float bg = gBase + (gTip - gBase) * t2;
            float bb = bBase + (bTip - bBase) * t2;
            float ba = aBase + (aTip - aBase) * t2;
            consumer.addVertex(matrix, ax - bx * w1, ay - by * w1, az).setColor(ar, ag, ab, aa).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, ax + bx * w1, ay + by * w1, az).setColor(ar, ag, ab, aa).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, bx2 + bx * w2, by2 + by * w2, bz).setColor(br, bg, bb, ba).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, bx2 - bx * w2, by2 - by * w2, bz).setColor(br, bg, bb, ba).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            float c1 = w1 * 0.28f;
            float c2 = w2 * 0.28f;
            consumer.addVertex(matrix, ax - bx * c1, ay - by * c1, az).setColor(1.0f, 1.0f, 1.0f, aa).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, ax + bx * c1, ay + by * c1, az).setColor(1.0f, 1.0f, 1.0f, aa).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, bx2 + bx * c2, by2 + by * c2, bz).setColor(1.0f, 1.0f, 1.0f, ba).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, bx2 - bx * c2, by2 - by * c2, bz).setColor(1.0f, 1.0f, 1.0f, ba).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawDiamondSpark(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float r, float g, float b, float a) {
        float h = size * 1.5f;
        float w = size * 0.6f;

        consumer.addVertex(matrix, cx - w, cy, cz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx + w, cy, cz).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        consumer.addVertex(matrix, cx, cy, cz - w).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy, cz + w).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        int arms = 4;
        int steps = 3;
        for (int arm = 0; arm < arms; arm++) {
            float ang = arm * ((float) Math.PI * 0.5f) + 0.35f;
            float dx = (float) Math.cos(ang);
            float dz = (float) Math.sin(ang);
            float px = -dz;
            float pz = dx;
            for (int i = 0; i < steps; i++) {
                float t1 = i / (float) steps;
                float t2 = (i + 1) / (float) steps;
                float bow1 = (float) Math.sin(t1 * Math.PI) * size * 0.45f;
                float bow2 = (float) Math.sin(t2 * Math.PI) * size * 0.45f;
                float d1 = size * (0.35f + 1.15f * t1);
                float d2 = size * (0.35f + 1.15f * t2);
                float w1 = size * 0.28f * (1.0f - t1);
                float w2 = size * 0.28f * (1.0f - t2);
                float x1 = cx + dx * d1 + px * bow1;
                float z1 = cz + dz * d1 + pz * bow1;
                float x2 = cx + dx * d2 + px * bow2;
                float z2 = cz + dz * d2 + pz * bow2;
                float a1 = a * (1.0f - t1 * 0.25f);
                float a2 = a * (1.0f - t2);
                consumer.addVertex(matrix, x1 - px * w1, cy, z1 - pz * w1).setColor(r, g, b, a1 * 0.4f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1 + px * w1, cy, z1 + pz * w1).setColor(r, g, b, a1 * 0.4f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 + px * w2, cy, z2 + pz * w2).setColor(r, g, b, a2 * 0.08f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 - px * w2, cy, z2 - pz * w2).setColor(r, g, b, a2 * 0.08f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1 - px * w1 * 0.3f, cy, z1 - pz * w1 * 0.3f).setColor(1.0f, 1.0f, 1.0f, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1 + px * w1 * 0.3f, cy, z1 + pz * w1 * 0.3f).setColor(1.0f, 1.0f, 1.0f, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 + px * w2 * 0.3f, cy, z2 + pz * w2 * 0.3f).setColor(1.0f, 1.0f, 1.0f, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 - px * w2 * 0.3f, cy, z2 - pz * w2 * 0.3f).setColor(1.0f, 1.0f, 1.0f, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            }
        }
    }
}
