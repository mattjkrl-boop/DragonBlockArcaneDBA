package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DimensionalSlashEntity;
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
 * Entity Renderer for Dimensional Slash in Minecraft 26.2.
 * Renders a physical 3D volumetric reality severance crescent:
 * - Volumetric double-beveled dimensional crescent blade with leading knife edge and trailing void slipstream
 * - Multi-layer gradient: Outer Abyssal Violet Rift -> Neon Blood-Magenta Core -> Pure White Reality Tear
 * - Transverse 3D reality tear fin spurs extending from tips and spine
 * - Central rotating 3D void singularity octahedron and counter-rotating accretion micro-rings
 * - Trailing spatial fissure ribbons streaming backward along trajectory
 */
public class DimensionalSlashRenderer extends EntityRenderer<DimensionalSlashEntity, DimensionalSlashRenderer.SlashRenderState> {

    public DimensionalSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SlashRenderState extends EntityRenderState {
        public boolean tiltRight = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public boolean isFirstPersonOwner = false;
    }

    @Override
    public boolean shouldRender(DimensionalSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public SlashRenderState createRenderState() {
        return new SlashRenderState();
    }

    @Override
    public void extractRenderState(DimensionalSlashEntity entity, SlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tiltRight = entity.getTilt();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(SlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        if (state.tiltRight) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(35.0f));
        } else {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-35.0f));
        }

        float fpScale = (state.isFirstPersonOwner && state.age < 5.0f) ? (0.60f + (state.age / 5.0f) * 0.40f) : 1.0f;
        poseStack.scale(fpScale, fpScale, fpScale);

        // Forward spin along movement vector
        poseStack.mulPose(Axis.YP.rotationDegrees((float) Math.sin(state.age * 0.4f) * 6.0f));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            float span = 3.4f;
            float chord = 1.05f;
            int segments = 20;
            float pulse = 0.90f + 0.10f * (float) Math.sin(state.age * 0.6f);

            // 1. Outer Abyssal Violet & Crimson Dimensional Rift Layer
            drawVolumetricCrescent(matrix, buffer, span + 0.35f, chord + 0.35f, 0.26f * pulse, 0.60f, segments,
                0.65f, 0.05f, 0.95f, 0.85f);

            // 2. Inner Neon Blood-Magenta Razor Core
            drawVolumetricCrescent(matrix, buffer, span, chord, 0.14f * pulse, 0.38f, segments,
                1.0f, 0.10f, 0.35f, 0.95f);

            // 3. Pure White-Hot Reality Severance Seam
            drawVolumetricCrescent(matrix, buffer, span * 0.72f, chord * 0.72f, 0.06f, 0.14f, segments,
                1.0f, 0.95f, 1.0f, 1.0f);

            // 4. Transverse 3D Reality Tear Fin Spurs (Stabbing out from tips and apex)
            drawTipSpurs(matrix, buffer, span, chord, 0.32f);

            // 5. Central Spinning Singularity Octahedron Core & Accretion Micro-Rings
            drawSingularityOctahedron(matrix, buffer, 0, 0, chord * 0.3f, 0.35f * pulse, state.age * 30.0f,
                0.02f, 0.0f, 0.05f, 0.99f,
                1.0f, 0.15f, 0.85f, 0.95f,
                1.0f, 1.0f, 1.0f, 1.0f);

            drawConcentricRing(matrix, buffer, 0, 0, chord * 0.3f, 0.75f * pulse, 0.55f * pulse, 16, state.age * 22.0f,
                0.60f, 0.05f, 0.95f, 0.85f);
            drawConcentricRing(matrix, buffer, 0, 0, chord * 0.3f, 0.50f * pulse, 0.38f * pulse, 12, -state.age * 30.0f,
                1.0f, 0.15f, 0.35f, 0.95f);

            // 6. Trailing High-Velocity Spatial Wake Ribbons
            float ribbonLen = 2.4f;
            float ribbonWidth = 0.14f;
            for (int i = 0; i < 4; i++) {
                float t = (i / 3.0f) * 2.0f - 1.0f; // -1 to 1
                float rx = t * span * 0.75f;
                float rz = (1.0f - t * t) * chord * 0.6f;
                drawTaperedRibbon(matrix, buffer, rx, 0, rz, rx, 0, rz - ribbonLen * (1.0f - Math.abs(t) * 0.4f), ribbonWidth,
                    0.75f, 0.05f, 0.90f, 0.80f,
                    1.0f, 0.15f, 0.45f, 0.0f);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawVolumetricCrescent(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float thickness, float trailLength, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;

            float x1 = t1 * span;
            float z1 = (1.0f - t1 * t1) * chord;
            float x2 = t2 * span;
            float z2 = (1.0f - t2 * t2) * chord;

            float th1 = thickness * (1.0f - Math.abs(t1) * 0.75f);
            float th2 = thickness * (1.0f - Math.abs(t2) * 0.75f);

            float z1Trail = z1 - trailLength * (1.0f - Math.abs(t1));
            float z2Trail = z2 - trailLength * (1.0f - Math.abs(t2));

            // Top Surface Bevel
            consumer.addVertex(matrix, x1, th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, th2 * 0.25f, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, th1 * 0.25f, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom Surface Bevel
            consumer.addVertex(matrix, x1, -th1 * 0.25f, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2 * 0.25f, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, -th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

            // Front Leading Knife Edge
            consumer.addVertex(matrix, x1, -th1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, -th2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, th2, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, th1, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawTipSpurs(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float spurSize) {
        // Left Tip Spur
        consumer.addVertex(matrix, -span, 0, 0).setColor(1.0f, 0.15f, 0.45f, 0.95f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -span - spurSize * 1.6f, 0, -spurSize * 2.2f).setColor(0.65f, 0.05f, 0.95f, 0.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -span + spurSize * 0.4f, 0, -spurSize).setColor(1.0f, 0.15f, 0.45f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -span, 0, 0).setColor(1.0f, 0.15f, 0.45f, 0.95f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Right Tip Spur
        consumer.addVertex(matrix, span, 0, 0).setColor(1.0f, 0.15f, 0.45f, 0.95f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, span + spurSize * 1.6f, 0, -spurSize * 2.2f).setColor(0.65f, 0.05f, 0.95f, 0.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, span - spurSize * 0.4f, 0, -spurSize).setColor(1.0f, 0.15f, 0.45f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, span, 0, 0).setColor(1.0f, 0.15f, 0.45f, 0.95f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawConcentricRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        float rotRad = (float) Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2.0) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2.0) + rotRad;

            float x1Out = cx + (float) Math.cos(a1) * rOuter;
            float y1Out = cy + (float) Math.sin(a1) * rOuter;
            float x2Out = cx + (float) Math.cos(a2) * rOuter;
            float y2Out = cy + (float) Math.sin(a2) * rOuter;

            float x1In = cx + (float) Math.cos(a1) * rInner;
            float y1In = cy + (float) Math.sin(a1) * rInner;
            float x2In = cx + (float) Math.cos(a2) * rInner;
            float y2In = cy + (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, x1In, y1In, cz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2In, y2In, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2Out, y2Out, cz).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1Out, y1Out, cz).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawSingularityOctahedron(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float rotDeg, float rVoid, float gVoid, float bVoid, float aVoid, float rGlow, float gGlow, float bGlow, float aGlow, float rCore, float gCore, float bCore, float aCore) {
        float rotRad = (float) Math.toRadians(rotDeg);
        float h = size * 1.2f;
        float w = size * 0.7f;

        for (int i = 0; i < 4; i++) {
            double a1 = (i / 4.0) * Math.PI * 2.0 + rotRad;
            double a2 = ((i + 1) / 4.0) * Math.PI * 2.0 + rotRad;

            float x1 = cx + (float) Math.cos(a1) * w;
            float y1 = cy + (float) Math.sin(a1) * w;
            float x2 = cx + (float) Math.cos(a2) * w;
            float y2 = cy + (float) Math.sin(a2) * w;

            // Top pyramid
            consumer.addVertex(matrix, x1, y1, cz).setColor(rGlow, gGlow, bGlow, aGlow).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2, cz).setColor(rGlow, gGlow, bGlow, aGlow).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, cy, cz + h).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, cy, cz + h).setColor(rCore, gCore, bCore, aCore).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom pyramid
            consumer.addVertex(matrix, x2, y2, cz).setColor(rGlow, gGlow, bGlow, aGlow).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, y1, cz).setColor(rGlow, gGlow, bGlow, aGlow).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, cx, cy, cz - h).setColor(rVoid, gVoid, bVoid, aVoid).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, cx, cy, cz - h).setColor(rVoid, gVoid, bVoid, aVoid).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }

    private static void drawTaperedRibbon(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float width, float r1, float g1, float b1, float a1, float r2, float g2, float b2, float a2) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * width * 0.5f;
        float nz = dx / len * width * 0.5f;

        consumer.addVertex(matrix, x1 - nx, y1, z1 - nz).setColor(r1, g1, b1, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx, y1, z1 + nz).setColor(r1, g1, b1, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx * 0.3f, y2, z2 + nz * 0.3f).setColor(r2, g2, b2, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx * 0.3f, y2, z2 - nz * 0.3f).setColor(r2, g2, b2, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
