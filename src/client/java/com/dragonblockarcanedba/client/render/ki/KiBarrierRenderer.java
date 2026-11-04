package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.entity.KiBarrierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * A ki barrier is a curved shell around the caster: a displaced dome pair
 * for the outer surface, a softer inner glow dome pair, and broken vortex
 * band rims, one flat and one vertical. The builders emit double-sided
 * geometry, so the wearer sees the shell from inside with no second wound
 * copy. Seeded per cast, so every barrier bulges differently.
 */
public class KiBarrierRenderer extends EntityRenderer<KiBarrierEntity, KiBarrierRenderer.BarrierRenderState> {

    public KiBarrierRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BarrierRenderState createRenderState() {
        return new BarrierRenderState();
    }

    @Override
    public void extractRenderState(KiBarrierEntity entity, BarrierRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.ageInTicks = entity.tickCount + partialTicks;
        int percent = entity.getUsedPercent();
        if (percent < 1) {
            percent = 1;
        }
        if (percent > 100) {
            percent = 100;
        }
        state.usedPercent = percent;
        state.seed = entity.getSeed();
    }

    @Override
    public void submit(BarrierRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float r = KiRenderHelper.red(state.color);
        float g = KiRenderHelper.green(state.color);
        float b = KiRenderHelper.blue(state.color);
        float glowR = r + (1.0f - r) * 0.7f;
        float glowG = g + (1.0f - g) * 0.7f;
        float glowB = b + (1.0f - b) * 0.7f;
        float age = state.ageInTicks;
        float breathe = 0.5f + 0.5f * Mth.sin(age * 0.22f);
        float radius = (1.02f + 0.28f * (state.usedPercent / 100.0f)) * (1.0f + 0.02f * breathe);
        float shellA = 0.22f + 0.06f * breathe;
        float glowA = 0.12f + 0.05f * breathe;
        float rimA = 0.82f;
        float spin = age * 2.4f;
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        long seed = state.seed;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().translate(0.0f, 0.95f, 0.0f));
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(spin)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed, radius, r, g, b, shellA, 1f, distScale);
            PoseStack.Pose shellFlip = pose.copy();
            shellFlip.rotate(Axis.XP.rotationDegrees(180f));
            BespokeGeometry.displacedDome(shellFlip, buffer, seed ^ 0x77AAL, radius, r, g, b, shellA, 1f, distScale);
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0x2B3CL, radius * 0.62f, glowR, glowG, glowB, glowA, 1f, distScale);
            PoseStack.Pose glowFlip = pose.copy();
            glowFlip.rotate(Axis.XP.rotationDegrees(180f));
            BespokeGeometry.displacedDome(glowFlip, buffer, seed ^ 0x4D5EL, radius * 0.62f, glowR, glowG, glowB, glowA, 1f, distScale);
        });
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().translate(0.0f, 0.95f, 0.0f));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x99L, radius * 0.97f, -0.03f, 0.03f, 0f,
                1.0f, 1.0f, 1.0f, rimA, distScale);
        });
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().translate(0.0f, 0.95f, 0.0f));
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(spin * 1.7f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack.Pose p = pose.copy();
            p.rotate(Axis.XP.rotationDegrees(90f));
            BespokeGeometry.brokenVortexBand(p, buffer, seed ^ 0xB7L, radius * 0.97f, -0.03f, 0.03f, 0f,
                glowR, glowG, glowB, rimA * 0.85f, distScale);
        });
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class BarrierRenderState extends EntityRenderState {
        public int color = 0xFF55DDFF;
        public float ageInTicks;
        public int usedPercent = 50;
        public int seed = 0;
    }
}
