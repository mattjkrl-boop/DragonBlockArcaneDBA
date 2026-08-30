package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.SpiritImpaleEntity;
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
 * Entity Renderer for Spirit Impale in Minecraft 26.2.
 * Renders 6 physical 3D ethereal celestial swords impaling the target, 3 counter-rotating
 * divine judgement ground arrays, an ascending divine pillar, and exploding crystal spikes.
 */
public class SpiritImpaleRenderer extends EntityRenderer<SpiritImpaleEntity, SpiritImpaleRenderer.SpiritImpaleRenderState> {

    public SpiritImpaleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SpiritImpaleRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float age = 0;
        public int maxLifetime = 28;
        public long seed = 0;
    }

    @Override
    public boolean shouldRender(SpiritImpaleEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public SpiritImpaleRenderState createRenderState() {
        return new SpiritImpaleRenderState();
    }

    @Override
    public void extractRenderState(SpiritImpaleEntity entity, SpiritImpaleRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getImpaleScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(SpiritImpaleRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float age = state.age;
        float scale = state.scale;
        float fade = age > 20.0f ? Math.max(0.0f, (state.maxLifetime - age) / 8.0f) : 1.0f;
        float slam = Math.min(1.0f, age / 4.0f);
        float pulse = 0.85f + 0.15f * (float) Math.sin(age * 0.4f);

        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Concentric Divine Judgement Ground Arrays (3 Counter-Rotating Runic Arrays)
            float baseRadius = 2.4f * scale * slam;
            if (fade > 0.02f) {
                // Outer Cyan Script Ring
                drawGroundRing(matrix, buffer, 0, 0.03f, 0, baseRadius, baseRadius * 0.82f, 24, age * 22.0f, 0.0f, 0.90f, 1.0f, fade * 0.85f);
                // Middle Golden Lotus Ring
                drawGroundRing(matrix, buffer, 0, 0.04f, 0, baseRadius * 0.68f, baseRadius * 0.48f, 18, -age * 30.0f, 1.0f, 0.82f, 0.15f, fade * 0.90f);
                // Inner White Flash Disk
                drawGroundRing(matrix, buffer, 0, 0.05f, 0, baseRadius * 0.35f * pulse, 0.0f, 12, age * 15.0f, 1.0f, 1.0f, 1.0f, fade * 0.95f);
            }

            // 2. Ascending Vertical Divine Ki Pillar & Helical Aura Ribbons
            float pillarHeight = 6.0f * slam;
            float pillarRadius = 0.65f * scale * pulse;
            drawCylinderPillar(matrix, buffer, 0, 0, pillarRadius, pillarHeight, 8, 0.0f, 0.92f, 1.0f, fade * 0.60f);
            drawCylinderPillar(matrix, buffer, 0, 0, pillarRadius * 0.42f, pillarHeight * 1.05f, 6, 1.0f, 1.0f, 1.0f, fade * 0.90f);

            // Helical Aura Ribbons around the pillar
            drawAscendingHelices(matrix, buffer, age, pillarHeight, pillarRadius * 1.25f, fade);

            // 3. 6 Physical 3D Ethereal Spirit Swords Plunging and Skewering the Target
            int swordCount = 6;
            float swordRingRadius = 1.65f * scale * (1.3f - slam * 0.3f);
            float startDropY = 3.5f * (1.0f - slam);
            float targetCenterY = 0.75f * scale + (float) Math.sin(age * 0.3f) * 0.04f;

            for (int i = 0; i < swordCount; i++) {
                double baseAngle = (i / (double) swordCount) * Math.PI * 2.0 + (age * 0.02f);
                float hx = (float) Math.cos(baseAngle) * swordRingRadius;
                float hz = (float) Math.sin(baseAngle) * swordRingRadius;
                float hy = targetCenterY + 1.2f + startDropY;

                // Tip aims into center target position
                float tipX = (float) Math.cos(baseAngle) * (0.25f * scale);
                float tipZ = (float) Math.sin(baseAngle) * (0.25f * scale);
                float tipY = targetCenterY - 0.2f * scale;

                // Sword vibration on impact
                float vibX = (rng.nextFloat() - 0.5f) * 0.04f * (1.0f - slam * 0.5f);
                float vibZ = (rng.nextFloat() - 0.5f) * 0.04f * (1.0f - slam * 0.5f);

                draw3DSpiritSword(matrix, buffer,
                    hx + vibX, hy, hz + vibZ,
                    tipX, tipY, tipZ,
                    scale, fade, pulse, i
                );
            }

            // 4. 8 Erupting 3D Divine Crystal Impalement Spikes along Ground Perimeter
            int spikeCount = 8;
            for (int s = 0; s < spikeCount; s++) {
                double sAngle = (s / (double) spikeCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.2);
                float sDist = baseRadius * (0.75f + rng.nextFloat() * 0.35f);
                float sx = (float) Math.cos(sAngle) * sDist;
                float sz = (float) Math.sin(sAngle) * sDist;

                float spikeH = (0.8f + rng.nextFloat() * 0.6f) * slam * scale;
                float spikeW = 0.22f * scale;

                float tx = -(float) Math.sin(sAngle) * spikeW * 0.5f;
                float tz = (float) Math.cos(sAngle) * spikeW * 0.5f;
                float tiltX = (float) Math.cos(sAngle) * 0.25f;
                float tiltZ = (float) Math.sin(sAngle) * 0.25f;

                drawCrystalSpike(matrix, buffer,
                    sx - tx, 0.04f, sz - tz,
                    sx + tx, 0.04f, sz + tz,
                    sx + tiltX, spikeH, sz + tiltZ,
                    0.0f, 0.70f, 0.95f, fade * 0.85f,
                    1.0f, 1.0f, 1.0f, fade
                );
            }
        });
    }

    private static void draw3DSpiritSword(Matrix4f matrix, VertexConsumer consumer,
                                          float hx, float hy, float hz,
                                          float tx, float ty, float tz,
                                          float scale, float fade, float pulse, int index) {
        float dx = tx - hx, dy = ty - hy, dz = tz - hz;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.01f) return;

        float dirX = dx / len, dirY = dy / len, dirZ = dz / len;

        // Orthogonal vectors
        float upX = 0, upY = 1, upZ = 0;
        float rightX = dirY * upZ - dirZ * upY;
        float rightY = dirZ * upX - dirX * upZ;
        float rightZ = dirX * upY - dirY * upX;
        float rLen = (float) Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
        if (rLen < 0.001f) {
            rightX = 1; rightY = 0; rightZ = 0;
        } else {
            rightX /= rLen; rightY /= rLen; rightZ /= rLen;
        }

        float normX = rightY * dirZ - rightZ * dirY;
        float normY = rightZ * dirX - rightX * dirZ;
        float normZ = rightX * dirY - rightY * dirX;

        float bladeWidth = 0.20f * scale;
        float bladeThickness = 0.07f * scale;
        float guardWidth = 0.48f * scale;
        float pommelSize = 0.12f * scale;

        // Hilt base to guard
        float guardOffset = len * 0.18f;
        float gx = hx + dirX * guardOffset;
        float gy = hy + dirY * guardOffset;
        float gz = hz + dirZ * guardOffset;

        // 1. Blade Body (Faceted 3D Diamond Cross-Section)
        // Guard to Tip
        float bx1 = gx - rightX * bladeWidth * 0.5f, by1 = gy - rightY * bladeWidth * 0.5f, bz1 = gz - rightZ * bladeWidth * 0.5f;
        float bx2 = gx + rightX * bladeWidth * 0.5f, by2 = gy + rightY * bladeWidth * 0.5f, bz2 = gz + rightZ * bladeWidth * 0.5f;
        float fx1 = gx + normX * bladeThickness, fy1 = gy + normY * bladeThickness, fz1 = gz + normZ * bladeThickness;
        float fx2 = gx - normX * bladeThickness, fy2 = gy - normY * bladeThickness, fz2 = gz - normZ * bladeThickness;

        // Colors: Radiant Celestial Cyan & Pure White Ridge
        float rCore = 0.0f, gCore = 0.95f * pulse, bCore = 1.0f;
        float rEdge = 0.85f, gEdge = 0.98f, bEdge = 1.0f;

        // Face 1 (Top-Left)
        drawTriangle(matrix, consumer, bx1, by1, bz1, fx1, fy1, fz1, tx, ty, tz, rCore, gCore, bCore, fade * 0.95f);
        // Face 2 (Top-Right)
        drawTriangle(matrix, consumer, fx1, fy1, fz1, bx2, by2, bz2, tx, ty, tz, rEdge, gEdge, bEdge, fade * 0.95f);
        // Face 3 (Bottom-Right)
        drawTriangle(matrix, consumer, bx2, by2, bz2, fx2, fy2, fz2, tx, ty, tz, rCore, gCore, bCore, fade * 0.95f);
        // Face 4 (Bottom-Left)
        drawTriangle(matrix, consumer, fx2, fy2, fz2, bx1, by1, bz1, tx, ty, tz, rEdge, gEdge, bEdge, fade * 0.95f);

        // 2. Winged Celestial Crossguard (Golden / Cyan Wing Flanges)
        float qx1 = gx - rightX * guardWidth * 0.5f, qy1 = gy - rightY * guardWidth * 0.5f, qz1 = gz - rightZ * guardWidth * 0.5f;
        float qx2 = gx + rightX * guardWidth * 0.5f, qy2 = gy + rightY * guardWidth * 0.5f, qz2 = gz + rightZ * guardWidth * 0.5f;
        float gThick = bladeThickness * 1.5f;
        drawQuad(matrix, consumer,
            qx1 - normX * gThick, qy1 - normY * gThick, qz1 - normZ * gThick,
            qx2 - normX * gThick, qy2 - normY * gThick, qz2 - normZ * gThick,
            qx2 + normX * gThick, qy2 + normY * gThick, qz2 + normZ * gThick,
            qx1 + normX * gThick, qy1 + normY * gThick, qz1 + normZ * gThick,
            1.0f, 0.85f, 0.20f, fade * 0.95f
        );

        // 3. Handle Grip & Diamond Pommel
        drawBeam(matrix, consumer, hx, hy, hz, gx, gy, gz, bladeThickness * 0.8f, 0.05f, 0.65f, 0.90f, fade * 0.90f);

        // Diamond Pommel Cap
        float px1 = hx - rightX * pommelSize, py1 = hy - rightY * pommelSize, pz1 = hz - rightZ * pommelSize;
        float px2 = hx + rightX * pommelSize, py2 = hy + rightY * pommelSize, pz2 = hz + rightZ * pommelSize;
        drawQuad(matrix, consumer,
            px1 - normX * pommelSize, py1 - normY * pommelSize, pz1 - normZ * pommelSize,
            px2 - normX * pommelSize, py2 - normY * pommelSize, pz2 - normZ * pommelSize,
            px2 + normX * pommelSize, py2 + normY * pommelSize, pz2 + normZ * pommelSize,
            px1 + normX * pommelSize, py1 + normY * pommelSize, pz1 + normZ * pommelSize,
            1.0f, 0.95f, 0.40f, fade
        );

        // 4. Trailing Ethereal Blade Energy Tendril extending backward into sky
        float trailLen = 1.4f * scale;
        float ex = hx - dirX * trailLen, ey = hy - dirY * trailLen + 0.3f, ez = hz - dirZ * trailLen;
        drawBeam(matrix, consumer, hx, hy, hz, ex, ey, ez, 0.08f * scale, 0.0f, 0.90f, 1.0f, fade * 0.65f);
        drawBeam(matrix, consumer, hx, hy, hz, ex, ey, ez, 0.03f * scale, 1.0f, 1.0f, 1.0f, fade * 0.85f);
    }

    private static void drawAscendingHelices(Matrix4f matrix, VertexConsumer consumer, float age, float height, float radius, float fade) {
        int steps = 14;
        for (int h = 0; h < 2; h++) {
            float phase = h * (float) Math.PI;
            for (int s = 0; s < steps; s++) {
                float p1 = s / (float) steps;
                float p2 = (s + 1) / (float) steps;

                float y1 = p1 * height;
                float y2 = p2 * height;

                double a1 = (p1 * Math.PI * 3.0) + (age * 0.25f) + phase;
                double a2 = (p2 * Math.PI * 3.0) + (age * 0.25f) + phase;

                float x1 = (float) Math.cos(a1) * radius;
                float z1 = (float) Math.sin(a1) * radius;
                float x2 = (float) Math.cos(a2) * radius;
                float z2 = (float) Math.sin(a2) * radius;

                float r = h == 0 ? 0.0f : 1.0f;
                float g = h == 0 ? 0.95f : 0.85f;
                float b = h == 0 ? 1.0f : 0.30f;

                drawBeam(matrix, consumer, x1, y1, z1, x2, y2, z2, 0.06f, r, g, b, fade * 0.70f);
            }
        }
    }

    private static void drawCrystalSpike(Matrix4f matrix, VertexConsumer consumer,
                                         float x1, float y1, float z1,
                                         float x2, float y2, float z2,
                                         float tipX, float tipY, float tipZ,
                                         float rBase, float gBase, float bBase, float aBase,
                                         float rTip, float gTip, float bTip, float aTip) {
        // Front
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Back
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }

    private static void drawGroundRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0 + rotRad;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0 + rotRad;

            float x1 = cx + (float) Math.cos(a1) * rOuter;
            float z1 = cz + (float) Math.sin(a1) * rOuter;
            float x2 = cx + (float) Math.cos(a2) * rOuter;
            float z2 = cz + (float) Math.sin(a2) * rOuter;

            float ix1 = cx + (float) Math.cos(a1) * rInner;
            float iz1 = cz + (float) Math.sin(a1) * rInner;
            float ix2 = cx + (float) Math.cos(a2) * rInner;
            float iz2 = cz + (float) Math.sin(a2) * rInner;

            drawQuad(matrix, consumer, ix1, cy, iz1, ix2, cy, iz2, x2, cy, z2, x1, cy, z1, r, g, b, a);
        }
    }

    private static void drawCylinderPillar(Matrix4f matrix, VertexConsumer consumer, float cx, float cz, float radius, float height, int sides, float r, float g, float b, float a) {
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2.0;

            float x1 = cx + (float) Math.cos(a1) * radius;
            float z1 = cz + (float) Math.sin(a1) * radius;
            float x2 = cx + (float) Math.cos(a2) * radius;
            float z2 = cz + (float) Math.sin(a2) * radius;

            consumer.addVertex(matrix, x1, 0.04f, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, 0.04f, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, height, z2).setColor(r, g, b, a * 0.25f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, height, z1).setColor(r, g, b, a * 0.25f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Reverse
            consumer.addVertex(matrix, x1, height, z1).setColor(r, g, b, a * 0.25f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, height, z2).setColor(r, g, b, a * 0.25f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, 0.04f, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, 0.04f, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }

    private static void drawBeam(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float radius, float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * radius;
        float nz = dx / len * radius;
        float ny = radius;

        drawQuad(matrix, consumer, x1 - nx, y1, z1 - nz, x1 + nx, y1, z1 + nz, x2 + nx, y2, z2 + nz, x2 - nx, y2, z2 - nz, r, g, b, a);
        drawQuad(matrix, consumer, x1, y1 - ny, z1, x1, y1 + ny, z1, x2, y2 + ny, z2, x2, y2 - ny, z2, r, g, b, a);
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

    private static void drawQuad(Matrix4f matrix, VertexConsumer consumer,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Reverse
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
