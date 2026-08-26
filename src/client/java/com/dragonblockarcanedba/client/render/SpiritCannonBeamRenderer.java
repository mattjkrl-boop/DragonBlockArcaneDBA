package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.SpiritCannonBeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Entity Renderer for Spirit Cannon Beam in Minecraft 26.2.
 * Renders a continuous, physical 3D geometric energy beam:
 * - Multi-layered prismatic core (superdense white spine, fluted cyan shroud, outer hexagonal energy sheath)
 * - Quad helical energy drill ribbons orbiting and boring forward
 * - Orbital Ki ring nodes and pulsing energy reticles
 * - Muzzle celestial rune array and convergence emitter geometry
 * - Terminus impact hemisphere dome and radiating splash shards
 */
public class SpiritCannonBeamRenderer extends EntityRenderer<SpiritCannonBeamEntity, SpiritCannonBeamRenderer.SpiritCannonRenderState> {

    public SpiritCannonBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SpiritCannonRenderState extends EntityRenderState {
        public float length = 32.0f;
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float age = 0.0f;
        public int casterId = -1;
    }

    @Override
    public boolean shouldRender(SpiritCannonBeamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public SpiritCannonRenderState createRenderState() {
        return new SpiritCannonRenderState();
    }

    @Override
    public void extractRenderState(SpiritCannonBeamEntity entity, SpiritCannonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.length = entity.getBeamLength();
        state.yRot = entity.getBeamYRot();
        state.xRot = entity.getBeamXRot();
        state.age = entity.tickCount + partialTicks;
        state.casterId = entity.getCasterId();
    }

    @Override
    public void submit(SpiritCannonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float beamLength = state.length;
        if (beamLength < 0.2f) return;

        float age = state.age;
        float pulse = 1.0f + 0.06f * (float) Math.sin(age * 1.5f);
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Multi-Layered Prismatic Core Beam
            // Layer A: Superdense White-Cyan Inner Core (8 sides)
            float innerRadius = 0.13f * pulse;
            drawPrismBeam(matrix, buffer, innerRadius, beamLength, 8, age * 5.0f, 1.0f, 1.0f, 1.0f, 0.95f);

            // Layer B: Fluted Radiant Cyan Shroud (8 sides)
            float midRadius = 0.26f * pulse;
            drawPrismBeam(matrix, buffer, midRadius, beamLength, 8, -age * 8.0f, 0.0f, 0.92f, 1.0f, 0.65f);

            // Layer C: Hexagonal Outer Energy Sheath with Traveling Wave (6 sides)
            float outerRadius = 0.42f * pulse;
            drawPrismBeam(matrix, buffer, outerRadius, beamLength, 6, age * 3.0f, 0.05f, 0.70f, 1.0f, 0.30f);

            // 2. Quad Helical Energy Drill Ribbons
            int segments = Math.min((int) (beamLength * 3.0f), 96);
            float spiralRadius = 0.48f * pulse;
            float ribbonSize = 0.065f;
            float spiralSpeed = age * 0.35f;

            for (int h = 0; h < 4; h++) {
                float phase = h * ((float) Math.PI * 0.5f);
                boolean isGold = (h % 2 == 1);
                float rCol = isGold ? 1.0f : 0.0f;
                float gCol = isGold ? 0.85f : 0.95f;
                float bCol = isGold ? 0.20f : 1.0f;
                float aCol = isGold ? 0.85f : 0.75f;

                for (int i = 0; i < segments; i++) {
                    float z1 = (i / (float) segments) * beamLength;
                    float z2 = ((i + 1) / (float) segments) * beamLength;

                    float p1 = z1 / beamLength;
                    float p2 = z2 / beamLength;

                    double a1 = (p1 * Math.PI * 8.0) + (spiralSpeed * (isGold ? -1.0f : 1.0f)) + phase;
                    double a2 = (p2 * Math.PI * 8.0) + (spiralSpeed * (isGold ? -1.0f : 1.0f)) + phase;

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

            // 3. Orbital Ki Ring Nodes (Planar Pulsing Reticles along Beam Length)
            int ringCount = Math.max(3, (int) (beamLength / 4.0f));
            for (int k = 1; k <= ringCount; k++) {
                float z = (k / (float) (ringCount + 1)) * beamLength;
                float ringPhase = (float) Math.sin(age * 0.4f + k * 1.2f);
                float ringR = (0.55f + ringPhase * 0.12f) * pulse;

                drawPlaneRing(matrix, buffer, 0, 0, z, ringR, ringR * 0.82f, 16, age * (15.0f + k * 4.0f),
                    0.0f, 0.95f, 1.0f, 0.70f);
            }

            // 4. Muzzle Celestial Emitter Array (at Caster Origin Z=0)
            drawMuzzleEmitter(matrix, buffer, age, pulse);

            // 5. Terminus Impact Geometry (at Hit Point Z=beamLength)
            drawTerminusImpact(matrix, buffer, beamLength, age, pulse);
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

    private static void drawMuzzleEmitter(Matrix4f matrix, VertexConsumer consumer, float age, float pulse) {
        // 2 Concentric Rotating Celestial Muzzle Seals
        drawPlaneRing(matrix, consumer, 0, 0, 0.15f, 0.70f * pulse, 0.52f * pulse, 16, age * 30.0f, 0.0f, 0.90f, 1.0f, 0.85f);
        drawPlaneRing(matrix, consumer, 0, 0, 0.20f, 0.48f * pulse, 0.32f * pulse, 12, -age * 45.0f, 1.0f, 0.85f, 0.20f, 0.90f);

        // 4 Inward Convergence Spikes
        for (int s = 0; s < 4; s++) {
            double sAngle = (s / 4.0) * Math.PI * 2.0 + Math.toRadians(age * 20.0f);
            float sx = (float) Math.cos(sAngle) * 0.85f * pulse;
            float sy = (float) Math.sin(sAngle) * 0.85f * pulse;

            drawQuad(matrix, consumer,
                sx - 0.06f, sy - 0.06f, 0.05f,
                sx + 0.06f, sy + 0.06f, 0.05f,
                0, 0, 0.45f,
                0, 0, 0.45f,
                1.0f, 1.0f, 1.0f, 0.95f
            );
        }
    }

    private static void drawTerminusImpact(Matrix4f matrix, VertexConsumer consumer, float beamLength, float age, float pulse) {
        // Expanding Impact Dome at Impact Contact Point
        float domeRadius = 0.85f * pulse;
        int segments = 12;

        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = (float) Math.cos(a1) * domeRadius;
            float y1 = (float) Math.sin(a1) * domeRadius;
            float x2 = (float) Math.cos(a2) * domeRadius;
            float y2 = (float) Math.sin(a2) * domeRadius;

            // Outer flared ring at contact point
            drawQuad(matrix, consumer,
                x1 * 0.5f, y1 * 0.5f, beamLength - 0.3f,
                x2 * 0.5f, y2 * 0.5f, beamLength - 0.3f,
                x2, y2, beamLength,
                x1, y1, beamLength,
                0.0f, 0.95f, 1.0f, 0.75f
            );

            // Center dense white flash point
            drawQuad(matrix, consumer,
                x1 * 0.3f, y1 * 0.3f, beamLength - 0.1f,
                x2 * 0.3f, y2 * 0.3f, beamLength - 0.1f,
                x2 * 0.3f, y2 * 0.3f, beamLength + 0.1f,
                x1 * 0.3f, y1 * 0.3f, beamLength + 0.1f,
                1.0f, 1.0f, 1.0f, 0.95f
            );
        }

        // 4 Radiating Impact Splash Fins
        for (int f = 0; f < 4; f++) {
            double fAngle = (f / 4.0) * Math.PI * 2.0 + Math.toRadians(age * 25.0f);
            float fx = (float) Math.cos(fAngle) * 1.1f * pulse;
            float fy = (float) Math.sin(fAngle) * 1.1f * pulse;

            drawQuad(matrix, consumer,
                0, 0, beamLength,
                fx * 0.4f, fy * 0.4f, beamLength - 0.2f,
                fx, fy, beamLength + 0.1f,
                0, 0, beamLength,
                1.0f, 0.90f, 0.30f, 0.85f
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
