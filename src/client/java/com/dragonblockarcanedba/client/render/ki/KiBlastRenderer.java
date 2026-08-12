package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.entity.KiBlastEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

/**
 * Renders a Ki Blast as a pulsing, glowing colored sphere (layered cubes).
 * Uses submitCustomGeometry with emissive translucent render type.
 */
public class KiBlastRenderer extends EntityRenderer<KiBlastEntity, KiBlastRenderer.BlastRenderState> {

    public KiBlastRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BlastRenderState createRenderState() {
        return new BlastRenderState();
    }

    @Override
    public void extractRenderState(KiBlastEntity entity, BlastRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.ageInTicks = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(BlastRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float r = KiRenderHelper.red(state.color);
        float g = KiRenderHelper.green(state.color);
        float b = KiRenderHelper.blue(state.color);
        RenderType renderType = KiRenderHelper.kiRenderType();

        float pulse = 1.0f + 0.15f * Mth.sin(state.ageInTicks * 0.8f);

        // Inner core — bright solid
        float coreSize = 0.25f * pulse;
        poseStack.pushPose();
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -coreSize, -coreSize, -coreSize,
                    coreSize, coreSize, coreSize,
                    r, g, b, 1.0f);
        });
        poseStack.popPose();

        // Outer aura — larger, semi-transparent, rotating
        float auraSize = 0.4f * pulse;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.ageInTicks * 3.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.ageInTicks * 2.0f));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -auraSize, -auraSize, -auraSize,
                    auraSize, auraSize, auraSize,
                    r, g, b, 0.4f);
        });
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class BlastRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float ageInTicks = 0;
    }
}
