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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Entity Renderer for Curse Lightning Strikes in Minecraft 26.2.
 * Renders a violent, multi-tiered 3D supernatural electric cataclysm:
 * - Multi-tier jagged 3D cross-volumetric lightning trunk with fractal kinking
 * - 4-8 secondary and tertiary fractal branching forks
 * - Ground crawling electrical arc fissures shooting across terrain
 * - Dual-layer rendering: Outer textured plasma discharge + Inner white-hot core beam
 * - Expanding ground shockwave rings and corrupted hex detonation seals
 */
public class CurseLightningRenderer extends EntityRenderer<CurseLightningEntity, CurseLightningRenderer.CurseLightningRenderState> {

    public CurseLightningRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CurseLightningRenderState extends EntityRenderState {
        public boolean isRare;
        public float age;
        public long seed;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(CurseLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(CurseLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
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
        state.age = (CurseLightningEntity.LIFE_TICKS - entity.life) + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(CurseLightningRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float flashTime = state.age;
        // The entity lives exactly CurseLightningEntity.LIFE_TICKS and the
        // fade below reaches zero at tick 9.5, so picture and lifetime agree.
        if (flashTime > CurseLightningEntity.LIFE_TICKS) return;

        float strike = ease(Math.min(1.0f, flashTime / 2.5f));
        float fadeT = flashTime <= 2.5f ? 1.0f : Math.max(0.0f, 1.0f - (flashTime - 2.5f) / 7.0f);
        float flashAlpha = ease(fadeT);
        if (flashAlpha <= 0.02f) return;

        boolean rare = state.isRare;
        float sheathR = rare ? 0.55f : 0.80f;
        float sheathG = 0.04f;
        float sheathB = rare ? 0.95f : 0.40f;
        RenderType glowType = KiRenderHelper.kiRenderType();
        float cloud = 16.0f;
        long boltSeed = state.seed;

        collector.submitCustomGeometry(poseStack, glowType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(boltSeed);

            int trunkSegments = 16;
            float[] nodeX = new float[trunkSegments + 1];
            float[] nodeY = new float[trunkSegments + 1];
            float[] nodeZ = new float[trunkSegments + 1];
            nodeX[0] = 0.0f;
            nodeY[0] = cloud;
            nodeZ[0] = 0.0f;

            for (int i = 1; i <= trunkSegments; i++) {
                float progress = i / (float) trunkSegments;
                nodeY[i] = cloud * (1.0f - progress);
                float variance = (float) Math.sin(progress * Math.PI) * 1.35f;
                nodeX[i] = nodeX[i - 1] + (rng.nextFloat() - 0.5f) * variance;
                nodeZ[i] = nodeZ[i - 1] + (rng.nextFloat() - 0.5f) * variance;
            }

            int drawn = Math.max(1, Math.min(trunkSegments, Math.round(strike * trunkSegments)));
            for (int i = 0; i < drawn; i++) {
                float u = i / (float) trunkSegments;
                float pinch = 0.25f + 0.75f * (float) Math.sin(Math.max(0.08f, u) * Math.PI);
                float width = 0.13f * pinch;
                renderJaggedBolt(matrix, buffer, nodeX[i], nodeY[i], nodeZ[i], nodeX[i + 1], nodeY[i + 1], nodeZ[i + 1], width, sheathR, sheathG, sheathB, flashAlpha);
            }

            int forkCount = 4;
            for (int f = 0; f < forkCount; f++) {
                int span = Math.max(1, drawn - 3);
                int startNode = 2 + rng.nextInt(span);
                if (startNode >= drawn) {
                    continue;
                }
                float curX = nodeX[startNode];
                float curY = nodeY[startNode];
                float curZ = nodeZ[startNode];
                float forkDirX = (rng.nextFloat() - 0.5f) * 1.4f;
                float forkDirZ = (rng.nextFloat() - 0.5f) * 1.4f;
                int forkLen = 3;
                for (int s = 0; s < forkLen; s++) {
                    float nxtX = curX + forkDirX + (rng.nextFloat() - 0.5f) * 0.55f;
                    float nxtY = curY - (0.85f + rng.nextFloat() * 0.65f);
                    float nxtZ = curZ + forkDirZ + (rng.nextFloat() - 0.5f) * 0.55f;
                    float width = 0.08f * (1.0f - s / (float) forkLen);
                    renderJaggedBolt(matrix, buffer, curX, curY, curZ, nxtX, nxtY, nxtZ, width, sheathR, sheathG, sheathB, flashAlpha * 0.85f);
                    curX = nxtX;
                    curY = nxtY;
                    curZ = nxtZ;
                }
            }

            float groundRingRadius = 3.2f * ease(Math.min(1.0f, flashTime / 6.0f));
            float ringAlpha = flashAlpha * Math.max(0.0f, 1.0f - flashTime / 9.0f);
            if (ringAlpha > 0.01f) {
                drawGroundRing(matrix, buffer, 0, 0.05f, 0, groundRingRadius, groundRingRadius * 0.78f, 28, sheathR, sheathG, sheathB, ringAlpha * 0.80f);
                drawGroundRing(matrix, buffer, 0, 0.06f, 0, groundRingRadius * 0.42f, groundRingRadius * 0.24f, 20, 1.0f, 0.95f, 0.96f, ringAlpha);
            }
        });

    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }

    /** One kink of the bolt: a bowed sheath, a white core, and a wake that dies. */
    private static void renderJaggedBolt(Matrix4f matrix, VertexConsumer consumer,
                                         float x1, float y1, float z1, float x2, float y2, float z2,
                                         float radius, float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f || a < 0.02f) {
            return;
        }
        float px = -dz / len;
        float pz = dx / len;
        float bow = radius * 2.4f;
        float mx = (x1 + x2) * 0.5f + px * bow;
        float my = (y1 + y2) * 0.5f + radius * 0.55f;
        float mz = (z1 + z2) * 0.5f + pz * bow;
        boltHalf(matrix, consumer, x1, y1, z1, mx, my, mz, radius, radius * 0.62f, r, g, b, a, a * 0.9f);
        boltHalf(matrix, consumer, mx, my, mz, x2, y2, z2, radius * 0.62f, radius * 0.28f, r, g, b, a * 0.9f, a * 0.55f);
        boltHalf(matrix, consumer, x1, y1, z1, mx, my, mz, radius * 0.28f, radius * 0.16f, 1.0f, 0.94f, 0.98f, a, a);
        boltHalf(matrix, consumer, mx, my, mz, x2, y2, z2, radius * 0.16f, radius * 0.06f, 1.0f, 0.94f, 0.98f, a, a * 0.7f);
        boltHalf(matrix, consumer, mx, my, mz, mx - px * bow * 1.5f, my - radius * 0.3f, mz - pz * bow * 1.5f, radius * 0.45f, 0.01f, r, g, b, a * 0.5f, 0.0f);
    }

    private static void boltHalf(Matrix4f matrix, VertexConsumer consumer,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 float w1, float w2, float r, float g, float b, float a1, float a2) {
        float dx = x2 - x1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + (y2 - y1) * (y2 - y1) + dz * dz);
        if (len < 0.001f) {
            return;
        }
        float nx = -dz / len;
        float nz = dx / len;
        consumer.addVertex(matrix, x1 - nx * w1, y1, z1 - nz * w1).setColor(r, g, b, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx * w1, y1, z1 + nz * w1).setColor(r, g, b, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx * w2, y2, z2 + nz * w2).setColor(r, g, b, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx * w2, y2, z2 - nz * w2).setColor(r, g, b, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1 - w1, z1).setColor(r, g, b, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, x1, y1 + w1, z1).setColor(r, g, b, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, x2, y2 + w2, z2).setColor(r, g, b, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, x2, y2 - w2, z2).setColor(r, g, b, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
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
