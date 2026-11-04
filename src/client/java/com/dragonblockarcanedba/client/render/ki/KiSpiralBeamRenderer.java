package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.entity.KiSpiralBeamEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
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
 * A spiral beam is a white ribbon core wrapped in a broken vortex sheath:
 * short seeded band segments stacked along the beam, each rolled a step
 * further than the last, so the sheath winds 3.5 turns over the full length
 * and keeps turning with age.
 */
public class KiSpiralBeamRenderer extends EntityRenderer<KiSpiralBeamEntity, KiSpiralBeamRenderer.SpiralBeamRenderState> {

    public KiSpiralBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public SpiralBeamRenderState createRenderState() {
        return new SpiralBeamRenderState();
    }

    @Override
    public void extractRenderState(KiSpiralBeamEntity entity, SpiralBeamRenderState state, float partialTicks) {
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
    public void submit(SpiralBeamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float r = KiRenderHelper.red(state.color);
        float g = KiRenderHelper.green(state.color);
        float b = KiRenderHelper.blue(state.color);
        RenderType renderType = KiRenderHelper.kiRenderType();
        float beamLength = state.length;
        float phaseDeg = (float) Math.toDegrees(state.ageInTicks * 0.35f);
        float scale = state.scale;
        float coreW = 0.16f * scale;
        float helix = 0.28f * scale;
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        long seed = state.seed;
        int segs = FxEnvelope.segments(16, distScale, 6);
        float segLen = beamLength / segs;
        float totalTurnDeg = 3.5f * 360f;

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            coreRibbon(pose, buffer, seed, beamLength, coreW, distScale, 0f);
            coreRibbon(pose, buffer, seed ^ 0x9E3779B9L, beamLength, coreW, distScale, 90f);
            for (int i = 0; i < segs; i++) {
                PoseStack.Pose p = pose.copy();
                p.translate(0f, 0f, i * segLen);
                p.rotate(Axis.ZP.rotationDegrees(phaseDeg + totalTurnDeg * (i / (float) segs)));
                p.rotate(Axis.XP.rotationDegrees(90f));
                BespokeGeometry.brokenVortexBand(p, buffer, seed + i * 131L, helix, 0f, segLen, 0f,
                    r, g, b, 0.85f, distScale);
            }
        });
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }

    /** One white torn ribbon lying along the beam axis: roll around the axis, then turn the run onto +Z. */
    private static void coreRibbon(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                   float length, float width, float distScale, float rollDeg) {
        PoseStack.Pose p = pose.copy();
        p.rotate(Axis.ZP.rotationDegrees(rollDeg));
        p.rotate(Axis.YP.rotationDegrees(-90f));
        BespokeGeometry.tornRibbon(p, buffer, seed, length, width, 0f, 1f, 1f, 1f, 0.95f, 1f, distScale);
    }

    public static class SpiralBeamRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float length = 50.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
        public float scale = 1.0f;
        public int seed = 0;
    }
}
