package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.EvilSpearProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Evil Spear Projectile in MC 26.2.
 * Renders a glowing, spectral crimson spear shaft with sharp spearhead.
 */
public class EvilSpearProjectileRenderer extends EntityRenderer<EvilSpearProjectileEntity, EvilSpearProjectileRenderer.EvilSpearRenderState> {
    public EvilSpearProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EvilSpearRenderState createRenderState() {
        return new EvilSpearRenderState();
    }

    @Override
    public void extractRenderState(EvilSpearProjectileEntity entity, EvilSpearRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
    }

    @Override
    public void submit(EvilSpearRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // 1. Long Crimson Shaft
            KiRenderHelper.drawColoredBox(pose, buffer,
                -0.08f, -0.08f, -1.8f,
                0.08f, 0.08f, 1.8f,
                0.85f, 0.0f, 0.15f, 0.95f
            );

            // 2. Radiant Crimson Spearhead
            KiRenderHelper.drawColoredBox(pose, buffer,
                -0.2f, -0.2f, 1.2f,
                0.2f, 0.2f, 2.2f,
                1.0f, 0.1f, 0.2f, 0.9f
            );

            // 3. Bright White Tip Core
            KiRenderHelper.drawColoredBox(pose, buffer,
                -0.05f, -0.05f, 1.8f,
                0.05f, 0.05f, 2.4f,
                1.0f, 0.8f, 0.8f, 1.0f
            );
        });

        poseStack.popPose();
    }

    public static class EvilSpearRenderState extends EntityRenderState {
        public float yRot;
        public float xRot;
    }
}
