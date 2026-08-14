package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DimensionalSlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class DimensionalSlashRenderer extends EntityRenderer<DimensionalSlashEntity, DimensionalSlashRenderer.SlashRenderState> {
    public DimensionalSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public SlashRenderState createRenderState() {
        return new SlashRenderState();
    }

    @Override
    public void extractRenderState(DimensionalSlashEntity entity, SlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tiltRight = entity.getTilt();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
    }

    @Override
    public void submit(SlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        // Face movement direction
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        // Tilt diagonally
        if (state.tiltRight) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(45.0f));
        } else {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0f));
        }

        // Draw thin, sharp purple wave
        float width = 1.5f;
        float height = 0.05f;
        float depth = 0.2f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // Core - Bright Red
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -width, -height, -depth,
                    width, height, depth,
                    1.0f, 0.1f, 0.1f, 0.9f); // Red
                    
            // Edge - Purple
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -width - 0.2f, -height * 0.5f, -depth - 0.1f,
                    width + 0.2f, height * 0.5f, depth + 0.1f,
                    0.5f, 0.0f, 0.8f, 0.7f); // Purple
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    public static class SlashRenderState extends EntityRenderState {
        public boolean tiltRight = false;
        public float yRot = 0;
        public float xRot = 0;
    }
}
