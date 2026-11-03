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

/**
 * A ki beam is one cylinder: a white core inside a colored sheath, aimed down the look direction.
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
        float life = Math.min(1f, state.ageInTicks / 20f);
        float alpha = 1f - life * life;

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawCylinder(pose, buffer, 0.22f, 0f, beamLength, 12, r, g, b, 0.45f * alpha);
            KiRenderHelper.drawCylinder(pose, buffer, 0.07f, 0f, beamLength, 8, 1f, 1f, 1f, 0.95f * alpha);
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
