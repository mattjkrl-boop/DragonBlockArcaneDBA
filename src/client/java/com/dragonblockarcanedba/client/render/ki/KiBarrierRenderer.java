package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.entity.KiBarrierEntity;
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
 * A ki barrier is a curved shell around the caster: an outer surface, the same
 * surface wound inward so the wearer sees it, a softer inner glow, and a bright rim.
 * Drawn with {@link KiRenderHelper} on the shared white ki texture.
 */
public class KiBarrierRenderer extends EntityRenderer<KiBarrierEntity, KiBarrierRenderer.BarrierRenderState> {

    public KiBarrierRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BarrierRenderState createRenderState() {
        return new BarrierRenderState();
    }

    @Override
    public void extractRenderState(KiBarrierEntity entity, BarrierRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.ageInTicks = entity.tickCount + partialTicks;
        int percent = entity.getUsedPercent();
        if (percent < 1) {
            percent = 1;
        }
        if (percent > 100) {
            percent = 100;
        }
        state.usedPercent = percent;
    }

    @Override
    public void submit(BarrierRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float r = KiRenderHelper.red(state.color);
        float g = KiRenderHelper.green(state.color);
        float b = KiRenderHelper.blue(state.color);
        float glowR = r + (1.0f - r) * 0.7f;
        float glowG = g + (1.0f - g) * 0.7f;
        float glowB = b + (1.0f - b) * 0.7f;
        float age = state.ageInTicks;
        float breathe = 0.5f + 0.5f * Mth.sin(age * 0.22f);
        float radius = (1.02f + 0.28f * (state.usedPercent / 100.0f)) * (1.0f + 0.02f * breathe);
        float shellA = 0.22f + 0.06f * breathe;
        float glowA = 0.12f + 0.05f * breathe;
        float rimA = 0.82f;
        float rimInner = radius * 0.9f;
        float rimOuter = radius * 1.04f;
        float spin = age * 2.4f;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().translate(0.0f, 0.95f, 0.0f));
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(spin)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawSphere(pose, buffer, radius, 10, 16, r, g, b, shellA);
            drawInvertedSphere(pose, buffer, radius * 0.985f, 10, 16, r, g, b, shellA);
            KiRenderHelper.drawSphere(pose, buffer, radius * 0.62f, 8, 12, glowR, glowG, glowB, glowA);
            drawInvertedSphere(pose, buffer, radius * 0.6f, 8, 12, glowR, glowG, glowB, glowA);
        });
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().translate(0.0f, 0.95f, 0.0f));
        poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawDisc(pose, buffer, rimInner, rimOuter, 28, 1.0f, 1.0f, 1.0f, rimA);
        });
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().translate(0.0f, 0.95f, 0.0f));
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(spin * 1.7f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            KiRenderHelper.drawDisc(pose, buffer, rimInner, rimOuter, 28, glowR, glowG, glowB, rimA * 0.85f);
        });
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }

    /** Same latitude-longitude shell as {@link KiRenderHelper#drawSphere}, wound the other way so it is visible from inside. */
    private static void drawInvertedSphere(PoseStack.Pose pose, VertexConsumer buffer,
                                           float radius, int stacks, int slices,
                                           float r, float g, float b, float a) {
        Matrix4f mat = pose.pose();
        float nScale = radius > 0.0001f ? -1.0f / radius : 0.0f;
        for (int lat = 0; lat < stacks; lat++) {
            double v0 = Math.PI * lat / stacks;
            double v1 = Math.PI * (lat + 1) / stacks;
            float y0 = (float) Math.cos(v0) * radius;
            float y1 = (float) Math.cos(v1) * radius;
            float rad0 = (float) Math.sin(v0) * radius;
            float rad1 = (float) Math.sin(v1) * radius;
            for (int lon = 0; lon < slices; lon++) {
                double u0 = Math.PI * 2.0 * lon / slices;
                double u1 = Math.PI * 2.0 * (lon + 1) / slices;
                float x00 = (float) Math.cos(u0) * rad0;
                float z00 = (float) Math.sin(u0) * rad0;
                float x10 = (float) Math.cos(u1) * rad0;
                float z10 = (float) Math.sin(u1) * rad0;
                float x01 = (float) Math.cos(u0) * rad1;
                float z01 = (float) Math.sin(u0) * rad1;
                float x11 = (float) Math.cos(u1) * rad1;
                float z11 = (float) Math.sin(u1) * rad1;
                KiRenderHelper.vertex(buffer, mat, pose, x00, y0, z00, r, g, b, a, 0, 0, x00 * nScale, y0 * nScale, z00 * nScale);
                KiRenderHelper.vertex(buffer, mat, pose, x01, y1, z01, r, g, b, a, 0, 1, x01 * nScale, y1 * nScale, z01 * nScale);
                KiRenderHelper.vertex(buffer, mat, pose, x11, y1, z11, r, g, b, a, 1, 1, x11 * nScale, y1 * nScale, z11 * nScale);
                KiRenderHelper.vertex(buffer, mat, pose, x10, y0, z10, r, g, b, a, 1, 0, x10 * nScale, y0 * nScale, z10 * nScale);
            }
        }
    }

    public static class BarrierRenderState extends EntityRenderState {
        public int color = 0xFF55DDFF;
        public float ageInTicks;
        public int usedPercent = 50;
    }
}
