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

    @Override
    public boolean shouldRender(HeavenSplitterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
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

        float fade = 1.0f - (progress * progress);
        float scale = state.scale * (1.0f + progress * 0.20f);
        RenderType renderType = KiRenderHelper.kiRenderType();

        Vec3 dir = state.direction.normalize();
        double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float yaw = (float) (Math.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) -(Math.atan2(dir.y, horiz) * (180.0 / Math.PI));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        float len = state.dashLength;
        float halfLen = len * 0.5f;
        float slashHeight = 6.5f * scale;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Towering Vertical Spatial Reality Cut Wall (Planar blade sheet slicing 6.5 blocks high)
            float wallThickness = 0.08f * scale;
            drawVerticalBladeWall(matrix, buffer, 0, len, slashHeight, wallThickness,
                0.0f, 0.85f, 1.0f, 0.70f * fade);

            // 2. Blinding White-Hot Razor Cutting Core Spine
            float coreThickness = 0.025f * scale;
            drawVerticalBladeWall(matrix, buffer, 0, len, slashHeight * 0.7f, coreThickness,
                1.0f, 1.0f, 1.0f, 0.98f * fade);

            // 3. Volumetric Longitudinal Plasma Beam along cutting trajectory
            float mantleRadius = 0.45f * scale;
            drawVolumetricBeamZ(matrix, buffer, 0, len, mantleRadius, 8,
                0.80f, 0.95f, 1.0f, 0.85f * fade);
            drawVolumetricBeamZ(matrix, buffer, 0, len, 0.15f * scale, 6,
                1.0f, 1.0f, 1.0f, 1.0f * fade);

            // 4. Twin Vacuum Shockwave Wings Flaring Outward
            float wingSpan = 1.2f * scale;
            drawHorizontalShockPlanes(matrix, buffer, 0, len, wingSpan,
                0.15f, 0.90f, 1.0f, 0.60f * fade);

            // 5. Transversal Reality Fracture Ribs & Vertical Spatial Fissures
            int ribCount = Math.max(6, (int) (len * 0.9f));
            for (int r = 0; r <= ribCount; r++) {
                float z = len * (r / (float) ribCount);
                float ribH = slashHeight * (0.8f + 0.3f * (float) Math.sin(r * 1.4f + state.age * 0.3f));
                float ribW = 0.8f * scale;
                drawTransversalFractureRib(matrix, buffer, z, ribW, ribH,
                    0.0f, 0.90f, 1.0f, fade * 0.80f);
            }

            // 6. Concentric Sonic Shock Rings at Start, Midpoint, and Destination
            float ringR = 2.2f * scale;
            drawShockRingZ(matrix, buffer, 0, ringR, ringR * 0.75f, 20, state.age * 20.0f, 0.0f, 0.85f, 1.0f, fade * 0.85f);
            drawShockRingZ(matrix, buffer, halfLen, ringR * 1.4f, ringR * 1.05f, 20, -state.age * 30.0f, 1.0f, 1.0f, 1.0f, fade * 0.95f);
            drawShockRingZ(matrix, buffer, len, ringR * 1.6f, ringR * 1.20f, 20, state.age * 40.0f, 0.0f, 0.95f, 1.0f, fade * 0.90f);

            // 8. Cascading Angled 3D Spatial Cross-Cleave Crescent Blades along dash corridor
            int slashArcCount = Math.max(5, (int) (len / 3.0f));
            for (int s = 0; s <= slashArcCount; s++) {
                float z = len * (s / (float) slashArcCount);
                float angle = (s % 2 == 0 ? 45.0f : -45.0f) + ((s % 3) * 15.0f);
                float arcSpan = (2.4f + 0.4f * (float) Math.sin(s * 1.5f)) * scale;
                drawAngledCrossCleave(matrix, buffer, z, arcSpan, angle,
                    0.0f, 0.90f, 1.0f, fade * 0.75f);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawAngledCrossCleave(Matrix4f matrix, VertexConsumer consumer, float z, float span, float angleDeg, float r, float g, float b, float a) {
        float rad = (float) Math.toRadians(angleDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float x1 = -span * cos;
        float y1 = -span * sin + 1.0f;
        float x2 = span * cos;
        float y2 = span * sin + 1.0f;

        float chord = span * 0.35f;
        float cx = 0;
        float cy = 1.0f + chord;
        float thick = 0.10f;

        // Front face
        consumer.addVertex(matrix, x1, y1 - thick, z).setColor(r, g, b, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, cx, cy, z).setColor(1.0f, 1.0f, 1.0f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2, y2 - thick, z).setColor(r, g, b, 0.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, 0, 1.0f - chord * 0.5f, z).setColor(r, g, b, a * 0.5f).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);

        // Back face
        consumer.addVertex(matrix, 0, 1.0f - chord * 0.5f, z).setColor(r, g, b, a * 0.5f).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
        consumer.addVertex(matrix, x2, y2 - thick, z).setColor(r, g, b, 0.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
        consumer.addVertex(matrix, cx, cy, z).setColor(1.0f, 1.0f, 1.0f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
        consumer.addVertex(matrix, x1, y1 - thick, z).setColor(r, g, b, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
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
