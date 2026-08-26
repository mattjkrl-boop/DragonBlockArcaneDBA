package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DimensionalRiftEntity;
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
 * Entity Renderer for Dimensional Rift in Minecraft 26.2.
 * Renders 3D concentric ground accretion shockwave rings, a towering vertical spatial fissure, 18 orbital crystalline void spikes, and a black hole singularity core.
 */
public class DimensionalRiftRenderer extends EntityRenderer<DimensionalRiftEntity, DimensionalRiftRenderer.DimensionalRiftRenderState> {

    public DimensionalRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DimensionalRiftRenderState extends EntityRenderState {
        public float radius = 3.0f;
        public boolean isCollapsing = false;
        public float age = 0;
    }

    @Override
    public boolean shouldRender(DimensionalRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public DimensionalRiftRenderState createRenderState() {
        return new DimensionalRiftRenderState();
    }

    @Override
    public void extractRenderState(DimensionalRiftEntity entity, DimensionalRiftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.isCollapsing = entity.isCollapsing();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(DimensionalRiftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float maxLife = (float) DimensionalRiftEntity.MAX_LIFETIME;
        float progress = Math.min(1.0f, state.age / maxLife);
        float fade = state.isCollapsing ? Math.max(0.0f, (maxLife - state.age) / 10.0f) : Math.min(1.0f, state.age / 5.0f);
        float radius = state.radius;

        float rot = state.age * (state.isCollapsing ? 25.0f : 8.0f);
        float pulse = 0.85f + 0.15f * (float) Math.sin(state.age * 0.25f);

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Concentric 3D Ground Accretion Shockwave Rings (Expanding across terrain)
            int ringSegments = 32;

            // Outer Accretion Shockwave (Primary ring at current radius)
            float outerWidth = Math.min(2.0f, radius * 0.25f);
            drawBeveledShockRing(matrix, buffer, 0, 0.05f, 0, radius, radius - outerWidth, 0.55f * pulse, ringSegments,
                0.05f, 0.0f, 0.12f, 0.0f,
                0.60f, 0.0f, 0.90f, 0.90f * fade,
                1.0f, 0.05f, 0.80f, 0.95f * fade
            );

            // Middle Accretion Shockwave (Counter-rotating at 65% radius)
            float midRadius = radius * 0.65f;
            float midWidth = Math.min(1.6f, midRadius * 0.28f);
            drawRotatingFlatRing(matrix, buffer, -rot * 1.4f, midRadius, midRadius - midWidth, 24,
                0.85f, 0.05f, 0.95f, 0.80f * fade
            );

            // Inner Accretion Compression Disk (High speed at 35% radius)
            float innerRadius = radius * 0.35f;
            float innerWidth = Math.min(1.2f, innerRadius * 0.35f);
            drawRotatingFlatRing(matrix, buffer, rot * 2.2f, innerRadius, innerRadius - innerWidth, 20,
                0.20f, 0.90f, 1.0f, 0.85f * fade
            );

            // 2. Vertical Spatial Fissure / Dimensional Eye Tear (Towering up to 8 blocks high)
            float vHeight = Math.min(8.0f, 2.5f + (radius * 0.4f)) * pulse;
            float vWidth = Math.min(1.8f, 0.4f + (radius * 0.1f));

            // Cross-Intersecting Dimensional Fissure Quads (Void core + radiant border)
            drawVerticalTearPlane(matrix, buffer, vWidth, vHeight, 0.0f, 0.05f, 0.0f, 0.10f, 0.95f * fade, 0.80f, 0.05f, 0.95f, 1.0f * fade);
            drawVerticalTearPlane(matrix, buffer, vWidth, vHeight, 60.0f, 0.05f, 0.0f, 0.10f, 0.95f * fade, 0.20f, 0.90f, 1.0f, 0.90f * fade);
            drawVerticalTearPlane(matrix, buffer, vWidth, vHeight, 120.0f, 0.05f, 0.0f, 0.10f, 0.95f * fade, 1.0f, 0.10f, 0.70f, 0.90f * fade);

            // 3. Orbital Gravitational Void Spikes / Soul Shards (18 3D crystalline spikes orbiting)
            int shardCount = 18;
            float shardOrbitRadius = radius * 0.80f;
            float shardHeight = 1.4f * pulse;

            for (int i = 0; i < shardCount; i++) {
                double shardAngle = (i / (double) shardCount) * Math.PI * 2.0 + Math.toRadians(rot * 0.8f);
                float sx = (float) Math.cos(shardAngle) * shardOrbitRadius;
                float sz = (float) Math.sin(shardAngle) * shardOrbitRadius;

                // Inward lean toward singularity core
                float leanInX = -(float) Math.cos(shardAngle) * 0.35f;
                float leanInZ = -(float) Math.sin(shardAngle) * 0.35f;

                float shardThickness = 0.22f;
                float perpX = -(float) Math.sin(shardAngle) * shardThickness;
                float perpZ = (float) Math.cos(shardAngle) * shardThickness;

                draw3DShardPyramid(matrix, buffer,
                    sx - perpX, 0.06f, sz - perpZ,
                    sx + perpX, 0.06f, sz + perpZ,
                    sx + leanInX, 0.06f + shardHeight, sz + leanInZ,
                    0.50f, 0.0f, 0.85f, 0.85f * fade,
                    0.95f, 0.2f, 1.0f, 0.95f * fade
                );
            }

            // 4. Central Singularity Black Hole Core (Pulsating 3D nested polyhedrons)
            float coreSize = Math.max(0.6f, 1.2f * pulse);
            drawSingularityOctahedron(matrix, buffer, coreSize, 0.02f, 0.0f, 0.05f, 0.98f * fade);
            drawSingularityOctahedron(matrix, buffer, coreSize * 0.55f, 0.95f, 0.90f, 1.0f, 1.0f * fade);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawBeveledShockRing(Matrix4f matrix, VertexConsumer consumer,
                                             float cx, float cy, float cz,
                                             float rOuter, float rInner, float height,
                                             int segments,
                                             float rBase, float gBase, float bBase, float aBase,
                                             float rMid, float gMid, float bMid, float aMid,
                                             float rTop, float gTop, float bTop, float aTop) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1Out = cx + (float) Math.cos(a1) * rOuter;
            float z1Out = cz + (float) Math.sin(a1) * rOuter;
            float x2Out = cx + (float) Math.cos(a2) * rOuter;
            float z2Out = cz + (float) Math.sin(a2) * rOuter;

            float x1In = cx + (float) Math.cos(a1) * rInner;
            float z1In = cz + (float) Math.sin(a1) * rInner;
            float x2In = cx + (float) Math.cos(a2) * rInner;
            float z2In = cz + (float) Math.sin(a2) * rInner;

            // Outer rising wall
            consumer.addVertex(matrix, x1Out, cy, z1Out).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy, z2Out).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy + height, z2Out).setColor(rTop, gTop, bTop, aTop).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1Out, cy + height, z1Out).setColor(rTop, gTop, bTop, aTop).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Inward slope connecting to inner floor
            consumer.addVertex(matrix, x1Out, cy + height, z1Out).setColor(rTop, gTop, bTop, aTop).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy + height, z2Out).setColor(rTop, gTop, bTop, aTop).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2In, cy, z2In).setColor(rMid, gMid, bMid, aMid).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1In, cy, z1In).setColor(rMid, gMid, bMid, aMid).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawRotatingFlatRing(Matrix4f matrix, VertexConsumer consumer, float rotDeg, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
        double radOffset = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0 + radOffset;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0 + radOffset;

            float x1Out = (float) Math.cos(a1) * rOuter;
            float z1Out = (float) Math.sin(a1) * rOuter;
            float x2Out = (float) Math.cos(a2) * rOuter;
            float z2Out = (float) Math.sin(a2) * rOuter;

            float x1In = (float) Math.cos(a1) * rInner;
            float z1In = (float) Math.sin(a1) * rInner;
            float x2In = (float) Math.cos(a2) * rInner;
            float z2In = (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, x1In, 0.08f, z1In).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2In, 0.08f, z2In).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, 0.08f, z2Out).setColor(r, g, b, a * 0.4f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1Out, 0.08f, z1Out).setColor(r, g, b, a * 0.4f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawVerticalTearPlane(Matrix4f matrix, VertexConsumer consumer, float halfWidth, float height, float angleDeg, float rCore, float gCore, float bCore, float aCore, float rEdge, float gEdge, float bEdge, float aEdge) {
        double rad = Math.toRadians(angleDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float x1 = -halfWidth * cos;
        float z1 = -halfWidth * sin;
        float x2 = halfWidth * cos;
        float z2 = halfWidth * sin;

        // Bottom to Top diamond tear
        consumer.addVertex(matrix, x1, 0.1f, z1).setColor(rEdge, gEdge, bEdge, aEdge).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, 0.1f, z2).setColor(rEdge, gEdge, bEdge, aEdge).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, height, 0).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, height, 0).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Reverse face
        consumer.addVertex(matrix, 0, height, 0).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, 0, height, 0).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, 0.1f, z2).setColor(rEdge, gEdge, bEdge, aEdge).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, 0.1f, z1).setColor(rEdge, gEdge, bEdge, aEdge).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }

    private static void draw3DShardPyramid(Matrix4f matrix, VertexConsumer consumer,
                                          float x1, float y1, float z1,
                                          float x2, float y2, float z2,
                                          float tipX, float tipY, float tipZ,
                                          float rBase, float gBase, float bBase, float aBase,
                                          float rTip, float gTip, float bTip, float aTip) {
        // Front Face
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Back Face
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }

    private static void drawSingularityOctahedron(Matrix4f matrix, VertexConsumer consumer, float s, float r, float g, float b, float a) {
        float[][] corners = { {s, 0, 0}, {0, 0, s}, {-s, 0, 0}, {0, 0, -s} };
        for (int i = 0; i < 4; i++) {
            int nxt = (i + 1) % 4;

            // Top pyramid
            consumer.addVertex(matrix, corners[i][0], 0.5f, corners[i][2]).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, corners[nxt][0], 0.5f, corners[nxt][2]).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, 0.5f + s * 1.25f, 0).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, 0.5f + s * 1.25f, 0).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom pyramid
            consumer.addVertex(matrix, corners[i][0], 0.5f, corners[i][2]).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, corners[nxt][0], 0.5f, corners[nxt][2]).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, 0, 0.5f - s * 1.25f, 0).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, 0, 0.5f - s * 1.25f, 0).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }
}
