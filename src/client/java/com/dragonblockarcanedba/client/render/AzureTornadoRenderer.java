package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.AzureTornadoEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * Entity Renderer for Azure Tornado in Minecraft 26.2.
 *
 * Spectacle pass: the funnel itself is the shared funnel standard,
 * LayeredRender.drawVortexBands (dense bright core, theme mid band, wispy
 * outer band, counter rotating, distance scaled), over ground suction rings,
 * orbiting wind shards, a ground anchor glow, and the crown outflow ring.
 * The old hand built funnel, ribbon strands, and a private halo pass are
 * gone: one layered system instead of three overlapping ones. Sizes match
 * the entity's physics zone exactly (radius scale x 8 giant / x 3 mini,
 * height scale x 16 / x 6); the renderer used to draw the giant at 7 and
 * the mini 6.5 high, so the visible funnel and the pull zone disagreed.
 * The fade is the shared FxEnvelope over the entity's real max lifetime.
 */
public class AzureTornadoRenderer extends EntityRenderer<AzureTornadoEntity, AzureTornadoRenderer.TornadoRenderState> {

    public AzureTornadoRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TornadoRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public boolean isGiant = false;
        public float age = 0;
        public int maxLife = 60;
        public double wx, wy, wz;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AzureTornadoEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AzureTornadoEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public TornadoRenderState createRenderState() {
        return new TornadoRenderState();
    }

    @Override
    public void extractRenderState(AzureTornadoEntity entity, TornadoRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getScale();
        state.isGiant = entity.isGiant();
        state.age = entity.tickCount + partialTicks;
        state.maxLife = entity.getMaxLifetime();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(TornadoRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float scale = state.scale;
        boolean giant = state.isGiant;
        // Same numbers as the entity's physics zone, so what you see is what pulls you.
        float baseRadius = scale * (giant ? 8.0f : 3.0f);
        float totalHeight = scale * (giant ? 16.0f : 6.0f);
        float age = state.age;

        float fade = FxEnvelope.envelope(age, Math.max(1.0f, state.maxLife));
        float ds = LayeredRender.distScale(cameraState, state.wx, state.wy, state.wz);

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Ground anchor glow at the funnel base.
            LayeredRender.drawAnchorGlow(pose, buffer, baseRadius * 0.85f, 0.06f,
                0.20f, 0.80f, 1.0f, 0.32f * fade, ds);

            // 2. Ground Whirlpool Suction Discs (Base vortex)
            int groundSegments = FxEnvelope.segments(32, ds, 12);
            float gRadius = baseRadius * 0.9f;
            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, gRadius, gRadius * 0.6f, groundSegments, age * 35.0f, 0.0f, 0.9f, 1.0f, (giant ? 0.70f : 0.50f) * fade);
            drawRotatingRing(matrix, buffer, 0, 0.10f, 0, gRadius * 0.75f, gRadius * 0.3f, groundSegments, -age * 45.0f, 0.2f, 1.0f, 0.9f, (giant ? 0.60f : 0.40f) * fade);

            // 3. The funnel: three counter rotating layered bands. The mid
            // band sits at 0.7 of the physics radius so the wispy outer band
            // (1.45x) lands right at the edge of the pull zone.
            LayeredRender.drawVortexBands(pose, buffer, baseRadius * 0.7f, totalHeight, age,
                0.25f, 0.80f, 1.0f, fade, ds);

            // 4. Physical Orbiting Wind Blades / Kinetic Shards
            int shardCount = giant ? 12 : 6;
            for (int i = 0; i < shardCount; i++) {
                float shardProgress = (i / (float) shardCount);
                float shardY = shardProgress * totalHeight * 0.9f + 0.5f;
                float shardRadius = baseRadius * (0.4f + 0.6f * (shardY / totalHeight));

                double shardAngle = (shardProgress * Math.PI * 2.0) + (age * (25.0f + i * 4.0f) * (Math.PI / 180.0));

                float sx = (float) Math.cos(shardAngle) * shardRadius;
                float sz = (float) Math.sin(shardAngle) * shardRadius;

                // Tangent orientation
                float tx = (float) -Math.sin(shardAngle);
                float tz = (float) Math.cos(shardAngle);

                float sLen = 0.65f * scale;
                float sWidth = 0.18f * scale;

                float tipX = sx + tx * (sLen * 0.6f);
                float tipZ = sz + tz * (sLen * 0.6f);
                float tailX = sx - tx * (sLen * 0.4f);
                float tailZ = sz - tz * (sLen * 0.4f);

                float nx = -tz * sWidth;
                float nz = tx * sWidth;

                drawTriangle(matrix, buffer,
                    tailX - nx, shardY, tailZ - nz,
                    tailX + nx, shardY, tailZ + nz,
                    tipX, shardY + 0.1f * scale, tipZ,
                    0.4f, 1.0f, 0.95f, (giant ? 0.85f : 0.65f) * fade
                );
            }

            // 5. Crown Outflow Ring (Flared upper storm rim)
            float crownY = totalHeight;
            float crownR = baseRadius * 1.15f;
            drawRotatingRing(matrix, buffer, 0, crownY, 0, crownR, crownR * 0.75f,
                FxEnvelope.segments(28, ds, 10), -age * 22.0f, 0.0f, 0.8f, 1.0f, (giant ? 0.60f : 0.40f) * fade);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawRotatingRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        double rotRad = rotDeg * (Math.PI / 180.0);
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2.0) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2.0) + rotRad;

            float x1 = cx + (float) Math.cos(a1) * rOuter;
            float z1 = cz + (float) Math.sin(a1) * rOuter;
            float x2 = cx + (float) Math.cos(a2) * rOuter;
            float z2 = cz + (float) Math.sin(a2) * rOuter;

            float ix1 = cx + (float) Math.cos(a1) * rInner;
            float iz1 = cz + (float) Math.sin(a1) * rInner;
            float ix2 = cx + (float) Math.cos(a2) * rInner;
            float iz2 = cz + (float) Math.sin(a2) * rInner;

            drawQuad(matrix, consumer, ix1, cy, iz1, ix2, cy, iz2, x2, cy, z2, x1, cy, z1, r, g, b, a);
        }
    }

    private static void drawTriangle(Matrix4f matrix, VertexConsumer consumer,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float x3, float y3, float z3,
                                     float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Reverse
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
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

        // Reverse
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
