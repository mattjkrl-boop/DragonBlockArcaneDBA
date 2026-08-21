package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.SkyCracksEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

import java.util.Random;

public class SkyCracksRenderer extends EntityRenderer<SkyCracksEntity, SkyCracksRenderer.SkyCracksRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/environment/sky_cracks.png");

    public SkyCracksRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SkyCracksRenderState extends EntityRenderState {
        public float age = 0;
        public long seed = 0;
    }

    @Override
    public SkyCracksRenderState createRenderState() {
        return new SkyCracksRenderState();
    }

    @Override
    public boolean shouldRender(SkyCracksEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public void extractRenderState(SkyCracksEntity entity, SkyCracksRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(SkyCracksRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float pulse = 0.8f + 0.2f * (float) Math.sin(state.age * 0.15f);

        // 1. Base Emissive Celestial Texture Quad
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(TEXTURE), (pose, buffer) -> {
            Matrix4f matrix4f = pose.pose();
            float size = 450.0f;

            buffer.addVertex(matrix4f, -size, 0, -size).setColor(255, 255, 255, (int)(220 * pulse)).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            buffer.addVertex(matrix4f, size, 0, -size).setColor(255, 255, 255, (int)(220 * pulse)).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            buffer.addVertex(matrix4f, size, 0, size).setColor(255, 255, 255, (int)(220 * pulse)).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            buffer.addVertex(matrix4f, -size, 0, size).setColor(255, 255, 255, (int)(220 * pulse)).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        });

        // 2. Glowing Procedural Cosmic Void Fissure Rays
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiRenderType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            int rayCount = 8;
            for (int r = 0; r < rayCount; r++) {
                double angle = (r / (double) rayCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.4);
                float rayLen = 120.0f + rng.nextFloat() * 180.0f;

                float curX = 0, curZ = 0;
                int steps = 6;
                for (int s = 0; s < steps; s++) {
                    float nxtX = curX + (float) Math.cos(angle) * (rayLen / steps) + (rng.nextFloat() - 0.5f) * 18.0f;
                    float nxtZ = curZ + (float) Math.sin(angle) * (rayLen / steps) + (rng.nextFloat() - 0.5f) * 18.0f;

                    drawSkyRay(matrix, buffer, curX, curZ, nxtX, nxtZ, 2.5f * (1.0f - (s / (float) steps)), 0.85f, 0.05f, 0.25f, 0.75f * pulse);
                    drawSkyRay(matrix, buffer, curX, curZ, nxtX, nxtZ, 0.9f * (1.0f - (s / (float) steps)), 1.0f, 0.9f, 0.95f, 0.95f * pulse);

                    curX = nxtX; curZ = nxtZ;
                }
            }
        });
    }

    private static void drawSkyRay(Matrix4f matrix, VertexConsumer consumer, float x1, float z1, float x2, float z2, float width, float r, float g, float b, float a) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * width;
        float nz = dx / len * width;

        consumer.addVertex(matrix, x1 - nx, -0.5f, z1 - nz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1 + nx, -0.5f, z1 + nz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2 + nx, -0.5f, z2 + nz).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2 - nx, -0.5f, z2 - nz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
