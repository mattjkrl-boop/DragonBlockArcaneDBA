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
        public boolean isFirstPersonOwner = false;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BanshoWindProjectileEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BanshoWindProjectileEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
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

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(WindProjectileRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        float age = state.ageInTicks;
        float fpScale = state.isFirstPersonOwner ? 0.55f : 1.0f;

        // Age envelope: every part used to sit at a constant alpha, so the
        // drill popped in at spawn and vanished at the 50 tick range cap. In
        // over the first 2 ticks, out over the last 5.
        float inT = Math.min(1.0f, age / 3.0f);
        float inEase = 1.0f - (1.0f - inT) * (1.0f - inT) * (1.0f - inT);
        float outT = age <= 42.0f ? 0.0f : Math.min(1.0f, (age - 42.0f) / 8.0f);
        float envelope = inEase * (1.0f - outT * outT * (3.0f - 2.0f * outT));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. High-Speed Spinning Aerodynamic Wind Drill (Central conical core)
            int drillSegments = 18;
            float drillLength = 1.35f * fpScale;
            // Secondary motion (T4): the drill radius breathes as it spins.
            float drillRadius = 0.32f * fpScale * (1.0f + 0.05f * (float) Math.sin(age * 1.5f));
            float drillRot = age * 45.0f * (float) (Math.PI / 180.0);

            for (int i = 0; i < drillSegments; i++) {
                double a1 = ((i / (double) drillSegments) * Math.PI * 2.0) + drillRot;
                double a2 = (((i + 1) / (double) drillSegments) * Math.PI * 2.0) + drillRot;

                float x1 = (float) Math.cos(a1) * drillRadius;
                float y1 = (float) Math.sin(a1) * drillRadius;
                float x2 = (float) Math.cos(a2) * drillRadius;
                float y2 = (float) Math.sin(a2) * drillRadius;

                // Outer emerald vortex drill cone in two bands (T2 gradient):
                // deep emerald at the tail band, bright jade from mid-cone to
                // the tip (the cone tapers to a point at +Z = drillLength * 0.5).
                float midX1 = (float) Math.cos(a1) * drillRadius * 0.52f;
                float midY1 = (float) Math.sin(a1) * drillRadius * 0.52f;
                float midX2 = (float) Math.cos(a2) * drillRadius * 0.52f;
                float midY2 = (float) Math.sin(a2) * drillRadius * 0.52f;

                drawQuad(matrix, buffer,
                    x1, y1, -drillLength * 0.5f,
                    x2, y2, -drillLength * 0.5f,
                    midX2, midY2, 0.0f,
                    midX1, midY1, 0.0f,
                    0.0f, 0.82f, 0.42f, 0.85f * envelope
                );
                drawTriangle(matrix, buffer,
                    midX1, midY1, 0.0f,
                    midX2, midY2, 0.0f,
                    0, 0, drillLength * 0.5f,
                    0.45f, 1.0f, 0.82f, 0.85f * envelope
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
                    0.7f, 1.0f, 0.90f, 0.95f * envelope
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
                float tipX = bx + tx * (bladeLen * 0.6f);
                float tipY = by + ty * (bladeLen * 0.6f);
                float tailX = bx - tx * (bladeLen * 0.4f);
                float tailY = by - ty * (bladeLen * 0.4f);
                float ox = (float) Math.cos(bAngle);
                float oy = (float) Math.sin(bAngle);
                drawLash(matrix, buffer,
                    tailX, tailY, -0.18f,
                    bx + ox * 0.14f, by + oy * 0.14f, 0.06f,
                    tipX, tipY, 0.34f,
                    0.07f * fpScale, 6,
                    0.05f, 0.72f, 0.38f,
                    0.82f, 1.0f, 0.92f,
                    0.92f * envelope
                );
            }

            // A thin helix wrapped on the drill, then one tapered wake behind it.
            int helixSteps = 14;
            float helixRot = age * 28.0f * (float) (Math.PI / 180.0);
            for (int helix = 0; helix < 2; helix++) {
                float phase = helix * (float) Math.PI;
                for (int s = 0; s < helixSteps; s++) {
                    float t1 = s / (float) helixSteps;
                    float t2 = (s + 1) / (float) helixSteps;
                    float z1 = -drillLength * 0.42f + t1 * drillLength * 0.85f;
                    float z2 = -drillLength * 0.42f + t2 * drillLength * 0.85f;
                    double a1 = helixRot + phase + t1 * Math.PI * 4.5;
                    double a2 = helixRot + phase + t2 * Math.PI * 4.5;
                    float rad1 = drillRadius * (1.08f - t1 * 0.30f);
                    float rad2 = drillRadius * (1.08f - t2 * 0.30f);
                    float x1 = (float) Math.cos(a1) * rad1;
                    float y1 = (float) Math.sin(a1) * rad1;
                    float x2 = (float) Math.cos(a2) * rad2;
                    float y2 = (float) Math.sin(a2) * rad2;
                    float w1 = 0.04f * fpScale * (1.0f - t1 * 0.35f);
                    float w2 = 0.04f * fpScale * (1.0f - t2 * 0.35f);
                    float px1 = (float) -Math.sin(a1) * w1;
                    float py1 = (float) Math.cos(a1) * w1;
                    float px2 = (float) -Math.sin(a2) * w2;
                    float py2 = (float) Math.cos(a2) * w2;
                    drawQuad(matrix, buffer,
                        x1 - px1, y1 - py1, z1,
                        x1 + px1, y1 + py1, z1,
                        x2 + px2, y2 + py2, z2,
                        x2 - px2, y2 - py2, z2,
                        helix == 0 ? 0.15f : 0.75f, 1.0f, helix == 0 ? 0.62f : 0.92f, (0.70f - t1 * 0.35f) * envelope
                    );
                }
            }

            // One tapered wake behind the drill. It is the follow-through, not a stack of rings.
            float wakeR = drillRadius * 0.85f;
            drawRingZ(matrix, buffer, -drillLength * 0.55f, wakeR, wakeR * 0.45f, 20, 0.15f, 1.0f, 0.75f, 0.55f * envelope);
            drawRingZ(matrix, buffer, -drillLength * 1.15f, wakeR * 0.45f, 0.02f, 16, 0.55f, 1.0f, 0.88f, 0.22f * envelope);

            // Tapered nose: several bands pinch to a point, with a white core and a fading tip.
            float tipZ = drillLength * 0.5f;
            int noseBands = 4;
            int noseSides = 8;
            for (int band = 0; band < noseBands; band++) {
                float p1 = band / (float) noseBands;
                float p2 = (band + 1) / (float) noseBands;
                float z1 = tipZ + p1 * 0.42f;
                float z2 = tipZ + p2 * 0.42f;
                float rad1 = 0.12f * (1.0f - p1) * fpScale;
                float rad2 = 0.12f * (1.0f - p2) * fpScale;
                float aBand = envelope * (1.0f - p1 * p1);
                for (int t = 0; t < noseSides; t++) {
                    double a1 = (t / (double) noseSides) * Math.PI * 2.0 + drillRot;
                    double a2 = ((t + 1) / (double) noseSides) * Math.PI * 2.0 + drillRot;
                    float x1 = (float) Math.cos(a1) * rad1;
                    float y1 = (float) Math.sin(a1) * rad1;
                    float x2 = (float) Math.cos(a2) * rad1;
                    float y2 = (float) Math.sin(a2) * rad1;
                    float x3 = (float) Math.cos(a2) * rad2;
                    float y3 = (float) Math.sin(a2) * rad2;
                    float x4 = (float) Math.cos(a1) * rad2;
                    float y4 = (float) Math.sin(a1) * rad2;
                    drawQuad(matrix, buffer, x1, y1, z1, x2, y2, z1, x3, y3, z2, x4, y4, z2,
                        0.45f, 1.0f, 0.82f, aBand * 0.75f);
                    drawQuad(matrix, buffer,
                        x1 * 0.35f, y1 * 0.35f, z1, x2 * 0.35f, y2 * 0.35f, z1,
                        x3 * 0.35f, y3 * 0.35f, z2, x4 * 0.35f, y4 * 0.35f, z2,
                        1.0f, 1.0f, 1.0f, aBand);
                }
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawLash(Matrix4f matrix, VertexConsumer consumer,
                                 float x0, float y0, float z0,
                                 float xc, float yc, float zc,
                                 float x1, float y1, float z1,
                                 float width, int steps,
                                 float er, float eg, float eb,
                                 float cr, float cg, float cb,
                                 float alpha) {
        int n = Math.max(4, steps);
        float px = x0;
        float py = y0;
        float pz = z0;
        for (int i = 1; i <= n; i++) {
            float t = i / (float) n;
            float u = 1.0f - t;
            float x = u * u * x0 + 2.0f * u * t * xc + t * t * x1;
            float y = u * u * y0 + 2.0f * u * t * yc + t * t * y1;
            float z = u * u * z0 + 2.0f * u * t * zc + t * t * z1;
            float w = width * (1.0f - t * 0.9f);
            float a = alpha * (1.0f - t * t);
            lashSpan(matrix, consumer, px, py, pz, x, y, z, w, er, eg, eb, a);
            lashSpan(matrix, consumer, px, py, pz, x, y, z, w * 0.32f, cr, cg, cb, Math.min(1.0f, a * 1.2f));
            px = x;
            py = y;
            pz = z;
        }
    }

    private static void lashSpan(Matrix4f matrix, VertexConsumer consumer,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float width,
                                 float r, float g, float b, float a) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float ax = 0.0f;
        float ay = 1.0f;
        float az = 0.0f;
        if (dx * dx + dz * dz < dy * dy) {
            ax = 1.0f;
            ay = 0.0f;
            az = 0.0f;
        }
        float px = dy * az - dz * ay;
        float py = dz * ax - dx * az;
        float pz = dx * ay - dy * ax;
        float plen = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (plen < 0.0001f) {
            return;
        }
        float s = width / plen;
        px *= s;
        py *= s;
        pz *= s;
        consumer.addVertex(matrix, x0 - px, y0 - py, z0 - pz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x0 + px, y0 + py, z0 + pz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 + px, y1 + py, z1 + pz).setColor(r, g, b, a * 0.65f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 - px, y1 - py, z1 - pz).setColor(r, g, b, a * 0.65f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 - px, y1 - py, z1 - pz).setColor(r, g, b, a * 0.65f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x1 + px, y1 + py, z1 + pz).setColor(r, g, b, a * 0.65f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x0 + px, y0 + py, z0 + pz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x0 - px, y0 - py, z0 - pz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
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
