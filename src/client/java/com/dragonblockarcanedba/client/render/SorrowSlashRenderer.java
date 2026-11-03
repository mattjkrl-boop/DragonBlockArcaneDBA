package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.SorrowSlashEntity;
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
 * Entity Renderer for Sorrow Slash in Minecraft 26.2.
 * Renders a massive 3D scythe crescent slash arc with abyssal void silhouette, weeping magenta blade crest, and razor cyan cutting edge.
 */
public class SorrowSlashRenderer extends EntityRenderer<SorrowSlashEntity, SorrowSlashRenderer.SorrowSlashRenderState> {

    public SorrowSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SorrowSlashRenderState extends EntityRenderState {
        public boolean tiltRight = false;
        public boolean isImpactBurst = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SorrowSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SorrowSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SorrowSlashRenderState createRenderState() {
        return new SorrowSlashRenderState();
    }

    @Override
    public void extractRenderState(SorrowSlashEntity entity, SorrowSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tiltRight = entity.getTilt();
        state.isImpactBurst = entity.isImpactBurst();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(SorrowSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float maxLife = state.isImpactBurst ? 16.0f : 30.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        float ease = progress * progress * (3.0f - 2.0f * progress);
        float fade = 1.0f - ease;
        // The flying wave eases in. The impact burst opens hot and eases shut.
        if (!state.isImpactBurst) {
            float intro = Math.min(1.0f, state.age / 4.0f);
            fade *= intro * intro * (3.0f - 2.0f * intro);
        }
        final float slashFade = fade;

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        // Diagonal / Scythe reap tilt. The flying wave also rolls slowly in
        // its tilt direction while it travels instead of flying as a frozen
        // stamp.
        float tiltAngle = state.tiltRight ? 42.0f : -42.0f;
        if (!state.isImpactBurst) {
            // Tips into the reap and settles, instead of spinning off the sickle.
            float settle = 1.0f - (float) Math.exp(-state.age * 0.22);
            tiltAngle += (state.tiltRight ? 14.0f : -14.0f) * settle;
        }
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(tiltAngle)));

        // Opens with an ease, then the fade above closes it.
        float scale = state.isImpactBurst ? (0.85f + ease * 0.45f) : (0.72f + ease * 0.48f);
        poseStack.scale(scale, scale, scale);

        float span = 4.2f;
        float chord = 1.35f;
        int segments = 40;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Soft wake of the same crescent, wider and dimmer, so the reap has a trail.
            drawCrescentArc(matrix, buffer, span + 0.95f, chord + 0.72f, 0.10f, segments,
                0.42f, 0.0f, 0.55f, 0.32f * slashFade);

            // 1. Abyssal Dark Void Outer Silhouette
            drawCrescentArc(matrix, buffer, span + 0.45f, chord + 0.4f, 0.26f, segments,
                0.08f, 0.0f, 0.15f, 0.90f * slashFade);

            // 2. Weeping Sorrow Radiant Magenta / Violet Blade Crest
            drawCrescentArc(matrix, buffer, span, chord, 0.14f, segments,
                0.80f, 0.05f, 0.85f, 0.95f * slashFade);

            // 3. Piercing Razor-Sharp Cyan / White Dimensional Edge.
            // On a hit the edge burns and then eases out with the crescent.
            float edge = state.isImpactBurst ? (0.45f + 0.55f * (1.0f - ease)) : 1.0f;
            drawCrescentArc(matrix, buffer, span * 0.72f, chord * 0.72f, 0.06f, segments,
                0.90f, 0.98f, 1.0f, slashFade * edge);

            // Hot core along the crest. Thin, so the edge stays a line.
            drawCrescentArc(matrix, buffer, span * 0.58f, chord * 0.58f, 0.028f, segments,
                1.0f, 0.94f, 1.0f, slashFade * edge);

            // The hit keeps its second crescent, crossing the first.
            if (state.isImpactBurst) {
                drawCrescentArc(matrix, buffer, span * 0.55f, -chord * 0.55f, 0.08f, segments,
                    0.65f, 0.0f, 0.90f, 0.75f * slashFade);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawCrescentArc(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float thickness, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;

            // Parabolic scythe bow. The pose tilt makes it a sickle; the bow itself stays deep.
            float x1 = t1 * span;
            float z1 = (1.0f - t1 * t1) * chord;
            float x2 = t2 * span;
            float z2 = (1.0f - t2 * t2) * chord;

            float th1 = thickness * (1.0f - Math.abs(t1) * 0.75f);
            float th2 = thickness * (1.0f - Math.abs(t2) * 0.75f);

            float z1Trail = z1 - 0.9f * (1.0f - Math.abs(t1));
            float z2Trail = z2 - 0.9f * (1.0f - Math.abs(t2));

            // Three-stop shading (redesign T2): the arc burns hottest at its
            // center and deepens toward the horn tips.
            float shade1 = 1.0f - Math.abs(t1) * 0.35f;
            float shade2 = 1.0f - Math.abs(t2) * 0.35f;

            // Top Face
            consumer.addVertex(matrix, x1, th1, z1).setColor(r * shade1, g * shade1, b * shade1, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2, z2).setColor(r * shade2, g * shade2, b * shade2, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2, z2Trail).setColor(r * shade2, g * shade2, b * shade2, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, th1, z1Trail).setColor(r * shade1, g * shade1, b * shade1, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom Face
            consumer.addVertex(matrix, x1, -th1, z1Trail).setColor(r * shade1, g * shade1, b * shade1, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2, z2Trail).setColor(r * shade2, g * shade2, b * shade2, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(r * shade2, g * shade2, b * shade2, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, -th1, z1).setColor(r * shade1, g * shade1, b * shade1, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }
}
