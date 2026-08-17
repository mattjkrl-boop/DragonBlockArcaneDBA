package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.GrandCrescentWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Grand Crescent Wave in Minecraft 26.2.
 * Renders a wide, glowing golden-white crescent energy slash.
 */
public class GrandCrescentWaveRenderer extends EntityRenderer<GrandCrescentWaveEntity, GrandCrescentWaveRenderer.GrandCrescentWaveRenderState> {
    public GrandCrescentWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public GrandCrescentWaveRenderState createRenderState() {
        return new GrandCrescentWaveRenderState();
    }

    @Override
    public void extractRenderState(GrandCrescentWaveEntity entity, GrandCrescentWaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(GrandCrescentWaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        float width = 3.2f;
        float height = 0.12f;
        float depth = 0.8f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // 1. Golden Radiant Outer Glow
            KiRenderHelper.drawColoredBox(pose, buffer,
                -width - 0.3f, -height * 1.5f, -depth - 0.2f,
                width + 0.3f, height * 1.5f, depth + 0.2f,
                1.0f, 0.85f, 0.1f, 0.85f // Gold
            );

            // 2. Pure White Radiant Sharp Core
            KiRenderHelper.drawColoredBox(pose, buffer,
                -width, -height, -depth,
                width, height, depth,
                1.0f, 1.0f, 0.95f, 0.95f // Bright White-Gold
            );

            // 3. Amber Trail Wings
            KiRenderHelper.drawColoredBox(pose, buffer,
                -width - 0.5f, -height * 0.5f, -depth * 0.5f,
                width + 0.5f, height * 0.5f, depth * 0.5f,
                1.0f, 0.65f, 0.0f, 0.70f
            );
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    public static class GrandCrescentWaveRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
    }
}
