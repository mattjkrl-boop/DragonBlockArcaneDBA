package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.entity.KiSpiralBeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * A spiral beam is a core cylinder with two ribbons winding around it.
 */
public class KiSpiralBeamRenderer extends EntityRenderer<KiSpiralBeamEntity, KiSpiralBeamRenderer.SpiralBeamRenderState> {

    public KiSpiralBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public SpiralBeamRenderState createRenderState() {
        return new SpiralBeamRenderState();
    }

    @Override
    public void extractRenderState(KiSpiralBeamEntity entity, SpiralBeamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.length = entity.getLength();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(SpiralBeamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float r = KiRenderHelper.red(state.color);
        float g = KiRenderHelper.green(state.color);
        float b = KiRenderHelper.blue(state.color);
        RenderType renderType = KiRenderHelper.kiRenderType();
        float beamLength = state.length;
        float phase = state.ageInTicks * 0.35f;
        int steps = Math.min(40, Math.max(8, (int) (beamLength * 1.2f)));

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawCylinder(pose, buffer, 0.08f, 0f, beamLength, 8, 1f, 1f, 1f, 0.95f);
            KiRenderHelper.drawHelix(pose, buffer, 0.28f, 0f, beamLength, 3.5f, phase, 0.045f, steps, r, g, b, 0.85f);
            KiRenderHelper.drawHelix(pose, buffer, 0.28f, 0f, beamLength, 3.5f, phase + (float) Math.PI, 0.045f, steps, r, g, b, 0.85f);
        });
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class SpiralBeamRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float length = 50.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
    }
}
