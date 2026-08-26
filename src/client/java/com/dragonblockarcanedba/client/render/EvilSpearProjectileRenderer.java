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
    }

    @Override
    public boolean shouldRender(EvilSpearProjectileEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
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
    }

    @Override
    public void submit(EvilSpearRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float age = state.age;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * 30.0f)); // Continuous high-speed spiral spin

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Demonic Multi-Faceted Spear Shaft (8-sided outer dark obsidian & inner glowing core)
            drawCylinder(matrix, buffer, 0.10f, -2.0f, 1.4f, 8, 0.22f, 0.02f, 0.08f, 0.95f);
            drawCylinder(matrix, buffer, 0.05f, -1.9f, 1.35f, 6, 1.0f, 0.15f, 0.30f, 1.0f);

            // 2. Multi-Beveled Diamond Blood Spearhead & Inner Burning Core
            drawSpearHead(matrix, buffer, 0.42f, 1.2f, 2.85f, 0.95f, 0.05f, 0.20f, 0.95f);
            drawSpearHead(matrix, buffer, 0.20f, 1.35f, 2.95f, 1.0f, 0.85f, 0.90f, 1.0f);

            // 3. Barbed Crossguards & 4 Flared Demonic Aerodynamic Fins
            drawFin(matrix, buffer, 0.55f, 0, -2.0f, -0.9f, 0.85f, 0.0f, 0.15f, 0.90f);
            drawFin(matrix, buffer, -0.55f, 0, -2.0f, -0.9f, 0.85f, 0.0f, 0.15f, 0.90f);
            drawFin(matrix, buffer, 0, 0.55f, -2.0f, -0.9f, 0.85f, 0.0f, 0.15f, 0.90f);
            drawFin(matrix, buffer, 0, -0.55f, -2.0f, -0.9f, 0.85f, 0.0f, 0.15f, 0.90f);

            // Barbed rear flukes at base of spearhead
            drawFin(matrix, buffer, 0.35f, 0, 1.2f, 0.7f, 1.0f, 0.1f, 0.25f, 0.95f);
            drawFin(matrix, buffer, -0.35f, 0, 1.2f, 0.7f, 1.0f, 0.1f, 0.25f, 0.95f);

            // 4. Supersonic Demonic 3D Mach Shock Cone
            int coneSegments = 16;
            float tipZ = 2.85f;
            float coneLength = 2.6f;
            float coneBaseRadius = 0.95f;
            float baseZ = tipZ - coneLength;

            for (int i = 0; i < coneSegments; i++) {
                double a1 = (i / (double) coneSegments) * Math.PI * 2.0;
                double a2 = ((i + 1) / (double) coneSegments) * Math.PI * 2.0;

                float x1 = (float) Math.cos(a1) * coneBaseRadius;
                float y1 = (float) Math.sin(a1) * coneBaseRadius;
                float x2 = (float) Math.cos(a2) * coneBaseRadius;
                float y2 = (float) Math.sin(a2) * coneBaseRadius;

                drawTriangle(matrix, buffer, 0, 0, tipZ, x1, y1, baseZ, x2, y2, baseZ, 1.0f, 0.08f, 0.22f, 0.50f);
            }

            // 5. Dynamic Physical 3D Trail: Twin Helical Blood Vortex Ribbons trailing behind spear
            int ribbonSteps = 14;
            float trailStart = -2.0f;
            float trailLength = 4.2f;

            for (int strand = 0; strand < 2; strand++) {
                float strandOffset = strand * (float) Math.PI;
                for (int s = 0; s < ribbonSteps; s++) {
                    float t1 = s / (float) ribbonSteps;
                    float t2 = (s + 1) / (float) ribbonSteps;

                    float z1 = trailStart - (t1 * trailLength);
                    float z2 = trailStart - (t2 * trailLength);

                    float r1 = (0.25f + t1 * 0.95f);
                    float r2 = (0.25f + t2 * 0.95f);

                    double ang1 = strandOffset + (t1 * Math.PI * 2.2) + (age * 0.3);
                    double ang2 = strandOffset + (t2 * Math.PI * 2.2) + (age * 0.3);

                    float rx1 = (float) Math.cos(ang1) * r1;
                    float ry1 = (float) Math.sin(ang1) * r1;
                    float rx2 = (float) Math.cos(ang2) * r2;
                    float ry2 = (float) Math.sin(ang2) * r2;

                    float rw1 = 0.20f * (1.0f - t1 * 0.5f);
                    float rw2 = 0.20f * (1.0f - t2 * 0.5f);

                    drawQuad(matrix, buffer,
                        rx1 - rw1, ry1, z1,
                        rx1 + rw1, ry1, z1,
                        rx2 + rw2, ry2, z2,
                        rx2 - rw2, ry2, z2,
                        1.0f, 0.10f + t1 * 0.3f, 0.25f, 0.85f * (1.0f - t1 * 0.6f)
                    );
                }
            }

            // 6. Expanding Wake Vortex Rings pulsing along the flight axis
            for (int k = 0; k < 3; k++) {
                float ringPhase = (age * 0.15f + (k / 3.0f)) % 1.0f;
                float ringZ = trailStart - (ringPhase * trailLength);
                float ringRadius = (0.35f + ringPhase * 1.2f);
                float ringAlpha = (float) Math.sin(ringPhase * Math.PI) * 0.65f;

                drawRing(matrix, buffer, 0, 0, ringZ, ringRadius, ringRadius * 0.80f, 16, 0.95f, 0.05f, 0.20f, ringAlpha);
            }

            // 7. Kinetic Plasma Thrust Core Beam
            drawBeam(matrix, buffer, 0, 0, trailStart + 0.2f, 0, 0, trailStart - trailLength * 0.75f, 0.08f, 1.0f, 0.80f, 0.85f, 0.95f);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawCylinder(Matrix4f matrix, VertexConsumer consumer, float radius, float zStart, float zEnd, int sides, float r, float g, float b, float a) {
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2;

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            consumer.addVertex(matrix, x1, y1, zStart).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2, zStart).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2, zEnd).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, y1, zEnd).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawSpearHead(Matrix4f matrix, VertexConsumer consumer, float width, float zBase, float zTip, float r, float g, float b, float a) {
        // 4-sided diamond pyramid head
        float[][] pts = { { width, 0 }, { 0, width }, { -width, 0 }, { 0, -width } };
        for (int i = 0; i < 4; i++) {
            int nxt = (i + 1) % 4;
            consumer.addVertex(matrix, pts[i][0], pts[i][1], zBase).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, pts[nxt][0], pts[nxt][1], zBase).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, 0, 0, zTip).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, 0, 0, zTip).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawFin(Matrix4f matrix, VertexConsumer consumer, float widthX, float widthY, float zStart, float zEnd, float r, float g, float b, float a) {
        consumer.addVertex(matrix, 0, 0, zStart).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, widthX, widthY, zStart).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, 0, 0, zEnd).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, 0, 0, zEnd).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
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

    private static void drawBeam(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float radius, float r, float g, float b, float a) {
        float nx = radius;
        float ny = radius;

        drawQuad(matrix, consumer,
            x1 - nx, y1, z1,
            x1 + nx, y1, z1,
            x2 + nx, y2, z2,
            x2 - nx, y2, z2,
            r, g, b, a
        );
        drawQuad(matrix, consumer,
            x1, y1 - ny, z1,
            x1, y1 + ny, z1,
            x2, y2 + ny, z2,
            x2, y2 - ny, z2,
            r, g, b, a
        );
    }
}
