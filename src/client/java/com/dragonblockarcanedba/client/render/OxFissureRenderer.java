package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.OxFissureEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Ox King's Ground Fissure in Minecraft 26.2.
 */
public class OxFissureRenderer extends EntityRenderer<OxFissureEntity, OxFissureRenderer.OxFissureRenderState> {
    public OxFissureRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public OxFissureRenderState createRenderState() {
        return new OxFissureRenderState();
    }

    @Override
    public void extractRenderState(OxFissureEntity entity, OxFissureRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(OxFissureRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float pulse = 0.7f + 0.3f * (float) Math.sin(state.ageInTicks * 0.2f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // Ground crack center
            KiRenderHelper.drawColoredBox(pose, buffer,
                -1.2f, 0.02f, -0.3f,
                1.2f, 0.08f, 0.3f,
                1.0f, 0.3f * pulse, 0.0f, 0.9f
            );
            // Cross crack
            KiRenderHelper.drawColoredBox(pose, buffer,
                -0.3f, 0.02f, -1.0f,
                0.3f, 0.08f, 1.0f,
                0.9f, 0.2f * pulse, 0.0f, 0.85f
            );
        });

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class OxFissureRenderState extends EntityRenderState {
        public float ageInTicks = 0;
    }
}
