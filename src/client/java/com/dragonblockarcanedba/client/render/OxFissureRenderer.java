package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.OxFissureEntity;
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
 * Entity Renderer for Ox King's Ground Fissure in Minecraft 26.2.
 * Renders jagged subterranean magma fracture trenches, erupting stone fissure teeth, and glowing volcanic core.
 */
public class OxFissureRenderer extends EntityRenderer<OxFissureEntity, OxFissureRenderer.OxFissureRenderState> {
    public OxFissureRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class OxFissureRenderState extends EntityRenderState {
        public float ageInTicks = 0;
        public long seed = 0;
        public float yRot = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(OxFissureEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(OxFissureEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public OxFissureRenderState createRenderState() {
        return new OxFissureRenderState();
    }

    @Override
    public void extractRenderState(OxFissureEntity entity, OxFissureRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
        state.yRot = entity.getYRot();
    }

    @Override
    public void submit(OxFissureRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float pulse = 0.75f + 0.25f * (float) Math.sin(state.ageInTicks * 0.25f);
        float life = state.ageInTicks / (float) OxFissureEntity.MAX_LIFETIME;
        float inT = Math.min(1.0f, state.ageInTicks / 4.0f);
        float inEase = 1.0f - (1.0f - inT) * (1.0f - inT) * (1.0f - inT);
        float outT = life <= 0.70f ? 0.0f : Math.min(1.0f, (life - 0.70f) / 0.30f);
        float fade = inEase * (1.0f - outT * outT * (3.0f - 2.0f * outT));

        // Align the trench with the yaw the spawner set (radially outward from
        // the slam). Every fissure used to render pointing the same way.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(com.mojang.math.Axis.YP.rotationDegrees(-state.yRot)));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // Draw jagged branching fracture trenches
            int mainSegments = 8;
            float totalLength = 3.6f;
            float[] fx = new float[mainSegments + 1];
            float[] fz = new float[mainSegments + 1];

            fx[0] = 0; fz[0] = -totalLength * 0.5f;

            for (int i = 1; i <= mainSegments; i++) {
                float progress = i / (float) mainSegments;
                fz[i] = -totalLength * 0.5f + (progress * totalLength);
                fx[i] = fx[i - 1] + (rng.nextFloat() - 0.5f) * 0.7f;
            }

            for (int i = 0; i < mainSegments; i++) {
                float x1 = fx[i], z1 = fz[i];
                float x2 = fx[i + 1], z2 = fz[i + 1];

                // Along-length gradient (redesign T2): the crack burns
                // hottest at its root near the slam and cools toward the tip.
                float segShade = 1.0f - (i / (float) mainSegments) * 0.35f;

                // Magma Trench Base
                drawTrenchSegment(matrix, buffer, x1, z1, x2, z2, 0.45f, 1.0f, 0.25f * pulse * segShade, 0.0f, 0.95f * fade);
                // Glowing Heat Core
                drawTrenchSegment(matrix, buffer, x1, z1, x2, z2, 0.18f, 1.0f, 0.90f * pulse * segShade, 0.2f, fade * (0.70f + 0.30f * segShade));
                drawTrenchSegment(matrix, buffer, x1, z1, x2, z2, 0.05f, 1.0f, 0.98f, 0.72f, fade * (0.85f + 0.15f * segShade) * (1.0f - outT));

                // Small 3D stone fissure teeth along edges
                if (i % 2 == 0) {
                    float side = (i % 4 == 0) ? 1.0f : -1.0f;
                    float toothW = 0.25f;
                    float toothH = (0.3f + rng.nextFloat() * 0.35f) * Math.min(1.0f, state.ageInTicks * 0.2f);
                    float toothX = (x1 + x2) * 0.5f + side * 0.3f;
                    float toothZ = (z1 + z2) * 0.5f;

                    drawRockTooth(matrix, buffer,
                        toothX - toothW * 0.5f, 0.05f, toothZ,
                        toothX + toothW * 0.5f, 0.05f, toothZ,
                        toothX, toothH, toothZ + side * 0.15f,
                        1.0f, 0.35f, 0.05f, 0.95f * fade,
                        0.25f, 0.20f, 0.16f, 1.0f * fade
                    );
                }
            }

            // Two jittered branch cracks splitting off the main line, so the
            // fissure reads as a radial fracture instead of one straight decal.
            for (int b = 0; b < 2; b++) {
                int fromSeg = b == 0 ? 3 : 5;
                float bx = fx[fromSeg], bz = fz[fromSeg];
                double branchAngle = (b == 0 ? -1.0 : 1.0) * (0.55 + rng.nextDouble() * 0.35);
                double dirX = Math.sin(branchAngle), dirZ = Math.cos(branchAngle);
                for (int s = 0; s < 2; s++) {
                    float segLen = 0.55f - s * 0.15f;
                    float nx2 = bx + (float) dirX * segLen + (rng.nextFloat() - 0.5f) * 0.2f;
                    float nz2 = bz + (float) dirZ * segLen;
                    float branchDie = 1.0f - s * 0.42f;
                    drawTrenchSegment(matrix, buffer, bx, bz, nx2, nz2, 0.30f * branchDie, 1.0f, 0.25f * pulse, 0.0f, 0.85f * fade * branchDie);
                    drawTrenchSegment(matrix, buffer, bx, bz, nx2, nz2, 0.12f * branchDie, 1.0f, 0.90f * pulse, 0.2f, 0.95f * fade * branchDie);
                    drawTrenchSegment(matrix, buffer, bx, bz, nx2, nz2, 0.04f * branchDie, 1.0f, 0.98f, 0.72f, 0.80f * fade * branchDie);
                    bx = nx2; bz = nz2;
                }
            }
        });

        // Additive glow halo (redesign T1): the heat core of the main trench
        // blooms so the crack reads as molten light in the ground. The point
        // path is rebuilt from the same seed, so the glow follows the crack.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);
            int mainSegments = 8;
            float totalLength = 3.6f;
            float[] fx = new float[mainSegments + 1];
            float[] fz = new float[mainSegments + 1];
            fx[0] = 0; fz[0] = -totalLength * 0.5f;
            for (int i = 1; i <= mainSegments; i++) {
                float progress = i / (float) mainSegments;
                fz[i] = -totalLength * 0.5f + (progress * totalLength);
                fx[i] = fx[i - 1] + (rng.nextFloat() - 0.5f) * 0.7f;
            }
            for (int i = 0; i < mainSegments; i++) {
                float segShade = 1.0f - (i / (float) mainSegments) * 0.35f;
                drawTrenchSegment(matrix, buffer, fx[i], fz[i], fx[i + 1], fz[i + 1], 0.20f,
                    1.0f, 0.90f * pulse * segShade, 0.2f, 0.35f * fade * (0.70f + 0.30f * segShade));
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawTrenchSegment(Matrix4f matrix, VertexConsumer consumer, float x1, float z1, float x2, float z2, float width, float r, float g, float b, float a) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * width;
        float nz = dx / len * width;

        consumer.addVertex(matrix, x1 - nx, 0.04f, z1 - nz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx, 0.04f, z1 + nz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx, 0.04f, z2 + nz).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx, 0.04f, z2 - nz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawRockTooth(Matrix4f matrix, VertexConsumer consumer,
                                      float x1, float y1, float z1,
                                      float x2, float y2, float z2,
                                      float tipX, float tipY, float tipZ,
                                      float rBase, float gBase, float bBase, float aBase,
                                      float rTip, float gTip, float bTip, float aTip) {
        float bx = (x1 + x2) * 0.5f;
        float by = (y1 + y2) * 0.5f;
        float bz = (z1 + z2) * 0.5f;
        float width = (float) Math.sqrt((x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1)) * 0.5f;
        float mx = (bx + tipX) * 0.5f + (x1 - x2) * 0.35f;
        float my = (by + tipY) * 0.62f;
        float mz = (bz + tipZ) * 0.5f + (z1 - z2) * 0.35f;
        int steps = 5;
        float px = bx;
        float py = by;
        float pz = bz;
        for (int i = 1; i <= steps; i++) {
            float t = i / (float) steps;
            float u = 1.0f - t;
            float x = u * u * bx + 2.0f * u * t * mx + t * t * tipX;
            float y = u * u * by + 2.0f * u * t * my + t * t * tipY;
            float z = u * u * bz + 2.0f * u * t * mz + t * t * tipZ;
            float w = width * (1.0f - t * 0.88f);
            float a = aBase * (1.0f - t * t * 0.55f);
            toothSpan(matrix, consumer, px, py, pz, x, y, z, w, rBase, gBase, bBase, a);
            toothSpan(matrix, consumer, px, py, pz, x, y, z, w * 0.30f, 1.0f, 0.82f, 0.22f, Math.min(1.0f, aTip * (1.0f - t * 0.4f)));
            px = x;
            py = y;
            pz = z;
        }
    }

    private static void toothSpan(Matrix4f matrix, VertexConsumer consumer,
                                  float x0, float y0, float z0,
                                  float x1, float y1, float z1,
                                  float width, float r, float g, float b, float a) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float px = -dz;
        float py = 0.0f;
        float pz = dx;
        float plen = (float) Math.sqrt(px * px + pz * pz + dy * dy);
        if (plen < 0.0001f) {
            return;
        }
        float s = width / plen;
        px *= s;
        pz *= s;
        consumer.addVertex(matrix, x0 - px, y0, z0 - pz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, 1, pz);
        consumer.addVertex(matrix, x0 + px, y0, z0 + pz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, 1, pz);
        consumer.addVertex(matrix, x1 + px, y1, z1 + pz).setColor(r, g, b, a * 0.6f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, 1, pz);
        consumer.addVertex(matrix, x1 - px, y1, z1 - pz).setColor(r, g, b, a * 0.6f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, 1, pz);
    }
}
