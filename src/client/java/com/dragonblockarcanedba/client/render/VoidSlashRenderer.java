package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.VoidSlashEntity;
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
 * Entity Renderer for Void Slash in Minecraft 26.2.
 * Renders a physical volumetric 3D crescent blade wave with deep abyssal void aura, cyan plasma energy core, pure white hyper-velocity cutting edge, and aerodynamic wake vanes.
 */
public class VoidSlashRenderer extends EntityRenderer<VoidSlashEntity, VoidSlashRenderer.SlashRenderState> {
    public VoidSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SlashRenderState extends EntityRenderState {
        public boolean tiltRight = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
    }

    @Override
    public boolean shouldRender(VoidSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public SlashRenderState createRenderState() {
        return new SlashRenderState();
    }

    @Override
    public void extractRenderState(VoidSlashEntity entity, SlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tiltRight = entity.getTilt();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(SlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float maxLife = 40.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        float fade = (1.0f - progress * progress);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        if (state.tiltRight) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(42.0f));
        } else {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-42.0f));
        }

        float span = 4.2f;
        float chord = 1.35f;
        int segments = 20;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Outer Deep Abyssal Void Aura (Expanding dark spatial distortion)
            drawVolumetricCrescent(matrix, buffer, span + 0.45f, chord + 0.40f, 0.28f, 0.60f, segments,
                0.08f, 0.0f, 0.16f, 0.90f * fade);

            // 2. Radiant Violet Void Blade Crest
            drawVolumetricCrescent(matrix, buffer, span, chord, 0.16f, 0.45f, segments,
                0.60f, 0.05f, 0.95f, 0.95f * fade);

            // 3. Cyan Plasma Ethereal Core
            drawVolumetricCrescent(matrix, buffer, span * 0.85f, chord * 0.85f, 0.10f, 0.30f, segments,
                0.15f, 0.92f, 1.0f, 0.98f * fade);

            // 4. Pure White Hyper-Velocity Cutting Edge
            drawVolumetricCrescent(matrix, buffer, span * 0.65f, chord * 0.65f, 0.05f, 0.12f, segments,
                1.0f, 1.0f, 1.0f, 1.0f * fade);

            // 5. Transversal Tip Winglet Flares (3D sharp tip spurs)
            drawTipSpurs(matrix, buffer, span, chord, 0.18f, fade);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawVolumetricCrescent(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float thickness, float trailLength, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;

            float x1 = t1 * span;
            float z1 = (1.0f - t1 * t1) * chord;
            float x2 = t2 * span;
            float z2 = (1.0f - t2 * t2) * chord;

            float th1 = thickness * (1.0f - Math.abs(t1) * 0.75f);
            float th2 = thickness * (1.0f - Math.abs(t2) * 0.75f);

            float z1Trail = z1 - trailLength * (1.0f - Math.abs(t1));
            float z2Trail = z2 - trailLength * (1.0f - Math.abs(t2));

            // Top Surface (Leading edge to trailing wake)
            consumer.addVertex(matrix, x1, th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2 * 0.3f, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, th1 * 0.3f, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom Surface
            consumer.addVertex(matrix, x1, -th1 * 0.3f, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2 * 0.3f, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, -th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

            // Front Leading Bevel Face (Volumetric front edge)
            consumer.addVertex(matrix, x1, -th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, th2, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, th1, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawTipSpurs(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float spurSize, float fade) {
        // Left Tip Spur
        consumer.addVertex(matrix, -span, 0, 0).setColor(0.15f, 0.95f, 1.0f, fade).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -span - spurSize * 1.5f, 0, -spurSize * 2.0f).setColor(0.6f, 0.05f, 0.95f, 0.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -span + spurSize * 0.5f, 0, -spurSize).setColor(0.15f, 0.95f, 1.0f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -span, 0, 0).setColor(0.15f, 0.95f, 1.0f, fade).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Right Tip Spur
        consumer.addVertex(matrix, span, 0, 0).setColor(0.15f, 0.95f, 1.0f, fade).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, span + spurSize * 1.5f, 0, -spurSize * 2.0f).setColor(0.6f, 0.05f, 0.95f, 0.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, span - spurSize * 0.5f, 0, -spurSize).setColor(0.15f, 0.95f, 1.0f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, span, 0, 0).setColor(0.15f, 0.95f, 1.0f, fade).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
