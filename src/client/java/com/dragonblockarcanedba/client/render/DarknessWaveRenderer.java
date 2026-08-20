package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DarknessWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Darkness Wave in Minecraft 26.2.
 * Renders a wide, glowing black/purple crescent darkness slash.
 */
public class DarknessWaveRenderer extends EntityRenderer<DarknessWaveEntity, DarknessWaveRenderer.DarknessWaveRenderState> {
    public DarknessWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public DarknessWaveRenderState createRenderState() {
        return new DarknessWaveRenderState();
    }

    @Override
    public void extractRenderState(DarknessWaveEntity entity, DarknessWaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.isSecondary = entity.isSecondary();
    }

    @Override
    public void submit(DarknessWaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        float width = state.isSecondary ? 2.2f : 3.5f;
        float height = 0.12f;
        float depth = 0.8f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // 1. Abyssal Black Outer Glow
            KiRenderHelper.drawColoredBox(pose, buffer,
                -width - 0.3f, -height * 1.6f, -depth - 0.2f,
                width + 0.3f, height * 1.6f, depth + 0.2f,
                0.08f, 0.0f, 0.15f, 0.95f // Pitch Black/Deep Violet
            );

            // 2. Dark Purple Core
            KiRenderHelper.drawColoredBox(pose, buffer,
                -width, -height, -depth,
                width, height, depth,
                0.45f, 0.05f, 0.75f, 0.9f // Purple Void Core
            );
        });

        poseStack.popPose();
    }

    public static class DarknessWaveRenderState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public boolean isSecondary;
    }
}
