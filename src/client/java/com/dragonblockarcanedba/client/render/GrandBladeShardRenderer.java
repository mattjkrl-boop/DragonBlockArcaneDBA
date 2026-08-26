package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.GrandBladeShardEntity;
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
 * Entity Renderer for Grand Blade Shard in Minecraft 26.2.
 * Renders physical 3D golden-steel blade geometry:
 * - Flying State: Multi-faceted diamond-beveled greatsword shard with radiant golden fuller,
 *   spinning helical energy wake ribbons, and razor-sharp white-gold cutting edges.
 * - Embedded State: Stuck into ground with glowing 3D golden fracture cracks, pulsing sanctum hazard seal,
 *   and vertical radiant golden hazard beacon steles.
 */
public class GrandBladeShardRenderer extends EntityRenderer<GrandBladeShardEntity, GrandBladeShardRenderer.BladeShardRenderState> {

    public GrandBladeShardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BladeShardRenderState extends EntityRenderState {
        public boolean isEmbedded = false;
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
    }

    @Override
    public boolean shouldRender(GrandBladeShardEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public BladeShardRenderState createRenderState() {
        return new BladeShardRenderState();
    }

    @Override
    public void extractRenderState(GrandBladeShardEntity entity, BladeShardRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isEmbedded = entity.isEmbedded();
        state.yRot = entity.isEmbedded() ? entity.getEmbeddedYaw() : entity.getYRot();
        state.xRot = entity.isEmbedded() ? entity.getEmbeddedPitch() : entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(BladeShardRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        if (state.isEmbedded) {
            // --- EMBEDDED GROUND STATE ---
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));

            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                Matrix4f matrix = pose.pose();
                float pulse = 0.85f + 0.15f * (float) Math.sin(state.ageInTicks * 0.25f);

                // 1. Glowing 3D Golden Ground Fracture Mandala & Tripping Hazard Seal
                int crackRays = 6;
                for (int c = 0; c < crackRays; c++) {
                    double angle = (c / (double) crackRays) * Math.PI * 2.0;
                    float crackLen = 0.75f + (c % 2 == 0 ? 0.35f : 0.0f);
                    float x2 = (float) Math.cos(angle) * crackLen;
                    float z2 = (float) Math.sin(angle) * crackLen;

                    drawCrackLine(matrix, buffer, 0, 0.03f, 0, x2, 0.03f, z2, 0.08f, 1.0f, 0.85f, 0.20f, 0.90f * pulse);
                    drawCrackLine(matrix, buffer, 0, 0.04f, 0, x2 * 0.7f, 0.04f, z2 * 0.7f, 0.03f, 1.0f, 1.0f, 0.90f, 1.0f * pulse);
                }

                // Ground Warning Ring
                drawGroundRing(matrix, buffer, 0.75f, 0.60f, 16, 1.0f, 0.80f, 0.10f, 0.65f * pulse);

                // 2. Vertical Radiant Golden Hazard Beacon Stele (Telegraphs Tripping Hazard in 3D)
                float beaconHeight = 1.6f * pulse;
                drawBeaconPlume(matrix, buffer, 0, 0, 0.08f, beaconHeight, 1.0f, 0.90f, 0.30f, 0.70f * pulse);

                // 3. Embedded Physical Greatsword Shard
                PoseStack bladeStack = new PoseStack();
                bladeStack.last().pose().set(pose.pose());
                bladeStack.last().normal().set(pose.normal());
                bladeStack.mulPose(Axis.XP.rotationDegrees(Math.max(35.0f, Math.abs(state.xRot))));
                bladeStack.scale(0.85f, 0.85f, 0.85f);

                drawPhysicalBlade(bladeStack.last().pose(), buffer, state.ageInTicks, 1.0f);
            });

        } else {
            // --- FLYING AIRBORNE STATE ---
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.ageInTicks * 35.0f)); // High-speed spin along flight axis
            poseStack.scale(0.75f, 0.75f, 0.75f);

            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                Matrix4f matrix = pose.pose();

                // 1. Physical 3D Greatsword Shard
                drawPhysicalBlade(matrix, buffer, state.ageInTicks, 1.0f);

                // 2. Volumetric 3D Helical Energy Trail Ribbon Vortex
                drawHelicalWake(matrix, buffer, state.ageInTicks);
            });
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawPhysicalBlade(Matrix4f matrix, VertexConsumer consumer, float age, float alpha) {
        float bladeLength = 1.10f;
        float bladeHalfWidth = 0.22f;
        float spineThickness = 0.07f;
        float hiltBack = -0.30f;

        // 1. Steel-Gold Diamond Faceted Blade Faces (Top & Bottom Ridge)
        // Top Left Face (Steel Blue / Gold)
        drawTriangle(matrix, consumer,
            0, spineThickness, hiltBack,
            -bladeHalfWidth, 0, 0,
            0, 0, bladeLength,
            0.35f, 0.65f, 0.95f, 0.95f * alpha
        );
        // Top Right Face (Gold Radiant)
        drawTriangle(matrix, consumer,
            0, spineThickness, hiltBack,
            0, 0, bladeLength,
            bladeHalfWidth, 0, 0,
            1.0f, 0.88f, 0.25f, 0.95f * alpha
        );
        // Bottom Left Face
        drawTriangle(matrix, consumer,
            0, -spineThickness, hiltBack,
            0, 0, bladeLength,
            -bladeHalfWidth, 0, 0,
            0.35f, 0.65f, 0.95f, 0.95f * alpha
        );
        // Bottom Right Face
        drawTriangle(matrix, consumer,
            0, -spineThickness, hiltBack,
            bladeHalfWidth, 0, 0,
            0, 0, bladeLength,
            1.0f, 0.88f, 0.25f, 0.95f * alpha
        );

        // 2. Central Raised Radiant Golden Spine / Fuller
        float fullerWidth = 0.05f;
        drawQuad3D(matrix, consumer,
            -fullerWidth, spineThickness * 1.05f, hiltBack,
            fullerWidth, spineThickness * 1.05f, hiltBack,
            fullerWidth, spineThickness * 0.4f, bladeLength * 0.8f,
            -fullerWidth, spineThickness * 0.4f, bladeLength * 0.8f,
            1.0f, 1.0f, 0.60f, 1.0f * alpha
        );

        // 3. Blinding White-Gold Cutting Edge Bevels
        drawEdgeLine(matrix, consumer, -bladeHalfWidth, 0, 0, 0, 0, bladeLength, 0.025f, 1.0f, 1.0f, 1.0f, 1.0f * alpha);
        drawEdgeLine(matrix, consumer, bladeHalfWidth, 0, 0, 0, 0, bladeLength, 0.025f, 1.0f, 1.0f, 1.0f, 1.0f * alpha);
    }

    private static void drawHelicalWake(Matrix4f matrix, VertexConsumer consumer, float age) {
        int steps = 10;
        float trailLength = 1.4f;

        for (int i = 0; i < steps; i++) {
            float p1 = i / (float) steps;
            float p2 = (i + 1) / (float) steps;

            float z1 = -0.2f - (p1 * trailLength);
            float z2 = -0.2f - (p2 * trailLength);

            float r1 = 0.20f * (1.0f + p1 * 0.5f);
            float r2 = 0.20f * (1.0f + p2 * 0.5f);

            double a1 = age * 0.8 + (p1 * Math.PI * 2.5);
            double a2 = age * 0.8 + (p2 * Math.PI * 2.5);

            float x1 = (float) Math.cos(a1) * r1;
            float y1 = (float) Math.sin(a1) * r1;
            float x2 = (float) Math.cos(a2) * r2;
            float y2 = (float) Math.sin(a2) * r2;

            float alpha = (1.0f - p1) * 0.75f;
            drawCrackLine(matrix, consumer, x1, y1, z1, x2, y2, z2, 0.04f, 1.0f, 0.85f, 0.20f, alpha);
        }
    }

    private static void drawBeaconPlume(Matrix4f matrix, VertexConsumer consumer, float cx, float cz, float radius, float height, float r, float g, float b, float a) {
        // 4-sided vertical energy prism
        for (int i = 0; i < 4; i++) {
            double a1 = (i / 4.0) * Math.PI * 2.0;
            double a2 = ((i + 1) / 4.0) * Math.PI * 2.0;

            float x1 = cx + (float) Math.cos(a1) * radius;
            float z1 = cz + (float) Math.sin(a1) * radius;
            float x2 = cx + (float) Math.cos(a2) * radius;
            float z2 = cz + (float) Math.sin(a2) * radius;

            consumer.addVertex(matrix, x1, 0.05f, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, 0.05f, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, height, cz).setColor(r, g, b, 0.0f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, height, cz).setColor(r, g, b, 0.0f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawTriangle(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawQuad3D(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawCrackLine(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float width, float r, float g, float b, float a) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * width * 0.5f;
        float nz = dx / len * width * 0.5f;

        consumer.addVertex(matrix, x1 - nx, y1, z1 - nz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx, y1, z1 + nz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx, y2, z2 + nz).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx, y2, z2 - nz).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawEdgeLine(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float width, float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1 - width, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + width, y1, z1).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + width, y2, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - width, y2, z2).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawGroundRing(Matrix4f matrix, VertexConsumer consumer, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2;

            float x1 = (float) Math.cos(a1) * rOuter;
            float z1 = (float) Math.sin(a1) * rOuter;
            float x2 = (float) Math.cos(a2) * rOuter;
            float z2 = (float) Math.sin(a2) * rOuter;

            float ix1 = (float) Math.cos(a1) * rInner;
            float iz1 = (float) Math.sin(a1) * rInner;
            float ix2 = (float) Math.cos(a2) * rInner;
            float iz2 = (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, ix1, 0.04f, iz1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ix2, 0.04f, iz2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, 0.04f, z2).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, 0.04f, z1).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }
}
