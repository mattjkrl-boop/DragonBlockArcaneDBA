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
 * Renders a Ki Laser as a thin, bright, instant line of energy.
 * White-hot core with colored glow.
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

        // Thin white-hot core
        float thinRadius = 0.04f;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -thinRadius, -thinRadius, 0,
                    thinRadius, thinRadius, laserLength,
                    1.0f, 1.0f, 1.0f, 1.0f);
        });
        poseStack.popPose();

        // Colored glow around it
        float glowRadius = 0.1f;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -glowRadius, -glowRadius, -0.05f,
                    glowRadius, glowRadius, laserLength + 0.1f,
                    r, g, b, 0.5f);
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
