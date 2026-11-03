package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DevilSlamShockwaveEntity;
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
 * Entity Renderer for Devil Slam Shockwave in Minecraft 26.2.
 * Renders physical 3D demonic ground-shatter fissure trenches, erupting jagged brimstone/obsidian spikes,
 * expanding shockwave demonic seals, and surging vertical hellfire eruption pillars.
 */
public class DevilSlamShockwaveRenderer extends EntityRenderer<DevilSlamShockwaveEntity, DevilSlamShockwaveRenderer.DevilSlamRenderState> {

    public DevilSlamShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DevilSlamRenderState extends EntityRenderState {
        public float radius = 5.0f;
        public float age = 0;
        public int lifetime = 35;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DevilSlamShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DevilSlamShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DevilSlamRenderState createRenderState() {
        return new DevilSlamRenderState();
    }

    @Override
    public void extractRenderState(DevilSlamShockwaveEntity entity, DevilSlamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(DevilSlamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        final float radius = state.radius;
        final float age = state.age;
        final float lifeT = Math.min(1.0f, age / (float) state.lifetime);
        final float fade = 1.0f - lifeT * lifeT * (3.0f - 2.0f * lifeT);
        final long seed = state.seed;
        final float growT = Math.min(1.0f, age / 10.0f);
        final float grow = 1.0f - (1.0f - growT) * (1.0f - growT) * (1.0f - growT);
        final float shockR = radius * 1.35f * grow;
        final float pillarT = Math.min(1.0f, age / 18.0f);
        final float pillarEase = pillarT * pillarT * (3.0f - 2.0f * pillarT);
        final float pillarAlpha = (1.0f - pillarEase) * (1.0f - pillarEase);
        final float riseT = Math.min(1.0f, age / 5.5f);
        final float pillarHeight = 6.7f * (1.0f - (1.0f - riseT) * (1.0f - riseT));
        final float pillarRadius = 0.72f * (1.0f + pillarEase * 0.28f);
        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(seed);

            // Seal: dark body, hot hairline edge, separate core, a ring that keeps traveling, barbed teeth.
            if (fade > 0.04f) {
                drawGroundRing(matrix, buffer, 0, 0.03f, 0, shockR, shockR * 0.9f, 48, 0.42f, 0.0f, 0.05f, fade * 0.82f);
                drawGroundRing(matrix, buffer, 0, 0.055f, 0, shockR * 1.02f, shockR * 0.965f, 56, 1.0f, 0.22f, 0.1f, fade);
                drawGroundRing(matrix, buffer, 0, 0.045f, 0, shockR * 0.62f, shockR * 0.46f, 36, 0.78f, 0.02f, 0.07f, fade * 0.8f);
                drawGroundRing(matrix, buffer, 0, 0.07f, 0, Math.max(0.2f, shockR * 0.16f), 0.0f, 20, 1.0f, 0.2f, 0.08f, fade);
                drawGroundRing(matrix, buffer, 0, 0.09f, 0, Math.max(0.08f, shockR * 0.055f), 0.0f, 12, 1.0f, 0.48f, 0.22f, fade);
                float followT = Math.min(1.0f, Math.max(0.0f, (age - 5.0f) / 14.0f));
                float followEase = followT * followT * (3.0f - 2.0f * followT);
                float followR = radius * (1.2f + 0.9f * followEase);
                float followA = fade * (1.0f - followEase) * (1.0f - followEase);
                if (followA > 0.03f) {
                    drawGroundRing(matrix, buffer, 0, 0.04f, 0, followR, followR * 0.9f, 44, 0.7f, 0.02f, 0.06f, followA);
                }
                int teeth = 12;
                for (int i = 0; i < teeth; i++) {
                    double tAng = (i / (double) teeth) * Math.PI * 2.0 + (age * 0.02);
                    float tx = (float) Math.cos(tAng) * (shockR * 0.92f);
                    float tz = (float) Math.sin(tAng) * (shockR * 0.92f);
                    drawCrystalSpike(matrix, buffer, tx, 0.02f, tz, (float) tAng, 0.46f * grow, 0.07f, fade);
                }
            }

            // Curved fissures: floor, hot core, two walls, and a lip on each wall. The tail keeps curling after the trench ends.
            int trenchCount = 9;
            float open = Math.min(1.0f, age * 0.35f);
            for (int t = 0; t < trenchCount; t++) {
                double tAngle = (t / (double) trenchCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.25);
                float trenchLen = radius * (0.75f + rng.nextFloat() * 0.50f);
                float bend = (rng.nextFloat() - 0.5f) * 1.2f;
                int steps = 7;
                for (int s = 0; s < steps; s++) {
                    float a0 = s / (float) steps;
                    float a1 = (s + 1) / (float) steps;
                    double ang0 = tAngle + bend * a0 * a0;
                    double ang1 = tAngle + bend * a1 * a1;
                    float x0 = (float) Math.cos(ang0) * trenchLen * a0;
                    float z0 = (float) Math.sin(ang0) * trenchLen * a0;
                    float x1 = (float) Math.cos(ang1) * trenchLen * a1;
                    float z1 = (float) Math.sin(ang1) * trenchLen * a1;
                    float w = 0.42f * (1.0f - a0) * open;
                    drawTrenchVolume(matrix, buffer, x0, z0, x1, z1, w, fade * (1.0f - a0 * 0.3f));
                }
                for (int s = 0; s < 3; s++) {
                    float a0 = 1.0f + s * 0.14f;
                    float a1 = 1.0f + (s + 1) * 0.14f;
                    double ang0 = tAngle + bend * a0 * a0;
                    double ang1 = tAngle + bend * a1 * a1;
                    float x0 = (float) Math.cos(ang0) * trenchLen * a0;
                    float z0 = (float) Math.sin(ang0) * trenchLen * a0;
                    float x1 = (float) Math.cos(ang1) * trenchLen * a1;
                    float z1 = (float) Math.sin(ang1) * trenchLen * a1;
                    float tail = 1.0f - (s + 1) / 3.0f;
                    drawTrenchVolume(matrix, buffer, x0, z0, x1, z1, 0.14f * tail * open, fade * tail * tail);
                }
            }

            // Faceted spikes: shell, inner core, ridge edges, one hook, and a streak that eases off the tip.
            int spikeCount = 14;
            for (int r = 0; r < spikeCount; r++) {
                double spikeAngle = (r / (double) spikeCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.35);
                float spikeDist = radius * (0.30f + rng.nextFloat() * 0.65f);
                float rx = (float) Math.cos(spikeAngle) * spikeDist;
                float rz = (float) Math.sin(spikeAngle) * spikeDist;
                float spikeRiseT = Math.min(1.0f, age / 8.0f);
                float rise = 1.0f - (1.0f - spikeRiseT) * (1.0f - spikeRiseT) * (1.0f - spikeRiseT);
                float sinkRaw = lifeT < 0.5f ? 0.0f : (lifeT - 0.5f) / 0.5f;
                float sink = sinkRaw * sinkRaw * (3.0f - 2.0f * sinkRaw);
                float spikeHeight = (0.85f + rng.nextFloat() * 1.15f) * rise * (1.0f - sink);
                float spikeWidth = 0.2f + rng.nextFloat() * 0.14f;
                drawCrystalSpike(matrix, buffer, rx, 0.02f, rz, (float) spikeAngle, spikeHeight, spikeWidth, fade);
            }

            // Hellfire column eases out instead of cutting off. Core, sheath, and a winding edge.
            if (pillarAlpha > 0.025f && pillarHeight > 0.15f) {
                drawCylinderPillar(matrix, buffer, 0, 0, pillarRadius, pillarHeight, 12, 0.55f, 0.0f, 0.06f, pillarAlpha * 0.55f);
                drawCylinderPillar(matrix, buffer, 0, 0, pillarRadius * 0.48f, pillarHeight * 1.02f, 8, 0.85f, 0.04f, 0.08f, pillarAlpha * 0.8f);
                drawCylinderPillar(matrix, buffer, 0, 0, pillarRadius * 0.16f, pillarHeight * 1.08f, 6, 1.0f, 0.28f, 0.12f, pillarAlpha);
                drawColumnHelix(matrix, buffer, pillarRadius * 0.86f, pillarHeight, age, pillarAlpha);
            }
        });

        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            if (fade > 0.04f) {
                drawGroundRing(matrix, buffer, 0, 0.08f, 0, Math.max(0.2f, shockR * 0.16f), 0.0f, 18, 1.0f, 0.18f, 0.06f, fade * 0.28f);
                float followT = Math.min(1.0f, Math.max(0.0f, (age - 5.0f) / 14.0f));
                float followEase = followT * followT * (3.0f - 2.0f * followT);
                float followA = fade * (1.0f - followEase) * (1.0f - followEase) * 0.22f;
                if (followA > 0.02f) {
                    float followR = radius * (1.2f + 0.9f * followEase);
                    drawGroundRing(matrix, buffer, 0, 0.05f, 0, followR, followR * 0.92f, 40, 0.9f, 0.08f, 0.08f, followA);
                }
            }
            if (pillarAlpha > 0.025f && pillarHeight > 0.15f) {
                drawCylinderPillar(matrix, buffer, 0, 0, pillarRadius * 0.22f, pillarHeight * 1.05f, 6, 1.0f, 0.22f, 0.1f, pillarAlpha * 0.35f);
            }
        });
    }

    private static void put(Matrix4f matrix, VertexConsumer consumer, float x, float y, float z, float r, float g, float b, float a) {
        consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    /** Both windings. The near edge uses the first color, the far edge the second. */
    private static void quadGrad(Matrix4f matrix, VertexConsumer consumer,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 float x3, float y3, float z3, float x4, float y4, float z4,
                                 float r1, float g1, float b1, float a1, float r2, float g2, float b2, float a2) {
        if (a1 < 0.012f && a2 < 0.012f) return;
        put(matrix, consumer, x1, y1, z1, r1, g1, b1, a1);
        put(matrix, consumer, x2, y2, z2, r1, g1, b1, a1);
        put(matrix, consumer, x3, y3, z3, r2, g2, b2, a2);
        put(matrix, consumer, x4, y4, z4, r2, g2, b2, a2);
        put(matrix, consumer, x1, y1, z1, r1, g1, b1, a1);
        put(matrix, consumer, x4, y4, z4, r2, g2, b2, a2);
        put(matrix, consumer, x3, y3, z3, r2, g2, b2, a2);
        put(matrix, consumer, x2, y2, z2, r1, g1, b1, a1);
    }

    /** Three-sided taper. A zero end girth closes to a point. */
    private static void drawTaper(Matrix4f matrix, VertexConsumer consumer,
                                  float x0, float y0, float z0, float x1, float y1, float z1,
                                  float g0, float g1,
                                  float r0, float gc0, float b0, float a0, float r1, float gc1, float b1, float a1) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.0008f) return;
        float ux = dx / len, uy = dy / len, uz = dz / len;
        float hx = 0f, hy = 1f, hz = 0f;
        if (Math.abs(uy) > 0.85f) {
            hx = 1f;
            hy = 0f;
        }
        float sx = uy * hz - uz * hy;
        float sy = uz * hx - ux * hz;
        float sz = ux * hy - uy * hx;
        float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl < 1.0E-4f) return;
        sx /= sl;
        sy /= sl;
        sz /= sl;
        float tx = uy * sz - uz * sy;
        float ty = uz * sx - ux * sz;
        float tz = ux * sy - uy * sx;
        int sides = 3;
        for (int s = 0; s < sides; s++) {
            double ang0 = (s / (double) sides) * Math.PI * 2.0;
            double ang1 = ((s + 1) / (double) sides) * Math.PI * 2.0;
            float c0 = (float) Math.cos(ang0), s0 = (float) Math.sin(ang0);
            float c1 = (float) Math.cos(ang1), s1 = (float) Math.sin(ang1);
            quadGrad(matrix, consumer,
                x0 + (sx * c0 + tx * s0) * g0, y0 + (sy * c0 + ty * s0) * g0, z0 + (sz * c0 + tz * s0) * g0,
                x0 + (sx * c1 + tx * s1) * g0, y0 + (sy * c1 + ty * s1) * g0, z0 + (sz * c1 + tz * s1) * g0,
                x1 + (sx * c1 + tx * s1) * g1, y1 + (sy * c1 + ty * s1) * g1, z1 + (sz * c1 + tz * s1) * g1,
                x1 + (sx * c0 + tx * s0) * g1, y1 + (sy * c0 + ty * s0) * g1, z1 + (sz * c0 + tz * s0) * g1,
                r0, gc0, b0, a0, r1, gc1, b1, a1);
        }
    }

    /** Floor, core, walls, and a bright lip. Not a pair of flat ribbons. */
    private static void drawTrenchVolume(Matrix4f matrix, VertexConsumer consumer, float x1, float z1, float x2, float z2, float width, float fade) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001f || width < 0.012f || fade < 0.02f) return;
        float nx = -dz / len;
        float nz = dx / len;
        float floor = 0.03f;
        float lip = 0.1f + width * 0.32f;
        float core = width * 0.3f;
        quadGrad(matrix, consumer,
            x1 - nx * width, floor, z1 - nz * width, x1 + nx * width, floor, z1 + nz * width,
            x2 + nx * width, floor, z2 + nz * width, x2 - nx * width, floor, z2 - nz * width,
            0.32f, 0.0f, 0.04f, fade * 0.9f, 0.5f, 0.01f, 0.05f, fade * 0.75f);
        quadGrad(matrix, consumer,
            x1 - nx * core, floor + 0.02f, z1 - nz * core, x1 + nx * core, floor + 0.02f, z1 + nz * core,
            x2 + nx * core, floor + 0.02f, z2 + nz * core, x2 - nx * core, floor + 0.02f, z2 - nz * core,
            1.0f, 0.16f, 0.07f, fade, 1.0f, 0.28f, 0.1f, fade * 0.7f);
        quadGrad(matrix, consumer,
            x1 + nx * width, floor, z1 + nz * width, x2 + nx * width, floor, z2 + nz * width,
            x2 + nx * width, floor + lip, z2 + nz * width, x1 + nx * width, floor + lip, z1 + nz * width,
            0.22f, 0.0f, 0.04f, fade, 0.7f, 0.03f, 0.06f, fade * 0.85f);
        quadGrad(matrix, consumer,
            x1 - nx * width, floor, z1 - nz * width, x1 - nx * width, floor + lip, z1 - nz * width,
            x2 - nx * width, floor + lip, z2 - nz * width, x2 - nx * width, floor, z2 - nz * width,
            0.22f, 0.0f, 0.04f, fade, 0.7f, 0.03f, 0.06f, fade * 0.85f);
        float lipIn = width * 0.22f;
        quadGrad(matrix, consumer,
            x1 + nx * width, floor + lip, z1 + nz * width, x1 + nx * (width - lipIn), floor + lip + 0.03f, z1 + nz * (width - lipIn),
            x2 + nx * (width - lipIn), floor + lip + 0.03f, z2 + nz * (width - lipIn), x2 + nx * width, floor + lip, z2 + nz * width,
            1.0f, 0.2f, 0.08f, fade, 1.0f, 0.12f, 0.06f, fade * 0.55f);
        quadGrad(matrix, consumer,
            x1 - nx * width, floor + lip, z1 - nz * width, x2 - nx * width, floor + lip, z2 - nz * width,
            x2 - nx * (width - lipIn), floor + lip + 0.03f, z2 - nz * (width - lipIn), x1 - nx * (width - lipIn), floor + lip + 0.03f, z1 - nz * (width - lipIn),
            1.0f, 0.2f, 0.08f, fade, 1.0f, 0.12f, 0.06f, fade * 0.55f);
    }

    /** Shell, hotter core, four ridge edges, a hook, and a streak that thins off the tip. */
    private static void drawCrystalSpike(Matrix4f matrix, VertexConsumer consumer,
                                         float bx, float by, float bz, float angle, float height, float radius, float fade) {
        if (height < 0.04f || radius < 0.008f || fade < 0.02f) return;
        float ox = (float) Math.cos(angle);
        float oz = (float) Math.sin(angle);
        float tipX = bx + ox * height * 0.32f;
        float tipY = by + height;
        float tipZ = bz + oz * height * 0.32f;
        int sides = 4;
        float[] band = {0f, 0.4f, 0.72f, 1f};
        float[] girth = {1f, 0.64f, 0.28f, 0f};
        for (int shell = 0; shell < 2; shell++) {
            boolean core = shell == 1;
            float scale = core ? 0.38f : 1f;
            for (int b = 0; b < 3; b++) {
                float t0 = band[b];
                float t1 = band[b + 1];
                float x0 = bx + (tipX - bx) * t0;
                float y0 = by + (tipY - by) * t0;
                float z0 = bz + (tipZ - bz) * t0;
                float x1 = bx + (tipX - bx) * t1;
                float y1 = by + (tipY - by) * t1;
                float z1 = bz + (tipZ - bz) * t1;
                float rad0 = radius * girth[b] * scale;
                float rad1 = radius * girth[b + 1] * scale;
                float r0 = core ? 0.78f + 0.22f * t0 : 0.16f + 0.5f * t0;
                float g0 = core ? 0.06f + 0.2f * t0 : 0.01f * t0;
                float b0 = core ? 0.04f + 0.06f * t0 : 0.03f;
                float r1 = core ? 0.78f + 0.22f * t1 : 0.16f + 0.84f * t1;
                float g1 = core ? 0.06f + 0.28f * t1 : 0.12f * t1;
                float b1 = core ? 0.04f + 0.1f * t1 : 0.04f + 0.05f * t1;
                for (int s = 0; s < sides; s++) {
                    double a0 = angle + (s / (double) sides) * Math.PI * 2.0;
                    double a1 = angle + ((s + 1) / (double) sides) * Math.PI * 2.0;
                    float c0x = (float) Math.cos(a0), c0z = (float) Math.sin(a0);
                    float c1x = (float) Math.cos(a1), c1z = (float) Math.sin(a1);
                    quadGrad(matrix, consumer,
                        x0 + c0x * rad0, y0, z0 + c0z * rad0,
                        x0 + c1x * rad0, y0, z0 + c1z * rad0,
                        x1 + c1x * rad1, y1, z1 + c1z * rad1,
                        x1 + c0x * rad1, y1, z1 + c0z * rad1,
                        r0, g0, b0, fade, r1, g1, b1, fade);
                }
            }
        }
        for (int s = 0; s < sides; s++) {
            double ang = angle + (s / (double) sides) * Math.PI * 2.0;
            float ex = bx + (float) Math.cos(ang) * radius;
            float ez = bz + (float) Math.sin(ang) * radius;
            drawTaper(matrix, consumer, ex, by + 0.02f, ez, tipX, tipY, tipZ,
                radius * 0.14f, 0f,
                0.8f, 0.04f, 0.07f, fade, 1.0f, 0.26f, 0.1f, fade);
        }
        float hx = bx + (tipX - bx) * 0.42f;
        float hy = by + (tipY - by) * 0.42f;
        float hz = bz + (tipZ - bz) * 0.42f;
        float bx1 = hx + ox * radius * 1.2f;
        float by1 = hy + height * 0.06f;
        float bz1 = hz + oz * radius * 1.2f;
        float bx2 = hx + ox * radius * 0.55f;
        float by2 = hy - height * 0.1f;
        float bz2 = hz + oz * radius * 0.4f;
        drawTaper(matrix, consumer, hx, hy, hz, bx1, by1, bz1, radius * 0.2f, radius * 0.08f,
            0.4f, 0.02f, 0.05f, fade, 0.9f, 0.08f, 0.08f, fade);
        drawTaper(matrix, consumer, bx1, by1, bz1, bx2, by2, bz2, radius * 0.08f, 0f,
            0.9f, 0.08f, 0.08f, fade, 1.0f, 0.2f, 0.08f, fade * 0.8f);
        float px = tipX, py = tipY, pz = tipZ;
        for (int i = 0; i < 4; i++) {
            float t = (i + 1) / 4.0f;
            float nx = tipX + ox * height * (0.16f + t * 0.5f) + oz * height * 0.1f * t;
            float ny = tipY + height * 0.16f * t;
            float nz = tipZ + oz * height * (0.16f + t * 0.5f) - ox * height * 0.1f * t;
            float tail = (1.0f - t) * (1.0f - t) * fade;
            drawTaper(matrix, consumer, px, py, pz, nx, ny, nz,
                radius * 0.1f * (1.0f - i / 4.0f), radius * 0.1f * (1.0f - t),
                1.0f, 0.18f, 0.08f, tail, 1.0f, 0.1f, 0.05f, tail * 0.15f);
            px = nx;
            py = ny;
            pz = nz;
        }
    }

    private static void drawColumnHelix(Matrix4f matrix, VertexConsumer consumer, float radius, float height, float age, float alpha) {
        int steps = 12;
        for (int strand = 0; strand < 2; strand++) {
            double off = strand * Math.PI;
            for (int i = 0; i < steps; i++) {
                float p1 = i / (float) steps;
                float p2 = (i + 1) / (float) steps;
                double a1 = off + age * 0.18 + p1 * Math.PI * 3.2;
                double a2 = off + age * 0.18 + p2 * Math.PI * 3.2;
                float x1 = (float) Math.cos(a1) * radius;
                float z1 = (float) Math.sin(a1) * radius;
                float x2 = (float) Math.cos(a2) * radius;
                float z2 = (float) Math.sin(a2) * radius;
                float y1 = 0.08f + p1 * height;
                float y2 = 0.08f + p2 * height;
                float dx = x2 - x1, dz = z2 - z1;
                float len = (float) Math.sqrt(dx * dx + dz * dz);
                if (len < 0.0001f) continue;
                float tail = (1.0f - p1) * (1.0f - p1) * alpha;
                float w = 0.07f * (1.0f - p1 * 0.45f);
                float nx = -dz / len * w;
                float nz = dx / len * w;
                quadGrad(matrix, consumer, x1 - nx, y1, z1 - nz, x1 + nx, y1, z1 + nz, x2 + nx, y2, z2 + nz, x2 - nx, y2, z2 - nz,
                    0.75f, 0.03f, 0.07f, tail, 1.0f, 0.14f, 0.08f, tail * 0.35f);
                float ew = w * 0.32f;
                quadGrad(matrix, consumer, x1 - nx * 0.2f, y1 + 0.04f, z1 - nz * 0.2f, x1 + ew, y1 + 0.04f, z1 + ew, x2 + ew, y2 + 0.04f, z2 + ew, x2 - nx * 0.2f, y2 + 0.04f, z2 - nz * 0.2f,
                    1.0f, 0.24f, 0.1f, tail, 1.0f, 0.16f, 0.08f, tail * 0.2f);
            }
        }
    }

    private static void drawGroundRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2;

            float x1 = cx + (float) Math.cos(a1) * rOuter;
            float z1 = cz + (float) Math.sin(a1) * rOuter;
            float x2 = cx + (float) Math.cos(a2) * rOuter;
            float z2 = cz + (float) Math.sin(a2) * rOuter;

            float ix1 = cx + (float) Math.cos(a1) * rInner;
            float iz1 = cz + (float) Math.sin(a1) * rInner;
            float ix2 = cx + (float) Math.cos(a2) * rInner;
            float iz2 = cz + (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, ix1, cy, iz1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ix2, cy, iz2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, cy, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, cy, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawCylinderPillar(Matrix4f matrix, VertexConsumer consumer, float cx, float cz, float radius, float height, int sides, float r, float g, float b, float a) {
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2;

            float x1 = cx + (float) Math.cos(a1) * radius;
            float z1 = cz + (float) Math.sin(a1) * radius;
            float x2 = cx + (float) Math.cos(a2) * radius;
            float z2 = cz + (float) Math.sin(a2) * radius;

            consumer.addVertex(matrix, x1, 0.05f, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, 0.05f, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, height, z2).setColor(r, g, b, a * 0.2f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, height, z1).setColor(r, g, b, a * 0.2f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Reverse
            consumer.addVertex(matrix, x1, height, z1).setColor(r, g, b, a * 0.2f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, height, z2).setColor(r, g, b, a * 0.2f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, 0.05f, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, 0.05f, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }
}
