package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.BraveSlashEntity;
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
import org.joml.Matrix4f;

/**
 * Entity Renderer for Brave Slash in MC 26.2.
 * Renders a razor-sharp, glowing golden/cyan aerodynamic crescent energy wave.
 */
public class BraveSlashRenderer extends EntityRenderer<BraveSlashEntity, BraveSlashRenderer.BraveSlashRenderState> {
    public BraveSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BraveSlashRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public int maxLifetime = BraveSlashEntity.MAX_LIFE;
    }

    @Override
    public BraveSlashRenderState createRenderState() {
        return new BraveSlashRenderState();
    }

    @Override
    public void extractRenderState(BraveSlashEntity entity, BraveSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
    }

    @Override
    public void submit(BraveSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        // Shared lifecycle: the cut builds over the first fifth of its real
        // life, peaks, then dissipates over the last third, the same envelope
        // the entity emits its wake particles with. Before this pass the fade
        // ran linearly across the whole flight, so the slash was half gone
        // mid flight, and the 40 was a second hardcoded copy of the lifetime.
        float life = (float) state.maxLifetime;
        float progress = Math.min(1.0f, state.age / life);
        float fade = FxEnvelope.envelope(state.age, life, 0.2f, 0.35f);
        float open = FxEnvelope.smoothstep(Math.min(1.0f, state.age / (life * 0.2f)));
        float span = 3.2f * (0.82f + 0.22f * open);
        float trail = 0.18f + 0.55f * FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.6f));
        float impact = strike(progress);
        int segments = FxEnvelope.segments(28,
            LayeredRender.distScale(cameraState, state.x, state.y, state.z), 12);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            drawHeroCut(matrix, buffer, span, trail, fade, impact, segments);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float strike(float progress) {
        if (progress >= 0.36f) return 0.0f;
        return (float) Math.sin((progress / 0.36f) * Math.PI);
    }

    /** Tapered gold-cyan crescent: bright core, thin white edge, a wake that follows the arc. */
    private static void drawHeroCut(Matrix4f matrix, VertexConsumer consumer, float span, float trail, float fade, float impact, int segments) {
        float chord = span * 0.36f;
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;
            float x1 = t1 * span;
            float x2 = t2 * span;
            float taper1 = 1.0f - Math.abs(t1) * 0.78f;
            float taper2 = 1.0f - Math.abs(t2) * 0.78f;
            float th1 = 0.13f * taper1;
            float th2 = 0.13f * taper2;
            float z1 = (1.0f - t1 * t1) * chord;
            float z2 = (1.0f - t2 * t2) * chord;
            float z1t = z1 - trail * taper1;
            float z2t = z2 - trail * taper2;

            consumer.addVertex(matrix, x1, th1, z1).setColor(1.0f, 0.84f, 0.0f, 0.90f * fade).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2, z2).setColor(1.0f, 0.84f, 0.0f, 0.90f * fade).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2 * 0.2f, z2t).setColor(1.0f, 0.84f, 0.0f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, th1 * 0.2f, z1t).setColor(1.0f, 0.84f, 0.0f, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            consumer.addVertex(matrix, x1, -th1 * 0.2f, z1t).setColor(1.0f, 0.84f, 0.0f, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2 * 0.2f, z2t).setColor(1.0f, 0.84f, 0.0f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(1.0f, 0.84f, 0.0f, 0.90f * fade).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, -th1, z1).setColor(1.0f, 0.84f, 0.0f, 0.90f * fade).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

            float c1 = th1 * 0.28f;
            float c2 = th2 * 0.28f;
            consumer.addVertex(matrix, x1, c1, z1).setColor(0.0f, 0.95f, 1.0f, fade).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, c2, z2).setColor(0.0f, 0.95f, 1.0f, fade).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, -c2, z2).setColor(0.0f, 0.95f, 1.0f, fade).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, -c1, z1).setColor(0.0f, 0.95f, 1.0f, fade).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            float e1 = th1 * 0.12f;
            float e2 = th2 * 0.12f;
            consumer.addVertex(matrix, x1, -e1, z1).setColor(1.0f, 1.0f, 1.0f, fade).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, -e2, z2).setColor(1.0f, 1.0f, 1.0f, fade).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, e2, z2).setColor(1.0f, 1.0f, 1.0f, fade).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, e1, z1).setColor(1.0f, 1.0f, 1.0f, fade).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }

        if (impact > 0.02f) {
            float s = 0.28f * impact;
            float belly = chord * 0.55f;
            consumer.addVertex(matrix, -s, 0.0f, belly).setColor(1.0f, 0.96f, 0.75f, impact).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, 0.0f, s, belly).setColor(1.0f, 1.0f, 1.0f, impact).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, s, 0.0f, belly).setColor(1.0f, 0.96f, 0.75f, impact).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, 0.0f, -s, belly).setColor(1.0f, 1.0f, 1.0f, impact).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }
}
