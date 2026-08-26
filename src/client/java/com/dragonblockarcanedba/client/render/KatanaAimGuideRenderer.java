package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.KatanaAimGuideEntity;
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
 * Entity Renderer for Katana Aim Guide in Minecraft 26.2.
 * Renders a sleek, physical 3D laser guide beam, calibrating reticle rings,
 * terminal target brackets, and traveling energy pulse nodes for Iaijutsu: Heaven Splitter.
 */
public class KatanaAimGuideRenderer extends EntityRenderer<KatanaAimGuideEntity, KatanaAimGuideRenderer.AimGuideRenderState> {

    public KatanaAimGuideRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AimGuideRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float maxRange = 24.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
    }

    @Override
    public boolean shouldRender(KatanaAimGuideEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public AimGuideRenderState createRenderState() {
        return new AimGuideRenderState();
    }

    @Override
    public void extractRenderState(KatanaAimGuideEntity entity, AimGuideRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.maxRange = entity.getMaxRange();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
        if (mc.player != null) {
            boolean isRightHanded = (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.KatanaItem && 
                !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.KatanaItem));
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }
    }

    @Override
    public void submit(AimGuideRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float charge = state.chargeRatio;
        float range = state.maxRange;
        float age = state.age;
        float pulse = 0.85f + 0.15f * (float) Math.sin(age * 0.5f);
        float alpha = (0.55f + charge * 0.45f) * pulse;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        if (state.isFirstPersonOwner) {
            float sideSign = state.onRight ? -1.0f : 1.0f;
            poseStack.translate(sideSign * 0.35f, -0.46f, 0.30f);
        }

        float zStart = state.isFirstPersonOwner ? 0.25f : 0.4f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Sleek Outer Cyan Laser Sheath (Volumetric quad beam)
            float sheathRadius = 0.035f + (charge * 0.025f);
            drawLaserBeam(matrix, buffer, zStart, range, sheathRadius, 0.0f, 0.85f, 1.0f, alpha * 0.65f);

            // 2. Blinding White-Hot Razor Cutting Core
            float coreRadius = 0.012f + (charge * 0.008f);
            drawLaserBeam(matrix, buffer, zStart, range, coreRadius, 1.0f, 1.0f, 1.0f, alpha * 0.95f);

            // 3. Calibrating Concentric Reticle Rings along the guide line (every 6 blocks)
            int stationCount = (int) (range / 6.0f);
            for (int s = 1; s <= stationCount; s++) {
                float dist = s * 6.0f;
                if (state.isFirstPersonOwner && dist < 3.0f) continue;
                float ringR = (0.18f + charge * 0.12f) * (1.0f + 0.1f * (float) Math.sin(age * 0.3f + s));
                float ringRot = age * (15.0f + s * 10.0f);
                drawRingZ(matrix, buffer, dist, ringR, ringR * 0.80f, 16, ringRot, 0.1f, 0.9f, 1.0f, alpha * 0.75f);
                drawDiamondBracket(matrix, buffer, dist, ringR * 1.25f, 0.85f, 0.98f, 1.0f, alpha * 0.85f);
            }

            // 4. Terminal Aim Crosshair & Focusing Target Diamond at endpoint
            float endR = 0.30f + (charge * 0.20f);
            drawRingZ(matrix, buffer, range, endR, endR * 0.82f, 16, -age * 40.0f, 0.0f, 0.95f, 1.0f, alpha * 0.90f);
            drawDiamondBracket(matrix, buffer, range, endR * 1.35f, 1.0f, 1.0f, 1.0f, alpha);
            drawCrosshairSpokes(matrix, buffer, range, endR * 1.5f, 0.0f, 0.9f, 1.0f, alpha * 0.8f);

            // 5. High-Velocity Energy Frequency Nodes traveling down the beam
            int nodeCount = 4;
            for (int n = 0; n < nodeCount; n++) {
                float nodeProg = (age * 0.15f + (n / (float) nodeCount)) % 1.0f;
                float nodeZ = 0.5f + nodeProg * (range - 0.5f);
                drawDiamondSparkZ(matrix, buffer, nodeZ, 0.08f * (1.0f + charge * 0.5f), 1.0f, 1.0f, 1.0f, alpha * (1.0f - nodeProg * 0.3f));
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawLaserBeam(Matrix4f matrix, VertexConsumer consumer, float zStart, float zEnd, float radius, float r, float g, float b, float a) {
        // Horizontal Quad
        consumer.addVertex(matrix, -radius, 0, zStart).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, radius, 0, zStart).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, radius, 0, zEnd).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -radius, 0, zEnd).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Vertical Quad
        consumer.addVertex(matrix, 0, -radius, zStart).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, 0, radius, zStart).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, 0, radius, zEnd).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, 0, -radius, zEnd).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);

        // Reverse Horizontal
        consumer.addVertex(matrix, -radius, 0, zEnd).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, radius, 0, zEnd).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, radius, 0, zStart).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, -radius, 0, zStart).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

        // Reverse Vertical
        consumer.addVertex(matrix, 0, -radius, zEnd).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, 0, radius, zEnd).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, 0, radius, zStart).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-1, 0, 0);
        consumer.addVertex(matrix, 0, -radius, zStart).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-1, 0, 0);
    }

    private static void drawRingZ(Matrix4f matrix, VertexConsumer consumer, float z, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        float rotRad = (float) Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2) + rotRad;

            float x1 = (float) Math.cos(a1) * rOuter;
            float y1 = (float) Math.sin(a1) * rOuter;
            float x2 = (float) Math.cos(a2) * rOuter;
            float y2 = (float) Math.sin(a2) * rOuter;

            float ix1 = (float) Math.cos(a1) * rInner;
            float iy1 = (float) Math.sin(a1) * rInner;
            float ix2 = (float) Math.cos(a2) * rInner;
            float iy2 = (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, ix1, iy1, z).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, ix2, iy2, z).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, y2, z).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, y1, z).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawDiamondBracket(Matrix4f matrix, VertexConsumer consumer, float z, float s, float r, float g, float b, float a) {
        float w = s * 0.15f;
        // 4 corner diamond brackets
        float[][] corners = { {s, 0}, {0, s}, {-s, 0}, {0, -s} };
        for (int i = 0; i < 4; i++) {
            float cx = corners[i][0];
            float cy = corners[i][1];
            consumer.addVertex(matrix, cx - w, cy, z).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, cx, cy + w, z).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, cx + w, cy, z).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, cx, cy - w, z).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawCrosshairSpokes(Matrix4f matrix, VertexConsumer consumer, float z, float len, float r, float g, float b, float a) {
        float w = 0.02f;
        // Horizontal Spoke
        consumer.addVertex(matrix, -len, -w, z).setColor(r, g, b, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, len, -w, z).setColor(r, g, b, 0.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, len, w, z).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, -len, w, z).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);

        // Vertical Spoke
        consumer.addVertex(matrix, -w, -len, z).setColor(r, g, b, 0.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, w, -len, z).setColor(r, g, b, 0.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, w, len, z).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, -w, len, z).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }

    private static void drawDiamondSparkZ(Matrix4f matrix, VertexConsumer consumer, float z, float size, float r, float g, float b, float a) {
        float h = size * 1.5f;
        float w = size * 0.6f;

        consumer.addVertex(matrix, -w, 0, z).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, z + h).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, w, 0, z).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, z - h).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
