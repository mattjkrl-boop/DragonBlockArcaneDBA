package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.BlasterBoltEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Blaster Bolt in MC 26.2.
 * Renders an elongated, glowing laser projectile scaling with Heat.
 */
public class BlasterBoltRenderer extends EntityRenderer<BlasterBoltEntity, BlasterBoltRenderer.BlasterBoltRenderState> {
    public BlasterBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BlasterBoltRenderState createRenderState() {
        return new BlasterBoltRenderState();
    }

    @Override
    public void extractRenderState(BlasterBoltEntity entity, BlasterBoltRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.isOvercharged = entity.isOvercharged();
        state.heatRatio = entity.getHeatRatio();
    }

    @Override
    public void submit(BlasterBoltRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        float radius = state.isOvercharged ? 0.35f : (0.12f + state.heatRatio * 0.1f);
        float length = state.isOvercharged ? 1.2f : 0.6f;

        float r = state.isOvercharged ? 1.0f : 1.0f;
        float g = state.isOvercharged ? 0.1f : (0.8f - state.heatRatio * 0.4f);
        float b = state.isOvercharged ? 0.2f : 0.05f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // Glowing outer beam
            KiRenderHelper.drawColoredBox(pose, buffer,
                -radius, -radius, -length,
                radius, radius, length,
                r, g, b, 0.9f
            );

            // Bright white core
            KiRenderHelper.drawColoredBox(pose, buffer,
                -radius * 0.5f, -radius * 0.5f, -length * 0.8f,
                radius * 0.5f, radius * 0.5f, length * 0.8f,
                1.0f, 1.0f, 1.0f, 1.0f
            );
        });

        poseStack.popPose();
    }

    public static class BlasterBoltRenderState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public boolean isOvercharged;
        public float heatRatio;
    }
}
