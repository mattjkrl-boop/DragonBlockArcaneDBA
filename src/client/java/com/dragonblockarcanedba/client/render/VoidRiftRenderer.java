package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.VoidRiftEntity;
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
 * Entity Renderer for Void Rift Singularity in Minecraft 26.2.
 * Renders a 3D spherical singularity event horizon, counter-rotating accretion disks, and vertical spatial distortion tears.
 */
public class VoidRiftRenderer extends EntityRenderer<VoidRiftEntity, VoidRiftRenderer.RiftRenderState> {

    public VoidRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class RiftRenderState extends EntityRenderState {
        public float radius = 2.5f;
        public boolean isImploding = false;
        public float age = 0;
    }

    @Override
    public RiftRenderState createRenderState() {
        return new RiftRenderState();
    }

    @Override
    public void extractRenderState(VoidRiftEntity entity, RiftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.isImploding = entity.isImploding();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(RiftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float radius = state.radius;
        float rot = state.age * (state.isImploding ? 30.0f : 10.0f);
        float pulse = 0.85f + 0.15f * (float) Math.sin(state.age * 0.3f);

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Central Singularity Black Sphere
            drawSingularityOctahedron(matrix, buffer, radius * 0.45f * pulse, 0.05f, 0.0f, 0.1f, 0.98f);
            drawSingularityOctahedron(matrix, buffer, radius * 0.25f, 1.0f, 1.0f, 1.0f, 1.0f);

            // 2. Primary Horizontal Accretion Disk (Spins Clockwise)
            drawRotatingDisk(matrix, buffer, rot, radius, radius * 0.5f, 28, 0.65f, 0.05f, 0.95f, 0.90f);

            // 3. Secondary Inclined Accretion Disk (Spins Counter-Clockwise)
            drawRotatingDisk(matrix, buffer, -rot * 1.3f, radius * 0.85f, radius * 0.4f, 24, 0.1f, 0.85f, 1.0f, 0.80f);

            // 4. Vertical Dimensional Eye Tear
            float vHeight = radius * 1.8f;
            float vWidth = radius * 0.35f;
            drawQuad(matrix, buffer, -vWidth, -vHeight, 0, vWidth, -vHeight, 0, vWidth, vHeight, 0, -vWidth, vHeight, 0, 0.9f, 0.1f, 1.0f, 0.9f);
            drawQuad(matrix, buffer, 0, -vHeight, -vWidth, 0, -vHeight, vWidth, 0, vHeight, vWidth, 0, vHeight, -vWidth, 0.15f, 0.9f, 1.0f, 0.9f);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawRotatingDisk(Matrix4f matrix, VertexConsumer consumer, float rotDeg, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
        double radOffset = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2 + radOffset;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2 + radOffset;

            float x1Out = (float) Math.cos(a1) * rOuter;
            float z1Out = (float) Math.sin(a1) * rOuter;
            float x2Out = (float) Math.cos(a2) * rOuter;
            float z2Out = (float) Math.sin(a2) * rOuter;

            float x1In = (float) Math.cos(a1) * rInner;
            float z1In = (float) Math.sin(a1) * rInner;
            float x2In = (float) Math.cos(a2) * rInner;
            float z2In = (float) Math.sin(a2) * rInner;

            drawQuad(matrix, consumer, x1In, 0, z1In, x2In, 0, z2In, x2Out, 0, z2Out, x1Out, 0, z1Out, r, g, b, a);
        }
    }

    private static void drawSingularityOctahedron(Matrix4f matrix, VertexConsumer consumer, float s, float r, float g, float b, float a) {
        float[][] top = { {s,0,0}, {0,0,s}, {-s,0,0}, {0,0,-s} };
        for (int i = 0; i < 4; i++) {
            int nxt = (i + 1) % 4;
            // Top pyramid
            consumer.addVertex(matrix, top[i][0], 0, top[i][2]).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, top[nxt][0], 0, top[nxt][2]).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, s * 1.3f, 0).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, s * 1.3f, 0).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom pyramid
            consumer.addVertex(matrix, top[i][0], 0, top[i][2]).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, top[nxt][0], 0, top[nxt][2]).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, 0, -s * 1.3f, 0).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, 0, -s * 1.3f, 0).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }

    private static void drawQuad(Matrix4f matrix, VertexConsumer consumer,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
