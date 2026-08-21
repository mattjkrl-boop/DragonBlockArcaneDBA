package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.EvilSpearProjectileEntity;
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
 * Entity Renderer for Evil Spear Projectile in MC 26.2.
 * Renders a glowing, demonic 3D spectral spear with multi-beveled spearhead and dark vortex fins.
 */
public class EvilSpearProjectileRenderer extends EntityRenderer<EvilSpearProjectileEntity, EvilSpearProjectileRenderer.EvilSpearRenderState> {
    public EvilSpearProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class EvilSpearRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
    }

    @Override
    public EvilSpearRenderState createRenderState() {
        return new EvilSpearRenderState();
    }

    @Override
    public void extractRenderState(EvilSpearProjectileEntity entity, EvilSpearRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(EvilSpearRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.age * 25.0f)); // Continuous spiral spin

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Demonic Crimson Spear Shaft (6-sided cylinder)
            drawCylinder(matrix, buffer, 0.09f, -1.8f, 1.4f, 6, 0.85f, 0.0f, 0.15f, 0.95f);
            drawCylinder(matrix, buffer, 0.04f, -1.7f, 1.3f, 6, 1.0f, 0.8f, 0.8f, 1.0f);

            // 2. Beveled Diamond Spearhead
            drawSpearHead(matrix, buffer, 0.35f, 1.2f, 2.6f, 1.0f, 0.1f, 0.2f, 0.95f);
            drawSpearHead(matrix, buffer, 0.15f, 1.4f, 2.75f, 1.0f, 0.9f, 0.9f, 1.0f);

            // 3. Trailing Demonic Vortex Fins
            drawFin(matrix, buffer, 0.45f, -1.8f, -1.0f, 0.75f, 0.0f, 0.25f, 0.85f);
            drawFin(matrix, buffer, -0.45f, -1.8f, -1.0f, 0.75f, 0.0f, 0.25f, 0.85f);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawCylinder(Matrix4f matrix, VertexConsumer consumer, float radius, float zStart, float zEnd, int sides, float r, float g, float b, float a) {
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2;

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            consumer.addVertex(matrix, x1, y1, zStart).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2, zStart).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2, zEnd).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, y1, zEnd).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawSpearHead(Matrix4f matrix, VertexConsumer consumer, float width, float zBase, float zTip, float r, float g, float b, float a) {
        // 4-sided diamond pyramid head
        float[][] pts = { { width, 0 }, { 0, width }, { -width, 0 }, { 0, -width } };
        for (int i = 0; i < 4; i++) {
            int nxt = (i + 1) % 4;
            consumer.addVertex(matrix, pts[i][0], pts[i][1], zBase).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, pts[nxt][0], pts[nxt][1], zBase).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, 0, 0, zTip).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, 0, 0, zTip).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawFin(Matrix4f matrix, VertexConsumer consumer, float width, float zStart, float zEnd, float r, float g, float b, float a) {
        consumer.addVertex(matrix, 0, 0, zStart).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, width, 0, zStart).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, 0, 0, zEnd).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, 0, 0, zEnd).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
    }
}
