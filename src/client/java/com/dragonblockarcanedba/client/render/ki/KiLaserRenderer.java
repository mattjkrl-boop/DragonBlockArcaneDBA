package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.entity.KiLaserEntity;
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
 * A ki laser is a thin line: crossed torn ribbons, a white core inside one
 * tight colored edge, aimed down the look direction and seeded per shot.
 */
public class KiLaserRenderer extends EntityRenderer<KiLaserEntity, KiLaserRenderer.LaserRenderState> {

    public KiLaserRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public LaserRenderState createRenderState() {
        return new LaserRenderState();
    }

    @Override
    public void extractRenderState(KiLaserEntity entity, LaserRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.length = entity.getLength();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.scale = entity.getScale();
        state.seed = entity.getSeed();
    }

    @Override
    public void submit(LaserRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float r = KiRenderHelper.red(state.color);
        float g = KiRenderHelper.green(state.color);
        float b = KiRenderHelper.blue(state.color);
        RenderType renderType = KiRenderHelper.kiRenderType();
        float laserLength = state.length;
        float scale = state.scale;
        float sheathW = 0.12f * scale;
        float coreW = 0.04f * scale;
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        long seed = state.seed;

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            laserRibbon(pose, buffer, seed, laserLength, sheathW, r, g, b, 0.4f, distScale, 0f);
            laserRibbon(pose, buffer, seed ^ 0x9E3779B9L, laserLength, sheathW, r, g, b, 0.4f, distScale, 90f);
            laserRibbon(pose, buffer, seed ^ 0x85EBCA6BL, laserLength, coreW, 1f, 1f, 1f, 1f, distScale, 45f);
            laserRibbon(pose, buffer, seed ^ 0xC2B2AE35L, laserLength, coreW, 1f, 1f, 1f, 1f, distScale, 135f);
        });
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }

    /** One torn ribbon lying along the laser axis: roll around the axis, then turn the ribbon run onto +Z. */
    private static void laserRibbon(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                    float length, float width,
                                    float r, float g, float b, float alpha,
                                    float distScale, float rollDeg) {
        PoseStack.Pose p = pose.copy();
        p.rotate(Axis.ZP.rotationDegrees(rollDeg));
        p.rotate(Axis.YP.rotationDegrees(-90f));
        BespokeGeometry.tornRibbon(p, buffer, seed, length, width, 0f, r, g, b, alpha, 1f, distScale);
    }

    public static class LaserRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float length = 45.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float scale = 1.0f;
        public int seed = 0;
    }
}
