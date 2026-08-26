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

    @Override
    public boolean shouldRender(OxStanceAuraEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
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
            int groundSegments = 36;
            float repulseWave = (age * 0.35f) % auraR;

            // Outer Boundary Perimeter Teeth & Runic Ring
            drawRotatingRing(matrix, buffer, 0, 0.04f, 0, auraR * 1.02f, auraR * 0.96f, groundSegments, age * 6.0f,
                1.0f, isPeak ? 0.85f : 0.45f, 0.05f, isPeak ? 0.95f : alpha * 0.75f);
            drawRotatingRing(matrix, buffer, 0, 0.03f, 0, auraR * 0.96f, auraR * 0.90f, groundSegments, age * -8.0f,
                0.90f, isPeak ? 0.50f : 0.20f, 0.02f, alpha * 0.65f);

            // 12 Radial Pressure Nodes
            for (int i = 0; i < 12; i++) {
                double nodeAng = (i / 12.0) * Math.PI * 2.0 + Math.toRadians(age * 6.0f);
                float nx1 = (float) Math.cos(nodeAng) * (auraR * 0.85f);
                float nz1 = (float) Math.sin(nodeAng) * (auraR * 0.85f);
                float nx2 = (float) Math.cos(nodeAng) * (auraR * 1.02f);
                float nz2 = (float) Math.sin(nodeAng) * (auraR * 1.02f);
                drawSegment(matrix, buffer, nx1, nz1, nx2, nz2, 0.18f, 1.0f, 0.60f, 0.05f, alpha * 0.85f);
            }

            // Expanding concentric repulsion shock ripples
            if (repulseWave > 0.5f) {
                float rippleFade = (1.0f - (repulseWave / auraR)) * alpha * 0.60f;
                drawRotatingRing(matrix, buffer, 0, 0.05f, 0, repulseWave, Math.max(0.1f, repulseWave - 0.6f), groundSegments, 0,
                    1.0f, 0.45f, 0.02f, rippleFade);
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

            // 3. 3D Swirling Molten Heat Dome & Upward Energy Helices
            int domeSegments = 24;
            float domeR = (2.2f + intensity * 0.8f) * pulse;
            drawHeatDome(matrix, buffer, 0, 0.1f, 0, domeR, avatarH * 0.85f, domeSegments, age * 16.0f,
                rCol, gCol, bCol, alpha * 0.40f);

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

                    drawSegment(matrix, buffer, sx1, sz1, sx2, sz2, 0.15f, 1.0f, 1.0f, 0.60f, 1.0f);
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
        float half = size * 0.5f;
        // 3D Pauldron Diamond Box
        consumer.addVertex(matrix, cx - half, cy, cz - half).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx + half, cy, cz - half).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx + half * 1.3f, cy + height, cz).setColor(r, g, b, a * 0.9f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx - half * 1.3f, cy + height, cz).setColor(r, g, b, a * 0.9f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawTitanHorn(Matrix4f matrix, VertexConsumer consumer, float bx, float by, float bz, float tx, float ty, float tz, float width, float r, float g, float b, float a) {
        consumer.addVertex(matrix, bx - width, by, bz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, bx + width, by, bz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tx, ty, tz).setColor(r * 1.1f, g * 1.1f, b * 1.1f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tx, ty, tz).setColor(r * 1.1f, g * 1.1f, b * 1.1f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawAegisPlate(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float width, float height, float r, float g, float b, float a) {
        float halfW = width * 0.5f;
        float halfH = height * 0.5f;
        consumer.addVertex(matrix, cx - halfW, cy - halfH, cz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx + halfW, cy - halfH, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx + halfW * 0.7f, cy + halfH, cz).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx - halfW * 0.7f, cy + halfH, cz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
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
