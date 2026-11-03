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
        public boolean orientToImpact = false;
        public float age = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BanshoShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BanshoShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
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
        state.orientToImpact = entity.isOrientToImpact();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(ShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = 14.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float exitT = progress <= 0.55f ? 0.0f : Math.min(1.0f, (progress - 0.55f) / 0.45f);
        float alpha = (1.0f - exitT * exitT * (3.0f - 2.0f * exitT)) * 0.95f;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        if (state.isCone) {
            // Directional Conical Launch Shockwave (Tempest Barrage)
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yaw)));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.pitch)));

            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                Matrix4f matrix = pose.pose();

                float expansion = (float) Math.sin(progress * (Math.PI / 2.0));
                float coneLength = state.maxRadius * (0.4f + 0.8f * expansion);
                float coneRadius = state.maxRadius * (0.3f + 0.9f * expansion);

                // 1. Forward-Surging 3D Conical Compression Shockwave (Outer & Inner Shells)
                int coneSegments = 32;
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
                    float cx1 = x1 * 0.42f;
                    float cy1 = y1 * 0.42f;
                    float cx2 = x2 * 0.42f;
                    float cy2 = y2 * 0.42f;
                    drawTriangle(matrix, buffer,
                        0, 0, 0.12f,
                        cx1, cy1, coneLength * 0.90f,
                        cx2, cy2, coneLength * 0.90f,
                        0.90f, 1.0f, 0.96f, alpha * 0.55f
                    );
                }

                // The mouth of the cone is one jade lip. The needles carry past it, then fall back as the wave resolves.
                float mouthR = coneRadius * 1.02f;
                drawRingZ(matrix, buffer, coneLength * 0.96f, mouthR, mouthR * 0.82f, 28, 0.85f, 1.0f, 0.92f, alpha * 0.90f);

                float follow = progress <= 0.40f ? 0.0f : Math.min(1.0f, (progress - 0.40f) / 0.40f);
                float needleReach = 1.05f + (1.0f - follow) * 0.35f;

                // 3. Radiating Forward Wind Needles / Flare Spikes
                int needleCount = 10;
                for (int i = 0; i < needleCount; i++) {
                    double angle = (i / (double) needleCount) * Math.PI * 2.0;
                    float nx = (float) Math.cos(angle) * (coneRadius * 0.85f);
                    float ny = (float) Math.sin(angle) * (coneRadius * 0.85f);

                    float curl = 0.16f;
                    float px = (float) -Math.sin(angle) * curl;
                    float py = (float) Math.cos(angle) * curl;
                    float tipZ = coneLength * (1.05f + (1.0f - follow) * 0.28f);
                    drawLash(matrix, buffer,
                        nx, ny, coneLength * 0.42f,
                        nx * (0.65f + needleReach * 0.28f) + px, ny * (0.65f + needleReach * 0.28f) + py, coneLength * 0.78f,
                        nx * needleReach, ny * needleReach, tipZ,
                        0.09f, 6,
                        0.12f, 0.72f, 0.42f,
                        0.95f, 1.0f, 0.96f,
                        alpha * 0.95f
                    );
                }

                // 4. Epicenter White-Emerald Muzzle Flare
                drawRingZ(matrix, buffer, 0.05f, 0.45f * (1.0f - progress), 0.05f, 24, 1.0f, 1.0f, 1.0f, alpha);
            });
        } else {
            // Radial Planar / Impact Shockwave (Gale Force / Wind Projectile Hit)
            // Projectile impacts orient the burst to the impact direction (the
            // cone mode rotation path) so hits on flying targets do not get a
            // flat ground ring floating in mid-air. Ground melee bursts keep
            // the flat orientation.
            if (state.orientToImpact) {
                poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yaw)));
                poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.pitch)));
            }
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                Matrix4f matrix = pose.pose();

                float expansionEase = (float) Math.sin(progress * (Math.PI / 2.0));
                float currentRadius = Math.max(0.4f, expansionEase * state.maxRadius);
                int segments = 40;

                // 1. Raised Beveled 3D Emerald Shockwave Ring
                float ringWidth = Math.min(1.2f, currentRadius * 0.4f);
                float rOuter = currentRadius;
                float rInner = Math.max(0.1f, currentRadius - ringWidth);
                float crestHeight = 0.55f * (1.0f - progress * 0.8f);

                drawBeveledShockRing(matrix, buffer, 0, 0.05f, 0, rOuter, rInner, crestHeight, segments,
                    0.0f, 1.0f, 0.55f, alpha * 0.90f,
                    0.8f, 1.0f, 0.95f, alpha * 0.95f
                );

                // 3. Physical Intersecting Emerald Wind Crescent Blades (Cross Slash Slices)
                int bladeCount = 6;
                for (int b = 0; b < bladeCount; b++) {
                    double bAngle = (b / (double) bladeCount) * Math.PI + (state.age * 0.1);
                    float span = currentRadius * 0.95f;
                    float bHeight = 0.85f * (1.0f - progress);

                    int arcSteps = 7;
                    float sweep = 1.05f;
                    for (int s = 0; s < arcSteps; s++) {
                        float u0 = s / (float) arcSteps;
                        float u1 = (s + 1) / (float) arcSteps;
                        double a0 = bAngle - sweep * 0.5 + sweep * u0;
                        double a1 = bAngle - sweep * 0.5 + sweep * u1;
                        float hump0 = (float) Math.sin(u0 * Math.PI);
                        float hump1 = (float) Math.sin(u1 * Math.PI);
                        float rOut0 = span * (0.78f + 0.22f * hump0);
                        float rOut1 = span * (0.78f + 0.22f * hump1);
                        float y0 = 0.05f + bHeight * hump0;
                        float y1 = 0.05f + bHeight * hump1;
                        drawQuad(matrix, buffer,
                            (float) Math.cos(a0) * rOut0 * 0.68f, y0 * 0.30f, (float) Math.sin(a0) * rOut0 * 0.68f,
                            (float) Math.cos(a1) * rOut1 * 0.68f, y1 * 0.30f, (float) Math.sin(a1) * rOut1 * 0.68f,
                            (float) Math.cos(a1) * rOut1, y1, (float) Math.sin(a1) * rOut1,
                            (float) Math.cos(a0) * rOut0, y0, (float) Math.sin(a0) * rOut0,
                            0.70f + 0.30f * hump0, 1.0f, 0.90f, alpha * (0.30f + 0.60f * hump0)
                        );
                    }
                }

                // 4. Epicenter Radiating White-Emerald Core
                float coreR = Math.max(0.1f, (state.maxRadius * 0.3f) * (1.0f - progress));
                drawFlatRing(matrix, buffer, 0, 0.12f, 0, coreR, 0.02f, 24, 0.95f, 1.0f, 0.98f, alpha);
            });
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawLash(Matrix4f matrix, VertexConsumer consumer,
                                 float x0, float y0, float z0,
                                 float xc, float yc, float zc,
                                 float x1, float y1, float z1,
                                 float width, int steps,
                                 float er, float eg, float eb,
                                 float cr, float cg, float cb,
                                 float alpha) {
        int n = Math.max(4, steps);
        float px = x0;
        float py = y0;
        float pz = z0;
        for (int i = 1; i <= n; i++) {
            float t = i / (float) n;
            float u = 1.0f - t;
            float x = u * u * x0 + 2.0f * u * t * xc + t * t * x1;
            float y = u * u * y0 + 2.0f * u * t * yc + t * t * y1;
            float z = u * u * z0 + 2.0f * u * t * zc + t * t * z1;
            float w = width * (1.0f - t * 0.9f);
            float a = alpha * (1.0f - t * t);
            lashSpan(matrix, consumer, px, py, pz, x, y, z, w, er, eg, eb, a);
            lashSpan(matrix, consumer, px, py, pz, x, y, z, w * 0.32f, cr, cg, cb, Math.min(1.0f, a * 1.2f));
            px = x;
            py = y;
            pz = z;
        }
    }

    private static void lashSpan(Matrix4f matrix, VertexConsumer consumer,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float width,
                                 float r, float g, float b, float a) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float ax = 0.0f;
        float ay = 1.0f;
        float az = 0.0f;
        if (dx * dx + dz * dz < dy * dy) {
            ax = 1.0f;
            ay = 0.0f;
            az = 0.0f;
        }
        float px = dy * az - dz * ay;
        float py = dz * ax - dx * az;
        float pz = dx * ay - dy * ax;
        float plen = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (plen < 0.0001f) {
            return;
        }
        float s = width / plen;
        px *= s;
        py *= s;
        pz *= s;
        consumer.addVertex(matrix, x0 - px, y0 - py, z0 - pz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x0 + px, y0 + py, z0 + pz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 + px, y1 + py, z1 + pz).setColor(r, g, b, a * 0.65f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 - px, y1 - py, z1 - pz).setColor(r, g, b, a * 0.65f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 - px, y1 - py, z1 - pz).setColor(r, g, b, a * 0.65f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x1 + px, y1 + py, z1 + pz).setColor(r, g, b, a * 0.65f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x0 + px, y0 + py, z0 + pz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x0 - px, y0 - py, z0 - pz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
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
