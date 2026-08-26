package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.OxChargeEntity;
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
 * Entity Renderer for Ox King's Groundbreaker Charging in Minecraft 26.2.
 * Renders dynamic 3D ground-shatter fracture trenches, levitating vibrating 3D basalt rock debris,
 * glowing magma fissure cores, and inward-tightening earthen compression rings.
 */
public class OxChargeRenderer extends EntityRenderer<OxChargeEntity, OxChargeRenderer.OxChargeRenderState> {

    public OxChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class OxChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
        public long seed = 0;
    }

    @Override
    public boolean shouldRender(OxChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public OxChargeRenderState createRenderState() {
        return new OxChargeRenderState();
    }

    @Override
    public void extractRenderState(OxChargeEntity entity, OxChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(OxChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float charge = state.chargeRatio;
        float age = state.age;

        float baseRadius = 1.2f + (charge * 2.6f);
        float baseAlpha = 0.70f + (charge * 0.30f);
        float tremble = (float) Math.sin(age * 2.5f) * (0.02f + charge * 0.06f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. 3D Dynamic Ground-Shatter Decal: Branching Tectonic Magma Fissure Trenches
            int trenchCount = 8 + (int) (charge * 6); // 8 to 14 trenches
            for (int t = 0; t < trenchCount; t++) {
                double tAngle = (t / (double) trenchCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.25);
                float trenchLen = baseRadius * (0.65f + rng.nextFloat() * 0.45f);

                float curX = 0, curZ = 0;
                int steps = 4;
                for (int s = 0; s < steps; s++) {
                    float nxtX = curX + (float) Math.cos(tAngle) * (trenchLen / steps) + (rng.nextFloat() - 0.5f) * 0.35f;
                    float nxtZ = curZ + (float) Math.sin(tAngle) * (trenchLen / steps) + (rng.nextFloat() - 0.5f) * 0.35f;

                    float width = 0.22f * (1.0f - (s / (float) steps) * 0.5f) * (0.6f + charge * 0.5f);
                    // Magma Base Trench
                    drawTrenchSegment(matrix, buffer, curX, curZ, nxtX, nxtZ, width, 0.95f, 0.25f, 0.02f, baseAlpha * 0.95f);
                    // Blazing Molten Core
                    drawTrenchSegment(matrix, buffer, curX, curZ, nxtX, nxtZ, width * 0.35f, 1.0f, 0.85f, 0.20f, baseAlpha);

                    curX = nxtX; curZ = nxtZ;
                }
            }

            // 2. Inward-Gathering Earthen Compression Rings
            int ringSegments = 28;
            drawRotatingRing(matrix, buffer, 0, 0.03f, 0, baseRadius * 1.05f, baseRadius * 0.92f, ringSegments, age * 12.0f,
                0.90f, 0.35f, 0.02f, baseAlpha * 0.80f);
            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, baseRadius * 0.70f, baseRadius * 0.58f, ringSegments, age * -18.0f,
                1.0f, 0.65f, 0.05f, baseAlpha * 0.90f);

            // 3. Physical Levitating and Vibrating 3D Rock Debris / Basalt Slabs
            int rockCount = 10 + (int) (charge * 8); // 10 to 18 rocks
            for (int r = 0; r < rockCount; r++) {
                double rockAngle = (r / (double) rockCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.4);
                float rockDist = 0.6f + rng.nextFloat() * (baseRadius * 0.85f);

                float rx = (float) Math.cos(rockAngle) * rockDist + (rng.nextFloat() - 0.5f) * tremble;
                float rz = (float) Math.sin(rockAngle) * rockDist + (rng.nextFloat() - 0.5f) * tremble;

                // Floating elevation scaling with charge
                float floatPhase = age * 0.25f + r * 1.4f;
                float ry = 0.08f + (charge * 1.4f) * (0.5f + 0.5f * (float) Math.sin(floatPhase)) + (rng.nextFloat() * 0.2f * charge);

                float slabHeight = 0.25f + rng.nextFloat() * 0.35f + charge * 0.25f;
                float slabWidth = 0.20f + rng.nextFloat() * 0.25f;

                // Leaning angle in 3D
                float tiltX = (float) Math.cos(rockAngle) * (0.15f + charge * 0.2f);
                float tiltZ = (float) Math.sin(rockAngle) * (0.15f + charge * 0.2f);

                float tx = -(float) Math.sin(rockAngle) * slabWidth * 0.5f;
                float tz = (float) Math.cos(rockAngle) * slabWidth * 0.5f;

                // 3D Rock Chunk Geometry (dark basalt top with fiery molten underbelly)
                drawRockChunk(matrix, buffer,
                    rx - tx, ry, rz - tz,
                    rx + tx, ry, rz + tz,
                    rx + tiltX, ry + slabHeight, rz + tiltZ,
                    1.0f, 0.40f, 0.05f, baseAlpha * 0.95f,
                    0.25f, 0.18f, 0.12f, baseAlpha
                );
            }

            // 4. Ascending Earthen Ki Heat Tendrils
            if (charge >= 0.25f) {
                int tendrils = 6 + (int) (charge * 6);
                for (int i = 0; i < tendrils; i++) {
                    double tAng = (i / (double) tendrils) * Math.PI * 2.0 + (age * 0.08);
                    float tDist = 0.4f + (float) (rng.nextDouble() * 0.8);
                    float tx1 = (float) Math.cos(tAng) * tDist;
                    float tz1 = (float) Math.sin(tAng) * tDist;
                    float tHeight = 0.5f + charge * 1.5f;

                    drawAscendingTendril(matrix, buffer, tx1, 0.05f, tz1, tx1 * 0.3f, tHeight, tz1 * 0.3f,
                        0.06f, 1.0f, 0.55f, 0.05f, baseAlpha * 0.75f);
                }
            }
        });

        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawTrenchSegment(Matrix4f matrix, VertexConsumer consumer, float x1, float z1, float x2, float z2, float width, float r, float g, float b, float a) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * width;
        float nz = dx / len * width;

        consumer.addVertex(matrix, x1 - nx, 0.04f, z1 - nz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx, 0.04f, z1 + nz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx, 0.04f, z2 + nz).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx, 0.04f, z2 - nz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawRotatingRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = rotRad + (i / (double) segments) * Math.PI * 2.0;
            double a2 = rotRad + ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1Out = cx + (float) Math.cos(a1) * rOuter;
            float z1Out = cz + (float) Math.sin(a1) * rOuter;
            float x2Out = cx + (float) Math.cos(a2) * rOuter;
            float z2Out = cz + (float) Math.sin(a2) * rOuter;

            float x1In = cx + (float) Math.cos(a1) * rInner;
            float z1In = cz + (float) Math.sin(a1) * rInner;
            float x2In = cx + (float) Math.cos(a2) * rInner;
            float z2In = cz + (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, x1Out, cy, z1Out).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy, z2Out).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2In, cy, z2In).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1In, cy, z1In).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawRockChunk(Matrix4f matrix, VertexConsumer consumer,
                                      float x1, float y1, float z1,
                                      float x2, float y2, float z2,
                                      float tipX, float tipY, float tipZ,
                                      float rBase, float gBase, float bBase, float aBase,
                                      float rTip, float gTip, float bTip, float aTip) {
        // Front Triangular Face
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Back Triangular Face (inverted normal)
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawAscendingTendril(Matrix4f matrix, VertexConsumer consumer,
                                             float x1, float y1, float z1,
                                             float x2, float y2, float z2,
                                             float width, float r, float g, float b, float a) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001f) {
            dx = 1; dz = 0; len = 1;
        }
        float nx = -dz / len * width;
        float nz = dx / len * width;

        consumer.addVertex(matrix, x1 - nx, y1, z1 - nz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx, y1, z1 + nz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx * 0.2f, y2, z2 + nz * 0.2f).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx * 0.2f, y2, z2 - nz * 0.2f).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
