package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.ErasureCannonBeamEntity;
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
 * Entity Renderer for Erasure Cannon Beam in MC 26.2.
 * Renders a massive, continuous physical 3D geometric laser cylinder across the 48-meter distance:
 * - Multi-layered synchrotron laser core (superdense white core, 12-sided cyan plasma shroud, outer hex barrier)
 * - Quad counter-rotating helical synchrotron drill ribbons spanning the entire 48m raycast length
 * - Stationed hexagonal focusing aperture rings along the beam
 * - Muzzle supercharge emitter geometry with compression blades
 * - Terminus vaporization hemisphere dome, cross shock rings, and radiating splash fins
 */
public class ErasureCannonBeamRenderer extends EntityRenderer<ErasureCannonBeamEntity, ErasureCannonBeamRenderer.ErasureCannonRenderState> {

    public ErasureCannonBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ErasureCannonRenderState extends EntityRenderState {
        public float length = 48.0f;
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float chargeRatio = 1.0f;
        public float age = 0.0f;
    }

    @Override
    public boolean shouldRender(ErasureCannonBeamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public ErasureCannonRenderState createRenderState() {
        return new ErasureCannonRenderState();
    }

    @Override
    public void extractRenderState(ErasureCannonBeamEntity entity, ErasureCannonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.length = entity.getBeamLength();
        state.yRot = entity.getBeamYRot();
        state.xRot = entity.getBeamXRot();
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(ErasureCannonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float beamLength = state.length;
        if (beamLength < 0.2f) return;

        float charge = state.chargeRatio;
        float age = state.age;
        float maxLifetime = ErasureCannonBeamEntity.MAX_LIFETIME;
        float progress = Math.min(1.0f, age / maxLifetime);

        // Rapid initial scale-in followed by steady beam and smooth alpha dissipation
        float scaleIn = Math.min(1.0f, age / 2.0f);
        float alpha = Math.max(0.0f, 1.0f - (float) Math.pow(progress, 1.8f));

        float pulse = (1.0f + 0.06f * (float) Math.sin(age * 1.8f)) * scaleIn;
        float baseRadius = (0.35f + charge * 0.45f) * pulse;

        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Multi-Layered Continuous 3D Cylindrical Laser Core
            // Layer A: Superdense White-Hot Inner Core Cylinder (12 sides)
            float innerRadius = baseRadius * 0.30f;
            drawPrismBeam(matrix, buffer, innerRadius, beamLength, 12, age * 6.0f, 1.0f, 1.0f, 1.0f, alpha * 0.98f);

            // Layer B: Fluted Radiant Cyan Synchrotron Plasma Shroud (12 sides)
            float midRadius = baseRadius * 0.65f;
            drawPrismBeam(matrix, buffer, midRadius, beamLength, 12, -age * 10.0f, 0.0f, 0.95f, 1.0f, alpha * 0.75f);

            // Layer C: Hexagonal Outer Synchrotron Containment Sheath (6 sides)
            float outerRadius = baseRadius * 1.05f;
            drawPrismBeam(matrix, buffer, outerRadius, beamLength, 6, age * 4.0f, 0.05f, 0.70f, 1.0f, alpha * 0.35f);

            // 2. Quad Helical Synchrotron Energy Drill Ribbons
            int segments = Math.min((int) (beamLength * 3.5f), 128);
            float spiralRadius = baseRadius * 1.25f;
            float ribbonSize = 0.075f * pulse;
            float spiralSpeed = age * 0.45f;

            for (int h = 0; h < 4; h++) {
                float phase = h * ((float) Math.PI * 0.5f);
                boolean isWhite = (h % 2 == 1);
                float rCol = isWhite ? 1.0f : 0.0f;
                float gCol = isWhite ? 1.0f : 0.92f;
                float bCol = isWhite ? 0.95f : 1.0f;
                float aCol = (isWhite ? 0.85f : 0.70f) * alpha;

                for (int i = 0; i < segments; i++) {
                    float z1 = (i / (float) segments) * beamLength;
                    float z2 = ((i + 1) / (float) segments) * beamLength;

                    float p1 = z1 / beamLength;
                    float p2 = z2 / beamLength;

                    double a1 = (p1 * Math.PI * 10.0) + (spiralSpeed * (isWhite ? -1.0f : 1.0f)) + phase;
                    double a2 = (p2 * Math.PI * 10.0) + (spiralSpeed * (isWhite ? -1.0f : 1.0f)) + phase;

                    float x1 = (float) Math.cos(a1) * spiralRadius;
                    float y1 = (float) Math.sin(a1) * spiralRadius;
                    float x2 = (float) Math.cos(a2) * spiralRadius;
                    float y2 = (float) Math.sin(a2) * spiralRadius;

                    drawQuad(matrix, buffer,
                        x1 - ribbonSize, y1 - ribbonSize, z1,
                        x1 + ribbonSize, y1 + ribbonSize, z1,
                        x2 + ribbonSize, y2 + ribbonSize, z2,
                        x2 - ribbonSize, y2 - ribbonSize, z2,
                        rCol, gCol, bCol, aCol
                    );
                }
            }

            // 3. Stationed Hexagonal Focusing Aperture Rings (along beam length)
            int ringCount = Math.max(4, (int) (beamLength / 5.0f));
            for (int k = 1; k <= ringCount; k++) {
                float z = (k / (float) (ringCount + 1)) * beamLength;
                float ringPhase = (float) Math.sin(age * 0.5f + k * 1.1f);
                float ringR = (baseRadius * 1.35f + ringPhase * 0.08f);

                drawPlaneRing(matrix, buffer, 0, 0, z, ringR, ringR * 0.82f, 16, age * (20.0f + k * 6.0f),
                    0.0f, 0.95f, 1.0f, alpha * 0.80f);
            }

            // 4. Muzzle Supercharge Emitter Array (at Z=0)
            drawMuzzleEmitter(matrix, buffer, age, pulse, alpha);

            // 5. Terminus Impact Singularity & Vaporization Dome (at Z=beamLength)
            drawTerminusImpact(matrix, buffer, beamLength, age, pulse, alpha, charge);
        });

        poseStack.popPose();
    }

    private static void drawPrismBeam(Matrix4f matrix, VertexConsumer consumer, float radius, float length, int sides, float rotDeg, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2.0 + rotRad;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2.0 + rotRad;

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            drawQuad(matrix, consumer,
                x1, y1, 0,
                x2, y2, 0,
                x2, y2, length,
                x1, y1, length,
                r, g, b, a
            );
        }
    }

    private static void drawPlaneRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0 + rotRad;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0 + rotRad;

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

    private static void drawMuzzleEmitter(Matrix4f matrix, VertexConsumer consumer, float age, float pulse, float alpha) {
        // Concentric rotating aperture muzzle seals
        drawPlaneRing(matrix, consumer, 0, 0, 0.15f, 0.90f * pulse, 0.70f * pulse, 16, age * 35.0f, 0.0f, 0.90f, 1.0f, alpha * 0.90f);
        drawPlaneRing(matrix, consumer, 0, 0, 0.22f, 0.65f * pulse, 0.45f * pulse, 12, -age * 55.0f, 1.0f, 1.0f, 1.0f, alpha * 0.95f);

        // 4 Inward compression blades
        for (int s = 0; s < 4; s++) {
            double sAngle = (s / 4.0) * Math.PI * 2.0 + Math.toRadians(age * 22.0f);
            float sx = (float) Math.cos(sAngle) * 1.10f * pulse;
            float sy = (float) Math.sin(sAngle) * 1.10f * pulse;

            drawQuad(matrix, consumer,
                sx - 0.08f, sy - 0.08f, 0.05f,
                sx + 0.08f, sy + 0.08f, 0.05f,
                0, 0, 0.60f,
                0, 0, 0.60f,
                1.0f, 1.0f, 1.0f, alpha * 0.95f
            );
        }
    }

    private static void drawTerminusImpact(Matrix4f matrix, VertexConsumer consumer, float beamLength, float age, float pulse, float alpha, float charge) {
        float domeRadius = (1.2f + charge * 0.8f) * pulse;
        int segments = 16;

        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = (float) Math.cos(a1) * domeRadius;
            float y1 = (float) Math.sin(a1) * domeRadius;
            float x2 = (float) Math.cos(a2) * domeRadius;
            float y2 = (float) Math.sin(a2) * domeRadius;

            // Outer flared impact dome
            drawQuad(matrix, consumer,
                x1 * 0.4f, y1 * 0.4f, beamLength - 0.4f,
                x2 * 0.4f, y2 * 0.4f, beamLength - 0.4f,
                x2, y2, beamLength,
                x1, y1, beamLength,
                0.0f, 0.95f, 1.0f, alpha * 0.85f
            );

            // Center dense white vaporization flash point
            drawQuad(matrix, consumer,
                x1 * 0.35f, y1 * 0.35f, beamLength - 0.15f,
                x2 * 0.35f, y2 * 0.35f, beamLength - 0.15f,
                x2 * 0.35f, y2 * 0.35f, beamLength + 0.15f,
                x1 * 0.35f, y1 * 0.35f, beamLength + 0.15f,
                1.0f, 1.0f, 1.0f, alpha * 0.98f
            );
        }

        // 6 Radiating impact vaporization splash fins
        int finCount = 6;
        for (int f = 0; f < finCount; f++) {
            double fAngle = (f / (double) finCount) * Math.PI * 2.0 + Math.toRadians(age * 30.0f);
            float fx = (float) Math.cos(fAngle) * domeRadius * 1.35f;
            float fy = (float) Math.sin(fAngle) * domeRadius * 1.35f;

            drawQuad(matrix, consumer,
                0, 0, beamLength,
                fx * 0.4f, fy * 0.4f, beamLength - 0.3f,
                fx, fy, beamLength + 0.15f,
                0, 0, beamLength,
                1.0f, 1.0f, 1.0f, alpha * 0.88f
            );
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

        // Reverse
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
