package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.HollowAfterimageEntity;
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

import java.util.Random;

/**
 * Entity Renderer for Hollow Afterimage in Minecraft 26.2.
 * Renders a premium, dynamic 3D fading cinematic ghost clone:
 * - Dynamic dash/stride athletic iaido posture with swept silhouette
 * - Multi-pass ethereal cyan-violet halo with pulsating spatial distortion ripples
 * - Trailing phantom speed wake ribbons
 * - Holographic displacement scanline planes
 * - Ascending disintegrating phantom polygon motes and diamond sparks as the ghost dissolves.
 */
public class HollowAfterimageRenderer extends EntityRenderer<HollowAfterimageEntity, HollowAfterimageRenderer.AfterimageRenderState> {

    public HollowAfterimageRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AfterimageRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public int skinColor = 0xFF8CC8FF;
        public int hairColor = 0xFF1EB4FF;
        public float age = 0;
        public long seed = 0;
    }

    @Override
    public boolean shouldRender(HollowAfterimageEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public AfterimageRenderState createRenderState() {
        return new AfterimageRenderState();
    }

    @Override
    public void extractRenderState(HollowAfterimageEntity entity, AfterimageRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getSyncYaw();
        state.xRot = entity.getSyncPitch();
        state.skinColor = entity.getSkinColor();
        state.hairColor = entity.getHairColor();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(AfterimageRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = 80.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float alpha = Math.max(0.05f, 0.90f * (1.0f - (progress * progress)));
        float pulse = 0.85f + 0.15f * (float) Math.sin(state.age * 0.35f);
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Outer Ethereal Phantom Halo (Pulsing Cyan-Violet Spatial Aura)
            renderGhostBody(pose, buffer, 0.06f, 0.10f, 0.85f, 1.0f, alpha * 0.35f * pulse);

            // 2. Main Spectral Void Body (Deep Indigo / Violet Core)
            renderGhostBody(pose, buffer, 0.0f, 0.15f, 0.05f, 0.35f, alpha * 0.85f);

            // 3. Central Spatial Eye Slit & Core Luminescence (White-Cyan)
            KiRenderHelper.drawColoredBox(pose, buffer,
                -0.12f, 1.55f, -0.22f,
                0.12f, 1.65f, -0.18f,
                0.85f, 0.98f, 1.0f, alpha * 0.95f
            );

            // 4. Holographic Horizontal Scanline Distortion Slices
            int sliceCount = 6;
            for (int s = 0; s < sliceCount; s++) {
                float sliceY = 0.15f + (s * 0.32f) + (float) Math.sin((state.age * 0.25f) + s) * 0.06f;
                float sliceOffset = (float) Math.sin((state.age * 0.5f) + s * 1.5f) * 0.06f;
                KiRenderHelper.drawColoredBox(pose, buffer,
                    -0.35f + sliceOffset, sliceY, -0.25f,
                    0.35f + sliceOffset, sliceY + 0.03f, 0.25f,
                    0.0f, 0.95f, 1.0f, alpha * 0.65f
                );
            }

            // 5. Trailing Phantom Speed Ribbons (Extending behind the dash stance)
            float ribbonAlpha = alpha * 0.45f;
            drawPhantomRibbon(matrix, buffer, 0.8f, 1.4f, 0.15f, 0.85f, ribbonAlpha);
            drawPhantomRibbon(matrix, buffer, 0.3f, 0.8f, 0.20f, 0.70f, ribbonAlpha * 0.8f);

            // 6. Spatial Footstep Ripple Ring
            float ringR = (0.5f + (state.age * 0.03f) % 0.8f);
            drawRippleRing(matrix, buffer, 0, 0.03f, 0, ringR, ringR * 0.85f, 16,
                0.0f, 0.85f, 1.0f, alpha * (1.0f - (ringR / 1.3f)));

            // 7. Ascending Disintegrating Phantom Polygon Motes & Diamond Sparks
            int moteCount = 16;
            for (int m = 0; m < moteCount; m++) {
                float moteProg = (progress + (m / (float) moteCount)) % 1.0f;
                float mx = (rng.nextFloat() - 0.5f) * 0.95f;
                float my = moteProg * 2.4f;
                float mz = (rng.nextFloat() - 0.5f) * 0.95f;
                float mSize = 0.06f * (1.0f - moteProg);
                float mMoteAlpha = alpha * (1.0f - moteProg);

                drawMoteDiamond(matrix, buffer, mx, my, mz, mSize, 0.1f, 0.85f, 1.0f, mMoteAlpha);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private void renderGhostBody(PoseStack.Pose pose,
                                 VertexConsumer buffer,
                                 float expand, float r, float g, float b, float a) {
        // Torso (Slightly leaned forward for athletic iaido sprint pose)
        KiRenderHelper.drawColoredBox(pose, buffer,
            -0.25f - expand, 0.7f - expand, -0.15f - expand - 0.05f,
            0.25f + expand, 1.4f + expand, 0.15f + expand - 0.05f,
            r, g, b, a);

        // Head (Facing forward)
        KiRenderHelper.drawColoredBox(pose, buffer,
            -0.2f - expand, 1.4f, -0.2f - expand - 0.08f,
            0.2f + expand, 1.82f + expand * 1.5f, 0.2f + expand - 0.08f,
            r * 1.2f, g * 1.2f, b * 1.2f, a);

        // Left Arm (Swept back in dash wake)
        KiRenderHelper.drawColoredBox(pose, buffer,
            -0.45f - expand, 0.65f - expand, 0.05f,
            -0.25f, 1.35f + expand, 0.32f + expand,
            r, g, b, a * 0.85f);

        // Right Arm (Forward iaido slash posture)
        KiRenderHelper.drawColoredBox(pose, buffer,
            0.25f, 0.75f - expand, -0.32f - expand,
            0.45f + expand, 1.45f + expand, -0.05f,
            r, g, b, a * 0.85f);

        // Left Leg (Forward stride)
        KiRenderHelper.drawColoredBox(pose, buffer,
            -0.22f - expand, 0.0f, -0.22f - expand,
            -0.02f, 0.7f + expand, 0.05f,
            r * 0.85f, g * 0.85f, b * 0.85f, a * 0.9f);

        // Right Leg (Trailing back stride)
        KiRenderHelper.drawColoredBox(pose, buffer,
            0.02f, 0.0f, 0.05f,
            0.22f + expand, 0.7f + expand, 0.28f + expand,
            r * 0.85f, g * 0.85f, b * 0.85f, a * 0.9f);
    }

    private static void drawPhantomRibbon(Matrix4f matrix, VertexConsumer consumer, float y1, float y2, float width, float trailDist, float a) {
        consumer.addVertex(matrix, -width, y1, 0.1f).setColor(0.1f, 0.85f, 1.0f, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, width, y1, 0.1f).setColor(0.1f, 0.85f, 1.0f, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, width * 0.5f, y2, trailDist).setColor(0.0f, 0.4f, 0.9f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, -width * 0.5f, y2, trailDist).setColor(0.0f, 0.4f, 0.9f, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }

    private static void drawRippleRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = cx + (float) Math.cos(a1) * rOuter;
            float z1 = cz + (float) Math.sin(a1) * rOuter;
            float x2 = cx + (float) Math.cos(a2) * rOuter;
            float z2 = cz + (float) Math.sin(a2) * rOuter;

            float ix1 = cx + (float) Math.cos(a1) * rInner;
            float iz1 = cz + (float) Math.sin(a1) * rInner;
            float ix2 = cx + (float) Math.cos(a2) * rInner;
            float iz2 = cz + (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, ix1, cy, iz1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ix2, cy, iz2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, cy, z2).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, cy, z1).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawMoteDiamond(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float r, float g, float b, float a) {
        float h = size * 1.4f;
        float w = size * 0.5f;

        consumer.addVertex(matrix, cx - w, cy, cz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx + w, cy, cz).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        consumer.addVertex(matrix, cx, cy, cz - w).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy + h, cz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy, cz + w).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, cx, cy - h, cz).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
    }
}
