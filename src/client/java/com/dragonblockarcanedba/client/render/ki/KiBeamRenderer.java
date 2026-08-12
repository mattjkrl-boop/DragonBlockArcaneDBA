package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.entity.KiBeamEntity;
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
 * Renders a Ki Beam as a long colored cylinder of light.
 * Inner core beam + outer translucent glow, oriented along look direction.
 */
public class KiBeamRenderer extends EntityRenderer<KiBeamEntity, KiBeamRenderer.BeamRenderState> {

    public KiBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BeamRenderState createRenderState() {
        return new BeamRenderState();
    }

    @Override
    public void extractRenderState(KiBeamEntity entity, BeamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.length = entity.getLength();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(BeamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float r = KiRenderHelper.red(state.color);
        float g = KiRenderHelper.green(state.color);
        float b = KiRenderHelper.blue(state.color);
        RenderType renderType = KiRenderHelper.kiRenderType();

        float beamLength = state.length;
        float pulse = 1.0f + 0.08f * Mth.sin(state.ageInTicks * 1.2f);

        // Inner core beam
        float coreRadius = 0.15f * pulse;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -coreRadius, -coreRadius, 0,
                    coreRadius, coreRadius, beamLength,
                    r, g, b, 0.9f);
        });
        poseStack.popPose();

        // Outer glow
        float glowRadius = 0.3f * pulse;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -glowRadius, -glowRadius, -0.1f,
                    glowRadius, glowRadius, beamLength + 0.2f,
                    r, g, b, 0.3f);
        });
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class BeamRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float length = 50.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
    }
}
