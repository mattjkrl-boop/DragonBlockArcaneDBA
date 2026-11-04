package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.entity.KiBeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * A ki beam is crossed torn ribbons: a white core inside a colored sheath,
 * aimed down the look direction. The ribbons are seeded per cast, so no two
 * beams tear the same way.
 */
public class KiBeamRenderer extends EntityRenderer<KiBeamEntity, KiBeamRenderer.BeamRenderState> {

    public KiBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BeamRenderState createRenderState() {
        return new BeamRenderState();
    }

    @Override
    public void extractRenderState(KiBeamEntity entity, BeamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.length = entity.getLength();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.scale = entity.getScale();
        state.seed = entity.getSeed();
    }

    @Override
    public void submit(BeamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float r = KiRenderHelper.red(state.color);
        float g = KiRenderHelper.green(state.color);
        float b = KiRenderHelper.blue(state.color);
        RenderType renderType = KiRenderHelper.kiRenderType();
        float beamLength = state.length;
        float life = Math.min(1f, state.ageInTicks / 20f);
        float alpha = 1f - life * life;
        float scale = state.scale;
        float sheathW = 0.44f * scale;
        float coreW = 0.14f * scale;
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        long seed = state.seed;

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            beamRibbon(pose, buffer, seed, beamLength, sheathW, r, g, b, 0.45f, alpha, distScale, 0f);
            beamRibbon(pose, buffer, seed ^ 0x9E3779B9L, beamLength, sheathW, r, g, b, 0.45f, alpha, distScale, 90f);
            beamRibbon(pose, buffer, seed ^ 0x85EBCA6BL, beamLength, coreW, 1f, 1f, 1f, 0.95f, alpha, distScale, 45f);
            beamRibbon(pose, buffer, seed ^ 0xC2B2AE35L, beamLength, coreW, 1f, 1f, 1f, 0.95f, alpha, distScale, 135f);
        });
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }

    /** One torn ribbon lying along the beam axis: roll around the axis, then turn the ribbon run onto +Z. */
    private static void beamRibbon(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                   float length, float width,
                                   float r, float g, float b, float alpha,
                                   float envelope, float distScale, float rollDeg) {
        PoseStack.Pose p = pose.copy();
        p.rotate(Axis.ZP.rotationDegrees(rollDeg));
        p.rotate(Axis.YP.rotationDegrees(-90f));
        BespokeGeometry.tornRibbon(p, buffer, seed, length, width, 0f, r, g, b, alpha, envelope, distScale);
    }

    public static class BeamRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float length = 50.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
        public float scale = 1.0f;
        public int seed = 0;
    }
}
