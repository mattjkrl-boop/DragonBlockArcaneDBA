package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.HollowAfterimageEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import java.util.Random;

/**
 * Entity Renderer for Hollow Afterimage in Minecraft 26.2.
 * Bespoke pass: the ghost is no longer a stack of colored boxes. The body
 * is a BespokeGeometry plume column (halo plume, skin-tinted body plume,
 * hair plume cap), the eye is a torn ribbon slit, the scanlines are torn
 * ribbon sheets, the speed wake is two torn ribbons trailing behind, the
 * footstep ripple is a jittered glow fan, and the disintegrating motes
 * are small torn ribbon flecks rising on the same seeded layout the old
 * diamond motes used. The public entity contract is untouched: same
 * synced skin and hair colors, same seed source (the entity UUID), same
 * 100 tick fade curve.
 */
public class HollowAfterimageRenderer extends EntityRenderer<HollowAfterimageEntity, HollowAfterimageRenderer.AfterimageRenderState> {

    public HollowAfterimageRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AfterimageRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public int skinColor = 0xFF8CC8FF;
        public int hairColor = 0xFF1EB4FF;
        public float age = 0;
        public long seed = 0;
        public double ex, ey, ez;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(HollowAfterimageEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(HollowAfterimageEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public AfterimageRenderState createRenderState() {
        return new AfterimageRenderState();
    }

    @Override
    public void extractRenderState(HollowAfterimageEntity entity, AfterimageRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getSyncYaw();
        state.xRot = entity.getSyncPitch();
        state.skinColor = entity.getSkinColor();
        state.hairColor = entity.getHairColor();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
    }

    @Override
    public void submit(AfterimageRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // The entity lives 100 ticks; this used to stop at 80, leaving the last
        // second invisible while the afterimage still applied its debuff.
        float maxLife = 100.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float alpha = Math.max(0.05f, 0.90f * (1.0f - (progress * progress)));
        float pulse = 0.85f + 0.15f * (float) Math.sin(state.age * 0.35f);
        float ds = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);
        long seed = state.seed;
        RenderType renderType = KiRenderHelper.kiRenderType();

        // Body colors, unchanged from the box era: the spectral body is
        // tinted by the caster's synced skin color, the cap by hair color.
        float skinR = KiRenderHelper.red(state.skinColor);
        float skinG = KiRenderHelper.green(state.skinColor);
        float skinB = KiRenderHelper.blue(state.skinColor);
        float bodyR = 0.10f + skinR * 0.45f;
        float bodyG = 0.04f + skinG * 0.30f;
        float bodyB = 0.22f + skinB * 0.45f;
        float hairR = KiRenderHelper.red(state.hairColor);
        float hairG = KiRenderHelper.green(state.hairColor);
        float hairB = KiRenderHelper.blue(state.hairColor);

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));

        // Body column at the base pose: outer halo plume, the skin-tinted
        // body plume inside it, and the footstep ripple as a glow fan
        // whose radius still breathes with age.
        float ringR = (0.5f + (state.age * 0.03f) % 0.8f);
        float ringA = alpha * (1.0f - (ringR / 1.3f));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.plume(pose, buffer, seed ^ 0x11L, 0.46f, 1.95f,
                0.10f, 0.85f, 1.0f, alpha * 0.35f * pulse, 1.0f, ds);
            BespokeGeometry.plume(pose, buffer, seed ^ 0x22L, 0.36f, 1.85f,
                bodyR, bodyG, bodyB, alpha * 0.85f, 1.0f, ds);
            if (ringA > 0.01f) {
                BespokeGeometry.glowFan(pose, buffer, seed ^ 0x71L, ringR, 0.03f,
                    0.0f, 0.85f, 1.0f, ringA, ds);
            }
        });

        // Hair cap: a short plume over the head of the column, in the
        // caster's synced hair color (the head spans y 1.4 to 1.82).
        poseStack.pushPose();
        poseStack.translate(0.0f, 1.56f, -0.04f);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.plume(pose, buffer, seed ^ 0x33L, 0.27f, 0.34f,
                hairR, hairG, hairB, alpha * 0.9f, 1.0f, ds));
        poseStack.popPose();

        // The spatial eye slit: a short torn ribbon standing vertical at
        // the face, where the bright box used to sit (y 1.55 to 1.65,
        // front z -0.22 to -0.18).
        poseStack.pushPose();
        poseStack.translate(-0.12f, 1.60f, -0.20f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x44L, 0.24f, 0.10f, 0.0f,
                0.85f, 0.98f, 1.0f, alpha * 0.95f, 1.0f, ds));
        poseStack.popPose();

        // Holographic scanline slices: torn ribbon sheets at the same six
        // heights, swaying with the same age terms as the old slices.
        int sliceCount = 6;
        for (int s = 0; s < sliceCount; s++) {
            float sliceY = 0.15f + (s * 0.32f) + (float) Math.sin((state.age * 0.25f) + s) * 0.06f;
            float sliceOffset = (float) Math.sin((state.age * 0.5f) + s * 1.5f) * 0.06f;
            long sliceSeed = seed ^ (0x50L + s);
            poseStack.pushPose();
            poseStack.translate(-0.35f + sliceOffset, sliceY, 0.0f);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.tornRibbon(pose, buffer, sliceSeed, 0.70f, 0.50f, 0.0f,
                    0.0f, 0.95f, 1.0f, alpha * 0.65f, 1.0f, ds));
            poseStack.popPose();
        }

        // Speed wake: two torn ribbons trailing behind the dash stance.
        // The Y rotation lays each ribbon's length along +Z (behind the
        // ghost, which faces -Z in this frame); the bend lifts the tail
        // to the old ribbons' end heights.
        float ribbonAlpha = alpha * 0.45f;
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.8f, 0.1f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x61L, 0.85f, 0.30f, 0.6f,
                0.1f, 0.85f, 1.0f, ribbonAlpha, 1.0f, ds));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0f, 0.3f, 0.1f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x62L, 0.70f, 0.40f, 0.5f,
                0.1f, 0.85f, 1.0f, ribbonAlpha * 0.8f, 1.0f, ds));
        poseStack.popPose();

        // Disintegrating motes: the old diamond motes' positions, from the
        // same seeded Random sequence, now drawn as tiny torn flecks.
        // Count steps down with camera distance like every other layer.
        Random rng = new Random(state.seed);
        int moteCount = FxEnvelope.segments(16, ds, 5);
        for (int m = 0; m < moteCount; m++) {
            float moteProg = (progress + (m / 16.0f)) % 1.0f;
            float mx = (rng.nextFloat() - 0.5f) * 0.95f;
            float my = moteProg * 2.4f;
            float mz = (rng.nextFloat() - 0.5f) * 0.95f;
            float mMoteAlpha = alpha * (1.0f - moteProg);
            if (mMoteAlpha <= 0.01f) continue;
            long moteSeed = seed ^ (0x80L + m);
            float moteYaw = m * 47.0f;
            poseStack.pushPose();
            poseStack.translate(mx, my, mz);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(moteYaw)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.tornRibbon(pose, buffer, moteSeed, 0.14f, 0.07f, 0.05f,
                    0.1f, 0.85f, 1.0f, mMoteAlpha, 1.0f, ds));
            poseStack.popPose();
        }

        // Additive halo pass: a wider plume shell so the afterimage glows
        // at its silhouette instead of only tinting flat.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) ->
            BespokeGeometry.plume(pose, buffer, seed ^ 0x19L, 0.48f, 1.95f,
                0.10f, 0.80f, 1.0f, alpha * 0.16f * pulse, 1.0f, ds));

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
