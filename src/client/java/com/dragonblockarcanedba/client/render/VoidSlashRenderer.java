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

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(VoidSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(VoidSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
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

        float progress = Math.min(1.0f, state.age / 40.0f);
        float open = ease(Math.min(1.0f, state.age / 6.0f));
        float close = ease(Math.max(0.0f, (progress - 0.70f) / 0.30f));
        float fade = (1.0f - close) * open;
        if (fade <= 0.01f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float radius = (1.45f + 0.40f * open) * (1.0f - 0.62f * close);
        float band = 0.30f * (1.0f - 0.35f * close);

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            drawVoidRing(matrix, buffer, radius * 1.16f, band * 1.25f, 0.55f, 0.08f, 0.0f, 0.16f, 0.80f * fade);
            drawVoidRing(matrix, buffer, radius, band, 0.28f, 0.60f, 0.05f, 0.95f, 0.95f * fade);
            drawVoidRing(matrix, buffer, radius, band * 0.45f, 0.12f, 0.15f, 0.92f, 1.0f, fade);
            drawVoidRing(matrix, buffer, radius, band * 0.14f, 0.0f, 1.0f, 1.0f, 1.0f, fade);
            float edgeLeft = 1.0f - close;
            drawVoidRing(matrix, buffer, radius * 1.22f, band * 0.18f * edgeLeft, 0.75f * edgeLeft, 0.20f, 0.95f, 1.0f, fade * edgeLeft);
            drawVoidRing(matrix, buffer, radius, band * 0.05f * (0.35f + 0.65f * edgeLeft), 0.08f * edgeLeft, 1.0f, 1.0f, 1.0f, fade);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }

    /**
     * A closed ring facing the direction of travel. The empty middle is the void,
     * the white line is the core, cyan is the edge, and the band slips backward
     * so the ring has a follow-through before it collapses.
     */
    private static void drawVoidRing(Matrix4f matrix, VertexConsumer consumer, float radius, float band, float trail, float r, float g, float b, float a) {
        int segments = 32;
        float outer = radius + band;
        float inner = Math.max(0.08f, radius - band);
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float c2 = (float) Math.cos(a2);
            float s2 = (float) Math.sin(a2);

            consumer.addVertex(matrix, c1 * inner, s1 * inner, 0.0f).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, c2 * inner, s2 * inner, 0.0f).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, c2 * outer, s2 * outer, 0.0f).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, c1 * outer, s1 * outer, 0.0f).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);

            if (trail > 0.01f) {
                consumer.addVertex(matrix, c1 * inner, s1 * inner, 0.0f).setColor(r, g, b, a * 0.65f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
                consumer.addVertex(matrix, c2 * inner, s2 * inner, 0.0f).setColor(r, g, b, a * 0.65f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
                consumer.addVertex(matrix, c2 * outer, s2 * outer, -trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
                consumer.addVertex(matrix, c1 * outer, s1 * outer, -trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            }
        }
    }
}
