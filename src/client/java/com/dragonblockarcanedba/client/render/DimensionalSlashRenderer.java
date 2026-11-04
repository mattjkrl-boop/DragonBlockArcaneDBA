package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.DimensionalSlashEntity;
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
 * Entity Renderer for Dimensional Slash in Minecraft 26.2.
 * Renders a physical 3D reality severance crescent, built entirely from the
 * bespoke toolkit (BESPOKE-STANDARD rule 1), all seeded from the entity's
 * synced seed so every client draws the same torn shape:
 * - A nicked, noise-profiled blade arc in three stops (abyssal violet,
 *   neon blood-magenta, pure white reality tear) plus an additive halo arc
 * - Shard bursts at the pinched tips where the cut frays
 * - A displaced-dome singularity pearl at the belly, front magenta and
 *   white, back void black, ringed by broken accretion bands
 * - Torn ribbons streaming backward along the trajectory
 */
public class DimensionalSlashRenderer extends EntityRenderer<DimensionalSlashEntity, DimensionalSlashRenderer.SlashRenderState> {

    public DimensionalSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SlashRenderState extends EntityRenderState {
        public boolean tiltRight = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public float fadeStart = -1;
        public int seed = 0;
        public boolean isFirstPersonOwner = false;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DimensionalSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DimensionalSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SlashRenderState createRenderState() {
        return new SlashRenderState();
    }

    @Override
    public void extractRenderState(DimensionalSlashEntity entity, SlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tiltRight = entity.getTilt();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.fadeStart = entity.getFadeStart();
        state.seed = entity.getSeed();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(SlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        if (state.tiltRight) {
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(35.0f)));
        } else {
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(-35.0f)));
        }

        float fpScale = (state.isFirstPersonOwner && state.age < 5.0f) ? (0.60f + (state.age / 5.0f) * 0.40f) : 1.0f;

        // Lifecycle, reconciled with the entity (point 5): one shared envelope
        // over the real flight life, so the old fade out window (ages 70 to
        // 100) is exactly the envelope's last 30 percent. On a hit the entity
        // now lingers instead of discarding, and the tear fades shut across
        // the linger while swelling slightly, like cut space straining open
        // before it closes.
        float distScale = FxEnvelope.distScale(Math.sqrt(Math.max(0.0, state.distanceToCameraSq)));
        float fade = FxEnvelope.envelope(state.age, DimensionalSlashEntity.MAX_LIFE_TICKS, 0.05f, 0.30f);
        float lingerT = -1.0f;
        if (state.fadeStart >= 0) {
            lingerT = Math.max(0.0f, Math.min(1.0f,
                (state.age - state.fadeStart) / (float) DimensionalSlashEntity.LINGER_TICKS));
            fade *= 1.0f - FxEnvelope.smoothstep(lingerT);
        }
        float tearScale = lingerT >= 0.0f ? 1.0f + 0.22f * FxEnvelope.smoothstep(lingerT) : 1.0f;
        poseStack.scale(fpScale * tearScale, fpScale * tearScale, fpScale * tearScale);
        final float fFade = fade;
        final float fDist = distScale;
        final long fSeed = state.seed;
        final float age = state.age;

        // Forward spin along movement vector
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees((float) Math.sin(state.age * 0.4f) * 6.0f)));

        float span = 3.4f;
        float pulse = 0.90f + 0.10f * (float) Math.sin(state.age * 0.6f);
        float body = 0.45f + 0.55f * fade;

        // The crescent body: blade arcs from the bespoke toolkit. The pose
        // block turns the builder's flat XZ arc into the slash's XY blade
        // (belly up, tips spread across X) and flattens it to the old wide,
        // shallow crescent proportions. Three stops, outer to core, then the
        // additive halo under the same pose.
        poseStack.pushPose();
        poseStack.translate(0, -1.02f, 0.12f);
        poseStack.scale(1.22f, 0.50f, 1.0f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.bladeArc(pose, buffer, fSeed,
                span * 1.30f, 132.0f, 0.65f, 0.05f, 0.95f, 0.82f, fFade, fDist);
            BespokeGeometry.bladeArc(pose, buffer, fSeed ^ 0xA53AL,
                span * 1.22f, 126.0f, 1.0f, 0.10f, 0.35f, 0.96f, fFade, fDist);
            BespokeGeometry.bladeArc(pose, buffer, fSeed ^ 0x7E44L,
                span * 0.86f, 118.0f, 1.0f, 1.0f, 1.0f, 1.0f, fFade, fDist);
        });
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) ->
            BespokeGeometry.bladeArc(pose, buffer, fSeed ^ 0x4A10L,
                span * 1.38f, 136.0f, 0.55f, 0.05f, 0.30f, 0.28f, fFade, fDist));
        poseStack.popPose();

        // The singularity at the belly: displaced domes back to back (magenta
        // and a white core toward the front, void black toward the back),
        // ringed by two broken accretion bands turning against each other.
        poseStack.pushPose();
        poseStack.translate(0, 0.42f, 0.30f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, fSeed ^ 0x51E6L,
                0.40f * pulse, 1.0f, 0.15f, 0.85f, 0.95f, fFade, fDist);
            BespokeGeometry.brokenVortexBand(pose, buffer, fSeed ^ 0xBEEFL,
                0.72f * pulse, -0.05f, 0.05f, (float) Math.toRadians(age * 22.0f),
                0.60f, 0.05f, 0.95f, 0.80f * fFade, fDist);
            BespokeGeometry.brokenVortexBand(pose, buffer, fSeed ^ 0xF00DL,
                0.46f * pulse, -0.04f, 0.04f, (float) Math.toRadians(-age * 30.0f),
                1.0f, 0.15f, 0.35f, 0.92f * fFade, fDist);
        });
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, 0.42f, 0.44f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.displacedDome(pose, buffer, fSeed ^ 0xC04EL,
                0.17f * pulse, 1.0f, 1.0f, 1.0f, 1.0f, fFade, fDist));
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, 0.42f, 0.30f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.displacedDome(pose, buffer, fSeed ^ 0xB01DL,
                0.40f * pulse, 0.02f, 0.0f, 0.05f, 0.99f, fFade, fDist));
        poseStack.popPose();

        // Tip spurs: shard bursts where the cut frays at the pinched ends,
        // stood up into the blade plane.
        for (int s = -1; s <= 1; s += 2) {
            poseStack.pushPose();
            poseStack.translate(s * span * 0.74f, -0.45f, 0.12f);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
            final long spurSeed = fSeed ^ (s < 0 ? 0x5911L : 0x5912L);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.shardFan(pose, buffer, spurSeed,
                    0.55f, 1.0f, 0.15f, 0.45f, 0.95f, fFade, fDist));
            poseStack.popPose();
        }

        // Trailing fissure ribbons: torn ribbons streaming backward off the
        // crescent, the center one longest where the belly bows furthest.
        float[] ribbonX = {-1.7f, 0.0f, 1.7f};
        float[] ribbonY = {0.05f, 0.50f, 0.05f};
        float[] ribbonArc = {0.55f, 1.0f, 0.55f};
        for (int i = 0; i < 3; i++) {
            final float len = (0.9f + 1.3f * fFade) * ribbonArc[i];
            final float wid = 0.05f + 0.13f * body;
            final long ribSeed = fSeed ^ (0x81B0L + i * 131L);
            final float rr = i == 1 ? 1.0f : 0.75f;
            final float rg = 0.15f;
            final float rb = i == 1 ? 0.45f : 0.90f;
            poseStack.pushPose();
            poseStack.translate(ribbonX[i], ribbonY[i], 0.35f);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.tornRibbon(pose, buffer, ribSeed,
                    len, wid, 0.22f, rr, rg, rb, 0.75f, fFade, fDist));
            poseStack.popPose();
        }

        // Lingering tear (point 1 and 5): while the struck crescent fades shut,
        // the cut itself swirls in the shared three layer bands, centered on
        // the crescent, so the hole in space reads as a closing vortex and not
        // a sprite dying.
        if (lingerT >= 0.0f && fade > 0.01f) {
            final float bandFade = fade;
            final float bandAge = age;
            final float bandDist = distScale;
            poseStack.pushPose();
            poseStack.translate(0, -0.95f, 0);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                LayeredRender.drawVortexBands(pose, buffer, 1.05f, 1.9f, bandAge,
                    0.62f, 0.10f, 0.92f, bandFade, bandDist));
            poseStack.popPose();

            // The tear mark the cut leaves behind: a bespoke crack star in
            // the blade plane, fading with the linger.
            poseStack.pushPose();
            poseStack.translate(0, 0.10f, 0.10f);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.crackStar(pose, buffer, fSeed ^ 0xC4ACL,
                    1.5f, 0.85f, 0.15f, 0.95f, 0.80f, bandFade, bandDist));
            poseStack.popPose();
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
