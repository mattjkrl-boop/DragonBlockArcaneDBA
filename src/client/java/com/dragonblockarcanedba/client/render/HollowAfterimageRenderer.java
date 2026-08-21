package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.HollowAfterimageEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Hollow Afterimage in Minecraft 26.2.
 * Renders a spectral, translucent phantom silhouette with glowing cyan/purple edge displacement ripples.
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
    }

    @Override
    public void submit(AfterimageRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));

        float alpha = Math.max(0.08f, 0.75f * (1.0f - (state.age / 80.0f)));
        float pulse = 0.85f + 0.15f * (float) Math.sin(state.age * 0.4f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // 1. Outer Ethereal Phantom Halo (Cyan-Violet Glow)
            renderSilhouette(pose, buffer, 0.04f, 0.2f, 0.85f, 1.0f, alpha * 0.45f * pulse);

            // 2. Main Void Silhouette (Dark Indigo / Violet)
            renderSilhouette(pose, buffer, 0.0f, 0.12f, 0.02f, 0.25f, alpha * 0.85f);

            // 3. Central Spatial Tear Core (White-Cyan Slit)
            KiRenderHelper.drawColoredBox(pose, buffer,
                -0.03f, 0.9f, -0.03f,
                0.03f, 1.3f, 0.03f,
                1.0f, 1.0f, 1.0f, alpha * 0.95f
            );
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private void renderSilhouette(PoseStack.Pose pose,
                                  VertexConsumer buffer,
                                  float expand, float r, float g, float b, float a) {
        // Torso
        KiRenderHelper.drawColoredBox(pose, buffer,
            -0.25f - expand, 0.7f - expand, -0.15f - expand,
            0.25f + expand, 1.4f + expand, 0.15f + expand,
            r, g, b, a);

        // Head
        KiRenderHelper.drawColoredBox(pose, buffer,
            -0.2f - expand, 1.4f, -0.2f - expand,
            0.2f + expand, 1.8f + expand * 1.5f, 0.2f + expand,
            r * 1.2f, g * 1.2f, b * 1.2f, a);

        // Left Arm
        KiRenderHelper.drawColoredBox(pose, buffer,
            -0.45f - expand, 0.7f - expand, -0.12f - expand,
            -0.25f, 1.4f + expand, 0.12f + expand,
            r, g, b, a * 0.85f);

        // Right Arm
        KiRenderHelper.drawColoredBox(pose, buffer,
            0.25f, 0.7f - expand, -0.12f - expand,
            0.45f + expand, 1.4f + expand, 0.12f + expand,
            r, g, b, a * 0.85f);

        // Left Leg
        KiRenderHelper.drawColoredBox(pose, buffer,
            -0.22f - expand, 0.0f, -0.12f - expand,
            -0.02f, 0.7f + expand, 0.12f + expand,
            r * 0.8f, g * 0.8f, b * 0.8f, a * 0.9f);

        // Right Leg
        KiRenderHelper.drawColoredBox(pose, buffer,
            0.02f, 0.0f, -0.12f - expand,
            0.22f + expand, 0.7f + expand, 0.12f + expand,
            r * 0.8f, g * 0.8f, b * 0.8f, a * 0.9f);
    }
}
