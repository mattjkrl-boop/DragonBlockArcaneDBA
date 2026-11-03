package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.HollowRushTrailEntity;
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
 * Entity Renderer for Hollow Rush Dash Corridor in Minecraft 26.2.
 * Renders a physical 3D volumetric void Mach corridor, twin counter-spiraling streamlines, origin/destination spatial puncture rings, and a hyper-velocity void core spike.
 */
public class HollowRushTrailRenderer extends EntityRenderer<HollowRushTrailEntity, HollowRushTrailRenderer.TrailRenderState> {

    public HollowRushTrailRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TrailRenderState extends EntityRenderState {
        public float length = 5.0f;
        public boolean isThirdDash = false;
        public float age = 0;
        public float yRot = 0;
        public float xRot = 0;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(HollowRushTrailEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(HollowRushTrailEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public TrailRenderState createRenderState() {
        return new TrailRenderState();
    }

    @Override
    public void extractRenderState(HollowRushTrailEntity entity, TrailRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.length = entity.getTrailLength();
        state.isThirdDash = entity.isThirdDash();
        state.age = entity.tickCount + partialTicks;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
        if (mc.player != null) {
            boolean isRightHanded = (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.HollowsEdgeItem && 
                !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.HollowsEdgeItem));
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }
    }

    @Override
    public void submit(TrailRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = 14.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float alpha = ease(1.0f - progress) * (state.isThirdDash ? 0.95f : 0.80f);
        float len = Math.max(0.5f, state.length);
        float fpScale = state.isFirstPersonOwner ? 0.50f : 1.0f;
        float widthScale = (state.isThirdDash ? 1.4f : 1.0f) * fpScale;

        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        boolean thirdDash = state.isThirdDash;
        float spin = state.age;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            float close = ease(progress);

            if (thirdDash) {
                float slashR = 1.15f * widthScale * (0.72f + 0.28f * (1.0f - close));
                float slashZ = len * 0.62f;
                float arc = (float) (Math.PI * 1.25);
                drawVoidArc(matrix, buffer, slashR, 0.22f * widthScale, arc, 0.90f, slashZ, 0.28f, 0.02f, 0.62f, alpha * 0.85f);
                drawVoidArc(matrix, buffer, slashR * 0.92f, 0.07f * widthScale, arc * 0.92f, 0.40f, slashZ, 0.15f, 0.95f, 1.0f, alpha);
                drawVoidArc(matrix, buffer, slashR * 0.90f, 0.022f * widthScale, arc * 0.84f, 0.0f, slashZ, 1.0f, 1.0f, 1.0f, alpha);
                drawRingXY(matrix, buffer, 0, 0, slashZ, slashR * 1.02f, slashR * 0.84f, 24, 0.45f, 0.06f, 0.90f, alpha * (1.0f - close));
            } else {
                int tunnelSegments = 12;
                int slices = 6;
                float edgeRadius = 0.50f * widthScale;
                float coreRadius = 0.16f * widthScale;
                for (int slice = 0; slice < slices; slice++) {
                    float t1 = slice / (float) slices;
                    float t2 = (slice + 1) / (float) slices;
                    float z1 = t1 * len;
                    float z2 = t2 * len;
                    float pinch1 = 0.58f + 0.42f * (float) Math.sin(t1 * Math.PI);
                    float pinch2 = 0.58f + 0.42f * (float) Math.sin(t2 * Math.PI);
                    float er1 = edgeRadius * pinch1;
                    float er2 = edgeRadius * pinch2;
                    float cr1 = coreRadius * pinch1;
                    float cr2 = coreRadius * pinch2;
                    float sliceAlpha = alpha * (1.0f - t1 * 0.4f);
                    for (int i = 0; i < tunnelSegments; i++) {
                        double a1 = (i / (double) tunnelSegments) * Math.PI * 2.0;
                        double a2 = ((i + 1) / (double) tunnelSegments) * Math.PI * 2.0;
                        float c1 = (float) Math.cos(a1);
                        float s1 = (float) Math.sin(a1);
                        float c2 = (float) Math.cos(a2);
                        float s2 = (float) Math.sin(a2);
                        drawQuad(matrix, buffer, c1 * er1, s1 * er1, z1, c2 * er1, s2 * er1, z1, c2 * er2, s2 * er2, z2, c1 * er2, s1 * er2, z2, 0.25f, 0.02f, 0.55f, sliceAlpha * 0.70f);
                        drawQuad(matrix, buffer, c1 * cr1, s1 * cr1, z1, c2 * cr1, s2 * cr1, z1, c2 * cr2, s2 * cr2, z2, c1 * cr2, s1 * cr2, z2, 0.15f, 0.90f, 1.0f, sliceAlpha * 0.85f);
                    }
                }
                int ribbonSteps = 12;
                for (int strand = 0; strand < 2; strand++) {
                    float strandOffset = strand * (float) Math.PI;
                    for (int s = 0; s < ribbonSteps; s++) {
                        float t1 = s / (float) ribbonSteps;
                        float t2 = (s + 1) / (float) ribbonSteps;
                        float z1 = t1 * len;
                        float z2 = t2 * len;
                        float r1 = edgeRadius * 1.12f * (0.75f + 0.25f * (float) Math.sin(t1 * Math.PI));
                        float r2 = edgeRadius * 1.12f * (0.75f + 0.25f * (float) Math.sin(t2 * Math.PI));
                        double ang1 = strandOffset + (t1 * Math.PI * 3.0) + (spin * 0.3);
                        double ang2 = strandOffset + (t2 * Math.PI * 3.0) + (spin * 0.3);
                        float rx1 = (float) Math.cos(ang1) * r1;
                        float ry1 = (float) Math.sin(ang1) * r1;
                        float rx2 = (float) Math.cos(ang2) * r2;
                        float ry2 = (float) Math.sin(ang2) * r2;
                        float rw = 0.07f * widthScale * (float) Math.sin(t1 * Math.PI);
                        float rw2 = 0.07f * widthScale * (float) Math.sin(t2 * Math.PI);
                        float ribbonA = alpha * (1.0f - progress * 0.45f);
                        drawQuad(matrix, buffer, rx1 - rw, ry1, z1, rx1 + rw, ry1, z1, rx2 + rw2, ry2, z2, rx2 - rw2, ry2, z2, 0.75f, 0.10f, 1.0f, ribbonA);
                        drawQuad(matrix, buffer, rx1 - rw * 0.28f, ry1, z1, rx1 + rw * 0.28f, ry1, z1, rx2 + rw2 * 0.28f, ry2, z2, rx2 - rw2 * 0.28f, ry2, z2, 1.0f, 1.0f, 1.0f, ribbonA);
                        buffer.addVertex(matrix, rx1, ry1, z1).setColor(0.75f, 0.10f, 1.0f, ribbonA * 0.5f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                        buffer.addVertex(matrix, rx1 * 0.75f, ry1 * 0.75f, z1).setColor(0.75f, 0.10f, 1.0f, ribbonA * 0.5f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                        buffer.addVertex(matrix, rx2 * 0.75f, ry2 * 0.75f, z2).setColor(0.75f, 0.10f, 1.0f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                        buffer.addVertex(matrix, rx1 * 0.75f, ry1 * 0.75f, z1 - 0.08f * len).setColor(0.75f, 0.10f, 1.0f, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                    }
                }
                float startR = (0.70f * (1.0f - close)) * widthScale;
                float endR = (0.55f + 0.45f * close) * widthScale;
                if (startR > 0.05f) {
                    drawRingXY(matrix, buffer, 0, 0, 0, startR, startR * 0.72f, 20, 0.40f, 0.05f, 0.85f, alpha);
                }
                drawRingXY(matrix, buffer, 0, 0, len, endR, endR * 0.78f, 20, 0.15f, 0.95f, 1.0f, alpha * (1.0f - close));
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }

    /** A tapered arc across the dash. Tips pinch shut and the belly leads forward. */
    private static void drawVoidArc(Matrix4f matrix, VertexConsumer consumer, float radius, float band, float arc, float trail, float zCenter, float r, float g, float b, float a) {
        int segments = 22;
        float start = (float) (Math.PI * 0.5 - arc * 0.5);
        for (int i = 0; i < segments; i++) {
            float u1 = i / (float) segments;
            float u2 = (i + 1) / (float) segments;
            double a1 = start + u1 * arc;
            double a2 = start + u2 * arc;
            float pinch1 = (float) Math.sin(u1 * Math.PI);
            float pinch2 = (float) Math.sin(u2 * Math.PI);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float c2 = (float) Math.cos(a2);
            float s2 = (float) Math.sin(a2);
            float outer1 = radius + band * pinch1;
            float inner1 = Math.max(0.02f, radius - band * 0.55f * pinch1);
            float outer2 = radius + band * pinch2;
            float inner2 = Math.max(0.02f, radius - band * 0.55f * pinch2);
            float z1 = zCenter + 0.38f * pinch1;
            float z2 = zCenter + 0.38f * pinch2;
            drawQuad(matrix, consumer, c1 * inner1, s1 * inner1, z1, c2 * inner2, s2 * inner2, z2, c2 * outer2, s2 * outer2, z2, c1 * outer1, s1 * outer1, z1, r, g, b, a);
            if (trail > 0.01f) {
                float back1 = z1 - trail * (0.35f + 0.65f * pinch1);
                float back2 = z2 - trail * (0.35f + 0.65f * pinch2);
                consumer.addVertex(matrix, c1 * outer1, s1 * outer1, z1).setColor(r, g, b, a * 0.7f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
                consumer.addVertex(matrix, c2 * outer2, s2 * outer2, z2).setColor(r, g, b, a * 0.7f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
                consumer.addVertex(matrix, c2 * radius, s2 * radius, back2).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
                consumer.addVertex(matrix, c1 * radius, s1 * radius, back1).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            }
        }
    }

    private static void drawRingXY(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
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
