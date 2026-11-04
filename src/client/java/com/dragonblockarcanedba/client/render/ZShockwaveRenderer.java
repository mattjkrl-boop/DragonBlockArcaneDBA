package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.ZShockwaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Z-Sword Massive Kinetic Shockwave in Minecraft 26.2.
 *
 * Bespoke pass: the crescent is a seeded BespokeGeometry blade arc stood
 * upright (the builder's flat arc is rotated into the vertical plane and
 * squashed to the spectacle pass span and height), with a narrower bright
 * core arc inside it, a dimmer trailing arc as the wake, and a shard fan
 * starburst as the apex solar core. Every layer takes the entity's synced
 * seed, so the nick pattern is frame stable and identical for every viewer.
 */
public class ZShockwaveRenderer extends EntityRenderer<ZShockwaveEntity, ZShockwaveRenderer.ZShockwaveRenderState> {
    public ZShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ZShockwaveRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public boolean isSubWave = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public long seed = 0;
        public boolean isFirstPersonOwner = false;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ZShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ZShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ZShockwaveRenderState createRenderState() {
        return new ZShockwaveRenderState();
    }

    @Override
    public void extractRenderState(ZShockwaveEntity entity, ZShockwaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.isSubWave = entity.isSubWave();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getSeed();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(ZShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        float charge = state.chargeRatio;
        boolean sub = state.isSubWave;
        float age = state.age;

        // Lifetime fade: the wave lives up to 80 ticks and used to vanish at full
        // opacity on discard. Fade in over the first 6 ticks, hold, then ease out
        // over the final 25 ticks.
        float fadeIn = smooth(Math.min(1.0f, age / 6.0f));
        float fadeOut = age <= 55.0f ? 1.0f : 1.0f - smooth((age - 55.0f) / 25.0f);
        float fade = fadeIn * Math.max(0.0f, fadeOut);
        float impact = strike(Math.min(1.0f, age / 18.0f));

        float fpScale = (state.isFirstPersonOwner && state.age < 6.0f) ? (0.55f + (state.age / 6.0f) * 0.45f) : 1.0f;
        // Mild growth across the fade in window: the crescent used to appear at
        // full 22 block span on its first tick. It now opens from 80 percent
        // while it leaves the caster; the damage band uses the full span, and
        // the core covers the small early gap at point blank range.
        float grow = 0.8f + 0.2f * fadeIn;
        float span = (sub ? (3.2f + charge * 3.2f) : (4.8f + charge * 6.2f)) * fpScale * grow;
        float chord = (sub ? (1.0f + charge * 0.6f) : (1.6f + charge * 1.4f)) * fpScale;
        float height = (sub ? (0.9f + charge * 0.8f) : (1.5f + charge * 1.8f)) * fpScale;
        float dist = LayeredRender.distScale(cameraState, state.x, state.y, state.z);

        // Blade arc fitting: with a 150 degree sweep the builder's band spans
        // about 0.61 of its radius sideways and about 0.98 of its radius
        // vertically, centred about 0.62 of its radius up. Solve the radius
        // from the span, squash Y so the band is exactly 2 * height tall, and
        // recenter it on the entity origin. The band sits at 60 percent of the
        // chord so it stays inside the damage band tolerance front and back.
        float arcRadius = span * 1.64f;
        float arcScaleY = (2.0f * height) / (0.98f * arcRadius);
        float arcCenterY = 0.62f * arcRadius * arcScaleY;
        float bandZ = chord * 0.6f;

        // Outer crescent: the golden blade itself.
        poseStack.pushPose();
        poseStack.translate(0.0f, -arcCenterY, bandZ);
        poseStack.scale(1.0f, arcScaleY, 1.0f);
        standArcUpright(poseStack);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.bladeArc(pose, buffer, state.seed, arcRadius, 150.0f,
                1.0f, 0.88f, 0.20f, 0.95f, fade, dist));
        poseStack.popPose();

        // Inner core: a narrow near white arc, the incandescent heart.
        poseStack.pushPose();
        poseStack.translate(0.0f, -arcCenterY * 0.78f, bandZ + 0.05f);
        poseStack.scale(0.30f, arcScaleY * 0.78f, 1.0f);
        standArcUpright(poseStack);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.bladeArc(pose, buffer, state.seed ^ 0xC0DEL, arcRadius, 150.0f,
                1.0f, 1.0f, 0.92f, 1.0f, fade, dist));
        poseStack.popPose();

        // Wake: the same blade, flatter and dimmer, trailing behind.
        poseStack.pushPose();
        poseStack.translate(0.0f, -arcCenterY * 0.48f, bandZ - 0.85f);
        poseStack.scale(0.92f, arcScaleY * 0.48f, 1.0f);
        standArcUpright(poseStack);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.bladeArc(pose, buffer, state.seed ^ 0xACE1L, arcRadius, 150.0f,
                1.0f, 0.80f, 0.15f, 0.40f, fade, dist));
        poseStack.popPose();

        // Apex solar core: a shard fan starburst facing forward at the chord.
        if (impact > 0.02f) {
            float coreRadius = (0.28f + charge * 0.16f) * Math.max(0.05f, impact) * 1.6f;
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.0f, chord);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.shardFan(pose, buffer, state.seed ^ 0xA9E5L, coreRadius,
                    1.0f, 1.0f, 0.95f, 0.95f, impact, dist));
            poseStack.popPose();
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /**
     * Rotates the flat blade arc (XZ plane, opening around +X) into the
     * vertical XY plane: +X becomes up, +Z becomes the span direction.
     * Applied after the caller's translate and scale, so the squash and
     * recentering happen in world axes, not arc axes.
     */
    private static void standArcUpright(PoseStack poseStack) {
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
    }

    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    private static float strike(float progress) {
        if (progress >= 0.7f) return 0.0f;
        return (float) Math.sin((progress / 0.7f) * Math.PI);
    }
}
