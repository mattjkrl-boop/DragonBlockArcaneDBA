package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.entity.KiDiskEntity;
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
 * A ki disk is a vertical razor: seeded blade arcs for the colored face and
 * the white rim, shard teeth bursting past the rim, spinning as it flies.
 * Each ring is three arcs offset by 120 degrees so no arc seam ever shows
 * as a bare gap.
 */
public class KiDiskRenderer extends EntityRenderer<KiDiskEntity, KiDiskRenderer.DiskRenderState> {

    public KiDiskRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public DiskRenderState createRenderState() {
        return new DiskRenderState();
    }

    @Override
    public void extractRenderState(KiDiskEntity entity, DiskRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.scale = entity.getScale();
        state.seed = entity.getSeed();
    }

    @Override
    public void submit(DiskRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float r = KiRenderHelper.red(state.color);
        float g = KiRenderHelper.green(state.color);
        float b = KiRenderHelper.blue(state.color);
        RenderType renderType = KiRenderHelper.kiRenderType();
        float spin = state.ageInTicks * 24f;
        float scale = state.scale;
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        long seed = state.seed;

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(spin)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack.Pose teeth = pose.copy();
            teeth.rotate(Axis.XP.rotationDegrees(90f));
            BespokeGeometry.shardFan(teeth, buffer, seed ^ 0x51ED27L, 0.85f * scale, r, g, b, 0.85f, 1f, distScale);
            for (int k = 0; k < 3; k++) {
                flatArc(pose, buffer, seed + k * 977L, 0.61f * scale, r, g, b, 0.9f, distScale, k * 120f);
                flatArc(pose, buffer, seed ^ (0xABCDEFL + k * 977L), 0.72f * scale, 1f, 1f, 1f, 1f, distScale, k * 120f + 60f);
            }
        });
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }

    /** One full blade arc stood up into the disc plane, spun within its own plane by spinDeg. */
    private static void flatArc(PoseStack.Pose pose, VertexConsumer buffer, long seed, float radius,
                                float r, float g, float b, float alpha, float distScale, float spinDeg) {
        PoseStack.Pose p = pose.copy();
        p.rotate(Axis.XP.rotationDegrees(90f));
        p.rotate(Axis.YP.rotationDegrees(spinDeg));
        BespokeGeometry.bladeArc(p, buffer, seed, radius, 360f, r, g, b, alpha, 1f, distScale);
    }

    public static class DiskRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float ageInTicks = 0;
        public float yRot = 0;
        public float xRot = 0;
        public float scale = 1.0f;
        public int seed = 0;
    }
}
