package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.BraveRushTrailEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
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
        public int maxLifetime = 30;
        public boolean isFirstPersonOwner = false;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BraveRushTrailEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BraveRushTrailEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
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
        state.maxLifetime = entity.getMaxLifetime();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(TrailRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // The lifetime comes from the entity, not a second hardcoded copy:
        // the trail now lives 30 ticks, long enough for the dash to read.
        float maxLife = (float) state.maxLifetime;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float open = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.25f));
        float alpha = FxEnvelope.envelope(state.age, maxLife, 0.25f, 0.4f) * 0.90f;
        float impact = strike(progress);
        float scale = state.trailScale * (state.isFirstPersonOwner ? 0.45f : 1.0f);
        float len = state.trailLength * (0.35f + 0.65f * open);
        float tipZ = state.isFirstPersonOwner ? (-0.6f * scale) : (0.6f * scale);

        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            int coneSegments = FxEnvelope.segments(24, distScale, 10);
            float coneLength = len;
            float coneBase = (state.isFirstPersonOwner ? 1.05f : 0.90f) * scale;
            float baseZ = tipZ - coneLength;
            float coneAlpha = state.isFirstPersonOwner ? (alpha * 0.32f) : (alpha * 0.72f);
            for (int i = 0; i < coneSegments; i++) {
                double a1 = (i / (double) coneSegments) * Math.PI * 2.0;
                double a2 = ((i + 1) / (double) coneSegments) * Math.PI * 2.0;
                float x1 = (float) Math.cos(a1) * coneBase;
                float y1 = (float) Math.sin(a1) * coneBase;
                float x2 = (float) Math.cos(a2) * coneBase;
                float y2 = (float) Math.sin(a2) * coneBase;
                drawTriangle(matrix, buffer, 0, 0, tipZ, x1, y1, baseZ, x2, y2, baseZ, 1.0f, 0.84f, 0.0f, coneAlpha);
                drawTriangle(matrix, buffer, 0, 0, tipZ * 0.85f, x1 * 0.42f, y1 * 0.42f, baseZ * 0.85f, x2 * 0.42f, y2 * 0.42f, baseZ * 0.85f, 0.0f, 0.95f, 1.0f, coneAlpha);
                drawTriangle(matrix, buffer, 0, 0, tipZ, x1 * 0.12f, y1 * 0.12f, tipZ - coneLength * 0.72f, x2 * 0.12f, y2 * 0.12f, tipZ - coneLength * 0.72f, 1.0f, 0.98f, 0.85f, alpha);
            }

            // Twin helical valor streamlines, as the header promises: two
            // strands half a turn apart, gold and cyan, each with a white
            // core. Before this pass the loop drew a single gold strand.
            int ribbonSteps = 14;
            for (int strand = 0; strand < 2; strand++) {
                double phase = strand * Math.PI;
                float sr = strand == 0 ? 1.0f : 0.0f;
                float sg = strand == 0 ? 0.84f : 0.95f;
                float sb = strand == 0 ? 0.0f : 1.0f;
                for (int s = 0; s < ribbonSteps; s++) {
                    float t1 = s / (float) ribbonSteps;
                    float t2 = (s + 1) / (float) ribbonSteps;
                    float z1 = tipZ - (t1 * len);
                    float z2 = tipZ - (t2 * len);
                    float r1 = (0.18f + t1 * 0.85f * open) * scale;
                    float r2 = (0.18f + t2 * 0.85f * open) * scale;
                    double ang1 = phase + (t1 * Math.PI * 2.4) + (state.age * 0.18);
                    double ang2 = phase + (t2 * Math.PI * 2.4) + (state.age * 0.18);
                    float rx1 = (float) Math.cos(ang1) * r1;
                    float ry1 = (float) Math.sin(ang1) * r1;
                    float rx2 = (float) Math.cos(ang2) * r2;
                    float ry2 = (float) Math.sin(ang2) * r2;
                    float rw1 = 0.10f * (1.0f - t1 * 0.4f) * scale;
                    float rw2 = 0.10f * (1.0f - t2 * 0.4f) * scale;
                    drawQuad(matrix, buffer,
                        rx1 - rw1, ry1, z1,
                        rx1 + rw1, ry1, z1,
                        rx2 + rw2, ry2, z2,
                        rx2 - rw2, ry2, z2,
                        sr, sg, sb, alpha * (1.0f - t1)
                    );
                    float cw1 = rw1 * 0.28f;
                    float cw2 = rw2 * 0.28f;
                    drawQuad(matrix, buffer,
                        rx1 - cw1, ry1, z1,
                        rx1 + cw1, ry1, z1,
                        rx2 + cw2, ry2, z2,
                        rx2 - cw2, ry2, z2,
                        1.0f, 0.98f, 0.9f, alpha * (1.0f - t1)
                    );
                }
            }

            if (impact > 0.02f) {
                float ringR = coneBase * (0.55f + 0.65f * impact);
                drawRing(matrix, buffer, 0, 0, tipZ, ringR, ringR * 0.62f, 24, 1.0f, 0.96f, 0.7f, impact * 0.85f);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float strike(float progress) {
        if (progress >= 0.34f) return 0.0f;
        return (float) Math.sin((progress / 0.34f) * Math.PI);
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

        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
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
}
