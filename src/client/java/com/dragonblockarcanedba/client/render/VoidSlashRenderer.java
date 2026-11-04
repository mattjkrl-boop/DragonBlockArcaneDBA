package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.VoidSlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Void Slash in Minecraft 26.2.
 * Bespoke pass: the crescent is drawn with the shared BespokeGeometry
 * toolkit instead of the old hand-rolled equal-segment rings. Layered
 * bladeArc sweeps (dark aura, violet body, cyan core, white edge) carry
 * seeded nicks and noise-swollen profiles, and two tornRibbon smears
 * trail behind the cut where the old ring extrusion used to slip back.
 * The seed comes from the entity UUID, so every client draws the same
 * nicks. The synced tilt finally leans the crescent, as it was always
 * extracted to do.
 */
public class VoidSlashRenderer extends EntityRenderer<VoidSlashEntity, VoidSlashRenderer.SlashRenderState> {
    public VoidSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SlashRenderState extends EntityRenderState {
        public boolean tiltRight = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public long seed = 0;
        public double ex, ey, ez;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(VoidSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(VoidSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SlashRenderState createRenderState() {
        return new SlashRenderState();
    }

    @Override
    public void extractRenderState(VoidSlashEntity entity, SlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tiltRight = entity.getTilt();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
    }

    @Override
    public void submit(SlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float progress = Math.min(1.0f, state.age / 40.0f);
        float open = ease(Math.min(1.0f, state.age / 6.0f));
        float close = ease(Math.max(0.0f, (progress - 0.70f) / 0.30f));
        float fade = (1.0f - close) * open;
        if (fade <= 0.01f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float radius = (1.45f + 0.40f * open) * (1.0f - 0.62f * close);
        float edgeLeft = 1.0f - close;
        float ds = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);
        long seed = state.seed;

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        // The crescent leans toward its synced tilt; the old renderer
        // extracted the tilt and never drew with it.
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(state.tiltRight ? -15.0f : 15.0f)));

        // Wake smears first (behind the cut): two torn ribbons crossing
        // behind the arc plane, the follow-through the old extruded ring
        // trail used to give. Length runs along local -Z in both frames.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0xB1L,
                radius * 0.55f, radius * 1.5f, radius * 0.10f,
                0.08f, 0.0f, 0.16f, 0.55f * fade, 1.0f, ds));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0xB2L,
                radius * 0.40f, radius * 1.3f, radius * 0.06f,
                0.60f, 0.05f, 0.95f, 0.35f * fade, 1.0f, ds));
        poseStack.popPose();

        // The crescent itself: bladeArc layers in the cut plane. The
        // builder draws in the XZ plane, so one X rotation lays it into
        // the slash's XY plane facing the travel direction.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0xA1L, radius * 1.30f, 360.0f,
                0.08f, 0.0f, 0.16f, 0.80f * fade, 1.0f, ds);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0xA2L, radius * 1.14f, 360.0f,
                0.60f, 0.05f, 0.95f, 0.95f * fade, 1.0f, ds);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0xA3L, radius * 1.04f, 360.0f,
                0.15f, 0.92f, 1.0f, fade, 1.0f, ds);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0xA4L, radius * 0.96f, 360.0f,
                1.0f, 1.0f, 1.0f, fade, 1.0f, ds);
            if (edgeLeft > 0.03f) {
                BespokeGeometry.bladeArc(pose, buffer, seed ^ 0xA5L, radius * 1.36f, 300.0f,
                    0.20f, 0.95f, 1.0f, fade * edgeLeft, 1.0f, ds);
            }
        });
        poseStack.popPose();

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }
}
