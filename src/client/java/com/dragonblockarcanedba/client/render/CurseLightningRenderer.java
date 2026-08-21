package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.CurseLightningEntity;
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

public class CurseLightningRenderer extends EntityRenderer<CurseLightningEntity, CurseLightningRenderer.CurseLightningRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/entity/curse_lightning.png");
    private static final Identifier RED_TEXTURE = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/entity/curse_lightning_red.png");

    public CurseLightningRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CurseLightningRenderState extends EntityRenderState {
        public boolean isRare;
        public float age;
        public long seed;
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
        state.age = (20 - entity.life) + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(CurseLightningRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float flashTime = state.age;
        if (flashTime > 16.0f) return;

        // Flash curve: intense burst at 0-3 ticks, then crackling flicker & fade out
        float flashAlpha = 1.0f;
        if (flashTime > 4.0f) {
            flashAlpha = Math.max(0.0f, 1.0f - ((flashTime - 4.0f) / 12.0f));
            if (((int) (flashTime * 4)) % 2 == 0) {
                flashAlpha *= 0.65f;
            }
        }

        final float finalAlpha = flashAlpha;
        Identifier tex = state.isRare ? TEXTURE : RED_TEXTURE; // Default is blood crimson, rare is cursed violet
        RenderType texturedType = RenderTypes.entityTranslucentEmissive(tex);
        RenderType glowType = KiRenderHelper.kiRenderType();

        // Pass 1: Textured 3D Volumetric Branching Lightning (Using High-Res Electric Texture)
        collector.submitCustomGeometry(poseStack, texturedType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            float r = 1.0f, g = 1.0f, b = 1.0f;

            // 1. Procedural Jagged Main Trunk
            int trunkSegments = 20;
            float totalHeight = 42.0f;
            float[] nodeX = new float[trunkSegments + 1];
            float[] nodeY = new float[trunkSegments + 1];
            float[] nodeZ = new float[trunkSegments + 1];

            nodeX[0] = 0; nodeY[0] = 0; nodeZ[0] = 0;

            for (int i = 1; i <= trunkSegments; i++) {
                float progress = i / (float) trunkSegments;
                nodeY[i] = progress * totalHeight;

                float variance = (float) Math.sin(progress * Math.PI) * 2.2f + (progress * 0.8f);
                nodeX[i] = nodeX[i - 1] + (rng.nextFloat() - 0.5f) * variance;
                nodeZ[i] = nodeZ[i - 1] + (rng.nextFloat() - 0.5f) * variance;
            }

            // Draw Main Trunk (Textured 3D volumetric cross ribbons)
            for (int i = 0; i < trunkSegments; i++) {
                float x1 = nodeX[i], y1 = nodeY[i], z1 = nodeZ[i];
                float x2 = nodeX[i + 1], y2 = nodeY[i + 1], z2 = nodeZ[i + 1];
                float v1 = i / (float) trunkSegments;
                float v2 = (i + 1) / (float) trunkSegments;

                renderBoltSegmentTextured(matrix, buffer, x1, y1, z1, x2, y2, z2, 0.85f, r, g, b, finalAlpha, v1, v2);
            }

            // 2. Procedural Branching Forks
            int forkCount = 4 + rng.nextInt(4);
            for (int f = 0; f < forkCount; f++) {
                int startNode = 4 + rng.nextInt(trunkSegments - 6);
                float curX = nodeX[startNode];
                float curY = nodeY[startNode];
                float curZ = nodeZ[startNode];

                float forkDirX = (rng.nextFloat() - 0.5f) * 1.8f;
                float forkDirZ = (rng.nextFloat() - 0.5f) * 1.8f;
                int forkLen = 3 + rng.nextInt(4);

                for (int s = 0; s < forkLen; s++) {
                    float nxtX = curX + forkDirX + (rng.nextFloat() - 0.5f) * 1.2f;
                    float nxtY = curY - (1.8f + rng.nextFloat() * 1.5f);
                    float nxtZ = curZ + forkDirZ + (rng.nextFloat() - 0.5f) * 1.2f;

                    float width = 0.55f * (1.0f - (s / (float) forkLen));
                    float v1 = s / (float) forkLen;
                    float v2 = (s + 1) / (float) forkLen;
                    renderBoltSegmentTextured(matrix, buffer, curX, curY, curZ, nxtX, nxtY, nxtZ, width, r, g, b, finalAlpha * 0.85f, v1, v2);

                    curX = nxtX; curY = nxtY; curZ = nxtZ;
                }
            }
        });

        // Pass 2: White-Hot Plasma Core & Ground Detonation Rings
        collector.submitCustomGeometry(poseStack, glowType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            float outerR = state.isRare ? 0.75f : 0.95f;
            float outerG = state.isRare ? 0.05f : 0.02f;
            float outerB = state.isRare ? 0.95f : 0.12f;

            // Ground Impact Detonation Shockwave Ring & Crawling Arcs
            float groundRingRadius = Math.min(3.8f, flashTime * 0.75f);
            float ringAlpha = finalAlpha * Math.max(0.0f, 1.0f - (flashTime / 10.0f));
            if (ringAlpha > 0.01f) {
                drawGroundRing(matrix, buffer, 0, 0.05f, 0, groundRingRadius, groundRingRadius * 0.75f, 16, outerR, outerG, outerB, ringAlpha * 0.75f);
                drawGroundRing(matrix, buffer, 0, 0.06f, 0, groundRingRadius * 0.6f, 0.0f, 12, 1.0f, 0.95f, 0.95f, ringAlpha * 0.9f);
            }
        });
    }

    private static void renderBoltSegmentTextured(Matrix4f matrix, VertexConsumer consumer,
                                                  float x1, float y1, float z1,
                                                  float x2, float y2, float z2,
                                                  float radius,
                                                  float r, float g, float b, float a,
                                                  float v1, float v2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * radius;
        float nz = dx / len * radius;
        float ny = radius;

        // Quad 1: X/Z plane (Front + Back)
        consumer.addVertex(matrix, x1 - nx, y1, z1 - nz).setColor(r, g, b, a).setUv(0, v1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx, y1, z1 + nz).setColor(r, g, b, a).setUv(1, v1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx, y2, z2 + nz).setColor(r, g, b, a).setUv(1, v2).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx, y2, z2 - nz).setColor(r, g, b, a).setUv(0, v2).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        consumer.addVertex(matrix, x2 - nx, y2, z2 - nz).setColor(r, g, b, a).setUv(0, v2).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2 + nx, y2, z2 + nz).setColor(r, g, b, a).setUv(1, v2).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1 + nx, y1, z1 + nz).setColor(r, g, b, a).setUv(1, v1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1 - nx, y1, z1 - nz).setColor(r, g, b, a).setUv(0, v1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

        // Quad 2: Orthogonal Y plane (Front + Back)
        consumer.addVertex(matrix, x1, y1 - ny, z1).setColor(r, g, b, a).setUv(0, v1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, x1, y1 + ny, z1).setColor(r, g, b, a).setUv(1, v1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, x2, y2 + ny, z2).setColor(r, g, b, a).setUv(1, v2).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, x2, y2 - ny, z2).setColor(r, g, b, a).setUv(0, v2).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);

        consumer.addVertex(matrix, x2, y2 - ny, z2).setColor(r, g, b, a).setUv(0, v2).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, x2, y2 + ny, z2).setColor(r, g, b, a).setUv(1, v2).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, x1, y1 + ny, z1).setColor(r, g, b, a).setUv(1, v1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, x1, y1 - ny, z1).setColor(r, g, b, a).setUv(0, v1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-1, 0, 0);
    }

    private static void drawGroundRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2;

            float x1 = cx + (float) Math.cos(a1) * rOuter;
            float z1 = cz + (float) Math.sin(a1) * rOuter;
            float x2 = cx + (float) Math.cos(a2) * rOuter;
            float z2 = cz + (float) Math.sin(a2) * rOuter;

            float ix1 = cx + (float) Math.cos(a1) * rInner;
            float iz1 = cz + (float) Math.sin(a1) * rInner;
            float ix2 = cx + (float) Math.cos(a2) * rInner;
            float iz2 = cz + (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, ix1, cy, iz1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ix2, cy, iz2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, cy, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, cy, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }
}
