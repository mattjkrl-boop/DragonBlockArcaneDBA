package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.SkyCracksEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Entity Renderer for Sky Cracks in Minecraft 26.2.
 * Renders a cosmic 3D celestial tear tearing through the atmosphere:
 * - 12 Procedural fractal void fissure branches extending hundreds of blocks
 * - 16 Hovering 3D tumbling crystalline void shards in the stratosphere
 * - Volumetric multi-layered spatial tear net with pulsing core
 * - Dual-layer rendering: Textured emissive celestial rift + Inner incandescent plasma beams
 */
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

        float pulse = 0.85f + 0.15f * (float) Math.sin(state.age * 0.18f);

        // 1. Base Emissive Celestial Texture Quad
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(TEXTURE), (pose, buffer) -> {
            Matrix4f matrix4f = pose.pose();
            float size = 480.0f;

            buffer.addVertex(matrix4f, -size, 0, -size).setColor(255, 255, 255, (int) (240 * pulse)).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            buffer.addVertex(matrix4f, size, 0, -size).setColor(255, 255, 255, (int) (240 * pulse)).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            buffer.addVertex(matrix4f, size, 0, size).setColor(255, 255, 255, (int) (240 * pulse)).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            buffer.addVertex(matrix4f, -size, 0, size).setColor(255, 255, 255, (int) (240 * pulse)).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        });

        // 2. Glowing Procedural Cosmic Void Fissure Rays & Hovering Void Crystals
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiRenderType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // A. 12 Major Procedural Void Fracture Branches
            int rayCount = 12;
            for (int r = 0; r < rayCount; r++) {
                double angle = (r / (double) rayCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.35);
                float rayLen = 140.0f + rng.nextFloat() * 220.0f;

                float curX = 0, curZ = 0;
                int steps = 7;
                for (int s = 0; s < steps; s++) {
                    float nxtX = curX + (float) Math.cos(angle) * (rayLen / steps) + (rng.nextFloat() - 0.5f) * 22.0f;
                    float nxtZ = curZ + (float) Math.sin(angle) * (rayLen / steps) + (rng.nextFloat() - 0.5f) * 22.0f;

                    float width = 3.2f * (1.0f - (s / (float) steps));

                    // Outer Corrupted Crimson/Violet Core
                    drawSkyRay(matrix, buffer, curX, curZ, nxtX, nxtZ, width, 0.95f, 0.05f, 0.25f, 0.85f * pulse);
                    // Inner Incandescent Arc
                    drawSkyRay(matrix, buffer, curX, curZ, nxtX, nxtZ, width * 0.35f, 1.0f, 0.90f, 0.95f, 0.98f * pulse);

                    curX = nxtX; curZ = nxtZ;
                }
            }

            // B. 16 Hovering 3D Crystalline Void Shards in Stratosphere
            int shardCount = 16;
            for (int i = 0; i < shardCount; i++) {
                double sAngle = (i / (double) shardCount) * Math.PI * 2.0 + (state.age * 0.02);
                float sRadius = 25.0f + (i * 12.0f);
                float sx = (float) Math.cos(sAngle) * sRadius;
                float sz = (float) Math.sin(sAngle) * sRadius;
                float sy = (float) Math.sin(state.age * 0.1f + i) * 6.0f;

                float shardSize = 3.5f + (i % 3) * 2.0f;
                drawSkyVoidShard(matrix, buffer, sx, sy, sz, shardSize, state.age * 12.0f + i * 45.0f, pulse);
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

    private static void drawSkyVoidShard(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float s, float rotDeg, float pulse) {
        double rad = Math.toRadians(rotDeg);
        float cos = (float) Math.cos(rad) * s;
        float sin = (float) Math.sin(rad) * s;

        // 3D Octahedron crystal
        float[][] pts = { {cos,0,sin}, {-sin,0,cos}, {-cos,0,-sin}, {sin,0,-cos} };
        for (int i = 0; i < 4; i++) {
            int nxt = (i + 1) % 4;
            consumer.addVertex(matrix, cx + pts[i][0], cy, cz + pts[i][2]).setColor(0.65f, 0.02f, 0.95f, 0.85f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx + pts[nxt][0], cy, cz + pts[nxt][2]).setColor(0.65f, 0.02f, 0.95f, 0.85f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, cy + s * 1.6f, cz).setColor(1.0f, 0.15f, 0.45f, 0.95f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, cy + s * 1.6f, cz).setColor(1.0f, 0.15f, 0.45f, 0.95f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            consumer.addVertex(matrix, cx + pts[i][0], cy, cz + pts[i][2]).setColor(0.65f, 0.02f, 0.95f, 0.85f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, cx + pts[nxt][0], cy, cz + pts[nxt][2]).setColor(0.65f, 0.02f, 0.95f, 0.85f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, cx, cy - s * 1.6f, cz).setColor(1.0f, 0.15f, 0.45f, 0.95f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, cx, cy - s * 1.6f, cz).setColor(1.0f, 0.15f, 0.45f, 0.95f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }
}
