package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DelayedLaunchEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * Telegraph for the Z Sword delayed launch. A violet gravity ring contracts
 * around the slam center as the delay runs down, with gold rune ticks that
 * spin faster near the end.
 *
 * Spectacle pass: the ring used to be hardcoded at 5.0 blocks shrinking to 1.2
 * while the slam that spawned it reached a radius of 18, so the warning covered
 * less than a third of the real area. The ring now starts at the true slam
 * radius (synced from the entity) and contracts to 35 percent of it, over the
 * true delay (40 to 80 ticks, also synced), and a faint anchor disc marks the
 * full footprint for the whole countdown.
 */
public class DelayedLaunchRenderer extends EntityRenderer<DelayedLaunchEntity, DelayedLaunchRenderer.DelayedLaunchRenderState> {
    public DelayedLaunchRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DelayedLaunchRenderState extends EntityRenderState {
        public float age = 0;
        public float radius = 10.0f;
        public int delayTicks = 40;
    }

    @Override
    public DelayedLaunchRenderState createRenderState() {
        return new DelayedLaunchRenderState();
    }

    @Override
    public void extractRenderState(DelayedLaunchEntity entity, DelayedLaunchRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.tickCount + partialTicks;
        state.radius = entity.getRadius();
        state.delayTicks = Math.max(1, entity.getDelayTicks());
    }

    @Override
    public void submit(DelayedLaunchRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float age = state.age;
        float progress = Math.min(1.0f, age / (float) state.delayTicks);
        float slamRadius = state.radius;

        // Contracting ring: starts at the real slam radius, closing to 35
        // percent of it at the launch tick.
        float radius = slamRadius * (1.0f - progress * 0.65f);
        float pulse = 0.60f + 0.40f * (float) Math.sin(age * (0.35 + progress * 0.85));
        float ringAlpha = (0.35f + progress * 0.45f) * pulse;
        float rot = age * (6.0f + progress * 30.0f);
        float dist = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        int ringSegs = com.dragonblockarcanedba.util.FxEnvelope.segments(48, dist, 20);

        poseStack.pushPose();
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Full footprint marker at the true slam radius for the whole
            // countdown, faint, so the warning size equals the hit size.
            drawGroundRing(matrix, buffer, slamRadius, 0.30f, ringSegs, 0.0f,
                0.58f, 0.10f, 0.98f, ringAlpha * 0.30f);
            // Outer violet ring band, alpha 0 at both edges, bright at the crest line.
            drawGroundRing(matrix, buffer, radius, 0.45f, ringSegs, 0.0f,
                0.58f, 0.10f, 0.98f, ringAlpha);
            // Inner gold ring closing tighter.
            drawGroundRing(matrix, buffer, radius * 0.62f, 0.22f, ringSegs, 0.0f,
                1.0f, 0.85f, 0.30f, ringAlpha * 0.9f);

            // Four rune ticks at the cardinal points, rotating as the launch nears.
            for (int i = 0; i < 4; i++) {
                double a = Math.toRadians(rot + i * 90.0);
                float cx = (float) Math.cos(a) * radius;
                float cz = (float) Math.sin(a) * radius;
                drawRuneTick(matrix, buffer, cx, cz, (float) a, 0.55f,
                    1.0f, 0.95f, 0.55f, Math.min(1.0f, ringAlpha + 0.25f));
            }
        });

        // Additive halo pass: a wider faint violet ring under the telegraph so
        // the warning glows against the ground as the launch nears, plus the
        // shared anchor glow across the full slam footprint.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            drawGroundRing(matrix, buffer, radius * 1.12f, 0.60f, ringSegs, 0.0f,
                0.58f, 0.10f, 0.98f, ringAlpha * 0.35f);
            com.dragonblockarcanedba.client.render.ki.LayeredRender.drawAnchorGlow(pose, buffer,
                slamRadius, 0.03f, 0.58f, 0.10f, 0.98f, ringAlpha * 0.22f, dist);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawGroundRing(Matrix4f matrix, VertexConsumer consumer, float radius, float width, int segments, float y,
                                       float r, float g, float b, float a) {
        float inner = Math.max(0.05f, radius - width);
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;
            float x1o = (float) Math.cos(a1) * radius, z1o = (float) Math.sin(a1) * radius;
            float x2o = (float) Math.cos(a2) * radius, z2o = (float) Math.sin(a2) * radius;
            float x1i = (float) Math.cos(a1) * inner, z1i = (float) Math.sin(a1) * inner;
            float x2i = (float) Math.cos(a2) * inner, z2i = (float) Math.sin(a2) * inner;

            consumer.addVertex(matrix, x1o, y + 0.06f, z1o).setColor(r, g, b, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2o, y + 0.06f, z2o).setColor(r, g, b, 0.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2i, y + 0.06f, z2i).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1i, y + 0.06f, z1i).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawRuneTick(Matrix4f matrix, VertexConsumer consumer, float cx, float cz, float facing, float len,
                                     float r, float g, float b, float a) {
        float dx = (float) Math.cos(facing), dz = (float) Math.sin(facing);
        float px = -dz * 0.09f, pz = dx * 0.09f;
        float x1 = cx - dx * len * 0.5f, z1 = cz - dz * len * 0.5f;
        float x2 = cx + dx * len * 0.5f, z2 = cz + dz * len * 0.5f;

        consumer.addVertex(matrix, x1 - px, 0.10f, z1 - pz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - px, 0.10f, z2 - pz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + px, 0.10f, z2 + pz).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + px, 0.10f, z1 + pz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
