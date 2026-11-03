package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.TridentProngEntity;
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
 * One shot: three barbed crimson needles in trident formation.
 * Same head as the swarm shards, smaller, with a wake that dies on impact.
 */
public class TridentProngRenderer extends EntityRenderer<TridentProngEntity, TridentProngRenderer.ProngState> {
    public TridentProngRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ProngState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public float age;
        public int dieTick;
    }

    public boolean shouldRender(TridentProngEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(TridentProngEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ProngState createRenderState() {
        return new ProngState();
    }

    @Override
    public void extractRenderState(TridentProngEntity entity, ProngState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTick;
        state.dieTick = entity.getDieTick();
    }

    @Override
    public void submit(ProngState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType type = KiRenderHelper.kiRenderType();
        float intro = Math.min(1.0f, state.age / 2.0f);
        float fade = intro * intro * (3.0f - 2.0f * intro);
        if (state.dieTick > 0) {
            float t = Math.min(1.0f, Math.max(0.0f, (state.age - state.dieTick) / 6.0f));
            float ease = t * t * (3.0f - 2.0f * t);
            fade *= 1.0f - ease;
        }
        final float needleFade = fade;
        final float spin = state.age;
        float[] offsets = { -0.26f, 0.0f, 0.26f };
        float[] lengths = { 1.25f, 1.65f, 1.25f };
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        for (int i = 0; i < 3; i++) {
            float offset = offsets[i];
            float length = lengths[i];
            poseStack.pushPose();
            poseStack.translate(offset, 0.0, 0.0);
            collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
                drawNeedle(pose.pose(), buffer, length, needleFade, spin);
            });
            poseStack.popPose();
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /** Same barbed head as the swarm, smaller, with a core, an edge, and a wake that eases out. */
    private static void drawNeedle(Matrix4f matrix, VertexConsumer buffer, float length, float fade, float age) {
        int slices = 6;
        float base = -0.1f;
        float widthMul = 0.42f;
        for (int shell = 0; shell < 2; shell++) {
            boolean core = shell == 1;
            float ws = (core ? 0.32f : 1.0f) * widthMul;
            float ts = core ? 0.45f : 1.0f;
            for (int i = 0; i < slices; i++) {
                float t0 = i / (float) slices;
                float t1 = (i + 1) / (float) slices;
                float z0 = base + (length - base) * t0;
                float z1 = base + (length - base) * t1;
                float belly0 = (float) Math.sin(Math.PI * Math.min(1.0, t0 * 0.8 + 0.05));
                float belly1 = (float) Math.sin(Math.PI * Math.min(1.0, t1 * 0.8 + 0.05));
                float fall0 = (float) Math.cos(t0 * Math.PI * 0.5);
                float fall1 = (float) Math.cos(t1 * Math.PI * 0.5);
                float w0 = 0.2f * belly0 * (0.28f + 0.72f * fall0) * ws;
                float w1 = 0.2f * belly1 * (0.28f + 0.72f * fall1) * ws;
                float h0 = (0.05f * (1.0f - t0) * (1.0f - t0) + 0.008f * (1.0f - t0)) * ts;
                float h1 = (0.05f * (1.0f - t1) * (1.0f - t1) + 0.008f * (1.0f - t1)) * ts;
                float r0 = core ? 0.82f + 0.18f * t0 : 0.2f + 0.55f * t0 * t0;
                float g0 = core ? 0.08f + 0.2f * t0 : 0.01f;
                float b0 = 0.04f;
                float r1 = core ? 0.82f + 0.18f * t1 : 0.2f + 0.8f * t1 * t1;
                float g1 = core ? 0.08f + 0.26f * t1 : 0.02f + 0.12f * t1;
                float b1 = 0.05f;
                float a = fade * (core ? 0.95f : 0.96f);
                quad(matrix, buffer, 0, h0, z0, -w0, 0, z0, -w1, 0, z1, 0, h1, z1, r0, g0, b0, a, r1, g1, b1, a);
                quad(matrix, buffer, 0, h0, z0, 0, h1, z1, w1, 0, z1, w0, 0, z0, r0, g0, b0, a, r1, g1, b1, a);
                quad(matrix, buffer, 0, -h0, z0, w0, 0, z0, w1, 0, z1, 0, -h1, z1, r0 * 0.7f, g0, b0, a, r1 * 0.75f, g1, b1, a);
                quad(matrix, buffer, 0, -h0, z0, 0, -h1, z1, -w1, 0, z1, -w0, 0, z0, r0 * 0.7f, g0, b0, a, r1 * 0.75f, g1, b1, a);
                if (!core) {
                    float e0 = 0.012f * (1.0f - t0);
                    float e1 = 0.012f * (1.0f - t1);
                    quad(matrix, buffer, -w0, 0, z0, -w0 - e0, h0 * 0.45f, z0, -w1 - e1, h1 * 0.45f, z1, -w1, 0, z1,
                        1.0f, 0.16f, 0.07f, a, 1.0f, 0.24f, 0.1f, a);
                    quad(matrix, buffer, w0, 0, z0, w1, 0, z1, w1 + e1, h1 * 0.45f, z1, w0 + e0, h0 * 0.45f, z0,
                        1.0f, 0.16f, 0.07f, a, 1.0f, 0.24f, 0.1f, a);
                }
            }
        }
        float zA = base + (length - base) * 0.12f;
        float zB = base + (length - base) * 0.36f;
        float zC = base + (length - base) * 0.08f;
        taper(matrix, buffer, -0.02f, 0, zA, -0.16f, 0.012f, zB, 0.02f, 0.008f, 0.45f, 0.02f, 0.05f, fade, 0.95f, 0.08f, 0.08f, fade);
        taper(matrix, buffer, -0.16f, 0.012f, zB, -0.07f, 0, zC, 0.008f, 0f, 0.95f, 0.08f, 0.08f, fade, 1.0f, 0.2f, 0.08f, fade * 0.7f);
        taper(matrix, buffer, 0.02f, 0, zA, 0.16f, 0.012f, zB, 0.02f, 0.008f, 0.45f, 0.02f, 0.05f, fade, 0.95f, 0.08f, 0.08f, fade);
        taper(matrix, buffer, 0.16f, 0.012f, zB, 0.07f, 0, zC, 0.008f, 0f, 0.95f, 0.08f, 0.08f, fade, 1.0f, 0.2f, 0.08f, fade * 0.7f);
        crystal(matrix, buffer, base, 0.028f, fade);
        int steps = 8;
        for (int strand = 0; strand < 3; strand++) {
            boolean core = strand == 2;
            double off = strand * (Math.PI * 2.0 / 3.0);
            float radius0 = core ? 0.02f : 0.045f;
            for (int i = 0; i < steps; i++) {
                float p1 = i / (float) steps;
                float p2 = (i + 1) / (float) steps;
                float z1 = base - p1 * 0.95f;
                float z2 = base - p2 * 0.95f;
                double a1 = off + age * 0.55 + p1 * Math.PI * 2.4;
                double a2 = off + age * 0.55 + p2 * Math.PI * 2.4;
                float rad1 = radius0 * (1.0f + p1 * 0.8f);
                float rad2 = radius0 * (1.0f + p2 * 0.8f);
                float x1 = (float) Math.cos(a1) * rad1;
                float y1 = (float) Math.sin(a1) * rad1;
                float x2 = (float) Math.cos(a2) * rad2;
                float y2 = (float) Math.sin(a2) * rad2;
                float tail = (1.0f - p1) * (1.0f - p1) * fade;
                float w = (core ? 0.012f : 0.02f) * (1.0f - p1);
                float dx = x2 - x1, dy = y2 - y1;
                float span = (float) Math.sqrt(dx * dx + dy * dy);
                if (span < 0.0008f) continue;
                float nx = -dy / span * w;
                float ny = dx / span * w;
                quad(matrix, buffer, x1 - nx, y1 - ny, z1, x1 + nx, y1 + ny, z1, x2 + nx, y2 + ny, z2, x2 - nx, y2 - ny, z2,
                    core ? 1.0f : 0.7f, core ? 0.18f : 0.03f, 0.06f, tail, core ? 1.0f : 0.7f, core ? 0.18f : 0.03f, 0.06f, 0f);
            }
        }
    }

    private static void put(Matrix4f matrix, VertexConsumer buffer, float x, float y, float z, float r, float g, float b, float a) {
        buffer.addVertex(matrix, x, y, z).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void quad(Matrix4f matrix, VertexConsumer buffer,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float r1, float g1, float b1, float a1, float r2, float g2, float b2, float a2) {
        if (a1 < 0.012f && a2 < 0.012f) return;
        put(matrix, buffer, x1, y1, z1, r1, g1, b1, a1);
        put(matrix, buffer, x2, y2, z2, r1, g1, b1, a1);
        put(matrix, buffer, x3, y3, z3, r2, g2, b2, a2);
        put(matrix, buffer, x4, y4, z4, r2, g2, b2, a2);
        put(matrix, buffer, x1, y1, z1, r1, g1, b1, a1);
        put(matrix, buffer, x4, y4, z4, r2, g2, b2, a2);
        put(matrix, buffer, x3, y3, z3, r2, g2, b2, a2);
        put(matrix, buffer, x2, y2, z2, r1, g1, b1, a1);
    }

    private static void taper(Matrix4f matrix, VertexConsumer buffer,
                              float x0, float y0, float z0, float x1, float y1, float z1, float g0, float g1,
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
            quad(matrix, buffer,
                x0 + (sx * c0 + tx * s0) * g0, y0 + (sy * c0 + ty * s0) * g0, z0 + (sz * c0 + tz * s0) * g0,
                x0 + (sx * c1 + tx * s1) * g0, y0 + (sy * c1 + ty * s1) * g0, z0 + (sz * c1 + tz * s1) * g0,
                x1 + (sx * c1 + tx * s1) * g1, y1 + (sy * c1 + ty * s1) * g1, z1 + (sz * c1 + tz * s1) * g1,
                x1 + (sx * c0 + tx * s0) * g1, y1 + (sy * c0 + ty * s0) * g1, z1 + (sz * c0 + tz * s0) * g1,
                r0, gc0, b0, a0, r1, gc1, b1, a1);
        }
    }

    private static void crystal(Matrix4f matrix, VertexConsumer buffer, float z, float s, float fade) {
        tri(matrix, buffer, 0, s, z, s, 0, z, 0, 0, z + s, 1.0f, 0.28f, 0.1f, fade);
        tri(matrix, buffer, 0, s, z, 0, 0, z + s, -s, 0, z, 1.0f, 0.28f, 0.1f, fade);
        tri(matrix, buffer, 0, s, z, -s, 0, z, 0, 0, z - s, 0.55f, 0.04f, 0.06f, fade);
        tri(matrix, buffer, 0, s, z, 0, 0, z - s, s, 0, z, 0.55f, 0.04f, 0.06f, fade);
        tri(matrix, buffer, 0, -s, z, 0, 0, z + s, s, 0, z, 0.55f, 0.04f, 0.06f, fade);
        tri(matrix, buffer, 0, -s, z, -s, 0, z, 0, 0, z + s, 0.55f, 0.04f, 0.06f, fade);
        tri(matrix, buffer, 0, -s, z, s, 0, z, 0, 0, z - s, 0.55f, 0.04f, 0.06f, fade);
        tri(matrix, buffer, 0, -s, z, 0, 0, z - s, -s, 0, z, 0.55f, 0.04f, 0.06f, fade);
    }

    private static void tri(Matrix4f matrix, VertexConsumer buffer,
                            float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
                            float r, float g, float b, float a) {
        put(matrix, buffer, x1, y1, z1, r, g, b, a);
        put(matrix, buffer, x2, y2, z2, r, g, b, a);
        put(matrix, buffer, x3, y3, z3, r, g, b, a);
        put(matrix, buffer, x3, y3, z3, r, g, b, a);
        put(matrix, buffer, x1, y1, z1, r, g, b, a);
        put(matrix, buffer, x3, y3, z3, r, g, b, a);
        put(matrix, buffer, x2, y2, z2, r, g, b, a);
        put(matrix, buffer, x2, y2, z2, r, g, b, a);
    }
}
