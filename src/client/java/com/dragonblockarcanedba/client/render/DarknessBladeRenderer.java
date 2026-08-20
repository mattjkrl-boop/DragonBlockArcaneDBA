package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DarknessBladeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Darkness Blade falling execution in MC 26.2.
 */
public class DarknessBladeRenderer extends EntityRenderer<DarknessBladeEntity, DarknessBladeRenderer.DarknessBladeRenderState> {
    public DarknessBladeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public DarknessBladeRenderState createRenderState() {
        return new DarknessBladeRenderState();
    }

    @Override
    public void extractRenderState(DarknessBladeEntity entity, DarknessBladeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
    }

    @Override
    public void submit(DarknessBladeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f)); // Point downward

        float bladeLength = 6.0f;
        float bladeWidth = 0.5f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // Dark abyssal blade geometry
            KiRenderHelper.drawColoredBox(pose, buffer,
                -bladeWidth, -bladeLength * 0.5f, -bladeWidth,
                bladeWidth, bladeLength * 0.5f, bladeWidth,
                0.05f, 0.0f, 0.1f, 0.95f
            );
            // Glowing purple edge
            KiRenderHelper.drawColoredBox(pose, buffer,
                -bladeWidth * 0.5f, -bladeLength * 0.5f - 0.2f, -bladeWidth * 0.5f,
                bladeWidth * 0.5f, bladeLength * 0.5f + 0.2f, bladeWidth * 0.5f,
                0.6f, 0.1f, 0.9f, 0.85f
            );
        });

        poseStack.popPose();
    }

    public static class DarknessBladeRenderState extends EntityRenderState {
        public float yRot;
        public float xRot;
    }
}
