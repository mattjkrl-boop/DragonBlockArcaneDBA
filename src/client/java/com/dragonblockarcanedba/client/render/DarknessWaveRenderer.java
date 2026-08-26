package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DarknessWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * Entity Renderer for Darkness Wave in Minecraft 26.2.
 * Renders a towering, volumetric 3D physical void-crescent wave with multi-layered
 * diamond-beveled blades, glowing purple void core, obsidian armor shell, and orbiting trailing shadow shards.
 */
public class DarknessWaveRenderer extends EntityRenderer<DarknessWaveEntity, DarknessWaveRenderer.DarknessWaveRenderState> {

    public DarknessWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DarknessWaveRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public boolean isSecondary = false;
        public boolean isFirstPersonOwner = false;
    }

    @Override
    public boolean shouldRender(DarknessWaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public DarknessWaveRenderState createRenderState() {
        return new DarknessWaveRenderState();
    }

    @Override
    public void extractRenderState(DarknessWaveEntity entity, DarknessWaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.isSecondary = entity.isSecondary();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(DarknessWaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        float fpScale = (state.isFirstPersonOwner && state.age < 5.0f) ? (0.55f + (state.age / 5.0f) * 0.45f) : 1.0f;
        float span = (state.isSecondary ? 3.0f : 4.4f) * fpScale;
        float chord = (state.isSecondary ? 1.0f : 1.5f) * fpScale;
        float height = (state.isSecondary ? 1.4f : 2.0f) * fpScale;
        int segments = 24;
        float age = state.age;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Abyssal Obsidian Outer Boundary Envelope
            draw3DVolumetricCrescent(matrix, buffer, span + 0.5f, chord + 0.4f, height * 1.25f, 0.38f, segments, 0.08f, 0.0f, 0.16f, 0.95f);

            // 2. Corrupted Void Violet Core Blade Body
            draw3DVolumetricCrescent(matrix, buffer, span, chord, height, 0.24f, segments, 0.58f, 0.05f, 0.90f, 0.96f);

            // 3. Demonic Crimson Razor Leading Edge & Runes
            draw3DVolumetricCrescent(matrix, buffer, span * 0.78f, chord * 0.78f, height * 0.75f, 0.12f, segments, 0.95f, 0.08f, 0.35f, 1.0f);

            // 4. Blinding Pure White-Purple Void Core Spine
            draw3DVolumetricCrescent(matrix, buffer, span * 0.45f, chord * 0.45f, height * 0.45f, 0.06f, segments, 1.0f, 0.85f, 1.0f, 1.0f);

            // 5. Orbiting Trailing 3D Shadow Crescents / Void Shards
            int trailingShards = 6;
            for (int i = 0; i < trailingShards; i++) {
                float side = (i % 2 == 0 ? 1.0f : -1.0f);
                float progress = (i / (float) trailingShards);
                float sx = side * (span * 0.5f + progress * span * 0.4f);
                float sz = -0.6f - progress * 1.2f;
                float sy = (float) Math.sin(age * 0.4f + i * 1.2f) * 0.4f;

                float shardSpan = 0.6f + (progress * 0.3f);
                float shardChord = 0.25f;

                drawMiniShardCrescent(matrix, buffer, sx, sy, sz, shardSpan, shardChord, side, 0.65f, 0.05f, 0.95f, 0.90f);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void draw3DVolumetricCrescent(Matrix4f matrix, VertexConsumer consumer,
                                                float span, float chord, float height, float thickness,
                                                int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;

            float x1 = t1 * span;
            float z1 = (1.0f - t1 * t1) * chord;
            float x2 = t2 * span;
            float z2 = (1.0f - t2 * t2) * chord;

            float curve1 = (1.0f - Math.abs(t1) * 0.65f);
            float curve2 = (1.0f - Math.abs(t2) * 0.65f);

            float th1 = thickness * curve1;
            float th2 = thickness * curve2;

            float h1 = (height * 0.5f) * curve1;
            float h2 = (height * 0.5f) * curve2;

            float z1Trail = z1 - 0.7f * curve1;
            float z2Trail = z2 - 0.7f * curve2;

            // Top Diamond Ridge
            consumer.addVertex(matrix, x1, h1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, h2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, 0, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, 0, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom Diamond Ridge
            consumer.addVertex(matrix, x1, 0, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, 0, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -h2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, -h1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

            // Leading Edge Bevel Faces (Left and Right Flanks)
            consumer.addVertex(matrix, x1, -h1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, -h2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, h2, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, h1, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawMiniShardCrescent(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float span, float chord, float side, float r, float g, float b, float a) {
        float x1 = cx - span * 0.5f;
        float x2 = cx + span * 0.5f;
        float zLead = cz + chord;
        float zTrail = cz - chord * 0.8f;
        float h = 0.22f;

        consumer.addVertex(matrix, x1, cy + h, cz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, cy + h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy, zLead).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy, zLead).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        consumer.addVertex(matrix, x1, cy - h, cz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, cx, cy, zLead).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, cy - h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, cy - h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
