package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.OxShockwaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Ox King's Groundbreaker Shockwave in Minecraft 26.2.
 * Renders an expanding 360-degree ground shockwave ring with heavy fiery/earthquake geometries.
 */
public class OxShockwaveRenderer extends EntityRenderer<OxShockwaveEntity, OxShockwaveRenderer.OxShockwaveRenderState> {
    public OxShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public OxShockwaveRenderState createRenderState() {
        return new OxShockwaveRenderState();
    }

    @Override
    public void extractRenderState(OxShockwaveEntity entity, OxShockwaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.isSubWave = entity.isSubWave();
        state.currentRadius = entity.getCurrentRadius();
    }

    @Override
    public void submit(OxShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float radius = state.currentRadius;
        int segments = 24;
        float height = 0.25f + (state.chargeRatio * 0.25f);
        float ringThickness = 0.4f + (state.chargeRatio * 0.3f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            for (int i = 0; i < segments; i++) {
                double a1 = (i / (double) segments) * Math.PI * 2.0;
                double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

                float x1 = (float) (Math.cos(a1) * radius);
                float z1 = (float) (Math.sin(a1) * radius);
                float x2 = (float) (Math.cos(a2) * radius);
                float z2 = (float) (Math.sin(a2) * radius);

                float midX = (x1 + x2) * 0.5f;
                float midZ = (z1 + z2) * 0.5f;

                // 1. Fiery Outer Glow
                KiRenderHelper.drawColoredBox(pose, buffer,
                    midX - ringThickness, 0.0f, midZ - ringThickness,
                    midX + ringThickness, height, midZ + ringThickness,
                    1.0f, state.isSubWave ? 0.6f : 0.25f, 0.0f, 0.85f
                );

                // 2. White-Gold Core Impact
                KiRenderHelper.drawColoredBox(pose, buffer,
                    midX - (ringThickness * 0.5f), 0.05f, midZ - (ringThickness * 0.5f),
                    midX + (ringThickness * 0.5f), height * 0.8f, midZ + (ringThickness * 0.5f),
                    1.0f, 0.95f, 0.7f, 0.95f
                );
            }
        });

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class OxShockwaveRenderState extends EntityRenderState {
        public float chargeRatio = 1.0f;
        public boolean isSubWave = false;
        public float currentRadius = 0.5f;
    }
}
