package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.LavaCleaveEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
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

/** Traveling lava crescent, drips under the lip, and a trail that burns out. */
public class LavaCleaveRenderer extends EntityRenderer<LavaCleaveEntity, LavaCleaveRenderer.CleaveState> {
    public LavaCleaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CleaveState extends EntityRenderState {
        public float age;
        public float yRot;
        public float xRot;
        public float life;
        public double posX;
        public double posY;
        public double posZ;
    }

    public boolean shouldRender(LavaCleaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(LavaCleaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public CleaveState createRenderState() {
        return new CleaveState();
    }

    @Override
    public void extractRenderState(LavaCleaveEntity entity, CleaveState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.life = entity.life();
        state.posX = entity.getX();
        state.posY = entity.getY();
        state.posZ = entity.getZ();
    }

    @Override
    public void submit(CleaveState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // The shared envelope drives the alpha (standard point 5): the
        // crescent builds as it opens and burns down over its last ticks
        // instead of starting at full alpha. Distance steps segments down.
        float scale = LayeredRender.distScale(cameraState, state.posX, state.posY, state.posZ);
        float env = FxEnvelope.envelope(state.age, state.life);
        float open = ease(Math.min(1.0f, state.age / 2.5f));
        float fade = env;
        float trail = ease(Math.max(0.0f, 1.0f - state.age / (state.life * 0.7f))) * env;
        int segs = FxEnvelope.segments(12, scale, 8);
        RenderType type = KiRenderHelper.kiRenderType();
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            // Three layers (standard point 1): the main mid crescent, a
            // smaller bright core crescent nested inside it, and the wispy
            // trailing crescent behind.
            drawCrescent(buffer, matrix, pose, 1.35f * (0.4f + 0.6f * open), 0.22f, fade, false, segs);
            drawCrescent(buffer, matrix, pose, 0.78f * (0.4f + 0.6f * open), 0.12f, fade * 0.95f, false, segs);
            drawCrescent(buffer, matrix, pose, 1.05f * trail, 0.12f, trail * 0.45f, true, segs);
            drawDrips(buffer, matrix, pose, state.age, fade);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Mth.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
    }

    private static void drawCrescent(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                                     float radius, float depth, float fade, boolean trail, int segs) {
        float z = trail ? -0.55f : 0.15f;
        for (int i = 0; i < segs; i++) {
            float t0 = i / (float) segs;
            float t1 = (i + 1) / (float) segs;
            float a0 = (float) ((-1.05 + 2.1 * t0) * Math.PI * 0.5);
            float a1 = (float) ((-1.05 + 2.1 * t1) * Math.PI * 0.5);
            float bow0 = 0.28f * (float) Math.sin(t0 * Math.PI);
            float bow1 = 0.28f * (float) Math.sin(t1 * Math.PI);
            float x0 = (float) Math.sin(a0) * radius;
            float y0 = (float) Math.cos(a0) * radius * 0.72f + bow0;
            float x1 = (float) Math.sin(a1) * radius;
            float y1 = (float) Math.cos(a1) * radius * 0.72f + bow1;
            float lip = depth * (0.45f + (float) Math.sin(t0 * Math.PI));
            float alpha = fade * (0.35f + 0.65f * (float) Math.sin(t1 * Math.PI));
            KiRenderHelper.vertex(buffer, matrix, pose, x0, y0, z - lip, 0.45f, 0.08f, 0.02f, alpha, 0, 0, 0, 0, 1);
            KiRenderHelper.vertex(buffer, matrix, pose, x0, y0, z + lip * 0.35f, 0.72f, 0.18f, 0.04f, alpha, 1, 0, 0, 0, 1);
            KiRenderHelper.vertex(buffer, matrix, pose, x1, y1, z + lip * 0.35f, 0.72f, 0.18f, 0.04f, alpha, 1, 1, 0, 0, 1);
            KiRenderHelper.vertex(buffer, matrix, pose, x1, y1, z - lip, 0.45f, 0.08f, 0.02f, alpha, 0, 1, 0, 0, 1);
            KiRenderHelper.vertex(buffer, matrix, pose, x0, y0 + 0.06f, z, 1.0f, 0.78f, 0.22f, alpha, 0, 0, 0, 1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x0, y0 - 0.04f, z, 0.35f, 0.05f, 0.01f, alpha * 0.7f, 1, 0, 0, 1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x1, y1 - 0.04f, z, 0.35f, 0.05f, 0.01f, alpha * 0.7f, 1, 1, 0, 1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x1, y1 + 0.06f, z, 1.0f, 0.78f, 0.22f, alpha, 0, 1, 0, 1, 0);
        }
    }

    private static void drawDrips(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose, float age, float fade) {
        int drops = 4;
        for (int i = 0; i < drops; i++) {
            float t = (i + 0.5f) / drops;
            float ang = (float) ((-1.0 + 2.0 * t) * Math.PI * 0.45);
            float x = (float) Math.sin(ang) * 1.05f;
            float y = (float) Math.cos(ang) * 0.7f - 0.15f - (age * 0.08f + i * 0.05f);
            float z = 0.1f;
            float drop = fade * (0.4f + 0.3f * (float) Math.sin(t * Math.PI));
            poseStackTranslate(buffer, matrix, pose, x, y, z, drop);
        }
    }

    /** A small molten bead hung under the crescent, not a second full sphere at the origin. */
    private static void poseStackTranslate(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                                           float x, float y, float z, float alpha) {
        int segs = 5;
        float r = 0.075f;
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (i * Math.PI * 2.0 / segs);
            float a1 = (float) ((i + 1) * Math.PI * 2.0 / segs);
            float x0 = x + (float) Math.cos(a0) * r;
            float z0 = z + (float) Math.sin(a0) * r;
            float x1 = x + (float) Math.cos(a1) * r;
            float z1 = z + (float) Math.sin(a1) * r;
            KiRenderHelper.vertex(buffer, matrix, pose, x0, y, z0, 1.0f, 0.45f, 0.05f, alpha, 0, 0, 0, -1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x1, y, z1, 1.0f, 0.45f, 0.05f, alpha, 1, 0, 0, -1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x, y - r * 2.2f, z, 0.55f, 0.08f, 0.02f, alpha * 0.2f, 0.5f, 1, 0, -1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x, y - r * 2.2f, z, 0.55f, 0.08f, 0.02f, 0.0f, 0.5f, 1, 0, -1, 0);
        }
    }
}
