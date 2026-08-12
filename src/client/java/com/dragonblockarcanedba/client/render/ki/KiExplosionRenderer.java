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
 * Renders a Ki Explosion as an expanding, fading sphere of light.
 * Multiple rotated cubes layered to approximate a sphere.
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

        // Expand over first 10 ticks, then hold
        float expansion = Math.min(1.0f, state.ageInTicks / 10.0f);
        float currentRadius = expansion * state.radius;

        // Fade out over 40 ticks
        float alpha = Math.max(0.0f, 1.0f - (state.ageInTicks / 40.0f));
        if (alpha <= 0.01f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float halfSize = currentRadius;

        // Layer 1 — base cube
        poseStack.pushPose();
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -halfSize, -halfSize, -halfSize,
                    halfSize, halfSize, halfSize,
                    r, g, b, alpha * 0.7f);
        });
        poseStack.popPose();

        // Layer 2 — rotated 45° on Y
        float s2 = halfSize * 0.92f;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0f));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -s2, -s2, -s2, s2, s2, s2,
                    r, g, b, alpha * 0.5f);
        });
        poseStack.popPose();

        // Layer 3 — rotated 45° on X
        float s3 = halfSize * 0.85f;
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0f));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -s3, -s3, -s3, s3, s3, s3,
                    r, g, b, alpha * 0.4f);
        });
        poseStack.popPose();

        // White-hot core
        float coreSize = halfSize * 0.4f;
        poseStack.pushPose();
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -coreSize, -coreSize, -coreSize,
                    coreSize, coreSize, coreSize,
                    1.0f, 1.0f, 1.0f, alpha * 0.9f);
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
