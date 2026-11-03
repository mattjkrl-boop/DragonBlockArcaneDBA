package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.entity.KiExplosionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * A ki explosion is one shell and a white core. It eases open, then fades out before the entity ends.
 */
public class KiExplosionRenderer extends EntityRenderer<KiExplosionEntity, KiExplosionRenderer.ExplosionRenderState> {

    public KiExplosionRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ExplosionRenderState createRenderState() {
        return new ExplosionRenderState();
    }

    @Override
    public void extractRenderState(KiExplosionEntity entity, ExplosionRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.radius = entity.getRadius();
        state.ageInTicks = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(ExplosionRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float r = KiRenderHelper.red(state.color);
        float g = KiRenderHelper.green(state.color);
        float b = KiRenderHelper.blue(state.color);
        RenderType renderType = KiRenderHelper.kiRenderType();

        float open = Math.min(1f, state.ageInTicks / 8f);
        float ease = 1f - (1f - open) * (1f - open);
        float fade = Math.max(0f, 1f - state.ageInTicks / 36f);
        fade = fade * fade;
        if (fade <= 0.02f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float radius = ease * state.radius;
        float shellA = 0.55f * fade;
        float coreA = 0.9f * fade;
        float ringA = 0.7f * fade * (1f - open);
        float ring = radius * (0.85f + open * 0.35f);

        poseStack.pushPose();
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawSphere(pose, buffer, radius, 8, 12, r, g, b, shellA);
            KiRenderHelper.drawSphere(pose, buffer, radius * 0.38f, 6, 8, 1f, 1f, 1f, coreA);
        });
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawDisc(pose, buffer, ring * 0.92f, ring, 24, r, g, b, ringA);
        });
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class ExplosionRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float radius = 1.0f;
        public float ageInTicks = 0;
    }
}
