package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.entity.KiDiskEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Renders a Ki Disk (Destructo Disk) as a flat, fast-spinning disc
 * with a bright razor edge.
 */
public class KiDiskRenderer extends EntityRenderer<KiDiskEntity, KiDiskRenderer.DiskRenderState> {

    public KiDiskRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public DiskRenderState createRenderState() {
        return new DiskRenderState();
    }

    @Override
    public void extractRenderState(KiDiskEntity entity, DiskRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.ageInTicks = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(DiskRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float r = KiRenderHelper.red(state.color);
        float g = KiRenderHelper.green(state.color);
        float b = KiRenderHelper.blue(state.color);
        RenderType renderType = KiRenderHelper.kiRenderType();

        float diskRadius = 0.75f;
        float diskHalfHeight = 0.03f;

        // Spinning inner disk
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.ageInTicks * 40.0f));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -diskRadius, -diskHalfHeight, -diskRadius,
                    diskRadius, diskHalfHeight, diskRadius,
                    r, g, b, 0.85f);
        });
        poseStack.popPose();

        // Outer edge ring — white-hot, slightly larger
        float edgeRadius = 0.85f;
        float edgeHalf = 0.015f;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.ageInTicks * 40.0f));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -edgeRadius, -edgeHalf, -edgeRadius,
                    edgeRadius, edgeHalf, edgeRadius,
                    1.0f, 1.0f, 1.0f, 0.9f);
        });
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class DiskRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float ageInTicks = 0;
    }
}
