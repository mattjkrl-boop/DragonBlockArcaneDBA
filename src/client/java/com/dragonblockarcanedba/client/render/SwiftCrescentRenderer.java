package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.SwiftCrescentEntity;
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
 * Entity Renderer for Swift Crescent in Minecraft 26.2.
 * Renders the Katana Flashdraw crescent in layers: a silver blade arc body,
 * a bright white core arc nested inside it, a colored cutting lip arc on the
 * outer edge, a torn ribbon wake behind the belly, an outer wispy halo copy,
 * and a shard fan spark at the belly that peaks at the strike tick. All
 * geometry comes from the shared BespokeGeometry builders seeded from the
 * entity UUID, so the same crescent draws the same torn silhouette on every
 * client (BESPOKE-STANDARD rule 1). Variant 0 cuts with a cyan lip; variant 1
 * mirrors the arc and cuts with a pale silver-blue lip. Alpha runs on the
 * shared FxEnvelope so the cut builds, peaks, and dissipates.
 */
public class SwiftCrescentRenderer extends EntityRenderer<SwiftCrescentEntity, SwiftCrescentRenderer.SwiftCrescentRenderState> {

    public SwiftCrescentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SwiftCrescentRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float tiltAngle = 0.0f;
        public int variant = 0;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public int maxLifetime = 10;
        public int strikeTick = -1;
        public long seed = 0;
        public double wx = 0, wy = 0, wz = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SwiftCrescentEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SwiftCrescentEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SwiftCrescentRenderState createRenderState() {
        return new SwiftCrescentRenderState();
    }

    @Override
    public void extractRenderState(SwiftCrescentEntity entity, SwiftCrescentRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getSlashScale();
        state.tiltAngle = entity.getTiltAngle();
        state.variant = entity.getSlashVariant();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.strikeTick = entity.getStrikeDamage() > 0f ? entity.getStrikeTick() : -1;
        state.seed = entity.getUUID().getMostSignificantBits();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(SwiftCrescentRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // Shared lifecycle: build over the first 25 percent, dissipate over
        // the last 35. Nothing renders outside the envelope.
        float fade = FxEnvelope.envelope(state.age, state.maxLifetime, 0.25f, 0.35f);
        if (fade <= 0.0f) return;
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);

        float open = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.25f));
        float baseScale = state.scale * (0.76f + 0.30f * open);
        float trail = 0.10f + 0.58f * FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.65f));
        // The belly spark peaks exactly when the carried strike lands.
        float strikeFrac = state.strikeTick >= 0
            ? state.strikeTick / (float) state.maxLifetime
            : 0.20f;
        float impact = strike(progress, strikeFrac);
        float dist = LayeredRender.distScale(cameraState, state.wx, state.wy, state.wz);
        RenderType renderType = KiRenderHelper.kiRenderType();

        // Variant 1 mirrors the arc and swaps the cyan lip for silver-blue.
        float mirror = state.variant == 1 ? -1.0f : 1.0f;
        float lipR = state.variant == 1 ? 0.55f : 0.0f;
        float lipG = state.variant == 1 ? 0.80f : 0.86f;
        float lipB = 1.0f;
        long seed = state.seed;

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(state.tiltAngle)));
        poseStack.scale(baseScale, baseScale, baseScale);

        // The crescent body: three nested blade arcs (silver body, white
        // core, colored lip) sharing one pose. The builder arc bulges along
        // its local +X; a -90 degree yaw turns the bulge to face +Z, the way
        // the old hand drawn strip bowed. Mirroring flips local Z.
        poseStack.pushPose();
        poseStack.scale(1.0f, 1.0f, mirror);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.bladeArc(pose, buffer, seed, 2.3f, 100.0f,
                0.86f, 0.95f, 1.0f, 0.92f, fade, dist);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0x1111L, 1.93f, 100.0f,
                1.0f, 1.0f, 1.0f, 0.55f, fade, dist);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0x2222L, 2.42f, 100.0f,
                lipR, lipG, lipB, 0.45f, fade, dist);
        });
        poseStack.popPose();

        // The follow-through wake: a torn ribbon trailing behind the belly,
        // always toward local -Z like the old strip wake.
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, 0.30f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x3333L, trail, 2.0f, 0.0f,
                0.86f, 0.95f, 1.0f, 0.45f, fade, dist);
        });
        poseStack.popPose();

        // The belly spark: a shard fan in the crescent plane at the point of
        // the cut, peaking at the strike tick. Its alpha ignores the fade,
        // matching the old spark: the strike window itself gates it.
        if (impact > 0.02f) {
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.02f, 0.38f * mirror);
            collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
                BespokeGeometry.shardFan(pose, buffer, seed ^ 0x4444L, 0.45f * impact,
                    1.0f, 1.0f, 1.0f, impact, 1.0f, dist);
            });
            poseStack.popPose();
        }

        // Outer layer: a larger, faint copy of the same arc so the cut has a
        // wispy edge that dissolves into the air (standard point 1).
        poseStack.pushPose();
        poseStack.scale(1.3f, 1.3f, 1.3f);
        poseStack.scale(1.0f, 1.0f, mirror);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.bladeArc(pose, buffer, seed, 2.3f, 100.0f,
                0.86f, 0.95f, 1.0f, 0.92f, fade * 0.18f, dist);
        });
        poseStack.popPose();

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /** Sine flash peaking at the strike fraction of life, gone by twice it. */
    private static float strike(float progress, float strikeFrac) {
        float window = Math.min(1.0f, strikeFrac * 2.0f);
        if (progress >= window) return 0.0f;
        return (float) Math.sin((progress / window) * Math.PI);
    }
}
