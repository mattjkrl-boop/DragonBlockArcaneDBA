package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.BanshoWindProjectileEntity;
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
 * Entity Renderer for Bansho Wind Projectile in Minecraft 26.2.
 * Renders a physical 3D spinning aerodynamic emerald/jade wind drill, revolving razor wind scythes,
 * trailing supersonic vapor compression rings, and a luminous diamond tip.
 */
public class BanshoWindProjectileRenderer extends EntityRenderer<BanshoWindProjectileEntity, BanshoWindProjectileRenderer.WindProjectileRenderState> {

    public BanshoWindProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class WindProjectileRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
    }

    @Override
    public boolean shouldRender(BanshoWindProjectileEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public WindProjectileRenderState createRenderState() {
        return new WindProjectileRenderState();
    }

    @Override
    public void extractRenderState(BanshoWindProjectileEntity entity, WindProjectileRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(WindProjectileRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        float age = state.ageInTicks;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. High-Speed Spinning Aerodynamic Wind Drill (Central conical core)
            int drillSegments = 12;
            float drillLength = 1.35f;
            float drillRadius = 0.32f;
            float drillRot = age * 45.0f * (float) (Math.PI / 180.0);

            for (int i = 0; i < drillSegments; i++) {
                double a1 = ((i / (double) drillSegments) * Math.PI * 2.0) + drillRot;
                double a2 = (((i + 1) / (double) drillSegments) * Math.PI * 2.0) + drillRot;

                float x1 = (float) Math.cos(a1) * drillRadius;
                float y1 = (float) Math.sin(a1) * drillRadius;
                float x2 = (float) Math.cos(a2) * drillRadius;
                float y2 = (float) Math.sin(a2) * drillRadius;

                // Outer emerald vortex drill cone (tapers to tip at +Z = drillLength * 0.5)
                drawTriangle(matrix, buffer,
                    x1, y1, -drillLength * 0.5f,
                    x2, y2, -drillLength * 0.5f,
                    0, 0, drillLength * 0.5f,
                    0.0f, 1.0f, 0.60f, 0.85f
                );

                // Inner luminous jade spindle
                float inX1 = (float) Math.cos(a1 + 0.3) * (drillRadius * 0.55f);
                float inY1 = (float) Math.sin(a1 + 0.3) * (drillRadius * 0.55f);
                float inX2 = (float) Math.cos(a2 + 0.3) * (drillRadius * 0.55f);
                float inY2 = (float) Math.sin(a2 + 0.3) * (drillRadius * 0.55f);

                drawTriangle(matrix, buffer,
                    inX1, inY1, -drillLength * 0.4f,
                    inX2, inY2, -drillLength * 0.4f,
                    0, 0, drillLength * 0.52f,
                    0.7f, 1.0f, 0.90f, 0.95f
                );
            }

            // 2. Revolving Razor Wind Scythes (4 aerodynamic orbital blades)
            int bladeCount = 4;
            float bladeOrbitR = drillRadius * 1.35f;
            float bladeRot = -age * 36.0f * (float) (Math.PI / 180.0);

            for (int b = 0; b < bladeCount; b++) {
                double bAngle = (b / (double) bladeCount) * Math.PI * 2.0 + bladeRot;

                float bx = (float) Math.cos(bAngle) * bladeOrbitR;
                float by = (float) Math.sin(bAngle) * bladeOrbitR;

                // Tangent vector
                float tx = (float) -Math.sin(bAngle);
                float ty = (float) Math.cos(bAngle);

                float bladeLen = 0.55f;
                float bladeWidth = 0.16f;

                float tipX = bx + tx * (bladeLen * 0.6f);
                float tipY = by + ty * (bladeLen * 0.6f);
                float tailX = bx - tx * (bladeLen * 0.4f);
                float tailY = by - ty * (bladeLen * 0.4f);

                float nx = -ty * bladeWidth;
                float ny = tx * bladeWidth;

                // 3D Curved Scythe Blade
                drawTriangle(matrix, buffer,
                    tailX - nx, tailY - ny, -0.2f,
                    tailX + nx, tailY + ny, -0.2f,
                    tipX, tipY, 0.25f,
                    0.3f, 1.0f, 0.85f, 0.90f
                );
                drawTriangle(matrix, buffer,
                    tailX - nx, tailY - ny, -0.2f,
                    tipX, tipY, 0.25f,
                    tailX + nx, tailY + ny, -0.2f,
                    0.0f, 0.95f, 0.55f, 0.90f
                );
            }

            // 3. Trailing Supersonic Compression Cones / Vapor Rings (3 trailing rings)
            int ringCount = 3;
            for (int r = 0; r < ringCount; r++) {
                float ringProgress = (r + 1) / (float) (ringCount + 1);
                float ringZ = -drillLength * 0.4f - (ringProgress * 0.75f);
                float ringR = drillRadius * (0.8f + ringProgress * 0.9f);
                float ringAlpha = (1.0f - ringProgress) * 0.70f;

                drawRingZ(matrix, buffer, ringZ, ringR, ringR * 0.75f, 16, 0.15f, 1.0f, 0.75f, ringAlpha);
            }

            // 4. Radiant Diamond Needle Tip (+Z Apex)
            float tipZ = drillLength * 0.5f;
            float tipLen = 0.35f;
            float tipW = 0.10f;
            for (int t = 0; t < 4; t++) {
                double a1 = (t / 4.0) * Math.PI * 2.0 + drillRot * 1.5;
                double a2 = ((t + 1) / 4.0) * Math.PI * 2.0 + drillRot * 1.5;

                float x1 = (float) Math.cos(a1) * tipW;
                float y1 = (float) Math.sin(a1) * tipW;
                float x2 = (float) Math.cos(a2) * tipW;
                float y2 = (float) Math.sin(a2) * tipW;

                drawTriangle(matrix, buffer,
                    x1, y1, tipZ,
                    x2, y2, tipZ,
                    0, 0, tipZ + tipLen,
                    1.0f, 1.0f, 1.0f, 0.98f
                );
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawRingZ(Matrix4f matrix, VertexConsumer consumer, float z, float rOut, float rIn, int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1Out = (float) Math.cos(a1) * rOut;
            float y1Out = (float) Math.sin(a1) * rOut;
            float x2Out = (float) Math.cos(a2) * rOut;
            float y2Out = (float) Math.sin(a2) * rOut;

            float x1In = (float) Math.cos(a1) * rIn;
            float y1In = (float) Math.sin(a1) * rIn;
            float x2In = (float) Math.cos(a2) * rIn;
            float y2In = (float) Math.sin(a2) * rIn;

            drawQuad(matrix, consumer, x1Out, y1Out, z, x2Out, y2Out, z, x2In, y2In, z, x1In, y1In, z, r, g, b, a);
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
