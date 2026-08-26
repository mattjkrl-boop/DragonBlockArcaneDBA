package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.CurseTelegraphEntity;
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
 * Entity Renderer for Curse Telegraph in Minecraft 26.2.
 * Renders a physical 3D ground runic decal, contracting implosion timer ring, and rising cursed beacon pillars before Curse Lightning strikes.
 */
public class CurseTelegraphRenderer extends EntityRenderer<CurseTelegraphEntity, CurseTelegraphRenderer.TelegraphRenderState> {

    public CurseTelegraphRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TelegraphRenderState extends EntityRenderState {
        public float radius = 1.8f;
        public float progress = 0.0f;
        public float age = 0;
    }

    @Override
    public boolean shouldRender(CurseTelegraphEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public TelegraphRenderState createRenderState() {
        return new TelegraphRenderState();
    }

    @Override
    public void extractRenderState(CurseTelegraphEntity entity, TelegraphRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.progress = entity.getProgress(partialTicks);
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(TelegraphRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        float progress = state.progress;
        float age = state.age;
        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Pulse rate accelerates as countdown reaches completion
            float pulseSpeed = 0.3f + (progress * 0.7f);
            float pulse = 0.8f + 0.2f * (float) Math.sin(age * pulseSpeed * 20.0f);

            // 1. Base Corrupted Runic Ground Star (Dual counter-rotating pentagram rings)
            int segments = 24;
            drawRotatingRing(matrix, buffer, 0, 0.04f, 0, radius, radius * 0.88f, segments, age * 18.0f, 0.95f, 0.02f, 0.15f, 0.85f * pulse);
            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, radius * 0.75f, radius * 0.62f, segments, -age * 25.0f, 0.65f, 0.01f, 0.85f, 0.75f * pulse);

            // 2. Concentrating Implosion Indicator Ring (Shrinks from radius down to 0 as progress -> 1.0)
            float shrinkR = radius * (1.0f - progress);
            if (shrinkR > 0.05f) {
                drawRotatingRing(matrix, buffer, 0, 0.06f, 0, shrinkR, shrinkR * 0.82f, 20, age * 40.0f, 1.0f, 0.9f, 0.95f, 0.95f);
            }

            // 3. Rising 3D Cursed Beacon Tendrils (Pillars extending upward around decal edge)
            int pillarCount = 6;
            float pillarH = 1.2f + (progress * 2.5f);
            for (int p = 0; p < pillarCount; p++) {
                double pAngle = (p / (double) pillarCount) * Math.PI * 2.0 + Math.toRadians(age * 15.0f);
                float px = (float) Math.cos(pAngle) * radius;
                float pz = (float) Math.sin(pAngle) * radius;

                renderBeam(matrix, buffer, px, 0.05f, pz, px, pillarH, pz, 0.08f * (1.0f - progress * 0.3f), 0.95f, 0.02f, 0.20f, 0.80f * pulse);
                renderBeam(matrix, buffer, px, 0.05f, pz, px, pillarH, pz, 0.03f, 1.0f, 0.95f, 0.95f, 0.95f);
            }

            // 4. Central Corrupted Hex Eye (Flashes violently right before strike)
            float coreSize = (0.25f + (progress * 0.25f)) * pulse;
            drawOctahedron(matrix, buffer, 0, 0.08f, 0, coreSize, 1.0f, 0.05f, 0.15f, 0.95f);
        });
    }

    private static void drawRotatingRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
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

    private static void renderBeam(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float radius, float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * radius;
        float nz = dx / len * radius;
        float ny = radius;

        drawQuad(matrix, consumer, x1 - nx, y1, z1 - nz, x1 + nx, y1, z1 + nz, x2 + nx, y2, z2 + nz, x2 - nx, y2, z2 - nz, r, g, b, a);
        drawQuad(matrix, consumer, x1, y1 - ny, z1, x1, y1 + ny, z1, x2, y2 + ny, z2, x2, y2 - ny, z2, r, g, b, a);
    }

    private static void drawOctahedron(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float s, float r, float g, float b, float a) {
        float[][] pts = { {s,0,0}, {0,0,s}, {-s,0,0}, {0,0,-s} };
        for (int i = 0; i < 4; i++) {
            int nxt = (i + 1) % 4;
            consumer.addVertex(matrix, cx + pts[i][0], cy, cz + pts[i][2]).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx + pts[nxt][0], cy, cz + pts[nxt][2]).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, cy + s * 1.5f, cz).setColor(1.0f, 0.9f, 0.95f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, cy + s * 1.5f, cz).setColor(1.0f, 0.9f, 0.95f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
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
