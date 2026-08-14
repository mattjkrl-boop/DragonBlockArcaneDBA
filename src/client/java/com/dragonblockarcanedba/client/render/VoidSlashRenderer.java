package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.VoidSlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class VoidSlashRenderer extends EntityRenderer<VoidSlashEntity, VoidSlashRenderer.SlashRenderState> {
    public VoidSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SlashRenderState extends EntityRenderState {
        public boolean tiltRight = false;
        public float yRot = 0;
        public float xRot = 0;
    }

    @Override
    public SlashRenderState createRenderState() {
        return new SlashRenderState();
    }

    @Override
    public void extractRenderState(VoidSlashEntity entity, SlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tiltRight = entity.getTilt();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
    }

    @Override
    public void submit(SlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        if (state.tiltRight) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(40.0f));
        } else {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-40.0f));
        }

        // Giant dark purple void crescent
        float width = 2.8f;
        float height = 0.08f;
        float depth = 0.4f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // Core - Dark Violet
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -width, -height, -depth,
                    width, height, depth,
                    0.4f, 0.0f, 0.7f, 0.95f);
                    
            // Outer Blade Edge - Ethereal Cyan/Purple
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -width - 0.4f, -height * 0.5f, -depth - 0.2f,
                    width + 0.4f, height * 0.5f, depth + 0.2f,
                    0.2f, 0.0f, 0.4f, 0.65f);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
