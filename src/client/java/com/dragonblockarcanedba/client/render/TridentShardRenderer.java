package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.TridentShardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Entity Renderer for Trident Shard in Minecraft 26.2.
 * Renders a high-detail physical 3D Demonic Trident Blade / Spearhead:
 * - Diamond-faceted dark obsidian/crimson blade body with razor-sharp cutting bevels.
 * - Raised glowing blood-red fuller spine and pulsating demonic rune core.
 * - Symmetrical barbed lateral mini-prongs extending from the collar.
 * - Volumetric 3D helical crimson energy wake vortex ribbons.
 */
public class TridentShardRenderer extends EntityRenderer<TridentShardEntity, TridentShardRenderer.ShardRenderState> {

    public TridentShardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShardRenderState extends EntityRenderState {
        public float ageInTicks = 0;
        public float yRot = 0;
        public float xRot = 0;
    }

    @Override
    public ShardRenderState createRenderState() {
        return new ShardRenderState();
    }

    @Override
    public void extractRenderState(TridentShardEntity entity, ShardRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.tickCount + partialTicks;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
    }

    @Override
    public void submit(ShardRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        // Orient with flight/orbit direction
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        // High-speed axial roll
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(state.ageInTicks * 22.0f)));
        // Whole blade pulse and spawn scale-in: the pulse used to touch only
        // the fuller alpha and core radius, so the blade body sat at a fixed
        // 0.85 scale for a swarm life that can last minutes and popped in at
        // full size on deploy. It now breathes as one piece and grows in over
        // the first 5 ticks.
        float bodyPulse = 0.93f + 0.07f * Mth.sin(state.ageInTicks * 0.35f);
        float spawnScale = 0.5f + 0.5f * Math.min(1.0f, state.ageInTicks / 5.0f);
        float shardScale = 0.85f * bodyPulse * spawnScale;
        poseStack.scale(shardScale, shardScale, shardScale);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            float pulse = 0.85f + 0.15f * Mth.sin(state.ageInTicks * 0.35f);

            drawBarbedHead(matrix, buffer, 0.98f, 1.0f, pulse);
            drawHelicalWake(matrix, buffer, state.ageInTicks, 1.0f);
        });

        // Bloom on the same silhouette. Head had this halo; it follows the curved head now.
        final float halo = 0.2f * bodyPulse;
        poseStack.pushPose();
        poseStack.scale(1.14f, 1.14f, 1.08f);
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            drawBarbedHead(pose.pose(), buffer, 0.98f, halo, 1.0f);
        });
        poseStack.popPose();

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /** Curved leaf: shell, hotter core, beveled edge, hooked barbs, and a faceted collar crystal. */
    private static void drawBarbedHead(Matrix4f matrix, VertexConsumer consumer, float length, float fade, float pulse) {
        int slices = 6;
        float base = -0.26f;
        for (int shell = 0; shell < 2; shell++) {
            boolean core = shell == 1;
            float widthScale = core ? 0.3f : 1.0f;
            float thickScale = core ? 0.45f : 1.0f;
            for (int i = 0; i < slices; i++) {
                float t0 = i / (float) slices;
                float t1 = (i + 1) / (float) slices;
                float z0 = base + (length - base) * t0;
                float z1 = base + (length - base) * t1;
                float w0 = headWidth(t0) * widthScale;
                float w1 = headWidth(t1) * widthScale;
                float h0 = headThick(t0) * thickScale;
                float h1 = headThick(t1) * thickScale;
                float r0, g0, b0, r1, g1, b1;
                if (core) {
                    r0 = 0.8f + 0.2f * t0; g0 = 0.08f + 0.22f * t0; b0 = 0.05f;
                    r1 = 0.8f + 0.2f * t1; g1 = 0.08f + 0.28f * t1; b1 = 0.06f;
                } else {
                    r0 = 0.18f + 0.62f * t0 * t0; g0 = 0.01f + 0.05f * t0; b0 = 0.03f;
                    r1 = 0.18f + 0.82f * t1 * t1; g1 = 0.01f + 0.14f * t1; b1 = 0.04f;
                }
                float a = fade * (core ? (0.85f + 0.15f * pulse) : 0.96f);
                headQuad(matrix, consumer, 0, h0, z0, -w0, 0, z0, -w1, 0, z1, 0, h1, z1, r0, g0, b0, a, r1, g1, b1, a);
                headQuad(matrix, consumer, 0, h0, z0, 0, h1, z1, w1, 0, z1, w0, 0, z0, r0, g0, b0, a, r1, g1, b1, a);
                headQuad(matrix, consumer, 0, -h0, z0, 0, -h1, z1, -w1, 0, z1, -w0, 0, z0, r0 * 0.7f, g0, b0, a, r1 * 0.75f, g1, b1, a);
                headQuad(matrix, consumer, 0, -h0, z0, w0, 0, z0, w1, 0, z1, 0, -h1, z1, r0 * 0.7f, g0, b0, a, r1 * 0.75f, g1, b1, a);
                if (!core) {
                    float e0 = 0.022f * (1.0f - t0);
                    float e1 = 0.022f * (1.0f - t1);
                    headQuad(matrix, consumer, -w0, 0, z0, -w0 - e0, h0 * 0.4f, z0, -w1 - e1, h1 * 0.4f, z1, -w1, 0, z1,
                        1.0f, 0.16f, 0.07f, a, 1.0f, 0.26f, 0.1f, a);
                    headQuad(matrix, consumer, w0, 0, z0, w1, 0, z1, w1 + e1, h1 * 0.4f, z1, w0 + e0, h0 * 0.4f, z0,
                        1.0f, 0.16f, 0.07f, a, 1.0f, 0.26f, 0.1f, a);
                }
            }
        }
        float zA = base + (length - base) * 0.1f;
        float zB = base + (length - base) * 0.34f;
        float zC = base + (length - base) * 0.06f;
        float shoulder = headWidth(0.12f);
        headTaper(matrix, consumer, -shoulder * 0.45f, 0, zA, -0.36f, 0.03f, zB, 0.045f, 0.02f,
            0.4f, 0.02f, 0.05f, fade, 0.9f, 0.08f, 0.08f, fade);
        headTaper(matrix, consumer, -0.36f, 0.03f, zB, -0.2f, 0.0f, zC, 0.02f, 0.0f,
            0.9f, 0.08f, 0.08f, fade, 1.0f, 0.2f, 0.08f, fade * 0.75f);
        headTaper(matrix, consumer, shoulder * 0.45f, 0, zA, 0.36f, 0.03f, zB, 0.045f, 0.02f,
            0.4f, 0.02f, 0.05f, fade, 0.9f, 0.08f, 0.08f, fade);
        headTaper(matrix, consumer, 0.36f, 0.03f, zB, 0.2f, 0.0f, zC, 0.02f, 0.0f,
            0.9f, 0.08f, 0.08f, fade, 1.0f, 0.2f, 0.08f, fade * 0.75f);
        drawCollarCrystal(matrix, consumer, base, 0.072f * pulse, fade);
    }

    private static float headWidth(float t) {
        float belly = (float) Math.sin(Math.PI * Math.min(1.0, t * 0.8 + 0.05));
        float fall = (float) Math.cos(t * Math.PI * 0.5);
        return 0.2f * belly * (0.28f + 0.72f * fall);
    }

    private static float headThick(float t) {
        return 0.068f * (1.0f - t) * (1.0f - t) + 0.01f * (1.0f - t);
    }

    private static void drawHelicalWake(Matrix4f matrix, VertexConsumer consumer, float age, float fade) {
        int steps = 10;
        float trailLength = 1.25f;
        for (int strand = 0; strand < 3; strand++) {
            boolean core = strand == 2;
            double strandOffset = strand * (Math.PI * 2.0 / 3.0);
            float radius0 = core ? 0.055f : 0.15f;
            for (int i = 0; i < steps; i++) {
                float p1 = i / (float) steps;
                float p2 = (i + 1) / (float) steps;
                float z1 = -0.22f - (p1 * trailLength);
                float z2 = -0.22f - (p2 * trailLength);
                float spread = core ? 0.15f : 0.5f;
                float r1 = radius0 * (1.0f + p1 * spread);
                float r2 = radius0 * (1.0f + p2 * spread);
                double a1 = strandOffset + age * 0.7 + (p1 * Math.PI * 2.6);
                double a2 = strandOffset + age * 0.7 + (p2 * Math.PI * 2.6);
                float x1 = (float) Math.cos(a1) * r1;
                float y1 = (float) Math.sin(a1) * r1;
                float x2 = (float) Math.cos(a2) * r2;
                float y2 = (float) Math.sin(a2) * r2;
                float tail = (1.0f - p1) * (1.0f - p1) * fade;
                float body = core ? 0.02f : 0.032f;
                wakeSpan(matrix, consumer, x1, y1, z1, x2, y2, z2, body * (1.0f - p1),
                    core ? 1.0f : 0.62f, core ? 0.2f : 0.02f, core ? 0.08f : 0.05f, tail);
                if (!core) {
                    float ox = (float) Math.cos(a1) * 0.02f;
                    float oy = (float) Math.sin(a1) * 0.02f;
                    wakeSpan(matrix, consumer, x1 + ox, y1 + oy, z1, x2 + (float) Math.cos(a2) * 0.02f, y2 + (float) Math.sin(a2) * 0.02f, z2,
                        body * 0.4f * (1.0f - p1), 1.0f, 0.18f, 0.08f, tail * 0.85f);
                }
            }
        }
    }

    private static void headPut(Matrix4f matrix, VertexConsumer consumer, float x, float y, float z, float r, float g, float b, float a) {
        consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void headQuad(Matrix4f matrix, VertexConsumer consumer,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 float x3, float y3, float z3, float x4, float y4, float z4,
                                 float r1, float g1, float b1, float a1, float r2, float g2, float b2, float a2) {
        if (a1 < 0.012f && a2 < 0.012f) return;
        headPut(matrix, consumer, x1, y1, z1, r1, g1, b1, a1);
        headPut(matrix, consumer, x2, y2, z2, r1, g1, b1, a1);
        headPut(matrix, consumer, x3, y3, z3, r2, g2, b2, a2);
        headPut(matrix, consumer, x4, y4, z4, r2, g2, b2, a2);
        headPut(matrix, consumer, x1, y1, z1, r1, g1, b1, a1);
        headPut(matrix, consumer, x4, y4, z4, r2, g2, b2, a2);
        headPut(matrix, consumer, x3, y3, z3, r2, g2, b2, a2);
        headPut(matrix, consumer, x2, y2, z2, r1, g1, b1, a1);
    }

    private static void headTaper(Matrix4f matrix, VertexConsumer consumer,
                                  float x0, float y0, float z0, float x1, float y1, float z1,
                                  float g0, float g1,
                                  float r0, float gc0, float b0, float a0, float r1, float gc1, float b1, float a1) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.0008f) return;
        float ux = dx / len, uy = dy / len, uz = dz / len;
        float hx = 0f, hy = 1f, hz = 0f;
        if (Math.abs(uy) > 0.85f) { hx = 1f; hy = 0f; }
        float sx = uy * hz - uz * hy;
        float sy = uz * hx - ux * hz;
        float sz = ux * hy - uy * hx;
        float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl < 1.0E-4f) return;
        sx /= sl; sy /= sl; sz /= sl;
        float tx = uy * sz - uz * sy;
        float ty = uz * sx - ux * sz;
        float tz = ux * sy - uy * sx;
        for (int s = 0; s < 3; s++) {
            double ang0 = (s / 3.0) * Math.PI * 2.0;
            double ang1 = ((s + 1) / 3.0) * Math.PI * 2.0;
            float c0 = (float) Math.cos(ang0), s0 = (float) Math.sin(ang0);
            float c1 = (float) Math.cos(ang1), s1 = (float) Math.sin(ang1);
            headQuad(matrix, consumer,
                x0 + (sx * c0 + tx * s0) * g0, y0 + (sy * c0 + ty * s0) * g0, z0 + (sz * c0 + tz * s0) * g0,
                x0 + (sx * c1 + tx * s1) * g0, y0 + (sy * c1 + ty * s1) * g0, z0 + (sz * c1 + tz * s1) * g0,
                x1 + (sx * c1 + tx * s1) * g1, y1 + (sy * c1 + ty * s1) * g1, z1 + (sz * c1 + tz * s1) * g1,
                x1 + (sx * c0 + tx * s0) * g1, y1 + (sy * c0 + ty * s0) * g1, z1 + (sz * c0 + tz * s0) * g1,
                r0, gc0, b0, a0, r1, gc1, b1, a1);
        }
    }

    private static void drawCollarCrystal(Matrix4f matrix, VertexConsumer consumer, float z, float s, float fade) {
        float r = 1.0f, g = 0.3f, b = 0.12f;
        float dr = 0.55f, dg = 0.04f, db = 0.06f;
        headTri(matrix, consumer, 0, s, z, s, 0, z, 0, 0, z + s, r, g, b, fade);
        headTri(matrix, consumer, 0, s, z, 0, 0, z + s, -s, 0, z, r, g, b, fade);
        headTri(matrix, consumer, 0, s, z, -s, 0, z, 0, 0, z - s * 0.55f, dr, dg, db, fade);
        headTri(matrix, consumer, 0, s, z, 0, 0, z - s * 0.55f, s, 0, z, dr, dg, db, fade);
        headTri(matrix, consumer, 0, -s, z, 0, 0, z + s, s, 0, z, dr, dg, db, fade);
        headTri(matrix, consumer, 0, -s, z, -s, 0, z, 0, 0, z + s, dr, dg, db, fade);
        headTri(matrix, consumer, 0, -s, z, 0, 0, z - s * 0.55f, -s, 0, z, dr, dg, db, fade * 0.8f);
        headTri(matrix, consumer, 0, -s, z, s, 0, z, 0, 0, z - s * 0.55f, dr, dg, db, fade * 0.8f);
    }

    private static void headTri(Matrix4f matrix, VertexConsumer consumer,
                                float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
                                float r, float g, float b, float a) {
        headPut(matrix, consumer, x1, y1, z1, r, g, b, a);
        headPut(matrix, consumer, x2, y2, z2, r, g, b, a);
        headPut(matrix, consumer, x3, y3, z3, r, g, b, a);
        headPut(matrix, consumer, x3, y3, z3, r, g, b, a);
        headPut(matrix, consumer, x1, y1, z1, r, g, b, a);
        headPut(matrix, consumer, x3, y3, z3, r, g, b, a);
        headPut(matrix, consumer, x2, y2, z2, r, g, b, a);
        headPut(matrix, consumer, x2, y2, z2, r, g, b, a);
    }

    private static void wakeSpan(Matrix4f matrix, VertexConsumer consumer,
                                 float x1, float y1, float z1, float x2, float y2, float z2, float width,
                                 float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.0008f || a < 0.012f) return;
        float nx = -dy / len * width;
        float ny = dx / len * width;
        headQuad(matrix, consumer, x1 - nx, y1 - ny, z1, x1 + nx, y1 + ny, z1, x2 + nx, y2 + ny, z2, x2 - nx, y2 - ny, z2,
            r, g, b, a, r, g, b, 0.0f);
    }

}
