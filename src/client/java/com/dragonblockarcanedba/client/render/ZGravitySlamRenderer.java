package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.ZGravitySlamEntity;
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
 * Entity Renderer for Z-Sword Gravity Slam in Minecraft 26.2.
 * Renders massive radiating 3D tectonic gravity canyon trenches, colossal erupted Katchin monolith slabs,
 * expanding 3D gravitational shockwave dome, and vertical erupting singularity kinetic geysers.
 */
public class ZGravitySlamRenderer extends EntityRenderer<ZGravitySlamEntity, ZGravitySlamRenderer.ZGravitySlamRenderState> {

    public ZGravitySlamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ZGravitySlamRenderState extends EntityRenderState {
        public float radius = 10.0f;
        public float powerRatio = 1.0f;
        public float age = 0;
        public int lifetime = 50;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ZGravitySlamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ZGravitySlamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ZGravitySlamRenderState createRenderState() {
        return new ZGravitySlamRenderState();
    }

    @Override
    public void extractRenderState(ZGravitySlamEntity entity, ZGravitySlamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.powerRatio = entity.getPowerRatio();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(ZGravitySlamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        float power = state.powerRatio;
        float age = state.age;
        float progress = Math.min(1.0f, age / (float) state.lifetime);
        float fade = 1.0f - smooth(progress);
        float grow = smooth(Math.min(1.0f, progress / 0.48f));
        float impact = strike(progress);
        // Late decay factor: monoliths sink and trenches shorten as they fade
        // instead of standing at full height while only their alpha drops.
        float decay = smooth(Math.max(0.0f, (progress - 0.60f) / 0.40f));
        float dist = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        RenderType renderType = KiRenderHelper.kiRenderType();

        float rCol = 1.0f;
        float gCol = 0.82f;
        float bCol = 0.20f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            float shockR = radius * grow;
            float domeH = (2.2f + power * 2.6f) * (0.35f + 0.65f * fade);
            if (fade > 0.04f) {
                drawShockwaveDome(matrix, buffer, 0, 0.05f, 0, shockR, domeH,
                    com.dragonblockarcanedba.util.FxEnvelope.segments(40, dist, 16), rCol, gCol, bCol, fade * 0.5f);
                drawGroundRing(matrix, buffer, 0, 0.04f, 0, shockR, shockR * 0.88f,
                    com.dragonblockarcanedba.util.FxEnvelope.segments(40, dist, 16), rCol, gCol, bCol, fade * 0.72f);
                drawGroundRing(matrix, buffer, 0, 0.07f, 0, shockR * 0.36f, shockR * 0.22f,
                    com.dragonblockarcanedba.util.FxEnvelope.segments(28, dist, 12), 1.0f, 0.96f, 0.72f, fade * (0.55f + 0.45f * impact));
            }

            // Trench endpoints feed the geyser pass below.
            float[] geyserX = new float[5];
            float[] geyserZ = new float[5];
            int geyserCount = 0;

            // 2. Radiating 3D Tectonic Gravity Canyon Fractures / Branching Rupture Trenches
            int trenchCount = 14 + (int) (power * 6); // 14 to 20 trenches
            for (int t = 0; t < trenchCount; t++) {
                double tAngle = (t / (double) trenchCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.25);
                float trenchLen = radius * grow * (1.0f - decay * 0.45f) * (0.75f + rng.nextFloat() * 0.40f);

                float curX = 0, curZ = 0;
                int steps = 6;
                for (int s = 0; s < steps; s++) {
                    float nxtX = curX + (float) Math.cos(tAngle) * (trenchLen / steps) + (rng.nextFloat() - 0.5f) * 0.45f;
                    float nxtZ = curZ + (float) Math.sin(tAngle) * (trenchLen / steps) + (rng.nextFloat() - 0.5f) * 0.45f;

                    float width = (0.35f + power * 0.20f) * (1.0f - (s / (float) steps) * 0.45f);
                    // Violet Gravity Base Trench
                    drawTrenchSegment(matrix, buffer, curX, curZ, nxtX, nxtZ, width, 0.35f, 0.08f, 0.65f, fade * 0.95f);
                    // Glowing Incandescent Golden Core
                    drawTrenchSegment(matrix, buffer, curX, curZ, nxtX, nxtZ, width * 0.40f, 1.0f, 0.95f, 0.45f, fade);

                    curX = nxtX; curZ = nxtZ;
                }
                if (t % 4 == 0 && geyserCount < geyserX.length) {
                    geyserX[geyserCount] = curX;
                    geyserZ[geyserCount] = curZ;
                    geyserCount++;
                }
            }

            // 3. Colossal Erupted 3D Katchin Monolith Slabs & Shattered Earth Crags
            int rockCount = 18 + (int) (power * 10); // 18 to 28 monoliths
            for (int r = 0; r < rockCount; r++) {
                double rockAngle = (r / (double) rockCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.35);
                float rockDist = radius * grow * (0.22f + rng.nextFloat() * 0.70f);

                float rx = (float) Math.cos(rockAngle) * rockDist;
                float rz = (float) Math.sin(rockAngle) * rockDist;

                float maxH = (1.8f + power * 2.2f) + rng.nextFloat() * (2.0f + power * 1.5f);
                // Monoliths sink back into the ground through the decay window.
                float slabHeight = maxH * grow * (1.0f - decay * 0.60f);
                float slabWidth = 0.22f + rng.nextFloat() * 0.16f;

                // Outward leaning angle
                float tiltFactor = 0.30f + rng.nextFloat() * 0.30f;
                float tiltX = (float) Math.cos(rockAngle) * (slabHeight * tiltFactor);
                float tiltZ = (float) Math.sin(rockAngle) * (slabHeight * tiltFactor);

                float tx = -(float) Math.sin(rockAngle) * slabWidth * 0.5f;
                float tz = (float) Math.cos(rockAngle) * slabWidth * 0.5f;

                // 3D Monolith (Dark Katchin top with radiant golden-violet underbelly)
                drawRockMonolith(matrix, buffer,
                    rx - tx, 0.05f, rz - tz,
                    rx + tx, 0.05f, rz + tz,
                    rx + tiltX, slabHeight, rz + tiltZ,
                    0.65f, 0.20f, 0.95f, 0.95f * fade,
                    0.20f, 0.15f, 0.28f, 1.0f * fade
                );
            }

            // 4. Vertical erupting singularity geysers at trench endpoints.
            // drawVerticalGeyser existed for this and was never called, while
            // the class header promised geysers. They erupt on the strike
            // envelope (peak near the damage tick) and are gone by mid life.
            if (impact > 0.03f) {
                float geyserH = (2.5f + power * 3.0f) * impact;
                float geyserW = 0.30f + power * 0.25f;
                for (int g = 0; g < geyserCount; g++) {
                    drawVerticalGeyser(matrix, buffer, geyserX[g], geyserZ[g], geyserH, geyserW, impact * fade);
                }
            }

        });

        // Anchor glow pass: a soft gold disc under the slam so the footprint
        // reads as grounded light, fading with the envelope.
        if (fade > 0.04f) {
            float glowR = radius * grow;
            collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
                com.dragonblockarcanedba.client.render.ki.LayeredRender.drawAnchorGlow(pose, buffer,
                    glowR, 0.03f, rCol, gCol, bCol, fade * 0.30f, dist);
            });
        }
    }

    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    private static float strike(float progress) {
        if (progress >= 0.42f) return 0.0f;
        return (float) Math.sin((progress / 0.42f) * Math.PI);
    }

    private static void drawTrenchSegment(Matrix4f matrix, VertexConsumer consumer, float x1, float z1, float x2, float z2, float width, float r, float g, float b, float a) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * width;
        float nz = dx / len * width;
        consumer.addVertex(matrix, x1 - nx, 0.04f, z1 - nz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx, 0.04f, z1 + nz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx * 0.86f, 0.04f, z2 + nz * 0.86f).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx * 0.86f, 0.04f, z2 - nz * 0.86f).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 - nx * 0.22f, 0.055f, z1 - nz * 0.22f).setColor(1.0f, 0.92f, 0.55f, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx * 0.22f, 0.055f, z1 + nz * 0.22f).setColor(1.0f, 0.92f, 0.55f, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx * 0.16f, 0.055f, z2 + nz * 0.16f).setColor(1.0f, 0.92f, 0.55f, a * 0.7f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx * 0.16f, 0.055f, z2 - nz * 0.16f).setColor(1.0f, 0.92f, 0.55f, a * 0.7f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawGroundRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
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

            consumer.addVertex(matrix, x1Out, cy, z1Out).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy, z2Out).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2In, cy, z2In).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1In, cy, z1In).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawShockwaveDome(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float radius, float height, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = cx + (float) Math.cos(a1) * radius;
            float z1 = cz + (float) Math.sin(a1) * radius;
            float x2 = cx + (float) Math.cos(a2) * radius;
            float z2 = cz + (float) Math.sin(a2) * radius;

            // Outer dome wall
            consumer.addVertex(matrix, x1, cy, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, cy, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2 * 0.35f, cy + height, z2 * 0.35f).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1 * 0.35f, cy + height, z1 * 0.35f).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawRockMonolith(Matrix4f matrix, VertexConsumer consumer,
                                         float x1, float y1, float z1,
                                         float x2, float y2, float z2,
                                         float tipX, float tipY, float tipZ,
                                         float rBase, float gBase, float bBase, float aBase,
                                         float rTip, float gTip, float bTip, float aTip) {
        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        float mz = (z1 + z2) * 0.5f;
        float hx = (x2 - x1) * 0.5f;
        float hy = (y2 - y1) * 0.5f;
        float hz = (z2 - z1) * 0.5f;
        float dx = tipX - mx;
        float dy = tipY - my;
        float dz = tipZ - mz;
        float bx = hy * dz - hz * dy;
        float by = hz * dx - hx * dz;
        float bz = hx * dy - hy * dx;
        float bl = (float) Math.sqrt(bx * bx + by * by + bz * bz);
        if (bl > 0.001f) {
            bx /= bl;
            by /= bl;
            bz /= bl;
        }
        float bowAmp = (float) Math.sqrt(hx * hx + hy * hy + hz * hz) * 0.65f;
        float cr = Math.max(rBase, rTip);
        float cg = Math.max(gBase, gTip);
        float cb = Math.max(bBase, bTip);
        int steps = 6;
        for (int i = 0; i < steps; i++) {
            float t1 = i / (float) steps;
            float t2 = (i + 1) / (float) steps;
            float bow1 = (float) Math.sin(t1 * Math.PI) * bowAmp;
            float bow2 = (float) Math.sin(t2 * Math.PI) * bowAmp;
            float w1 = 1.0f - t1 * 0.8f;
            float w2 = 1.0f - t2 * 0.8f;
            float ax = mx + dx * t1 + bx * bow1;
            float ay = my + dy * t1 + by * bow1;
            float az = mz + dz * t1 + bz * bow1;
            float cx = mx + dx * t2 + bx * bow2;
            float cy = my + dy * t2 + by * bow2;
            float cz = mz + dz * t2 + bz * bow2;
            float ar = rBase + (rTip - rBase) * t1;
            float ag = gBase + (gTip - gBase) * t1;
            float ab = bBase + (bTip - bBase) * t1;
            float aa = aBase + (aTip - aBase) * t1;
            float br = rBase + (rTip - rBase) * t2;
            float bg = gBase + (gTip - gBase) * t2;
            float bb = bBase + (bTip - bBase) * t2;
            float ba = aBase + (aTip - aBase) * t2;
            consumer.addVertex(matrix, ax - hx * w1, ay - hy * w1, az - hz * w1).setColor(ar, ag, ab, aa).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ax + hx * w1, ay + hy * w1, az + hz * w1).setColor(ar, ag, ab, aa).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx + hx * w2, cy + hy * w2, cz + hz * w2).setColor(br, bg, bb, ba).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx - hx * w2, cy - hy * w2, cz - hz * w2).setColor(br, bg, bb, ba).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            float k1 = w1 * 0.2f;
            float k2 = w2 * 0.2f;
            consumer.addVertex(matrix, ax - hx * k1, ay - hy * k1, az - hz * k1).setColor(cr, cg, cb, aa).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ax + hx * k1, ay + hy * k1, az + hz * k1).setColor(cr, cg, cb, aa).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx + hx * k2, cy + hy * k2, cz + hz * k2).setColor(cr, cg, cb, ba).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx - hx * k2, cy - hy * k2, cz - hz * k2).setColor(cr, cg, cb, ba).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawVerticalGeyser(Matrix4f matrix, VertexConsumer consumer, float x, float z, float height, float width, float fade) {
        // Core Golden Column
        consumer.addVertex(matrix, x - width, 0.05f, z).setColor(1.0f, 0.90f, 0.30f, fade * 0.95f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x + width, 0.05f, z).setColor(1.0f, 0.90f, 0.30f, fade * 0.95f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x + width * 0.3f, height, z).setColor(0.60f, 0.15f, 0.95f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x - width * 0.3f, height, z).setColor(0.60f, 0.15f, 0.95f, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Perpendicular Violet Column
        consumer.addVertex(matrix, x, 0.05f, z - width).setColor(1.0f, 0.90f, 0.30f, fade * 0.95f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x, 0.05f, z + width).setColor(1.0f, 0.90f, 0.30f, fade * 0.95f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x, height, z + width * 0.3f).setColor(0.60f, 0.15f, 0.95f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x, height, z - width * 0.3f).setColor(0.60f, 0.15f, 0.95f, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
