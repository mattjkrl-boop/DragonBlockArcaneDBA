package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.ValorFieldEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Grand Sword's Valor Field in Minecraft 26.2.
 * Renders a radiant translucent golden energy dome.
 */
public class ValorFieldRenderer extends EntityRenderer<ValorFieldEntity, ValorFieldRenderer.ValorFieldRenderState> {
    public ValorFieldRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ValorFieldRenderState createRenderState() {
        return new ValorFieldRenderState();
    }

    @Override
    public void extractRenderState(ValorFieldEntity entity, ValorFieldRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(ValorFieldRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float radius = ValorFieldEntity.FIELD_RADIUS;
        float pulse = 0.85f + 0.15f * (float) Math.sin(state.ageInTicks * 0.15f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // Draw 3 perpendicular golden orbital rings forming the protective sphere
            int ringSegments = 32;

            // 1. Horizontal Equator Ring
            for (int i = 0; i < ringSegments; i++) {
                double a = (i / (double) ringSegments) * Math.PI * 2.0;
                float x = (float) (Math.cos(a) * radius);
                float z = (float) (Math.sin(a) * radius);
                KiRenderHelper.drawColoredBox(pose, buffer,
                    x - 0.15f, 0.9f, z - 0.15f,
                    x + 0.15f, 1.1f, z + 0.15f,
                    1.0f, 0.85f, 0.2f, 0.75f * pulse
                );
            }

            // 2. Vertical X-Y Ring
            for (int i = 0; i < ringSegments; i++) {
                double a = (i / (double) ringSegments) * Math.PI * 2.0;
                float x = (float) (Math.cos(a) * radius);
                float y = (float) (Math.sin(a) * radius) + 1.0f;
                KiRenderHelper.drawColoredBox(pose, buffer,
                    x - 0.15f, y - 0.1f, -0.15f,
                    x + 0.15f, y + 0.1f, 0.15f,
                    1.0f, 0.75f, 0.1f, 0.65f * pulse
                );
            }

            // 3. Vertical Y-Z Ring
            for (int i = 0; i < ringSegments; i++) {
                double a = (i / (double) ringSegments) * Math.PI * 2.0;
                float z = (float) (Math.cos(a) * radius);
                float y = (float) (Math.sin(a) * radius) + 1.0f;
                KiRenderHelper.drawColoredBox(pose, buffer,
                    -0.15f, y - 0.1f, z - 0.15f,
                    0.15f, y + 0.1f, z + 0.15f,
                    1.0f, 0.95f, 0.3f, 0.65f * pulse
                );
            }
        });

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class ValorFieldRenderState extends EntityRenderState {
        public float ageInTicks = 0;
    }
}
