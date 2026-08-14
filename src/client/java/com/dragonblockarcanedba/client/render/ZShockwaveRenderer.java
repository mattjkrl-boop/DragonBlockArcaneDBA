package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.ZShockwaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Z Shockwave in Minecraft 26.2.
 * Renders an expansive, glowing crescent energy arc with golden divine hues.
 */
public class ZShockwaveRenderer extends EntityRenderer<ZShockwaveEntity, ZShockwaveRenderer.ZShockwaveRenderState> {
    public ZShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ZShockwaveRenderState createRenderState() {
        return new ZShockwaveRenderState();
    }

    @Override
    public void extractRenderState(ZShockwaveEntity entity, ZShockwaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.isSubWave = entity.isSubWave();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(ZShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        // Align with projectile trajectory
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        float width = state.isSubWave ? (2.0f + state.chargeRatio * 2.5f) : (3.5f + state.chargeRatio * 5.0f);
        float height = 0.08f + (state.chargeRatio * 0.08f);
        float depth = 0.6f + (state.chargeRatio * 0.6f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // 1. Divine Golden Outer Edge
            KiRenderHelper.drawColoredBox(pose, buffer,
                -width - 0.4f, -height * 1.5f, -depth - 0.2f,
                width + 0.4f, height * 1.5f, depth + 0.2f,
                1.0f, 0.84f, 0.0f, 0.85f // Gold
            );

            // 2. Pure White Radiant Core
            KiRenderHelper.drawColoredBox(pose, buffer,
                -width, -height, -depth,
                width, height, depth,
                1.0f, 1.0f, 0.9f, 0.95f // Bright White-Gold
            );

            // 3. Orange Horizon Glow
            KiRenderHelper.drawColoredBox(pose, buffer,
                -width - 0.2f, -height * 0.5f, -depth - 0.4f,
                width + 0.2f, height * 0.5f, depth * 0.5f,
                1.0f, 0.55f, 0.0f, 0.65f // Amber Orange
            );
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    public static class ZShockwaveRenderState extends EntityRenderState {
        public float chargeRatio = 1.0f;
        public boolean isSubWave = false;
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
    }
}
