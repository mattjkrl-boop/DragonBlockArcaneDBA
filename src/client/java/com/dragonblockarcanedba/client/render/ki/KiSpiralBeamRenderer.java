package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.entity.KiSpiralBeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

/**
 * Renders a Spiral Beam — a core beam with two helix tendrils rotating around it.
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
    }

    @Override
    public void submit(SpiralBeamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float r = KiRenderHelper.red(state.color);
        float g = KiRenderHelper.green(state.color);
        float b = KiRenderHelper.blue(state.color);
        RenderType renderType = KiRenderHelper.kiRenderType();

        float beamLength = state.length;
        float pulse = 1.0f + 0.06f * Mth.sin(state.ageInTicks * 1.5f);
        float age = state.ageInTicks;

        // Inner core beam
        float coreRadius = 0.12f * pulse;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -coreRadius, -coreRadius, 0,
                    coreRadius, coreRadius, beamLength,
                    r, g, b, 0.9f);
        });
        poseStack.popPose();

        // Spiral tendrils — draw small cubes along a helix path
        float spiralRadius = 0.35f;
        float spiralSegSize = 0.08f;
        int segments = Math.min((int)(beamLength * 2), 120); // cap segments
        float spiralSpeed = age * 3.0f;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            for (int i = 0; i < segments; i++) {
                float z = (i / (float)segments) * beamLength;
                float angle = spiralSpeed + (i * 0.8f);

                // Tendril 1
                float tx1 = Mth.cos(angle) * spiralRadius;
                float ty1 = Mth.sin(angle) * spiralRadius;
                KiRenderHelper.drawColoredBox(pose, buffer,
                        tx1 - spiralSegSize, ty1 - spiralSegSize, z - spiralSegSize,
                        tx1 + spiralSegSize, ty1 + spiralSegSize, z + spiralSegSize,
                        r, g, b, 0.6f);

                // Tendril 2 (opposite side)
                float tx2 = Mth.cos(angle + Mth.PI) * spiralRadius;
                float ty2 = Mth.sin(angle + Mth.PI) * spiralRadius;
                KiRenderHelper.drawColoredBox(pose, buffer,
                        tx2 - spiralSegSize, ty2 - spiralSegSize, z - spiralSegSize,
                        tx2 + spiralSegSize, ty2 + spiralSegSize, z + spiralSegSize,
                        r, g, b, 0.6f);
            }
        });
        poseStack.popPose();

        // Outer glow
        float glowRadius = 0.25f * pulse;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawColoredBox(pose, buffer,
                    -glowRadius, -glowRadius, -0.1f,
                    glowRadius, glowRadius, beamLength + 0.2f,
                    r, g, b, 0.2f);
        });
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class SpiralBeamRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float length = 50.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
    }
}
