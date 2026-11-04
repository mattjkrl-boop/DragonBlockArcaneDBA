package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.BraveShockwaveEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
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
 * Entity Renderer for Brave Sword Heroic Impact Shockwave in Minecraft 26.2.
 * Renders physical 3D golden raised beveled crests, cyan harmonic compression rings,
 * vertical crystalline valor blades/spikes, and an epicenter starburst core.
 */
public class BraveShockwaveRenderer extends EntityRenderer<BraveShockwaveEntity, BraveShockwaveRenderer.ShockwaveRenderState> {

    public BraveShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShockwaveRenderState extends EntityRenderState {
        public float maxRadius = 7.5f;
        public float age = 0;
        public int maxLifetime = 44;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BraveShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BraveShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ShockwaveRenderState createRenderState() {
        return new ShockwaveRenderState();
    }

    @Override
    public void extractRenderState(BraveShockwaveEntity entity, ShockwaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.maxRadius = entity.getMaxRadius();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
    }

    @Override
    public void submit(ShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // The lifetime comes from the entity (44 ticks), not a hardcoded copy:
        // the ring now has time to cross its full radius and dissipate.
        float maxLife = (float) state.maxLifetime;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float expansionEase = (float) Math.sin(progress * (Math.PI / 2.0));
        float currentRadius = Math.max(0.05f, expansionEase * state.maxRadius);
        float env = FxEnvelope.envelope(state.age, maxLife, 0.15f, 0.35f);
        float alpha = env * 0.92f;
        float crest = 0.85f * env;
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        int ringSegments = FxEnvelope.segments(40, distScale, 14);
        float age = state.age;

        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Ground anchor glow under the whole effect (standard point 1).
            LayeredRender.drawAnchorGlow(pose, buffer, currentRadius * 0.95f, 0.03f,
                1.0f, 0.84f, 0.0f, alpha * 0.45f, distScale);

            // Outer layer: a wide, faint gold wisp ring ahead of the front.
            float wispOuter = currentRadius * 1.14f;
            drawFlatRing(matrix, buffer, 0, 0.04f, 0, wispOuter, currentRadius * 1.02f,
                ringSegments, 1.0f, 0.84f, 0.0f, alpha * LayeredRender.ALPHA_OUTER);

            // Mid layer: the beveled crest ring, gold base to cyan top.
            float ringWidth = Math.min(0.85f, currentRadius * 0.16f);
            float rOuter = currentRadius;
            float rInner = Math.max(0.1f, currentRadius - ringWidth);
            drawBeveledShockRing(matrix, buffer, 0, 0.05f, 0, rOuter, rInner, crest, ringSegments,
                1.0f, 0.84f, 0.0f, alpha,
                0.0f, 0.95f, 1.0f, alpha
            );

            // Vertical crystalline valor spikes riding the ring front, as the
            // header promises: twelve blades whose heights vary around the
            // ring and slowly rotate with age.
            drawValorSpikes(matrix, buffer, currentRadius, 0.05f, crest, age, alpha,
                FxEnvelope.segments(12, distScale, 6));

            // Core layer: the bright white epicenter ring and starburst core.
            float coreOuter = Math.max(0.12f, rInner * 0.72f);
            drawFlatRing(matrix, buffer, 0, 0.09f, 0, coreOuter, coreOuter * 0.62f,
                FxEnvelope.segments(32, distScale, 12), 1.0f, 1.0f, 1.0f, alpha * 0.9f);
            float burstRadius = state.maxRadius * 0.20f * (0.75f + 0.25f * (float) Math.sin(age * 0.35f));
            drawStarburstCore(matrix, buffer, 0, 0.10f, 0, burstRadius, age * 14.0f, alpha);
        });

        super.submit(state, poseStack, collector, cameraState);
    }

    /** Twelve crystalline spikes standing on the ring front, tips up. */
    private static void drawValorSpikes(Matrix4f matrix, VertexConsumer consumer, float radius, float baseY,
                                        float crest, float age, float alpha, int spikeCount) {
        if (radius < 0.4f || alpha <= 0.01f) return;
        float rot = age * 0.045f;
        for (int k = 0; k < spikeCount; k++) {
            double a = rot + (k / (double) spikeCount) * Math.PI * 2.0;
            float vary = 0.75f + 0.45f * (float) Math.sin(k * 2.7f + age * 0.22f);
            float height = (0.7f + crest * 1.15f) * vary;
            float halfWidth = 0.16f;
            double ax = Math.cos(a), az = Math.sin(a);
            double tx = -az, tz = ax;
            float bx = (float) (ax * radius), bz = (float) (az * radius);
            float lx = bx + (float) (tx * halfWidth), lz = bz + (float) (tz * halfWidth);
            float rx = bx - (float) (tx * halfWidth), rz = bz - (float) (tz * halfWidth);
            float tipX = bx + (float) (ax * 0.22f), tipZ = bz + (float) (az * 0.22f);
            // Pale gold crystalline blade, front and back.
            drawTriangle(matrix, consumer,
                lx, baseY, lz, rx, baseY, rz, tipX, baseY + height, tipZ,
                1.0f, 0.95f, 0.72f, alpha * 0.8f);
        }
    }

    /** Eight point starburst at the epicenter, gold heart to cyan rim. */
    private static void drawStarburstCore(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz,
                                          float radius, float rotDeg, float alpha) {
        if (radius < 0.05f || alpha <= 0.01f) return;
        double rot = Math.toRadians(rotDeg);
        int points = 8;
        for (int i = 0; i < points * 2; i++) {
            double a1 = rot + (i / (double) (points * 2)) * Math.PI * 2.0;
            double a2 = rot + ((i + 1) / (double) (points * 2)) * Math.PI * 2.0;
            float r1 = (i % 2 == 0) ? radius : radius * 0.38f;
            float r2 = ((i + 1) % 2 == 0) ? radius : radius * 0.38f;
            float x1 = cx + (float) Math.cos(a1) * r1, z1 = cz + (float) Math.sin(a1) * r1;
            float x2 = cx + (float) Math.cos(a2) * r2, z2 = cz + (float) Math.sin(a2) * r2;
            boolean tip = (i % 2 == 0);
            float er = tip ? 0.0f : 1.0f, eg = tip ? 0.95f : 0.88f, eb = tip ? 1.0f : 0.2f;
            drawTriangle(matrix, consumer,
                cx, cy, cz, x1, cy, z1, x2, cy, z2,
                er, eg, eb, alpha * 0.85f);
        }
    }

    private static void drawBeveledShockRing(Matrix4f matrix, VertexConsumer consumer,
                                             float cx, float cy, float cz,
                                             float rOuter, float rInner, float height,
                                             int segments,
                                             float rBase, float gBase, float bBase, float aBase,
                                             float rTop, float gTop, float bTop, float aTop) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1Out = cx + (float) Math.cos(a1) * rOuter;
            float z1Out = cz + (float) Math.sin(a1) * rOuter;
            float x2Out = cx + (float) Math.cos(a2) * rOuter;
            float z2Out = cz + (float) Math.sin(a2) * rOuter;

            float x1In = cx + (float) Math.cos(a1) * rInner;
            float z1In = cz + (float) Math.sin(a1) * rInner;
            float x2In = cx + (float) Math.cos(a2) * rInner;
            float z2In = cz + (float) Math.sin(a2) * rInner;

            // Outer rising bevel
            consumer.addVertex(matrix, x1Out, cy, z1Out).setColor(rBase, gBase, bBase, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy, z2Out).setColor(rBase, gBase, bBase, 0.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy + height, z2Out).setColor(rTop, gTop, bTop, aTop).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1Out, cy + height, z1Out).setColor(rTop, gTop, bTop, aTop).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Inward slope
            consumer.addVertex(matrix, x1Out, cy + height, z1Out).setColor(rTop, gTop, bTop, aTop).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy + height, z2Out).setColor(rTop, gTop, bTop, aTop).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2In, cy, z2In).setColor(rBase, gBase, bBase, aBase * 0.5f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1In, cy, z1In).setColor(rBase, gBase, bBase, aBase * 0.5f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawFlatRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1Out = cx + (float) Math.cos(a1) * rOuter;
            float z1Out = cz + (float) Math.sin(a1) * rOuter;
            float x2Out = cx + (float) Math.cos(a2) * rOuter;
            float z2Out = cz + (float) Math.sin(a2) * rOuter;

            float x1In = cx + (float) Math.cos(a1) * rInner;
            float z1In = cz + (float) Math.sin(a1) * rInner;
            float x2In = cx + (float) Math.cos(a2) * rInner;
            float z2In = cz + (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, x1In, cy, z1In).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2In, cy, z2In).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy, z2Out).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1Out, cy, z1Out).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawTriangle(Matrix4f matrix, VertexConsumer consumer,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float x3, float y3, float z3,
                                     float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Reverse
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
