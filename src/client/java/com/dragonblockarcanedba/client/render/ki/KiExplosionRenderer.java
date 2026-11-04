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
 * A ki explosion is a displaced dome shell around a white core, with a plume
 * rising through the middle and a broken vortex ring racing along the
 * ground. It eases open, then fades out before the entity ends. The domes
 * are seeded per blast, so no two explosions bulge the same way.
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
        state.seed = entity.getSeed();
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
        float ring = radius * (0.85f + open * 0.35f);
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        long seed = state.seed;
        float fadeEnv = fade;

        poseStack.pushPose();
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed, radius, r, g, b, 0.55f, fadeEnv, distScale);
            PoseStack.Pose shellFlip = pose.copy();
            shellFlip.rotate(Axis.XP.rotationDegrees(180f));
            BespokeGeometry.displacedDome(shellFlip, buffer, seed ^ 0x77AAL, radius, r, g, b, 0.55f, fadeEnv, distScale);
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0x2B3CL, radius * 0.38f, 1f, 1f, 1f, 0.9f, fadeEnv, distScale);
            PoseStack.Pose coreFlip = pose.copy();
            coreFlip.rotate(Axis.XP.rotationDegrees(180f));
            BespokeGeometry.displacedDome(coreFlip, buffer, seed ^ 0x4D5EL, radius * 0.38f, 1f, 1f, 1f, 0.9f, fadeEnv, distScale);
            BespokeGeometry.plume(pose, buffer, seed ^ 0x1234L, radius * 0.5f, radius * 0.9f, r, g, b, 0.35f, fadeEnv, distScale);
        });
        poseStack.popPose();

        poseStack.pushPose();
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x99L, ring, -0.04f, 0.04f, 0f,
                r, g, b, 0.7f * fadeEnv * (1f - open), distScale);
        });
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class ExplosionRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float radius = 1.0f;
        public float ageInTicks = 0;
        public int seed = 0;
    }
}
