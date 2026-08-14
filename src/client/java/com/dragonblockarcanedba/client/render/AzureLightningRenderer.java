package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.AzureLightningEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

public class AzureLightningRenderer extends EntityRenderer<AzureLightningEntity, AzureLightningRenderer.AzureLightningRenderState> {

    public AzureLightningRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AzureLightningRenderState extends EntityRenderState {
        public boolean isRare;
    }

    @Override
    public boolean shouldRender(AzureLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public AzureLightningRenderState createRenderState() {
        return new AzureLightningRenderState();
    }

    @Override
    public void extractRenderState(AzureLightningEntity entity, AzureLightningRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isRare = entity.isRare();
    }

    @Override
    public void submit(AzureLightningRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);
        
        RenderType renderType = KiRenderHelper.kiRenderType();
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix4f = pose.pose();
            float width = 1.8f;
            float height = 36.0f;

            float r = state.isRare ? 0.2f : 0.0f;
            float g = state.isRare ? 0.9f : 0.85f;
            float b = 1.0f;
            float a = 0.95f;

            // Draw volumetric 4-way cross for electric dragon lightning
            drawQuad(matrix4f, buffer, -width, width, height, 0.0f, 0.0f, r, g, b, a);
            drawQuad(matrix4f, buffer, 0.0f, 0.0f, height, -width, width, r, g, b, a);

            // Inner core - Bright white
            drawQuad(matrix4f, buffer, -width * 0.4f, width * 0.4f, height, 0.0f, 0.0f, 0.9f, 1.0f, 1.0f, 1.0f);
            drawQuad(matrix4f, buffer, 0.0f, 0.0f, height, -width * 0.4f, width * 0.4f, 0.9f, 1.0f, 1.0f, 1.0f);
        });
    }

    private void drawQuad(Matrix4f matrix, VertexConsumer consumer, float minX, float maxX, float maxY, float minZ, float maxZ, float r, float g, float b, float a) {
        consumer.addVertex(matrix, minX, 0, minZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, maxX, 0, maxZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        
        // Reverse for backface
        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, maxX, 0, maxZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, minX, 0, minZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
