package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.ErasureChargeOrbEntity;
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
 * Entity Renderer for Erasure Cannon Muzzle Charge Orb in MC 26.2.
 * Renders an expanding physical 3D geometric energy orb model:
 * - Blinding white-cyan octahedron/icosahedron energy singularity core
 * - Expanding outer geodesic energy cage with counter-rotating polyhedral shells
 * - 3 concentric synchrotron accelerator rings aligned with the gun muzzle axis
 * - 8 inward-converging 3D plasma intake spikes spiraling into the singularity
 * - Helical containment ribbons and overdrive corona discharge arcs
 */
public class ErasureChargeOrbRenderer extends EntityRenderer<ErasureChargeOrbEntity, ErasureChargeOrbRenderer.ErasureChargeRenderState> {

    public ErasureChargeOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ErasureChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float age = 0.0f;
    }

    @Override
    public boolean shouldRender(ErasureChargeOrbEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public ErasureChargeRenderState createRenderState() {
        return new ErasureChargeRenderState();
    }

    @Override
    public void extractRenderState(ErasureChargeOrbEntity entity, ErasureChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(ErasureChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float charge = state.chargeRatio;
        float age = state.age;
        float pulse = 0.92f + 0.08f * (float) Math.sin(age * 0.65f);

        float orbRadius = (0.22f + charge * 0.95f) * pulse;
        float alpha = 0.75f + charge * 0.25f;

        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Inner Energy Singularity (Hyper-dense white-cyan rotating octahedron core)
            float coreRadius = orbRadius * 0.42f;
            drawRotatingOctahedron(matrix, buffer, coreRadius, age * 45.0f, age * 30.0f, 1.0f, 1.0f, 1.0f, 0.98f);

            // 2. Middle Radiant Cyan Plasma Geodesic Sphere
            drawGeodesicSphere(matrix, buffer, orbRadius * 0.75f, age * -25.0f, 12, 0.0f, 0.95f, 1.0f, alpha * 0.85f);

            // 3. Outer Hexagonal Synchrotron Energy Cage
            drawGeodesicSphere(matrix, buffer, orbRadius, age * 18.0f, 8, 0.10f, 0.70f, 1.0f, alpha * 0.45f);

            // 4. 3 Concentric Synchrotron Muzzle Accelerator Rings (perpendicular to look vector)
            int ringSegments = 24;
            // Outer Ring
            drawPlaneRing(matrix, buffer, 0, 0, 0, orbRadius * 1.45f, orbRadius * 1.25f, ringSegments, age * 35.0f,
                0.0f, 0.90f, 1.0f, alpha * 0.85f);
            // Middle Counter-rotating Ring
            drawPlaneRing(matrix, buffer, 0, 0, 0.05f, orbRadius * 1.20f, orbRadius * 1.05f, ringSegments, age * -50.0f,
                1.0f, 1.0f, 1.0f, alpha * 0.90f);
            // Inner High-Frequency Ring
            drawPlaneRing(matrix, buffer, 0, 0, -0.05f, orbRadius * 0.95f, orbRadius * 0.82f, ringSegments, age * 70.0f,
                0.0f, 0.80f, 1.0f, alpha * 0.75f);

            // 5. 8 Inward-Converging Plasma Intake Spikes (Spiraling and gathering into orb)
            int spikeCount = 8;
            float intakeRadius = orbRadius * (2.2f - charge * 0.6f);
            float intakeSpeed = 30.0f + (charge * 60.0f);

            for (int s = 0; s < spikeCount; s++) {
                double angle = (s / (double) spikeCount) * Math.PI * 2.0 + Math.toRadians(age * intakeSpeed);
                float sx = (float) Math.cos(angle) * intakeRadius;
                float sy = (float) Math.sin(angle) * intakeRadius;
                float sz = (float) Math.sin(age * 0.4f + s * 1.2f) * (orbRadius * 0.6f);

                float toCenterX = -sx;
                float toCenterY = -sy;
                float toCenterZ = -sz;
                float len = (float) Math.sqrt(toCenterX * toCenterX + toCenterY * toCenterY + toCenterZ * toCenterZ);
                if (len > 0.001f) {
                    toCenterX /= len;
                    toCenterY /= len;
                    toCenterZ /= len;
                }

                float spikeLen = orbRadius * 0.7f;
                float tipX = sx + toCenterX * spikeLen;
                float tipY = sy + toCenterY * spikeLen;
                float tipZ = sz + toCenterZ * spikeLen;

                float baseWidth = 0.05f + (charge * 0.04f);
                drawQuad(matrix, buffer,
                    sx - baseWidth, sy - baseWidth, sz,
                    sx + baseWidth, sy + baseWidth, sz,
                    tipX, tipY, tipZ,
                    tipX, tipY, tipZ,
                    0.0f, 0.95f, 1.0f, alpha * 0.90f
                );
            }

            // 6. Overdrive Corona Discharge Arcs (At high charge >= 0.75)
            if (charge >= 0.75f) {
                float overdriveRatio = (charge - 0.75f) / 0.25f;
                int arcCount = 6;
                for (int a = 0; a < arcCount; a++) {
                    double arcAngle = (a / (double) arcCount) * Math.PI * 2.0 + Math.toRadians(age * 80.0f);
                    float ax = (float) Math.cos(arcAngle) * orbRadius * (1.2f + overdriveRatio * 0.6f);
                    float ay = (float) Math.sin(arcAngle) * orbRadius * (1.2f + overdriveRatio * 0.6f);

                    drawQuad(matrix, buffer,
                        ax * 0.6f, ay * 0.6f, 0,
                        ax * 0.6f + 0.04f, ay * 0.6f + 0.04f, 0,
                        ax, ay, 0.1f,
                        ax, ay, -0.1f,
                        1.0f, 1.0f, 1.0f, 0.95f
                    );
                }
            }
        });

        poseStack.popPose();
    }

    private static void drawRotatingOctahedron(Matrix4f matrix, VertexConsumer consumer, float r, float yawDeg, float pitchDeg, float red, float green, float blue, float alpha) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);

        // 6 Vertices of an octahedron
        float topX = 0, topY = r, topZ = 0;
        float botX = 0, botY = -r, botZ = 0;

        float p1X = r, p1Y = 0, p1Z = 0;
        float p2X = 0, p2Y = 0, p2Z = r;
        float p3X = -r, p3Y = 0, p3Z = 0;
        float p4X = 0, p4Y = 0, p4Z = -r;

        // Draw 8 triangular faces
        drawTriangle(matrix, consumer, topX, topY, topZ, p1X, p1Y, p1Z, p2X, p2Y, p2Z, red, green, blue, alpha);
        drawTriangle(matrix, consumer, topX, topY, topZ, p2X, p2Y, p2Z, p3X, p3Y, p3Z, red, green, blue, alpha);
        drawTriangle(matrix, consumer, topX, topY, topZ, p3X, p3Y, p3Z, p4X, p4Y, p4Z, red, green, blue, alpha);
        drawTriangle(matrix, consumer, topX, topY, topZ, p4X, p4Y, p4Z, p1X, p1Y, p1Z, red, green, blue, alpha);

        drawTriangle(matrix, consumer, botX, botY, botZ, p2X, p2Y, p2Z, p1X, p1Y, p1Z, red, green, blue, alpha);
        drawTriangle(matrix, consumer, botX, botY, botZ, p3X, p3Y, p3Z, p2X, p2Y, p2Z, red, green, blue, alpha);
        drawTriangle(matrix, consumer, botX, botY, botZ, p4X, p4Y, p4Z, p3X, p3Y, p3Z, red, green, blue, alpha);
        drawTriangle(matrix, consumer, botX, botY, botZ, p1X, p1Y, p1Z, p4X, p4Y, p4Z, red, green, blue, alpha);
    }

    private static void drawGeodesicSphere(Matrix4f matrix, VertexConsumer consumer, float radius, float rotDeg, int segments, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        int rings = 6;
        for (int i = 0; i < rings; i++) {
            double v1 = (i / (double) rings) * Math.PI - Math.PI / 2.0;
            double v2 = ((i + 1) / (double) rings) * Math.PI - Math.PI / 2.0;

            float y1 = (float) Math.sin(v1) * radius;
            float r1 = (float) Math.cos(v1) * radius;
            float y2 = (float) Math.sin(v2) * radius;
            float r2 = (float) Math.cos(v2) * radius;

            for (int j = 0; j < segments; j++) {
                double u1 = (j / (double) segments) * Math.PI * 2.0 + rotRad;
                double u2 = ((j + 1) / (double) segments) * Math.PI * 2.0 + rotRad;

                float x1 = (float) Math.cos(u1) * r1;
                float z1 = (float) Math.sin(u1) * r1;
                float x2 = (float) Math.cos(u2) * r1;
                float z2 = (float) Math.sin(u2) * r1;

                float x3 = (float) Math.cos(u2) * r2;
                float z3 = (float) Math.sin(u2) * r2;
                float x4 = (float) Math.cos(u1) * r2;
                float z4 = (float) Math.sin(u1) * r2;

                drawQuad(matrix, consumer, x1, y1, z1, x2, y1, z2, x3, y2, z3, x4, y2, z4, r, g, b, a);
            }
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

    private static void drawTriangle(Matrix4f matrix, VertexConsumer consumer,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float x3, float y3, float z3,
                                     float r, float g, float b, float a) {
        drawQuad(matrix, consumer, x1, y1, z1, x2, y2, z2, x3, y3, z3, x1, y1, z1, r, g, b, a);
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

        // Reverse side
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
