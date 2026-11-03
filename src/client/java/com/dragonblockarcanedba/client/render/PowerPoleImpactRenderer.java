package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.PowerPoleImpactEntity;
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

import java.util.Random;

/**
 * Entity Renderer for Power Pole Impact in Minecraft 26.2.
 * Renders physical 3D kinetic impact shockwave & shatter:
 * - Concentric expanding dual shockwave disks (radiant gold & crimson)
 * - Kinetic atmospheric compression shockwave dome
 * - 16 physical 3D geometric gold/ruby shatter fragments flying outwards
 * - Detonation starburst spikes at the strike focal point
 */
public class PowerPoleImpactRenderer extends EntityRenderer<PowerPoleImpactEntity, PowerPoleImpactRenderer.ImpactRenderState> {

    public PowerPoleImpactRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ImpactRenderState extends EntityRenderState {
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float scale = 2.2f;
        public float age = 0.0f;
        public int maxLifetime = 14;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(PowerPoleImpactEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(PowerPoleImpactEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ImpactRenderState createRenderState() {
        return new ImpactRenderState();
    }

    @Override
    public void extractRenderState(PowerPoleImpactEntity entity, ImpactRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getEntityYaw();
        state.xRot = entity.getEntityPitch();
        state.scale = entity.getImpactScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(ImpactRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float age = state.age;
        float maxLife = (float) state.maxLifetime;
        float lifeT = maxLife <= 0.0f ? 1.0f : Math.min(1.0f, age / maxLife);
        float fade = 1.0f - lifeT * lifeT * (3.0f - 2.0f * lifeT);
        if (fade <= 0.01f) return;

        float scale = state.scale;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        SubmitNodeCollector.CustomGeometryRenderer geometryPlanar = (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Concentric Expanding Dual Shockwave Disks
            // Ease-out cubic expansion: the burst snaps out fast, then settles,
            // instead of the old mechanical linear ramp.
            float rawProgress = Math.min(1.0f, age / 8.0f);
            float shockProgress = 1.0f - (1.0f - rawProgress) * (1.0f - rawProgress) * (1.0f - rawProgress);
            float outerR = scale * (0.3f + shockProgress * 1.5f);
            float innerR = outerR * 0.72f;

            // Outer Golden Shockwave Ring (radial gradient: white-gold hot inner
            // edge cooling to full gold at the rim)
            drawShockRingGradient(matrix, buffer, 0, innerR, outerR, 36,
                1.0f, 0.97f, 0.75f, fade * 0.85f,
                1.0f, 0.85f, 0.15f, fade * 0.85f);
            drawShockRingGradient(matrix, buffer, 0.03f, outerR * 0.96f, outerR * 1.08f, 36,
                1.0f, 1.0f, 0.92f, fade * 0.7f,
                1.0f, 0.78f, 0.25f, fade * 0.15f);

            // Inner Crimson Compression Ring
            float crimsonR = outerR * 0.55f;
            drawShockRing(matrix, buffer, 0.05f, crimsonR * 0.6f, crimsonR, 30,
                0.90f, 0.08f, 0.12f, fade * 0.75f);

            // 4. Detonation Starburst Spikes
            float starR = scale * (0.8f * (1.0f - shockProgress * 0.4f));
            int points = 8;
            for (int p = 0; p < points; p++) {
                double ang = (p / (double) points) * Math.PI * 2.0 + (age * 0.1);
                float px = (float) Math.cos(ang) * starR;
                float py = (float) Math.sin(ang) * starR;

                drawBurstNeedle(matrix, buffer, px, py, 0.14f, 1.0f, 0.90f, 0.30f, fade * 0.85f);
            }
        };

        SubmitNodeCollector.CustomGeometryRenderer geometryVolume = (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);
            float rawProgress = Math.min(1.0f, age / 8.0f);
            float shockProgress = 1.0f - (1.0f - rawProgress) * (1.0f - rawProgress) * (1.0f - rawProgress);

            // 2. Kinetic Compression Shockwave Dome
            float domeR = scale * (0.4f + shockProgress * 1.1f);
            int domeLat = 8;
            int domeLon = 24;
            for (int lat = 0; lat < domeLat; lat++) {
                float phi1 = (lat / (float) domeLat) * (float) (Math.PI * 0.5);
                float phi2 = ((lat + 1) / (float) domeLat) * (float) (Math.PI * 0.5);

                float z1 = (float) Math.sin(phi1) * domeR * 0.5f;
                float z2 = (float) Math.sin(phi2) * domeR * 0.5f;
                float r1 = (float) Math.cos(phi1) * domeR;
                float r2 = (float) Math.cos(phi2) * domeR;

                float domeAlpha = fade * 0.35f * (1.0f - (lat / (float) domeLat) * 0.5f);

                for (int lon = 0; lon < domeLon; lon++) {
                    double theta1 = (lon / (double) domeLon) * Math.PI * 2.0;
                    double theta2 = ((lon + 1) / (double) domeLon) * Math.PI * 2.0;

                    float x1 = (float) Math.cos(theta1) * r1;
                    float y1 = (float) Math.sin(theta1) * r1;
                    float x2 = (float) Math.cos(theta2) * r1;
                    float y2 = (float) Math.sin(theta2) * r1;
                    float x3 = (float) Math.cos(theta2) * r2;
                    float y3 = (float) Math.sin(theta2) * r2;
                    float x4 = (float) Math.cos(theta1) * r2;
                    float y4 = (float) Math.sin(theta1) * r2;

                    drawQuad(matrix, buffer,
                        x1, y1, z1,
                        x2, y2, z1,
                        x3, y3, z2,
                        x4, y4, z2,
                        1.0f, 0.95f, 0.70f, domeAlpha
                    );
                }
            }

            // 3. 16 Physical 3D Geometric Shatter Fragments
            int shardCount = 16;
            for (int s = 0; s < shardCount; s++) {
                float yawS = rng.nextFloat() * (float) Math.PI * 2.0f;
                float pitchS = (rng.nextFloat() - 0.5f) * (float) Math.PI * 0.8f;
                float speed = 0.18f + rng.nextFloat() * 0.22f;

                float dirX = (float) (Math.cos(yawS) * Math.cos(pitchS));
                float dirY = (float) (Math.sin(yawS) * Math.cos(pitchS));
                float dirZ = (float) (Math.sin(pitchS) * 0.5 + 0.3f);

                float flyT = Math.min(1.0f, age / 8.0f);
                float fly = 1.0f - (1.0f - flyT) * (1.0f - flyT) * (1.0f - flyT);
                float shardDist = speed * fly * 8.0f * scale;
                float sx = dirX * shardDist;
                float sy = dirY * shardDist;
                float sz = dirZ * shardDist;

                float shardSize = (0.12f + rng.nextFloat() * 0.10f) * scale * (1.0f - (age / maxLife) * 0.3f);
                boolean isGoldShard = rng.nextBoolean();
                float rCol = isGoldShard ? 1.0f : 0.85f;
                float gCol = isGoldShard ? 0.85f : 0.08f;
                float bCol = isGoldShard ? 0.15f : 0.12f;

                // 3D Diamond Shard Geometry
                drawDiamondShard(matrix, buffer, sx, sy, sz, shardSize, rCol, gCol, bCol, fade * 0.90f);
            }
        };

        collector.submitCustomGeometry(poseStack, renderType, geometryVolume);
        collector.submitCustomGeometry(poseStack, renderType, geometryPlanar);
        // The same rings and needles, turned, so the burst has volume from the side.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotationY((float) Math.PI / 2.0f));
        collector.submitCustomGeometry(poseStack, renderType, geometryPlanar);
        poseStack.popPose();

        poseStack.popPose();
    }

    // Ring with a radial two-color gradient: inner edge uses the first color,
    // outer edge the second (the drawQuad helper only supports one flat color).
    private static void drawShockRingGradient(Matrix4f matrix, VertexConsumer buffer, float z, float innerR, float outerR, int segments,
                                              float rIn, float gIn, float bIn, float aIn,
                                              float rOut, float gOut, float bOut, float aOut) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = (float) Math.cos(a1) * innerR;
            float y1 = (float) Math.sin(a1) * innerR;
            float x2 = (float) Math.cos(a2) * innerR;
            float y2 = (float) Math.sin(a2) * innerR;

            float x3 = (float) Math.cos(a2) * outerR;
            float y3 = (float) Math.sin(a2) * outerR;
            float x4 = (float) Math.cos(a1) * outerR;
            float y4 = (float) Math.sin(a1) * outerR;

            buffer.addVertex(matrix, x1, y1, z).setColor(rIn, gIn, bIn, aIn).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            buffer.addVertex(matrix, x2, y2, z).setColor(rIn, gIn, bIn, aIn).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            buffer.addVertex(matrix, x3, y3, z).setColor(rOut, gOut, bOut, aOut).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            buffer.addVertex(matrix, x4, y4, z).setColor(rOut, gOut, bOut, aOut).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawShockRing(Matrix4f matrix, VertexConsumer buffer, float z, float innerR, float outerR, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = (float) Math.cos(a1) * innerR;
            float y1 = (float) Math.sin(a1) * innerR;
            float x2 = (float) Math.cos(a2) * innerR;
            float y2 = (float) Math.sin(a2) * innerR;

            float x3 = (float) Math.cos(a2) * outerR;
            float y3 = (float) Math.sin(a2) * outerR;
            float x4 = (float) Math.cos(a1) * outerR;
            float y4 = (float) Math.sin(a1) * outerR;

            drawQuad(matrix, buffer, x1, y1, z, x2, y2, z, x3, y3, z, x4, y4, z, r, g, b, a);
        }
    }

    private static void drawDiamondShard(Matrix4f matrix, VertexConsumer buffer, float x, float y, float z, float size, float r, float g, float b, float a) {
        float h = size * 1.55f;
        float w = size * 0.42f;
        float tipZ = z + w;
        float tailZ = z - w * 0.7f;
        drawTriangle(matrix, buffer, x - w, y, z, x, y + w * 0.7f, z, x, y, tipZ, r, g, b, a);
        drawTriangle(matrix, buffer, x, y + w * 0.7f, z, x + w, y, z, x, y, tipZ, r, g, b, a);
        drawTriangle(matrix, buffer, x + w, y, z, x, y - w * 0.7f, z, x, y, tipZ, r * 0.7f, g * 0.7f, b * 0.7f, a);
        drawTriangle(matrix, buffer, x, y - w * 0.7f, z, x - w, y, z, x, y, tipZ, r * 0.7f, g * 0.7f, b * 0.7f, a);
        drawTriangle(matrix, buffer, x - w * 0.35f, y, z, x, y, z + h * 0.15f, x, y + h, z, 1.0f, 0.97f, 0.8f, a);
        drawTriangle(matrix, buffer, x, y - w * 0.55f, z, x + w * 0.35f, y, z, x, y, tailZ, r * 0.5f, g * 0.5f, b * 0.5f, a * 0.35f);
    }

    /** Tapered ray: gold edge, white core, and a tail that dies back at the burst. */
    private static void drawBurstNeedle(Matrix4f matrix, VertexConsumer buffer, float tipX, float tipY, float tipZ, float r, float g, float b, float a) {
        float len = (float) Math.sqrt(tipX * tipX + tipY * tipY + tipZ * tipZ);
        if (len < 1.0e-4f) return;
        float dx = tipX / len;
        float dy = tipY / len;
        float dz = tipZ / len;
        float px = -dy;
        float py = dx;
        float plen = (float) Math.sqrt(px * px + py * py);
        if (plen < 1.0e-4f) {
            px = 1.0f;
            py = 0.0f;
            plen = 1.0f;
        }
        px /= plen;
        py /= plen;
        float qx = dy * 0.0f - dz * py;
        float qy = dz * px - dx * 0.0f;
        float qz = dx * py - dy * px;
        float base = len * 0.28f;
        float thick = len * 0.09f;
        float bx = dx * base;
        float by = dy * base;
        float bz = dz * base;
        float[] xs = { bx + px * thick, bx + qx * thick, bx - px * thick, bx - qx * thick };
        float[] ys = { by + py * thick, by + qy * thick, by - py * thick, by - qy * thick };
        float[] zs = { bz, bz + qz * thick, bz, bz - qz * thick };
        for (int i = 0; i < 4; i++) {
            int n = (i + 1) % 4;
            drawTriangle(matrix, buffer, xs[i], ys[i], zs[i], xs[n], ys[n], zs[n], tipX, tipY, tipZ, r, g, b, a);
            drawTriangle(matrix, buffer,
                bx + (xs[i] - bx) * 0.32f, by + (ys[i] - by) * 0.32f, bz + (zs[i] - bz) * 0.32f,
                bx + (xs[n] - bx) * 0.32f, by + (ys[n] - by) * 0.32f, bz + (zs[n] - bz) * 0.32f,
                dx * len * 0.88f, dy * len * 0.88f, dz * len * 0.88f,
                1.0f, 0.98f, 0.9f, a);
        }
        drawTriangle(matrix, buffer, xs[0], ys[0], zs[0], xs[2], ys[2], zs[2], 0, 0, 0, r, g, b, a * 0.2f);
    }

    private static void drawQuad(Matrix4f matrix, VertexConsumer buffer,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float r, float g, float b, float a) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }

    private static void drawTriangle(Matrix4f matrix, VertexConsumer buffer,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float x3, float y3, float z3,
                                     float r, float g, float b, float a) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }
}
