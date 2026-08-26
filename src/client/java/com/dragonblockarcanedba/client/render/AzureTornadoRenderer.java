package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.AzureTornadoEntity;
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
 * Entity Renderer for Azure Tornado in Minecraft 26.2.
 * Renders physical 3D multi-layered swirling vortex funnels, ascending helical wind ribbons, ground suction discs, and orbital kinetic wind shards.
 */
public class AzureTornadoRenderer extends EntityRenderer<AzureTornadoEntity, AzureTornadoRenderer.TornadoRenderState> {

    public AzureTornadoRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TornadoRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public boolean isGiant = false;
        public float age = 0;
    }

    @Override
    public boolean shouldRender(AzureTornadoEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public TornadoRenderState createRenderState() {
        return new TornadoRenderState();
    }

    @Override
    public void extractRenderState(AzureTornadoEntity entity, TornadoRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getScale();
        state.isGiant = entity.isGiant();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(TornadoRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float scale = state.scale;
        boolean giant = state.isGiant;
        float baseRadius = scale * (giant ? 7.0f : 3.0f);
        float totalHeight = scale * (giant ? 16.0f : 6.5f);
        float age = state.age;

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Ground Whirlpool Suction Discs (Base vortex)
            int groundSegments = 24;
            float gRadius = baseRadius * 0.9f;
            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, gRadius, gRadius * 0.6f, groundSegments, age * 35.0f, 0.0f, 0.9f, 1.0f, giant ? 0.70f : 0.50f);
            drawRotatingRing(matrix, buffer, 0, 0.10f, 0, gRadius * 0.75f, gRadius * 0.3f, groundSegments, -age * 45.0f, 0.2f, 1.0f, 0.9f, giant ? 0.60f : 0.40f);

            // 2. High-Speed Inner Core Funnel (Dense inverted vortex cone)
            int coreLevels = 8;
            int coreSegments = 16;
            float coreRot = age * 28.0f * (float) (Math.PI / 180.0);

            for (int lvl = 0; lvl < coreLevels; lvl++) {
                float p1 = lvl / (float) coreLevels;
                float p2 = (lvl + 1) / (float) coreLevels;

                float y1 = p1 * totalHeight;
                float y2 = p2 * totalHeight;

                float r1 = (baseRadius * 0.35f) * (0.25f + 0.75f * p1);
                float r2 = (baseRadius * 0.35f) * (0.25f + 0.75f * p2);

                float alpha = (giant ? 0.75f : 0.55f) * (1.0f - p1 * 0.2f);

                for (int i = 0; i < coreSegments; i++) {
                    double a1 = ((i / (double) coreSegments) * Math.PI * 2.0) + coreRot + (p1 * 1.2);
                    double a2 = (((i + 1) / (double) coreSegments) * Math.PI * 2.0) + coreRot + (p1 * 1.2);
                    double a3 = (((i + 1) / (double) coreSegments) * Math.PI * 2.0) + coreRot + (p2 * 1.2);
                    double a4 = ((i / (double) coreSegments) * Math.PI * 2.0) + coreRot + (p2 * 1.2);

                    float x1 = (float) Math.cos(a1) * r1;
                    float z1 = (float) Math.sin(a1) * r1;
                    float x2 = (float) Math.cos(a2) * r1;
                    float z2 = (float) Math.sin(a2) * r1;

                    float x3 = (float) Math.cos(a3) * r2;
                    float z3 = (float) Math.sin(a3) * r2;
                    float x4 = (float) Math.cos(a4) * r2;
                    float z4 = (float) Math.sin(a4) * r2;

                    drawQuad(matrix, buffer, x1, y1, z1, x2, y1, z2, x3, y2, z3, x4, y2, z4, 0.7f, 1.0f, 1.0f, alpha);
                }
            }

            // 3. Multi-Strand Outer Helical Wind Ribbons (3 ascending spiral bands)
            int strands = 3;
            int ribbonSteps = 16;
            for (int s = 0; s < strands; s++) {
                float strandOffset = (s / (float) strands) * (float) (Math.PI * 2.0);

                for (int step = 0; step < ribbonSteps; step++) {
                    float t1 = step / (float) ribbonSteps;
                    float t2 = (step + 1) / (float) ribbonSteps;

                    float y1 = t1 * totalHeight;
                    float y2 = t2 * totalHeight;

                    // Flaring outward with altitude
                    float r1 = baseRadius * (0.3f + 0.7f * t1 * t1);
                    float r2 = baseRadius * (0.3f + 0.7f * t2 * t2);

                    double ang1 = strandOffset + (age * 18.0f * (Math.PI / 180.0)) + (t1 * Math.PI * 3.5);
                    double ang2 = strandOffset + (age * 18.0f * (Math.PI / 180.0)) + (t2 * Math.PI * 3.5);

                    float x1 = (float) Math.cos(ang1) * r1;
                    float z1 = (float) Math.sin(ang1) * r1;
                    float x2 = (float) Math.cos(ang2) * r2;
                    float z2 = (float) Math.sin(ang2) * r2;

                    float bandWidth1 = 0.35f * scale * (0.5f + t1 * 0.8f);
                    float bandWidth2 = 0.35f * scale * (0.5f + t2 * 0.8f);

                    float tx1 = (float) -Math.sin(ang1) * bandWidth1;
                    float tz1 = (float) Math.cos(ang1) * bandWidth1;
                    float tx2 = (float) -Math.sin(ang2) * bandWidth2;
                    float tz2 = (float) Math.cos(ang2) * bandWidth2;

                    drawQuad(matrix, buffer,
                        x1 - tx1, y1, z1 - tz1,
                        x1 + tx1, y1, z1 + tz1,
                        x2 + tx2, y2, z2 + tz2,
                        x2 - tx2, y2, z2 - tz2,
                        0.0f, 0.90f, 1.0f, giant ? 0.70f : 0.50f
                    );
                }
            }

            // 4. Physical Orbiting Wind Blades / Kinetic Shards
            int shardCount = giant ? 12 : 6;
            for (int i = 0; i < shardCount; i++) {
                float shardProgress = (i / (float) shardCount);
                float shardY = shardProgress * totalHeight * 0.9f + 0.5f;
                float shardRadius = baseRadius * (0.4f + 0.6f * (shardY / totalHeight));

                double shardAngle = (shardProgress * Math.PI * 2.0) + (age * (25.0f + i * 4.0f) * (Math.PI / 180.0));

                float sx = (float) Math.cos(shardAngle) * shardRadius;
                float sz = (float) Math.sin(shardAngle) * shardRadius;

                // Tangent orientation
                float tx = (float) -Math.sin(shardAngle);
                float tz = (float) Math.cos(shardAngle);

                float sLen = 0.65f * scale;
                float sWidth = 0.18f * scale;

                float tipX = sx + tx * (sLen * 0.6f);
                float tipZ = sz + tz * (sLen * 0.6f);
                float tailX = sx - tx * (sLen * 0.4f);
                float tailZ = sz - tz * (sLen * 0.4f);

                float nx = -tz * sWidth;
                float nz = tx * sWidth;

                drawTriangle(matrix, buffer,
                    tailX - nx, shardY, tailZ - nz,
                    tailX + nx, shardY, tailZ + nz,
                    tipX, shardY + 0.1f * scale, tipZ,
                    0.4f, 1.0f, 0.95f, giant ? 0.85f : 0.65f
                );
            }

            // 5. Crown Outflow Ring (Flared upper storm rim)
            float crownY = totalHeight;
            float crownR = baseRadius * 1.15f;
            drawRotatingRing(matrix, buffer, 0, crownY, 0, crownR, crownR * 0.75f, 20, -age * 22.0f, 0.0f, 0.8f, 1.0f, giant ? 0.60f : 0.40f);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawRotatingRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        double rotRad = rotDeg * (Math.PI / 180.0);
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2.0) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2.0) + rotRad;

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
