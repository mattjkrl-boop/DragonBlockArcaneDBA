package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.OxChargeEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
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

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(OxChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(OxChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
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

        float chargeEase = charge * charge * (3.0f - 2.0f * charge);
        float baseRadius = 1.2f + (chargeEase * 2.6f);
        // Lifecycle (point 5): the charge visual used to appear at full alpha
        // on frame one. It now builds in over the first 6 ticks.
        float fadeIn = FxEnvelope.smoothstep(Math.min(1.0f, age / 6.0f));
        float baseAlpha = (0.70f + (chargeEase * 0.30f)) * fadeIn;
        float tremble = (float) Math.sin(age * 2.5f) * (0.02f + charge * 0.06f);
        float distScale = FxEnvelope.distScale(Math.sqrt(state.distanceToCameraSq));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. 3D Dynamic Ground-Shatter Decal: Branching Tectonic Magma Fissure Trenches
            int trenchCount = FxEnvelope.segments(8 + (int) (charge * 6), distScale, 6); // 8 to 14 trenches near
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

            // One compression ring: dark basalt lip on the outside, molten gold along the inner edge.
            int ringSegments = FxEnvelope.segments(40, distScale, 16);
            drawRotatingRing(matrix, buffer, 0, 0.03f, 0, baseRadius * 1.04f, baseRadius * 0.90f, ringSegments, age * 8.0f,
                0.55f, 0.16f, 0.02f, baseAlpha * 0.85f);
            drawRotatingRing(matrix, buffer, 0, 0.045f, 0, baseRadius * 0.93f, baseRadius * 0.88f, ringSegments, age * 8.0f,
                1.0f, 0.78f, 0.18f, baseAlpha * 0.95f);

            // Tapered heart of the brace. It rises with the charge and thins out overhead.
            int colSteps = 6;
            int colSides = 8;
            float colH = 0.35f + chargeEase * 2.4f;
            float colSpin = age * 0.35f;
            for (int s = 0; s < colSteps; s++) {
                float p1 = s / (float) colSteps;
                float p2 = (s + 1) / (float) colSteps;
                float y1 = 0.04f + p1 * colH;
                float y2 = 0.04f + p2 * colH;
                float rad1 = (0.16f + chargeEase * 0.22f) * (1.0f - p1 * 0.88f);
                float rad2 = (0.16f + chargeEase * 0.22f) * (1.0f - p2 * 0.88f);
                float a1 = baseAlpha * (0.85f - p1 * 0.75f);
                float a2 = baseAlpha * (0.85f - p2 * 0.75f);
                for (int i = 0; i < colSides; i++) {
                    double ang1 = colSpin + (i / (double) colSides) * Math.PI * 2.0;
                    double ang2 = colSpin + ((i + 1) / (double) colSides) * Math.PI * 2.0;
                    float x1 = (float) Math.cos(ang1) * rad1;
                    float z1 = (float) Math.sin(ang1) * rad1;
                    float x2 = (float) Math.cos(ang2) * rad1;
                    float z2 = (float) Math.sin(ang2) * rad1;
                    float x3 = (float) Math.cos(ang2) * rad2;
                    float z3 = (float) Math.sin(ang2) * rad2;
                    float x4 = (float) Math.cos(ang1) * rad2;
                    float z4 = (float) Math.sin(ang1) * rad2;
                    buffer.addVertex(matrix, x1, y1, z1).setColor(1.0f, 0.55f, 0.08f, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                    buffer.addVertex(matrix, x2, y1, z2).setColor(1.0f, 0.55f, 0.08f, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                    buffer.addVertex(matrix, x3, y2, z3).setColor(1.0f, 0.95f, 0.45f, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                    buffer.addVertex(matrix, x4, y2, z4).setColor(1.0f, 0.95f, 0.45f, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                }
            }

            // 3. Physical Levitating and Tumbling 3D Rock Debris / Basalt Slabs.
            // The chunks used to only bob on a sine and lean at a fixed angle;
            // each one now also tumbles: its base orientation and tip lean
            // rotate with age on its own phase, so the ring reads as debris
            // churning in the charge field, not a fixed formation.
            int rockCount = FxEnvelope.segments(10 + (int) (charge * 8), distScale, 6); // 10 to 18 rocks near
            for (int r = 0; r < rockCount; r++) {
                double rockAngle = (r / (double) rockCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.4);
                float rockDist = 0.6f + rng.nextFloat() * (baseRadius * 0.85f);

                float rx = (float) Math.cos(rockAngle) * rockDist + (rng.nextFloat() - 0.5f) * tremble;
                float rz = (float) Math.sin(rockAngle) * rockDist + (rng.nextFloat() - 0.5f) * tremble;

                // Floating elevation scaling with charge
                float floatPhase = age * 0.25f + r * 1.4f;
                float ry = 0.08f + (chargeEase * 1.4f) * (0.5f + 0.5f * (float) Math.sin(floatPhase)) + (rng.nextFloat() * 0.2f * chargeEase);

                float slabHeight = 0.25f + rng.nextFloat() * 0.35f + charge * 0.25f;
                float slabWidth = 0.20f + rng.nextFloat() * 0.25f;

                // Tumble: rotate the chunk's footprint and lean with age.
                double spin = age * 0.55 + r * 2.1;
                float baseTx = -(float) Math.sin(rockAngle) * slabWidth * 0.5f;
                float baseTz = (float) Math.cos(rockAngle) * slabWidth * 0.5f;
                float tx = (float) (baseTx * Math.cos(spin) - baseTz * Math.sin(spin));
                float tz = (float) (baseTx * Math.sin(spin) + baseTz * Math.cos(spin));
                float leanMag = 0.15f + charge * 0.2f;
                double leanAng = rockAngle + age * 0.4 + r * 1.3;
                float tiltX = (float) Math.cos(leanAng) * leanMag;
                float tiltZ = (float) Math.sin(leanAng) * leanMag;

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
                    float tHeight = 0.5f + chargeEase * 1.5f;
                    float bow = 0.28f + chargeEase * 0.20f;
                    float mx = tx1 * 0.62f + (float) Math.cos(tAng + 1.15) * bow;
                    float mz = tz1 * 0.62f + (float) Math.sin(tAng + 1.15) * bow;
                    float my = tHeight * 0.48f;
                    drawLash(matrix, buffer,
                        tx1, 0.05f, tz1,
                        mx, my, mz,
                        tx1 * 0.22f, tHeight, tz1 * 0.22f,
                        0.07f, 6,
                        1.0f, 0.42f, 0.05f,
                        1.0f, 0.92f, 0.40f,
                        baseAlpha * 0.80f
                    );
                }
            }
        });

        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawLash(Matrix4f matrix, VertexConsumer consumer,
                                 float x0, float y0, float z0,
                                 float xc, float yc, float zc,
                                 float x1, float y1, float z1,
                                 float width, int steps,
                                 float er, float eg, float eb,
                                 float cr, float cg, float cb,
                                 float alpha) {
        int n = Math.max(4, steps);
        float px = x0;
        float py = y0;
        float pz = z0;
        for (int i = 1; i <= n; i++) {
            float t = i / (float) n;
            float u = 1.0f - t;
            float x = u * u * x0 + 2.0f * u * t * xc + t * t * x1;
            float y = u * u * y0 + 2.0f * u * t * yc + t * t * y1;
            float z = u * u * z0 + 2.0f * u * t * zc + t * t * z1;
            float w = width * (1.0f - t * 0.9f);
            float a = alpha * (1.0f - t * t);
            lashSpan(matrix, consumer, px, py, pz, x, y, z, w, er, eg, eb, a);
            lashSpan(matrix, consumer, px, py, pz, x, y, z, w * 0.32f, cr, cg, cb, Math.min(1.0f, a * 1.2f));
            px = x;
            py = y;
            pz = z;
        }
    }

    private static void lashSpan(Matrix4f matrix, VertexConsumer consumer,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float width,
                                 float r, float g, float b, float a) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float ax = 0.0f;
        float ay = 1.0f;
        float az = 0.0f;
        if (dx * dx + dz * dz < dy * dy) {
            ax = 1.0f;
            ay = 0.0f;
            az = 0.0f;
        }
        float px = dy * az - dz * ay;
        float py = dz * ax - dx * az;
        float pz = dx * ay - dy * ax;
        float plen = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (plen < 0.0001f) {
            return;
        }
        float s = width / plen;
        px *= s;
        py *= s;
        pz *= s;
        consumer.addVertex(matrix, x0 - px, y0 - py, z0 - pz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x0 + px, y0 + py, z0 + pz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 + px, y1 + py, z1 + pz).setColor(r, g, b, a * 0.65f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 - px, y1 - py, z1 - pz).setColor(r, g, b, a * 0.65f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 - px, y1 - py, z1 - pz).setColor(r, g, b, a * 0.65f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x1 + px, y1 + py, z1 + pz).setColor(r, g, b, a * 0.65f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x0 + px, y0 + py, z0 + pz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x0 - px, y0 - py, z0 - pz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
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
                                             float width, float r, float g, float b, float a, float aEnd) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001f) {
            dx = 1; dz = 0; len = 1;
        }
        float nx = -dz / len * width;
        float nz = dx / len * width;

        consumer.addVertex(matrix, x1 - nx, y1, z1 - nz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx, y1, z1 + nz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx * 0.2f, y2, z2 + nz * 0.2f).setColor(r, g, b, aEnd).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx * 0.2f, y2, z2 - nz * 0.2f).setColor(r, g, b, aEnd).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
