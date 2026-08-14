package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.HollowAfterimageEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class HollowAfterimageRenderer extends EntityRenderer<HollowAfterimageEntity, HollowAfterimageRenderer.AfterimageRenderState> {

    public HollowAfterimageRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AfterimageRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public int skinColor = 0xFF8CC8FF;
        public int hairColor = 0xFF1EB4FF;
        public float age = 0;
    }

    @Override
    public AfterimageRenderState createRenderState() {
        return new AfterimageRenderState();
    }

    @Override
    public void extractRenderState(HollowAfterimageEntity entity, AfterimageRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getSyncYaw();
        state.xRot = entity.getSyncPitch();
        state.skinColor = entity.getSkinColor();
        state.hairColor = entity.getHairColor();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(AfterimageRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));

        float alpha = Math.max(0.1f, 0.65f * (1.0f - (state.age / 100.0f)));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // Torso
            KiRenderHelper.drawColoredBox(pose, buffer,
                -0.25f, 0.7f, -0.15f,
                0.25f, 1.4f, 0.15f,
                0.1f, 0.0f, 0.2f, alpha);

            // Head
            KiRenderHelper.drawColoredBox(pose, buffer,
                -0.2f, 1.4f, -0.2f,
                0.2f, 1.8f, 0.2f,
                0.15f, 0.05f, 0.3f, alpha);

            // Left Arm
            KiRenderHelper.drawColoredBox(pose, buffer,
                -0.45f, 0.7f, -0.12f,
                -0.25f, 1.4f, 0.12f,
                0.1f, 0.0f, 0.2f, alpha * 0.8f);

            // Right Arm
            KiRenderHelper.drawColoredBox(pose, buffer,
                0.25f, 0.7f, -0.12f,
                0.45f, 1.4f, 0.12f,
                0.1f, 0.0f, 0.2f, alpha * 0.8f);

            // Legs
            KiRenderHelper.drawColoredBox(pose, buffer,
                -0.22f, 0.0f, -0.12f,
                -0.02f, 0.7f, 0.12f,
                0.08f, 0.0f, 0.15f, alpha * 0.9f);
            KiRenderHelper.drawColoredBox(pose, buffer,
                0.02f, 0.0f, -0.12f,
                0.22f, 0.7f, 0.12f,
                0.08f, 0.0f, 0.15f, alpha * 0.9f);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
