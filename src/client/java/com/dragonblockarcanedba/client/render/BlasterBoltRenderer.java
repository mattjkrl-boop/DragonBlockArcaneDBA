package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.BlasterBoltEntity;
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
 * Entity Renderer for Blaster Bolt in MC 26.2.
 * Renders an aerodynamic high-energy physical 3D laser bolt:
 * - Superdense white-hot cylindrical inner core rod with aerodynamic conical head
 * - Multi-faceted fluted plasma compression sheath with heat-based color shifting
 * - 4 physical aerodynamic stabilizer energy vanes radiating along the fuselage
 * - Orbital plasma compression nodes and leading edge cross-aperture flare
 * - Extended 3D ion tracer tail wake with alpha gradient decay
 */
public class BlasterBoltRenderer extends EntityRenderer<BlasterBoltEntity, BlasterBoltRenderer.BlasterBoltRenderState> {
    public BlasterBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BlasterBoltRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public boolean isOvercharged = false;
        public float heatRatio = 0.0f;
        public float age = 0.0f;
        public int impactAge = -1;
        public boolean isFirstPersonOwner = false;
    }

    @Override
    public BlasterBoltRenderState createRenderState() {
        return new BlasterBoltRenderState();
    }

    @Override
    public void extractRenderState(BlasterBoltEntity entity, BlasterBoltRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.isOvercharged = entity.isOvercharged();
        state.heatRatio = entity.getHeatRatio();
        state.age = entity.tickCount + partialTicks;
        state.impactAge = entity.getImpactAge();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(BlasterBoltRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        float age = state.age;
        float pulse = 1.0f + 0.08f * (float) Math.sin(age * 2.5f);

        boolean overcharged = state.isOvercharged;
        float heat = state.heatRatio;

        float fpScale = state.isFirstPersonOwner ? 0.60f : 1.0f;
        float radius = (overcharged ? 0.36f * pulse : (0.18f + heat * 0.12f) * pulse) * fpScale;
        float headLength = (overcharged ? 1.4f : (0.85f + heat * 0.35f)) * fpScale;
        float tailLength = (overcharged ? 3.4f : (2.0f + heat * 1.0f)) * fpScale;

        // Color palette based on heat / overcharge
        float r, g, b;
        if (overcharged) {
            r = 1.0f;
            g = 0.12f;
            b = 0.32f;
        } else if (heat > 0.6f) {
            r = 1.0f;
            g = 0.45f;
            b = 0.05f;
        } else {
            r = 1.0f;
            g = 0.88f - heat * 0.30f;
            b = 0.10f;
        }

        // On impact the same finned bolt collapses and fades. It does not swap for a disc.
        final float impactT = state.impactAge < 0 ? 0.0f : Math.min(1.0f, state.impactAge / 5.0f);
        final float impactEase = 1.0f - (1.0f - impactT) * (1.0f - impactT) * (1.0f - impactT);
        final float boltDie = 1.0f - impactT * impactT * (3.0f - 2.0f * impactT);
        final float drawRadius = radius * (1.0f - impactEase * 0.62f);
        final float drawHead = headLength * (1.0f - impactEase * 0.70f);
        final float drawTail = tailLength * (1.0f - impactEase * 0.20f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            int sides = 12;

            // Soft edge of the same tapered sheath, then the body, then the white core.
            drawPlasmaBolt(matrix, buffer, drawRadius * 1.22f, drawHead, drawTail * 0.85f, sides, age * 20.0f, r, g, b, 0.22f * boltDie);
            drawPlasmaBolt(matrix, buffer, drawRadius, drawHead, drawTail, sides, age * 20.0f, r, g, b, 0.88f * boltDie);
            drawPlasmaBolt(matrix, buffer, drawRadius * 0.38f, drawHead * 0.85f, drawTail * 0.55f, sides, -age * 30.0f, 1.0f, 0.97f, 0.82f, boltDie);

            float finSpan = drawRadius * 2.3f;
            float finBaseZ = -drawHead * 0.2f;
            float finBackZ = -drawTail * 0.55f;

            for (int f = 0; f < 4; f++) {
                double angle = (f / 4.0) * Math.PI * 2.0 + Math.toRadians(age * 12.0f);
                float fx = (float) Math.cos(angle) * finSpan;
                float fy = (float) Math.sin(angle) * finSpan;
                float ox = (float) Math.cos(angle);
                float oy = (float) Math.sin(angle);
                drawLash(matrix, buffer,
                    ox * drawRadius * 0.25f, oy * drawRadius * 0.25f, drawHead * 0.28f,
                    fx * 0.92f, fy * 0.92f, finBaseZ,
                    fx * 0.38f, fy * 0.38f, finBackZ,
                    drawRadius * 0.20f, 6,
                    r, g, b,
                    1.0f, 0.96f, 0.78f,
                    0.88f * boltDie
                );
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

    private static void drawPlasmaBolt(Matrix4f matrix, VertexConsumer consumer,
                                       float radius, float headLen, float tailLen, int sides,
                                       float rotDeg, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2.0 + rotRad;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2.0 + rotRad;

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            // Forward aerodynamic nose cone
            drawTriangle(matrix, consumer,
                0, 0, headLen,
                x2, y2, 0,
                x1, y1, 0,
                r, g, b, a
            );

            // Cylindrical body mid-section
            drawQuad(matrix, consumer,
                x1, y1, 0,
                x2, y2, 0,
                x2, y2, -tailLen * 0.35f,
                x1, y1, -tailLen * 0.35f,
                r, g, b, a
            );

            // Backward ion tracer tail (Tapered to vertex point with alpha fade)
            consumer.addVertex(matrix, x1, y1, -tailLen * 0.35f).setColor(r, g, b, a).setUv(0, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, x2, y2, -tailLen * 0.35f).setColor(r, g, b, a).setUv(1, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, 0, 0, -tailLen).setColor(r, g, b, 0.0f).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);
            consumer.addVertex(matrix, 0, 0, -tailLen).setColor(r, g, b, 0.0f).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, -1);

            // Reverse for tail
            consumer.addVertex(matrix, 0, 0, -tailLen).setColor(r, g, b, 0.0f).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, 0, 0, -tailLen).setColor(r, g, b, 0.0f).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, y2, -tailLen * 0.35f).setColor(r, g, b, a).setUv(1, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, y1, -tailLen * 0.35f).setColor(r, g, b, a).setUv(0, 0.5f).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    private static void drawTriangle(Matrix4f matrix, VertexConsumer consumer,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float x3, float y3, float z3,
                                     float r, float g, float b, float a) {
        drawQuad(matrix, consumer, x1, y1, z1, x2, y2, z2, x3, y3, z3, x1, y1, z1, r, g, b, a);
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

        // Reverse side
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
