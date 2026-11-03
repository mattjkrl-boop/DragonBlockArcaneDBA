package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.entity.KiLaserEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * A ki laser is a thin line: white core, one tight colored edge, aimed down the look direction.
 */
public class KiLaserRenderer extends EntityRenderer<KiLaserEntity, KiLaserRenderer.LaserRenderState> {

    public KiLaserRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public LaserRenderState createRenderState() {
        return new LaserRenderState();
    }

    @Override
    public void extractRenderState(KiLaserEntity entity, LaserRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.length = entity.getLength();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
    }

    @Override
    public void submit(LaserRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float r = KiRenderHelper.red(state.color);
        float g = KiRenderHelper.green(state.color);
        float b = KiRenderHelper.blue(state.color);
        RenderType renderType = KiRenderHelper.kiRenderType();
        float laserLength = state.length;

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawCylinder(pose, buffer, 0.06f, 0f, laserLength, 8, r, g, b, 0.4f);
            KiRenderHelper.drawCylinder(pose, buffer, 0.02f, 0f, laserLength, 6, 1f, 1f, 1f, 1f);
        });
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class LaserRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float length = 45.0f;
        public float yRot = 0;
        public float xRot = 0;
    }
}
