package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.DimensionalWarpRiftEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Dimensional Warp Rift in Minecraft 26.2.
 * Renders a physical 3D spacetime tear and void portal, built entirely from
 * the bespoke toolkit (BESPOKE-STANDARD rule 1), all seeded from the entity's
 * synced seed so every client draws the same torn shape:
 * - Ground anchor glow and the shared layered vortex column
 * - A wandering crack star of fissures and a dark shard fan levered up at the rim
 * - A broken ground band where the old astrolabe ring turned, and a tall
 *   broken orbit band where the crystalline shards circled
 * - The towering vertical tear itself: four torn ribbons deep (violet corona,
 *   magenta slit, black event horizon, white-hot seam), drawn on two crossed
 *   planes, with a torn wake streaming behind it
 * - A displaced-dome singularity pearl at the tear's heart, ringed by two
 *   tilted broken gyroscopic bands
 */
public class DimensionalWarpRiftRenderer extends EntityRenderer<DimensionalWarpRiftEntity, DimensionalWarpRiftRenderer.WarpRiftRenderState> {

    public DimensionalWarpRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class WarpRiftRenderState extends EntityRenderState {
        public float radius = 3.5f;
        public boolean isDestination = false;
        public float age = 0;
        public int maxLifetime = 24;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DimensionalWarpRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DimensionalWarpRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public WarpRiftRenderState createRenderState() {
        return new WarpRiftRenderState();
    }

    @Override
    public void extractRenderState(DimensionalWarpRiftEntity entity, WarpRiftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRiftRadius();
        state.isDestination = entity.isDestination();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getSeed();
    }

    @Override
    public void submit(WarpRiftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float life = (float) state.maxLifetime;
        float progress = Math.min(1.0f, state.age / life);
        if (progress >= 1.0f) return;

        // Shared lifecycle envelope (point 5): build over the first 20 percent,
        // full presence through the burst, dissipate over the last 35 percent.
        // Replaces the old sine hump, which spent half the rift's short life
        // half faded. The tear's own open and shut shaping below still applies.
        float env = FxEnvelope.envelope(state.age, life, 0.20f, 0.35f);
        if (env <= 0.001f) return;
        float fade = env;
        float distScale = FxEnvelope.distScale(Math.sqrt(Math.max(0.0, state.distanceToCameraSq)));

        float entrance = ease(Math.min(1.0f, state.age / 6.0f));
        float expansion = state.isDestination ? (0.6f + progress * 0.7f) : (1.0f - progress * 0.4f);
        float radius = state.radius * entrance * expansion;
        float pulse = 0.88f + 0.12f * (float) Math.sin(state.age * 0.5f);
        RenderType renderType = KiRenderHelper.kiRenderType();

        final float fRadius = radius;
        final float fAge = state.age;
        final float fFade = fade;
        final float fDist = distScale;
        final float fProgress = progress;
        final boolean fDest = state.isDestination;
        final long fSeed = state.seed;

        float shut = ease(Math.max(0.0f, (fProgress - 0.55f) / 0.45f));
        float openTear = 1.0f - 0.88f * shut;
        float tearH = (2.8f + pulse * 0.35f) * entrance * openTear * (fDest ? (0.55f + 0.55f * ease(fProgress)) : (1.0f - 0.45f * ease(fProgress)));
        float tearW = 0.42f * entrance * openTear * (fDest ? (0.35f + 0.75f * ease(fProgress)) : (1.0f - 0.7f * ease(fProgress)));
        float tearY = 1.4f;
        final float fShut = shut;
        final float fOpen = openTear;
        final float fTearH = tearH;
        final float fTearW = tearW;

        poseStack.pushPose();

        // Ground work in the base pose: anchor glow, the layered vortex
        // column, the fissure crack star, the rim shard fan, the broken
        // ground band, and the tall broken orbit band.
        float orbitFrac = fDest
            ? 0.30f + 0.65f * ease(fProgress)
            : 0.85f - 0.50f * ease(fProgress);
        final float fOrbitR = fRadius * orbitFrac;
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            LayeredRender.drawAnchorGlow(pose, buffer, fRadius * 1.2f, 0.05f,
                0.62f, 0.10f, 0.92f, 0.55f * fFade, fDist);
            LayeredRender.drawVortexBands(pose, buffer, fRadius * 0.55f, 3.4f, fAge,
                0.62f, 0.10f, 0.92f, fFade, fDist);

            // The fissure trenches, as one wandering crack star.
            BespokeGeometry.crackStar(pose, buffer, fSeed,
                fRadius * 1.02f, 0.55f, 0.05f, 0.90f, 0.50f, fFade, fDist);
            // Shattered void shards levered up around the rim.
            BespokeGeometry.shardFan(pose, buffer, fSeed ^ 0x51ABL,
                fRadius * 0.95f, 0.10f, 0.02f, 0.18f, 0.90f, fFade, fDist);
            // The ground band, turning slowly where the astrolabe ring was.
            BespokeGeometry.brokenVortexBand(pose, buffer, fSeed ^ 0x81A6L,
                fRadius * 0.66f, 0.03f, 0.20f, (float) Math.toRadians(fAge * 10.0f),
                0.55f, 0.02f, 0.90f, 0.75f * fFade, fDist);
            // The orbiting shards, as one tall broken band whose radius
            // spirals in at the origin and erupts out at the destination,
            // exactly the motion the pyramids traced.
            BespokeGeometry.brokenVortexBand(pose, buffer, fSeed ^ 0x08B1L,
                fOrbitR, 0.5f, 2.3f, fAge * 0.12f,
                1.0f, 0.15f, 0.85f, 0.55f * fFade, fDist);
        });

        // The tear itself: four torn ribbons deep, on two crossed planes so
        // the slit reads from every side. Ribbons run upward from the corona
        // base; the shorter layers sit inside the corona's span.
        float tearBase = tearY - fTearH * 1.3f;
        for (int plane = 0; plane < 2; plane++) {
            final long planeSeed = fSeed ^ (plane == 0 ? 0x7EA1L : 0x7EA2L);
            poseStack.pushPose();
            poseStack.translate(0, tearBase, 0);
            if (plane == 1) {
                poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
            }
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                float envTear = fFade * fOpen;
                BespokeGeometry.tornRibbon(pose, buffer, planeSeed,
                    fTearH * 2.6f, fTearW * 3.2f, 0.12f, 0.60f, 0.05f, 0.90f, 0.70f, envTear, fDist);
                BespokeGeometry.tornRibbon(pose, buffer, planeSeed ^ 0x11L,
                    fTearH * 2.0f, fTearW * 2.0f, 0.12f, 1.0f, 0.15f, 0.85f, 0.90f, envTear, fDist);
                BespokeGeometry.tornRibbon(pose, buffer, planeSeed ^ 0x22L,
                    fTearH * 1.4f, fTearW * 1.0f, 0.12f, 0.02f, 0.0f, 0.05f, 0.98f, envTear, fDist);
                BespokeGeometry.tornRibbon(pose, buffer, planeSeed ^ 0x33L,
                    fTearH * 1.8f, fTearW * 0.3f, 0.12f, 1.0f, 1.0f, 1.0f, 1.0f, envTear, fDist);
            });
            poseStack.popPose();
        }

        // The tear wake: one torn ribbon streaming backward, pinching away
        // as the rift shuts.
        float trail = 1.15f * (1.0f - shut);
        if (trail >= 0.04f && fade >= 0.02f) {
            final float fTrail = trail;
            poseStack.pushPose();
            poseStack.translate(0, tearY, 0);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.tornRibbon(pose, buffer, fSeed ^ 0xA9EL,
                    fTrail, fTearW * 1.7f, 0.18f, 0.75f, 0.08f, 0.95f, 0.75f,
                    fFade * (1.0f - fShut), fDist));
            poseStack.popPose();
        }

        // The singularity pearl at the tear's heart: displaced domes front
        // (magenta shell, white core) and back (void black).
        poseStack.pushPose();
        poseStack.translate(0, tearY, 0);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.displacedDome(pose, buffer, fSeed ^ 0x5166L,
                0.34f * pulse, 1.0f, 0.15f, 0.85f, 0.95f, fFade, fDist));
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, tearY, 0.20f * pulse);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.displacedDome(pose, buffer, fSeed ^ 0xC08EL,
                0.17f * pulse, 1.0f, 1.0f, 1.0f, 1.0f, fFade, fDist));
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, tearY, 0);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.displacedDome(pose, buffer, fSeed ^ 0xB01DL,
                0.34f * pulse, 0.02f, 0.0f, 0.05f, 0.99f, fFade, fDist));
        poseStack.popPose();

        // Dual gyroscopic bands around the tear, tilted against each other
        // and turning at the old rings' speeds.
        float[] gyroTilt = {38.0f, -38.0f};
        float[] gyroSpeed = {24.0f, -19.0f};
        float[][] gyroColor = {{1.0f, 0.15f, 0.85f}, {0.60f, 0.05f, 0.95f}};
        float[] gyroAlpha = {0.65f, 0.55f};
        for (int gi = 0; gi < 2; gi++) {
            final long gyroSeed = fSeed ^ (gi == 0 ? 0x6910L : 0x6911L);
            final float gyroTwist = (float) Math.toRadians(fAge * gyroSpeed[gi]);
            final float gr = gyroColor[gi][0], gg = gyroColor[gi][1], gb = gyroColor[gi][2];
            final float ga = gyroAlpha[gi] * fFade;
            poseStack.pushPose();
            poseStack.translate(0, tearY, 0);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(gyroTilt[gi])));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.brokenVortexBand(pose, buffer, gyroSeed,
                    fRadius * 0.62f, -0.07f, 0.07f, gyroTwist, gr, gg, gb, ga, fDist));
            poseStack.popPose();
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }
}
