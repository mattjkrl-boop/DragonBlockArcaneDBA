package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.HeavenSplitterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Entity Renderer for Heaven Splitter in Minecraft 26.2.
 * Renders a towering, physical 3D dimensional slash model across the entire dash path:
 * - Towering vertical spatial dimensional reality cut wall (6-8 blocks tall along raycast path)
 * - Inner blinding white-hot razor cutting core spine
 * - Cyan & silver plasma mantle with twin vacuum shock ribbons
 * - Transversal reality fracture ribs and vertical spatial fissures
 * - Concentric sonic shock rings at endpoints and midpoint.
 */
public class HeavenSplitterRenderer extends EntityRenderer<HeavenSplitterEntity, HeavenSplitterRenderer.HeavenSplitterRenderState> {

    public HeavenSplitterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class HeavenSplitterRenderState extends EntityRenderState {
        public Vec3 direction = new Vec3(1, 0, 0);
        public float dashLength = 16.0f;
        public float scale = 1.0f;
        public float age = 0;
        public int maxLifetime = 20;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(HeavenSplitterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(HeavenSplitterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public HeavenSplitterRenderState createRenderState() {
        return new HeavenSplitterRenderState();
    }

    @Override
    public void extractRenderState(HeavenSplitterEntity entity, HeavenSplitterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.direction = entity.getDashDirection();
        state.dashLength = entity.getDashLength();
        state.scale = entity.getSlashScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(HeavenSplitterRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        if (progress >= 1.0f) return;

        float rise = smooth(Math.min(1.0f, progress / 0.42f));
        float fade = 1.0f - smooth(progress);
        float impact = strike(progress);
        float scale = state.scale * (0.92f + 0.14f * rise);
        RenderType renderType = KiRenderHelper.kiRenderType();

        Vec3 dir = state.direction.normalize();
        double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float yaw = (float) (Math.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) -(Math.atan2(dir.y, horiz) * (180.0 / Math.PI));

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-yaw)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(pitch)));

        float len = state.dashLength;
        float slashHeight = 7.2f * scale * (0.12f + 0.88f * rise);

        float wallHalf = 0.045f * scale;
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            drawArcingSlashWall(matrix, buffer, len, slashHeight * 0.92f, wallHalf * 2.6f, 0.0f, 0.75f, 1.0f, 0.22f * fade, false);
            drawArcingSlashWall(matrix, buffer, len, slashHeight, wallHalf, 0.0f, 0.85f, 1.0f, 0.82f * fade, true);
            drawArcingSlashWall(matrix, buffer, len, slashHeight * 0.58f, wallHalf * 0.28f, 1.0f, 1.0f, 1.0f, fade, false);

            int arcs = Math.max(4, (int) (len / 3.5f));
            for (int s = 0; s <= arcs; s++) {
                float z = len * (s / (float) arcs);
                float angle = (s % 2 == 0 ? 36.0f : -36.0f);
                float arcSpan = 1.55f * scale * (0.78f + 0.22f * (float) Math.sin(s * 1.3));
                drawAngledCrossCleave(matrix, buffer, z, arcSpan, angle, 0.0f, 0.90f, 1.0f, fade * 0.72f);
            }

            if (impact > 0.02f) {
                float ringR = 1.7f * scale * (0.55f + 0.45f * impact);
                drawShockRingZ(matrix, buffer, 0, ringR, ringR * 0.70f, 28, 0.0f, 0.0f, 0.85f, 1.0f, impact * 0.75f);
                drawShockRingZ(matrix, buffer, len * 0.5f, ringR * 1.15f, ringR * 0.82f, 28, 0.0f, 1.0f, 1.0f, 1.0f, impact * 0.9f);
                drawShockRingZ(matrix, buffer, len, ringR * 1.25f, ringR * 0.88f, 28, 0.0f, 0.0f, 0.95f, 1.0f, impact);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    private static float strike(float progress) {
        if (progress >= 0.38f) return 0.0f;
        return (float) Math.sin((progress / 0.38f) * Math.PI);
    }

    /** Tall cut whose crest rises and falls, with a thin white lip along that arch. */
    private static void drawArcingSlashWall(Matrix4f matrix, VertexConsumer consumer, float len, float height, float half, float r, float g, float b, float alpha, boolean crest) {
        int segments = 28;
        for (int i = 0; i < segments; i++) {
            float u1 = i / (float) segments;
            float u2 = (i + 1) / (float) segments;
            float z1 = u1 * len;
            float z2 = u2 * len;
            float arch1 = (float) Math.sin(u1 * Math.PI);
            float arch2 = (float) Math.sin(u2 * Math.PI);
            float h1 = height * (0.20f + 0.80f * arch1);
            float h2 = height * (0.20f + 0.80f * arch2);
            float lead = 0.40f + 0.60f * u2;
            float aTop = alpha * lead;

            consumer.addVertex(matrix, -half, 0.0f, z1).setColor(r, g, b, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-1, 0, 0);
            consumer.addVertex(matrix, -half, h1, z1).setColor(r, g, b, aTop).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-1, 0, 0);
            consumer.addVertex(matrix, -half, h2, z2).setColor(r, g, b, aTop).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-1, 0, 0);
            consumer.addVertex(matrix, -half, 0.0f, z2).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-1, 0, 0);

            consumer.addVertex(matrix, half, 0.0f, z2).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
            consumer.addVertex(matrix, half, h2, z2).setColor(r, g, b, aTop).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
            consumer.addVertex(matrix, half, h1, z1).setColor(r, g, b, aTop).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
            consumer.addVertex(matrix, half, 0.0f, z1).setColor(r, g, b, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);

            if (crest) {
                float edge = half * 1.4f;
                consumer.addVertex(matrix, -edge, h1, z1).setColor(1.0f, 1.0f, 1.0f, aTop).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, edge, h1, z1).setColor(1.0f, 1.0f, 1.0f, aTop).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, edge, h2 + 0.05f, z2).setColor(0.85f, 0.96f, 1.0f, aTop).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, -edge, h2 + 0.05f, z2).setColor(0.85f, 0.96f, 1.0f, aTop).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            }
        }
    }

    private static void drawAngledCrossCleave(Matrix4f matrix, VertexConsumer consumer, float z, float span, float angleDeg, float r, float g, float b, float a) {
        float rad = (float) Math.toRadians(angleDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        float px = -sin;
        float py = cos;
        int steps = 8;
        for (int i = 0; i < steps; i++) {
            float t1 = (i / (float) steps) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) steps) * 2.0f - 1.0f;
            float u1 = t1 * span;
            float u2 = t2 * span;
            float v1 = (1.0f - t1 * t1) * span * 0.38f;
            float v2 = (1.0f - t2 * t2) * span * 0.38f;
            float w1 = 0.11f * (1.0f - Math.abs(t1) * 0.82f);
            float w2 = 0.11f * (1.0f - Math.abs(t2) * 0.82f);
            float x1 = u1 * cos + px * v1;
            float y1 = u1 * sin + py * v1 + 1.0f;
            float x2 = u2 * cos + px * v2;
            float y2 = u2 * sin + py * v2 + 1.0f;
            float edge = a * (0.35f + 0.65f * (1.0f - Math.abs(t1)));
            consumer.addVertex(matrix, x1 - px * w1, y1 - py * w1, z).setColor(r, g, b, edge * 0.55f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1 + px * w1, y1 + py * w1, z).setColor(r, g, b, edge).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2 + px * w2, y2 + py * w2, z).setColor(r, g, b, edge).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2 - px * w2, y2 - py * w2, z).setColor(r, g, b, edge * 0.55f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            float c1 = w1 * 0.22f;
            float c2 = w2 * 0.22f;
            consumer.addVertex(matrix, x1 - px * c1, y1 - py * c1, z).setColor(1.0f, 1.0f, 1.0f, edge).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1 + px * c1, y1 + py * c1, z).setColor(1.0f, 1.0f, 1.0f, edge).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2 + px * c2, y2 + py * c2, z).setColor(1.0f, 1.0f, 1.0f, edge).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2 - px * c2, y2 - py * c2, z).setColor(1.0f, 1.0f, 1.0f, edge).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1 - px * w1, y1 - py * w1, z - 0.16f).setColor(r, g, b, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1 + px * w1 * 0.2f, y1 + py * w1 * 0.2f, z).setColor(r, g, b, edge * 0.4f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2 + px * w2 * 0.2f, y2 + py * w2 * 0.2f, z).setColor(r, g, b, edge * 0.4f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2 - px * w2, y2 - py * w2, z - 0.16f).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawVerticalBladeWall(Matrix4f matrix, VertexConsumer consumer, float zStart, float zEnd, float height, float thickness, float r, float g, float b, float a) {
        float hTop = height;
        float hBottom = -0.5f;

        // Left Face
        consumer.addVertex(matrix, -thickness, hBottom, zStart).setColor(r, g, b, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, -thickness, hTop, zStart).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, -thickness, hTop, zEnd).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, -thickness, hBottom, zEnd).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-1, 0, 0);

        // Right Face
        consumer.addVertex(matrix, thickness, hBottom, zEnd).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, thickness, hTop, zEnd).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, thickness, hTop, zStart).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, thickness, hBottom, zStart).setColor(r, g, b, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
    }

    private static void drawVolumetricBeamZ(Matrix4f matrix, VertexConsumer consumer, float zStart, float zEnd, float radius, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            consumer.addVertex(matrix, x1, y1, zStart).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2, zStart).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2, zEnd).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, y1, zEnd).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawHorizontalShockPlanes(Matrix4f matrix, VertexConsumer consumer, float zStart, float zEnd, float span, float r, float g, float b, float a) {
        // Horizontal Quad
        consumer.addVertex(matrix, -span, 0, zStart).setColor(r, g, b, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, span, 0, zStart).setColor(r, g, b, 0.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, span, 0, zEnd).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -span, 0, zEnd).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Center band
        consumer.addVertex(matrix, -span * 0.3f, 0, zStart).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, span * 0.3f, 0, zStart).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, span * 0.3f, 0, zEnd).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -span * 0.3f, 0, zEnd).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawTransversalFractureRib(Matrix4f matrix, VertexConsumer consumer, float z, float width, float height, float r, float g, float b, float a) {
        consumer.addVertex(matrix, -width, 0, z).setColor(r, g, b, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, 0, height, z).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, width, 0, z).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, 0, -0.3f, z).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }

    private static void drawShockRingZ(Matrix4f matrix, VertexConsumer consumer, float z, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        float rotRad = (float) Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2) + rotRad;

            float x1 = (float) Math.cos(a1) * rOuter;
            float y1 = (float) Math.sin(a1) * rOuter;
            float x2 = (float) Math.cos(a2) * rOuter;
            float y2 = (float) Math.sin(a2) * rOuter;

            float ix1 = (float) Math.cos(a1) * rInner;
            float iy1 = (float) Math.sin(a1) * rInner;
            float ix2 = (float) Math.cos(a2) * rInner;
            float iy2 = (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, ix1, iy1, z).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, ix2, iy2, z).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, y2, z).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, y1, z).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawDiamondSpark(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float r, float g, float b, float a) {
        float h = size * 1.5f;
        float w = size * 0.5f;

        consumer.addVertex(matrix, cx - w, cy, cz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx + w, cy, cz).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        consumer.addVertex(matrix, cx, cy, cz - w).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy, cz + w).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
    }
}
