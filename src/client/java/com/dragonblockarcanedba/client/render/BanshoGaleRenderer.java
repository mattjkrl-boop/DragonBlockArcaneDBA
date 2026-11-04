package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.BanshoGaleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * An opening folding fan of wind, drawn entirely from bespoke torn ribbons
 * seeded by the entity's synced id: 9 ribs fanning out from the swing origin
 * and 3 torn arcs chorded across them (the pale leading edge, the fold, and a
 * shorter wake). The open, follow, and tip dissolve timing is unchanged from
 * the ruled version; the silhouettes are ripped now, not ruled.
 */
public class BanshoGaleRenderer extends EntityRenderer<BanshoGaleEntity, BanshoGaleRenderer.GaleState> {
    public BanshoGaleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class GaleState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public float age;
        public double ex, ey, ez;
        public long seed;
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
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L ^ 0x6A1E5EEDL;
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
        float distScale = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);
        RenderType type = KiRenderHelper.kiRenderType();
        final long seed = state.seed;

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        // The 9 ribs: one posed submit each, the pose rotated so the torn
        // ribbon's +X runs along the rib's fanned direction. A wide faint
        // ribbon carries the rib and a narrow bright one rides it as the core.
        int ribs = 9;
        float half = (ribs - 1) * 0.5f;
        float len = 2.85f;
        float near = 0.14f;
        float[] base = new float[3];
        for (int i = 0; i < ribs; i++) {
            float side = i - half;
            fanPoint(side, 0.0f, spread, open, len, near, lift, base);
            float angMid = side * spread * 0.30f + side * 0.065f * open * 0.25f;
            final long ribSeed = seed ^ (i * 81L + 3L);
            final float bend = 0.10f * open;
            poseStack.pushPose();
            poseStack.translate(base[0], base[1], base[2]);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees((float) Math.toDegrees(angMid) - 90.0f)));
            collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
                BespokeGeometry.tornRibbon(pose, buffer, ribSeed,
                    2.66f, 0.10f, bend, 0.30f, 0.88f, 0.52f, 0.85f, fade, distScale);
                BespokeGeometry.tornRibbon(pose, buffer, ribSeed ^ 7L,
                    2.66f, 0.045f, bend, 0.80f, 1.0f, 0.90f, 0.90f, fade, distScale);
            });
            poseStack.popPose();
        }

        // The 3 arcs across the ribs, each chorded in 4 torn ribbon segments
        // that follow the fanPoint curve: pale leading edge, fold, wake.
        drawTornArc(collector, poseStack, type, seed ^ 0xA11CL, 0.98f, half, 0.050f, spread, open, len, near, lift,
            0.90f, 1.0f, 0.96f, 1.0f, fade * (1.0f - tipDie), distScale);
        drawTornArc(collector, poseStack, type, seed ^ 0xA22CL, 0.62f, half, 0.035f, spread, open, len * 0.96f, near, lift,
            0.12f, 0.72f, 0.40f, 0.48f, fade, distScale);
        drawTornArc(collector, poseStack, type, seed ^ 0xA33CL, 0.90f, half, 0.030f, spread, open, len * 0.74f, near, lift * 0.55f,
            0.05f, 0.50f, 0.26f, 0.26f, fade * (1.0f - tipDie * 0.4f), distScale);

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawTornArc(SubmitNodeCollector collector, PoseStack poseStack, RenderType type,
                                    long arcSeed, float t, float half, float width,
                                    float spread, float open, float len, float near, float lift,
                                    float r, float g, float b, float alpha, float envelope,
                                    float distScale) {
        int segments = 4;
        float[] p0 = new float[3];
        float[] p1 = new float[3];
        for (int s = 0; s < segments; s++) {
            float u0 = s / (float) segments;
            float u1 = (s + 1) / (float) segments;
            float side0 = -half + u0 * (half * 2.0f);
            float side1 = -half + u1 * (half * 2.0f);
            fanPoint(side0, t, spread, open, len, near, lift, p0);
            fanPoint(side1, t, spread, open, len, near, lift, p1);
            float dx = p1[0] - p0[0];
            float dz = p1[2] - p0[2];
            float chord = (float) Math.sqrt(dx * dx + dz * dz);
            if (chord < 0.001f) continue;
            final long segSeed = arcSeed ^ (s * 29L);
            final float bend = p1[1] - p0[1];
            poseStack.pushPose();
            poseStack.translate(p0[0], p0[1], p0[2]);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees((float) Math.toDegrees(Math.atan2(-dz, dx)))));
            collector.submitCustomGeometry(poseStack, type, (pose, buffer) ->
                BespokeGeometry.tornRibbon(pose, buffer, segSeed,
                    chord, width, bend, r, g, b, alpha, envelope, distScale));
            poseStack.popPose();
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
}
