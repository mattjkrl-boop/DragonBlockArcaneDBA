package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.EvilSpearProjectileEntity;
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
 * Entity Renderer for Evil Spear Projectile in MC 26.2.
 * Renders a high-fidelity 3D demonic spectral spear with faceted shaft, multi-beveled diamond spearhead,
 * barbed vortex fins, supersonic Mach shock cone, twin helical dynamic trail ribbons, and expanding wake vortex rings.
 */
public class EvilSpearProjectileRenderer extends EntityRenderer<EvilSpearProjectileEntity, EvilSpearProjectileRenderer.EvilSpearRenderState> {
    public EvilSpearProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class EvilSpearRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public boolean isFirstPersonOwner = false;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(EvilSpearProjectileEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(EvilSpearProjectileEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public EvilSpearRenderState createRenderState() {
        return new EvilSpearRenderState();
    }

    @Override
    public void extractRenderState(EvilSpearProjectileEntity entity, EvilSpearRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(EvilSpearRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float age = state.age;
        // Distance scaling (standard point 1): distant throws step their
        // segment counts down instead of costing full geometry.
        final float ds = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(cameraState, state.x, state.y, state.z);

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(age * 30.0f))); // Continuous high-speed spiral spin

        float fpScale = (state.isFirstPersonOwner && state.age < 5.0f) ? (0.60f + (state.age / 5.0f) * 0.40f) : 1.0f;
        poseStack.scale(fpScale, fpScale, fpScale);

        // The throw builds in over its first 3 ticks, holds, then eases out
        // over the last part of the flight. Nothing pops in at full alpha.
        float spawnT = Math.min(1.0f, age / 3.0f);
        float spawnFade = spawnT * spawnT * (3.0f - 2.0f * spawnT);
        float endT = age > 32.0f ? Math.min(1.0f, (age - 32.0f) / 8.0f) : 0.0f;
        final float bodyFade = spawnFade * (1.0f - endT * endT * (3.0f - 2.0f * endT));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Tapered shaft: narrow at the butt, wider at the neck under the diamond. Not a rod.
            drawTaper(matrix, buffer, 0.055f, 0.13f, -2.0f, 1.35f, 12, 0.22f, 0.02f, 0.08f, 0.95f * bodyFade);
            drawTaper(matrix, buffer, 0.022f, 0.048f, -1.85f, 1.22f, 8, 1.0f, 0.55f, 0.68f, 1.0f * bodyFade);

            // Diamond head: crimson edge, pale cutting tip, and a hotter core inside it.
            drawSpearHead(matrix, buffer, 0.42f, 1.2f, 2.85f,
                0.95f, 0.05f, 0.20f, 0.95f * bodyFade,
                1.0f, 0.85f, 0.90f, 1.0f * bodyFade);
            drawSpearHead(matrix, buffer, 0.16f, 1.55f, 2.68f,
                1.0f, 0.72f, 0.80f, bodyFade,
                1.0f, 0.96f, 0.98f, bodyFade);

            // 3. Barbed Crossguards & 4 Flared Demonic Aerodynamic Fins
            drawFin(matrix, buffer, 0.55f, 0, -2.0f, -0.9f, 0.85f, 0.0f, 0.15f, 0.90f * bodyFade);
            drawFin(matrix, buffer, -0.55f, 0, -2.0f, -0.9f, 0.85f, 0.0f, 0.15f, 0.90f * bodyFade);
            drawFin(matrix, buffer, 0, 0.55f, -2.0f, -0.9f, 0.85f, 0.0f, 0.15f, 0.90f * bodyFade);
            drawFin(matrix, buffer, 0, -0.55f, -2.0f, -0.9f, 0.85f, 0.0f, 0.15f, 0.90f * bodyFade);

            // Barbed rear flukes at base of spearhead
            drawFin(matrix, buffer, 0.35f, 0, 1.2f, 0.7f, 1.0f, 0.1f, 0.25f, 0.95f * bodyFade);
            drawFin(matrix, buffer, -0.35f, 0, 1.2f, 0.7f, 1.0f, 0.1f, 0.25f, 0.95f * bodyFade);

            // Mach cone around the head. Pale at the tip, crimson at the mouth, with a thin rim.
            int coneSegments = com.dragonblockarcanedba.util.FxEnvelope.segments(28, ds, 14);
            float coneTipZ = 2.85f;
            float coneBaseZ = coneTipZ - 2.55f;
            float coneRadius = 0.92f;
            for (int i = 0; i < coneSegments; i++) {
                double a1 = (i / (double) coneSegments) * Math.PI * 2.0;
                double a2 = ((i + 1) / (double) coneSegments) * Math.PI * 2.0;
                float x1 = (float) Math.cos(a1) * coneRadius;
                float y1 = (float) Math.sin(a1) * coneRadius;
                float x2 = (float) Math.cos(a2) * coneRadius;
                float y2 = (float) Math.sin(a2) * coneRadius;
                drawTriangle(matrix, buffer, 0, 0, coneTipZ, x1, y1, coneBaseZ, x2, y2, coneBaseZ,
                    1.0f, 0.12f, 0.24f, 0.42f * bodyFade);
            }
            drawRing(matrix, buffer, 0, 0, coneBaseZ, coneRadius * 1.06f, coneRadius * 0.84f,
                com.dragonblockarcanedba.util.FxEnvelope.segments(24, ds, 12),
                1.0f, 0.72f, 0.78f, 0.55f * bodyFade);

            // Blood ribbons trailing the throw: two dense strands plus a
            // third wide, faint wisp so the trail has layered depth (core,
            // mid, outer) instead of two equal ribbons. Width sits across
            // the helix.
            int ribbonSteps = 14;
            float trailStart = -2.0f;
            float trailLength = 4.2f;

            for (int strand = 0; strand < 3; strand++) {
                boolean wispy = strand == 2;
                float strandOffset = strand * (float) Math.PI * (wispy ? 0.5f : 1.0f);
                float strandRadiusMul = wispy ? 1.35f : 1.0f;
                float strandAlphaMul = wispy ? 0.32f : 1.0f;
                for (int s = 0; s < ribbonSteps; s++) {
                    float t1 = s / (float) ribbonSteps;
                    float t2 = (s + 1) / (float) ribbonSteps;

                    float z1 = trailStart - (t1 * trailLength);
                    float z2 = trailStart - (t2 * trailLength);

                    float r1 = (0.25f + t1 * 0.95f) * strandRadiusMul;
                    float r2 = (0.25f + t2 * 0.95f) * strandRadiusMul;

                    double ang1 = strandOffset + (t1 * Math.PI * 2.2) + (age * 0.3);
                    double ang2 = strandOffset + (t2 * Math.PI * 2.2) + (age * 0.3);

                    float rx1 = (float) Math.cos(ang1) * r1;
                    float ry1 = (float) Math.sin(ang1) * r1;
                    float rx2 = (float) Math.cos(ang2) * r2;
                    float ry2 = (float) Math.sin(ang2) * r2;

                    float rw1 = 0.18f * (1.0f - t1 * 0.6f);
                    float rw2 = 0.18f * (1.0f - t2 * 0.6f);
                    float px1 = (float) -Math.sin(ang1) * rw1;
                    float py1 = (float) Math.cos(ang1) * rw1;
                    float px2 = (float) -Math.sin(ang2) * rw2;
                    float py2 = (float) Math.cos(ang2) * rw2;
                    float tail = 1.0f - t1;
                    tail = tail * tail;

                    drawQuad(matrix, buffer,
                        rx1 - px1, ry1 - py1, z1,
                        rx1 + px1, ry1 + py1, z1,
                        rx2 + px2, ry2 + py2, z2,
                        rx2 - px2, ry2 - py2, z2,
                        1.0f, 0.10f + t1 * 0.25f, 0.22f, 0.85f * tail * bodyFade * strandAlphaMul
                    );
                }
            }

            // One wake ring eases back off the spear and dies.
            float wake = (age * 0.12f) % 1.0f;
            float wakeEase = wake * wake * (3.0f - 2.0f * wake);
            float ringZ = trailStart - wakeEase * trailLength;
            float ringRadius = 0.28f + wakeEase * 0.7f;
            float ringAlpha = (float) Math.sin(wake * Math.PI) * 0.5f * bodyFade;
            drawRing(matrix, buffer, 0, 0, ringZ, ringRadius, ringRadius * 0.78f, 20, 0.95f, 0.05f, 0.20f, ringAlpha);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawTaper(Matrix4f matrix, VertexConsumer consumer, float radiusStart, float radiusEnd, float zStart, float zEnd, int sides, float r, float g, float b, float a) {
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2;

            float x1s = (float) Math.cos(a1) * radiusStart;
            float y1s = (float) Math.sin(a1) * radiusStart;
            float x2s = (float) Math.cos(a2) * radiusStart;
            float y2s = (float) Math.sin(a2) * radiusStart;
            float x1e = (float) Math.cos(a1) * radiusEnd;
            float y1e = (float) Math.sin(a1) * radiusEnd;
            float x2e = (float) Math.cos(a2) * radiusEnd;
            float y2e = (float) Math.sin(a2) * radiusEnd;

            consumer.addVertex(matrix, x1s, y1s, zStart).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2s, y2s, zStart).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2e, y2e, zEnd).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1e, y1e, zEnd).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawSpearHead(Matrix4f matrix, VertexConsumer consumer, float width, float zBase, float zTip,
                                      float r, float g, float b, float a,
                                      float tr, float tg, float tb, float ta) {
        // 4-sided diamond. The base is the crimson edge, the tip is the pale core.
        float[][] pts = { { width, 0 }, { 0, width }, { -width, 0 }, { 0, -width } };
        for (int i = 0; i < 4; i++) {
            int nxt = (i + 1) % 4;
            consumer.addVertex(matrix, pts[i][0], pts[i][1], zBase).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, pts[nxt][0], pts[nxt][1], zBase).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, 0, 0, zTip).setColor(tr, tg, tb, ta).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, 0, 0, zTip).setColor(tr, tg, tb, ta).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    /** Curved barb: dark root, bright outer edge, a taper to the tip, and a short wake behind the root. */
    private static void drawFin(Matrix4f matrix, VertexConsumer consumer, float widthX, float widthY, float zStart, float zEnd, float r, float g, float b, float a) {
        float span = (float) Math.sqrt(widthX * widthX + widthY * widthY);
        float tx = 0.0f;
        float ty = 1.0f;
        if (span > 1.0e-4f) {
            tx = -widthY / span;
            ty = widthX / span;
        }
        int steps = 7;
        for (int i = 0; i < steps; i++) {
            float t1 = i / (float) steps;
            float t2 = (i + 1) / (float) steps;
            float bow1 = (float) Math.sin(t1 * Math.PI);
            float bow2 = (float) Math.sin(t2 * Math.PI);
            float reach1 = (1.0f - t1) * (0.4f + 0.6f * bow1);
            float reach2 = (1.0f - t2) * (0.4f + 0.6f * bow2);
            float thick1 = 0.05f * (1.0f - t1);
            float thick2 = 0.05f * (1.0f - t2);
            float z1 = zStart + (zEnd - zStart) * t1;
            float z2 = zStart + (zEnd - zStart) * t2;
            float ox1 = widthX * reach1;
            float oy1 = widthY * reach1;
            float ox2 = widthX * reach2;
            float oy2 = widthY * reach2;
            float er = r + (1.0f - r) * 0.55f;
            float eg = g + (0.85f - g) * 0.55f;
            float eb = b + (0.9f - b) * 0.55f;
            consumer.addVertex(matrix, 0, 0, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
            consumer.addVertex(matrix, ox1, oy1, z1).setColor(er, eg, eb, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
            consumer.addVertex(matrix, ox2, oy2, z2).setColor(er, eg, eb, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
            consumer.addVertex(matrix, 0, 0, z2).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
            consumer.addVertex(matrix, ox1, oy1, z1).setColor(er, eg, eb, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ox1 + tx * thick1, oy1 + ty * thick1, z1).setColor(1.0f, 0.9f, 0.92f, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ox2 + tx * thick2, oy2 + ty * thick2, z2).setColor(1.0f, 0.9f, 0.92f, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ox2, oy2, z2).setColor(er, eg, eb, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
        float wakeZ = zStart + Math.copySign(0.34f, zStart - zEnd);
        consumer.addVertex(matrix, 0, 0, zStart).setColor(r, g, b, a * 0.5f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, widthX * 0.28f, widthY * 0.28f, zStart).setColor(r, g, b, a * 0.5f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, widthX * 0.05f, widthY * 0.05f, wakeZ).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, 0, 0, wakeZ).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
    }

    private static void drawTriangle(Matrix4f matrix, VertexConsumer consumer,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float x3, float y3, float z3,
                                     float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawQuad(Matrix4f matrix, VertexConsumer consumer,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Reverse
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }

    private static void drawRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1Out = cx + (float) Math.cos(a1) * rOuter;
            float y1Out = cy + (float) Math.sin(a1) * rOuter;
            float x2Out = cx + (float) Math.cos(a2) * rOuter;
            float y2Out = cy + (float) Math.sin(a2) * rOuter;

            float x1In = cx + (float) Math.cos(a1) * rInner;
            float y1In = cy + (float) Math.sin(a1) * rInner;
            float x2In = cx + (float) Math.cos(a2) * rInner;
            float y2In = cy + (float) Math.sin(a2) * rInner;

            drawQuad(matrix, consumer, x1In, y1In, cz, x2In, y2In, cz, x2Out, y2Out, cz, x1Out, y1Out, cz, r, g, b, a);
        }
    }

}
