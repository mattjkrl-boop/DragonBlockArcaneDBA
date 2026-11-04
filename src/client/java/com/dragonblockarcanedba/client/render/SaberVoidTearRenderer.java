package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.SaberVoidTearEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * Entity Renderer for Saber Void Tear in Minecraft 26.2.
 *
 * <p>Bespoke pass: the hand-rolled diamond slit, equal segment rings,
 * 16 segment cone and curved spike strips are deleted. The slit is now
 * layered {@link BespokeGeometry} torn ribbons stood vertical (dark
 * violet halo, cyan layer, white core woven from both ends), the spikes
 * are jagged branch streaks, the sonic cone is a displaced dome shell,
 * and the dual counter-rotating shock rings are broken vortex bands.
 * Envelope timing, the destination boost, colors and alphas are unchanged
 * from the spectacle pass; only the silhouettes are seeded and torn.</p>
 */
public class SaberVoidTearRenderer extends EntityRenderer<SaberVoidTearEntity, SaberVoidTearRenderer.VoidTearRenderState> {

    public SaberVoidTearRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class VoidTearRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public boolean isDestination = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public int maxLifetime = 12;
        public double ex = 0;
        public double ey = 0;
        public double ez = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SaberVoidTearEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SaberVoidTearEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public VoidTearRenderState createRenderState() {
        return new VoidTearRenderState();
    }

    @Override
    public void extractRenderState(SaberVoidTearEntity entity, VoidTearRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getTearScale();
        state.isDestination = entity.isDestination();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
        // Synced seed: entity id plus the synced scale and destination flag,
        // identical on every client, frame stable for the tear's life.
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L
            ^ Float.floatToIntBits(entity.getTearScale()) * 271L
            ^ (entity.isDestination() ? 0xDE57L : 0L);
    }

    @Override
    public void submit(VoidTearRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        if (progress >= 1.0f) return;

        // Alpha rides the shared envelope as well as the local fade, so the
        // tear builds in and seals shut on the same curve the entity's
        // particles use, and never pops at either end.
        float env = FxEnvelope.envelope(state.age, (float) state.maxLifetime, 0.25f, 0.35f);
        float fade = (1.0f - FxEnvelope.smoothstep(progress)) * Math.min(1.0f, env * 1.6f);
        float open = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.45f));
        float impact = strike(progress);
        float scale = state.scale * (0.55f + 0.7f * open);
        float ds = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);
        // The synced destination flag renders as before: the tear the
        // player arrives through is wider and burns at full core alpha, the
        // origin tear they left is narrower and dimmer.
        float slitBoost = state.isDestination ? 1.30f : 1.0f;
        float coreBoost = state.isDestination ? 1.0f : 0.8f;
        float fadeCore = fade * coreBoost;
        long seed = state.seed;
        float slitH = 1.35f * (0.4f + 0.6f * open) * state.scale;
        float slitW = 0.12f * (1.0f - progress * 0.55f) * state.scale * slitBoost;
        float haloLen = slitH * 3.0f;
        float coreLen = slitH * 2.0f;
        float coneFade = fade * (1.0f - FxEnvelope.smoothstep(Math.max(0.0f, (progress - 0.4f) / 0.6f)));
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        // Rift slit, part 1: the dark violet halo as one tall torn ribbon.
        // The ribbon runs along +X in its own frame; the translate and the
        // two rotations stand it vertical in the tear's XY plane, centered.
        poseStack.pushPose();
        poseStack.translate(0, -haloLen * 0.5f, 0);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x4A10L, haloLen, slitW * 5.0f, slitH * 0.30f,
                0.15f, 0.0f, 0.32f, fadeCore * 0.55f, 1.0f, ds));
        poseStack.popPose();

        // Rift slit, part 2: the cyan layer, brightest at the bottom.
        poseStack.pushPose();
        poseStack.translate(0, -coreLen * 0.5f, 0);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0xC9AAL, coreLen, slitW * 2.4f, slitH * 0.25f,
                0.0f, 0.95f, 1.0f, fadeCore * 0.95f, 1.0f, ds));
        poseStack.popPose();

        // Rift slit, part 3: the white core woven from both ends, so the
        // core burns at the top and the bottom of the slit, not one end.
        poseStack.pushPose();
        poseStack.translate(0, coreLen * 0.5f, 0);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(-90f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x1117L, coreLen, slitW * 1.1f, slitH * 0.20f,
                1.0f, 1.0f, 1.0f, fadeCore, 1.0f, ds));
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, -coreLen * 0.5f, 0);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x1118L, coreLen, slitW * 1.1f, slitH * 0.20f,
                1.0f, 1.0f, 1.0f, fadeCore, 1.0f, ds));
        poseStack.popPose();

        // Six rotating rift spikes as jagged branch streaks, each with the
        // thin white core the old curved strips carried.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            int spikeCount = 6;
            for (int i = 0; i < spikeCount; i++) {
                double angle = (i / (double) spikeCount) * Math.PI * 2.0 + (state.age * 0.12);
                float spikeLen = 1.15f * scale;
                Vec3 tip = new Vec3(Math.cos(angle) * spikeLen, Math.sin(angle) * spikeLen, 0);
                float w = 0.20f * (0.45f + 0.55f * open);
                BespokeGeometry.branch(pose, buffer, seed + i * 101L, Vec3.ZERO, tip,
                    w, 0.0f, 0.85f, 1.0f, fade * 0.85f, 1.0f, ds);
                BespokeGeometry.branch(pose, buffer, seed + i * 101L + 7L, Vec3.ZERO, tip,
                    w * 0.28f, 1.0f, 1.0f, 1.0f, fade * 0.85f, 1.0f, ds);
            }
        });

        // Sonic shell: the old forward cone is a displaced dome now, same
        // white fade-out window after 40 percent of life. The extra X
        // rotation points the dome's +Y axis down the tear's +Z.
        if (coneFade > 0.02f) {
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xD03EL, 1.4f * scale,
                    1.0f, 1.0f, 1.0f, coneFade * 0.9f, 1.0f, ds));
            poseStack.popPose();
        }

        // Dual counter-rotating shock rings as broken vortex bands. The
        // bands circle the Y axis in their own frame; the X rotation lays
        // them into the tear's XY plane, and the twist arguments carry the
        // old opposed spin rates (age * 40 one way, age * -55 the other).
        if (impact > 0.02f) {
            float rOuter = 1.15f * scale * (0.45f + impact);
            float rCounter = 0.95f * scale * (0.50f + impact);
            float twistOuter = (float) Math.toRadians(state.age * 40.0);
            float twistCounter = (float) Math.toRadians(-state.age * 55.0);
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xBADD1L,
                    rOuter, 0.0f, 0.10f * scale, twistOuter,
                    0.0f, 0.85f, 1.0f, impact * 0.9f, ds);
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xBADD2L,
                    rCounter, 0.0f, 0.08f * scale, twistCounter,
                    0.45f, 0.20f, 1.0f, impact * 0.65f, ds);
            });
            poseStack.popPose();
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float strike(float progress) {
        if (progress >= 0.48f) return 0.0f;
        return (float) Math.sin((progress / 0.48f) * Math.PI);
    }
}
