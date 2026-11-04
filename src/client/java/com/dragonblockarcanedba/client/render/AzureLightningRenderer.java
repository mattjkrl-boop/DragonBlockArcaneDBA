package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.AzureLightningEntity;
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
 * Telegraph renderer for AzureLightningEntity.
 *
 * Spectacle pass: the bolt itself is drawn by the shared BoltEntity now, so
 * this renderer draws the warning and the aftermath. During the telegraph
 * window it draws an anchor glow and a hard ring at exactly the strike
 * radius (the warning's size equals the hit's size), a converging inner
 * ring that closes as the strike nears, and a thin charge column. After the
 * strike tick it draws an 8 tick afterglow ring that fades out. The old
 * textured 44 block bolt mesh is retired; the BoltEntity's branched seeded
 * bolt replaces it.
 */
public class AzureLightningRenderer extends EntityRenderer<AzureLightningEntity, AzureLightningRenderer.AzureLightningRenderState> {

    public AzureLightningRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AzureLightningRenderState extends EntityRenderState {
        public boolean isRare;
        public float age;
        public long seed;
        public int telegraphTicks = 12;
        public float radius = 3.5f;
        public double wx, wy, wz;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AzureLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AzureLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public AzureLightningRenderState createRenderState() {
        return new AzureLightningRenderState();
    }

    @Override
    public void extractRenderState(AzureLightningEntity entity, AzureLightningRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isRare = entity.isRare();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
        state.telegraphTicks = entity.getTelegraphTicks();
        state.radius = entity.getRadius();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(AzureLightningRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float age = state.age;
        float strikeAt = Math.max(1.0f, state.telegraphTicks);
        float radius = state.radius;
        if (age >= strikeAt + AzureLightningEntity.AFTERGLOW_TICKS) return;

        float ds = LayeredRender.distScale(cameraState, state.wx, state.wy, state.wz);
        int ringSegments = FxEnvelope.segments(32, ds, 12);

        // Theme colors: azure for a normal strike, pale gold for a rare one.
        float cr = state.isRare ? 1.0f : 0.15f;
        float cg = state.isRare ? 0.85f : 0.85f;
        float cb = state.isRare ? 0.45f : 1.0f;

        RenderType glowType = KiRenderHelper.kiRenderType();

        if (age < strikeAt) {
            // Telegraph phase: t runs 0 to 1 across the warning window.
            float t = FxEnvelope.smoothstep(age / strikeAt);
            // The warning pulses faster as the strike closes in.
            float blink = (Math.sin(age * (0.6 + t * 1.6)) > 0.0) ? 1.0f : 0.72f;
            float warnAlpha = (0.35f + 0.55f * t) * blink;

            collector.submitCustomGeometry(poseStack, glowType, (pose, buffer) -> {
                Matrix4f matrix = pose.pose();

                // Anchor glow filling the warned circle.
                LayeredRender.drawAnchorGlow(pose, buffer, radius, 0.06f,
                    cr, cg, cb, (0.16f + 0.30f * t) * blink, ds);

                // Hard warning ring at exactly the damage radius.
                drawGroundRing(matrix, buffer, 0, 0.07f, 0, radius, radius * 0.92f, ringSegments,
                    cr, cg, cb, warnAlpha);

                // Converging inner ring: closes to the center as t reaches 1.
                float convR = radius * (1.0f - t) + 0.35f;
                drawGroundRing(matrix, buffer, 0, 0.08f, 0, convR, Math.max(0.0f, convR - 0.3f), ringSegments,
                    0.95f, 1.0f, 1.0f, 0.55f * t * blink);

                // Thin charge column rising from the strike point.
                float beamAlpha = (0.10f + 0.28f * t) * blink;
                drawChargeBeam(matrix, buffer, 0.22f, 30.0f, cr, cg, cb, beamAlpha);
            });
        } else {
            // Afterglow phase: the struck circle cools and fades over 8 ticks.
            float k = (age - strikeAt) / (float) AzureLightningEntity.AFTERGLOW_TICKS;
            float fade = 1.0f - FxEnvelope.smoothstep(k);

            collector.submitCustomGeometry(poseStack, glowType, (pose, buffer) -> {
                LayeredRender.drawAnchorGlow(pose, buffer, radius, 0.06f,
                    cr, cg, cb, 0.30f * fade, ds);
                drawGroundRing(pose.pose(), buffer, 0, 0.07f, 0, radius, radius * 0.8f, ringSegments,
                    cr, cg, cb, 0.55f * fade);
            });
        }
    }

    /** Two crossed thin quads forming a vertical charge column at the origin. */
    private static void drawChargeBeam(Matrix4f matrix, VertexConsumer consumer, float halfWidth, float height,
                                       float r, float g, float b, float a) {
        // Facing Z quad (width along X), alpha fading with height via two rows.
        quad(matrix, consumer, -halfWidth, 0, 0, halfWidth, 0, 0, halfWidth, height, 0, -halfWidth, height, 0, r, g, b, a, a * 0.15f);
        // Facing X quad (width along Z).
        quad(matrix, consumer, 0, 0, -halfWidth, 0, 0, halfWidth, 0, height, halfWidth, 0, height, -halfWidth, r, g, b, a, a * 0.15f);
    }

    private static void quad(Matrix4f matrix, VertexConsumer consumer,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float r, float g, float b, float aBase, float aTop) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, aTop).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, aTop).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, aTop).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, aTop).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }

    private static void drawGroundRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2;

            float x1 = cx + (float) Math.cos(a1) * rOuter;
            float z1 = cz + (float) Math.sin(a1) * rOuter;
            float x2 = cx + (float) Math.cos(a2) * rOuter;
            float z2 = cz + (float) Math.sin(a2) * rOuter;

            float ix1 = cx + (float) Math.cos(a1) * rInner;
            float iz1 = cz + (float) Math.sin(a1) * rInner;
            float ix2 = cx + (float) Math.cos(a2) * rInner;
            float iz2 = cz + (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, ix1, cy, iz1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ix2, cy, iz2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, cy, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, cy, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }
}
