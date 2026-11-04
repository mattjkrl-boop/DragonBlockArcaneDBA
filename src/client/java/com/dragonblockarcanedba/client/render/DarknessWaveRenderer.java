package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.DarknessWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Darkness Wave in Minecraft 26.2.
 *
 * Bespoke pass: the hand-rolled beveled crescent, fang, and shard helpers
 * are gone. The wave body is layered BespokeGeometry.bladeArc crescents
 * (seeded nicks, torn profile) standing in the vertical plane, with
 * tornRibbon lashes streaming off the back and one jagged tongue at the
 * leading edge where the fangs used to be. The seed comes from the
 * entity UUID, so every client draws the same torn silhouette. The
 * lifecycle math is unchanged: opens over 5 ticks, full to tick 45,
 * gone at tick 60, matching the entity's 60 tick life.
 */
public class DarknessWaveRenderer extends EntityRenderer<DarknessWaveEntity, DarknessWaveRenderer.DarknessWaveRenderState> {

    public DarknessWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DarknessWaveRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public double px = 0, py = 0, pz = 0;
        public boolean isSecondary = false;
        public boolean isFirstPersonOwner = false;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DarknessWaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DarknessWaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DarknessWaveRenderState createRenderState() {
        return new DarknessWaveRenderState();
    }

    @Override
    public void extractRenderState(DarknessWaveEntity entity, DarknessWaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.px = entity.getX();
        state.py = entity.getY();
        state.pz = entity.getZ();
        state.isSecondary = entity.isSecondary();
        state.seed = entity.getUUID().getMostSignificantBits();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(DarknessWaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        float fpScale = (state.isFirstPersonOwner && state.age < 5.0f) ? (0.55f + (state.age / 5.0f) * 0.45f) : 1.0f;
        float span = (state.isSecondary ? 3.0f : 4.4f) * fpScale;
        float chord = (state.isSecondary ? 1.0f : 1.5f) * fpScale;
        float height = (state.isSecondary ? 1.4f : 2.0f) * fpScale;
        float age = state.age;

        // The wave seethes while it flies, opens over the first ticks, and
        // dies across the last 15. Same windows as before the conversion.
        float pulse = 0.90f + 0.10f * (float) Math.sin(age * 0.6f);
        float open = ease(Math.min(1.0f, age / 5.0f));
        float fadeRaw = age <= 45.0f ? 1.0f : Math.max(0.0f, 1.0f - (age - 45.0f) / 15.0f);
        float fade = ease(fadeRaw) * open;
        float drawnHeight = height * (0.55f + 0.45f * open);
        float fangReach = chord * (0.40f + 0.60f * open) * (0.22f + 0.78f * fadeRaw);
        float ds = LayeredRender.distScale(cameraState, state.px, state.py, state.pz);
        long seed = state.seed;

        // Crescent body: layered blade arcs. bladeArc lives in the XZ
        // plane, so the pose tips it upright (XP -90 puts its plane into
        // XY), centers the sector on the origin, and stretches it wide:
        // the old crescent was about four times wider than tall.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
        float arcR = drawnHeight * 0.53f;
        // The sector spans builder x from about 0.07R to 1.25R; scaled by
        // 5.2 that is 0.36R to 6.5R, so shift back by half to center it.
        poseStack.translate(-arcR * 3.3f, 0, 0);
        poseStack.scale(5.2f, 1.0f, 1.0f);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.bladeArc(pose, buffer, seed, arcR * 1.12f, 168f,
                0.08f, 0.0f, 0.16f, 0.94f, fade, ds);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0xA1B2L, arcR * 0.98f, 160f,
                0.58f, 0.05f, 0.90f, 0.96f, fade, ds);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0xC3D4L, arcR * 0.70f, 150f,
                0.95f, 0.08f, 0.35f, pulse, fade, ds);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0xE5F6L, arcR * 0.42f, 140f,
                1.0f, 0.85f, 1.0f, pulse, fade, ds);
        });
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0x1718L, arcR * 1.18f, 164f,
                0.35f, 0.03f, 0.55f, 0.24f, fade, ds);
        });
        poseStack.popPose();

        // Leading tongue: one jagged torn ribbon jutting forward where the
        // fang row used to sit, plus a shorter violet lash beside it.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        poseStack.translate(0, 0, 0.3f);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0xFA66L, fangReach * 2.2f, 0.55f, 0.25f,
                0.95f, 0.08f, 0.32f, 0.90f, fade, ds);
        });
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        poseStack.translate(-span * 0.3f, -0.2f, 0.2f);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0xBEEFL, fangReach * 1.4f, 0.4f, 0.2f,
                0.58f, 0.05f, 0.90f, 0.80f, fade, ds);
        });
        poseStack.popPose();

        // Trailing lashes: torn ribbons streaming backward off the wave.
        for (int i = 0; i < 2; i++) {
            float xOff = (i == 0 ? -1f : 1f) * span * 0.32f;
            long lashSeed = seed ^ (0x7EA1L + i * 131L);
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
            poseStack.translate(xOff, (i == 0 ? 0.15f : -0.1f), 0);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.tornRibbon(pose, buffer, lashSeed, 1.8f, 0.8f, 0.4f,
                    0.45f, 0.04f, 0.80f, 0.70f, fade, ds);
            });
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
