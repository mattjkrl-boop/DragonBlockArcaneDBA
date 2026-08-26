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

    @Override
    public boolean shouldRender(BanshoCycloneEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
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

        // Fade in rapidly during first 3 ticks and fade out during last 5 ticks of 25-tick lifetime
        float alphaMult = 1.0f;
        if (age < 3.0f) {
            alphaMult = age / 3.0f;
        } else if (age > 20.0f) {
            alphaMult = Math.max(0.0f, (25.0f - age) / 5.0f);
        }

        final float finalAlphaMult = alphaMult;

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Dual Ground Whirlpool Suction Discs (Base Vortex)
            int groundSegments = 24;
            float gRadius = baseRadius * 0.95f;
            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, gRadius, gRadius * 0.55f, groundSegments, age * 38.0f,
                0.0f, 1.0f, 0.55f, 0.65f * finalAlphaMult);
            drawRotatingRing(matrix, buffer, 0, 0.10f, 0, gRadius * 0.70f, gRadius * 0.25f, groundSegments, -age * 48.0f,
                0.3f, 1.0f, 0.85f, 0.55f * finalAlphaMult);

            // 2. High-Speed 3D Inverted Tornado Funnel (10-tiered twisting cone)
            int coreLevels = 10;
            int coreSegments = 18;
            float coreRot = age * 30.0f * (float) (Math.PI / 180.0);

            for (int lvl = 0; lvl < coreLevels; lvl++) {
                float p1 = lvl / (float) coreLevels;
                float p2 = (lvl + 1) / (float) coreLevels;

                float y1 = p1 * totalHeight;
                float y2 = p2 * totalHeight;

                float r1 = (baseRadius * 0.38f) * (0.28f + 0.72f * p1);
                float r2 = (baseRadius * 0.38f) * (0.28f + 0.72f * p2);

                float alpha = (0.65f * (1.0f - p1 * 0.25f)) * finalAlphaMult;

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
                        0.15f, 1.0f, 0.65f, alpha);
                }
            }

            // 3. Ascending Triple-Helical Jade Wind Ribbons
            int strands = 3;
            int ribbonSteps = 16;
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

                    drawQuad(matrix, buffer,
                        x1 - tx1, y1, z1 - tz1,
                        x1 + tx1, y1, z1 + tz1,
                        x2 + tx2, y2, z2 + tz2,
                        x2 - tx2, y2, z2 - tz2,
                        0.0f, 1.0f, 0.60f, 0.60f * finalAlphaMult
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
                float sWidth = 0.22f * scale;

                float tipX = sx + tx * (sLen * 0.65f);
                float tipZ = sz + tz * (sLen * 0.65f);
                float tailX = sx - tx * (sLen * 0.35f);
                float tailZ = sz - tz * (sLen * 0.35f);

                float nx = -tz * sWidth;
                float nz = tx * sWidth;

                // 3D Diamond Wind Blade
                drawTriangle(matrix, buffer,
                    tailX - nx, bladeY, tailZ - nz,
                    tailX + nx, bladeY, tailZ + nz,
                    tipX, bladeY + 0.12f * scale, tipZ,
                    0.6f, 1.0f, 0.90f, 0.85f * finalAlphaMult
                );
                drawTriangle(matrix, buffer,
                    tailX - nx, bladeY, tailZ - nz,
                    tipX, bladeY + 0.12f * scale, tipZ,
                    tailX + nx, bladeY, tailZ + nz,
                    0.2f, 1.0f, 0.70f, 0.85f * finalAlphaMult
                );
            }

            // 5. Luminous Tempest Spindle Core (Vertical Glowing Spine)
            int spineLevels = 6;
            for (int s = 0; s < spineLevels; s++) {
                float y1 = (s / (float) spineLevels) * totalHeight;
                float y2 = ((s + 1) / (float) spineLevels) * totalHeight;
                float rSpine = 0.22f * scale;
                float rotSpine = age * 50.0f * (float) (Math.PI / 180.0) + s * 0.5f;

                for (int i = 0; i < 4; i++) {
                    double a1 = (i / 4.0) * Math.PI * 2.0 + rotSpine;
                    double a2 = ((i + 1) / 4.0) * Math.PI * 2.0 + rotSpine;

                    float x1 = (float) Math.cos(a1) * rSpine;
                    float z1 = (float) Math.sin(a1) * rSpine;
                    float x2 = (float) Math.cos(a2) * rSpine;
                    float z2 = (float) Math.sin(a2) * rSpine;

                    drawQuad(matrix, buffer,
                        x1, y1, z1,
                        x2, y1, z2,
                        x2, y2, z2,
                        x1, y2, z1,
                        0.95f, 1.0f, 0.98f, 0.80f * finalAlphaMult
                    );
                }
            }

            // 6. Upper Crown Outflow Rim (Flared top storm rim)
            float crownY = totalHeight;
            float crownR = baseRadius * 1.12f;
            drawRotatingRing(matrix, buffer, 0, crownY, 0, crownR, crownR * 0.72f, 20, -age * 26.0f,
                0.05f, 0.95f, 0.65f, 0.50f * finalAlphaMult);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
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
