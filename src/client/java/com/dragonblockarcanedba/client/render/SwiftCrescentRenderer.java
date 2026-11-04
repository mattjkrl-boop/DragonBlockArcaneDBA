package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.SwiftCrescentEntity;
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
 * Entity Renderer for Swift Crescent in Minecraft 26.2.
 * Renders the Katana Flashdraw crescent in layers: a silver body, a bright
 * white core ribbon, a colored cutting lip, an outer wispy halo copy, and a
 * belly spark that peaks at the strike tick. Variant 0 cuts with a cyan lip;
 * variant 1 mirrors the arc and cuts with a pale silver-blue lip. Alpha runs
 * on the shared FxEnvelope so the cut builds, peaks, and dissipates.
 */
public class SwiftCrescentRenderer extends EntityRenderer<SwiftCrescentEntity, SwiftCrescentRenderer.SwiftCrescentRenderState> {

    public SwiftCrescentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SwiftCrescentRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float tiltAngle = 0.0f;
        public int variant = 0;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public int maxLifetime = 10;
        public int strikeTick = -1;
        public double wx = 0, wy = 0, wz = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SwiftCrescentEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SwiftCrescentEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SwiftCrescentRenderState createRenderState() {
        return new SwiftCrescentRenderState();
    }

    @Override
    public void extractRenderState(SwiftCrescentEntity entity, SwiftCrescentRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getSlashScale();
        state.tiltAngle = entity.getTiltAngle();
        state.variant = entity.getSlashVariant();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.strikeTick = entity.getStrikeDamage() > 0f ? entity.getStrikeTick() : -1;
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(SwiftCrescentRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // Shared lifecycle: build over the first 25 percent, dissipate over
        // the last 35. Nothing renders outside the envelope.
        float fade = FxEnvelope.envelope(state.age, state.maxLifetime, 0.25f, 0.35f);
        if (fade <= 0.0f) return;
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);

        float open = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.25f));
        float baseScale = state.scale * (0.76f + 0.30f * open);
        float trail = 0.10f + 0.58f * FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.65f));
        // The belly spark peaks exactly when the carried strike lands.
        float strikeFrac = state.strikeTick >= 0
            ? state.strikeTick / (float) state.maxLifetime
            : 0.20f;
        float impact = strike(progress, strikeFrac);
        float dist = LayeredRender.distScale(cameraState, state.wx, state.wy, state.wz);
        int segments = FxEnvelope.segments(22, dist, 12);
        RenderType renderType = KiRenderHelper.kiRenderType();

        // Variant 1 mirrors the arc and swaps the cyan lip for silver-blue.
        float mirror = state.variant == 1 ? -1.0f : 1.0f;
        float lipR = state.variant == 1 ? 0.55f : 0.0f;
        float lipG = state.variant == 1 ? 0.80f : 0.86f;
        float lipB = 1.0f;

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(state.tiltAngle)));
        poseStack.scale(baseScale, baseScale, baseScale);

        float span = 2.4f;
        float chord = 0.85f * mirror;
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            drawSilverCrescent(matrix, buffer, span, chord, trail, fade, impact, segments, lipR, lipG, lipB);
        });

        // Outer layer: a larger, faint copy of the same crescent so the cut
        // has a wispy edge that dissolves into the air (standard point 1).
        poseStack.scale(1.3f, 1.3f, 1.3f);
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            drawSilverCrescent(matrix, buffer, span, chord, trail * 1.3f, fade * 0.18f, 0.0f, segments, lipR, lipG, lipB);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /** Sine flash peaking at the strike fraction of life, gone by twice it. */
    private static float strike(float progress, float strikeFrac) {
        float window = Math.min(1.0f, strikeFrac * 2.0f);
        if (progress >= window) return 0.0f;
        return (float) Math.sin((progress / window) * Math.PI);
    }

    /**
     * One silver crescent. The colored lip is the edge, the white ribbon is the core,
     * and the fading wake is the follow-through. The flash at the belly ends.
     * A negative chord mirrors the arc (variant 1).
     */
    private static void drawSilverCrescent(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float trailLength, float fade, float impact, int segments, float lipR, float lipG, float lipB) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;
            float x1 = t1 * span;
            float z1 = (1.0f - t1 * t1) * chord;
            float x2 = t2 * span;
            float z2 = (1.0f - t2 * t2) * chord;
            float taper1 = 1.0f - Math.abs(t1) * 0.78f;
            float taper2 = 1.0f - Math.abs(t2) * 0.78f;
            float th1 = 0.12f * taper1;
            float th2 = 0.12f * taper2;
            float z1Trail = z1 - trailLength * taper1;
            float z2Trail = z2 - trailLength * taper2;

            consumer.addVertex(matrix, x1, th1, z1).setColor(0.86f, 0.95f, 1.0f, 0.92f * fade).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2, z2).setColor(0.86f, 0.95f, 1.0f, 0.92f * fade).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2 * 0.22f, z2Trail).setColor(0.86f, 0.95f, 1.0f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, th1 * 0.22f, z1Trail).setColor(0.86f, 0.95f, 1.0f, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            consumer.addVertex(matrix, x1, -th1 * 0.22f, z1Trail).setColor(0.86f, 0.95f, 1.0f, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2 * 0.22f, z2Trail).setColor(0.86f, 0.95f, 1.0f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(0.86f, 0.95f, 1.0f, 0.92f * fade).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, -th1, z1).setColor(0.86f, 0.95f, 1.0f, 0.92f * fade).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

            float lip = 0.02f * Math.signum(chord);
            consumer.addVertex(matrix, x1, -th1, z1).setColor(lipR, lipG, lipB, 0.95f * fade).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(lipR, lipG, lipB, 0.95f * fade).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, th2, z2 + lip).setColor(lipR, lipG, lipB, 0.95f * fade).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, th1, z1 + lip).setColor(lipR, lipG, lipB, 0.95f * fade).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);

            float c1 = th1 * 0.32f;
            float c2 = th2 * 0.32f;
            consumer.addVertex(matrix, x1, c1, z1).setColor(1.0f, 1.0f, 1.0f, fade).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, c2, z2).setColor(1.0f, 1.0f, 1.0f, fade).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, 0.0f, z2 - trailLength * 0.18f * taper2).setColor(1.0f, 1.0f, 1.0f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, 0.0f, z1 - trailLength * 0.18f * taper1).setColor(1.0f, 1.0f, 1.0f, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }

        float spur = 0.16f;
        for (int side = -1; side <= 1; side += 2) {
            float sign = side;
            int spurSteps = 5;
            for (int s = 0; s < spurSteps; s++) {
                float t1 = s / (float) spurSteps;
                float t2 = (s + 1) / (float) spurSteps;
                float z1 = -spur * 1.8f * t1;
                float z2 = -spur * 1.8f * t2;
                float bow1 = (float) Math.sin(t1 * Math.PI) * spur * 0.5f;
                float bow2 = (float) Math.sin(t2 * Math.PI) * spur * 0.5f;
                float x1 = sign * span + sign * (spur * 1.35f * t1 + bow1);
                float x2 = sign * span + sign * (spur * 1.35f * t2 + bow2);
                float w1 = spur * 0.2f * (1.0f - t1);
                float w2 = spur * 0.2f * (1.0f - t2);
                float a1 = fade * (1.0f - t1);
                float a2 = fade * (1.0f - t2) * 0.12f;
                consumer.addVertex(matrix, x1, -w1, z1).setColor(0.86f, 0.95f, 1.0f, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1, w1, z1).setColor(0.86f, 0.95f, 1.0f, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2, w2, z2).setColor(lipR, lipG, lipB, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2, -w2, z2).setColor(lipR, lipG, lipB, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1, -w1 * 0.3f, z1).setColor(1.0f, 1.0f, 1.0f, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1, w1 * 0.3f, z1).setColor(1.0f, 1.0f, 1.0f, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2, w2 * 0.3f, z2).setColor(1.0f, 1.0f, 1.0f, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2, -w2 * 0.3f, z2).setColor(1.0f, 1.0f, 1.0f, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            }
        }

        if (impact > 0.02f) {
            drawDiamondSpark(matrix, consumer, 0.0f, 0.0f, chord * 0.45f, 0.20f * impact, 1.0f, 1.0f, 1.0f, impact);
        }
    }

    private static void drawDiamondSpark(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float r, float g, float b, float a) {
        float h = size * 1.5f;
        float w = size * 0.5f;

        consumer.addVertex(matrix, cx - w, cy, cz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx + w, cy, cz).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        consumer.addVertex(matrix, cx, cy, cz - w).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy, cz + w).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        int arms = 4;
        int steps = 3;
        for (int arm = 0; arm < arms; arm++) {
            float ang = arm * ((float) Math.PI * 0.5f) + 0.35f;
            float dx = (float) Math.cos(ang);
            float dz = (float) Math.sin(ang);
            float px = -dz;
            float pz = dx;
            for (int i = 0; i < steps; i++) {
                float t1 = i / (float) steps;
                float t2 = (i + 1) / (float) steps;
                float bow1 = (float) Math.sin(t1 * Math.PI) * size * 0.45f;
                float bow2 = (float) Math.sin(t2 * Math.PI) * size * 0.45f;
                float d1 = size * (0.35f + 1.15f * t1);
                float d2 = size * (0.35f + 1.15f * t2);
                float w1 = size * 0.28f * (1.0f - t1);
                float w2 = size * 0.28f * (1.0f - t2);
                float x1 = cx + dx * d1 + px * bow1;
                float z1 = cz + dz * d1 + pz * bow1;
                float x2 = cx + dx * d2 + px * bow2;
                float z2 = cz + dz * d2 + pz * bow2;
                float a1 = a * (1.0f - t1 * 0.25f);
                float a2 = a * (1.0f - t2);
                consumer.addVertex(matrix, x1 - px * w1, cy, z1 - pz * w1).setColor(r, g, b, a1 * 0.4f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1 + px * w1, cy, z1 + pz * w1).setColor(r, g, b, a1 * 0.4f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 + px * w2, cy, z2 + pz * w2).setColor(r, g, b, a2 * 0.08f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 - px * w2, cy, z2 - pz * w2).setColor(r, g, b, a2 * 0.08f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1 - px * w1 * 0.3f, cy, z1 - pz * w1 * 0.3f).setColor(1.0f, 1.0f, 1.0f, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1 + px * w1 * 0.3f, cy, z1 + pz * w1 * 0.3f).setColor(1.0f, 1.0f, 1.0f, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 + px * w2 * 0.3f, cy, z2 + pz * w2 * 0.3f).setColor(1.0f, 1.0f, 1.0f, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2 - px * w2 * 0.3f, cy, z2 - pz * w2 * 0.3f).setColor(1.0f, 1.0f, 1.0f, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            }
        }
    }
}
