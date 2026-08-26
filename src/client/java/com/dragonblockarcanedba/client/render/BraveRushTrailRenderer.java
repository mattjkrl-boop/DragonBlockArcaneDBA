package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.BraveRushTrailEntity;
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
 * Entity Renderer for Brave Sword Piercing Heroic Dash Flight Trail in Minecraft 26.2.
 * Renders physical 3D supersonic golden-cyan Mach shock cones, twin helical valor streamlines,
 * volumetric kinetic thrust core beam, and expanding wake vortex rings.
 */
public class BraveRushTrailRenderer extends EntityRenderer<BraveRushTrailEntity, BraveRushTrailRenderer.TrailRenderState> {

    public BraveRushTrailRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TrailRenderState extends EntityRenderState {
        public float trailScale = 1.0f;
        public float trailLength = 10.0f;
        public float age = 0;
        public float yRot = 0;
        public float xRot = 0;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
    }

    @Override
    public boolean shouldRender(BraveRushTrailEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public TrailRenderState createRenderState() {
        return new TrailRenderState();
    }

    @Override
    public void extractRenderState(BraveRushTrailEntity entity, TrailRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.trailScale = entity.getTrailScale();
        state.trailLength = entity.getTrailLength();
        state.age = entity.tickCount + partialTicks;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
        if (mc.player != null) {
            boolean isRightHanded = (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.BraveSwordItem && 
                !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.BraveSwordItem));
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }
    }

    @Override
    public void submit(TrailRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = 14.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float alpha = (1.0f - (progress * progress)) * 0.90f;
        float scale = state.trailScale * (1.0f + progress * 0.4f) * (state.isFirstPersonOwner ? 0.45f : 1.0f);
        float len = state.trailLength;

        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Supersonic 3D Heroic Mach Cone (Faceted aerodynamic cone expanding backwards)
            int coneSegments = 16;
            float coneLength = 3.6f * scale;
            float coneBaseRadius = (state.isFirstPersonOwner ? 1.45f : 1.2f) * scale;
            float tipZ = state.isFirstPersonOwner ? (-0.6f * scale) : (0.6f * scale);
            float baseZ = tipZ - coneLength;
            float coneAlpha = state.isFirstPersonOwner ? (alpha * 0.35f) : (alpha * 0.75f);

            for (int i = 0; i < coneSegments; i++) {
                double a1 = (i / (double) coneSegments) * Math.PI * 2.0;
                double a2 = ((i + 1) / (double) coneSegments) * Math.PI * 2.0;

                float x1 = (float) Math.cos(a1) * coneBaseRadius;
                float y1 = (float) Math.sin(a1) * coneBaseRadius;
                float x2 = (float) Math.cos(a2) * coneBaseRadius;
                float y2 = (float) Math.sin(a2) * coneBaseRadius;

                // Outer golden cone face
                drawTriangle(matrix, buffer, 0, 0, tipZ, x1, y1, baseZ, x2, y2, baseZ, 1.0f, 0.84f, 0.0f, coneAlpha);
                // Inner cyan accent cone face
                drawTriangle(matrix, buffer, 0, 0, tipZ * 0.8f, x1 * 0.7f, y1 * 0.7f, baseZ * 0.8f, x2 * 0.7f, y2 * 0.7f, baseZ * 0.8f, 0.0f, 0.95f, 1.0f, coneAlpha * 1.1f);
            }

            // 2. Twin Helical Golden Valor Streamlines / Wing Vanes
            int ribbonSteps = 16;
            for (int strand = 0; strand < 2; strand++) {
                float strandOffset = strand * (float) Math.PI;
                boolean isGold = (strand == 0);
                float sr = isGold ? 1.0f : 0.0f;
                float sg = isGold ? 0.84f : 0.95f;
                float sb = isGold ? 0.0f : 1.0f;

                for (int s = 0; s < ribbonSteps; s++) {
                    float t1 = s / (float) ribbonSteps;
                    float t2 = (s + 1) / (float) ribbonSteps;

                    float z1 = tipZ - (t1 * len);
                    float z2 = tipZ - (t2 * len);

                    float r1 = (0.25f + t1 * 1.1f) * scale;
                    float r2 = (0.25f + t2 * 1.1f) * scale;

                    double ang1 = strandOffset + (t1 * Math.PI * 3.0) + (state.age * 0.22);
                    double ang2 = strandOffset + (t2 * Math.PI * 3.0) + (state.age * 0.22);

                    float rx1 = (float) Math.cos(ang1) * r1;
                    float ry1 = (float) Math.sin(ang1) * r1;
                    float rx2 = (float) Math.cos(ang2) * r2;
                    float ry2 = (float) Math.sin(ang2) * r2;

                    float rw1 = 0.24f * (1.0f - t1 * 0.35f) * scale;
                    float rw2 = 0.24f * (1.0f - t2 * 0.35f) * scale;

                    drawQuad(matrix, buffer,
                        rx1 - rw1, ry1, z1,
                        rx1 + rw1, ry1, z1,
                        rx2 + rw2, ry2, z2,
                        rx2 - rw2, ry2, z2,
                        sr, sg, sb, alpha * (1.0f - t1 * 0.45f)
                    );
                }
            }

            // 3. Ultra-Dense Kinetic Thrust Beam Core (Length along entire dash path)
            drawBeam(matrix, buffer, 0, 0, tipZ + 0.3f, 0, 0, tipZ - len, 0.16f * scale, 1.0f, 0.98f, 0.85f, alpha * 0.95f);
            drawBeam(matrix, buffer, 0, 0, tipZ + 0.2f, 0, 0, tipZ - len * 0.6f, 0.28f * scale, 1.0f, 0.80f, 0.0f, alpha * 0.60f);

            // 4. Expanding Wake Vortex Rings along path
            int ringCount = Math.max(2, (int) (len / 6.0f));
            for (int r = 1; r <= ringCount; r++) {
                float ringZ = tipZ - (r * (len / (ringCount + 1)));
                float ringR = (0.9f + progress * 0.8f + r * 0.3f) * scale;
                drawRing(matrix, buffer, 0, 0, ringZ, ringR, ringR * 0.78f, 16, 0.0f, 0.95f, 1.0f, alpha * 0.60f);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawTriangle(Matrix4f matrix, VertexConsumer consumer,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float x3, float y3, float z3,
                                     float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Reverse
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
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
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) return;

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
