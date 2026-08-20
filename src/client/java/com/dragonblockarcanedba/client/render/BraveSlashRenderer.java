package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.BraveSlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Brave Slash in MC 26.2.
 * Renders a brilliant golden/cyan crescent energy slash.
 */
public class BraveSlashRenderer extends EntityRenderer<BraveSlashEntity, BraveSlashRenderer.BraveSlashRenderState> {
    public BraveSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BraveSlashRenderState createRenderState() {
        return new BraveSlashRenderState();
    }

    @Override
    public void extractRenderState(BraveSlashEntity entity, BraveSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
    }

    @Override
    public void submit(BraveSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        float width = 3.0f;
        float height = 0.15f;
        float depth = 0.7f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // 1. Radiant Gold Outer Crescent
            KiRenderHelper.drawColoredBox(pose, buffer,
                -width - 0.2f, -height * 1.5f, -depth - 0.2f,
                width + 0.2f, height * 1.5f, depth + 0.2f,
                1.0f, 0.84f, 0.0f, 0.9f // Gold
            );

            // 2. Cyan Heroic Inner Core
            KiRenderHelper.drawColoredBox(pose, buffer,
                -width, -height, -depth,
                width, height, depth,
                0.0f, 0.95f, 1.0f, 0.95f // Cyan Heroic Core
            );
        });

        poseStack.popPose();
    }

    public static class BraveSlashRenderState extends EntityRenderState {
        public float yRot;
        public float xRot;
    }
}
