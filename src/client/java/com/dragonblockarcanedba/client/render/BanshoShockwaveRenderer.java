package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.BanshoShockwaveEntity;
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
 * Entity Renderer for Bansho Fan Shockwaves in Minecraft 26.2.
 * Handles both the directional 3D emerald conical launch compression waves (Tempest Barrage)
 * and the radial 3D emerald ground/impact shockwaves (Gale Force & projectile hits).
 */
public class BanshoShockwaveRenderer extends EntityRenderer<BanshoShockwaveEntity, BanshoShockwaveRenderer.ShockwaveRenderState> {

    public BanshoShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShockwaveRenderState extends EntityRenderState {
        public float yaw = 0;
        public float pitch = 0;
        public float maxRadius = 3.0f;
        public boolean isCone = false;
        public float age = 0;
    }

    @Override
    public boolean shouldRender(BanshoShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public ShockwaveRenderState createRenderState() {
        return new ShockwaveRenderState();
    }

    @Override
    public void extractRenderState(BanshoShockwaveEntity entity, ShockwaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = entity.getEntityYaw();
        state.pitch = entity.getEntityPitch();
        state.maxRadius = entity.getMaxRadius();
        state.isCone = entity.isCone();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(ShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = 14.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float alpha = (1.0f - (progress * progress)) * 0.95f;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        if (state.isCone) {
            // Directional Conical Launch Shockwave (Tempest Barrage)
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));

            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                Matrix4f matrix = pose.pose();

                float expansion = (float) Math.sin(progress * (Math.PI / 2.0));
                float coneLength = state.maxRadius * (0.4f + 0.8f * expansion);
                float coneRadius = state.maxRadius * (0.3f + 0.9f * expansion);

                // 1. Forward-Surging 3D Conical Compression Shockwave (Outer & Inner Shells)
                int coneSegments = 24;
                for (int i = 0; i < coneSegments; i++) {
                    double a1 = (i / (double) coneSegments) * Math.PI * 2.0;
                    double a2 = ((i + 1) / (double) coneSegments) * Math.PI * 2.0;

                    float x1 = (float) Math.cos(a1) * coneRadius;
                    float y1 = (float) Math.sin(a1) * coneRadius;
                    float x2 = (float) Math.cos(a2) * coneRadius;
                    float y2 = (float) Math.sin(a2) * coneRadius;

                    // Cone surface pointing forward along +Z
                    drawTriangle(matrix, buffer,
                        0, 0, 0.1f,
                        x1, y1, coneLength,
                        x2, y2, coneLength,
                        0.0f, 1.0f, 0.60f, alpha * 0.70f
                    );
                    drawTriangle(matrix, buffer,
                        0, 0, 0.1f,
                        x2, y2, coneLength,
                        x1, y1, coneLength,
                        0.4f, 1.0f, 0.85f, alpha * 0.85f
                    );
                }

                // 2. Dual Forward-Expanding Supersonic Vapor Rings
                float ringZ1 = coneLength * 0.6f;
                float ringR1 = coneRadius * 0.7f;
                drawRingZ(matrix, buffer, ringZ1, ringR1, ringR1 * 0.75f, 20, 0.2f, 1.0f, 0.75f, alpha * 0.80f);

                float ringZ2 = coneLength * 0.95f;
                float ringR2 = coneRadius * 1.05f;
                drawRingZ(matrix, buffer, ringZ2, ringR2, ringR2 * 0.80f, 20, 0.0f, 1.0f, 0.55f, alpha * 0.60f);

                // 3. Radiating Forward Wind Needles / Flare Spikes
                int needleCount = 10;
                for (int i = 0; i < needleCount; i++) {
                    double angle = (i / (double) needleCount) * Math.PI * 2.0;
                    float nx = (float) Math.cos(angle) * (coneRadius * 0.85f);
                    float ny = (float) Math.sin(angle) * (coneRadius * 0.85f);

                    float perpX = (float) -Math.sin(angle) * 0.12f;
                    float perpY = (float) Math.cos(angle) * 0.12f;

                    drawTriangle(matrix, buffer,
                        nx - perpX, ny - perpY, coneLength * 0.5f,
                        nx + perpX, ny + perpY, coneLength * 0.5f,
                        nx * 1.35f, ny * 1.35f, coneLength * 1.25f,
                        0.90f, 1.0f, 0.95f, alpha * 0.90f
                    );
                }

                // 4. Epicenter White-Emerald Muzzle Flare
                drawRingZ(matrix, buffer, 0.05f, 0.45f * (1.0f - progress), 0.05f, 16, 1.0f, 1.0f, 1.0f, alpha);
            });
        } else {
            // Radial Planar / Impact Shockwave (Gale Force / Wind Projectile Hit)
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                Matrix4f matrix = pose.pose();

                float expansionEase = (float) Math.sin(progress * (Math.PI / 2.0));
                float currentRadius = Math.max(0.4f, expansionEase * state.maxRadius);
                int segments = 28;

                // 1. Raised Beveled 3D Emerald Shockwave Ring
                float ringWidth = Math.min(1.2f, currentRadius * 0.4f);
                float rOuter = currentRadius;
                float rInner = Math.max(0.1f, currentRadius - ringWidth);
                float crestHeight = 0.55f * (1.0f - progress * 0.8f);

                drawBeveledShockRing(matrix, buffer, 0, 0.05f, 0, rOuter, rInner, crestHeight, segments,
                    0.0f, 1.0f, 0.55f, alpha * 0.90f,
                    0.8f, 1.0f, 0.95f, alpha * 0.95f
                );

                // 2. Secondary Inner Jade Harmonic Ring
                float rSubOuter = currentRadius * 0.60f;
                float rSubInner = Math.max(0.05f, rSubOuter - (ringWidth * 0.55f));
                drawFlatRing(matrix, buffer, 0, 0.08f, 0, rSubOuter, rSubInner, 20, 0.3f, 1.0f, 0.85f, alpha * 0.65f);

                // 3. Physical Intersecting Emerald Wind Crescent Blades (Cross Slash Slices)
                int bladeCount = 6;
                for (int b = 0; b < bladeCount; b++) {
                    double bAngle = (b / (double) bladeCount) * Math.PI + (state.age * 0.1);
                    float span = currentRadius * 0.95f;
                    float bHeight = 0.85f * (1.0f - progress);

                    float bx1 = (float) Math.cos(bAngle) * span;
                    float bz1 = (float) Math.sin(bAngle) * span;
                    float bx2 = (float) -Math.cos(bAngle) * span;
                    float bz2 = (float) -Math.sin(bAngle) * span;

                    drawQuad(matrix, buffer,
                        bx1, 0.05f, bz1,
                        bx2, 0.05f, bz2,
                        bx2 * 0.6f, bHeight, bz2 * 0.6f,
                        bx1 * 0.6f, bHeight, bz1 * 0.6f,
                        0.2f, 1.0f, 0.75f, alpha * 0.75f
                    );
                }

                // 4. Epicenter Radiating White-Emerald Core
                float coreR = Math.max(0.1f, (state.maxRadius * 0.3f) * (1.0f - progress));
                drawFlatRing(matrix, buffer, 0, 0.12f, 0, coreR, 0.02f, 16, 0.95f, 1.0f, 0.98f, alpha);
            });
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawRingZ(Matrix4f matrix, VertexConsumer consumer, float z, float rOut, float rIn, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1Out = (float) Math.cos(a1) * rOut;
            float y1Out = (float) Math.sin(a1) * rOut;
            float x2Out = (float) Math.cos(a2) * rOut;
            float y2Out = (float) Math.sin(a2) * rOut;

            float x1In = (float) Math.cos(a1) * rIn;
            float y1In = (float) Math.sin(a1) * rIn;
            float x2In = (float) Math.cos(a2) * rIn;
            float y2In = (float) Math.sin(a2) * rIn;

            drawQuad(matrix, consumer, x1Out, y1Out, z, x2Out, y2Out, z, x2In, y2In, z, x1In, y1In, z, r, g, b, a);
        }
    }

    private static void drawBeveledShockRing(Matrix4f matrix, VertexConsumer consumer,
                                            float cx, float cy, float cz,
                                            float rOuter, float rInner, float crestHeight,
                                            int segments,
                                            float rBase, float gBase, float bBase, float aBase,
                                            float rCrest, float gCrest, float bCrest, float aCrest) {
        float rMid = (rOuter + rInner) * 0.5f;

        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1Out = cx + (float) Math.cos(a1) * rOuter;
            float z1Out = cz + (float) Math.sin(a1) * rOuter;
            float x2Out = cx + (float) Math.cos(a2) * rOuter;
            float z2Out = cz + (float) Math.sin(a2) * rOuter;

            float x1Mid = cx + (float) Math.cos(a1) * rMid;
            float z1Mid = cz + (float) Math.sin(a1) * rMid;
            float x2Mid = cx + (float) Math.cos(a2) * rMid;
            float z2Mid = cz + (float) Math.sin(a2) * rMid;

            float x1In = cx + (float) Math.cos(a1) * rInner;
            float z1In = cz + (float) Math.sin(a1) * rInner;
            float x2In = cx + (float) Math.cos(a2) * rInner;
            float z2In = cz + (float) Math.sin(a2) * rInner;

            // Outer incline to crest
            consumer.addVertex(matrix, x1Out, cy, z1Out).setColor(rBase, gBase, bBase, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy, z2Out).setColor(rBase, gBase, bBase, 0.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Mid, cy + crestHeight, z2Mid).setColor(rCrest, gCrest, bCrest, aCrest).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1Mid, cy + crestHeight, z1Mid).setColor(rCrest, gCrest, bCrest, aCrest).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Inner decline from crest
            consumer.addVertex(matrix, x1Mid, cy + crestHeight, z1Mid).setColor(rCrest, gCrest, bCrest, aCrest).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Mid, cy + crestHeight, z2Mid).setColor(rCrest, gCrest, bCrest, aCrest).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2In, cy, z2In).setColor(rBase, gBase, bBase, aBase * 0.5f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1In, cy, z1In).setColor(rBase, gBase, bBase, aBase * 0.5f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawFlatRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
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

            drawQuad(matrix, consumer, x1Out, cy, z1Out, x2Out, cy, z2Out, x2In, cy, z2In, x1In, cy, z1In, r, g, b, a);
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

    private static void drawTriangle(Matrix4f matrix, VertexConsumer consumer,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float x3, float y3, float z3,
                                    float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
