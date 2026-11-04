package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.entity.KiBlastEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

/**
 * Entity Renderer for Ki Blast / Energy Laser Bolts in Minecraft 26.2.
 * Renders physical 3D aerodynamic energy laser geometry, all bespoke:
 * - Directionally aligned with entity flight pitch and yaw.
 * - Crossed torn ribbons for the plasma sheath and the white-hot core,
 *   seeded per shot, full width at the piercing nose and torn thin at the
 *   trailing tail.
 * - A shard fan flaring at the nose, a broken vortex band sheath winding
 *   down the wake, and a broken resonance ring around the body.
 */
public class KiBlastRenderer extends EntityRenderer<KiBlastEntity, KiBlastRenderer.BlastRenderState> {

    public KiBlastRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BlastRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float ageInTicks = 0;
        public float yRot = 0;
        public float xRot = 0;
        public float scale = 1.0f;
        public int seed = 0;
    }

    @Override
    public BlastRenderState createRenderState() {
        return new BlastRenderState();
    }

    @Override
    public void extractRenderState(KiBlastEntity entity, BlastRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.scale = entity.getScale();
        state.seed = entity.getSeed();
    }

    @Override
    public void submit(BlastRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float r = KiRenderHelper.red(state.color);
        float g = KiRenderHelper.green(state.color);
        float b = KiRenderHelper.blue(state.color);
        RenderType renderType = KiRenderHelper.kiRenderType();

        float pulse = 1.0f + 0.12f * Mth.sin(state.ageInTicks * 0.9f);
        float scale = state.scale;
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        long seed = state.seed;
        float age = state.ageInTicks;

        poseStack.pushPose();
        // Align with projectile trajectory
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        // Spin along flight axis
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(state.ageInTicks * 12.0f)));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            float radius = 0.20f * pulse * scale;
            float headLen = 0.75f * scale;
            float tailLen = 1.6f * scale;

            // 1. Outer aerodynamic plasma sheath: crossed torn ribbons, nose first
            boltRibbon(pose, buffer, seed, headLen, headLen + tailLen, radius * 2f, r, g, b, 0.85f, distScale, 0f);
            boltRibbon(pose, buffer, seed ^ 0x9E3779B9L, headLen, headLen + tailLen, radius * 2f, r, g, b, 0.85f, distScale, 60f);
            boltRibbon(pose, buffer, seed ^ 0x85EBCA6BL, headLen, headLen + tailLen, radius * 2f, r, g, b, 0.85f, distScale, 120f);

            // 2. White-hot piercing energy core
            float coreLen = headLen * 0.95f + tailLen * 0.65f;
            boltRibbon(pose, buffer, seed ^ 0xC2B2AE35L, headLen * 0.95f, coreLen, radius * 0.9f, 1.0f, 1.0f, 0.95f, 1.0f, distScale, 30f);
            boltRibbon(pose, buffer, seed ^ 0x27D4EB2FL, headLen * 0.95f, coreLen, radius * 0.9f, 1.0f, 1.0f, 0.95f, 1.0f, distScale, 90f);
            boltRibbon(pose, buffer, seed ^ 0x165667B1L, headLen * 0.95f, coreLen, radius * 0.9f, 1.0f, 1.0f, 0.95f, 1.0f, distScale, 150f);

            // 3. Nose flare: shards bursting sideways just behind the tip
            PoseStack.Pose flare = pose.copy();
            flare.translate(0f, 0f, headLen * 0.8f);
            flare.rotate(Axis.XP.rotationDegrees(90f));
            BespokeGeometry.shardFan(flare, buffer, seed ^ 0x51ED27L, radius * 1.6f, r, g, b, 0.9f, 1f, distScale);

            // 4. Helical energy wake: two broken band segments winding down the tail
            float wakeTotal = tailLen + 0.2f;
            float segLen = wakeTotal / 2f;
            for (int i = 0; i < 2; i++) {
                float fracMid = (i + 0.5f) / 2f;
                float rollDeg = (float) Math.toDegrees(age * 0.5 + fracMid * Math.PI * 3.0);
                PoseStack.Pose p = pose.copy();
                p.translate(0f, 0f, 0.2f - (i + 1) * segLen);
                p.rotate(Axis.ZP.rotationDegrees(rollDeg));
                p.rotate(Axis.XP.rotationDegrees(90f));
                BespokeGeometry.brokenVortexBand(p, buffer, seed + i * 131L,
                    radius * 1.35f * (1.0f - fracMid * 0.4f), 0f, segLen, 0f,
                    r, g, b, (1.0f - fracMid) * 0.70f, distScale);
            }

            // 5. Perpendicular energy resonance ring
            PoseStack.Pose ringPose = pose.copy();
            ringPose.rotate(Axis.XP.rotationDegrees(90f));
            BespokeGeometry.brokenVortexBand(ringPose, buffer, seed ^ 0x99L, radius * 1.5f, -0.03f, 0.03f, 0f,
                r, g, b, 0.75f, distScale);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /**
     * One torn ribbon lying along the bolt axis with its full width at the
     * nose: shift to the nose, roll around the axis, then turn the ribbon
     * run onto -Z so it tapers toward the tail.
     */
    private static void boltRibbon(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                   float noseZ, float length, float width,
                                   float r, float g, float b, float alpha,
                                   float distScale, float rollDeg) {
        PoseStack.Pose p = pose.copy();
        p.translate(0f, 0f, noseZ);
        p.rotate(Axis.ZP.rotationDegrees(rollDeg));
        p.rotate(Axis.YP.rotationDegrees(90f));
        BespokeGeometry.tornRibbon(p, buffer, seed, length, width, 0f, r, g, b, alpha, 1f, distScale);
    }
}
