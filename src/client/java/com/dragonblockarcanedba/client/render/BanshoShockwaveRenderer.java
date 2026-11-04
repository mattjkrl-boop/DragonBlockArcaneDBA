package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.BanshoShockwaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Bansho Fan Shockwaves in Minecraft 26.2.
 * Handles both the directional emerald conical launch compression waves
 * (Tempest Barrage) and the radial emerald ground/impact shockwaves (Gale
 * Force and projectile hits). All geometry is bespoke toolkit work seeded by
 * the entity's synced id: the cone is a pair of rotated plumes with shard fan
 * lips, the radial burst is stacked broken vortex bands over a glow fan core
 * with blade arc crescents, on the same 14 tick progress envelope as before.
 */
public class BanshoShockwaveRenderer extends EntityRenderer<BanshoShockwaveEntity, BanshoShockwaveRenderer.ShockwaveRenderState> {

    public BanshoShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShockwaveRenderState extends EntityRenderState {
        public float yaw = 0;
        public float pitch = 0;
        public float maxRadius = 3.0f;
        public boolean isCone = false;
        public boolean orientToImpact = false;
        public float age = 0;
        public double ex, ey, ez;
        public long seed;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BanshoShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BanshoShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ShockwaveRenderState createRenderState() {
        return new ShockwaveRenderState();
    }

    @Override
    public void extractRenderState(BanshoShockwaveEntity entity, ShockwaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = entity.getEntityYaw();
        state.pitch = entity.getEntityPitch();
        state.maxRadius = entity.getMaxRadius();
        state.isCone = entity.isCone();
        state.orientToImpact = entity.isOrientToImpact();
        state.age = entity.tickCount + partialTicks;
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L ^ (int) (entity.getMaxRadius() * 64.0f) ^ 0x510C4L;
    }

    @Override
    public void submit(ShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = 14.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float exitT = progress <= 0.55f ? 0.0f : Math.min(1.0f, (progress - 0.55f) / 0.45f);
        float alpha = (1.0f - exitT * exitT * (3.0f - 2.0f * exitT)) * 0.95f;
        float distScale = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);
        RenderType renderType = KiRenderHelper.kiRenderType();
        final long seed = state.seed;

        poseStack.pushPose();

        if (state.isCone) {
            // Directional Conical Launch Shockwave (Tempest Barrage)
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yaw)));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.pitch)));

            float expansion = (float) Math.sin(progress * (Math.PI / 2.0));
            float coneLength = state.maxRadius * (0.4f + 0.8f * expansion);
            float coneRadius = state.maxRadius * (0.3f + 0.9f * expansion);
            float mouthR = coneRadius * 1.02f;
            float follow = progress <= 0.40f ? 0.0f : Math.min(1.0f, (progress - 0.40f) / 0.40f);
            float needleReach = 1.05f + (1.0f - follow) * 0.35f;

            // 1. The surging cone itself: two bespoke plumes rotated onto the
            // shot axis, an emerald outer surge and a pale core surge. The
            // plume's jittered puff discs replace the ruled cone shells.
            poseStack.pushPose();
            poseStack.translate(0, 0, 0.05f);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.plume(pose, buffer, seed ^ 61L,
                    coneRadius * 0.62f, coneLength, 0.0f, 1.0f, 0.60f, 0.70f, alpha, distScale);
                BespokeGeometry.plume(pose, buffer, seed ^ 67L,
                    coneRadius * 0.30f, coneLength * 0.92f, 0.4f, 1.0f, 0.85f, 0.85f, alpha, distScale);
            });
            poseStack.popPose();

            // 2. The mouth lip: a shard fan facing forward at the cone mouth,
            // shards lifting downrange like the old jade lip's spikes.
            poseStack.pushPose();
            poseStack.translate(0, 0, coneLength * 0.96f);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.shardFan(pose, buffer, seed ^ 71L,
                    mouthR * 1.02f, 0.85f, 1.0f, 0.92f, 0.90f, alpha, distScale));
            poseStack.popPose();

            // 3. The forward needles: a second, deeper shard fan mid-cone,
            // reaching past the mouth early and falling back as the wave
            // resolves (the needleReach curve is unchanged).
            poseStack.pushPose();
            poseStack.translate(0, 0, coneLength * 0.45f);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.shardFan(pose, buffer, seed ^ 73L,
                    coneRadius * 0.85f * needleReach, 0.12f, 0.72f, 0.42f, 0.95f, alpha, distScale));
            poseStack.popPose();

            // 4. Epicenter white-emerald muzzle flare: a shrinking glow fan.
            poseStack.pushPose();
            poseStack.translate(0, 0, 0.06f);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.glowFan(pose, buffer, seed ^ 79L,
                    Math.max(0.03f, 0.45f * (1.0f - progress)), 0.0f, 1.0f, 1.0f, 1.0f, alpha, distScale));
            poseStack.popPose();
        } else {
            // Radial Planar / Impact Shockwave (Gale Force / Wind Projectile Hit)
            // Projectile impacts orient the burst to the impact direction (the
            // cone mode rotation path) so hits on flying targets do not get a
            // flat ground ring floating in mid-air. Ground melee bursts keep
            // the flat orientation.
            if (state.orientToImpact) {
                poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yaw)));
                poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.pitch)));
            }

            float expansionEase = (float) Math.sin(progress * (Math.PI / 2.0));
            float currentRadius = Math.max(0.4f, expansionEase * state.maxRadius);
            float crestHeight = 0.55f * (1.0f - progress * 0.8f);
            float coreR = Math.max(0.1f, (state.maxRadius * 0.3f) * (1.0f - progress));

            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                // Epicenter radiating white-emerald core: a glow fan splash.
                BespokeGeometry.glowFan(pose, buffer, seed ^ 83L,
                    coreR, 0.12f, 0.95f, 1.0f, 0.98f, alpha, distScale);

                // The expanding wall: two stacked broken vortex bands replace
                // the beveled ring, an emerald base band and a pale crest band
                // riding at the old crest height.
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 89L,
                    currentRadius * 0.88f, 0.02f, crestHeight + 0.10f, state.age * 0.15f,
                    0.0f, 1.0f, 0.55f, 0.90f * alpha, distScale);
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 97L,
                    currentRadius * 0.95f, Math.max(0.03f, crestHeight * 0.45f), crestHeight + 0.16f, -state.age * 0.1f,
                    0.8f, 1.0f, 0.95f, 0.95f * alpha, distScale);

                // The burst crown: a low plume over the core so the wave has
                // height in the middle, not only at its rim.
                BespokeGeometry.plume(pose, buffer, seed ^ 101L,
                    currentRadius * 0.42f, crestHeight * 2.2f + 0.25f, 0.0f, 1.0f, 0.55f, 0.50f, alpha, distScale);
            });

            // Intersecting emerald crescent blades (6): bespoke blade arcs on
            // the ground plane, centered on the same rotating angles the old
            // ruled arc strips used. They lie flat now; the plume above
            // carries the height the old humps had.
            int bladeCount = 6;
            float span = currentRadius * 0.95f;
            for (int b = 0; b < bladeCount; b++) {
                double bAngle = (b / (double) bladeCount) * Math.PI + (state.age * 0.1);
                final long bladeSeed = seed ^ (103L + b * 13L);
                poseStack.pushPose();
                poseStack.translate(0, 0.05f, 0);
                poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees((float) -Math.toDegrees(bAngle))));
                collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                    BespokeGeometry.bladeArc(pose, buffer, bladeSeed,
                        span * 0.85f, 60f, 0.85f, 1.0f, 0.93f, 0.90f, alpha, distScale));
                poseStack.popPose();
            }
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
