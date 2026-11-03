package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.BanshoCycloneEntity;
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
 * Renders physical 3D emerald/jade multi-layered vortex funnels, counter-rotating suction whirlpools,
 * ascending triple-helical jade wind ribbons, 8 high-speed orbiting razor wind blades, and a radiant spindle core.
 */
public class BanshoCycloneRenderer extends EntityRenderer<BanshoCycloneEntity, BanshoCycloneRenderer.CycloneRenderState> {

    public BanshoCycloneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CycloneRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float age = 0;
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
    }

    @Override
    public void submit(CycloneRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float scale = state.scale;
        float baseRadius = scale * 3.5f;
        float totalHeight = scale * 6.5f;
        float age = state.age;

        float inT = Math.min(1.0f, age / 4.0f);
        float inEase = 1.0f - (1.0f - inT) * (1.0f - inT) * (1.0f - inT);
        float outT = age <= 17.0f ? 0.0f : Math.min(1.0f, (age - 17.0f) / 8.0f);
        float alphaMult = inEase * (1.0f - outT * outT * (3.0f - 2.0f * outT));

        final float finalAlphaMult = alphaMult;

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // One suction ring at the foot, and a pale eye that is the core of that ring.
            int groundSegments = 36;
            float gRadius = baseRadius * 0.95f;
            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, gRadius, gRadius * 0.78f, groundSegments, age * 28.0f,
                0.0f, 0.72f, 0.38f, 0.70f * finalAlphaMult);
            drawRotatingRing(matrix, buffer, 0, 0.08f, 0, gRadius * 0.22f, 0.04f, 20, -age * 18.0f,
                0.85f, 1.0f, 0.92f, 0.80f * finalAlphaMult);

            // 2. High-Speed 3D Inverted Tornado Funnel (10-tiered twisting cone)
            int coreLevels = 10;
            int coreSegments = 28;
            float coreRot = age * 30.0f * (float) (Math.PI / 180.0);

            for (int lvl = 0; lvl < coreLevels; lvl++) {
                float p1 = lvl / (float) coreLevels;
                float p2 = (lvl + 1) / (float) coreLevels;

                float y1 = p1 * totalHeight;
                float y2 = p2 * totalHeight;

                // Secondary motion (T4): the funnel wall breathes as it climbs.
                float breathe1 = 1.0f + 0.03f * (float) Math.sin(age * 0.9f + p1 * 5.0f);
                float breathe2 = 1.0f + 0.03f * (float) Math.sin(age * 0.9f + p2 * 5.0f);
                float r1 = (baseRadius * 0.38f) * (0.28f + 0.72f * p1) * breathe1;
                float r2 = (baseRadius * 0.38f) * (0.28f + 0.72f * p2) * breathe2;

                float alpha = (0.65f * (1.0f - p1 * 0.25f)) * finalAlphaMult;

                // 3-stop gradient (T2): deep emerald at the base, full emerald
                // mid-funnel, pale jade at the crown.
                float crF, cgF, cbF;
                if (p1 < 0.5f) {
                    float k = p1 / 0.5f;
                    crF = 0.0f + (0.15f - 0.0f) * k; cgF = 0.82f + (1.0f - 0.82f) * k; cbF = 0.42f + (0.65f - 0.42f) * k;
                } else {
                    float k = (p1 - 0.5f) / 0.5f;
                    crF = 0.15f + (0.55f - 0.15f) * k; cgF = 1.0f; cbF = 0.65f + (0.85f - 0.65f) * k;
                }

                for (int i = 0; i < coreSegments; i++) {
                    double a1 = ((i / (double) coreSegments) * Math.PI * 2.0) + coreRot + (p1 * 1.4);
                    double a2 = (((i + 1) / (double) coreSegments) * Math.PI * 2.0) + coreRot + (p1 * 1.4);
                    double a3 = (((i + 1) / (double) coreSegments) * Math.PI * 2.0) + coreRot + (p2 * 1.4);
                    double a4 = ((i / (double) coreSegments) * Math.PI * 2.0) + coreRot + (p2 * 1.4);

                    float x1 = (float) Math.cos(a1) * r1;
                    float z1 = (float) Math.sin(a1) * r1;
                    float x2 = (float) Math.cos(a2) * r1;
                    float z2 = (float) Math.sin(a2) * r1;

                    float x3 = (float) Math.cos(a3) * r2;
                    float z3 = (float) Math.sin(a3) * r2;
                    float x4 = (float) Math.cos(a4) * r2;
                    float z4 = (float) Math.sin(a4) * r2;

                    drawQuad(matrix, buffer, x1, y1, z1, x2, y1, z2, x3, y2, z3, x4, y2, z4,
                        crF, cgF, cbF, alpha);
                }
            }

            // 3. Ascending Triple-Helical Jade Wind Ribbons
            int strands = 3;
            int ribbonSteps = 22;
            for (int s = 0; s < strands; s++) {
                float strandOffset = (s / (float) strands) * (float) (Math.PI * 2.0);

                for (int step = 0; step < ribbonSteps; step++) {
                    float t1 = step / (float) ribbonSteps;
                    float t2 = (step + 1) / (float) ribbonSteps;

                    float y1 = t1 * totalHeight;
                    float y2 = t2 * totalHeight;

                    float r1 = baseRadius * (0.35f + 0.65f * t1 * t1);
                    float r2 = baseRadius * (0.35f + 0.65f * t2 * t2);

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

            // 4. Physical Orbiting Razor Wind Scythes / Kinetic Gale Blades (8 blades)
            int bladeCount = 8;
            for (int i = 0; i < bladeCount; i++) {
                float bladeProgress = (i / (float) bladeCount);
                float bladeY = bladeProgress * totalHeight * 0.85f + 0.4f;
                float bladeRadius = baseRadius * (0.45f + 0.55f * (bladeY / totalHeight));

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

            // 5. Luminous Tempest Spindle Core (Vertical Glowing Spine)
            int spineLevels = 6;
            for (int s = 0; s < spineLevels; s++) {
                float y1 = (s / (float) spineLevels) * totalHeight;
                float y2 = ((s + 1) / (float) spineLevels) * totalHeight;
                float climb = s / (float) spineLevels;
                float climb2 = (s + 1) / (float) spineLevels;
                float rSpine = 0.20f * scale * (1.0f - climb * 0.62f);
                float rSpine2 = 0.20f * scale * (1.0f - climb2 * 0.62f);
                float rotSpine = age * 50.0f * (float) (Math.PI / 180.0) + s * 0.5f;

                for (int i = 0; i < 8; i++) {
                    double a1 = (i / 8.0) * Math.PI * 2.0 + rotSpine;
                    double a2 = ((i + 1) / 8.0) * Math.PI * 2.0 + rotSpine;

                    float x1 = (float) Math.cos(a1) * rSpine;
                    float z1 = (float) Math.sin(a1) * rSpine;
                    float x2 = (float) Math.cos(a2) * rSpine;
                    float z2 = (float) Math.sin(a2) * rSpine;
                    float x3 = (float) Math.cos(a2) * rSpine2;
                    float z3 = (float) Math.sin(a2) * rSpine2;
                    float x4 = (float) Math.cos(a1) * rSpine2;
                    float z4 = (float) Math.sin(a1) * rSpine2;

                    drawQuad(matrix, buffer,
                        x1, y1, z1,
                        x2, y1, z2,
                        x3, y2, z3,
                        x4, y2, z4,
                        0.55f, 1.0f, 0.78f, 0.55f * finalAlphaMult
                    );
                    drawQuad(matrix, buffer,
                        x1 * 0.38f, y1, z1 * 0.38f,
                        x2 * 0.38f, y1, z2 * 0.38f,
                        x3 * 0.38f, y2, z3 * 0.38f,
                        x4 * 0.38f, y2, z4 * 0.38f,
                        0.95f, 1.0f, 0.98f, 0.90f * finalAlphaMult
                    );
                }
            }

            // 6. Upper Crown Outflow Rim (Flared top storm rim)
            float crownY = totalHeight;
            float crownR = baseRadius * 1.12f;
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
    }

    private static void drawTriangle(Matrix4f matrix, VertexConsumer consumer,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float x3, float y3, float z3,
                                    float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
