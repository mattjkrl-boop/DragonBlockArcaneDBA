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
 * Renders a physical 3D multi-layered inverted event horizon core, dual counter-rotating 3D beveled accretion disks, 14 orbital gravitational void shards, vertical 4-way spatial eye tears, and dynamic implosion collapse shockwaves.
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
    public boolean shouldRender(VoidRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
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
        boolean imploding = state.isImploding;

        float rot = state.age * (imploding ? 40.0f : 12.0f);
        float pulseSpeed = imploding ? 0.9f : 0.28f;
        float pulse = 0.88f + 0.12f * (float) Math.sin(state.age * pulseSpeed);
        float collapseScale = imploding ? Math.max(0.2f, 1.0f - (state.age % 10.0f) * 0.08f) : 1.0f;

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Central 3D Inverted Singularity Core (Nested Polyhedrons)
            float coreSize = radius * (imploding ? 0.65f : 0.45f) * pulse * collapseScale;
            // Pitch-Black Event Horizon
            drawSingularityOctahedron(matrix, buffer, coreSize, 0.02f, 0.0f, 0.04f, 0.99f);
            // Glowing Radiant Cyan/Violet Singularity Shell
            drawSingularityOctahedron(matrix, buffer, coreSize * 0.70f, 0.65f, 0.05f, 0.95f, 0.95f);
            // Pristine White Singularity Eye
            drawSingularityOctahedron(matrix, buffer, coreSize * 0.35f, 1.0f, 1.0f, 1.0f, 1.0f);

            // 2. Primary 3D Beveled Horizontal Accretion Disk (Spins Clockwise)
            int diskSegments = 32;
            float rOut1 = radius * collapseScale;
            float rIn1 = radius * 0.45f * collapseScale;
            float diskHeight = 0.18f * pulse;

            draw3DBeveledDisk(matrix, buffer, 0, 0, 0, rOut1, rIn1, diskHeight, diskSegments, rot,
                0.40f, 0.02f, 0.70f, 0.85f,
                0.85f, 0.10f, 1.0f, 0.95f,
                0.15f, 0.90f, 1.0f, 0.90f
            );

            // 3. Secondary 3D Inclined Accretion Disk (Tilted at 28°, Spins Counter-Clockwise)
            float rOut2 = radius * 0.82f * collapseScale;
            float rIn2 = radius * 0.38f * collapseScale;
            drawInclinedDisk(matrix, buffer, 28.0f, -rot * 1.4f, rOut2, rIn2, 24,
                0.15f, 0.90f, 1.0f, 0.80f,
                0.80f, 0.10f, 0.95f, 0.90f
            );

            // 4. Orbital Gravitational Void Spikes (14 3D crystalline void shards orbiting & leaning inward)
            int shardCount = 14;
            float orbitR = radius * 0.82f * collapseScale;
            float shardHeight = 1.1f * pulse;

            for (int i = 0; i < shardCount; i++) {
                double shardAngle = (i / (double) shardCount) * Math.PI * 2.0 + Math.toRadians(rot * 0.75f);
                float sx = (float) Math.cos(shardAngle) * orbitR;
                float sz = (float) Math.sin(shardAngle) * orbitR;

                float leanX = -(float) Math.cos(shardAngle) * (0.35f * pulse);
                float leanZ = -(float) Math.sin(shardAngle) * (0.35f * pulse);

                float perpX = -(float) Math.sin(shardAngle) * 0.16f;
                float perpZ = (float) Math.cos(shardAngle) * 0.16f;

                draw3DShardPyramid(matrix, buffer,
                    sx - perpX, 0, sz - perpZ,
                    sx + perpX, 0, sz + perpZ,
                    sx + leanX, shardHeight * (i % 2 == 0 ? 1.0f : -1.0f), sz + leanZ,
                    0.30f, 0.02f, 0.60f, 0.85f,
                    0.15f, 0.95f, 1.0f, 0.98f
                );
            }

            // 5. Vertical 4-Way Cross-Planar Dimensional Eye Fissures (Towering spatial tear quads)
            float vHeight = radius * 1.6f * pulse;
            float vWidth = radius * 0.38f * collapseScale;
            drawVerticalTearPlane(matrix, buffer, vWidth, vHeight, 0.0f, 0.05f, 0.0f, 0.10f, 0.95f, 0.75f, 0.05f, 0.95f, 0.95f);
            drawVerticalTearPlane(matrix, buffer, vWidth, vHeight, 45.0f, 0.05f, 0.0f, 0.10f, 0.90f, 0.15f, 0.90f, 1.0f, 0.90f);
            drawVerticalTearPlane(matrix, buffer, vWidth, vHeight, 90.0f, 0.05f, 0.0f, 0.10f, 0.95f, 0.75f, 0.05f, 0.95f, 0.95f);
            drawVerticalTearPlane(matrix, buffer, vWidth, vHeight, 135.0f, 0.05f, 0.0f, 0.10f, 0.90f, 0.15f, 0.90f, 1.0f, 0.90f);

            // 6. Dynamic Implosion Detonation Shockwave Rings (Expanding outward when collapsing)
            if (imploding) {
                float burstR = radius * (1.2f + (state.age % 10.0f) * 0.35f);
                float burstAlpha = Math.max(0.0f, 1.0f - (state.age % 10.0f) * 0.10f);
                drawFlatRing(matrix, buffer, rot * 3.0f, burstR, burstR * 0.85f, 28, 0.95f, 0.20f, 1.0f, burstAlpha * 0.95f);
                drawFlatRing(matrix, buffer, -rot * 2.5f, burstR * 0.70f, burstR * 0.55f, 24, 0.15f, 0.95f, 1.0f, burstAlpha * 0.85f);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void draw3DBeveledDisk(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz,
                                          float rOuter, float rInner, float halfHeight, int segments, float rotDeg,
                                          float rBase, float gBase, float bBase, float aBase,
                                          float rMid, float gMid, float bMid, float aMid,
                                          float rEdge, float gEdge, float bEdge, float aEdge) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2.0) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2.0) + rotRad;

            float x1Out = cx + (float) Math.cos(a1) * rOuter;
            float z1Out = cz + (float) Math.sin(a1) * rOuter;
            float x2Out = cx + (float) Math.cos(a2) * rOuter;
            float z2Out = cz + (float) Math.sin(a2) * rOuter;

            float x1In = cx + (float) Math.cos(a1) * rInner;
            float z1In = cz + (float) Math.sin(a1) * rInner;
            float x2In = cx + (float) Math.cos(a2) * rInner;
            float z2In = cz + (float) Math.sin(a2) * rInner;

            // Top Bevel Face
            drawQuad(matrix, consumer,
                x1In, cy + halfHeight * 0.5f, z1In,
                x2In, cy + halfHeight * 0.5f, z2In,
                x2Out, cy, z2Out,
                x1Out, cy, z1Out,
                rMid, gMid, bMid, aMid
            );

            // Bottom Bevel Face
            drawQuad(matrix, consumer,
                x1Out, cy, z1Out,
                x2Out, cy, z2Out,
                x2In, cy - halfHeight * 0.5f, z2In,
                x1In, cy - halfHeight * 0.5f, z1In,
                rBase, gBase, bBase, aBase
            );

            // Outer Rim
            drawQuad(matrix, consumer,
                x1Out, cy - halfHeight * 0.2f, z1Out,
                x2Out, cy - halfHeight * 0.2f, z2Out,
                x2Out, cy + halfHeight * 0.2f, z2Out,
                x1Out, cy + halfHeight * 0.2f, z1Out,
                rEdge, gEdge, bEdge, aEdge
            );
        }
    }

    private static void drawInclinedDisk(Matrix4f matrix, VertexConsumer consumer, float inclineDeg, float rotDeg,
                                         float rOuter, float rInner, int segments,
                                         float r1, float g1, float b1, float a1,
                                         float r2, float g2, float b2, float a2) {
        double incRad = Math.toRadians(inclineDeg);
        float cosInc = (float) Math.cos(incRad);
        float sinInc = (float) Math.sin(incRad);
        double rotRad = Math.toRadians(rotDeg);

        for (int i = 0; i < segments; i++) {
            double aStart = ((i / (double) segments) * Math.PI * 2.0) + rotRad;
            double aEnd = (((i + 1) / (double) segments) * Math.PI * 2.0) + rotRad;

            float x1Out = (float) Math.cos(aStart) * rOuter;
            float z1Out = (float) Math.sin(aStart) * rOuter;
            float y1Out = z1Out * sinInc;
            z1Out *= cosInc;

            float x2Out = (float) Math.cos(aEnd) * rOuter;
            float z2Out = (float) Math.sin(aEnd) * rOuter;
            float y2Out = z2Out * sinInc;
            z2Out *= cosInc;

            float x1In = (float) Math.cos(aStart) * rInner;
            float z1In = (float) Math.sin(aStart) * rInner;
            float y1In = z1In * sinInc;
            z1In *= cosInc;

            float x2In = (float) Math.cos(aEnd) * rInner;
            float z2In = (float) Math.sin(aEnd) * rInner;
            float y2In = z2In * sinInc;
            z2In *= cosInc;

            drawQuad(matrix, consumer,
                x1In, y1In, z1In,
                x2In, y2In, z2In,
                x2Out, y2Out, z2Out,
                x1Out, y1Out, z1Out,
                r1, g1, b1, a1
            );
        }
    }

    private static void drawFlatRing(Matrix4f matrix, VertexConsumer consumer, float rotDeg, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2.0) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2.0) + rotRad;

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

    private static void drawVerticalTearPlane(Matrix4f matrix, VertexConsumer consumer, float halfWidth, float height, float angleDeg,
                                              float rCore, float gCore, float bCore, float aCore,
                                              float rEdge, float gEdge, float bEdge, float aEdge) {
        double rad = Math.toRadians(angleDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float x1 = -halfWidth * cos;
        float z1 = -halfWidth * sin;
        float x2 = halfWidth * cos;
        float z2 = halfWidth * sin;

        // Top diamond half
        consumer.addVertex(matrix, x1, 0, z1).setColor(rEdge, gEdge, bEdge, aEdge).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, 0, z2).setColor(rEdge, gEdge, bEdge, aEdge).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, height, 0).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, height, 0).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Bottom diamond half
        consumer.addVertex(matrix, x1, 0, z1).setColor(rEdge, gEdge, bEdge, aEdge).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, 0, z2).setColor(rEdge, gEdge, bEdge, aEdge).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, 0, -height, 0).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, 0, -height, 0).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

        // Reverse Faces
        consumer.addVertex(matrix, 0, height, 0).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, 0, height, 0).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, 0, z2).setColor(rEdge, gEdge, bEdge, aEdge).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, 0, z1).setColor(rEdge, gEdge, bEdge, aEdge).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

        consumer.addVertex(matrix, 0, -height, 0).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, -height, 0).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, 0, z2).setColor(rEdge, gEdge, bEdge, aEdge).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, 0, z1).setColor(rEdge, gEdge, bEdge, aEdge).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
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

        // Reverse
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
