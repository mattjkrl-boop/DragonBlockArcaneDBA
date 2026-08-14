package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.entity.CurseLightningEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public class CurseLightningRenderer extends EntityRenderer<CurseLightningEntity, CurseLightningRenderer.CurseLightningRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/entity/curse_lightning.png");
    private static final Identifier RED_TEXTURE = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/entity/curse_lightning_red.png");

    public CurseLightningRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CurseLightningRenderState extends EntityRenderState {
        public boolean isRare;
    }

    @Override
    public boolean shouldRender(CurseLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public CurseLightningRenderState createRenderState() {
        return new CurseLightningRenderState();
    }

    @Override
    public void extractRenderState(CurseLightningEntity entity, CurseLightningRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isRare = entity.isRare();
    }

    @Override
    public void submit(CurseLightningRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);
        
        Identifier tex = state.isRare ? RED_TEXTURE : TEXTURE;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(tex), (pose, buffer) -> {
            Matrix4f matrix4f = pose.pose();
            float width = 2.0f;
            float height = 30.0f;

            // Draw a giant cross facing all directions so it looks volumetric
            drawQuad(matrix4f, buffer, -width, width, height, 0.0f, 0.0f);
            drawQuad(matrix4f, buffer, 0.0f, 0.0f, height, -width, width);
        });
    }

    private void drawQuad(Matrix4f matrix, VertexConsumer consumer, float minX, float maxX, float maxY, float minZ, float maxZ) {
        consumer.addVertex(matrix, minX, 0, minZ).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 1, 0);
        consumer.addVertex(matrix, maxX, 0, maxZ).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 1, 0);
        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 1, 0);
        consumer.addVertex(matrix, minX, maxY, minZ).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 1, 0);
        
        // Reverse for backface
        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, -1, 0);
        consumer.addVertex(matrix, maxX, 0, maxZ).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, -1, 0);
        consumer.addVertex(matrix, minX, 0, minZ).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, -1, 0);
        consumer.addVertex(matrix, minX, maxY, minZ).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, -1, 0);
    }
}
