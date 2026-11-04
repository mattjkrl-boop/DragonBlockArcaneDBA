package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.BanshoCycloneEntity;
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
 * Entity Renderer for Bansho Fan Cyclone in Minecraft 26.2.
 * The funnel wall is the shared layered vortex (LayeredRender.drawVortexBands):
 * a bright core band, an emerald mid band, and a wispy outer band whose edge
 * sits exactly on the cyclone's 4 block effect radius, so what you see is
 * what hurts you. The bands are drawn double sided by the shared helper, and
 * this renderer's own quads are double sided too, so the funnel reads from
 * inside the storm. Around the bands: the suction ring and eye at the foot,
 * the triple helical jade ribbons, 8 orbiting razor blades, and the crown
 * outflow ring, all breathing on the same FxEnvelope as the entity.
 */
public class BanshoCycloneRenderer extends EntityRenderer<BanshoCycloneEntity, BanshoCycloneRenderer.CycloneRenderState> {

    public BanshoCycloneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CycloneRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float age = 0;
        public int lifetime = BanshoCycloneEntity.LIFE_TICKS;
        public double ex, ey, ez;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BanshoCycloneEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BanshoCycloneEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public CycloneRenderState createRenderState() {
        return new CycloneRenderState();
    }

    @Override
    public void extractRenderState(BanshoCycloneEntity entity, CycloneRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getScale();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
    }

    @Override
    public void submit(CycloneRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float scale = state.scale;
        float effectRadius = scale * 4.0f;
        float fullHeight = scale * 6.5f;
        float age = state.age;
        float life = Math.max(1, state.lifetime);

        // One envelope for everything, the same fractions the entity uses for
        // its damage window and forces, so visual and numbers agree.
        float envelope = FxEnvelope.envelope(age, life, BanshoCycloneEntity.BUILD_FRAC, BanshoCycloneEntity.DECAY_FRAC);
        if (envelope <= 0.01f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }
        float growth = FxEnvelope.smoothstep(age / (life * BanshoCycloneEntity.BUILD_FRAC));
        float radiusNow = effectRadius * (0.45f + 0.55f * growth);
        float heightNow = fullHeight * (0.35f + 0.65f * growth);
        float distScale = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);

        final float finalAlphaMult = envelope;

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Ground anchor glow: the storm is planted on the world, not pasted on it.
            LayeredRender.drawAnchorGlow(pose, buffer, radiusNow * 1.05f, 0.06f,
                0.15f, 1.0f, 0.65f, 0.30f * finalAlphaMult, distScale);

            // The funnel itself: three counter-rotating layered bands. The mid
            // band radius is chosen so the wispy outer band (1.45x) lands
            // exactly on the effect radius: the visible wall IS the hit wall.
            LayeredRender.drawVortexBands(pose, buffer, radiusNow / 1.45f, heightNow, age,
                0.15f, 1.0f, 0.65f, finalAlphaMult, distScale);

            // Suction ring at the foot, and a pale eye that is the core of that ring.
            int groundSegments = FxEnvelope.segments(36, distScale, 12);
            float gRadius = radiusNow * 0.95f;
            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, gRadius, gRadius * 0.78f, groundSegments, age * 28.0f,
                0.0f, 0.72f, 0.38f, 0.70f * finalAlphaMult);
            drawRotatingRing(matrix, buffer, 0, 0.08f, 0, gRadius * 0.22f, 0.04f, 20, -age * 18.0f,
                0.85f, 1.0f, 0.92f, 0.80f * finalAlphaMult);

            // Ascending Triple-Helical Jade Wind Ribbons, riding the funnel wall.
            int strands = 3;
            int ribbonSteps = 22;
            for (int s = 0; s < strands; s++) {
                float strandOffset = (s / (float) strands) * (float) (Math.PI * 2.0);

                for (int step = 0; step < ribbonSteps; step++) {
                    float t1 = step / (float) ribbonSteps;
                    float t2 = (step + 1) / (float) ribbonSteps;

                    float y1 = t1 * heightNow;
                    float y2 = t2 * heightNow;

                    float r1 = radiusNow * (0.35f + 0.65f * t1 * t1);
                    float r2 = radiusNow * (0.35f + 0.65f * t2 * t2);

                    double ang1 = strandOffset + (age * 22.0f * (Math.PI / 180.0)) + (t1 * Math.PI * 3.8);
                    double ang2 = strandOffset + (age * 22.0f * (Math.PI / 180.0)) + (t2 * Math.PI * 3.8);

                    float x1 = (float) Math.cos(ang1) * r1;
                    float z1 = (float) Math.sin(ang1) * r1;
                    float x2 = (float) Math.cos(ang2) * r2;
                    float z2 = (float) Math.sin(ang2) * r2;

                    float bandWidth1 = 0.38f * scale * (0.6f + t1 * 0.7f);
                    float bandWidth2 = 0.38f * scale * (0.6f + t2 * 0.7f);

                    float tx1 = (float) -Math.sin(ang1) * bandWidth1;
                    float tz1 = (float) Math.cos(ang1) * bandWidth1;
                    float tx2 = (float) -Math.sin(ang2) * bandWidth2;
                    float tz2 = (float) Math.cos(ang2) * bandWidth2;

                    // Gradient along the climb (T2): deep jade at the base
                    // brightening to pale jade at the top of the strand.
                    drawQuad(matrix, buffer,
                        x1 - tx1, y1, z1 - tz1,
                        x1 + tx1, y1, z1 + tz1,
                        x2 + tx2, y2, z2 + tz2,
                        x2 - tx2, y2, z2 - tz2,
                        0.45f * t1, 1.0f, 0.48f + 0.34f * t1, 0.60f * finalAlphaMult
                    );
                }
            }

            // Physical Orbiting Razor Wind Scythes / Kinetic Gale Blades (8 blades)
            int bladeCount = 8;
            for (int i = 0; i < bladeCount; i++) {
                float bladeProgress = (i / (float) bladeCount);
                float bladeY = bladeProgress * heightNow * 0.85f + 0.4f;
                float bladeRadius = radiusNow * (0.45f + 0.55f * (bladeY / Math.max(0.001f, heightNow)));

                double bladeAngle = (bladeProgress * Math.PI * 2.0) + (age * (32.0f + i * 3.0f) * (Math.PI / 180.0));

                float sx = (float) Math.cos(bladeAngle) * bladeRadius;
                float sz = (float) Math.sin(bladeAngle) * bladeRadius;

                // Tangent orientation
                float tx = (float) -Math.sin(bladeAngle);
                float tz = (float) Math.cos(bladeAngle);

                float sLen = 0.75f * scale;
                float tipX = sx + tx * (sLen * 0.65f);
                float tipZ = sz + tz * (sLen * 0.65f);
                float tailX = sx - tx * (sLen * 0.35f);
                float tailZ = sz - tz * (sLen * 0.35f);
                float radX = (float) Math.cos(bladeAngle);
                float radZ = (float) Math.sin(bladeAngle);
                drawLash(matrix, buffer,
                    tailX, bladeY, tailZ,
                    sx + radX * 0.28f * scale, bladeY + 0.22f * scale, sz + radZ * 0.28f * scale,
                    tipX, bladeY + 0.04f * scale, tipZ,
                    0.10f * scale, 6,
                    0.15f, 0.78f, 0.48f,
                    0.88f, 1.0f, 0.95f,
                    0.90f * finalAlphaMult
                );
            }

            // Upper Crown Outflow Rim (Flared top storm rim)
            float crownY = heightNow;
            float crownR = radiusNow * 1.12f;
            drawRotatingRing(matrix, buffer, 0, crownY, 0, crownR, crownR * 0.72f, 30, -age * 26.0f,
                0.05f, 0.95f, 0.65f, 0.50f * finalAlphaMult);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawLash(Matrix4f matrix, VertexConsumer consumer,
                                 float x0, float y0, float z0,
                                 float xc, float yc, float zc,
                                 float x1, float y1, float z1,
                                 float width, int steps,
                                 float er, float eg, float eb,
                                 float cr, float cg, float cb,
                                 float alpha) {
        int n = Math.max(4, steps);
        float px = x0;
        float py = y0;
        float pz = z0;
        for (int i = 1; i <= n; i++) {
            float t = i / (float) n;
            float u = 1.0f - t;
            float x = u * u * x0 + 2.0f * u * t * xc + t * t * x1;
            float y = u * u * y0 + 2.0f * u * t * yc + t * t * y1;
            float z = u * u * z0 + 2.0f * u * t * zc + t * t * z1;
            float w = width * (1.0f - t * 0.9f);
            float a = alpha * (1.0f - t * t);
            lashSpan(matrix, consumer, px, py, pz, x, y, z, w, er, eg, eb, a);
            lashSpan(matrix, consumer, px, py, pz, x, y, z, w * 0.32f, cr, cg, cb, Math.min(1.0f, a * 1.2f));
            px = x;
            py = y;
            pz = z;
        }
    }

    private static void lashSpan(Matrix4f matrix, VertexConsumer consumer,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float width,
                                 float r, float g, float b, float a) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float ax = 0.0f;
        float ay = 1.0f;
        float az = 0.0f;
        if (dx * dx + dz * dz < dy * dy) {
            ax = 1.0f;
            ay = 0.0f;
            az = 0.0f;
        }
        float px = dy * az - dz * ay;
        float py = dz * ax - dx * az;
        float pz = dx * ay - dy * ax;
        float plen = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (plen < 0.0001f) {
            return;
        }
        float s = width / plen;
        px *= s;
        py *= s;
        pz *= s;
        consumer.addVertex(matrix, x0 - px, y0 - py, z0 - pz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x0 + px, y0 + py, z0 + pz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 + px, y1 + py, z1 + pz).setColor(r, g, b, a * 0.65f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 - px, y1 - py, z1 - pz).setColor(r, g, b, a * 0.65f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 - px, y1 - py, z1 - pz).setColor(r, g, b, a * 0.65f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x1 + px, y1 + py, z1 + pz).setColor(r, g, b, a * 0.65f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x0 + px, y0 + py, z0 + pz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x0 - px, y0 - py, z0 - pz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
    }

    private static void drawRotatingRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        double rotRad = rotDeg * (Math.PI / 180.0);
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2.0) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2.0) + rotRad;

            float x1Out = cx + (float) Math.cos(a1) * rOuter;
            float z1Out = cz + (float) Math.sin(a1) * rOuter;
            float x2Out = cx + (float) Math.cos(a2) * rOuter;
            float z2Out = cz + (float) Math.sin(a2) * rOuter;

            float x1In = cx + (float) Math.cos(a1) * rInner;
            float z1In = cz + (float) Math.sin(a1) * rInner;
            float x2In = cx + (float) Math.cos(a2) * rInner;
            float z2In = cz + (float) Math.sin(a2) * rInner;

            drawQuad(matrix, consumer, x1Out, cy, z1Out, x2Out, cy, z2Out, x2In, cy, z2In, x1In, cy, z1In, r, g, b, a);
        }
    }

    // Double sided: the funnel must read from inside the storm and from below,
    // so every quad is emitted in both windings.
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
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
