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
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SpiritCannonBeamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SpiritCannonBeamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
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

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
        if (mc.player != null) {
            boolean isRightHanded = (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.SpiritSwordItem && 
                !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.SpiritSwordItem));
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }
    }

    @Override
    public void submit(SpiritCannonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float beamLength = state.length;
        if (beamLength < 0.2f) return;

        float age = state.age;
        float pulse = 1.0f + 0.06f * (float) Math.sin(age * 1.5f);
        float fadeRaw = Math.min(1.0f, age / 5.0f);
        float fadeIn = fadeRaw * fadeRaw * (3.0f - 2.0f * fadeRaw);
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        if (state.isFirstPersonOwner) {
            // Anchor beam origin to the Spirit Sword blade tip: way lower and on weapon hand side
            float sideSign = state.onRight ? -1.0f : 1.0f;
            poseStack.translate(sideSign * 0.40f, -0.46f, 0.35f);
        }

        float zStart = state.isFirstPersonOwner ? 0.25f : 0.0f;
        float fpScale = state.isFirstPersonOwner ? 0.50f : 1.0f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Multi-Layered Prismatic Core Beam (scaled down in first person)
            // Layer A: Superdense White-Cyan Inner Core (8 sides)
            float innerRadius = 0.13f * pulse * fpScale;
            drawPrismBeam(matrix, buffer, innerRadius, zStart, beamLength, 8, age * 5.0f, 1.0f, 1.0f, 1.0f, 0.95f * fadeIn);

            // Layer B: Fluted Radiant Cyan Shroud (8 sides)
            float midRadius = 0.26f * pulse * fpScale;
            drawPrismBeam(matrix, buffer, midRadius, zStart, beamLength, 8, -age * 8.0f, 0.0f, 0.92f, 1.0f, 0.65f * fadeIn);

            // Layer C: Hexagonal Outer Energy Sheath with Traveling Wave (6 sides)
            // Drawn in segments with a traveling brightness band (redesign T7)
            // so energy visibly flows down the beam instead of sitting uniform.
            float outerRadius = 0.42f * pulse * fpScale;
            drawPrismBeamFlow(matrix, buffer, outerRadius, zStart, beamLength, 6, age * 3.0f, age, 0.05f, 0.70f, 1.0f, 0.30f * fadeIn);

            int ribbonSegments = Math.min((int) (beamLength * 3.0f), 72);
            float spiralRadius = 0.48f * pulse * fpScale;
            float ribbonSize = 0.045f * fpScale;
            float spiralSpeed = age * 0.35f;
            if (ribbonSegments > 1) {
                for (int h = 0; h < 4; h++) {
                    float phase = h * ((float) Math.PI * 0.5f);
                    boolean isGold = (h % 2 == 1);
                    float rCol = isGold ? 1.0f : 0.0f;
                    float gCol = isGold ? 0.85f : 0.95f;
                    float bCol = isGold ? 0.20f : 1.0f;
                    float aCol = (isGold ? 0.85f : 0.75f) * fadeIn;
                    float dir = isGold ? -1.0f : 1.0f;
                    for (int i = 0; i < ribbonSegments; i++) {
                        float z1 = (i / (float) ribbonSegments) * (beamLength - zStart) + zStart;
                        float z2 = ((i + 1) / (float) ribbonSegments) * (beamLength - zStart) + zStart;
                        float p1 = z1 / beamLength;
                        float p2 = z2 / beamLength;
                        double a1 = (p1 * Math.PI * 8.0) + (spiralSpeed * dir) + phase;
                        double a2 = (p2 * Math.PI * 8.0) + (spiralSpeed * dir) + phase;
                        float x1 = (float) Math.cos(a1) * spiralRadius;
                        float y1 = (float) Math.sin(a1) * spiralRadius;
                        float x2 = (float) Math.cos(a2) * spiralRadius;
                        float y2 = (float) Math.sin(a2) * spiralRadius;
                        float w1 = ribbonSize * (0.35f + 0.65f * (float) Math.sin(p1 * Math.PI));
                        float w2 = ribbonSize * (0.35f + 0.65f * (float) Math.sin(p2 * Math.PI));
                        drawQuad(matrix, buffer, x1 - w1, y1, z1, x1 + w1, y1, z1, x2 + w2, y2, z2, x2 - w2, y2, z2, rCol, gCol, bCol, aCol);
                        float c1 = w1 * 0.32f;
                        float c2 = w2 * 0.32f;
                        drawQuad(matrix, buffer, x1 - c1, y1, z1, x1 + c1, y1, z1, x2 + c2, y2, z2, x2 - c2, y2, z2, 1.0f, 1.0f, 1.0f, aCol);
                        float ix1 = x1 * 0.7f;
                        float iy1 = y1 * 0.7f;
                        float ix2 = x2 * 0.7f;
                        float iy2 = y2 * 0.7f;
                        buffer.addVertex(matrix, x1, y1, z1).setColor(rCol, gCol, bCol, aCol * 0.55f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                        buffer.addVertex(matrix, x1 + (ix1 - x1) * 0.15f, y1 + (iy1 - y1) * 0.15f, z1).setColor(rCol, gCol, bCol, aCol * 0.55f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                        buffer.addVertex(matrix, ix2, iy2, z2).setColor(rCol, gCol, bCol, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                        buffer.addVertex(matrix, ix1, iy1, z1).setColor(rCol, gCol, bCol, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                    }
                }
            }

            // Muzzle at the blade tip, then the impact where the beam ends.
            drawMuzzleEmitter(matrix, buffer, age, pulse);

            // 5. Terminus Impact Geometry (at Hit Point Z=beamLength)
            drawTerminusImpact(matrix, buffer, beamLength, age, pulse);
        });

        poseStack.popPose();
    }

    private static void drawPrismBeam(Matrix4f matrix, VertexConsumer consumer, float radius, float zStart, float length, int sides, float rotDeg, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2.0 + rotRad;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2.0 + rotRad;

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            drawQuad(matrix, consumer,
                x1, y1, zStart,
                x2, y2, zStart,
                x2, y2, length,
                x1, y1, length,
                r, g, b, a
            );
        }
    }

    /**
     * Segmented prism beam with a traveling brightness band (redesign T7).
     * Alpha pulses in a sine wave whose phase runs along the beam length and
     * advances with age, so the sheath reads as flowing energy.
     */
    private static void drawPrismBeamFlow(Matrix4f matrix, VertexConsumer consumer, float radius, float zStart, float length, int sides, float rotDeg, float age, float r, float g, float b, float a) {
        int flowSegments = 12;
        for (int sgm = 0; sgm < flowSegments; sgm++) {
            float z1 = zStart + (length - zStart) * (sgm / (float) flowSegments);
            float z2 = zStart + (length - zStart) * ((sgm + 1) / (float) flowSegments);
            float p = sgm / (float) flowSegments;
            float band = 0.70f + 0.55f * (0.5f + 0.5f * (float) Math.sin(p * Math.PI * 6.0 - age * 0.9f));
            drawPrismBeam(matrix, consumer, radius, z1, z2, sides, rotDeg, r, g, b, a * band);
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
        drawPlaneRing(matrix, consumer, 0, 0, 0.15f, 0.70f * pulse, 0.52f * pulse, 24, age * 30.0f, 0.0f, 0.90f, 1.0f, 0.85f);
        drawPlaneRing(matrix, consumer, 0, 0, 0.20f, 0.48f * pulse, 0.32f * pulse, 16, -age * 45.0f, 0.0f, 0.95f, 1.0f, 0.90f);

        for (int s = 0; s < 4; s++) {
            double sAngle = (s / 4.0) * Math.PI * 2.0 + Math.toRadians(age * 20.0f);
            float sx = (float) Math.cos(sAngle) * 0.85f * pulse;
            float sy = (float) Math.sin(sAngle) * 0.85f * pulse;
            drawCurvedNeedle(matrix, consumer, sx, sy, 0.06f, 0.0f, 0.0f, 0.48f, 1.0f, 1.0f, 1.0f, 0.95f);
        }
    }

    private static void drawTerminusImpact(Matrix4f matrix, VertexConsumer consumer, float beamLength, float age, float pulse) {
        // Expanding Impact Dome at Impact Contact Point
        float domeRadius = 0.85f * pulse;
        int segments = 16;

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

        for (int f = 0; f < 4; f++) {
            double fAngle = (f / 4.0) * Math.PI * 2.0 + Math.toRadians(age * 25.0f);
            float fx = (float) Math.cos(fAngle) * 1.1f * pulse;
            float fy = (float) Math.sin(fAngle) * 1.1f * pulse;
            drawCurvedNeedle(matrix, consumer, 0.0f, 0.0f, beamLength, fx, fy, beamLength + 0.15f, 1.0f, 0.90f, 0.30f, 0.85f);
        }
    }

    /** A tapered curve with a bright core and a tail that fades, used where a spike used to sit. */
    private static void drawCurvedNeedle(Matrix4f matrix, VertexConsumer consumer,
                                         float x1, float y1, float z1, float x2, float y2, float z2,
                                         float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) {
            return;
        }
        float px = -dy / len;
        float py = dx / len;
        float bow = len * 0.28f;
        float mx = (x1 + x2) * 0.5f + px * bow;
        float my = (y1 + y2) * 0.5f + py * bow;
        float mz = (z1 + z2) * 0.5f + dz / len * bow * 0.15f;
        float w = Math.min(0.07f, len * 0.12f);
        needleHalf(matrix, consumer, x1, y1, z1, mx, my, mz, w, w * 0.45f, r, g, b, a, a * 0.7f);
        needleHalf(matrix, consumer, mx, my, mz, x2, y2, z2, w * 0.45f, 0.008f, r, g, b, a * 0.7f, 0.0f);
        needleHalf(matrix, consumer, x1, y1, z1, mx, my, mz, w * 0.28f, w * 0.12f, 1.0f, 1.0f, 1.0f, a, a * 0.8f);
        needleHalf(matrix, consumer, mx, my, mz, mx - px * bow, my - py * bow, mz, w * 0.35f, 0.006f, r, g, b, a * 0.4f, 0.0f);
    }

    private static void needleHalf(Matrix4f matrix, VertexConsumer consumer,
                                   float x1, float y1, float z1, float x2, float y2, float z2,
                                   float w1, float w2, float r, float g, float b, float a1, float a2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + 0.0001f);
        float nx = -dy / len;
        float ny = dx / len;
        consumer.addVertex(matrix, x1 - nx * w1, y1 - ny * w1, z1).setColor(r, g, b, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x1 + nx * w1, y1 + ny * w1, z1).setColor(r, g, b, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2 + nx * w2, y2 + ny * w2, z2).setColor(r, g, b, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2 - nx * w2, y2 - ny * w2, z2).setColor(r, g, b, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x1, y1, z1 - w1).setColor(r, g, b, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1, z1 + w1).setColor(r, g, b, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2 + w2).setColor(r, g, b, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2 - w2).setColor(r, g, b, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
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
