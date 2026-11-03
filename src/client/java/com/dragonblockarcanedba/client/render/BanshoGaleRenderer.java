package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.BanshoGaleEntity;
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
 * An opening folding fan of wind. Curved ribs, a pale leading arc, and a
 * short wake behind that arc. The bansho fan's own swing.
 */
public class BanshoGaleRenderer extends EntityRenderer<BanshoGaleEntity, BanshoGaleRenderer.GaleState> {
    public BanshoGaleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class GaleState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public float age;
    }

    public boolean shouldRender(BanshoGaleEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(BanshoGaleEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public GaleState createRenderState() {
        return new GaleState();
    }

    @Override
    public void extractRenderState(BanshoGaleEntity entity, GaleState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTick;
    }

    @Override
    public void submit(GaleState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // Opens over 6 ticks with an ease-out, then the tips dissolve across the last 4.
        float openT = Math.min(1.0f, state.age / 6.0f);
        float open = 1.0f - (1.0f - openT) * (1.0f - openT) * (1.0f - openT);
        float follow = openT >= 1.0f ? Math.min(1.0f, (state.age - 6.0f) / 4.0f) : 0.0f;
        float exitT = follow;
        float fade = 1.0f - exitT * exitT * (3.0f - 2.0f * exitT);
        float tipDie = follow * follow;
        float spread = (0.20f + open * 1.05f) * (1.0f + follow * 0.06f);
        float lift = 0.10f * open * (1.0f - follow * 0.45f);
        RenderType type = KiRenderHelper.kiRenderType();
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            int ribs = 9;
            float half = (ribs - 1) * 0.5f;
            float len = 2.85f;
            float near = 0.14f;
            for (int i = 0; i < ribs; i++) {
                drawRib(buffer, matrix, pose, i - half, spread, open, len, near, lift, fade, tipDie);
            }
            // Pale jade leading edge of the paper, then the fold behind it, then a shorter wake.
            drawArc(buffer, matrix, pose, 0.98f, half, 28, 0.020f, spread, open, len, near, lift,
                0.90f, 1.0f, 0.96f, fade * (1.0f - tipDie));
            drawArc(buffer, matrix, pose, 0.62f, half, 22, 0.012f, spread, open, len * 0.96f, near, lift,
                0.12f, 0.72f, 0.40f, fade * 0.48f);
            drawArc(buffer, matrix, pose, 0.90f, half, 22, 0.010f, spread, open, len * 0.74f, near, lift * 0.55f,
                0.05f, 0.50f, 0.26f, fade * 0.26f * (1.0f - tipDie * 0.4f));
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawRib(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                                float side, float spread, float open, float len, float near, float lift,
                                float fade, float tipDie) {
        int steps = 8;
        float[] p0 = new float[3];
        float[] p1 = new float[3];
        for (int s = 0; s < steps; s++) {
            float t0 = s / (float) steps;
            float t1 = (s + 1) / (float) steps;
            fanPoint(side, t0, spread, open, len, near, lift, p0);
            fanPoint(side, t1, spread, open, len, near, lift, p1);
            float w0 = 0.014f + 0.020f * (float) Math.sin(t0 * Math.PI);
            float w1 = 0.014f + 0.020f * (float) Math.sin(t1 * Math.PI);
            float a0 = fade * (1.0f - tipDie * t0 * t0);
            float a1 = fade * (1.0f - tipDie * t1 * t1);
            float r0 = 0.05f + t0 * 0.83f;
            float g0 = 0.42f + t0 * 0.58f;
            float b0 = 0.20f + t0 * 0.74f;
            float r1 = 0.05f + t1 * 0.83f;
            float g1 = 0.42f + t1 * 0.58f;
            float b1 = 0.20f + t1 * 0.74f;
            drawRibbon(buffer, matrix, pose, p0[0], p0[1], p0[2], p1[0], p1[1], p1[2], w0, w1, r0, g0, b0, a0, r1, g1, b1, a1);
            drawRibbon(buffer, matrix, pose,
                p0[0], p0[1] + 0.012f, p0[2], p1[0], p1[1] + 0.012f, p1[2],
                w0 * 0.38f, w1 * 0.38f,
                0.62f + t0 * 0.38f, 0.94f, 0.78f + t0 * 0.18f, a0,
                0.62f + t1 * 0.38f, 0.94f, 0.78f + t1 * 0.18f, a1);
        }
    }

    private static void drawArc(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                                float t, float half, int steps, float width,
                                float spread, float open, float len, float near, float lift,
                                float r, float g, float b, float a) {
        float[] p0 = new float[3];
        float[] p1 = new float[3];
        for (int s = 0; s < steps; s++) {
            float u0 = s / (float) steps;
            float u1 = (s + 1) / (float) steps;
            float side0 = -half + u0 * (half * 2.0f);
            float side1 = -half + u1 * (half * 2.0f);
            fanPoint(side0, t, spread, open, len, near, lift, p0);
            fanPoint(side1, t, spread, open, len, near, lift, p1);
            drawRibbon(buffer, matrix, pose, p0[0], p0[1], p0[2], p1[0], p1[1], p1[2], width, width, r, g, b, a, r, g, b, a);
        }
    }

    private static void fanPoint(float side, float t, float spread, float open, float len, float near, float lift, float[] o) {
        float base = side * spread * 0.30f;
        float curl = side * 0.065f * open;
        float ang = base + curl * t * t;
        float along = t * (0.88f + 0.12f * (float) Math.sin(t * (float) Math.PI));
        float rad = near + (len - near) * along;
        float bowMag = (float) Math.sin(t * Math.PI) * 0.22f * open;
        float bowSign = side < 0.0f ? -1.0f : (side > 0.0f ? 1.0f : 0.0f);
        float bow = bowMag * bowSign * Math.min(1.0f, Math.abs(side) / 3.2f);
        o[0] = (float) (Math.sin(ang) * rad + Math.cos(ang) * bow);
        o[2] = (float) (Math.cos(ang) * rad - Math.sin(ang) * bow);
        o[1] = (float) Math.sin(t * Math.PI * 0.92) * (0.05f + lift) * (0.30f + 0.70f * t);
    }

    private static void drawRibbon(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                                   float x0, float y0, float z0, float x1, float y1, float z1,
                                   float w0, float w1,
                                   float r0, float g0, float b0, float a0,
                                   float r1, float g1, float b1, float a1) {
        float dx = x1 - x0;
        float dz = z1 - z0;
        float px = -dz;
        float pz = dx;
        float plen = (float) Math.sqrt(px * px + pz * pz);
        if (plen < 0.0001f) {
            px = 1.0f;
            pz = 0.0f;
            plen = 1.0f;
        }
        px /= plen;
        pz /= plen;
        float ux = px * 0.55f;
        float uy = 0.84f;
        float uz = pz * 0.55f;
        float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul;
        uy /= ul;
        uz /= ul;

        float ax = x0 - ux * w0;
        float ay = y0 - uy * w0;
        float az = z0 - uz * w0;
        float bx = x0 + ux * w0;
        float by = y0 + uy * w0;
        float bz = z0 + uz * w0;
        float cx = x1 + ux * w1;
        float cy = y1 + uy * w1;
        float cz = z1 + uz * w1;
        float dxp = x1 - ux * w1;
        float dyp = y1 - uy * w1;
        float dzp = z1 - uz * w1;

        KiRenderHelper.vertex(buffer, matrix, pose, ax, ay, az, r0, g0, b0, a0, 0, 0, ux, uy, uz);
        KiRenderHelper.vertex(buffer, matrix, pose, bx, by, bz, r0, g0, b0, a0, 1, 0, ux, uy, uz);
        KiRenderHelper.vertex(buffer, matrix, pose, cx, cy, cz, r1, g1, b1, a1, 1, 1, ux, uy, uz);
        KiRenderHelper.vertex(buffer, matrix, pose, dxp, dyp, dzp, r1, g1, b1, a1, 0, 1, ux, uy, uz);

        KiRenderHelper.vertex(buffer, matrix, pose, dxp, dyp, dzp, r1, g1, b1, a1, 0, 1, -ux, -uy, -uz);
        KiRenderHelper.vertex(buffer, matrix, pose, cx, cy, cz, r1, g1, b1, a1, 1, 1, -ux, -uy, -uz);
        KiRenderHelper.vertex(buffer, matrix, pose, bx, by, bz, r0, g0, b0, a0, 1, 0, -ux, -uy, -uz);
        KiRenderHelper.vertex(buffer, matrix, pose, ax, ay, az, r0, g0, b0, a0, 0, 0, -ux, -uy, -uz);
    }
}
