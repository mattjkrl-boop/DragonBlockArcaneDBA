package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.TimeShatterEntity;
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
 * Entity Renderer for Time Shatter in Minecraft 26.2.
 * Renders an explosive physical 3D celestial glass shatter and fractured chrono-mirror burst:
 * - 16 Physical 3D Prismatic Glass & Chrono-Mirror Shards tumbling in 3D space
 * - Fractured radial chrono-mirror web & crack spokes
 * - Splintering celestial chrono-dial ring
 * - Radiant celestial core diamond / octahedron burst
 */
public class TimeShatterRenderer extends EntityRenderer<TimeShatterEntity, TimeShatterRenderer.ShatterRenderState> {

    public TimeShatterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShatterRenderState extends EntityRenderState {
        public float scale = 1.2f;
        public float age = 0;
        public int maxLifetime = 18;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(TimeShatterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(TimeShatterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ShatterRenderState createRenderState() {
        return new ShatterRenderState();
    }

    @Override
    public void extractRenderState(TimeShatterEntity entity, ShatterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getShatterScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(ShatterRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        if (progress >= 1.0f) return;

        float travel = 1.0f - (1.0f - progress) * (1.0f - progress) * (1.0f - progress);
        float exitT = progress <= 0.50f ? 0.0f : Math.min(1.0f, (progress - 0.50f) / 0.50f);
        float fade = 1.0f - exitT * exitT * (3.0f - 2.0f * exitT);
        float scale = state.scale * (0.55f + travel * 0.85f);
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        // The dial and spokes are drawn flat in the local XY plane and the
        // entity syncs no yaw, so they used to face one fixed world direction
        // and nearly vanish edge-on. That layer is now billboarded to the
        // camera with the conjugated camera orientation; the 3D shards and
        // core below stay in world space.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(new org.joml.Quaternionf(cameraState.orientation).conjugate()));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Splintering Celestial Chrono-Dial Ring
            int ringSegments = 36;
            float ringR = 1.8f * scale;
            float ringWidth = 0.12f * (1.0f - progress * 0.6f);
            drawSplinteringDial(matrix, buffer, ringR, ringWidth, ringSegments, state.age * 25.0f, progress, fade);

            // 2. Fractured Chrono-Mirror Web & Radial Crack Spokes (8 Spokes)
            int spokeCount = 8;
            for (int i = 0; i < spokeCount; i++) {
                double angle = (i / (double) spokeCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.2);
                float spokeLen = (1.5f + rng.nextFloat() * 1.0f) * scale;
                float spokeWidth = 0.16f * (1.0f - progress * 0.8f);

                float tipX = (float) Math.cos(angle) * spokeLen;
                float tipY = (float) Math.sin(angle) * spokeLen;
                float tipZ = (rng.nextFloat() - 0.5f) * 0.4f * scale;

                drawSpoke(matrix, buffer, 0, 0, 0, tipX, tipY, tipZ, spokeWidth,
                    0.95f, 0.98f, 1.0f, fade * 0.9f,
                    0.50f, 0.85f, 1.0f, fade * 0.7f);
            }
        });

        poseStack.popPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 3. 16 Physical 3D Prismatic Glass & Chrono-Mirror Shards
            int shardCount = 16;
            for (int s = 0; s < shardCount; s++) {
                double shardAngle = (s / (double) shardCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.35);
                float speed = 1.4f + rng.nextFloat() * 1.8f;
                float dist = travel * speed * 2.4f * scale;

                float sx = (float) Math.cos(shardAngle) * dist;
                float sy = (float) Math.sin(shardAngle) * dist + (rng.nextFloat() - 0.5f) * 0.6f;
                float sz = (rng.nextFloat() - 0.5f) * dist * 0.8f;

                float shardSize = (0.22f + rng.nextFloat() * 0.18f) * (1.0f - progress * 0.7f) * scale;
                float rot = state.age * (30.0f + rng.nextFloat() * 50.0f);

                // Iridescent celestial colors: cyan, diamond-white, and lilac
                float cr = s % 3 == 0 ? 0.95f : (s % 3 == 1 ? 0.50f : 0.85f);
                float cg = s % 3 == 0 ? 0.98f : (s % 3 == 1 ? 0.90f : 0.75f);
                float cb = 1.0f;

                drawGlassPrismShard(matrix, buffer, sx, sy, sz, shardSize, rot, cr, cg, cb, fade);
                drawGlassPrismShard(matrix, buffer, sx, sy, sz, shardSize * 0.40f, rot + 16.0f, 1.0f, 1.0f, 1.0f, fade * 0.80f);
                float wake = 0.38f;
                drawSpoke(matrix, buffer,
                    sx * (1.0f - wake), sy * (1.0f - wake), sz * (1.0f - wake),
                    sx, sy, sz,
                    shardSize * 0.22f,
                    cr, cg, cb, 0.0f,
                    1.0f, 1.0f, 1.0f, fade * 0.50f);
            }

            // 4. Radiant Celestial Core Diamond / Octahedron Burst
            float coreSize = 0.85f * (1.0f - progress * 0.7f) * scale;
            drawOctahedralCore(matrix, buffer, 0, 0, 0, coreSize, state.age * 40.0f, 0.98f, 0.98f, 1.0f, fade);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawSplinteringDial(Matrix4f matrix, VertexConsumer consumer, float radius, float width, int segments, float rotDeg, float progress, float fade) {
        float rotRad = (float) Math.toRadians(rotDeg);
        float rInner = radius - width;
        float rOuter = radius + width;

        for (int i = 0; i < segments; i++) {
            // Slight separation between segments as time shatters
            float gap = progress * 0.15f;
            double a1 = ((i / (double) segments) * Math.PI * 2) + rotRad + gap;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2) + rotRad - gap;

            float x1 = (float) Math.cos(a1) * rOuter;
            float y1 = (float) Math.sin(a1) * rOuter;
            float x2 = (float) Math.cos(a2) * rOuter;
            float y2 = (float) Math.sin(a2) * rOuter;

            float ix1 = (float) Math.cos(a1) * rInner;
            float iy1 = (float) Math.sin(a1) * rInner;
            float ix2 = (float) Math.cos(a2) * rInner;
            float iy2 = (float) Math.sin(a2) * rInner;

            float alpha = fade * (0.85f - (i % 2 == 0 ? 0.0f : 0.25f));
            float r = i % 2 == 0 ? 0.95f : 0.60f;
            float g = i % 2 == 0 ? 0.98f : 0.90f;
            float b = 1.0f;

            // Radial gradient (T2): the inner edge of each segment runs hotter,
            // halfway to white, cooling to the segment color at the rim.
            float ir = r + (1.0f - r) * 0.5f;
            float ig = g + (1.0f - g) * 0.5f;
            float ib = 1.0f;

            consumer.addVertex(matrix, ix1, iy1, 0).setColor(ir, ig, ib, alpha).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, ix2, iy2, 0).setColor(ir, ig, ib, alpha).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, alpha).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, alpha).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);

            // Double sided
            consumer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, alpha).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, alpha).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, ix2, iy2, 0).setColor(ir, ig, ib, alpha).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, ix1, iy1, 0).setColor(ir, ig, ib, alpha).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
        }
    }

    private static void drawSpoke(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float width, float r1, float g1, float b1, float a1, float r2, float g2, float b2, float a2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) return;

        float bx = -dy;
        float by = dx;
        float bz = 0.0f;
        if (dx * dx + dy * dy < dz * dz) {
            bx = 1.0f;
            by = 0.0f;
            bz = 0.0f;
        }
        float blen = (float) Math.sqrt(bx * bx + by * by + bz * bz);
        bx /= blen;
        by /= blen;
        bz /= blen;

        int steps = 6;
        for (int i = 0; i < steps; i++) {
            float t1 = i / (float) steps;
            float t2 = (i + 1) / (float) steps;
            float bow1 = (float) Math.sin(t1 * Math.PI) * width * 0.85f;
            float bow2 = (float) Math.sin(t2 * Math.PI) * width * 0.85f;
            float w1 = width * (1.0f - t1 * 0.85f) * 0.5f;
            float w2 = width * (1.0f - t2 * 0.85f) * 0.5f;
            float fade1 = 1.0f - t1 * t1 * 0.40f;
            float fade2 = 1.0f - t2 * t2 * 0.40f;
            float er1 = r1 + (r2 - r1) * t1;
            float eg1 = g1 + (g2 - g1) * t1;
            float eb1 = b1 + (b2 - b1) * t1;
            float ea1 = (a1 + (a2 - a1) * t1) * fade1;
            float er2 = r1 + (r2 - r1) * t2;
            float eg2 = g1 + (g2 - g1) * t2;
            float eb2 = b1 + (b2 - b1) * t2;
            float ea2 = (a1 + (a2 - a1) * t2) * fade2;
            float sx1 = x1 + dx * t1 + bx * bow1;
            float sy1 = y1 + dy * t1 + by * bow1;
            float sz1 = z1 + dz * t1 + bz * bow1;
            float sx2 = x1 + dx * t2 + bx * bow2;
            float sy2 = y1 + dy * t2 + by * bow2;
            float sz2 = z1 + dz * t2 + bz * bow2;

            consumer.addVertex(matrix, sx1 - bx * w1, sy1 - by * w1, sz1 - bz * w1).setColor(er1, eg1, eb1, ea1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(bx, by, bz);
            consumer.addVertex(matrix, sx1 + bx * w1, sy1 + by * w1, sz1 + bz * w1).setColor(er1, eg1, eb1, ea1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(bx, by, bz);
            consumer.addVertex(matrix, sx2 + bx * w2, sy2 + by * w2, sz2 + bz * w2).setColor(er2, eg2, eb2, ea2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(bx, by, bz);
            consumer.addVertex(matrix, sx2 - bx * w2, sy2 - by * w2, sz2 - bz * w2).setColor(er2, eg2, eb2, ea2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(bx, by, bz);

            float c1 = w1 * 0.32f;
            float c2 = w2 * 0.32f;
            consumer.addVertex(matrix, sx1 - bx * c1, sy1 - by * c1, sz1 - bz * c1).setColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, ea1 * 1.2f)).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(bx, by, bz);
            consumer.addVertex(matrix, sx1 + bx * c1, sy1 + by * c1, sz1 + bz * c1).setColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, ea1 * 1.2f)).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(bx, by, bz);
            consumer.addVertex(matrix, sx2 + bx * c2, sy2 + by * c2, sz2 + bz * c2).setColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, ea2 * 1.2f)).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(bx, by, bz);
            consumer.addVertex(matrix, sx2 - bx * c2, sy2 - by * c2, sz2 - bz * c2).setColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, ea2 * 1.2f)).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(bx, by, bz);
        }
    }

    private static void drawGlassPrismShard(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float rotDeg, float r, float g, float b, float a) {
        float rotRad = (float) Math.toRadians(rotDeg);
        float cosR = (float) Math.cos(rotRad);
        float sinR = (float) Math.sin(rotRad);

        float l = size * 1.6f;
        float w = size * 0.7f;
        float thickness = size * 0.35f;

        // 3 Corner points of front triangle
        float p1x = cx - sinR * w;
        float p1y = cy + cosR * w;
        float p1z = cz - thickness;

        float p2x = cx + sinR * w;
        float p2y = cy - cosR * w;
        float p2z = cz - thickness;

        float p3x = cx + cosR * l;
        float p3y = cy + sinR * l;
        float p3z = cz + thickness;

        // Front face
        consumer.addVertex(matrix, p1x, p1y, p1z).setColor(r, g, b, a * 0.85f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, p2x, p2y, p2z).setColor(r, g, b, a * 0.85f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, p3x, p3y, p3z).setColor(1.0f, 1.0f, 1.0f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, p3x, p3y, p3z).setColor(1.0f, 1.0f, 1.0f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);

        // Back face
        consumer.addVertex(matrix, p3x, p3y, p3z).setColor(1.0f, 1.0f, 1.0f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
        consumer.addVertex(matrix, p3x, p3y, p3z).setColor(1.0f, 1.0f, 1.0f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
        consumer.addVertex(matrix, p2x, p2y, p2z).setColor(r, g, b, a * 0.85f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
        consumer.addVertex(matrix, p1x, p1y, p1z).setColor(r, g, b, a * 0.85f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
    }

    private static void drawOctahedralCore(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float rotDeg, float r, float g, float b, float a) {
        float rotRad = (float) Math.toRadians(rotDeg);
        float h = size * 1.3f;
        float w = size * 0.8f;

        for (int i = 0; i < 4; i++) {
            double a1 = (i / 4.0) * Math.PI * 2.0 + rotRad;
            double a2 = ((i + 1) / 4.0) * Math.PI * 2.0 + rotRad;

            float x1 = cx + (float) Math.cos(a1) * w;
            float z1 = cz + (float) Math.sin(a1) * w;
            float x2 = cx + (float) Math.cos(a2) * w;
            float z2 = cz + (float) Math.sin(a2) * w;

            // Top pyramid
            consumer.addVertex(matrix, x1, cy, z1).setColor(0.6f, 0.9f, 1.0f, a * 0.8f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, cy, z2).setColor(0.6f, 0.9f, 1.0f, a * 0.8f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom pyramid
            consumer.addVertex(matrix, x2, cy, z2).setColor(0.6f, 0.9f, 1.0f, a * 0.8f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, cy, z1).setColor(0.6f, 0.9f, 1.0f, a * 0.8f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }
}
