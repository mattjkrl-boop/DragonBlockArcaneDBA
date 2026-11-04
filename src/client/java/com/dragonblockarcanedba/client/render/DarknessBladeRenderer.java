package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.DarknessBladeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Darkness Blade falling execution in MC 26.2.
 *
 * Bespoke pass: the hand-built greatsword (beveled blade boxes, horned
 * crossguard, hilt, smooth plunge cones) is gone. The falling sword is
 * now layered BespokeGeometry.bladeArc crescents standing vertical, the
 * wide base end of the arc reading as the guard mass, in obsidian,
 * fuller red, and a white hot core, with tornRibbon wake trails
 * streaming off the plunge. The seed comes from the entity UUID so the
 * torn silhouette is the same on every client. The spin (roll from yRot
 * plus age) and the 3 tick fade-in are unchanged from the spectacle pass.
 */
public class DarknessBladeRenderer extends EntityRenderer<DarknessBladeEntity, DarknessBladeRenderer.DarknessBladeRenderState> {

    public DarknessBladeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DarknessBladeRenderState extends EntityRenderState {
        public float yRot = 0;
        public float age = 0;
        public double px = 0, py = 0, pz = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DarknessBladeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DarknessBladeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DarknessBladeRenderState createRenderState() {
        return new DarknessBladeRenderState();
    }

    @Override
    public void extractRenderState(DarknessBladeEntity entity, DarknessBladeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.age = entity.tickCount + partialTicks;
        state.px = entity.getX();
        state.py = entity.getY();
        state.pz = entity.getZ();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(DarknessBladeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float ds = LayeredRender.distScale(cameraState, state.px, state.py, state.pz);
        long seed = state.seed;

        poseStack.pushPose();
        // Blades plunge point-first and roll as they fall (same spin math).
        float roll = state.yRot + state.age * 18.0f;
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(roll)));

        float fade = Math.min(1.0f, state.age / 3.0f);
        float pulse = 0.82f + 0.18f * (float) Math.sin(state.age * 0.8f);

        // The sword body: a wide-sweep bladeArc (236 degrees) tipped
        // upright with XP -90 inside this rotated frame. Radius 1.9 gives
        // a roughly 4.2 block silhouette, close to the old sword's 4.4
        // block total length, and the arc's thick base end sits up top
        // where the hilt and guard mass used to be.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
        poseStack.translate(1.35f, 0, 0);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.bladeArc(pose, buffer, seed, 1.9f, 236f,
                0.05f, 0.01f, 0.12f, 0.97f, fade, ds);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0x5A11L, 1.55f, 226f,
                0.06f, 0.0f, 0.14f, 0.99f, fade, ds);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0xF011L, 0.95f, 205f,
                0.72f, 0.03f, 0.22f, pulse, fade, ds);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0xC0DEL, 0.45f, 180f,
                1.0f, 0.90f, 1.0f, 0.95f, fade, ds);
        });
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0xAD01L, 2.1f, 240f,
                0.40f, 0.04f, 0.75f, 0.16f, fade, ds);
        });
        poseStack.popPose();

        // Plunge wake: torn ribbons streaming upward off the falling
        // blade (the wake trails toward +Y in local space), replacing the
        // two smooth plunge cones.
        for (int i = 0; i < 2; i++) {
            final float xOff = (i == 0 ? -0.45f : 0.45f);
            final long wakeSeed = seed ^ (0x9AE1L + i * 97L);
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
            poseStack.translate(xOff, 0.6f, 0);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.tornRibbon(pose, buffer, wakeSeed, 2.4f, 0.55f, 0.3f,
                    0.30f, 0.02f, 0.55f, 0.55f, fade, ds);
            });
            poseStack.popPose();
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
