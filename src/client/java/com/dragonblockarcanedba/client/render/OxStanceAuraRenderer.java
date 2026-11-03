package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.OxStanceAuraEntity;
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
 * Entity Renderer for Ox King's Colossal Stance Aura in Minecraft 26.2.
 * Renders a physical 3D King's Titan Aegis avatar, 12-block rotating repulsion boundary disc,
 * swirling molten heat dome, and incandescent Critical Peak overdrive energy.
 */
public class OxStanceAuraRenderer extends EntityRenderer<OxStanceAuraEntity, OxStanceAuraRenderer.OxStanceRenderState> {

    public OxStanceAuraRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class OxStanceRenderState extends EntityRenderState {
        public int heldTicks = 0;
        public boolean isPeak = false;
        public float age = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(OxStanceAuraEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(OxStanceAuraEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public OxStanceRenderState createRenderState() {
        return new OxStanceRenderState();
    }

    @Override
    public void extractRenderState(OxStanceAuraEntity entity, OxStanceRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.heldTicks = entity.getHeldTicks();
        state.isPeak = entity.isPeak();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(OxStanceRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        int held = state.heldTicks;
        boolean isPeak = state.isPeak;
        float age = state.age;

        float intensity = Math.min(1.0f, held / 100.0f);
        float pulse = 0.92f + 0.08f * (float) Math.sin(age * 0.45f);
        float alpha = 0.70f + (intensity * 0.25f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. 12-Block Battlefield Denial Repulsion Boundary Disc on the Ground
            float auraR = 12.0f;
            int groundSegments = 48;
            float cycle = (age * 0.18f) % 1.0f;
            float eased = cycle * cycle * (3.0f - 2.0f * cycle);
            float repulseWave = eased * auraR;

            // One boundary ring: bright gold lip, darker molten inner edge of the same band.
            drawRotatingRing(matrix, buffer, 0, 0.04f, 0, auraR * 1.02f, auraR * 0.94f, groundSegments, age * 4.0f,
                0.72f, isPeak ? 0.28f : 0.16f, 0.02f, alpha * 0.80f);
            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, auraR * 0.99f, auraR * 0.96f, groundSegments, age * 4.0f,
                1.0f, isPeak ? 0.90f : 0.62f, isPeak ? 0.30f : 0.08f, isPeak ? 0.95f : alpha * 0.90f);

            // 12 Radial Pressure Nodes
            for (int i = 0; i < 12; i++) {
                double nodeAng = (i / 12.0) * Math.PI * 2.0 + Math.toRadians(age * 6.0f);
                float nx1 = (float) Math.cos(nodeAng) * (auraR * 0.85f);
                float nz1 = (float) Math.sin(nodeAng) * (auraR * 0.85f);
                float nx2 = (float) Math.cos(nodeAng) * (auraR * 1.02f);
                float nz2 = (float) Math.sin(nodeAng) * (auraR * 1.02f);
                drawLash(matrix, buffer,
                    nx1, 0.05f, nz1,
                    (nx1 + nx2) * 0.5f, 0.28f, (nz1 + nz2) * 0.5f,
                    nx2, 0.08f, nz2,
                    0.10f, 5,
                    0.85f, 0.35f, 0.04f,
                    1.0f, 0.86f, 0.30f,
                    alpha * 0.90f
                );
            }

            // Expanding concentric repulsion shock ripples
            if (eased > 0.08f && eased < 0.96f) {
                float rippleFade = (1.0f - eased) * (1.0f - eased) * alpha * 0.70f;
                drawRotatingRing(matrix, buffer, 0, 0.05f, 0, repulseWave, Math.max(0.1f, repulseWave - 0.45f), groundSegments, 0,
                    1.0f, 0.55f, 0.08f, rippleFade);
            }

            // 2. Towering Ethereal 3D Titan Aegis / Armor Avatar around Caster
            float avatarH = 3.6f * pulse;
            float avatarW = 1.6f * pulse;

            // Color: Golden-amber base, blazing molten crimson/gold at peak
            float rCol = isPeak ? 1.0f : 1.0f;
            float gCol = isPeak ? 0.92f : 0.48f;
            float bCol = isPeak ? 0.35f : 0.05f;

            // Titan Fortress Shoulders & Pauldrons
            drawShoulderPauldron(matrix, buffer, -avatarW * 0.75f, 2.2f, 0.0f, 0.65f, avatarH * 0.4f, rCol, gCol, bCol, alpha * 0.85f);
            drawShoulderPauldron(matrix, buffer, avatarW * 0.75f, 2.2f, 0.0f, 0.65f, avatarH * 0.4f, rCol, gCol, bCol, alpha * 0.85f);

            // Titan Horned Mountain Crest / Crown (horns reaching upward)
            drawTitanHorn(matrix, buffer, -0.55f, 2.6f, 0.1f, -0.95f, 3.8f, 0.3f, 0.22f, rCol, gCol, bCol, alpha * 0.90f);
            drawTitanHorn(matrix, buffer, 0.55f, 2.6f, 0.1f, 0.95f, 3.8f, 0.3f, 0.22f, rCol, gCol, bCol, alpha * 0.90f);

            // Titan Ethereal Chestplate & Aegis Shell
            drawAegisPlate(matrix, buffer, 0, 1.2f, 0.65f, avatarW * 0.8f, 1.4f, rCol, gCol, bCol, alpha * 0.75f);
            drawAegisPlate(matrix, buffer, 0, 1.2f, -0.65f, avatarW * 0.8f, 1.4f, rCol, gCol, bCol, alpha * 0.75f);
            // Inner hot core plates (redesign T2): the avatar plates were one
            // flat tone; a smaller, hotter plate inside each shell gives the
            // chest a depth ramp toward white heat at its heart.
            float coreG = Math.min(1.0f, gCol + 0.30f);
            float coreB = Math.min(1.0f, bCol + 0.15f);
            drawAegisPlate(matrix, buffer, 0, 1.2f, 0.67f, avatarW * 0.44f, 0.8f, 1.0f, coreG, coreB, alpha * 0.85f);
            drawAegisPlate(matrix, buffer, 0, 1.2f, -0.67f, avatarW * 0.44f, 0.8f, 1.0f, coreG, coreB, alpha * 0.85f);

            // 3. 3D Swirling Molten Heat Dome & Upward Energy Helices
            int domeSegments = 32;
            float domeR = (2.2f + intensity * 0.8f) * pulse;
            drawHeatDome(matrix, buffer, 0, 0.1f, 0, domeR, avatarH * 0.85f, domeSegments, age * 16.0f,
                rCol, gCol, bCol, alpha * 0.40f);
            drawHeatDome(matrix, buffer, 0, 0.15f, 0, domeR * 0.58f, avatarH * 0.55f, domeSegments, -age * 11.0f,
                1.0f, Math.min(1.0f, gCol + 0.35f), Math.min(1.0f, bCol + 0.20f), alpha * 0.55f);

            // Dual Helical Fiery Energy Ribbons
            int helixSteps = 20;
            for (int h = 0; h < 2; h++) {
                float hOffset = h * (float) Math.PI;
                for (int s = 0; s < helixSteps; s++) {
                    float prog1 = s / (float) helixSteps;
                    float prog2 = (s + 1) / (float) helixSteps;

                    float y1 = 0.2f + prog1 * avatarH;
                    float y2 = 0.2f + prog2 * avatarH;

                    float rad1 = (1.4f - prog1 * 0.5f) * pulse;
                    float rad2 = (1.4f - prog2 * 0.5f) * pulse;

                    double a1 = age * 0.18f + prog1 * Math.PI * 3.0 + hOffset;
                    double a2 = age * 0.18f + prog2 * Math.PI * 3.0 + hOffset;

                    float x1 = (float) Math.cos(a1) * rad1;
                    float z1 = (float) Math.sin(a1) * rad1;
                    float x2 = (float) Math.cos(a2) * rad2;
                    float z2 = (float) Math.sin(a2) * rad2;

                    drawRibbonSegment(matrix, buffer, x1, y1, z1, x2, y2, z2, 0.16f,
                        rCol, gCol, bCol, alpha * 0.85f * (1.0f - prog1 * 0.3f));
                    drawRibbonSegment(matrix, buffer, x1, y1, z1, x2, y2, z2, 0.045f,
                        1.0f, Math.min(1.0f, gCol + 0.40f), Math.min(1.0f, bCol + 0.25f), alpha * 0.95f * (1.0f - prog1 * 0.15f));
                }
            }

            // 4. Critical Peak Window Overdrive (14s–15s / 280–300 ticks)
            if (isPeak) {
                // Intense vibrating golden energy spikes radiating outwards
                int spikeCount = 12;
                for (int i = 0; i < spikeCount; i++) {
                    double spkAng = (i / (double) spikeCount) * Math.PI * 2.0 + (age * 0.25f);
                    float spkLen = 2.5f + (float) (Math.sin(age * 0.8f + i * 1.5f) * 0.6f);
                    float sx1 = (float) Math.cos(spkAng) * 0.8f;
                    float sz1 = (float) Math.sin(spkAng) * 0.8f;
                    float sx2 = (float) Math.cos(spkAng) * spkLen;
                    float sz2 = (float) Math.sin(spkAng) * spkLen;
                    float sy = 1.0f + (float) Math.sin(i * 1.2f) * 0.6f;

                    float curl = (float) Math.sin(spkAng + 1.4) * 0.35f;
                    float curlZ = (float) Math.cos(spkAng + 1.4) * 0.35f;
                    drawLash(matrix, buffer,
                        sx1, sy, sz1,
                        (sx1 + sx2) * 0.5f + curl, sy + 0.45f, (sz1 + sz2) * 0.5f + curlZ,
                        sx2, sy * 0.4f, sz2,
                        0.12f, 6,
                        1.0f, 0.72f, 0.18f,
                        1.0f, 0.98f, 0.70f,
                        0.95f
                    );
                }
            }
        });

        super.submit(state, poseStack, collector, cameraState);
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

    private static void drawSegment(Matrix4f matrix, VertexConsumer consumer, float x1, float z1, float x2, float z2, float width, float r, float g, float b, float a) {
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

    private static void drawShoulderPauldron(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float height, float r, float g, float b, float a) {
        int steps = 6;
        float out = cx >= 0.0f ? 1.0f : -1.0f;
        for (int i = 0; i < steps; i++) {
            float u0 = i / (float) steps;
            float u1 = (i + 1) / (float) steps;
            float a0 = (u0 - 0.5f) * 1.35f;
            float a1 = (u1 - 0.5f) * 1.35f;
            float lift0 = (float) Math.sin(u0 * Math.PI);
            float lift1 = (float) Math.sin(u1 * Math.PI);
            float x0 = cx + (float) Math.sin(a0) * size;
            float x1 = cx + (float) Math.sin(a1) * size;
            float z0 = cz + out * (float) (1.0 - Math.cos(a0)) * size * 0.45f;
            float z1 = cz + out * (float) (1.0 - Math.cos(a1)) * size * 0.45f;
            float yb0 = cy + lift0 * height * 0.12f;
            float yb1 = cy + lift1 * height * 0.12f;
            float yt0 = cy + lift0 * height;
            float yt1 = cy + lift1 * height;
            consumer.addVertex(matrix, x0, yb0, z0).setColor(r, g, b, a * (0.35f + 0.65f * lift0)).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, yb1, z1).setColor(r, g, b, a * (0.35f + 0.65f * lift1)).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, yt1, z1 + out * 0.04f).setColor(1.0f, Math.min(1.0f, g + 0.25f), Math.min(1.0f, b + 0.15f), a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x0, yt0, z0 + out * 0.04f).setColor(1.0f, Math.min(1.0f, g + 0.25f), Math.min(1.0f, b + 0.15f), a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawTitanHorn(Matrix4f matrix, VertexConsumer consumer, float bx, float by, float bz, float tx, float ty, float tz, float width, float r, float g, float b, float a) {
        float out = bx >= 0.0f ? 1.0f : -1.0f;
        drawLash(matrix, consumer,
            bx, by, bz,
            (bx + tx) * 0.5f + out * 0.22f, (by + ty) * 0.55f + 0.18f, (bz + tz) * 0.5f + 0.10f,
            tx, ty, tz,
            width, 6,
            r, g, b,
            1.0f, Math.min(1.0f, g + 0.28f), Math.min(1.0f, b + 0.16f),
            a
        );
    }

    private static void drawAegisPlate(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float width, float height, float r, float g, float b, float a) {
        int cols = 5;
        float halfH = height * 0.5f;
        float zSign = cz >= 0.0f ? 1.0f : -1.0f;
        for (int i = 0; i < cols; i++) {
            float u0 = i / (float) cols;
            float u1 = (i + 1) / (float) cols;
            float bow0 = (float) Math.sin(u0 * Math.PI) * width * 0.16f;
            float bow1 = (float) Math.sin(u1 * Math.PI) * width * 0.16f;
            float x0 = cx + (u0 - 0.5f) * width;
            float x1 = cx + (u1 - 0.5f) * width;
            float xt0 = cx + (u0 - 0.5f) * width * 0.70f;
            float xt1 = cx + (u1 - 0.5f) * width * 0.70f;
            consumer.addVertex(matrix, x0, cy - halfH, cz + bow0 * zSign).setColor(r * 0.75f, g * 0.75f, b * 0.75f, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, zSign);
            consumer.addVertex(matrix, x1, cy - halfH, cz + bow1 * zSign).setColor(r * 0.75f, g * 0.75f, b * 0.75f, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, zSign);
            consumer.addVertex(matrix, xt1, cy + halfH, cz + bow1 * 1.2f * zSign).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, zSign);
            consumer.addVertex(matrix, xt0, cy + halfH, cz + bow0 * 1.2f * zSign).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, zSign);
        }
    }

    private static void drawHeatDome(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float radius, float height, int segments, float rotDeg, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = rotRad + (i / (double) segments) * Math.PI * 2.0;
            double a2 = rotRad + ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = cx + (float) Math.cos(a1) * radius;
            float z1 = cz + (float) Math.sin(a1) * radius;
            float x2 = cx + (float) Math.cos(a2) * radius;
            float z2 = cz + (float) Math.sin(a2) * radius;

            consumer.addVertex(matrix, x1, cy, z1).setColor(r, g, b, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, cy, z2).setColor(r, g, b, 0.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2 * 0.2f, cy + height, z2 * 0.2f).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1 * 0.2f, cy + height, z1 * 0.2f).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawRibbonSegment(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float width, float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1 - width * 0.5f, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1 + width * 0.5f, z1).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2 + width * 0.5f, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2 - width * 0.5f, z2).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
